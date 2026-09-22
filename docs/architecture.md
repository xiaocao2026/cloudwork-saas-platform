# CloudWork 架构设计

## 1. 架构目标

CloudWork 是一个多租户 SaaS 协作平台，当前核心目标不是堆叠中间件，而是完成一个清晰、可运行、可演示的微服务业务闭环。

核心业务：

```text
Workspace
   ↓
Project
   ↓
Task
   ↓
TODO → DOING → DONE
```

架构设计重点关注以下问题：

- 如何完成统一登录认证
- 如何通过网关统一路由请求
- 如何完成服务注册与发现
- 如何完成服务间调用
- 如何保证 SaaS 租户隔离
- 如何避免跨服务数据库强耦合
- 如何保证核心事务一致性
- 如何在当前项目规模下控制微服务复杂度

---

## 2. 总体架构

```text
                        ┌─────────────────────┐
                        │       Vue 3         │
                        │    Element Plus     │
                        └──────────┬──────────┘
                                   │
                                   ▼
                        ┌─────────────────────┐
                        │ Spring Cloud Gateway│
                        │       :8080         │
                        └──────────┬──────────┘
                                   │
              ┌────────────────────┼─────────────────────┐
              │                    │                     │
              ▼                    ▼                     ▼
       ┌─────────────┐      ┌─────────────┐      ┌──────────────┐
       │    Auth     │      │   System    │      │    Tenant    │
       │    :9200    │      │    :9201    │      │    :9202     │
       └─────────────┘      └─────────────┘      └──────┬───────┘
                                                        ▲
                                                        │ OpenFeign
                                                        │
                                                 ┌──────┴───────┐
                                                 │   Project    │
                                                 │    :9203     │
                                                 │ Project/Task │
                                                 └──────────────┘

               ┌─────────┐      ┌─────────┐      ┌─────────┐
               │  Nacos  │      │  Redis  │      │  MySQL  │
               └─────────┘      └─────────┘      └─────────┘
```

---

## 3. 服务职责

### Gateway

职责：

- 统一 API 入口
- JWT 认证过滤
- Redis 登录状态校验
- 请求路由
- 用户身份信息向下游透传

端口：

```text
8080
```

---

### Auth

职责：

- 用户登录认证
- Token 生成
- 登录流程处理

端口：

```text
9200
```

---

### System

职责：

- 用户基础信息
- 系统角色
- 菜单与权限等基础平台能力

端口：

```text
9201
```

System 中的角色属于平台层 RBAC。

---

### Tenant Service

服务名：

```text
cloudwork-tenant
```

端口：

```text
9202
```

职责：

- Workspace 创建
- Workspace 查询
- Tenant Role
- Tenant Member
- Workspace 成员权限校验
- 为其他业务服务提供租户访问验证能力

核心数据：

```text
tenant
tenant_role
tenant_member
```

---

### Project Service

服务名：

```text
cloudwork-project
```

端口：

```text
9203
```

职责：

- Project 创建
- Project 查询
- Task 创建
- Task 查询
- Task 状态流转
- 调用 Tenant Service 验证 Workspace 权限

核心数据：

```text
project
task
```

---

## 4. 为什么 Project 与 Task 在同一个服务

当前设计：

```text
cloudwork-project
├── Project
└── Task
```

而不是：

```text
cloudwork-project
cloudwork-task
```

主要原因是 Project 与 Task 属于同一个协作领域，两者业务关联较强。

如果当前阶段强行拆分，会额外增加：

- 服务注册
- 服务发现
- Feign 调用
- 网络失败场景
- 分布式事务复杂度
- 部署单元
- 运维成本

但业务收益有限。

因此当前阶段采用领域内聚优先的设计。

如果未来 Task 领域扩展为独立复杂模块，例如增加：

- 评论
- 附件
- 工作流
- SLA
- 自动规则
- 任务订阅
- 大规模任务调度

再考虑拆分为独立 Task Service。

---

## 5. 请求链路

以创建 Project 为例：

```text
Browser
   ↓
Vue
   ↓
POST /project/projects
   ↓
Gateway
   ↓
JWT 校验
   ↓
Redis Session 校验
   ↓
注入当前用户身份
   ↓
cloudwork-project
   ↓
SecurityUtils.getUserId()
   ↓
OpenFeign
   ↓
cloudwork-tenant
   ↓
tenant_member
   ↓
是否为 ACTIVE 成员
   ↓
是
   ↓
INSERT project
```

