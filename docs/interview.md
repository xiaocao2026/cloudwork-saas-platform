# CloudWork 面试准备

## 1. 一分钟项目介绍

CloudWork 是我基于 Spring Cloud 和 Vue 3 开发的一个多租户 SaaS 协作平台。

项目以 Workspace 作为租户边界，用户可以在 Workspace 下创建 Project，并在 Project 中创建和管理 Task，任务支持：

```text
TODO → DOING → DONE
```

后端采用微服务架构，主要包含：

- Gateway
- Auth
- System
- Tenant Service
- Project Service

其中 Tenant Service 负责 Workspace、租户角色和成员关系，Project Service 负责 Project 和 Task。

我在项目中重点实现了：

- JWT + Redis 登录会话
- Spring Cloud Gateway
- Nacos 配置中心与服务发现
- OpenFeign 服务间调用
- SaaS 多租户权限校验
- tenant_id 数据隔离
- 本地事务
- Testcontainers 集成测试
- Docker 开发环境
- Vue 3 完整业务页面

项目最核心的设计是：

```text
平台身份认证
+
Workspace 成员授权
+
tenant_id SQL 隔离
```

防止用户通过修改请求中的 tenantId 越权访问其他 Workspace 数据。

---

## 2. 为什么做这个项目

我的其他项目已经覆盖了：

- 单体 Web 系统
- Redis
- Kafka
- AI Agent
- RAG
- 向量数据库

所以 CloudWork 主要希望补充：

```text
微服务
+
SaaS 多租户
+
服务治理
+
跨服务调用
+
容器化
```

这类工程能力。

因此没有继续做普通博客或 AI 功能，而是选择了企业 SaaS 协作场景。

---

## 3. 项目的核心业务是什么

核心业务关系：

```text
Workspace
   ↓
Project
   ↓
Task
```

Workspace 是 Tenant。

一个用户可以属于多个 Workspace。

每个 Workspace 拥有自己的：

```text
Project
Task
Role
Member
```

Task 当前支持：

```text
TODO
↓
DOING
↓
DONE
```

---

## 4. 为什么需要 Tenant Service

如果把 Tenant 逻辑散落在各个业务服务中，例如：

```text
Project Service 自己维护成员关系
Task Service 自己维护成员关系
File Service 自己维护成员关系
```

会导致：

- 权限逻辑重复
- 数据模型重复
- 不同服务权限规则不一致
- Tenant 逻辑难以统一维护

所以单独设计：

```text
cloudwork-tenant
```

负责：

```text
Workspace
Tenant Role
Tenant Member
Tenant Access
```

其他业务服务只需要调用 Tenant Service 判断：

```text
当前用户能不能访问这个 Workspace
```

---

## 5. 为什么 Project 和 Task 没拆成两个微服务

当前设计：

```text
cloudwork-project
├── Project
└── Task
```

主要原因是 Project 和 Task 属于同一个协作领域，并且当前业务规模不大。

如果拆成两个服务，会额外增加：

- 服务注册
- OpenFeign 调用
- 网络失败
- 部署成本
- 分布式事务问题
- 运维复杂度

而当前业务收益有限。

所以采用：

```text
先保证领域内聚
再根据业务规模拆分
```

如果未来 Task 增加：

- 评论
- 附件
- 工作流
- SLA
- 自动化规则
- 大规模任务调度

再考虑独立拆成 Task Service。

---

## 6. 多租户是怎么做的

CloudWork 的多租户不是简单在前端传一个 tenantId。

采用三层控制：

```text
第一层：平台认证
第二层：Tenant 成员授权
第三层：SQL tenant_id 隔离
```

### 第一层：平台认证

Gateway 校验：

```text
JWT
+
Redis Login Session
```

确定：

```text
当前用户是谁
```

---

### 第二层：Tenant 授权

Project Service 收到：

```text
tenantId
```

后，不直接相信客户端。

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

查询当前用户是否是该 Workspace 的 ACTIVE 成员。

---

