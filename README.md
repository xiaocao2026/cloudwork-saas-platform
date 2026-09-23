# CloudWork

CloudWork 是一个基于 Spring Cloud 与 Vue 3 构建的多租户 SaaS 协作平台。

平台以 **Workspace（工作空间）** 作为租户边界，提供 Project（项目）与 Task（任务）协作能力，重点实现了微服务环境下的统一认证、API 网关、服务发现、配置中心、服务间调用、租户权限校验、数据隔离以及完整的前端业务闭环。

当前核心业务流程：

```text
Workspace
   ↓
Project
   ↓
Task
   ↓
TODO → DOING → DONE
```

---

## 一、项目功能

### 1. 多租户 Workspace

支持：

- 创建 Workspace
- 查询当前用户可访问的 Workspace
- 查看 Workspace 详情
- Workspace 内置角色：
    - OWNER
    - ADMIN
    - MEMBER
- Workspace 创建者自动成为 ACTIVE 状态的 OWNER
- 基于 `tenant_member` 维护用户与租户之间的成员关系

### 2. Project 项目管理

支持：

- 在指定 Workspace 下创建 Project
- 按 Workspace 查询 Project 列表
- 查询 Project 详情
- 服务端自动生成 Project Code
- Project 访问前执行 Workspace 成员权限校验
- Project 数据通过 `tenant_id` 进行租户隔离

### 3. Task 任务协作

支持：

- 在指定 Project 下创建 Task
- 按 Workspace + Project 查询 Task
- Task 优先级：
    - LOW
    - MEDIUM
    - HIGH
- Task 状态流转：

```text
TODO → DOING → DONE
```

- 前端提供 TODO / DOING / DONE 三列任务看板
- 状态修改成功后自动刷新任务列表

---

## 二、多租户隔离设计

CloudWork 没有仅依赖前端传递的 `tenantId` 判断权限，而是采用多层租户隔离策略：

```text
平台身份认证
    +
Workspace 成员授权
    +
tenant_id SQL 数据隔离
```

Project Service 不会直接访问 Tenant Service 的数据库。

当用户访问 Project 或 Task 时：

```text
当前登录用户
    ↓
Project Service
    ↓
OpenFeign
    ↓
Tenant Service
    ↓
tenant_member
    ↓
验证是否为 ACTIVE 成员
```

只有验证成功后，Project Service 才会继续执行对应业务。

同时 Project 和 Task 的 SQL 查询、更新操作继续携带 `tenant_id` 条件，从数据库访问层进一步防止跨租户访问。

因此，仅修改客户端请求中的 `tenantId` 无法越权访问其他 Workspace 的数据。

---

## 三、系统架构

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

## 四、技术栈

### 后端

- Java 21
- Spring Boot 3
- Spring Cloud
- Spring Cloud Alibaba
- Spring Cloud Gateway
- Nacos Config
- Nacos Discovery
- OpenFeign
- MyBatis
- MySQL 8
- Redis
- Maven
- Testcontainers

### 前端

- Vue 3
- Vue Router
- Element Plus
- Vite
- Yarn

### 基础设施

- Docker
- Docker Compose
- Kubernetes manifests
- Git
- GitHub

---

## 五、服务划分

| 服务 | 端口 | 主要职责 |
| --- | ---: | --- |
| Gateway | 8080 | API 网关、请求路由、认证过滤 |
| Auth | 9200 | 用户认证 |
| System | 9201 | 用户与系统基础管理 |
| Tenant | 9202 | Workspace、成员关系、租户权限校验 |
| Project | 9203 | Project 与 Task 协作业务 |

其中 Project 与 Task 当前放在同一个微服务中。

这是因为二者属于同一协作领域，业务关联较强。在当前项目规模下保持在一个服务内，可以减少不必要的分布式复杂度。

---

## 六、认证流程

平台登录认证基于 JWT 与 Redis 登录会话。

整体流程：

```text
客户端
  ↓
Authorization: Bearer <JWT>
  ↓
Gateway
  ↓
校验登录状态
  ↓
解析用户身份
  ↓
向下游服务传递用户信息
  ↓
SecurityContext
```

业务服务可以通过：

