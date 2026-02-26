/**
 * 从 axios 错误中提取可读的错误信息。
 * 优先取 response.data 中的 message/error/msg，其次取 e.message。
 * 若内容全为 ?/空/替换字符则返回空串，由调用方决定兜底文案。
 */
export function resolveErrorMessage(e) {
  const data = e.response?.data
  if (data != null && typeof data === 'object' && !Array.isArray(data)) {
    const s = data.message ?? data.error ?? data.msg
    if (typeof s === 'string' && s.trim()) return s.trim()
  }
  if (typeof e.message === 'string' && e.message.trim()) return e.message.trim()
  return ''
}

/** 从 axios 错误中提取展示用错误信息，含兜底逻辑。 */
export function formatApiError(e, fallback = '操作失败，请稍后重试') {
  if (e.code === 'ECONNABORTED') return '请求超时，请稍后重试'
  const msg = resolveErrorMessage(e)
  if (msg && !/^[\s?\uFFFD]*$/i.test(msg)) return msg
  const status = e.response?.status
  return status ? `${fallback}（${status}）` : fallback
}