### 第三层：SQL 隔离

即使已经完成权限验证，SQL 仍然必须：

```sql
WHERE tenant_id = ?
```

例如：

```sql
WHERE project_id = ?
AND tenant_id = ?
```

不能只根据：

```sql
WHERE project_id = ?
```

查询。

---

## 7. 为什么需要三层隔离

因为任何单层都可能存在风险。

例如只依赖前端：

```text
tenantId = 1
```

攻击者可以直接改成：

```text
tenantId = 2
```

如果只做 Service 层验证，但 Mapper SQL：

```sql
UPDATE task
SET status = ?
WHERE task_id = ?
```

一旦业务代码未来出现遗漏，就可能出现越权。

因此采用：

```text
Service 授权
+
SQL tenant_id
```

形成双重保护。

---

## 8. tenantId 是不是用户提交的

是。

例如创建 Project：

```json
{
  "tenantId": 1,
  "projectName": "CloudWork"
}
```

但是：

```text
客户端提交 tenantId
≠
客户端拥有 tenantId 权限
```

服务端会重新验证：

```text
SecurityUtils.getUserId()
+
tenantId
+
tenant_member
```

确认真实权限。

---

## 9. userId 为什么不能由客户端提交

如果接口允许：

```json
{
  "tenantId": 1,
  "userId": 2
}
```

攻击者可能伪造：

```text
userId
```

所以当前用户 ID 必须来自：

```java
SecurityUtils.getUserId();
```

而不是 Request Body 或 Query Parameter。

这样用户无法伪造其他人的身份。

---

## 10. Gateway 在项目中做什么

Gateway 主要职责：

- 统一入口
- JWT 校验
- Redis 登录状态校验
- 请求路由
- 用户身份透传

请求流程：

```text
Browser
↓
Gateway
↓
JWT
↓
Redis
↓
Route
↓
Business Service
```

例如：

```text
/project/**
```

路由到：

```text
cloudwork-project
```

---

## 11. 为什么还需要 Redis

JWT 本身可以携带身份信息，但 CloudWork 当前认证体系还使用 Redis 保存登录会话。

Redis 可以用于：

- 判断 Token 对应登录状态是否仍有效
- 主动退出登录
- 控制会话有效期
- 获取 LoginUser

因此不是完全无状态 JWT。

---

## 12. JWT 为什么没有直接作为唯一登录状态

纯 JWT 有一个常见问题：

```text
Token 签发以后
在过期之前很难主动失效
```

结合 Redis 后：

```text
JWT
+
Redis Session
```

可以在服务端控制：

```text
登出
会话失效
有效期
```

---

## 13. Nacos 在项目里做什么

CloudWork 使用 Nacos 两个能力：

```text
Nacos Config
+
Nacos Discovery
```

### Config

保存：

```text
cloudwork-tenant-dev.yml
cloudwork-project-dev.yml
ruoyi-gateway-dev.yml
```

等配置。

### Discovery

服务启动后注册到 Nacos。

例如：

```text
cloudwork-project
```

通过服务名调用：

```text
cloudwork-tenant
```

不用硬编码：

```text
192.168.x.x:9202
```

---

## 14. OpenFeign 在哪里使用

主要调用链：

```text
Project Service
↓
OpenFeign
↓
Tenant Service
```

Project Service 需要验证 Workspace 权限时：

```text
TenantAccessRemoteService
```

调用：

```http
GET /internal/access/{tenantId}
```

Tenant Service 返回当前用户是否：

```text
accessible = true
```

---

## 15. Feign 如何知道当前用户是谁

用户最初请求经过 Gateway。

Gateway 完成认证以后，下游安全组件恢复当前用户上下文。

Feign 调用继续通过项目已有的请求拦截机制透传必要认证信息。

因此：

```text
Browser
↓
Gateway
↓
Project
↓
Feign
↓
Tenant
```

整个调用链对应的仍是同一个登录用户。

---

## 16. 为什么 Project Service 不直接查询 tenant_member

因为：

