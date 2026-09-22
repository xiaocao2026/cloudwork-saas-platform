# CloudWork 数据库设计

## 1. 设计目标

CloudWork 是一个多租户 SaaS 协作平台。

数据库设计重点解决以下问题：

- Workspace 如何作为 SaaS Tenant 边界
- 用户如何加入不同 Workspace
- Tenant Role 与平台角色如何解耦
- Project / Task 如何实现租户隔离
- 微服务之间如何保持数据所有权
- 为什么不使用跨服务物理外键
- 如何为常用查询建立必要索引

当前主要业务数据库：

```text
cloudwork_tenant
cloudwork_project
```

其中：

```text
Tenant Service
    ↓
cloudwork_tenant
```

```text
Project Service
    ↓
cloudwork_project
```

两个业务服务不直接访问彼此数据库。

---

## 2. 数据库边界

当前数据库划分：

```text
cloudwork_tenant
├── tenant
├── tenant_role
└── tenant_member

cloudwork_project
├── project
└── task
```

设计原则：

```text
服务拥有自己的数据库
+
跨服务通过 API 交互
+
不跨服务直接 JOIN
+
不建立跨服务物理外键
```

例如 Project Service 需要判断用户是否属于某个 Workspace 时：

不会直接执行：

```text
cloudwork_project
JOIN
cloudwork_tenant.tenant_member
```

而是：

```text
Project Service
   ↓
OpenFeign
   ↓
Tenant Service
   ↓
tenant_member
```

这样可以维持清晰的服务边界。

---

## 3. cloudwork_tenant

`cloudwork_tenant` 负责多租户 Workspace 相关数据。

主要表：

```text
tenant
tenant_role
tenant_member
```

---

## 4. tenant

### 作用

保存 Workspace 基本信息。

一个 Workspace 对应 CloudWork 中的一个逻辑 Tenant。

主要字段：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| tenant_id | BIGINT | 主键 |
| tenant_code | VARCHAR(64) | Workspace 唯一编码 |
| tenant_name | VARCHAR(100) | Workspace 名称 |
| status | TINYINT | Workspace 状态 |
| created_by_user_id | BIGINT | 创建用户 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| version | INT | 乐观版本字段 |
| remark | VARCHAR | 备注 |

---

## 5. Tenant 状态

当前 Tenant 状态定义：

```text
0 = ACTIVE
1 = DISABLED
2 = ARCHIVED
```

当前核心业务主要使用：

```text
ACTIVE
```

状态。

---

## 6. tenant_code

Workspace Code 由服务端生成。

格式：

```text
cw_<UUID>
```

例如：

```text
cw_45b9b784a201461b9d3fe20c2801bcf6
```

客户端不能自己提交：

```text
tenantCode
```

避免：

- 重复
- 伪造
- 客户端控制系统字段

---

## 7. tenant_role

### 作用

保存 Workspace 内部角色。

Tenant Role 与 System Service 中的平台角色属于不同概念。

```text
平台角色
≠
Workspace 角色
```

当前内置角色：

```text
OWNER
ADMIN
MEMBER
```

主要字段：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| role_id | BIGINT | 主键 |
| tenant_id | BIGINT | Workspace ID |
| role_code | VARCHAR | 角色编码 |
| role_name | VARCHAR | 角色名称 |
| role_type | TINYINT | 系统角色 / 自定义角色 |
| status | TINYINT | 角色状态 |
| sort_order | INT | 排序 |
| description | VARCHAR | 描述 |
| version | INT | 版本 |

---

## 8. Tenant Role 唯一约束

同一个 Workspace 下，Role Code 必须唯一。

逻辑约束：

```text
UNIQUE(
    tenant_id,
    role_code
)
```

因此：

```text
Workspace A / OWNER
Workspace B / OWNER
```

可以同时存在。

但：

```text
Workspace A / OWNER
Workspace A / OWNER
```

不能重复。

---

## 9. tenant_member

### 作用

维护用户与 Workspace 之间的成员关系。

它是 CloudWork 多租户授权的核心表。

主要字段：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| member_id | BIGINT | 主键 |
| tenant_id | BIGINT | Workspace |
| user_id | BIGINT | 平台用户 ID |
| role_id | BIGINT | Workspace Role |
| member_status | TINYINT | 成员状态 |
| invited_by_user_id | BIGINT | 邀请人 |
| invited_at | DATETIME | 邀请时间 |
| joined_at | DATETIME | 加入时间 |
| version | INT | 版本 |

---

## 10. Tenant Member 状态

当前设计：

```text
0 = INVITED
1 = ACTIVE
2 = DISABLED
3 = LEFT
```

当前权限判断必须满足：

