# HMall MCP Tools 说明

本文档描述 HMall MCP Server 提供的全部 tools，按 BC 分组。

- **面向**：使用 MCP Client（如 Cursor、Claude Desktop）的用户与 AI Agent。
- **后端**：调用 HMall 各 service 的 REST API（默认 `http://localhost:8080/api`）。

---

## 工具总览（16 个，均已实现）

### Catalog（8 个）

| 工具名 | 用途 |
|--------|------|
| `catalog_categories` | 类目：列表 / 树 / 详情 / 创建 / 修改 / 删除 |
| `catalog_products` | 商品(SPU)：列表或搜索 / 详情(基础或完整) / 创建(含 productType) / 修改 / 删除 |
| `catalog_dimensions` | 规格维度与选项：列表 / 添加维度 / 添加选项 / 删除选项 |
| `catalog_skus` | SKU：列表 / 创建 / 修改 / 删除 |
| `catalog_service_bindings` | 服务绑定：列表 / 创建 / 删除（将服务 SKU 绑定到实体 SPU） |
| `catalog_upload_image` | 上传本地图片，返回可访问 URL |
| `catalog_product_images` | 产品级展示图：列表 / 添加(支持 URL 或本地路径) / 删除 |
| `catalog_option_images` | 选项级展示图：列表 / 添加(支持 URL 或本地路径) / 删除 |

### Inventory（1 个）

| 工具名 | 用途 |
|--------|------|
| `inventory_stock` | 库存水位：按 skuId 查询（可用/已占用）/ 初始化或更新可用库存 |

> **设计说明**：Inventory BC 的占用（occupy）和释放（release）API 属于系统协调接口（仅由 Order BC 在 Saga 流程中调用），不暴露为 MCP tool，以避免绕过 Order 的一致性保证。

### Cart（1 个）

| 工具名 | 用途 |
|--------|------|
| `cart_manage` | 购物车：查看 / 加购 / 修改数量 / 删除 / 结算预览 |

### Order（2 个）

| 工具名 | 用途 |
|--------|------|
| `order_query` | 订单查询与管理：按 ID 查详情 / 按用户查订单列表 / 取消订单 |
| `order_create` | 创建订单：提交商品和收货地址下单 |

### Fulfillment（1 个）

| 工具名 | 用途 |
|--------|------|
| `fulfillment_orders` | 履约单：查询详情 / 列表筛选 / 配货 / 发货 / 签收确认 |

> **设计说明**：Fulfillment BC 的创建（create）和取消（cancel）API 属于系统协调接口（仅由 Order BC 在下单/取消流程中调用），不暴露为 MCP tool，以避免产生「有履约单无对应订单」的脏数据。

### User（2 个）

| 工具名 | 用途 |
|--------|------|
| `user_manage` | 用户管理：列表 / 详情 / 创建用户 |
| `user_addresses` | 收货地址：列表 / 详情 / 新增 / 修改 / 删除 |

> **设计说明**：User BC 的登录（`POST /api/login`）API 属于认证流程，由前端处理，不暴露为 MCP tool。AI 不应代替用户登录获取 token。

### Activity（1 个）

| 工具名 | 用途 |
|--------|------|
| `activity_query` | 业务活动：活动列表 / 最近活动 / 统计仪表盘（订单、支付、履约、库存概览） |

> **userId 注入**：`cart_manage`、`order_query`（list）、`order_create` 需要用户身份。userId 由 Smart Interaction 服务端从认证信息中提取后自动注入到 tool 调用参数中（决策 S10），前端和 LLM 均不直接传递 userId。

---

## 1. catalog_categories

类目（分类）的查询与维护。支持树形结构：根类目无 `parentId`，子类目传 `parentId`。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `tree` \| `get` \| `create` \| `update` \| `delete` |
| `parentId` | number | 否 | **list** 时：不传查根类目，传则查该父类目下子类目 |
| `categoryId` | number | action 为 get/update/delete 时必填 | 类目 ID |
| `name` | string | create/update 时必填 | 类目名称 |
| `description` | string | 否 | 类目描述 |

**示例**

- 查根类目：`action=list`
- 查某类目下子类目：`action=list`, `parentId=2`
- 查完整类目树：`action=tree`
- 查类目详情：`action=get`, `categoryId=2`
- 创建根类目：`action=create`, `name=数码`
- 创建子类目：`action=create`, `name=手机`, `parentId=1`
- 修改：`action=update`, `categoryId=2`, `name=新名称`
- 删除：`action=delete`, `categoryId=2`

---