```text
tenant_member
```

属于 Tenant Service 的数据。

如果 Project Service 直接连接：

```text
cloudwork_tenant
```

会造成：

```text
跨服务数据库访问
```

最终导致：

- 数据所有权混乱
- Schema 强耦合
- 服务无法独立演进
- 服务边界失去意义

所以采用：

```text
API / Feign
```

而不是：

```text
跨库 SELECT
```

---

## 17. 为什么不用数据库外键

主要是服务自治。

例如：

```text
project.tenant_id
```

逻辑上关联：

```text
tenant.tenant_id
```

但两张表属于不同服务数据库。

如果使用物理外键：

```text
Project DB
→
Tenant DB
```

会导致服务无法真正独立。

所以通过：

- Tenant API
- Service 校验
- SQL tenant_id
- 唯一索引

保证业务关系。

---

## 18. Task 为什么也保存 tenant_id

理论上：

```text
Task
→
Project
→
Tenant
```

可以间接找到 tenantId。

但 Task 中仍然保存：

```text
tenant_id
```

原因：

- 查询可以直接按租户过滤
- Update 可以直接增加 tenant 条件
- 减少 JOIN
- 安全边界更加直观
- 方便未来按 Tenant 分区

属于有业务意义的冗余字段。

---

## 19. Workspace 创建怎么保证事务

一次 Workspace 创建会写：

```text
tenant
↓
OWNER Role
ADMIN Role
MEMBER Role
↓
Creator Member
```

使用：

```java
@Transactional(rollbackFor = Exception.class)
```

保证本地事务。

如果最后：

```text
tenant_member
```

插入失败：

前面的：

```text
Tenant
Role
```

全部回滚。

---

## 20. 为什么没有用 Seata

因为当前核心强一致事务都发生在：

```text
同一个 Service
+
同一个 Database
```

例如 Workspace 创建全部发生在 Tenant DB。

目前没有：

```text
Tenant DB
+
Project DB
```

必须同时提交或同时回滚的核心操作。

如果为了展示技术强行加入 Seata，会增加：

- TC
- RM
- 全局事务
- 锁
- 故障场景

却没有实际业务收益。

因此当前不引入。

---

## 21. 为什么不用 Kafka

当前核心流程：

```text
Create Workspace
Create Project
Create Task
Update Task Status
```

都是同步核心操作。

目前没有真正需要：

```text
削峰
异步通知
事件广播
最终一致性
```

的必要业务。

未来如果增加：

```text
Task 创建后发送通知
Task DONE 后产生 Activity Event
Project 创建后写审计日志
```

可以使用 Kafka。

当前不为了堆技术栈强行增加。

---

## 22. 项目有没有真正做过安全测试

有。

当前用户属于：

```text
tenantId = 1
```

实际发送：

```text
tenantId = 999999
```

创建 Project。

服务返回：

```text
403
```

随后数据库检查：

```sql
SELECT *
FROM project
WHERE tenant_id = 999999;
```

返回：

```text
Empty set
```

说明请求不仅失败，而且没有产生脏数据。

---

## 23. Task 越权怎么验证

已有：

```text
taskId = 1
tenantId = 1
status = DONE
```

使用：

```text
tenantId = 999999
status = DOING
```

调用状态更新接口。

返回：

```text
403
```

重新查询：

```text
status = DONE
```

证明原数据没有被越权修改。

---

## 24. 项目里遇到最有代表性的 Bug

最典型的是 Tenant Access 查询问题。

现象：

```text
Project 创建一直 403
```

Tenant Access 返回：

```text
roleCode = null
accessible = false
```

但是数据库中明明存在：

```text
tenant_id = 1
user_id = 1
member_status = 1
role_code = OWNER
```

---

## 25. 这个 Bug 是怎么排查的

没有直接修改代码，而是逐层验证。

第一层：

```text
JWT
```

正常。

第二层：

```text
Gateway
```

正常。

第三层：

```text
SecurityUtils.getUserId()
```

返回：

```text
1
```

