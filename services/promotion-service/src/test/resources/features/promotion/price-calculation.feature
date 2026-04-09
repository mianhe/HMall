# language: zh-CN
@promotion
功能: 价格计算与券生命周期
  计算含券折扣的价格，锁定/核销/释放优惠券。

  背景:
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 3            | 30        |
    并且 管理员向用户 1001 发放 1 张该模板的优惠券

  场景: 使用满减券计算价格
    当 计算价格:
      | userId | couponId | skuId | unitPriceCents | quantity |
      | 1001   | latest   | 101   | 6000           | 1        |
      | 1001   | latest   | 102   | 5000           | 1        |
    那么 返回状态码 200
    并且 原价为 11000
    并且 优惠金额为 2000
    并且 实付金额为 9000

  场景: 不传couponId时价格等于原价
    当 不传券计算价格:
      | userId | skuId | unitPriceCents | quantity |
      | 1001   | 101   | 6000           | 2        |
    那么 返回状态码 200
    并且 原价为 12000
    并且 优惠金额为 0
    并且 实付金额为 12000

  场景: 订单金额未达门槛时返回400
    当 计算价格:
      | userId | couponId | skuId | unitPriceCents | quantity |
      | 1001   | latest   | 101   | 3000           | 1        |
    那么 返回状态码 400

  场景: 查询订单可用优惠券
    当 查询用户 1001 订单金额 11000 的可用优惠券
    那么 返回状态码 200
    并且 返回的优惠券列表长度至少为 1

  场景: 锁定优惠券
    当 锁定该优惠券关联订单 5001
    那么 返回状态码 200
    并且 返回的优惠券状态为 "LOCKED"

  场景: 核销优惠券
    当 锁定该优惠券关联订单 5002
    并且 核销该优惠券
    那么 返回状态码 200
    并且 返回的优惠券状态为 "USED"

  场景: 释放已锁定的优惠券
    当 锁定该优惠券关联订单 5003
    并且 释放该优惠券
    那么 返回状态码 200
    并且 返回的优惠券状态为 "AVAILABLE"

  场景: 释放已核销的优惠券（取消退券）
    当 锁定该优惠券关联订单 5004
    并且 核销该优惠券
    并且 释放该优惠券
    那么 返回状态码 200
    并且 返回的优惠券状态为 "AVAILABLE"

  场景: 定向活动仅对命中用户生效
    当 创建定向活动:
      | name        | targetSkuId | discountCents | levelsIn |
      | L2会员直降  | 101         | 500           | L2       |
    那么 返回状态码 201
    当 不传券计算价格:
      | userId | skuId | unitPriceCents | quantity |
      | 2002   | 101   | 6000           | 1        |
    那么 返回状态码 200
    并且 优惠金额为 500
    并且 实付金额为 5500
    当 不传券计算价格:
      | userId | skuId | unitPriceCents | quantity |
      | 1001   | 101   | 6000           | 1        |
    那么 返回状态码 200
    并且 优惠金额为 0
    并且 实付金额为 6000

  场景: 满件活动在达到件数门槛时生效
    当 创建满件活动:
      | name      | targetSkuId | scopeType | scopeIds | minQuantity | discountType | discountValue |
      | 满3件减300 | 101         | SKU       | 101      | 3           | AMOUNT_OFF   | 300           |
    那么 返回状态码 201
    当 不传券计算价格:
      | userId | skuId | unitPriceCents | quantity |
      | 1001   | 101   | 1000           | 2        |
    那么 返回状态码 200
    并且 优惠金额为 0
    并且 实付金额为 2000
    当 不传券计算价格:
      | userId | skuId | unitPriceCents | quantity |
      | 1001   | 101   | 1000           | 3        |
    那么 返回状态码 200
    并且 优惠金额为 300
    并且 实付金额为 2700