如果用户不是目标 Workspace 的 ACTIVE 成员：

```text
Project Service
   ↓
Tenant Service
   ↓
accessible = false
   ↓
403
```

---

## 6. 认证与授权分层

CloudWork 将平台身份认证与 SaaS 租户授权分离。

### 平台认证

解决：

```text
你是谁？
```

由：

- JWT
- Redis
- Gateway
- SecurityContext

共同完成。

业务代码通过：

```java
SecurityUtils.getUserId();
```

获取当前用户身份。

---

### Tenant 授权

解决：

```text
你能否访问这个 Workspace？
```

通过：

```text
tenant_member
```

判断：

```text
tenant_id
+
user_id
+
member_status = ACTIVE
```

只有符合条件的用户才能访问对应 Workspace 业务。

---

## 7. 多租户隔离策略

CloudWork 采用三层隔离：

```text
身份认证
   ↓
租户成员授权
   ↓
tenant_id SQL 条件
```

### 第一层：身份认证

保证当前请求来自合法登录用户。

### 第二层：Tenant Service 授权

Project Service 不信任客户端直接提交的 `tenantId`。

收到请求后会通过 OpenFeign 调用 Tenant Service。

Tenant Service 使用当前认证用户查询：

```text
tenant_member
```

判断其是否是目标 Workspace 的 ACTIVE 成员。

### 第三层：SQL 隔离

即使业务层已经完成权限判断，Project / Task SQL 仍然必须携带：

```sql
tenant_id = ?
```

例如 Project 查询：

```sql
WHERE tenant_id = ?
  AND project_id = ?
```

而不是：

```sql
WHERE project_id = ?
```

Task 更新同样使用：

```sql
WHERE task_id = ?
  AND tenant_id = ?
```

形成双层防护。

---

## 8. 为什么不用 RuoYi DataScope 做 Tenant 隔离

RuoYi DataScope 主要用于：

- 部门范围
- 组织范围
- 平台角色数据权限

CloudWork 的 Tenant 属于 SaaS 业务边界。

两者概念不同：

```text
平台 RBAC
≠
SaaS Tenant Role
```

因此 CloudWork 单独建立：

```text
tenant
tenant_role
tenant_member
```

维护 Workspace 级权限。

避免把 SaaS 租户语义强行混入平台 DataScope。

---

## 9. 服务间调用

Project Service 通过 OpenFeign 调用 Tenant Service。

Feign Client：

```text
TenantAccessRemoteService
```

目标服务：

```text
cloudwork-tenant
```

调用：

```http
GET /internal/access/{tenantId}
```

当前用户身份不会通过请求参数传递：

```text
userId
```

而是继续由认证上下文获取：

```java
SecurityUtils.getUserId();
```

从而避免客户端伪造用户身份。

---

## 10. 用户身份透传

Gateway 完成 JWT 与 Redis 登录状态校验后，向下游服务透传用户身份。

下游服务通过安全组件恢复：

```text
SecurityContext
```

Feign 调用继续复用项目中的请求拦截器，将必要认证上下文传递给下游服务。

因此调用链：

```text
Gateway
↓
Project
↓
Feign
↓
Tenant
```

能够保持同一个当前登录用户身份。

---

## 11. 数据库边界

CloudWork 当前主要业务数据库：

```text
cloudwork_tenant
cloudwork_project
```

Tenant Service 只访问：

```text
cloudwork_tenant
```

Project Service 只访问：

```text
cloudwork_project
```

Project Service 不直接查询：

```text
tenant_member
```

而是通过 Tenant Service API 完成权限判断。

这样可以保持服务的数据所有权边界。

---

## 12. 为什么不使用跨服务物理外键

例如 Project 表中的：

```text
tenant_id
```

在逻辑上指向 Tenant Service。

但是不建立：

```text
project.tenant_id
→
tenant.tenant_id
```

物理外键。

原因是：

- 两张表属于不同服务
- 服务未来可能独立扩容
- 数据库可能独立部署
- 跨服务数据库外键会破坏服务自治
- 数据库结构变化会产生强耦合

因此跨服务一致性通过：

- API 校验
- 业务规则
- tenant_id
- 服务边界

维护。

---

## 13. 事务策略

当前主要事务场景是 Workspace 创建。

一次 Workspace 创建需要写入：

```text
tenant
↓
tenant_role OWNER
tenant_role ADMIN
tenant_role MEMBER
↓
tenant_member
```

