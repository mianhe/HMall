# language: zh-CN
@settings
功能: 系统设置

  背景:
    假如 系统设置已重置

  场景: 获取系统设置（首次自动创建）
    当 获取系统设置
    那么 应返回 200
    并且 adminBasePrompt 为空
    并且 consumerBasePrompt 为空

  场景: 更新管理端 base prompt
    当 更新系统设置 adminBasePrompt 为 "你是管理端助手，%s 是当前页面。"
    那么 应返回 200
    并且 返回的 adminBasePrompt 为 "你是管理端助手，%s 是当前页面。"
    当 获取系统设置
    那么 返回的 adminBasePrompt 为 "你是管理端助手，%s 是当前页面。"

  场景: 更新消费端 base prompt
    当 更新系统设置 consumerBasePrompt 为 "你是消费端助手，%s 是当前页面。绝不编造数据。"
    那么 应返回 200
    并且 返回的 consumerBasePrompt 为 "你是消费端助手，%s 是当前页面。绝不编造数据。"

  场景: 同时更新两端 base prompt
    当 更新系统设置 adminBasePrompt 为 "管理端提示词 %s" consumerBasePrompt 为 "消费端提示词 %s"
    那么 应返回 200
    并且 返回的 adminBasePrompt 为 "管理端提示词 %s"
    并且 返回的 consumerBasePrompt 为 "消费端提示词 %s"

  场景: 清空 base prompt 后应回退到默认
    假如 已更新系统设置 adminBasePrompt 为 "自定义提示词 %s"
    当 更新系统设置 adminBasePrompt 为 ""
    那么 应返回 200
    并且 adminBasePrompt 为空

  场景: 重置设置恢复系统默认提示词
    假如 已更新系统设置 adminBasePrompt 为 "自定义管理端 %s"
    当 重置系统设置
    那么 应返回 200
    并且 adminBasePrompt 不为空
    并且 consumerBasePrompt 不为空
