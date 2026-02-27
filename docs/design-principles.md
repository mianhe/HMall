# HMall 系统设计原则

本文档定义**架构与实现约定**，适用于后端与前端。与**领域模型**、**需求与验收测试**、**界面规格（UISpec）**、**Skills** 共同构成可复现输入。

---

## 一、文档与输入的关系

| 输入类型 | 作用 | 位置 |
|----------|------|------|
| **领域模型** | 后端：实体、不变式、聚合边界 | `docs/bounded-contexts/<context>/domain-model.md` |
| **需求列表** | 后端：功能与 .feature 对应；前端：页面、路由、API 对应 | `docs/bounded-contexts/<context>/requirements.md`、`docs/frontend/*/ui-spec.md` |
| **业务需求方案** | 跨 BC 业务需求的整体方案、设计决策、迭代计划 | `docs/business-requirements/<name>/overview.md` |
| **API 契约** | 后端 REST 与前端请求/响应对齐 | `docs/bounded-contexts/<context>/api.yaml` |
| **界面规格（UISpec）** | **前端**：页面需求、组件分层（FSD/Atomic）、交互与风格；**不写业务规则** | `docs/frontend/admin/ui-spec.md`、`docs/frontend/web/ui-spec.md` |
| **设计原则** | 全项目架构与实现约定（本文档） | `docs/design-principles.md` |
| **Skills** | 操作指南：何时用、按何顺序、引用哪些输入 | `.cursor/skills/` |

**界面规格（UISpec）的定位**：前端的 `ui-spec.md` 将原来的 requirements 与 design-input 合并为一份文档，既回答「做什么功能、调什么 API」，也回答「长什么样、怎么拆组件」。新页面或大改结构时更新 UISpec；小改动时也在 UISpec 中维护。

---

## 二、后端：架构与约定

### 2.1 DDD 分层

后端按限界上下文组织，每上下文四层：

```
com.hmall.<context>/
├── api/            # REST Controller、DTO、异常处理
├── application/    # 应用服务、用例编排、事务
├── domain/         # 实体、不变式、仓储接口（无外部依赖）
└── infrastructure/ # 仓储实现、JPA 实体
```

依赖方向：api → application → domain；infrastructure → domain。

### 2.2 限界上下文与契约

- 上下文独立目录（`com.hmall.catalog` 等），通过接口或事件通信；
- REST API 由 OpenAPI YAML 定义；实现与契约一致；
- 文档与测试按上下文组织：`docs/bounded-contexts/<context>/`、`features/<context>/`。

### 2.3 数据隔离

验收测试与生产/开发数据库完全隔离。生产用 PostgreSQL；验收测试用 H2 内存库（`@ActiveProfiles("test")`、`create-drop`），运行结束即销毁。每场景独立 Given 数据，不共享状态。

### 2.4 实现约定

| 类别 | 约定 |
|------|------|
| 领域实体 | 充血模型，封装不变式；区分「新建」与「从持久化还原」 |
| 仓储 | 接口在 domain，实现以 `XxxRepositoryImpl` 命名 |
| 应用服务 | `XxxApplicationService`，方法表达用例意图 |
| DTO | `XxxCreateDto`、`XxxDto` 等，与 OpenAPI schema 一致 |
| 异常 | 业务校验失败 → 领域异常 → 400；资源不存在 → 404；统一 `{ "message": "..." }` |
| 持久化 | domain 与 JPA 分离；仓储内完成 domain ↔ entity 转换 |

### 2.4a 实现顺序：从域对象出发，自内向外

后端代码变更（新增或修改）必须按以下顺序执行：

```
域对象（Domain） → 仓储接口/实现 → 应用服务 → API / DTO
```

**原则**：
- 每改完一层，编译确认无类型不一致
- 禁止从外层（DTO、Entity、Controller）开始改然后"回头补"域对象
- 域对象中的类型（如 `Long` vs `long`）必须与领域模型文档中的 nullable 约束一致
- 全链路（Domain → Repository → AppService → DTO）类型必须统一

**为什么**：域对象是"源头真相"。如果从外层开始改，域对象容易被遗漏，导致自动拆箱 NPE 或语义不一致。编译器能检查出类型不匹配，但前提是域对象先改。

### 2.5 事件驱动与跨 BC 通信

#### 核心原则

- **所有跨 BC 事件统一通过 Kafka 发布/消费**，不使用 Spring `ApplicationEventPublisher` / `@EventListener`。
- **同步调用 + 事件**可共存：例如 Order 同步调用 Inventory 占用库存，Inventory 完成后发布 `StockReserved` 事件到 Kafka 供审计/读模型订阅。
- 事件契约定义在各 BC 的 `docs/bounded-contexts/<context>/event-flow.md`；上下游关系见 `docs/context-map.md`。

#### 发布端（出站事件）