## 2. catalog_products

商品（SPU）的查询与维护。商品挂在叶子类目下。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `get` \| `create` \| `update` \| `delete` |
| `categoryId` | number | 否 | **list** 时：按类目过滤；不传则与 keyword 二选一或返回全部（依实现） |
| `keyword` | string | 否 | **list** 时：按关键词搜索商品名称（跨类目模糊匹配） |
| `productId` | number | get/update/delete 时必填 | 商品 ID |
| `detail` | string | 否 | **get** 时：`basic` 仅基础信息，`full` 含规格维度、选项及 SKU；默认可为 basic |
| `name` | string | create/update 时必填 | 商品名称 |
| `description` | string | 否 | 商品描述 |
| `productType` | string | 否 | **create** 时：`PHYSICAL`（默认）或 `SERVICE`（服务类商品） |

**示例**

- 按类目查列表：`action=list`, `categoryId=3`
- 按关键词搜索：`action=list`, `keyword=手机`
- 查商品详情（含规格与 SKU）：`action=get`, `productId=1`, `detail=full`
- 创建：`action=create`, `categoryId=3`, `name=iPhone 16`
- 修改：`action=update`, `productId=1`, `name=新名称`
- 删除：`action=delete`, `productId=1`

---

## 3. catalog_dimensions

某 SPU 的规格维度及选项（如「颜色」「容量」）。创建 SKU 时需指定各维度的选项 ID。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `add_dimension` \| `add_option` \| `delete_option` |
| `spuId` | number | 是 | 商品(SPU) ID |
| `dimensionId` | number | add_option/delete_option 时必填 | 维度 ID |
| `optionId` | number | delete_option 时必填 | 选项 ID |
| `name` | string | add_dimension 时必填 | 维度名称，如「颜色」「容量」 |
| `required` | boolean | add_dimension 时必填 | 创建 SKU 时是否必选该维度 |
| `optionValue` | string | add_option 时必填 | 选项值，如「128G」「黑色」 |
| `sortOrder` | number | 否 | add_option 时排序（越小越靠前） |

**示例**

- 查维度与选项：`action=list`, `spuId=1`
- 添加维度：`action=add_dimension`, `spuId=1`, `name=容量`, `required=true`
- 为维度添加选项：`action=add_option`, `spuId=1`, `dimensionId=1`, `optionValue=128G`
- 删除选项：`action=delete_option`, `spuId=1`, `dimensionId=1`, `optionId=2`

---

## 4. catalog_skus

某 SPU 下 SKU 的查询与维护。SKU 由各规格维度的选项组合 + 价格（及可选展示名）构成。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `create` \| `update` \| `delete` |
| `spuId` | number | 是 | 商品(SPU) ID |
| `skuId` | number | update/delete 时必填 | SKU ID |
| `specOptionIds` | number[] | create 时必填 | 各必填维度所选选项 ID 列表 |
| `priceCents` | number | create 时必填；update 时可选 | 价格，单位：分 |
| `displayName` | string | 否 | 展示名 |

**示例**

- 查某 SPU 下所有 SKU：`action=list`, `spuId=1`
- 创建 SKU：`action=create`, `spuId=1`, `specOptionIds=[1,3]`, `priceCents=599900`
- 改价格：`action=update`, `spuId=1`, `skuId=5`, `priceCents=549900`
- 删除：`action=delete`, `spuId=1`, `skuId=5`

---

## 5. catalog_upload_image

上传本地图片文件到服务器，返回可访问的 URL。该 URL 可用于「添加展示图」等接口的 `imageUrl` 参数。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `localPath` | string | 是 | 本地图片文件的绝对路径 |

**示例**

- `localPath=/Users/me/photo.png` → 返回 `{ url: "https://..." }` 或文本中包含 URL。

---

## 6. catalog_product_images

产品级展示图（不绑定到具体规格选项）。添加时支持直接传 URL 或本地路径（传本地路径时内部先上传再关联）。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `add` \| `delete` |
| `spuId` | number | 是 | 商品(SPU) ID |
| `imageUrl` | string | add 时与 localPath 二选一 | 图片 URL（可来自 catalog_upload_image） |
| `localPath` | string | add 时与 imageUrl 二选一 | 本地图片路径（内部先上传再添加） |
| `sortOrder` | number | 否 | 排序，越小越靠前 |
| `imageId` | number | delete 时必填 | 展示图 ID |

**示例**

