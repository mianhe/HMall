---
name: frontend-development
description: 实现或扩展 HMall 前端（frontend-admin、frontend-web）。需求优先、契约对齐、不重复业务规则；新页面/大改时维护 Design Input。触发：前端开发、加页面、加功能、前端需求。
---

# 前端开发（HMall）

适用于 **frontend-admin**、**frontend-web**。全项目前端约定见 `docs/design-principles.md` 第三节「前端：开发约定」；本 Skill 给出**操作顺序**与**文档引用**。

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
| `docs/frontend-<名>/requirements.md` | 该前端的页面、路由、功能、调用的 API |
| `docs/frontend-<名>/design-input.md` | 该前端的页面结构、组件分层（FSD/Atomic）、风格（若存在） |
| `docs/bounded-contexts/<context>/api.yaml` | 请求路径、请求/响应 body，与实现严格一致 |
| `docs/design-principles.md` | 前端流程原则、Design Input 定位、技术栈与目录约定 |

输出：符合 requirements 与 api 契约的页面/组件；必要时已更新的 requirements 与 design-input。

---

## 三、开发流程（三步）

### 步骤 1：确定/更新需求（必做）

- 打开该前端的 **requirements.md**（见下表「文档位置」）。
- 新增或修改：路由、页面名、功能简述、使用的 API（方法 + path）。
- 不在未更新需求的情况下直接加页面或大改行为。

### 步骤 2：设计输入（按需）

- **何时做**：新页面、大改结构或交互、引入新的组件层级时。
- **做什么**：更新该前端的 **design-input.md**：FSD 范围（实体/页面/路由）、Atomic 分层（atoms/molecules/organisms）、页面结构、设计风格。不写业务规则与 API 细节。
- **不做也行**：小改动或仅调 API 时，可只维护 requirements，实现时与现有页面结构一致即可。
- Design Input 的完整定位见 `docs/design-principles.md` 第一节与第三节 3.2。

### 步骤 3：实现

- **API**：在 `src/shared/api/` 按 BC 或模块封装，路径与 api.yaml 一致，`/api/...` 由 Vite 代理。
- **路由**：在 `src/router/index.js` 注册 path、name、component（懒加载）、meta.title。
- **页面**：`pages/*Page.vue`；复用块 `shared/ui/`，复杂时可分子目录 atoms / molecules / organisms。
- **展示与错误**：只根据后端返回展示成功或 `e.response?.data?.message`（或 `e.message` /「加载失败」），不写死业务文案。
- **样式**：Tailwind，与现有风格一致（VMALL 主色、顶栏、圆角卡片等，见 design-input 或 design-principles）。

---

## 四、各前端文档位置

| 前端 | 目录 | 需求 | 设计输入 | 开发端口 |
|------|------|------|----------|----------|
| frontend-admin | `frontend-admin/` | `frontend-admin/docs/requirements.md` 或 `docs/frontend-admin/requirements.md` | `frontend-admin/docs/design-input.md` 或 `docs/frontend-admin/design-input.md` | 5173 |
| frontend-web | `frontend-web/` | `docs/frontend-web/requirements.md` | `docs/frontend-web/design-input.md` | 5174 |

实施前确认该前端以哪份 requirements/design-input 为准（应用内 `docs/` 或项目 `docs/frontend-<名>/`），只改一份并保持与实现一致。

---

## 五、第一次加一个页面（速查）

1. 在 requirements 中加一行：path、页面名、功能、API。
2. 在 `shared/api/` 对应模块加请求函数（路径与 api.yaml 一致）。
3. 在 router 中加路由，component 懒加载 `*Page.vue`。
4. 复制同应用已有 `*Page.vue`，改 API 调用与展示字段；错误用 `e.response?.data?.message`。
5. 需要入口时在 AppHeader 或首页加 `router-link`。
6. `npm run dev` 验证。

API 字段查 `docs/bounded-contexts/<对应 BC>/api.yaml`。

---

## 六、可选：v0 作布局参考

v0 生成多为 React；HMall 为 Vue。仅用 v0 的**布局与 Tailwind 类**在 Vue 中复刻，数据与错误处理仍按 requirements 与 api.yaml，不在 v0 里写业务逻辑。

---

## 七、检查清单（做完可对一遍）

- [ ] 已更新该前端的 **requirements.md**（若为新能力或行为变更）
- [ ] 新页面/大改时已更新 **design-input.md**（若存在且范围涉及）
- [ ] 新接口已在 **shared/api** 封装，路径与 api.yaml 一致
- [ ] 路由已注册，错误与成功展示均以后端返回为准
- [ ] 本地 dev 验证通过
