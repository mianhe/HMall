/**
 * 镭雕服务判断与内容校验工具。
 * 供 ProductDetailPage、CheckoutPage、E2E helper 等复用，避免重复定义。
 */

/** serviceKind=ENGRAVING 或名称含「镭雕」（兜底：Catalog 未配置 serviceKind 时） */
export function isEngravingService(svc) {
  if (!svc) return false
  if (String(svc.serviceKind || '').toUpperCase() === 'ENGRAVING') return true
  return (svc.name || '').includes('镭雕')
}

/** 构建提交给后端的 serviceAttributes 对象（仅含非空字段） */
export function buildEngravingAttributes(patternId, patternName, text) {
  const attrs = {}
  if (patternId != null) attrs.engravingPatternId = patternId
  if (patternName) attrs.engravingPatternName = patternName
  const trimmed = (text || '').trim()
  if (trimmed) attrs.engravingText = trimmed
  return Object.keys(attrs).length > 0 ? attrs : null
}

/** 校验镭雕内容：至少选图案或文字其一，文字≤20字 */
export function validateEngravingContent(patternId, text) {
  const hasPattern = patternId != null
  const hasText = (text || '').trim().length > 0
  if (!hasPattern && !hasText) return '请选择图案或输入文字（至少选其一）'
  if (hasText && text.trim().length > 20) return '雕刻文字不能超过20字'
  return ''
}