- 查产品级图：`action=list`, `spuId=1`
- 用 URL 添加：`action=add`, `spuId=1`, `imageUrl=https://...`
- 用本地文件添加：`action=add`, `spuId=1`, `localPath=/path/to/img.jpg`
- 删除：`action=delete`, `spuId=1`, `imageId=2`

---

## 7. catalog_option_images

为某规格维度的某选项绑定展示图（如「颜色：黑色」对应一张图）。添加时支持 URL 或本地路径。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `add` \| `delete` |
| `spuId` | number | 是 | 商品(SPU) ID |
| `dimensionId` | number | 是 | 维度 ID |
| `optionId` | number | 是 | 选项 ID |
| `imageUrl` | string | add 时与 localPath 二选一 | 图片 URL |
| `localPath` | string | add 时与 imageUrl 二选一 | 本地图片路径 |
| `sortOrder` | number | 否 | 排序 |
| `imageId` | number | delete 时必填 | 展示图 ID |

**示例**

- 查某选项的图：`action=list`, `spuId=1`, `dimensionId=1`, `optionId=2`
- 添加：`action=add`, `spuId=1`, `dimensionId=1`, `optionId=2`, `localPath=/path/to/black.jpg`
- 删除：`action=delete`, `spuId=1`, `dimensionId=1`, `optionId=2`, `imageId=1`

---

## 8. catalog_service_bindings

服务绑定(ServiceBinding)管理。将 SERVICE 类型商品的 SKU 绑定到 PHYSICAL 类型商品(SPU)，支持三种定价模式：

1. **无 binding**：服务独立售卖，价格 = SKU.priceCents
2. **binding + priceCents 为空**：限定适用范围，继承 SKU 标准价
3. **binding + priceCents 非空**：上下文定价，覆盖 SKU 标准价

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `create` \| `delete` |
| `skuId` | number | 是 | 服务 SKU ID |
| `bindingId` | number | delete 时必填 | 绑定 ID |
| `targetSpuId` | number | create 时必填 | 绑定到的实体商品(SPU) ID |
| `priceCents` | number \| null | 否 | create 时可选：绑定价格（分），null 表示继承 SKU 标准价 |

**示例**

- 查某服务 SKU 的绑定：`action=list`, `skuId=10`
- 创建绑定（指定价格）：`action=create`, `skuId=10`, `targetSpuId=1`, `priceCents=29900`
- 创建绑定（继承标准价）：`action=create`, `skuId=10`, `targetSpuId=2`
- 删除绑定：`action=delete`, `skuId=10`, `bindingId=5`

---

## 典型流程简述

1. **类目**：`catalog_categories` 建树（根 → 子 → 叶子）。
2. **商品**：在叶子类目下 `catalog_products` create；需要规格时用 `catalog_dimensions` 添加维度和选项。
3. **SKU**：用 `catalog_dimensions` list 拿到选项 ID，再用 `catalog_skus` create 组合选项与价格。
4. **展示图**：用 `catalog_upload_image` 得到 URL，或用 `catalog_product_images` / `catalog_option_images` 的 add + `localPath` 一步完成；list/delete 按需调用。

---

## 环境与依赖

- 需 **catalog-service** 已启动，默认 API 基地址：`http://localhost:8080/api`，可通过环境变量 `HMALL_API_BASE` 覆盖。
- **上传图像**（`catalog_upload_image` 及展示图 add 的 localPath）：依赖后端 **MinIO**。须在 catalog-service 中配置 `minio.enabled=true`（默认已为 true），并确保 MinIO 服务（如 `http://localhost:9000`）已启动，否则 `/api/files/upload` 不会注册或会报错。

---

# Inventory Tools

## 8. inventory_stock

库存水位的查询与管理。面向管理后台的库存运营场景。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `get` \| `update` |
| `skuId` | number | get/update 时必填 | SKU ID |
| `available` | number | update 时必填 | 可用数量，≥0 |

**action 说明**

- `list`：查询全部库存列表，返回每个 SKU 的可用/已占用数量及名称，附带汇总合计。无记录时提示为空。
- `get`：查询单个 SKU 的可用数量（available）和已占用数量（reserved）。不存在时返回 404。
- `update`：初始化或更新可用库存。不存在则创建（reserved=0），存在则仅更新 available。

**示例**

- 查全部库存：`action=list`
- 查单个库存：`action=get`, `skuId=100`
- 设置库存为 500：`action=update`, `skuId=100`, `available=500`

---

---

# Cart Tools

## 9. cart_manage