| 层 | 命名 | 职责 |
|----|------|------|
| `application/port/` | `XxxEventPublisher`（接口） | 领域事件发布端口，定义 `publish(XxxEvent)` 方法 |
| `infrastructure/kafka/` | `SpringXxxEventPublisher` 或 `KafkaXxxEventPublisher`（实现） | 通过 `KafkaTemplate` 发送到 Kafka topic |
| `infrastructure/kafka/` | `XxxMessage`（record） | Kafka 消息体，`from(...)` 静态工厂构造 |
| `infrastructure/kafka/` | `XxxKafkaProperties`（`@ConfigurationProperties`） | topic 名称配置 |

- 当 `KafkaTemplate` 不可用时（测试环境），使用 `@Autowired(required = false)` 或 `@AutoConfiguration(after = KafkaAutoConfiguration.class)` + `@ConditionalOnBean(KafkaTemplate.class)` 保护。
- 生产环境 `application.yml` 中 **不排除** `KafkaAutoConfiguration`（Kafka 始终启用）。

#### 消费端（入站事件）

| 层 | 命名 | 职责 |
|----|------|------|
| `infrastructure/kafka/` | `KafkaXxxEventConsumer` | `@KafkaListener` 消费 topic，解析 `Map<String, Object>`，调用 `XxxEventService` |

- Consumer 由 `@AutoConfiguration` 类条件注册（见 Order 的 `OrderKafkaAutoConfiguration`）。
- topic 名称通过 `${order.kafka.topic.xxx:默认值}` 引用，便于配置覆盖。

#### 测试约定

| 关注点 | 做法 |
|--------|------|
| 出站事件 | `EventCapture` 实现发布端口并记录事件；测试配置中以 `@Primary` 覆盖生产实现 |
| 入站事件模拟 | 测试直接调用 `XxxEventService.onXxx(...)` 方法，不通过 `ApplicationEventPublisher` |
| Kafka 隔离 | 测试 `application.yml` 中 `spring.autoconfigure.exclude: KafkaAutoConfiguration` |

#### Topic 命名

`<bc>.<event-type>`，例如：`payment.completed`、`order.created`、`inventory.stock.reserved`、`fulfillment.shipped`。

#### 配置

```yaml
# application.yml（生产） — Kafka 始终启用
spring:
  kafka:
    bootstrap-servers: localhost:9092
<bc>:
  kafka:
    topic:
      <event>: <bc>.<event-type>

# application.yml（测试） — 排除 Kafka
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
```

### 2.6 验收测试

需求与 .feature 一一对应；先红后绿；Step Definitions 按 OpenAPI 调用；场景覆盖成功与失败（404、400）情形。

---

## 三、前端：开发约定

### 3.1 流程原则

- **需求优先**：新能力先落该前端的 `ui-spec.md`（页面、路由、API），再实现；不在未更新需求时直接加页或大改行为。
- **契约对齐**：请求路径、请求体/响应体与 `docs/bounded-contexts/<context>/api.yaml` 一致；前端不发明字段。
- **不重复业务规则**：校验、错误文案以后端返回为准；前端只展示 `e.response?.data?.message` 或通用「加载失败」，不写死「用户名已存在」等业务文案。

### 3.2 界面规格（UISpec）使用

- **何时维护**：新页面、大改结构或交互、引入新组件层级（如 Atomic 新一层）时，更新该前端的 `ui-spec.md`。
- **写什么**：页面需求（路由、功能、API）、FSD 范围（实体/页面/路由）、Atomic 分层（atoms/molecules/organisms）、页面结构、设计风格（如 VMALL 主色与留白）。不写后端业务逻辑。
- **位置**：`docs/frontend/admin/ui-spec.md`、`docs/frontend/web/ui-spec.md`。若某前端暂无 UISpec，实现时保持与现有页面结构一致即可。

### 3.3 技术栈与结构

- **栈**：Vue 3（Composition API）、Vite、Vue Router、Tailwind CSS、axios。
- **目录**：`src/shared/`（api、ui）、`pages/`、`router/`；API 按 BC 或模块封装在 `shared/api/`。
- **代理**：开发时 Vite 将 `/api` 代理到 BFF 或后端（见各前端 `vite.config.js`）。
- **样式**：Tailwind；与项目设计风格一致（如 VMALL：主色 `#C7000B`、背景 `#F5F5F5`、顶栏红底白字）。Tailwind 已配置 `vmall-red`、`vmall-gray-bg` 等，优先使用。

### 3.4 实现约定

| 类别 | 约定 |
|------|------|
| 页面 | `pages/` 下以 `*Page.vue` 命名；懒加载注册到 `router` |
| 复用 UI | 放 `shared/ui/`；复杂页面可按 atoms / molecules / organisms 分子目录组织 |
| API 封装 | `shared/api/<模块>.js`，路径与 OpenAPI 一致，返回 `data` |
| 错误展示 | `try/catch`，展示 `e.response?.data?.message` 或 `e.message` 或「加载失败」 |

---

## 四、变更与例外

新增功能：后端按 ATDD 流程；前端按「UISpec → 实现」。原则冲突以本文档为准，例外需注明原因。
