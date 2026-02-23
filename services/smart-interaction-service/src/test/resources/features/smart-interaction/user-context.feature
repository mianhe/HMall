# language: zh-CN
@user-context
功能: 用户上下文注入
  消费者端所有操作必须限定在当前登录用户范围内。
  Smart Interaction 从请求头提取 userId，自动注入到 MCP tool 调用参数中。

  场景: 消费者端对话应将 userId 注入到 tool 调用参数
    假如 已创建 Skill "购物助手" 描述 "购物" systemPrompt "购物助手" allowedTools "cart_manage" audience "consumer"
    并且 MCP 已注册工具 "cart_manage"
    并且 LLM 路由会将消息匹配到 Skill "购物助手"
    并且 LLM 会返回 tool call "cart_manage" 参数 "{\"action\":\"list\"}"
    并且 MCP 执行 "cart_manage" 将返回 "购物车为空"
    并且 LLM 收到工具结果后会回复 "购物车是空的"
    当 消费者端用户 42 发送消息 "看看购物车"
    那么 MCP 收到的 "cart_manage" 调用参数应包含 userId 42
    并且 SSE 流应以 done 事件结束

  场景: 未认证消费者端用户发起对话应返回 401
    当 未认证消费者端用户发送消息 "查看我的购物车"
    那么 响应状态码应为 401

  场景: 管理后台对话不注入 userId
    假如 MCP 已注册工具 "inventory_stock"
    并且 LLM 会返回 tool call "inventory_stock" 参数 "{\"action\":\"list\"}"
    并且 MCP 执行 "inventory_stock" 将返回 "库存列表"
    并且 LLM 收到工具结果后会回复 "以下是库存列表"
    当 用户发送消息 "查看库存"
    那么 MCP 收到的 "inventory_stock" 调用参数不应包含 userId
    并且 SSE 流应以 done 事件结束
