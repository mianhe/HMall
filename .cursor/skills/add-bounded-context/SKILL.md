---
name: add-bounded-context
description: 在 HMall 中新增一个限界上下文（BC）：创建文档、后端四层骨架、验收测试脚手架，更新上下文地图与脚本。最小实现，无业务代码，仅保证 mvn test 可运行。触发词：新增限界上下文、新建 BC、add bounded context。
---

# 新增限界上下文

在 HMall 中新增一个 Bounded Context，搭建完整骨架。具体功能由 `evolve-feature` 后续实现。

## 前置输入

| 输入 | 说明 |
|------|------|
| **BC 名称** | 小写英文，如 `cart`、`order` |
| **职责** | 一句话描述该 BC 负责什么 |
| **上游依赖** | 依赖哪些已有 BC |

## 产出清单（需要创建或更新的文件）

参考**最近创建的 BC**（如 `inventory-service`、`cart-service`）的实际代码，保持结构与风格一致。

| 类别 | 需创建/更新 |
|------|------------|
| **文档** | `docs/bounded-contexts/<ctx>/` 下：`domain-model.md`、`requirements.md`、`api.yaml` |
| **文档更新** | `docs/context-map.md`（概览图、说明表、集成关系表）、`docs/README.md`（文档索引表）、`docs/project-status.md`（BC 状态表） |
| **后端骨架** | `services/<ctx>-service/` 下：`pom.xml`、`XxxApplication.java`、四层包（api / application / domain / infrastructure，各含 `package-info.java`）、占位 Controller（GET 返回 200）、ExceptionHandler、ErrorDto、BadRequestException、`application.yml`（主 + 测试） |
| **测试脚手架** | `smoke.feature`（1 个冒烟场景，`@<ctx>` tag）、`XxxSmokeStepDefinitions`、`AcceptanceTestConfig`、`CucumberSpringConfiguration`、`RunCucumberTest` |
| **脚本更新** | `scripts/hmall.sh`（status / start / stop / test 的所有相关位置）、`scripts/README.md` |

## 约定

- **命名**：BC 名小写，包名 `com.hmall.<ctx>`，服务目录 `services/<ctx>-service`。
- **异常**：每个 BC 独立 `*ExceptionHandler`（`basePackageClasses` 指向自己的 Controller），不共用。
- **冒烟**：技术脚手架，不属于业务需求，**不在 requirements 中列出**。smoke.feature 中 GET 占位端点并断言 200。
- **StepDefinitions**：不用 `@Component`，在 `AcceptanceTestConfig` 中以 `@Bean` 注册。
- **端口**：选一个未被占用的端口（现有：8080 catalog / 8081 order / 8082 user / 8083 inventory / 8084 payment / 8085 bff / 8086 activity / 8087 cart）。

## 检查清单

- [ ] 文档骨架已创建，context-map、README、project-status 已更新
- [ ] 后端四层骨架可编译，占位 Controller 返回 200
- [ ] `mvn test` 通过（冒烟场景真实 HTTP 请求并断言 200）
- [ ] `./scripts/hmall.sh status` 显示新服务；`./scripts/hmall.sh test --bc <ctx>` 可用

## 参考

- 现有 BC 实现作为模板：`services/inventory-service/`、`services/cart-service/`
- 设计原则：`docs/design-principles.md`
- 后续功能开发：`evolve-feature` Skill
