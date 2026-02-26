---
name: extract-bounded-context
description: 从已有服务中拆分出独立的限界上下文（BC）：特性与文档拆分 → 创建新 BC 骨架 → 代码搬迁 → 测试迁移 → 原宿主清理 → 脚本与文档更新。全程保证双侧测试绿色。触发词：拆分 BC、提取 BC、extract bounded context、拆分限界上下文。
---

# 拆分限界上下文

从一个已有服务（宿主）中将某个领域模块提取为独立的限界上下文。与 `add-bounded-context`（从零新建）不同，拆分涉及代码搬迁、测试迁移、原宿主清理，需要全程保持双侧绿色。

## 适用场景

| 场景 | 说明 |
|------|------|
| 模块已越界 | 某模块有独立的领域服务、独立的配置和依赖，与宿主其余功能无交集 |
| 独立生命周期 | 模块的变更频率、部署需求与宿主不同 |
| 演进需要 | 模块即将引入持久化、领域模型等，留在宿主中会造成职责混乱 |

**不适用**：功能尚在探索期，边界不清晰时不要拆；纯粹的代码重构（不涉及 BC 边界变更）用常规重构即可。

## 前置条件

- 待拆分模块的边界已明确（能说清哪些类、配置、测试属于它）
- 宿主当前测试全绿

---

## Step 0：评估与规划

### 0a. 确认拆分范围

| 维度 | 要回答的问题 |
|------|------------|
| **哪些代码** | 哪些包/类要搬到新 BC？列出清单 |
| **哪些配置** | 哪些 application.yml 配置项属于该模块？ |
| **哪些测试** | 哪些 feature 文件、step definitions、test config 要迁移？ |
| **哪些文档** | 哪些特性、设计文档要从宿主文档中拆出？ |
| **依赖方向** | 该模块是否依赖宿主的任何代码？宿主是否依赖该模块的任何代码？（理想状态：零依赖） |

### 0b. 确认新 BC 的定位

| 维度 | 要回答的问题 |
|------|------------|
| **BC 名称** | 新 BC 的名称和包名 |
| **端口** | 分配哪个端口？（检查 hmall.sh 中已用端口） |
| **是否需要数据库** | 当前和近期是否需要持久化？ |
| **架构风格** | 是否采用 DDD 四层架构？还是更轻量的结构？ |

### 0c. 前置读取

| 读取对象 | 用途 |
|----------|------|
| 宿主服务的完整代码（待拆分模块） | 了解当前实现 |
| 宿主的测试代码（待迁移部分） | 了解测试结构 |
| 宿主的特性（requirements.md） | 识别哪些特性要拆出 |
| `docs/context-map.md` | 了解系统全貌和集成关系 |
| `scripts/hmall.sh` | 确认已用端口和脚本结构 |
| `add-bounded-context` Skill | 作为新 BC 骨架的模板参考 |

---

## Step 1：特性与文档拆分

**目标**：将待拆分模块的特性从宿主文档中分离出来，建立新 BC 的文档。

### 1a. 创建新 BC 文档目录

`docs/bounded-contexts/<new-ctx>/`，创建以下文件：

| 文件 | 内容来源 |
|------|---------|
| `requirements.md` | 从宿主 requirements.md 中提取相关特性与场景 |
| `domain-model.md`（如有领域模型） | 新写或标注"当前无聚合根，仅有领域服务" |
| `api.yaml`（如有 REST API） | 从宿主 api.yaml 中提取或新写 |

### 1b. 更新宿主文档

从宿主的 `requirements.md` 中移除已拆出的章节，替换为指向新 BC 文档的引用。

### 1c. 与用户确认

文档拆分完成后，向用户展示新 BC 的 requirements.md 和宿主更新后的 requirements.md（承载的内容是特性），确认拆分边界正确。

---

## Step 2：创建新 BC 骨架

参考 `add-bounded-context` Skill 的流程，但有以下区别：

| 区别点 | add-bounded-context | extract-bounded-context |
|--------|--------------------|-----------------------|
| 业务代码 | 无，只有 smoke 测试 | 搬迁已有代码 |
| 测试 | 只有 smoke 场景 | 搬迁已有验收测试 |
| Controller | 只有 /health | 搬迁已有 Controller |

**执行**：

1. 创建 `services/<new-ctx>-service/` 目录和 `pom.xml`（参考宿主或其他已有 BC）
2. 创建 Application 启动类
3. 创建包结构（根据 0b 的架构决策）
4. 创建测试 application.yml（H2 或 WireMock，根据需要）
5. 创建 Cucumber 脚手架（RunCucumberTest、CucumberSpringConfiguration）
6. **此时先不搬迁代码**，只确保空骨架 `mvn compile` 通过

---

## Step 3：代码搬迁

**原则**：逐步搬迁，每一步都保证编译通过。

### 3a. 搬迁顺序

建议按依赖关系从底向上搬迁：

1. **配置类**（Config、Properties）
2. **DTO / 值对象**
3. **领域服务 / 核心逻辑**
4. **基础设施层**（Client、Adapter、Bridge）
5. **Controller**

### 3b. 搬迁步骤（对每个文件/类）

1. 将文件从宿主复制到新 BC 对应位置
2. 修改 package 声明
3. 调整 import（内部引用改为新包名）
4. 编译新 BC，确认无编译错误
5. **暂不删除宿主中的原文件**（Step 5 统一清理）

