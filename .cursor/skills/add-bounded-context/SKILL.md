---
name: add-bounded-context
description: 在 HMall 中新增一个限界上下文（BC）：创建文档骨架、后端四层骨架、验收测试脚手架，更新上下文地图。最小实现，无业务代码，仅保证 mvn test 可运行。触发词：新增限界上下文、新建 BC、add bounded context。
---

# 新增限界上下文

在 HMall 中新增一个 Bounded Context，搭建完整骨架。具体功能由 `add-new-feature` 后续实现。

## 前置输入

执行前需确认：

| 输入 | 说明 |
|------|------|
| **BC 名称** | 小写英文，如 `user`、`order` |
| **职责** | 一句话描述该 BC 负责什么 |
| **上游依赖** | 依赖哪些已有 BC（如 Order 依赖 Catalog、User） |

## 产出清单

| 类别 | 路径 | 说明 |
|------|------|------|
| 文档 | `docs/bounded-contexts/<context>/domain-model.md` | 领域模型骨架（占位） |
| 文档 | `docs/bounded-contexts/<context>/requirements.md` | 需求列表骨架（占位） |
| 文档 | `docs/bounded-contexts/<context>/api.yaml` | OpenAPI 空契约 |
| 文档 | `docs/bounded-contexts/<context>/process/` | 过程性文档目录（.gitkeep） |
| 文档 | `docs/bounded-contexts/context-map.md` | 更新：新增 BC 及集成关系 |
| 文档 | `docs/README.md` | 更新：对应关系表 |
| 后端 | `com.hmall.<context>/` 四层 | api、application、domain、infrastructure |
| 后端 | 占位 Controller、ExceptionHandler、ErrorDto | 保证应用可启动 |
| 测试 | `features/<context>/smoke.feature` | 占位场景，1 个 Scenario 可执行 |
| 测试 | `com.hmall.<context>.acceptance/` | XxxSmokeStepDefinitions、config/XxxAcceptanceTestConfig |
| 测试 | `RunCucumberTest` | 在 glue 中增加 `com.hmall.<context>.acceptance` |
| 测试 | `CucumberSpringConfiguration` | 在 classes 中增加 `XxxAcceptanceTestConfig.class` |

## 执行步骤

### 阶段一：文档与上下文地图

1. 创建 `docs/bounded-contexts/<context>/` 及 `domain-model.md`、`requirements.md`、`api.yaml`、`process/.gitkeep`（可参考 Catalog 结构写占位内容）。
2. 更新 `docs/bounded-contexts/context-map.md`：在概览、说明表、集成关系表中加入新 BC。
3. 更新 `docs/README.md` 对应关系表。

### 阶段二：后端骨架

4. 创建 `com.hmall.<context>/` 四层包：`api/`、`application/`、`domain/`、`infrastructure/`，各层含 `package-info.java`。
5. 在 `api/dto/` 下创建 `ErrorDto`（与 Catalog 结构一致：`{ "message": "..." }`）。
6. 在 `api/` 下创建**占位 Controller**（如 `XxxController`，一个空 GET 返回 200 或空列表），保证有可被 `@RestControllerAdvice` 作用到的 Controller。
7. 在 `api/` 下创建**该 BC 专属 ExceptionHandler**：`@RestControllerAdvice(basePackageClasses = { XxxController.class })`，处理 `IllegalArgumentException`→404、业务异常→400，统一返回 `ErrorDto`。
8. 在 `application/` 下创建占位 `XxxBadRequestException`（可选，供 ExceptionHandler 使用）。

### 阶段三：验收测试脚手架

9. 创建 `features/<context>/smoke.feature`：占位 Feature，含 1 个 Scenario。**在 Feature 上添加 tag**（如 `@user`），以便 `./scripts/hmall.sh test --bc <context>` 可筛选执行。
10. 创建 `XxxSmokeStepDefinitions`：实现占位步骤（无实际逻辑），**不可**用 `@Component`；在 `com.hmall.<context>.acceptance.config.XxxAcceptanceTestConfig` 中注册为 `@Bean`。
11. 更新 `RunCucumberTest`：将 `GLUE_PROPERTY_NAME` 改为包含 `com.hmall.<context>.acceptance`。
12. 更新 `CucumberSpringConfiguration`：在 `classes` 中增加 `XxxAcceptanceTestConfig.class`。
13. 执行 `mvn test`，确认通过（含 Catalog 场景 + 新 BC 的 1 个占位场景）。

## 约定

- **命名**：BC 名小写英文，包名 `com.hmall.<context>`。
- **异常**：每个 BC 独立 `*ExceptionHandler`，不与其他 BC 共用。
- **首个功能**：由 `add-new-feature` 在新建 BC 下实现，本 Skill 仅产出占位 smoke.feature。

## 检查清单

- [ ] 文档骨架已创建，context-map 与 README 已更新
- [ ] 后端四层骨架、占位 Controller、ExceptionHandler、ErrorDto 已创建
- [ ] smoke.feature、SmokeStepDefinitions、AcceptanceTestConfig 已创建，RunCucumberTest glue 与 CucumberSpringConfiguration 已更新
- [ ] `mvn test` 通过

## 参考

- 设计原则：`docs/design-principles.md`
- Catalog 示例：`com.hmall.catalog/`、`docs/bounded-contexts/catalog/`
- 后续功能开发：使用 `add-new-feature` Skill
