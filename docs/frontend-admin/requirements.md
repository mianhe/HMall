# 前端功能需求 — 管理后台（frontend-admin）

管理后台以**展示为主**，增删改由 MCP 完成。后端需求见 `docs/bounded-contexts/catalog/requirements.md`。

---

## 一、定位

| 端 | 职责 |
|----|------|
| **管理后台（frontend-admin）** | 查看、刷新；不做增删改 |
| **MCP** | 类目/商品/规格/SKU 的增删改 |

---

## 二、页面结构

**单页分层展示**：一个页面呈现类目 → 商品（含规格维度）→ SKU 的完整层级。

| 路由 | 页面 | 功能 |
|------|------|------|
| `/` | HomePage | 入口，导航到 Catalog 浏览 |
| `/catalog` | CatalogPage | **一页全览**：类目 → 商品（含 dimension）→ SKU 分层展示 |

---

## 三、Catalog 展示

### 3.1 层级结构

```
[类目 A]
  商品 1（规格维度：颜色、容量）
    SKU：黑色 128G ¥99
    SKU：白色 256G ¥129
  商品 2（规格维度：尺寸）
    SKU：S ¥50
    SKU：M ¥55
[类目 B]
  商品 3
    SKU：...
```

- **类目**：可展开/折叠，展示子类目；叶子类目下挂商品
- **商品**：展示名称、描述、规格维度（dimension 名称列表）
- **SKU**：展示规格组合、价格、展示名

### 3.2 交互

- 支持展开/折叠各级；
- 支持刷新按钮，重新拉取数据；
- 无增删改操作。

### 3.3 后端 API

使用现有接口组装数据：

- `GET /api/categories`（递归按 parentId 获取类目树）
- `GET /api/products?categoryId=:id`（叶子类目下的商品）
- `GET /api/products/:id`、`/dimensions`、`/skus`（商品详情、规格、SKU）

数据量小时，前端多次调用即可；若需优化，可后续增加聚合接口。

---

## 四、系统设置

### 4.1 页面

| 路由 | 页面 | 功能 |
|------|------|------|
| `/settings` | SettingsPage | 系统级配置项，按分类展示；当前包含「支付设置」 |

### 4.2 支付设置

| 配置项 | 说明 | API |
|--------|------|-----|
| 支付超时时间（分钟） | 整数，最小 1，默认 30 | `GET /api/payments/settings` → `{ expireMinutes }` |
|  |  | `PUT /api/payments/settings` ← `{ expireMinutes }` |

- 页面加载时读取当前值，编辑后点「保存」提交
- 成功展示「保存成功」提示；失败展示后端返回的 message

---

## 五、活动监控

### 5.1 页面

| 路由 | 页面 | 功能 |
|------|------|------|
| `/activity` | ActivityPage | 活动监控仪表盘：统计卡片 + 最近事件流水 |

### 5.2 功能

| 功能 | 说明 | API |
|------|------|-----|
| 统计卡片 | 订单概览（3 指标）、支付概览（4 指标）、库存活动（2 指标） | `GET /api/activities/stats?period=today\|last7\|last30` 或 `?from=&to=` |
| 时间范围切换 | 今日 / 最近7天 / 最近30天 快捷选择 + 自定义日期 | 同上 |
| 最近活动流水 | 最近事件列表（时间、事件类型、订单 ID、Topic） | `GET /api/activities/recent?limit=20` |
| 自动刷新 | 页面加载时默认查今日统计 + 最近活动 | — |

---

## 六、AI 智能对话

### 6.1 全局对话窗

AI Chat 以全局 Drawer 形式存在，从右侧滑出，覆盖在当前页面之上，所有路由页面共享同一个对话实例。

| 功能 | 说明 | API |
|------|------|-----|
| 呼出/关闭 | 右下角浮动按钮点击，或快捷键 Ctrl+K / Cmd+K | — |
| 流式对话 | SSE 流式接收 LLM 回复，逐 token 渲染 | `POST /api/ai/chat` (SSE) |
| Tool Call 展示 | 折叠卡片展示工具名、参数、状态、执行结果 | — |
| Markdown 渲染 | 支持表格、列表等（marked + DOMPurify） | — |
| 结构化回复 | 区分思考过程（可折叠）、正式结论、后续建议 | — |
| 流式中断 | 清空对话时中断进行中的 SSE 流 | — |
| 模型切换 | 可选择不同 LLM 提供商（千问、DeepSeek 等） | `GET /api/ai/models` |

### 6.2 组件结构

```
src/shared/ui/ai-chat/
├── AiChatPanel.vue          # Drawer 主面板
├── AiChatButton.vue         # 右下角浮动按钮
├── AiMessageList.vue        # 消息列表（滚动容器）
├── AiMessageBubble.vue      # 单条消息（区分 user/assistant/tool）
├── AiToolCallCard.vue       # Tool Call 可折叠展示卡片
└── AiChatInput.vue          # 输入框 + 发送按钮

src/shared/composables/
└── useAiChat.js             # 核心逻辑：SSE 连接、消息管理、状态

src/shared/api/
└── ai.js                    # AI API 封装
```

后端需求见 `docs/bounded-contexts/smart-interaction/requirements.md`。技术设计见 `docs/architecture/ai-chat.md`。

---

## 七、相关文档

| 文档 | 用途 |
|------|------|
| `docs/bounded-contexts/catalog/requirements.md` | 后端需求与 .feature |
| `docs/bounded-contexts/smart-interaction/requirements.md` | Smart Interaction 需求（AI Chat 后端） |
| `docs/architecture/ai-chat.md` | AI Chat 技术设计（LLM + MCP） |
| `docs/bounded-contexts/activity/api.yaml` | Activity BC API 契约 |
| `docs/frontend-admin/design-input.md` | 管理后台设计输入（FSD + Atomic、组件结构） |
| `docs/frontend-web/requirements.md` | 消费者端需求 |
