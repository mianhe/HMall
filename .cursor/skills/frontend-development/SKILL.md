---
name: frontend-development
description: Implements or extends the HMall admin frontend using Vue 3, Vite, FSD-like structure, and Atomic Design. Uses design input doc for pages/entities/components; API in shared/api; UI shows backend responses without duplicating business rules. Use when adding frontend pages, new UI features, Vue components, or when the user asks for frontend development or 前端开发.
---

# 前端开发（HMall 管理后台）

前端以**展示为主**，增删改由 MCP 完成。在现有 Vue 3 + Vite 前端上增改页面与功能，按设计输入与分层实现，前端不重复后端业务规则。

## 技术栈与结构

- **栈**：Vue 3（Composition API）、Vite、Vue Router、Tailwind CSS、axios。
- **目录**：`frontend/src/` 下 `shared/`（api、ui）、`pages/`、`router/`。API 封装在 `shared/api/`，与后端契约一致（见 `docs/bounded-contexts/catalog/api.yaml`）。
- **代理**：开发时 Vite 将 `/api` 代理到 `http://localhost:8080`（见 `vite.config.js`）。

## 设计输入

- **位置**：`docs/frontend/design-input.md`。
- **内容**：FSD（实体、页面/路由、功能）、Atomic（atoms/molecules/organisms）、每页结构。
- **用法**：加新页面或改交互前，先看或更新设计输入；实现时按其中的页面、实体、组件层级来写。

## 开发流程（加新页面/功能时）

1. **更新设计输入（若需要）**  
   在 `docs/frontend/design-input.md` 中补充或修改：新路由、新功能、新组件（含 Atomic 层级）。若用户有偏好（如弹窗 vs 独立页），先更新文档再实现。

2. **API 封装**  
   若后端有新接口，在 `shared/api/` 下对应文件（如 `catalog.js`）增加方法，请求路径与 `catalog-api.yaml` 一致。使用相对路径 `/api/...`，由 Vite 代理转发。

3. **路由**  
   在 `src/router/index.js` 中增加路由：path、name、component（懒加载 `() => import('...')`）、meta.title。

4. **页面与组件**  
   - 新页面放在 `pages/`，以 `*Page.vue` 命名；复用块放在 `shared/ui/` 或按 Atomic 层级组织。
   - 页面内：调用 `shared/api` 方法，用 ref/reactive 存数据，错误时展示 **后端返回的 message**（如 `e.response?.data?.message`），不在前端写业务规则文案。
   - 列表/表单：成功后再跳转或刷新列表；失败只展示接口返回信息。

5. **样式**  
   参考华为商城 VMALL：主色 `vmall-red`、背景 `vmall-gray-bg`、文字 `vmall-gray-text`；顶栏红底白字；主按钮红底白字；简洁、留白充足。

6. **验证**  
   本地运行：先起后端与数据库，再在 `frontend` 下 `npm run dev`，在浏览器中验证新页面与接口调用。

## 原则

- **不重复业务规则**：校验、错误提示以后端为准；前端只发请求并展示 `response.data` 或 `error.response?.data?.message`。
- **与契约一致**：请求/响应结构与 OpenAPI YAML 对齐；新增接口时同步在 `shared/api` 与设计输入中体现。
- **FSD/Atomic**：新功能落在合适的层（shared/ui、pages、或新 organism）；组件命名与设计输入中的清单一致或更新清单。

## 检查清单

- [ ] 设计输入已更新（新路由、功能、组件）
- [ ] 新接口已在 `shared/api` 中封装
- [ ] 路由已注册且 meta.title 正确
- [ ] 页面/组件只根据后端返回展示成功或错误，无重复业务逻辑
- [ ] 样式使用 Tailwind，与现有页面一致
- [ ] 本地 dev 下功能与接口行为正确

## 参考

- 设计输入与引导说明：`docs/frontend/design-input.md`
- 后端 API 契约：`docs/bounded-contexts/catalog/api.yaml`
- 现有页面示例：`frontend/src/pages/CatalogPage.vue`（目标）；API 示例：`frontend/src/shared/api/catalog.js`。