这些数据全部属于 Tenant Service 的同一个数据库。

因此直接使用：

```java
@Transactional(rollbackFor = Exception.class)
```

即可完成事务一致性。

Testcontainers 集成测试验证了：

```text
TenantMember 插入失败
↓
Tenant 回滚
Role 回滚
Member 回滚
```

当前核心业务暂时不存在必须跨多个服务同时提交的强一致操作，因此没有引入 Seata。

---

## 14. 配置与服务发现

CloudWork 使用 Nacos：

```text
Nacos Config
+
Nacos Discovery
```

完成配置管理与服务注册发现。

主要配置：

```text
application-dev.yml
cloudwork-tenant-dev.yml
cloudwork-project-dev.yml
ruoyi-gateway-dev.yml
```

服务启动后注册到 Nacos。

Project Service 通过：

```text
cloudwork-tenant
```

服务名进行 Feign 调用，而不是硬编码 IP 地址。

---

## 15. Gateway 路由

Tenant：

```text
/tenant/**
↓
lb://cloudwork-tenant
↓
StripPrefix=1
```

Project：

```text
/project/**
↓
lb://cloudwork-project
↓
StripPrefix=1
```

例如：

```http
GET /tenant/workspaces/mine
```

经过 Gateway 后：

```http
GET /workspaces/mine
```

发送给 Tenant Service。

---

## 16. 前端架构

前端基于：

- Vue 3
- Vue Router
- Element Plus
- Vite

CloudWork 页面主要包括：

```text
Dashboard
Workspace
Project
Task
```

API 封装：

```text
src/api/cloudwork/tenant.js
src/api/cloudwork/project.js
```

统一复用：

```text
@/utils/request
```

避免页面直接创建新的 HTTP Client。

---

## 17. 核心业务闭环

当前已经完成真实浏览器验证：

```text
登录
 ↓
Dashboard
 ↓
Workspace
 ↓
Projects
 ↓
Create Project
 ↓
Open Tasks
 ↓
Create Task
 ↓
TODO
 ↓
Start
 ↓
DOING
 ↓
Complete
 ↓
DONE
```

---

## 18. 安全验证

已验证跨租户 Project 创建。

当前用户属于：

```text
tenantId = 1
```

尝试：

```text
tenantId = 999999
```

创建 Project：

```text
403
```

数据库检查：

```sql
SELECT *
FROM project
WHERE tenant_id = 999999;
```

结果：

```text
Empty set
```

说明请求不仅被拒绝，而且没有产生脏数据。

Task 使用错误 tenantId 更新同样返回：

```text
403
```

且原任务状态保持不变。

---

## 19. 实际故障排查案例

曾出现：

```text
Project 创建返回 403
```

Tenant 权限接口返回：

```text
roleCode = null
accessible = false
```

数据库手工查询却存在正确 OWNER 成员记录。

开启 MyBatis Debug 后：

```text
Parameters: 1(Long), 1(Long), 1(Integer)
Total: 1
```

说明 SQL 已成功查询到记录。

最终定位：

```text
数据库字段
tenant_id
user_id
role_code
```

没有正确映射：

```text
Java VO
tenantId
userId
roleCode
```

修复：

```sql
m.tenant_id AS tenantId,
m.user_id AS userId,
r.role_code AS roleCode
```

之后权限链恢复：

```text
roleCode = OWNER
accessible = true
```

该问题体现了从：

```text
网关
→ 认证
→ 服务
→ Mapper
→ SQL
→ 数据库
→ ORM 映射
```

逐层缩小故障范围的排查方式。

---

## 20. 当前架构取舍

CloudWork 当前没有引入：

- Kafka
- Elasticsearch
- MinIO
- Seata
- Prometheus
- Grafana

原因不是这些技术没有价值，而是当前核心业务并不需要为了展示技术栈而强行引入额外分布式复杂度。

当前阶段优先保证：

```text
业务完整
+
服务边界清晰
+
租户隔离可靠
+
系统可以真实运行
+
功能可以真实演示
```

未来业务规模增长后，再根据实际需求引入相应基础设施。

---

## 21. 后续演进方向

如果系统继续扩展，可以演进为：

```text
Tenant Service
Project Service
Task Service
File Service
Notification Service
Search Service
```

同时根据业务需要逐步引入：

```text
Kafka
Elasticsearch
MinIO
Prometheus
Grafana
CI/CD
Kubernetes
```

而不是一次性将所有中间件加入核心链路。