正常。

第四层：

直接执行数据库 SQL。

返回：

```text
OWNER
```

正常。

第五层：

开启 MyBatis Debug。

发现：

```text
Parameters:
1(Long), 1(Long), 1(Integer)

Total:
1
```

说明 SQL 已经查询到一条数据。

但 Java 返回：

```text
null
```

于是问题从：

```text
SQL
```

缩小到了：

```text
Result Mapping
```

最终发现：

```text
tenant_id
user_id
role_code
```

没有正确映射：

```text
tenantId
userId
roleCode
```

最后修改为：

```sql
m.tenant_id AS tenantId,
m.user_id AS userId,
r.role_code AS roleCode
```

问题解决。

---

## 26. 从这个 Bug 学到了什么

排查复杂系统问题时，不应该直接猜代码。

更有效的方式是：

```text
网络层
↓
网关层
↓
认证层
↓
业务层
↓
Mapper
↓
SQL
↓
数据库
↓
ORM Mapping
```

逐层建立：

```text
已知正确边界
```

最终缩小故障范围。

这比一开始就不断修改代码更可靠。

---

## 27. 项目自动化测试做了什么

Tenant Service 使用：

```text
Testcontainers
+
MySQL 8 Container
```

进行集成测试。

覆盖：

- Workspace 创建
- 三个默认 Role 创建
- Creator 成为 OWNER
- listMine
- 非成员访问
- 非 ACTIVE 成员访问
- Transaction Rollback

---

## 28. 为什么用 Testcontainers

如果测试直接使用开发数据库：

```text
cloudwork_tenant
```

可能出现：

- 污染本地数据
- 测试依赖已有数据
- 测试结果不可重复
- 开发环境差异

Testcontainers 每次提供独立 MySQL 环境。

测试结束后容器销毁。

更接近真实数据库行为，也不会污染开发库。

---

## 29. 为什么不使用 H2 做测试

因为生产环境使用：

```text
MySQL
```

H2 和 MySQL 在：

- SQL 方言
- 自增
- 类型
- 索引
- DDL

等方面存在差异。

Testcontainers 使用真实 MySQL，可以提高测试可信度。

---

## 30. 系统是否支持真正多租户数据库隔离

当前属于：

```text
Shared Database
Shared Schema
tenant_id Isolation
```

也就是：

```text
多个 Tenant
共享业务数据库和表
```

通过：

```text
tenant_id
```

区分数据。

不是：

```text
Database per Tenant
```

也不是：

```text
Schema per Tenant
```

---

## 31. 为什么选择 tenant_id 模式

对于当前项目规模：

```text
Shared DB + tenant_id
```

优点：

- 实现简单
- 成本低
- 运维简单
- 查询方便
- 适合中小规模 SaaS

缺点：

- SQL 必须严格 tenant scoped
- 隔离依赖应用层设计

因此 CloudWork 又增加：

```text
Tenant Access API
+
SQL tenant_id
```

降低越权风险。

---

## 32. 如果客户要求更强隔离怎么办

可以升级为：

```text
Database per Tenant
```

或者：

```text
Schema per Tenant
```

例如大型企业客户使用独立 Database。

普通客户继续 Shared Database。

可以形成：

```text
Hybrid Multi-Tenancy
```

但当前项目没有实现。

---

## 33. 有没有缓存

Redis 当前主要用于平台：

```text
Login Session
```

CloudWork Tenant / Project 业务数据暂时没有增加业务缓存。

原因：

当前数据规模较小。

如果一开始缓存：

```text
Workspace
Project
Task
```

还要额外考虑：

```text
缓存一致性
缓存失效
Tenant Cache Key
```

当前收益有限。

---

## 34. 如果以后缓存 Project 怎么设计

Key 必须带 tenantId。

例如：

```text
cloudwork:project:{tenantId}:{projectId}
```

不能：

```text
cloudwork:project:{projectId}
```

这样可以进一步减少不同 Tenant 缓存 Key 冲突与错误复用风险。

