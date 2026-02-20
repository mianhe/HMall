# language: zh-CN
@cart
功能: Cart 冒烟测试
  验证 Cart 上下文可正常启动。

  场景: Cart 服务已就绪
    假如 用户已登录，userId 为 1
    当 用户查询购物车
    那么 应返回 200
    并且 返回 0 个购物车项
