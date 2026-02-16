# language: zh-CN
@order
功能: 查询订单
  作为用户，我希望能够按 ID 查询订单详情或按 userId 查询订单列表，以便查看订单信息。

  场景: 按 ID 查询订单时应返回订单详情（含明细、收货地址、status）
    假如 已存在用户 "alice" 密码 "secret123"
    并且 Catalog 已有商品 "iPhone 15" skuId 123 价格 599900 分
    当 用户 "alice" 提交订单 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902" 购买 "iPhone 15" 数量 1
    当 按 orderId 查询该订单
    那么 应返回订单详情
    并且 订单详情包含 orderId、status、items、shippingAddress
    并且 订单 status 为 PENDING_PAYMENT

  场景: 订单不存在时查询应返回 404
    当 按 orderId 查询不存在的订单 999999
    那么 应返回 404

  场景: 按 userId 查询订单列表时应返回该用户的订单（可分页）
    假如 已存在用户 "alice" 密码 "secret123"
    并且 Catalog 已有商品 "iPhone 15" skuId 123 价格 599900 分
    当 用户 "alice" 提交订单 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902" 购买 "iPhone 15" 数量 1
    当 用户 "alice" 提交订单 收货地址 "何勉" "13641793760" "上海" "上海" "浦东新区" "羽山路100弄9号2902" 购买 "iPhone 15" 数量 2
    当 按 userId 查询 "alice" 的订单列表 第 1 页 每页 10 条
    那么 应返回订单列表
    并且 订单列表包含 2 条订单
    并且 列表中的订单包含 orderId、status