---

## 35. 为什么用了乐观锁 version 字段但现在没有大量使用

当前数据模型预留：

```text
version
```

主要是为未来并发更新准备。

当前 MVP 的更新操作比较简单，还没有引入完整：

```text
UPDATE ...
WHERE version = ?
```

的乐观锁流程。

这是扩展点，不应该把它描述成当前已经完整实现的能力。

---

## 36. 这个项目最大的工程亮点是什么

如果只能选一个：

```text
微服务环境下的 SaaS 多租户安全链
```

即：

```text
JWT / Redis Authentication
↓
Gateway
↓
Project Service
↓
Feign
↓
Tenant Service
↓
tenant_member
↓
tenant_id SQL Isolation
```

不是简单：

```text
WHERE tenant_id = ?
```

而是把：

```text
身份认证
租户授权
数据隔离
```

分开设计。

---

## 37. 第二个工程亮点是什么

服务边界。

例如：

```text
Project Service
```

不能直接查询：

```text
Tenant DB
```

需要：

```text
Feign
```

调用 Tenant Service。

这样才能真正体现：

```text
微服务数据所有权
```

而不是多个 Spring Boot 项目共用一套数据库。

---

## 38. 第三个工程亮点是什么

测试与真实联调。

不仅写代码，还实际验证了：

```text
Maven Build
Testcontainers
Gateway
JWT
Feign
Nacos
MySQL
Frontend
```

并完成：

```text
Workspace
→
Project
→
Task
→
TODO
→
DOING
→
DONE
```

完整业务闭环。

---

## 39. 项目的不足是什么

当前不足包括：

- Tenant 成员邀请未实现
- 自定义 Tenant Role 未实现
- Project 成员体系较简单
- Task Assignee 流程尚未完整
- 没有消息通知
- 没有全文搜索
- 没有完整可观测性
- 没有完整生产 Kubernetes 验证
- Project Service 自动化测试覆盖还可以继续增加

这些属于下一阶段演进，而不是隐藏项目不足。

---

## 40. 如果再给一周时间最先做什么

第一优先：

```text
Tenant Member Invitation
+
Project Member
```

让协作业务更完整。

第二优先：

```text
Kafka Notification
```

Task 创建或状态变化产生领域事件。

第三优先：

```text
Prometheus
+
Grafana
```

增加微服务可观测性。

第四优先：

```text
CI/CD
+
Kubernetes
```

完善工程交付链。

---

## 41. 为什么项目是基于 RuoYi-Cloud

项目不是从零重复造：

```text
用户管理
菜单系统
登录系统
```

而是利用成熟微服务脚手架作为平台基础。

自己的重点放在：

```text
SaaS Tenant
Project
Task
Tenant Authorization
Service Boundary
Tenant Isolation
```

同时保留：

```text
MIT License
UPSTREAM.md
```

明确上游来源。

---

## 42. 面试时如何说明基于开源项目

可以直接说：

CloudWork 的认证、网关和基础系统能力使用 RuoYi-Cloud 作为基础脚手架。我没有把这些基础框架功能描述成自己原创。

我主要完成的是 CloudWork SaaS 业务扩展，包括：

- Tenant Service
- Workspace 模型
- Tenant Role / Member
- Tenant Access
- Project Service
- Project / Task
- Feign 跨服务租户授权
- tenant_id 隔离
- Testcontainers
- CloudWork 前端页面

上游仓库和版本信息也保留在 `UPSTREAM.md`。

---

## 43. 面试官问“为什么不用单体”

不能回答：

```text
因为微服务高级
```

应该回答：

这个项目的主要目标之一就是学习并实践：

```text
Gateway
Service Discovery
Config Center
Feign
Service Boundary
```

所以主动采用微服务架构。

如果业务真实规模非常小，单体确实可能更简单。

微服务不是默认比单体更好，而是在：

```text
组织规模
业务边界
独立部署
扩缩容
技术复杂度
```

之间做权衡。

---

## 44. 面试官问“你这个项目微服务是不是拆太细”

