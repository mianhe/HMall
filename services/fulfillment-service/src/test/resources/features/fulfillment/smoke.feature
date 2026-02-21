# language: zh-CN
@fulfillment
功能: Fulfillment 冒烟测试
  验证 Fulfillment 上下文可正常启动。

  场景: Fulfillment 服务已就绪
    当 请求 Fulfillment 健康检查接口
    那么 应返回 200
