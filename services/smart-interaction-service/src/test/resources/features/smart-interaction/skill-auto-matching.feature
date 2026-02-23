# language: zh-CN
@skill-auto-matching
功能: Skill 自动匹配
  作为后台管理员，我希望不手动选择 Skill 也能获得精准的 AI 对话体验，
  系统应根据我的消息内容自动匹配相关 Skill 并注入领域知识。

  背景:
    假如 已创建 Skill "商品目录管理助手" 描述 "管理类目、商品、规格、SKU 与展示图" systemPrompt "你是商品目录管理助手。类目是树形结构，商品挂在叶子类目下。" allowedTools "catalog_*" audience "admin"
    并且 已创建 Skill "库存管理助手" 描述 "按 SKU 查询库存水位，初始化或调整可用库存" systemPrompt "你是库存管理助手。库存以 SKU 为粒度，有 available 和 reserved 两个数量。" allowedTools "inventory_*" audience "admin"
    并且 已创建 Skill "购物助手" 描述 "帮助用户搜索商品、浏览类目" systemPrompt "你是购物助手。帮助用户搜索和发现商品。" allowedTools "catalog_categories,catalog_products" audience "consumer"
    并且 MCP 已注册工具 "catalog_categories"
    并且 MCP 已注册工具 "inventory_stock"

  场景: 自动匹配单个 Skill 并注入其 systemPrompt
    假如 LLM 路由会将消息匹配到 Skill "商品目录管理助手"
    并且 LLM 会流式返回文本 "好的，我来帮你查类目"
    当 用户不指定 Skill 发送消息 "帮我查一下有哪些类目"
    那么 发送给 LLM 的 system prompt 应包含 "你是商品目录管理助手"
    并且 发送给 LLM 的 tools 应包含 "catalog_categories"
    并且 发送给 LLM 的 tools 应包含 "inventory_stock"
    并且 SSE 流应包含 skill_matched 事件

  场景: 自动匹配多个 Skill 并合并 systemPrompt
    假如 LLM 路由会将消息匹配到 Skill "商品目录管理助手" 和 "库存管理助手"
    并且 LLM 会流式返回文本 "好的，我来查商品和库存信息"
    当 用户不指定 Skill 发送消息 "帮我看看 SKU 100 的商品信息和库存"
    那么 发送给 LLM 的 system prompt 应包含 "你是商品目录管理助手"
    并且 发送给 LLM 的 system prompt 应包含 "你是库存管理助手"
    并且 发送给 LLM 的 tools 应包含 "catalog_categories"
    并且 发送给 LLM 的 tools 应包含 "inventory_stock"

  场景: 无 Skill 匹配时应使用基础 system prompt 和全量工具
    假如 LLM 路由未匹配到任何 Skill
    并且 LLM 会流式返回文本 "你好，有什么可以帮你的"
    当 用户不指定 Skill 发送消息 "今天天气怎么样"
    那么 发送给 LLM 的 system prompt 应包含 "HMall 智能助手"
    并且 发送给 LLM 的 system prompt 不应包含 "你是商品目录管理助手"
    并且 发送给 LLM 的 tools 应包含 "catalog_categories"
    并且 发送给 LLM 的 tools 应包含 "inventory_stock"

  场景: 手动指定 skillId 时应跳过自动匹配并按 allowedTools 过滤
    假如 LLM 会流式返回文本 "库存查询结果"
    当 用户使用 Skill "库存管理助手" 发送消息 "查一下库存"
    那么 发送给 LLM 的 system prompt 应包含 "你是库存管理助手"
    并且 发送给 LLM 的 tools 应包含 "inventory_stock"
    并且 发送给 LLM 的 tools 不应包含 "catalog_categories"
    并且 SSE 流不应包含 skill_matched 事件

  场景: skillMode 为 none 时应跳过自动匹配使用基础 prompt
    假如 LLM 会流式返回文本 "你好，我是基础助手"
    当 用户以无 Skill 模式发送消息 "你好"
    那么 发送给 LLM 的 system prompt 应包含 "HMall 智能助手"
    并且 发送给 LLM 的 system prompt 不应包含 "你是商品目录管理助手"
    并且 发送给 LLM 的 system prompt 不应包含 "你是库存管理助手"
    并且 发送给 LLM 的 tools 应包含 "catalog_categories"
    并且 发送给 LLM 的 tools 应包含 "inventory_stock"
    并且 SSE 流不应包含 skill_matched 事件

  场景: 消费者端自动匹配应仅在 consumer audience 的 Skill 中路由并按其 allowedTools 过滤工具
    假如 LLM 路由会将消息匹配到 Skill "购物助手"
    并且 LLM 会流式返回文本 "帮你搜索商品"
    当 消费者端用户发送消息 "帮我搜索手机"
    那么 LLM 路由的候选 Skill 应包含 "购物助手"
    并且 LLM 路由的候选 Skill 不应包含 "商品目录管理助手"
    并且 LLM 路由的候选 Skill 不应包含 "库存管理助手"
    并且 发送给 LLM 的 tools 应包含 "catalog_categories"
    并且 发送给 LLM 的 tools 不应包含 "inventory_stock"
    并且 SSE 流应包含 skill_matched 事件

  场景: 消费者端无匹配 Skill 时不应提供任何工具且应注入无工具拒答指令
    假如 LLM 路由未匹配到任何 Skill
    并且 LLM 会流式返回文本 "你好，请问有什么可以帮你的"
    当 消费者端用户发送消息 "今天天气怎么样"
    那么 发送给 LLM 的 tools 不应包含 "catalog_categories"
    并且 发送给 LLM 的 tools 不应包含 "inventory_stock"
    并且 发送给 LLM 的 system prompt 应包含 "当前没有可用工具"
    并且 发送给 LLM 的 system prompt 应包含 "绝对不要编造"

  场景: 消费者端有匹配 Skill 时不应注入无工具拒答指令
    假如 LLM 路由会将消息匹配到 Skill "购物助手"
    并且 LLM 会流式返回文本 "帮你搜索商品"
    当 消费者端用户发送消息 "有什么手机"
    那么 发送给 LLM 的 tools 应包含 "catalog_categories"
    并且 发送给 LLM 的 system prompt 不应包含 "当前没有可用工具"