回答：

没有继续拆 Task Service。

当前只有：

```text
Tenant
Project
```

两个 CloudWork 领域服务。

Project 和 Task 被保留在同一服务中，就是为了避免为了微服务而微服务。

我的拆分原则更偏向：

```text
业务边界
+
数据所有权
+
领域内聚
```

而不是：

```text
一张表一个服务
```

---

## 45. 面试官问“Feign 挂了怎么办”

当前 MVP 采用同步 Feign。

如果 Tenant Service 不可用，Project 权限无法可靠判断，因此应该：

```text
Fail Closed
```

也就是拒绝 Project 操作，而不是绕过权限继续执行。

对于权限服务：

```text
不可确认权限
≈
不允许访问
```

比 Fail Open 更安全。

未来可以：

- 设置合理超时
- 限制重试
- 熔断
- 监控 Tenant Service
- 缓存短期 Tenant Membership

但权限缓存要非常谨慎处理失效。

---

## 46. 面试官问“为什么内部接口还能经过用户认证”

因为 Project Service 需要验证：

```text
当前这个真实登录用户
```

是否属于 Tenant。

所以 Feign 调用必须继续携带当前用户上下文。

内部 API 不是：

```text
Project Service 自己告诉 Tenant
userId = xxx
```

而是 Tenant Service 自己根据认证上下文：

```java
SecurityUtils.getUserId();
```

获得当前用户。

---

## 47. 面试官问“如果 Project Service 被攻击，能不能伪造身份”

在当前系统边界中，服务间身份仍主要依赖已有认证上下文透传。

如果进一步做生产级 Zero Trust，可以增加：

- mTLS
- Service Identity
- Internal Token
- Service Mesh
- 内部接口访问控制

当前项目没有实现这些能力。

---

## 48. 面试官问“如果 tenantId 不加到 Task 表行不行”

可以。

可以：

```text
task.project_id
↓
project.tenant_id
```

间接判断。

但当前设计保留 `task.tenant_id`，主要为了：

- 查询简单
- 更新条件直接
- Tenant 边界明确
- 避免额外 JOIN
- 方便未来分区

属于空间换查询与安全清晰度。

---

## 49. 面试官问“有没有出现 N+1”

当前 Project / Task 查询以简单列表为主，没有复杂对象图，也没有循环中逐条查询大量关联对象。

因此当前核心路径没有明显的 ORM N+1 问题。

项目使用 MyBatis，也可以直接控制 SQL。

---

## 50. 面试官问“为什么 MyBatis 不用 MyBatis-Plus”

当前基础项目已经采用 MyBatis XML。

为了：

- 保持现有技术体系一致
- 明确控制 tenant_id SQL
- 避免为了少量 CRUD 再引入框架

继续使用 MyBatis XML。

对于多租户场景，显式 SQL 也更方便面试时说明：

```text
每一条敏感 SQL
如何包含 tenant_id
```

---

## 51. 面试官问“高并发怎么办”

当前项目没有宣称已经经过大规模压测。

如果请求量增加，可以从以下方向演进：

```text
Gateway 水平扩容
Service 水平扩容
Redis
DB Index
Read Replica
Cache
MQ
Database Sharding
```

但应该基于具体瓶颈进行优化，而不是提前假设所有组件都是瓶颈。

---

## 52. 面试官问“你做过压测吗”

如果没有做，就直接回答：

当前版本重点验证的是业务正确性、租户安全和微服务调用链，没有完成系统性的性能压测。

不要虚构 QPS。

可以补充：

如果继续优化，我会使用：

```text
JMeter
或
k6
```

针对：

- Gateway
- Project List
- Task List
- Tenant Access

进行基准测试，再根据指标定位瓶颈。

---

## 53. 面试官问“你最满意的地方是什么”

可以回答：

我比较满意的不是用了多少中间件，而是租户权限这条链真正做了完整验证。

从：

```text
JWT
Gateway
Feign
Tenant Member
SQL tenant_id
```

