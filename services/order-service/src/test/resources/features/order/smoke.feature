# language: zh-CN
@order
功能: Order 微服务
  占位验收场景。

  场景: Order 服务已就绪
    假如 已存在用户 "alice" 密码 "secret123"
    并且 Catalog 已有 SKU 1 价格 100 分 名称 "测试商品"
    当 用户 "alice" 提交订单 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "测试地址" 购买 skuId 1 数量 1
    那么 订单应创建成功
