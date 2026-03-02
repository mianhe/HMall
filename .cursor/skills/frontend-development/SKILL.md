---
name: frontend-development
description: 实现或扩展 HMall 前端（frontend/admin、frontend/web）。需求优先、契约对齐、不重复业务规则；新页面/大改时维护界面规格（UISpec）。触发：前端开发、加页面、加功能、前端需求。
---

# 前端开发（HMall）

适用于 **frontend/admin**、**frontend/web**。全项目前端约定见 `docs/design-principles.md` 第三节「前端：开发约定」；本 Skill 给出**操作顺序**与**文档引用**。

---

## 一、何时用本 Skill

- 新增/修改前端页面或 UI 功能
- 用户提到「前端开发」「加一个页面」「消费者端/管理后台要做 xxx」
- 需要知道：先改哪份文档、再写哪部分代码、API 和错误怎么对齐

---

## 二、输入与输出（和谁对齐）

| 输入 | 用途 |
|------|------|
| `docs/project-status.md` | 当前前端进度、下一步计划 |
| `docs/frontend/<名>/ui-spec.md` | 该前端的界面规格（UISpec）：页面规格、组件结构、视觉规范 |
| `docs/frontend/<名>/testing.md` | 前端测试说明（Smoke E2E 分层、用例清单、运行方式、维护策略） |
| `docs/bounded-contexts/<context>/api.yaml` | 请求路径、请求/响应 body，与实现严格一致 |
| `docs/design-principles.md` | 前端流程原则、界面规格定位、技术栈与目录约定 |

输出：符合 ui-spec 与 api 契约的页面/组件；必要时已更新的 ui-spec；Smoke E2E 全绿。

---

## 三、开发流程（四步）

### 步骤 1：确定/更新界面规格（必做）

- 打开该前端的 **ui-spec.md**（见下表「文档位置」）。
- 新增或修改：路由、页面名、交互行为简述、使用的 API（方法 + path）。
- 新页面或大改结构时，同步更新组件结构（Atomic Design 分层）、页面结构、视觉规范。
- 不在未更新需求的情况下直接加页面或大改行为。

### 步骤 2：实现

- **API**：在 `src/shared/api/` 按 BC 或模块封装，路径与 api.yaml 一致，`/api/...` 由 Vite 代理。
- **路由**：在 `src/router/index.js` 注册 path、name、component（懒加载）、meta.title。
- **页面**：`pages/*Page.vue`；复用块 `shared/ui/`，复杂时可分子目录 atoms / molecules / organisms。
- **展示与错误**：只根据后端返回展示成功或 `e.response?.data?.message`（或 `e.message` /「加载失败」），不写死业务文案。
- **样式**：Tailwind，与现有风格一致（VMALL 主色、顶栏、圆角卡片等，见 ui-spec 或 design-principles）。

### 步骤 3：自动化验证（必做）

> **`vite build` 通过 ≠ 前端正确。** 构建只检查语法和模块解析，不能验证渲染、数据绑定、交互行为。自动化验证通过两层 E2E 测试保证既有链路不被破坏，并对关键业务需求提供验收验证。

完成实现后**必须按顺序执行**：

1. **`npm run build`** — 确认无编译错误。
2. **`npm run test:smoke:e2e:p0`** — 运行 Smoke E2E P0（核心交易链路），**必须全绿**。
   - 需要后端服务可用（通过 `./scripts/hmall.sh start` 启动，或手动启动所需服务）。
   - 若 Smoke P0 失败，优先排查是本次变更引入的回归还是环境问题（参考 `docs/frontend/web/testing.md`）。
   - 任何因本次变更导致的 P0 失败必须修复后才能继续。
   - P1 用例不自动运行，由开发者按需手工验证或通过 `npm run test:smoke:e2e` 全量运行。

> **Business E2E 验收与 Smoke 入选评估**由 `deliver-requirement` 在迭代所有工作项完成后统一执行（E2E 交付门禁），不在单次前端开发中执行。详见 `deliver-requirement` Skill Step 2。