购物车管理。所有操作需要 userId（由系统自动注入）。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `add` \| `update_quantity` \| `remove` \| `checkout_preview` |
| `userId` | number | 系统注入 | 用户 ID（Smart Interaction 自动注入，无需手动传递） |
| `skuId` | number | add 时必填 | SKU ID |
| `quantity` | number | add/update_quantity 时必填 | 数量（add ≥1；update_quantity 中 0 = 删除） |
| `cartItemId` | number | update_quantity/remove 时必填 | 购物车项 ID |
| `cartItemIds` | number[] | checkout_preview 时必填 | 选中的购物车项 ID 列表 |

**action 说明**

- `list`：查看当前用户购物车，返回商品名、数量、单价。
- `add`：添加商品到购物车。同一 SKU 重复添加会累加数量。
- `update_quantity`：修改购物车项数量。quantity=0 时删除该项。
- `remove`：删除单个购物车项。
- `checkout_preview`：结算预览，返回选中商品的明细和总价。

**示例**

- 查看购物车：`action=list`
- 加购：`action=add`, `skuId=100`, `quantity=2`
- 改数量：`action=update_quantity`, `cartItemId=5`, `quantity=3`
- 删除：`action=remove`, `cartItemId=5`
- 结算预览：`action=checkout_preview`, `cartItemIds=[5,6]`

---

# Order Tools

## 10. order_query

订单查询与管理。按 ID 查询详情、按用户查询订单列表、取消订单。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `get` \| `list` \| `cancel` |
| `userId` | number | list 时系统注入 | 用户 ID（系统自动注入） |
| `orderId` | number | get/cancel 时必填 | 订单 ID |
| `page` | number | 否 | list 时分页页码，从 0 开始，默认 0 |
| `size` | number | 否 | list 时每页条数，默认 20 |

**action 说明**

- `get`：按订单 ID 查询详情，返回状态、商品明细、收货地址。
- `list`：查询当前用户订单列表，支持分页。
- `cancel`：取消订单。仅待付款（PENDING_PAYMENT）、已付款（PAID）、配货中（FULFILLING）状态可取消；已发货或已签收状态取消会返回错误。

**示例**

- 查订单详情：`action=get`, `orderId=42`
- 查用户订单列表：`action=list`
- 取消订单：`action=cancel`, `orderId=42`

## 11. order_create

创建订单。提交商品列表和收货地址完成下单。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | number | 系统注入 | 用户 ID（系统自动注入） |
| `items` | array | 是 | 购买商品列表，每项含 `skuId`(number) 和 `quantity`(number, ≥1) |
| `shippingAddress` | object | 是 | 收货地址，含 `recipientName`, `phone`, `province`, `city`, `district`, `detail` |

**示例**

- 下单：`items=[{skuId:100, quantity:1}]`, `shippingAddress={recipientName:"张三", phone:"13800138000", province:"广东省", city:"深圳市", district:"南山区", detail:"科技园路1号"}`

---

---

# Fulfillment Tools

## 12. fulfillment_orders

履约单的查询与操作推进。面向管理后台的仓储运营场景。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `get` \| `list` \| `allocate` \| `ship` \| `deliver` |
| `fulfillmentOrderId` | number | get/allocate/ship/deliver 时必填 | 履约单 ID |
| `orderId` | number | 否 | list 时可选，按订单 ID 筛选 |
| `status` | string | 否 | list 时可选，按状态筛选（CREATED/ALLOCATING/SHIPPED/DELIVERED/CANCELLED） |
| `carrier` | string | ship 时必填 | 物流公司名称 |
| `trackingNumber` | string | ship 时必填 | 物流单号 |

**action 说明**

- `get`：按履约单 ID 查询详情，返回状态、商品明细、收货地址、物流信息。
- `list`：查询履约单列表，可按 orderId 和/或 status 筛选。无参数时返回全部。
- `allocate`：配货，将状态从「待配货」推进到「配货中」。
- `ship`：发货，需提供物流公司和物流单号，将状态从「配货中」推进到「已发货」。
- `deliver`：签收确认，将状态从「已发货」推进到「已签收」。

**示例**

- 查履约单详情：`action=get`, `fulfillmentOrderId=1`
- 按订单查履约单：`action=list`, `orderId=42`
- 查所有待配货的履约单：`action=list`, `status=CREATED`
- 配货：`action=allocate`, `fulfillmentOrderId=1`
- 发货：`action=ship`, `fulfillmentOrderId=1`, `carrier=顺丰速运`, `trackingNumber=SF1234567890`
- 签收：`action=deliver`, `fulfillmentOrderId=1`

---

---

# User Tools

## 13. user_manage

