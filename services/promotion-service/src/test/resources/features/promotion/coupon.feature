# language: zh-CN
@promotion
功能: 优惠券管理
  管理员发券、用户领券、查询和过期处理。

  场景: 管理员向用户发放满减券
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 3            | 30        |
    当 管理员向用户 1001 发放 1 张该模板的优惠券
    那么 返回状态码 200
    并且 返回的优惠券列表长度为 1
    并且 返回的第一张优惠券状态为 "AVAILABLE"
    并且 返回的第一张优惠券用户ID为 1001

  场景: 用户领取优惠券
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 3            | 30        |
    当 用户 2001 领取该模板的优惠券
    那么 返回状态码 200
    并且 返回的优惠券状态为 "AVAILABLE"

  场景: 查询用户的优惠券列表
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 3            | 30        |
    并且 管理员向用户 3001 发放 2 张该模板的优惠券
    当 查询用户 3001 的优惠券列表
    那么 返回状态码 200
    并且 返回的优惠券列表长度为 2

  场景: 按状态查询用户的优惠券
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 3            | 30        |
    并且 管理员向用户 4001 发放 1 张该模板的优惠券
    当 查询用户 4001 状态为 AVAILABLE 的优惠券
    那么 返回状态码 200
    并且 返回的优惠券列表长度为 1

  场景: 查询可领券模板列表
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 3            | 30        |
    当 查询用户 5001 的可领券模板列表
    那么 返回状态码 200
    并且 返回的可领券模板数量至少为 1

  场景: 领券时模板已停用返回400
    假如 已存在以下已停用券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 3            | 30        |
    当 用户 6001 领取该模板的优惠券
    那么 返回状态码 400

  场景: 领券超过每人限领数量返回400
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 1            | 30        |
    并且 用户 7001 已领取该模板 1 张优惠券
    当 用户 7001 领取该模板的优惠券
    那么 返回状态码 400

  场景: 库存不足时领券返回400
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1             | 3            | 30        |
    并且 管理员向用户 8001 发放 1 张该模板的优惠券
    当 用户 8002 领取该模板的优惠券
    那么 返回状态码 400

  场景: 过期优惠券自动失效
    假如 已存在以下活跃券模板:
      | name      | type       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20 | AMOUNT_OFF | 10000          | 2000          | 1000          | 3            | 1         |
    并且 管理员向用户 9001 发放了一张已过期的优惠券
    当 执行优惠券过期扫描
    那么 用户 9001 的该优惠券状态变为 "EXPIRED"
