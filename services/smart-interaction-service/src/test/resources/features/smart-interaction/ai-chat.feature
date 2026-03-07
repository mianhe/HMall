# language: zh-CN
@ai-chat
功能: AI 对话
  作为后台管理员，我希望通过自然语言与系统对话，系统能够调用工具获取真实数据并返回结果。

  场景: 纯文本对话应通过 SSE 流式返回
    假如 LLM 会流式返回文本 "你好，我是 HMall 助手"
    当 用户发送消息 "你好"
    那么 SSE 流应包含 delta 事件
    并且 拼接所有 delta 内容为 "你好，我是 HMall 助手"
    并且 SSE 流应以 done 事件结束

  场景: 单轮 Tool Call 应调用 MCP 工具并返回结果
    假如 MCP 已注册工具 "catalog_list_categories"
    并且 LLM 会返回 tool call "catalog_list_categories" 参数 "{}"
    并且 MCP 执行 "catalog_list_categories" 将返回 "手机, 平板, 笔记本"
    并且 LLM 收到工具结果后会回复 "共有 3 个根类目"
    当 用户发送消息 "有哪些类目"
    那么 SSE 流应包含 tool_call 事件 工具名为 "catalog_list_categories"
    并且 SSE 流应包含 tool_result 事件 工具名为 "catalog_list_categories"
    并且 拼接所有 delta 内容为 "共有 3 个根类目"
    并且 SSE 流应以 done 事件结束

  场景: 多轮 Tool Call 应逐轮执行并最终返回结果
    假如 MCP 已注册工具 "catalog_list_categories"
    并且 MCP 已注册工具 "catalog_list_products"
    并且 LLM 第 1 轮会返回 tool call "catalog_list_categories" 参数 "{}"
    并且 MCP 执行 "catalog_list_categories" 将返回 "[{id:1,name:手机}]"
    并且 LLM 第 2 轮会返回 tool call "catalog_list_products" 参数 "{\"categoryId\":1}"
    并且 MCP 执行 "catalog_list_products" 将返回 "[{id:101,name:Mate70}]"
    并且 LLM 第 3 轮会回复 "手机类目下有 1 款商品：Mate70"
    当 用户发送消息 "手机类目下有什么商品"
    那么 SSE 流应包含 tool_call 事件 工具名为 "catalog_list_categories"
    并且 SSE 流应包含 tool_call 事件 工具名为 "catalog_list_products"
    并且 拼接所有 delta 内容为 "手机类目下有 1 款商品：Mate70"
    并且 SSE 流应以 done 事件结束

  场景: 查询可用模型列表
    当 用户请求可用模型列表
    那么 应返回模型列表包含 "qwen"

  场景: 指定 skillId 时应加载 Skill 的 systemPrompt 并过滤工具
    假如 已创建 Skill "库存助手" systemPrompt "你是库存管理专家" allowedTools "inventory_*"
    并且 MCP 已注册工具 "catalog_list_categories"
    并且 MCP 已注册工具 "inventory_query_stock"
    并且 LLM 会返回 tool call "inventory_query_stock" 参数 "{\"skuId\":1}"
    并且 MCP 执行 "inventory_query_stock" 将返回 "库存: 100"
    并且 LLM 收到工具结果后会回复 "SKU 1 的库存为 100"
    当 用户使用 Skill "库存助手" 发送消息 "查一下 SKU 1 的库存"
    那么 发送给 LLM 的 system prompt 应包含 "你是库存管理专家"
    并且 发送给 LLM 的 tools 应包含 "inventory_query_stock"
    并且 发送给 LLM 的 tools 不应包含 "catalog_list_categories"
    并且 拼接所有 delta 内容为 "SKU 1 的库存为 100"

  场景: 未指定 skillId 时应使用默认 Skill
    假如 已创建默认 Skill "通用助手" systemPrompt "你是 HMall 通用助手"
    并且 LLM 会流式返回文本 "我是通用助手"
    当 用户发送消息 "你好"
    那么 发送给 LLM 的 system prompt 应包含 "你是 HMall 通用助手"

  场景: 无默认 Skill 时应使用基础 system prompt
    假如 系统中无任何 Skill
    并且 LLM 会流式返回文本 "你好"
    当 用户发送消息 "你好"
    那么 发送给 LLM 的 system prompt 应包含 "HMall"

  场景: 手动指定 Skill 时 system prompt 应同时包含 base prompt 和 Skill 领域知识
    假如 已创建 Skill "库存助手" systemPrompt "库存以 SKU 为粒度" allowedTools "inventory_*"
    并且 MCP 已注册工具 "inventory_query_stock"
    并且 LLM 会流式返回文本 "好的"
    当 用户使用 Skill "库存助手" 发送消息 "查一下库存"
    那么 发送给 LLM 的 system prompt 应包含 "HMall"
    并且 发送给 LLM 的 system prompt 应包含 "以下是当前对话匹配到的操作指引与示例"
    并且 发送给 LLM 的 system prompt 应包含 "库存以 SKU 为粒度"

  场景: Tool Call 轮次超过限制时应终止并提示
    假如 已设置 maxToolCallRounds 为 2
    并且 MCP 已注册工具 "catalog_list_categories"
    并且 LLM 每轮都会返回 tool call "catalog_list_categories" 参数 "{}"
    并且 MCP 执行 "catalog_list_categories" 将返回 "数据"
    当 用户发送消息 "不断查询"
    那么 SSE 流应包含 error 事件