> **Smoke P0 时间预算**：目标 ≤ 1 分钟，硬上限 ≤ 2 分钟。P0 永不降级。

### 步骤 4：开发者确认（必做）

自动化验证通过后，仍需开发者最终确认：

1. **列出受影响的页面**：URL、预期展示内容和交互行为。
2. **停顿并请开发者确认**：开发者可通过 browser-use 截图或手动访问验证。
3. 开发者确认通过后方可标记完成。

如果后端无法启动导致 Smoke E2E 无法运行，需告知开发者「代码已完成且 build 通过，需启动服务后运行 Smoke E2E 并人工验证」，不可跳过确认环节。

#### ⚠️ 严禁前端硬编码业务逻辑

**这是最高优先级的约束。** 前端代码中不得出现任何业务规则的硬编码，包括但不限于：

- 事件/状态分类映射（如"哪些事件类型是补偿事件"）
- 业务实体的中文标签/名称映射（如"OrderCreated → 订单创建"）
- 业务实体之间的关系映射（如"StockReleased 补偿了 StockReserved"）
- 状态机、流程判断、业务校验规则

**正确做法**：

1. 业务语义信息由**后端 API 返回**（如 metadata 字段、枚举接口），前端只做展示。
2. 前端只处理**纯展示逻辑**（如"补偿类型显示红色"、"异常类型显示琥珀色"），这些颜色映射基于后端返回的 `category` 字段，而非 `eventType`。
3. 如果后端 API 尚未提供所需的业务语义信息，应先**要求后端补充接口或字段**，而非在前端填补。

**违反此约束的后果**：多端不一致、维护成本指数增长、业务变更需改多处代码。

**如果不得不临时硬编码**（极少数紧急情况），必须：
1. 在代码中加 `// FIXME: 业务逻辑硬编码，需后端提供 API 后移除` 注释
2. **立即告知开发者**，确认这是临时方案并约定后端何时补上

---

## 四、各前端文档位置

| 前端 | 目录 | 界面规格 | 开发端口 |
|------|------|----------|----------|
| frontend/admin | `frontend/admin/` | `docs/frontend/admin/ui-spec.md` | 5173 |
| frontend/web | `frontend/web/` | `docs/frontend/web/ui-spec.md` | 5174 |

---

## 五、第一次加一个页面（速查）

1. 在 ui-spec 中加一行：path、页面名、功能、API。
2. 在 `shared/api/` 对应模块加请求函数（路径与 api.yaml 一致）。
3. 在 router 中加路由，component 懒加载 `*Page.vue`。
4. 复制同应用已有 `*Page.vue`，改 API 调用与展示字段；错误用 `e.response?.data?.message`。
5. 需要入口时在 AppHeader 或首页加 `router-link`。
6. `npm run build` 确认无编译错误。
7. `npm run test:smoke:e2e` 确认既有链路不受影响。

API 字段查 `docs/bounded-contexts/<对应 BC>/api.yaml`。

---

## 六、可选：v0 作布局参考

v0 生成多为 React；HMall 为 Vue。仅用 v0 的**布局与 Tailwind 类**在 Vue 中复刻，数据与错误处理仍按 requirements 与 api.yaml，不在 v0 里写业务逻辑。

---

## 七、检查清单（做完可对一遍）

- [ ] 已更新该前端的 **ui-spec.md**（若为新能力或行为变更）
- [ ] 新接口已在 **shared/api** 封装，路径与 api.yaml 一致
- [ ] 路由已注册，错误与成功展示均以后端返回为准
- [ ] **无业务逻辑硬编码**：所有业务语义（标签、分类、关系、规则）均来自后端 API，前端无硬编码映射表
- [ ] **`npm run build` 通过**：无编译错误
- [ ] **Smoke P0 全绿**：`npm run test:smoke:e2e:p0` 通过，核心链路未被破坏
- [ ] **开发者已确认**：受影响页面列出，开发者确认渲染与交互正确
- [ ] **Business E2E 与 Smoke 入选**：由 `deliver-requirement` 在迭代收尾时统一执行，本 Skill 不负责
