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
