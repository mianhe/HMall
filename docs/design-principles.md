# HMall 系统设计原则

本文档定义架构与实现约定，与**领域模型**、**需求与验收测试**、**Skills** 共同构成可复现输入。代码可依据这四类输入重新生成。

---

## 一、架构

### 1.1 DDD 分层

后端按限界上下文组织，每上下文四层：

```
com.hmall.<context>/
├── api/            # REST Controller、DTO、异常处理
├── application/    # 应用服务、用例编排、事务
├── domain/         # 实体、不变式、仓储接口（无外部依赖）
└── infrastructure/ # 仓储实现、JPA 实体
```

依赖方向：api → application → domain；infrastructure → domain。

### 1.2 限界上下文与契约

- 上下文独立目录（`com.hmall.catalog` 等），通过接口或事件通信；
- REST API 由 OpenAPI YAML 定义；实现与契约一致；
- 文档与测试按上下文组织：`docs/bounded-contexts/<context>/`、`features/<context>/`。

---

## 二、数据隔离

验收测试与生产/开发数据库完全隔离。生产用 PostgreSQL；验收测试用 H2 内存库（`@ActiveProfiles("test")`、`create-drop`），运行结束即销毁。每场景独立 Given 数据，不共享状态。

---

## 三、实现约定

| 类别 | 约定 |
|------|------|
| 领域实体 | 充血模型，封装不变式；区分「新建」与「从持久化还原」 |
| 仓储 | 接口在 domain，实现以 `XxxRepositoryImpl` 命名 |
| 应用服务 | `XxxApplicationService`，方法表达用例意图 |
| DTO | `XxxCreateDto`、`XxxDto` 等，与 OpenAPI schema 一致 |
| 异常 | 业务校验失败 → 领域异常 → 400；资源不存在 → 404；统一 `{ "message": "..." }` |
| 持久化 | domain 与 JPA 分离；仓储内完成 domain ↔ entity 转换 |

---

## 四、验收测试

需求与 .feature 一一对应；先红后绿；Step Definitions 按 OpenAPI 调用；场景覆盖成功与失败（404、400）情形。

---

## 五、文档与输入

| 输入类型 | 位置 |
|----------|------|
| 领域模型 | `docs/bounded-contexts/<context>/domain-model.md` |
| 需求列表 | `docs/bounded-contexts/<context>/requirements.md` |
| API 契约 | `docs/bounded-contexts/<context>/api.yaml` |
| 验收测试 | `backend/src/test/resources/features/<context>/` |
| 设计原则 | `docs/design-principles.md` |
| Skills | `.cursor/skills/` |

---

## 六、变更

新增功能按 ATDD 流程；原则冲突以本文档为准，例外需注明原因。
