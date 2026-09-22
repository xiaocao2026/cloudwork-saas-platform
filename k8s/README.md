# CloudWork Kubernetes 部署说明

该目录提供 CloudWork 的 Kubernetes 部署参考模板。

> 当前 manifests 用于展示服务部署结构和后续容器化部署方案，尚未在本项目本地 Kubernetes 集群中进行完整部署验证。

## 文件

```text
k8s/
├── namespace.yaml
├── cloudwork-services.yaml
└── README.md
```

## 服务

当前模板包含：

- Gateway
- Auth
- System
- Tenant
- Project
- Frontend

所有资源部署到：

```text
cloudwork
```

Namespace。

## 外部依赖

运行 CloudWork 还需要：

- Nacos
- MySQL
- Redis

当前 Kubernetes 模板假设 Nacos 可以通过：

```text
nacos:8848
```

访问。

实际部署时，可以将 Nacos、MySQL、Redis 部署在 Kubernetes 集群内部，也可以使用外部托管服务。

## 镜像

YAML 中的：

```text
cloudwork/ruoyi-gateway:latest
cloudwork/ruoyi-auth:latest
cloudwork/ruoyi-system:latest
cloudwork/cloudwork-tenant:latest
cloudwork/cloudwork-project:latest
cloudwork/frontend:latest
```

均为部署模板中的镜像名称。

正式部署前，需要先根据各服务构建实际 Docker Image，并推送到可访问的 Image Registry，然后修改 manifests 中的 `image`。

## 应用 Namespace

```bash
kubectl apply -f namespace.yaml
```

## 应用服务

```bash
kubectl apply -f cloudwork-services.yaml
```

## 查看 Pod

```bash
kubectl get pods -n cloudwork
```

## 查看 Service

```bash
kubectl get svc -n cloudwork
```

## Frontend

模板中 Frontend 使用：

```text
NodePort 30080
```

在支持 NodePort 的 Kubernetes 环境中可以通过节点地址访问。

生产环境更建议使用：

```text
Ingress
+
TLS
+
Domain
```

统一暴露前端与 Gateway。

## 生产化演进

当前模板是最小部署参考。

如果进一步生产化，可以增加：

- ConfigMap
- Secret
- Ingress
- TLS
- HPA
- PodDisruptionBudget
- Readiness Probe
- Liveness Probe
- PersistentVolume
- Prometheus Metrics
- Centralized Logging
- CI/CD
- Private Image Registry

这些能力目前不作为 CloudWork 已实现功能。