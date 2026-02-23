# language: zh-CN
@skill
功能: Skill 管理
  作为后台管理员，我希望能创建和管理 Skill，以便定义 AI 助手在不同场景下的行为。

  场景: 创建 Skill 应成功并返回详情
    当 创建 Skill 名称 "库存管理助手" 描述 "查询和管理商品库存" systemPrompt "你是库存管理专家" allowedTools "inventory_*,catalog_list_skus"
    那么 应返回 200
    并且 返回的 Skill 名称为 "库存管理助手"
    并且 返回的 Skill 包含 allowedTools "inventory_*" 和 "catalog_list_skus"

  场景: 创建 Skill 时名称为空应返回 400
    当 创建 Skill 名称 "" 描述 "测试" systemPrompt "" allowedTools ""
    那么 应返回 400

  场景: 查询 Skill 列表应返回所有 Skill
    假如 已存在 Skill "商品管理助手"
    并且 已存在 Skill "订单查询助手"
    当 查询 Skill 列表
    那么 应返回 200
    并且 列表中应包含 "商品管理助手" 和 "订单查询助手"

  场景: 按 ID 查询 Skill 详情应返回完整信息
    假如 已存在 Skill "库存管理助手" 且 allowedTools 为 "inventory_*"
    当 按 ID 查询该 Skill
    那么 应返回 200
    并且 返回的 Skill 名称为 "库存管理助手"
    并且 返回的 Skill 包含 allowedTools "inventory_*"

  场景: 更新 Skill 应修改对应字段
    假如 已存在 Skill "旧名称"
    当 更新该 Skill 名称为 "新名称" 描述为 "新描述"
    那么 应返回 200
    并且 返回的 Skill 名称为 "新名称"

  场景: 删除 Skill 应成功
    假如 已存在 Skill "待删除助手"
    当 删除该 Skill
    那么 应返回 204
    当 按 ID 查询该 Skill
    那么 应返回 404

  场景: 设为默认 Skill 应自动取消原默认
    假如 已存在默认 Skill "助手A"
    并且 已存在 Skill "助手B"
    当 将 "助手B" 设为默认
    那么 应返回 200
    并且 "助手B" 的 isDefault 为 true
    当 按 ID 查询 "助手A"
    那么 "助手A" 的 isDefault 为 false

  场景: 删除默认 Skill 后应无默认 Skill
    假如 已存在默认 Skill "默认助手"
    当 删除该 Skill
    那么 应返回 204
    当 查询 Skill 列表
    那么 列表中无 isDefault 为 true 的 Skill

  场景: 创建 Skill 时指定 audience 应成功
    当 创建 Skill 名称 "购物助手" 描述 "帮用户搜商品" systemPrompt "你是购物助手" allowedTools "catalog_*" audience "consumer"
    那么 应返回 200
    并且 返回的 Skill 名称为 "购物助手"
    并且 返回的 Skill audience 为 "consumer"

  场景: 创建 Skill 不指定 audience 时默认为 all
    当 创建 Skill 名称 "通用助手" 描述 "通用" systemPrompt "" allowedTools ""
    那么 应返回 200
    并且 返回的 Skill audience 为 "all"