### 3c. 搬迁配置

1. 将相关配置项从宿主 `application.yml` 复制到新 BC 的 `application.yml`
2. 新 BC 的测试 `application.yml` / `application-test.yml` 也同步配置
3. 如有 `@ConfigurationProperties`，确认前缀和注册方式

### 3d. 编译验证

```
cd services/<new-ctx>-service && mvn compile
```

---

## Step 4：测试迁移

### 4a. 搬迁测试文件

1. 将 feature 文件从宿主复制到新 BC 的 `src/test/resources/features/<new-ctx>/`
2. 将 step definitions 从宿主复制到新 BC 的 `acceptance/` 包
3. 将 test config（WireMock 配置、stub 等）从宿主复制到新 BC
4. 修改 package 声明和 import
5. 调整 CucumberSpringConfiguration 中的 classes 和 DynamicPropertySource

### 4b. 运行新 BC 测试

```
cd services/<new-ctx>-service && mvn test
```

确认所有场景通过。如有失败，逐一修复。

### 4c. 确认宿主测试仍然通过

此时宿主中仍保留原代码和测试，运行宿主测试确认无影响：

```
cd services/<host>-service && mvn test
```

---

## Step 5：原宿主清理

**目标**：从宿主中移除已搬迁的代码、配置、测试。这是最容易出错的步骤，需要细心。

### 5a. 清理清单

| 类型 | 要删除的内容 |
|------|-------------|
| 源代码 | 已搬迁的包/类 |
| 配置 | `application.yml` 中已搬迁的配置项 |
| 测试配置 | `application.yml`（测试）和 `application-test.yml` 中已搬迁的配置项 |
| 测试代码 | 已搬迁的 feature 文件、step definitions、test config |
| 依赖 | `pom.xml` 中仅被已搬迁代码使用的依赖 |
| 启动类注解 | 如 `@EnableConfigurationProperties` 中已搬迁的类引用 |
| 其他 | 如 Filter 中对已搬迁路径的排除规则 |

### 5b. 逐步清理并验证

每删除一批相关文件后，立即编译和测试宿主：

```
cd services/<host>-service && mvn test
```

确认宿主原有功能不受影响。

---

## Step 6：更新脚本

**`scripts/hmall.sh`** 需修改以下位置（参考 `add-bounded-context` Skill 第六步）：

1. `ALL_COMPONENTS` 变量：追加 `<new-ctx>-service`
2. 新增 `status_<new-ctx>_service()` 函数
3. `cmd_status()` 中追加调用
4. 新增 `start_<new-ctx>_service()` / `launch_<new-ctx>_service()` 函数
5. 新增 `stop_<new-ctx>_service()` 函数
6. `port_of()` 追加映射
7. `run_start` 的 `case` 中追加分支
8. `run_stop` 的 `case` 中追加分支 + 默认停止顺序
9. `print_test_summary` 的 name 映射追加
10. `cmd_test` 中：`--bc` 的 `case` 追加、`elif` 链追加、全部测试列表追加
11. `usage()` 的 components 和 test options 说明追加

**`scripts/README.md`**：组件列表、命令参数、示例中追加新服务。

**注意**：如果宿主的 `--bc` 标签或测试范围因拆分而变化，也需要同步更新。

---

## Step 7：更新系统文档

| 文档 | 更新内容 |
|------|---------|
| `docs/context-map.md` | 新增 BC 行；更新宿主行（移除已拆出的职责描述）；如有新的集成关系，更新架构图和集成关系表 |
| `docs/project-status.md` | 新增 BC 行；更新宿主行；添加变更日志条目 |
| `docs/README.md` | 文档索引表新增一行 |
| 宿主 `requirements.md` | 确认已拆出的章节已被移除或替换为引用 |

---

## Step 8：全量验证

1. **新 BC 测试**：`cd services/<new-ctx>-service && mvn test`
2. **宿主测试**：`cd services/<host>-service && mvn test`
3. **脚本验证**：`./scripts/hmall.sh test --bc <new-ctx>` 和 `./scripts/hmall.sh test --bc <host-tag>`
4. **全量测试**（可选）：`./scripts/hmall.sh test`

全部通过后，拆分完成。

---

## 检查清单

- [ ] 新 BC 文档已创建（特性/requirements.md 等）
- [ ] 宿主文档已更新（移除已拆出内容）
- [ ] 新 BC 骨架可编译
- [ ] 代码已搬迁，新 BC 测试全绿
- [ ] 宿主已清理，宿主测试全绿
- [ ] `hmall.sh` 所有位置已更新；`./scripts/hmall.sh test --bc <new-ctx>` 可用
- [ ] `scripts/README.md` 已更新
- [ ] `context-map.md`、`project-status.md`、`README.md` 已更新
- [ ] 新 BC 可独立启动（`mvn spring-boot:run`）

---

## 与其他 Skill 的关系

| 关系 | 说明 |
|------|------|
| **analyze-requirement** | 如果拆分涉及重新定义领域模型或事件流，先用 analyze-requirement 完成分析 |
| **add-bounded-context** | 本 Skill 的 Step 2 参考 add-bounded-context 的骨架创建流程，但不从零开始 |
| **evolve-feature** | 拆分完成后，新 BC 的后续功能演进使用 evolve-feature |
