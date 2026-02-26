/**
 * 用户限定上下文 API，与 docs/bounded-contexts/user/api.yaml 一致
 */
import client from './client.js'

export async function register(username, password) {
  const { data } = await client.post('/users', { username, password })
  return data
}

export async function login(username, password) {
  const { data } = await client.post('/login', { username, password })
  return data
}

/** 按 userId 查询地址列表 */
export async function getAddresses(userId) {
  const { data } = await client.get(`/users/${userId}/addresses`)
  return data
}

/** 新增收货地址 */
export async function createAddress(userId, body) {
  const { data } = await client.post(`/users/${userId}/addresses`, body)
  return data
}

/** 修改收货地址 */
export async function updateAddress(userId, addressId, body) {
  const { data } = await client.put(`/users/${userId}/addresses/${addressId}`, body)
  return data
}

/** 删除收货地址 */
export async function deleteAddress(userId, addressId) {
  await client.delete(`/users/${userId}/addresses/${addressId}`)
}
