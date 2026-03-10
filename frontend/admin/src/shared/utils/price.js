/**
 * 价格格式化：分 -> 元，保留两位小数，带 ¥ 符号
 */
export function formatPrice(cents) {
  return '¥' + (cents / 100).toFixed(2)
}