到非法 Tenant 的实际攻击测试都跑过。

并且开发中真的遇到过 ORM Mapping 问题，通过 MyBatis Debug 把问题逐层定位出来。

所以这个项目对我最大的价值是：

```text
从写功能
提升到理解系统调用链和安全边界
```

---

## 54. 面试官问“如果让你重新设计，会改什么”

可以回答：

第一，Tenant Access 可以抽象成更加统一的内部授权 SDK 或 starter，避免每个业务服务重复写。

第二，会给 Project Service 增加完整 Testcontainers 集成测试。

第三，会完善成员邀请和 Project Member，使协作场景更完整。

第四，在有真实异步事件后再引入 Kafka。

第五，增加 Prometheus / Grafana 与 CI/CD，使工程交付更完整。

---

## 55. 简历上不要写的内容

没有完成就不要写：

```text
百万 QPS
高并发系统
生产级 K8s 集群
Kafka 最终一致性
Elasticsearch 搜索
MinIO
Seata 分布式事务
Prometheus
Grafana
完整 CI/CD
```

---

## 56. 简历上可以写的内容

可以写：

```text
Spring Boot 3
Spring Cloud
Spring Cloud Gateway
Nacos
OpenFeign
MyBatis
MySQL
Redis
JWT
Docker
Vue 3
Testcontainers
多租户 SaaS
Tenant Isolation
```

这些都有实际项目依据。

---

## 57. 项目讲解推荐顺序

面试时不要从：

```text
我用了 Spring Boot...
```

开始。

推荐：

### 第一段

先说业务：

```text
这是一个多租户 SaaS 协作平台。
```

### 第二段

说架构：

```text
Gateway
Tenant
Project
Nacos
Redis
MySQL
```

### 第三段

重点说：

```text
租户隔离
```

### 第四段

说：

```text
Feign 服务边界
```

### 第五段

说：

```text
事务 + Testcontainers
```

### 第六段

讲：

```text
MyBatis Mapping Bug
```

这样比单纯罗列技术栈更容易体现工程能力。

---

## 58. 三分钟项目介绍模板

CloudWork 是我做的一个多租户 SaaS 协作平台，业务上以 Workspace 为租户边界，一个用户可以属于多个 Workspace，在 Workspace 中可以创建 Project，并在 Project 下管理 Task，Task 支持 TODO、DOING、DONE 的状态流转。

后端基于 Spring Cloud，入口使用 Gateway，认证使用 JWT 和 Redis 会话，Nacos 用于配置管理和服务发现。我自己主要实现了 Tenant Service 和 Project Service。

Tenant Service 管理 Workspace、Tenant Role 和 Tenant Member；Project Service 管理 Project 和 Task。

项目中我比较关注的是多租户隔离。客户端虽然会传 tenantId，但后端不会直接相信这个参数。Project Service 会先通过 OpenFeign 调 Tenant Service，由 Tenant Service 根据 SecurityContext 中当前真实 userId 查询 tenant_member，确认这个用户是不是目标 Workspace 的 ACTIVE 成员。验证通过以后，Project 和 Task 的 SQL 还会继续增加 tenant_id 条件，形成权限校验和数据层隔离两层保护。

另外 Workspace 创建涉及 Tenant、三个默认 Role 和 Creator Member 多次写入，我使用本地事务保证原子性，并用 Testcontainers 的 MySQL 集成测试验证了成功创建和异常回滚。

开发过程中还遇到过一个比较典型的问题：Tenant 权限 SQL 的 MyBatis 日志显示 Total=1，但最终 VO 却是 null，导致 Project 一直 403。我从 Gateway、JWT、SecurityContext、Mapper 参数、数据库 SQL 一层层排查，最后发现是 tenant_id、user_id、role_code 没有正确映射到 Java 的驼峰字段，通过 SQL Alias 修复。

前端使用 Vue 3 和 Element Plus，目前已经完整跑通 Workspace → Project → Task → TODO → DOING → DONE 的业务链。