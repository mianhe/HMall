---
name: add-bounded-context
description: 在 HMall 中新增一个限界上下文（BC）：后端四层骨架、验收测试脚手架、API 契约、文档与脚本更新。最小实现，无业务代码，仅保证 mvn test 可运行。触发词：新增限界上下文、新建 BC、add bounded context。
---

# 新增限界上下文

在 HMall 中新增一个 Bounded Context，搭建完整骨架。具体功能由 `evolve-feature` 后续实现。

## 前置条件

- `analyze-requirement` 已完成，`docs/bounded-contexts/<ctx>/` 下已有 `requirements.md`、`domain-model.md` 等。
- 若文档尚未创建，先执行 `analyze-requirement`。

## 执行流程

### 第一步：读参考文件

| 读取对象 | 用途 |
|----------|------|
| `services/cart-service/` 的代码结构 | 作为骨架模板（pom.xml、Application、Controller、ExceptionHandler、ErrorDto、BadRequestException、package-info.java、application.yml、smoke.feature、AcceptanceTestConfig、CucumberSpringConfiguration、RunCucumberTest） |
| `scripts/hmall.sh` | 确认已用端口、定位需修改的位置 |
| `scripts/README.md` | 同步更新 |
| `docs/context-map.md` | 更新 BC 状态 |
| `docs/README.md` | 更新文档索引 |
| `docs/project-status.md` | 更新 BC 进度表 |
| 本 BC 的 `docs/bounded-contexts/<ctx>/event-flow.md`（若有） | api.yaml 的 API 契约来源 |

### 第二步：创建后端骨架

目录：`services/<ctx>-service/`。包名 `com.hmall.<ctx>`。

| 文件 | 要点 |
|------|------|
| `pom.xml` | 照搬 cart-service，改 artifactId/name/description |
| `XxxApplication.java` | 标准 `@SpringBootApplication` 启动类 |
| 四层 `package-info.java` | `api`、`application`、`domain`、`infrastructure` |
| `XxxController.java` | `@RequestMapping("/api/<ctx>")`，一个 `GET /health` 返回 200 |
| `XxxExceptionHandler.java` | `@RestControllerAdvice(basePackageClasses = XxxController.class)`；处理 BadRequest(400)、MethodArgumentNotValid(400)、兜底(500) |
| `dto/ErrorDto.java` | `record ErrorDto(String message)` |
| `XxxBadRequestException.java` | 放在 `application` 包 |
| `application.yml`（主） | **端口**：读 hmall.sh 确认已用端口，选下一个未占用的 |
| `application.yml`（测试） | H2 内存库，`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`，`ddl-auto: create-drop` |

### 第三步：创建测试脚手架

目录：`src/test/`。

| 文件 | 要点 |
|------|------|
| `features/<ctx>/smoke.feature` | `# language: zh-CN`，`@<ctx>` tag，一个场景调 health 断言 200 |
| `XxxSmokeStepDefinitions.java` | 注入 `TestRestTemplate` + `XxxTestContext`；**不加** `@Component` |
| `XxxTestContext.java` | 持有 `lastStatusCode` 等共享状态 |
| `config/AcceptanceTestConfig.java` | `@Configuration`；以 `@Bean` 注册 TestContext；以 **`@Bean` + `@Primary`** 注册 StepDefinitions（Cucumber 会自动扫描包注册一份，`@Primary` 避免冲突） |
| `CucumberSpringConfiguration.java` | `@CucumberContextConfiguration` + `@SpringBootTest(classes={XxxApplication.class, AcceptanceTestConfig.class}, webEnvironment=RANDOM_PORT)` + `@ActiveProfiles("test")` |
| `RunCucumberTest.java` | `@Suite` + `@SelectClasspathResource("features")` + `glue = "com.hmall.<ctx>.acceptance"` |

### 第四步：创建 API 契约文档

`docs/bounded-contexts/<ctx>/api.yaml`：基于 `event-flow.md`（或 `requirements.md`）中定义的 REST 接口，编写 OpenAPI 3.0 契约。骨架阶段只需定义 path/method/request/response schema，无需实现。

### 第五步：更新文档

| 文件 | 修改内容 |
|------|---------|
| `docs/context-map.md` | BC 状态从"需求已完成"改为"骨架已建"，补充端口号 |
| `docs/README.md` | 文档索引表新增一行 |
| `docs/project-status.md` | BC 状态表更新，补充端口号 |

### 第六步：更新脚本

**`scripts/hmall.sh`** 需修改以下位置：

1. `ALL_COMPONENTS` 变量：追加 `<ctx>-service`
2. 新增 `status_<ctx>_service()` 函数
3. `cmd_status()` 中追加调用
4. 新增 `start_<ctx>_service()` 函数
5. 新增 `stop_<ctx>_service()` 函数
6. `run_start` 的 `case` 中追加分支
7. `run_stop` 的 `case` 中追加分支 + 默认停止顺序
8. `print_test_summary` 的 name 映射追加
9. `cmd_test` 中：`--bc` 的 `case` 追加、`elif` 链追加、全部测试列表追加
10. `usage()` 的 components 和 test options 说明追加

**`scripts/README.md`**：组件列表、命令参数、示例中追加新服务。

### 第七步：验证

```
cd services/<ctx>-service && mvn test
```

预期：1 scenario，0 failures。若失败，根据错误修复后重跑。

## 检查清单

- [ ] 后端四层骨架可编译，占位 Controller 返回 200
- [ ] `mvn test` 通过（冒烟场景真实 HTTP 请求并断言 200）
- [ ] 若定义了 Domain 层接口（端口、事件发布者等），`infrastructure` 层有生产环境默认实现，`mvn spring-boot:run` 可启动
- [ ] `api.yaml` 已创建
- [ ] `context-map`、`README`、`project-status` 已更新
- [ ] `hmall.sh` 所有位置已更新；`./scripts/hmall.sh test --bc <ctx>` 可用
- [ ] `scripts/README.md` 已更新
