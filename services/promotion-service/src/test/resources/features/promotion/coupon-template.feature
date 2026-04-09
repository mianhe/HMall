# language: zh-CN
@promotion
功能: 券模板管理
  管理员创建、查询和停用优惠券模板。

  场景: 创建满减券模板
    当 管理员创建满减券模板:
      | name       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20  | 10000          | 2000          | 1000          | 3            | 30        |
    那么 返回状态码 201
    并且 返回的券模板类型为 "AMOUNT_OFF"
    并且 返回的券模板状态为 "ACTIVE"
    并且 返回的券模板已发放数量为 0

  场景: 创建折扣券模板
    当 管理员创建折扣券模板:
      | name       | thresholdCents | discountRate | maxDiscountCents | totalQuantity | perUserLimit | validDays |
      | 全场85折   | 0              | 0.85         | 5000             | 500           | 1            | 7         |
    那么 返回状态码 201
    并且 返回的券模板类型为 "PERCENTAGE_OFF"

  场景: 查询券模板列表
    假如 已存在以下券模板:
      | name       | type        | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20  | AMOUNT_OFF  | 10000          | 2000          | 1000          | 3            | 30        |
      | 满200减50  | AMOUNT_OFF  | 20000          | 5000          | 500           | 2            | 15        |
    当 查询券模板列表
    那么 返回状态码 200
    并且 返回的券模板数量至少为 2

  场景: 查询单个券模板详情
    假如 已存在以下券模板:
      | name       | type        | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20  | AMOUNT_OFF  | 10000          | 2000          | 1000          | 3            | 30        |
    当 查询该券模板详情
    那么 返回状态码 200
    并且 返回的券模板名称为 "满100减20"

  场景: 停用券模板
    假如 已存在以下券模板:
      | name       | type        | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 满100减20  | AMOUNT_OFF  | 10000          | 2000          | 1000          | 3            | 30        |
    当 停用该券模板
    那么 返回状态码 200
    并且 返回的券模板状态为 "INACTIVE"

  场景: 创建模板时必填字段缺失返回400
    当 管理员创建券模板但缺少名称
    那么 返回状态码 400

  场景: 创建模板时门槛金额为负数返回400
    当 管理员创建满减券模板:
      | name       | thresholdCents | discountCents | totalQuantity | perUserLimit | validDays |
      | 测试负值   | -100           | 2000          | 1000          | 3            | 30        |
    那么 返回状态码 400

  场景: 创建折扣券时折扣率不在有效范围返回400
    当 管理员创建折扣券模板:
      | name       | thresholdCents | discountRate | maxDiscountCents | totalQuantity | perUserLimit | validDays |
      | 测试无效率 | 0              | 1.5          | 5000             | 500           | 1            | 7         |
    那么 返回状态码 400

  场景: 停用不存在的模板返回404
    当 停用ID为 99999 的券模板
    那么 返回状态码 404