```java
SecurityUtils.getUserId();
```

获取当前登录用户 ID。

Gateway 负责平台层面的身份认证，而具体 Workspace 是否允许访问，则由 Tenant Service 进一步判断。

---

## 七、租户授权流程

Project Service 在执行 Project / Task 操作前，会通过 OpenFeign 请求 Tenant Service。

流程：

```text
客户端
   ↓
Gateway
   ↓
Project Service
   ↓
SecurityUtils.getUserId()
   ↓
OpenFeign
   ↓
Tenant Service
   ↓
tenant_member
   ↓
是否为 ACTIVE 成员？
   ├── 是 → 继续执行
   └── 否 → 403
```

内部权限校验接口：

```http
GET /internal/access/{tenantId}
```

该接口不会接收客户端传递的 `userId`。

用户身份仍然来自当前认证上下文：

```java
SecurityUtils.getUserId();
```

这样可以避免客户端伪造其他用户 ID。

---

## 八、数据库设计

CloudWork 当前将租户数据与协作业务数据拆分到不同数据库。

```text
cloudwork_tenant
├── tenant
├── tenant_role
└── tenant_member

cloudwork_project
├── project
└── task
```

### 为什么不使用跨服务物理外键

例如：

```text
project.tenant_id
```

在业务含义上属于 Tenant Service 中的租户，但 CloudWork 没有建立跨数据库物理外键。

服务之间通过：

- API
- OpenFeign
- 业务校验
- tenant_id 数据隔离

维持一致的业务边界。

这样可以避免 Project Service 与 Tenant Service 数据库结构产生强耦合。

---

## 九、Tenant 数据模型

### tenant

保存 Workspace 基本信息。

主要字段：

```text
tenant_id
tenant_code
tenant_name
status
created_by_user_id
create_time
update_time
version
```

### tenant_role

保存 Workspace 内角色。

当前内置：

```text
OWNER
ADMIN
MEMBER
```

### tenant_member

维护用户与 Workspace 之间的成员关系。

主要字段：

```text
member_id
tenant_id
user_id
role_id
member_status
joined_at
```

只有 ACTIVE 状态成员可以访问 Workspace 业务资源。

---

## 十、Project 数据模型

### project

主要字段：

```text
project_id
tenant_id
project_code
project_name
description
status
created_by_user_id
```

每个 Project 都必须属于一个 Workspace。

服务端自动生成：

```text
prj_<UUID>
```

形式的 Project Code。

---

## 十一、Task 数据模型

主要字段：

```text
task_id
tenant_id
project_id
title
description
status
priority
assignee_user_id
created_by_user_id
```

当前任务状态：

```text
TODO
DOING
DONE
```

任务状态流：

```text
TODO
  ↓
DOING
  ↓
DONE
```

当前版本没有实现任意状态回退，以保持核心业务流程简单明确。

---

## 十二、核心 API

### Workspace

```http
POST /tenant/workspaces

GET /tenant/workspaces/mine

GET /tenant/workspaces/{tenantId}
```

Tenant 内部授权：

```http
GET /tenant/internal/access/{tenantId}
```

### Project

```http
POST /project/projects

GET /project/projects?tenantId={tenantId}

GET /project/projects/{projectId}?tenantId={tenantId}
```

### Task

```http
POST /project/tasks

GET /project/tasks?tenantId={tenantId}&projectId={projectId}

PUT /project/tasks/{taskId}/status
```

---

## 十三、前端业务流程

当前前端已经实现完整业务演示流程：