```text
member_status = ACTIVE
```

也就是：

```text
member_status = 1
```

仅仅存在：

```text
tenant_id + user_id
```

关系还不够。

成员必须处于 ACTIVE 状态。

---

## 11. Workspace 成员唯一约束

一个用户在一个 Workspace 下只能存在一条成员关系。

约束：

```text
UNIQUE(
    tenant_id,
    user_id
)
```

防止：

```text
同一个用户
在同一个 Workspace
出现多条 Member 记录
```

---

## 12. Workspace 创建事务

创建 Workspace 时会同时写入：

```text
tenant
↓
tenant_role OWNER
tenant_role ADMIN
tenant_role MEMBER
↓
tenant_member
```

创建者最终成为：

```text
ACTIVE OWNER
```

整个过程位于同一个本地事务中。

如果其中任意步骤失败：

```text
整个事务回滚
```

不会留下：

```text
Tenant 已创建
但是没有 OWNER
```

这种不完整数据。

---

## 13. cloudwork_project

`cloudwork_project` 保存协作业务。

当前包括：

```text
project
task
```

Project 与 Task 属于同一个领域，因此目前由：

```text
cloudwork-project
```

服务统一管理。

---

## 14. project

### 作用

保存 Workspace 下的 Project。

主要字段：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| project_id | BIGINT | 主键 |
| tenant_id | BIGINT | 所属 Workspace |
| project_code | VARCHAR(64) | Project 唯一编码 |
| project_name | VARCHAR(100) | Project 名称 |
| description | VARCHAR(500) | 描述 |
| status | TINYINT | Project 状态 |
| created_by_user_id | BIGINT | 创建人 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| version | INT | 版本 |

---

## 15. Project Code

由服务端生成：

```text
prj_<UUID>
```

例如：

```text
prj_5b75187a0bae4b81b9284b6401c03803
```

客户端不能自己控制：

```text
projectCode
```

---

## 16. Project 状态

当前：

```text
0 = ACTIVE
1 = ARCHIVED
```

当前列表接口主要查询：

```text
ACTIVE
```

Project。

---

## 17. Project 租户隔离

每一条 Project 必须带：

```text
tenant_id
```

例如：

```text
project_id = 1
tenant_id = 1
```

查询 Project 详情时，不允许：

```sql
WHERE project_id = ?
```

而应该：

```sql
WHERE project_id = ?
  AND tenant_id = ?
```

这样即使不同租户知道某个：

```text
project_id
```

也无法直接读取。

---

## 18. Project 索引设计

主要索引包括：

```text
INDEX(tenant_id, status)
```

用于：

```text
按 Workspace 查询 ACTIVE Project
```

以及：

```text
INDEX(
    tenant_id,
    created_by_user_id
)
```

用于后续按租户与创建人查询。

---

## 19. task

### 作用

保存 Project 下的任务。

主要字段：

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| task_id | BIGINT | 主键 |
| tenant_id | BIGINT | Workspace |
| project_id | BIGINT | Project |
| title | VARCHAR(200) | 标题 |
| description | VARCHAR(1000) | 描述 |
| status | VARCHAR(20) | 状态 |
| priority | VARCHAR(20) | 优先级 |
| assignee_user_id | BIGINT | 指派用户 |
| created_by_user_id | BIGINT | 创建人 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| version | INT | 版本 |

---

## 20. Task Status

当前任务状态：

```text
TODO
DOING
DONE
```

状态流：

```text
TODO
 ↓
DOING
 ↓
DONE
```

当前版本没有实现任意状态跳转。

例如：

```text
DONE → TODO
```

不是当前核心流程。

---

## 21. Task Priority

当前：

```text
LOW
MEDIUM
HIGH
```

默认：

```text
MEDIUM
```

---

## 22. Task 租户隔离

Task 同时保存：

```text
tenant_id
project_id
```

查询任务：

```sql
WHERE tenant_id = ?
  AND project_id = ?
```

更新任务状态：

```sql
UPDATE task
SET status = ?
WHERE task_id = ?
  AND tenant_id = ?
```

而不是：

```sql
UPDATE task
SET status = ?
WHERE task_id = ?
```

这是为了防止通过其他租户的 `taskId` 修改数据。

---

## 23. Task 创建校验

创建 Task 前：

```text
tenant access
↓
验证 Project 是否属于 tenantId
↓
创建 Task
```

Project 存在性校验同时包含：

```text
project_id
tenant_id
```

不能仅根据：

```text
project_id
```

判断。

---

## 24. Task 索引设计

主要索引：

```text
INDEX(
    tenant_id,
    project_id
)
```

用于 Project Task 列表。

以及：

