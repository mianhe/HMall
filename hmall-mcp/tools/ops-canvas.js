/**
 * ops_canvas — AI 到画布的专用通信通道。
 * AI 调用此工具在智能运营画布上渲染可视化内容。
 * 工具本身不访问后端 API，仅校验参数并原样返回给前端。
 */
import { z } from 'zod'

const SUPPORTED_VIEWS = [
  'line_chart', 'bar_chart', 'pie_chart',
  'stat_cards', 'timeline', 'table', 'event_list',
]

export function registerOpsCanvasTools(server) {
  server.tool(
    'ops_canvas',
    '在智能运营画布上渲染数据可视化面板。可在一轮对话中多次调用以展示多个面板（如洞察卡片 + 趋势图）。' +
    'view 指定图表类型，title 指定标题，data 传入对应结构。' +
    '调用前应先通过 activity_query 获取数据。' +
    'stat_cards 使用 cards 数组格式：{ cards: [{ label, value(字符串，已格式化), status?("success"|"warning"|"critical"), description? }] }。' +
    'AI 应从原始数据中提炼洞察指标（如成功率、取消率、日均值、峰值），而非直接传入原始数字。',
    {
      view: z.enum(SUPPORTED_VIEWS).describe('可视化类型'),
      title: z.string().describe('画布标题'),
      data: z.object({}).passthrough().describe('视图数据，结构随 view 类型不同'),
    },
    async ({ view, title, data }) => {
      return {
        content: [{ type: 'text', text: `已渲染：${title}` }],
        _raw: { type: 'canvas_command', view, title, data },
      }
    }
  )
}