用户管理。查看用户列表、用户详情、创建用户。不含登录（认证由前端处理）。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `get` \| `create` |
| `userId` | number | get 时必填 | 用户 ID |
| `username` | string | create 时必填 | 用户名（全局唯一） |
| `password` | string | create 时必填 | 密码 |

**示例**

- 查用户列表：`action=list`
- 查用户详情：`action=get`, `userId=1`
- 创建用户：`action=create`, `username=zhangsan`, `password=123456`

---

## 14. user_addresses

用户收货地址管理。地址六要素：recipientName、phone、province、city、district、detail。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `get` \| `create` \| `update` \| `delete` |
| `userId` | number | 是 | 用户 ID |
| `addressId` | number | get/update/delete 时必填 | 地址 ID |
| `recipientName` | string | create/update 时必填 | 收件人姓名 |
| `phone` | string | create/update 时必填 | 联系电话 |
| `province` | string | create/update 时必填 | 省份 |
| `city` | string | create/update 时必填 | 城市 |
| `district` | string | create/update 时必填 | 区/县 |
| `detail` | string | create/update 时必填 | 详细地址 |

**示例**

- 查地址列表：`action=list`, `userId=1`
- 查地址详情：`action=get`, `userId=1`, `addressId=5`
- 新增地址：`action=create`, `userId=1`, `recipientName=张三`, `phone=13800138000`, `province=广东省`, `city=深圳市`, `district=南山区`, `detail=科技园路1号`
- 修改地址：`action=update`, `userId=1`, `addressId=5`, `recipientName=李四`, `phone=13900139000`, `province=北京市`, `city=北京市`, `district=海淀区`, `detail=中关村大街1号`
- 删除地址：`action=delete`, `userId=1`, `addressId=5`

---

---

# Activity Tools

## 15. activity_query

业务活动查询与统计。面向管理后台仪表盘场景。

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `action` | string | 是 | `list` \| `recent` \| `stats` |
| `orderId` | number | 否 | list 时可选，按订单 ID 筛选 |
| `limit` | number | 否 | list/recent 时可选，返回条数，默认 20 |
| `period` | string | 否 | stats 时可选，快捷周期：`today` \| `last7` \| `last30`，默认 today |
| `from` | string | 否 | stats 时可选，起始日期 YYYY-MM-DD，与 to 同时传入 |
| `to` | string | 否 | stats 时可选，结束日期 YYYY-MM-DD，与 from 同时传入 |

**action 说明**

- `list`：按条件查询活动列表。可选按 orderId 筛选（同一订单的事件时间线）。
- `recent`：查询最近活动（跨所有订单，按时间倒序）。
- `stats`：查询统计指标，返回订单概览（创建/取消/完成）、支付概览（尝试/成功/失败/过期/金额）、履约概览（创建/配货/发货/签收）、库存活动（占用/释放）。支持 `period` 快捷周期或 `from`+`to` 自定义日期范围。

**示例**

- 查活动列表：`action=list`
- 按订单查活动：`action=list`, `orderId=42`
- 查最近 10 条活动：`action=recent`, `limit=10`
- 查今日统计：`action=stats`
- 查最近 7 天统计：`action=stats`, `period=last7`
- 查自定义区间统计：`action=stats`, `from=2026-01-01`, `to=2026-01-31`

---

## 环境与依赖

- 需对应后端服务已启动。
- **Catalog**：默认 API 基地址 `http://localhost:8080/api`，可通过 `HMALL_API_BASE` 覆盖。上传图像依赖后端 MinIO（`minio.enabled=true`）。
- **Inventory**：默认 API 基地址 `http://localhost:8083/api`，可通过 `HMALL_INVENTORY_API_BASE` 覆盖。`list` action 会额外调用 Catalog API 获取 SKU 名称。
- **Cart**：默认 API 基地址 `http://localhost:8087/api`，可通过 `HMALL_CART_API_BASE` 覆盖。
- **Order**：默认 API 基地址 `http://localhost:8081/api`，可通过 `HMALL_ORDER_API_BASE` 覆盖。

- **Fulfillment**：默认 API 基地址 `http://localhost:8088/api`，可通过 `HMALL_FULFILLMENT_API_BASE` 覆盖。
- **User**：默认 API 基地址 `http://localhost:8082/api`，可通过 `HMALL_USER_API_BASE` 覆盖。
- **Activity**：默认 API 基地址 `http://localhost:8086/api`，可通过 `HMALL_ACTIVITY_API_BASE` 覆盖。

*当前实现已与本文档一致：上述 16 个 tool 均已实现。*