```text
INDEX(
    tenant_id,
    project_id,
    status
)
```

支持后续按任务状态过滤。

另外：

```text
INDEX(
    tenant_id,
    assignee_user_id
)
```

为后续：

```text
My Tasks
```

功能预留。

---

## 25. 为什么 Task 同时保存 tenant_id

理论上：

```text
Task
→ Project
→ Tenant
```

可以通过 Project 间接得到 tenant_id。

但 CloudWork 仍然在 Task 中冗余保存：

```text
tenant_id
```

原因主要有：

- SQL 可直接租户过滤
- 降低查询 JOIN
- 更新时可以直接带 tenant_id
- 提高安全边界清晰度
- 支持后续分区或归档
- 支持租户级查询

这种冗余属于有明确业务意义的数据冗余。

---

## 26. 为什么不用物理外键

当前没有建立：

```text
task.project_id
→
project.project_id
```

数据库物理 FK。

同时更不会建立：

```text
project.tenant_id
→
cloudwork_tenant.tenant.tenant_id
```

跨服务 FK。

主要原因：

### 服务自治

服务应该控制自己的数据模型。

### 降低耦合

数据库 Schema 变更不会直接影响其他服务。

### 独立部署

未来不同服务可能使用：

```text
不同数据库实例
```

### 微服务边界

服务之间应该通过：

```text
API
```

协作，而不是通过数据库约束形成隐式耦合。

---

## 27. 数据一致性责任

没有物理 FK 并不意味着不做一致性校验。

CloudWork 通过：

```text
Service 层
+
Tenant Access API
+
Project Exists 校验
+
tenant_id SQL 条件
+
数据库唯一索引
```

保证当前核心数据关系。

---

## 28. 跨服务数据关系

System User：

```text
System Service
user_id
```

Tenant Member：

```text
Tenant Service
tenant_member.user_id
```

两者之间不存在数据库物理外键。

Tenant Service 信任平台层提供的：

```text
userId
```

但用户身份必须来自：

```text
SecurityContext
```

而不是客户端自由提交。

---

## 29. 典型租户授权 SQL

Tenant Access 查询逻辑：

```sql
SELECT
    m.tenant_id AS tenantId,
    m.user_id AS userId,
    r.role_code AS roleCode
FROM tenant_member m
INNER JOIN tenant_role r
    ON r.role_id = m.role_id
   AND r.tenant_id = m.tenant_id
WHERE m.tenant_id = ?
  AND m.user_id = ?
  AND m.member_status = 1
  AND r.status = 0;
```

查询成功后：

```text
accessible = true
```

查不到：

```text
accessible = false
```

---

## 30. MyBatis 映射问题

项目实际开发中曾遇到：

```text
SQL Total = 1
但是 Java VO = null
```

原因是查询返回：

```text
tenant_id
user_id
role_code
```

而 Java 属性：

```text
tenantId
userId
roleCode
```

最终显式使用：

```sql
AS tenantId
AS userId
AS roleCode
```

保证 MyBatis 映射正确。

这也是当前 SQL 中保留显式 Alias 的原因。

---

## 31. 数据安全验证

已经进行过实际越权验证。

当前用户属于：

```text
tenantId = 1
```

请求：

```text
tenantId = 999999
```

创建 Project。

结果：

```text
403
```

随后数据库执行：

```sql
SELECT
    project_id,
    tenant_id,
    project_code,
    project_name
FROM project
WHERE tenant_id = 999999;
```

结果：

```text
Empty set
```

证明失败请求没有产生脏数据。

---

## 32. Task 越权验证

已有 Task：

```text
taskId = 1
tenantId = 1
status = DONE
```

尝试：

```text
tenantId = 999999
status = DOING
```

结果：

```text
403
```

重新查询 Task：

```text
status = DONE
```

证明非法请求没有修改原数据。

---

## 33. 当前数据库设计取舍

当前重点：

```text
简单
清晰
安全
可验证
```

没有提前增加：

- 分库分表
- 全局分布式 ID
- CDC
- Event Sourcing
- CQRS
- 分布式事务
- 数据仓库

这些技术只有在业务量或一致性需求真实出现时再引入。

---

## 34. 后续演进

如果未来业务规模增长，可以考虑：

### Project / Task 数据量增长

引入：

```text
tenant_id 分区
```

或者：

```text
分库分表
```

### 高频查询

增加：

```text
Redis Cache
```

### 跨服务事件

增加：

```text
Kafka
```

### Project / Task 搜索

增加：

```text
Elasticsearch
```

### 文件附件

增加：

```text
MinIO
```

### 数据审计

增加：

```text
audit_log
```

当前版本不提前引入这些复杂度。