# language: zh-CN
@user
功能: 管理用户
  作为运营人员，我希望能够创建和查询用户，以便管理系统中的用户账号。

  场景: 创建用户时应成功并返回 userId 和 username
    当 用户创建用户 "alice" 密码 "secret123"
    那么 应创建成功
    并且 返回的 userId 不为空
    并且 返回的 username 为 "alice"

  场景: 用户名已存在时创建用户应失败并返回错误提示
    假如 已存在用户 "alice" 密码 "secret123"
    当 用户创建用户 "alice" 密码 "other456"
    那么 应创建失败
    并且 应返回 400

  场景: 用户名为空时创建用户应失败并返回错误提示
    当 用户创建用户 "" 密码 "secret123"
    那么 应创建失败
    并且 应返回 400

  场景: 密码为空时创建用户应失败并返回错误提示
    当 用户创建用户 "alice" 密码 ""
    那么 应创建失败
    并且 应返回 400

  场景: 按 ID 请求用户详情时应返回用户信息（不含 passwordHash）
    假如 已存在用户 "alice" 密码 "secret123"
    当 用户请求用户 "alice" 的详情
    那么 应返回 200
    并且 返回的用户信息包含 username "alice"
    并且 返回的用户信息不含 passwordHash

  场景: 请求用户列表时应返回用户列表
    假如 已存在用户 "alice" 和 "bob"
    当 用户请求用户列表
    那么 应返回 200
    并且 返回的用户列表中至少有 2 个用户
    并且 返回的列表中包含 username "alice" 和 "bob"

  场景: 用户不存在时请求详情应返回 404
    当 用户请求用户 ID 999999 的详情
    那么 应返回 404

  场景: 按 ID 请求用户分群信息时应返回 level 和 tags
    假如 已存在用户 "segment-user" 密码 "secret123"
    当 用户请求用户 "segment-user" 的分群信息
    那么 应返回 200
    并且 返回的用户分群 level 为 "L1"
    并且 返回的用户分群 tags 为空

  场景: 运营更新用户等级后查询分群应返回新等级
    假如 已存在用户 "level-user" 密码 "secret123"
    当 运营将用户 "level-user" 的等级更新为 "L2"
    那么 应返回 200
    当 用户请求用户 "level-user" 的分群信息
    那么 应返回 200
    并且 返回的用户分群 level 为 "L2"

  场景: 运营更新用户标签后查询分群应返回新标签
    假如 已存在用户 "tag-user" 密码 "secret123"
    当 运营将用户 "tag-user" 的标签更新为 "VIP,NEW_USER"
    那么 应返回 200
    当 用户请求用户 "tag-user" 的分群信息
    那么 应返回 200
    并且 返回的用户分群 tags 包含 "VIP"

  场景: 创建圈选规则并预览命中后可激活
    假如 已存在用户 "rule-user" 密码 "secret123"
    当 运营将用户 "rule-user" 的标签更新为 "VIP"
    那么 应返回 200
    当 运营创建圈选规则 "vip-rule" 条件 levelsIn "-" tagsAny "VIP" tagsAll "-" excludeTags "-"
    那么 应创建成功
    并且 圈选规则状态为 "DRAFT"
    当 运营预览最近创建的圈选规则
    那么 应返回 200
    并且 圈选预览命中人数为 1
    当 运营激活最近创建的圈选规则
    那么 应返回 200
    并且 圈选规则状态为 "ACTIVE"

  场景: 命中人数为 0 时圈选规则激活应失败
    假如 已存在用户 "rule-user-2" 密码 "secret123"
    当 运营创建圈选规则 "nobody-hit" 条件 levelsIn "L3" tagsAny "-" tagsAll "-" excludeTags "-"
    那么 应创建成功
    当 运营预览最近创建的圈选规则
    那么 应返回 200
    并且 圈选预览命中人数为 0
    当 运营激活最近创建的圈选规则
    那么 应返回 400

  场景: 圈选规则条件冲突时创建应失败
    当 运营创建圈选规则 "conflict-rule" 条件 levelsIn "-" tagsAny "VIP" tagsAll "-" excludeTags "VIP"
    那么 应返回 400