```text
Dashboard
   ↓
Workspaces
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

Projects 页面支持：

- Workspace 选择
- Project 创建
- Project 查询
- Project 列表展示
- 跳转 Task 看板

Tasks 页面支持：

- Project 信息展示
- Task 创建
- Task 查询
- LOW / MEDIUM / HIGH 优先级
- TODO / DOING / DONE 三列看板
- TODO → DOING
- DOING → DONE

没有额外引入拖拽组件或其他前端依赖。

---

## 十四、本地开发环境

推荐环境：

```text
JDK 21
Maven 3.9+
Docker Desktop
Git
IntelliJ IDEA
```

当前 Windows 开发环境中，前端 Node.js / Yarn 不直接安装到宿主机，而是在 Docker 容器中运行。

---

## 十五、启动基础设施

项目根目录执行：

```powershell
docker compose -f .\backend\docker-compose-dev.yml up -d
```

当前 Docker 开发环境主要包含：

```text
MySQL
Redis
Nacos
Frontend
```

Java 微服务在开发阶段通过 IntelliJ IDEA 启动。

---

## 十六、初始化数据库

CloudWork Tenant：

```text
backend/sql/cloudwork_tenant_v1.sql
```

CloudWork Project：

```text
backend/sql/cloudwork_project_v1.sql
```

此外，还需要初始化现有认证与配置数据库。

---

## 十七、Nacos 配置

CloudWork 主要业务配置：

```text
cloudwork-tenant-dev.yml
cloudwork-project-dev.yml
```

Gateway 路由：

```text
/tenant/**
    ↓
cloudwork-tenant

/project/**
    ↓
cloudwork-project
```

业务服务同时通过 Nacos 完成服务注册与发现。

---

## 十八、后端启动顺序

建议：

```text
System
   ↓
Auth
   ↓
Tenant
   ↓
Project
   ↓
Gateway
```

主要启动类：

```text
RuoYiSystemApplication
RuoYiAuthApplication
CloudWorkTenantApplication
CloudWorkProjectApplication
RuoYiGatewayApplication
```

启动成功后，可以在 Nacos 服务列表中看到：

```text
ruoyi-system
ruoyi-auth
ruoyi-gateway
cloudwork-tenant
cloudwork-project
```

---

## 十九、前端启动

开发环境：

```text
http://localhost
```

生产构建验证：

```powershell
docker exec cloudwork-frontend yarn build:prod
```

当前版本已完成生产构建验证。

---

## 二十、自动化测试

Tenant Service 使用 Testcontainers 编写集成测试。

测试覆盖：

- Workspace 创建
- OWNER / ADMIN / MEMBER 默认角色创建
- 创建者自动成为 ACTIVE OWNER
- Workspace 创建事务回滚
- 非成员访问阻断
- 非 ACTIVE 成员访问阻断
- 当前用户 Workspace 查询

Testcontainers 使用独立 MySQL 容器运行，不污染本地开发数据库。

---

## 二十一、人工联调验证

Project / Task 已通过 Gateway 与真实登录 Token 完成全链路联调。

已验证：

```text
创建 Project
查询 Project 列表
查询 Project 详情

创建 Task
查询 Task

TODO → DOING
DOING → DONE
```

同时验证了多租户越权场景。

### 非法 tenantId 创建 Project

使用不存在或无权限的：

```json
{
  "tenantId": 999999,
  "projectName": "Illegal Project"
}
```

请求会返回：

```text
403
```

并且数据库中不会产生：

```text
tenant_id = 999999
```

的数据。

### 非法 tenantId 修改 Task

使用错误 tenantId 修改已有 Task：

```text
403
```

原任务数据保持不变。

---

## 二十二、事务设计

Workspace 创建过程中需要同时创建：

```text
Tenant
   ↓
OWNER
ADMIN
MEMBER
   ↓
Creator TenantMember
```

这些操作都在 Tenant Service 的本地事务中完成。

任一数据库写入失败时：

```text
整个 Workspace 创建事务回滚
```

当前核心业务没有必须跨多个微服务数据库同时提交的同步操作，因此暂未引入 Seata。

---

## 二十三、真实问题排查案例

开发过程中曾出现：

```text
Project 创建始终返回 403
```

但数据库中实际上存在：

```text
tenant_id = 1
user_id = 1
member_status = ACTIVE
role_code = OWNER
```

逐层排查：

```text
JWT                  ✅
Gateway              ✅
SecurityContext      ✅
userId               ✅
Tenant Controller    ✅
Mapper 参数          ✅
数据库记录           ✅
SQL 手工执行         ✅
```

开启 MyBatis Debug 日志后发现：

```text
Parameters: 1(Long), 1(Long), 1(Integer)
Total: 1
```

SQL 已经查询到一条数据，但返回结果：

```text
roleCode = null
accessible = false
```

最终定位到数据库字段：

```text
tenant_id
user_id
role_code
```

没有正确映射到 Java VO：

```text
tenantId
userId
roleCode
```

最终通过显式 SQL Alias：

```sql
m.tenant_id AS tenantId,
m.user_id AS userId,
r.role_code AS roleCode
```

修复。

修复后返回：

```text
tenantId = 1
userId = 1
roleCode = OWNER
accessible = true
```

该问题最终通过：

```text
Gateway 验证
→ Service 验证
→ 数据库直接查询
→ MyBatis SQL Debug
→ Java VO 映射检查
```

完成定位。

---

## 二十四、为什么 Project 与 Task 放在同一个服务

Project 与 Task 属于同一个协作领域：

```text
Project
   ↓
Task
```

两者存在较强业务关联。

如果当前阶段拆成：

```text
Project Service
Task Service
```

会引入额外：

- 服务注册
- Feign 调用
- 分布式失败
- 网络开销
- 服务治理成本

但业务收益较低。

因此当前设计为：

```text
cloudwork-project
├── Project
└── Task
```

如果未来 Task 领域进一步复杂，例如增加：

- 评论
- 附件
- 工作流
- SLA
- 自动化规则
- 大规模任务调度

再考虑独立拆分 Task Service。

---

## 二十五、为什么没有直接使用 DataScope 做租户隔离

基础框架 DataScope 主要面向：

```text
部门
角色
组织权限
```

CloudWork 中的 SaaS Tenant 是另一层独立业务边界。

因此没有直接复用平台 DataScope 作为租户隔离机制，而是建立独立：

```text
tenant
tenant_role
tenant_member
```

模型。

平台角色与 Workspace 角色相互独立：

```text
平台 RBAC
≠
SaaS Tenant Role
```

---

## 二十六、项目目录

```text
cloudwork/
├── backend/
│   ├── ruoyi-auth/
│   ├── ruoyi-gateway/
│   ├── ruoyi-api/
│   ├── ruoyi-common/
│   ├── ruoyi-modules/
│   │
│   ├── cloudwork-services/
│   │   ├── cloudwork-tenant/
│   │   └── cloudwork-project/
│   │
│   ├── sql/
│   └── docker-compose-dev.yml
│
├── frontend/
│   └── src/
│       ├── api/
│       │   └── cloudwork/
│       └── views/
│           └── cloudwork/
│               ├── workspace/
│               ├── project/
│               └── task/
│
├── docs/
├── k8s/
├── UPSTREAM.md
└── README.md
```

---

## 二十七、后续规划

以下能力作为后续扩展方向，目前不作为已完成功能：

- Workspace 成员邀请
- 自定义租户角色
- Project 成员管理
- Task 指派流程
- Kafka 消息通知
- Elasticsearch 全文搜索
- MinIO 文件存储
- Prometheus + Grafana 可观测性
- CI/CD
- 更完整的 Kubernetes 部署
- 分布式事务

项目当前阶段优先保证：

```text
核心业务闭环
+
多租户安全
+
微服务调用
+
可运行
+
可演示
```

而不是简单堆叠中间件数量。

---

## 二十八、基础脚手架说明

CloudWork 基于成熟的 Spring Cloud 微服务与 Vue 3 前端脚手架构建，并在此基础上形成自己的 SaaS 产品能力。

上游仓库及对应版本信息记录在：

```text
UPSTREAM.md
```

项目保留必要的 MIT License 与版权声明。

项目仓库：[CloudWork SaaS Platform](https://github.com/xiaocao2026/cloudwork-saas-platform)

CloudWork 在基础脚手架之上新增的主要内容包括：

- Workspace 多租户领域
- Tenant Role / Tenant Member 模型
- Tenant Internal Access API
- Project / Task 协作领域
- OpenFeign 跨服务租户授权
- tenant_id 数据隔离
- CloudWork Dashboard
- Workspace 前端
- Projects 前端
- Task 三列看板
- Docker 前端开发环境
- Tenant Testcontainers 集成测试

---

## 二十九、License

本项目保留并遵循上游项目的开源许可证要求。

相关信息：

```text
LICENSE
UPSTREAM.md
```
