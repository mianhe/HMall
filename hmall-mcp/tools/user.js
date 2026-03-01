/**
 * User 模块的 MCP tools：2 个 tool（user_manage、user_addresses）。
 * 不暴露 login（认证由前端处理，AI 不应代替用户登录）。
 */
import { z } from 'zod'

const USER_API_BASE = process.env.HMALL_USER_API_BASE || 'http://localhost:8082/api'

async function userApi(method, path, body) {
  const url = `${USER_API_BASE}${path}`
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
  }
  if (body !== undefined) {
    opts.body = JSON.stringify(body)
  }
  const res = await fetch(url, opts)
  const text = await res.text()
  if (!res.ok) {
    let msg = `${res.status}`
    try { msg = JSON.parse(text).message || msg } catch {}
    throw new Error(msg)
  }
  return text ? JSON.parse(text) : null
}

function ok(text) {
  return { content: [{ type: 'text', text }] }
}

function err(e) {
  if (e.cause?.code === 'ECONNREFUSED' || e.message?.includes('ECONNREFUSED') || e.message?.includes('fetch failed')) {
    return { content: [{ type: 'text', text: `错误：无法连接后端服务（${USER_API_BASE}），请确认服务已启动。原始错误：${e.message}` }] }
  }
  return { content: [{ type: 'text', text: `错误：${e.message}` }] }
}

function formatAddress(addr) {
  return `[${addr.addressId}] ${addr.recipientName} ${addr.phone} ${addr.province}${addr.city}${addr.district}${addr.detail}`
}

export function registerUserTools(server) {
  server.tool(
    'user_manage',
    '用户管理。action=list 查用户列表；get(userId) 查用户详情；create(username, password) 创建用户。不含登录（认证由前端处理）。',
    {
      action: z.enum(['list', 'get', 'create']).describe('list|get|create'),
      userId: z.number().optional().describe('get 时必填，用户 ID'),
      username: z.string().optional().describe('create 时必填，用户名（全局唯一）'),
      password: z.string().optional().describe('create 时必填，密码'),
    },
    async ({ action, userId, username, password }) => {
      try {
        if (action === 'list') {
          const users = await userApi('GET', '/users')
          if (!users.length) return ok('暂无用户。')
          const lines = users.map(u => `[${u.id}] ${u.username}`)
          return ok(lines.join('\n'))
        }
        if (action === 'get') {
          if (!userId) return err(new Error('get 需要提供 userId'))
          const u = await userApi('GET', `/users/${userId}`)
          return ok(`用户详情：\n- ID: ${u.id}\n- 用户名: ${u.username}`)
        }
        if (action === 'create') {
          if (!username) return err(new Error('create 需要提供 username'))
          if (!password) return err(new Error('create 需要提供 password'))
          const u = await userApi('POST', '/users', { username, password })
          return ok(`创建用户成功：ID=${u.id}，用户名="${u.username}"`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )

  server.tool(
    'user_addresses',
    '用户收货地址管理。action=list(userId) 查地址列表；get(userId, addressId) 查地址详情；create(userId + 六要素) 新增地址；update(userId, addressId + 六要素) 修改地址；delete(userId, addressId) 删除地址。地址六要素：recipientName、phone、province、city、district、detail。',
    {
      action: z.enum(['list', 'get', 'create', 'update', 'delete']).describe('list|get|create|update|delete'),
      userId: z.number().describe('用户 ID'),
      addressId: z.number().optional().describe('get/update/delete 时必填，地址 ID'),
      recipientName: z.string().optional().describe('create/update 时必填，收件人姓名'),
      phone: z.string().optional().describe('create/update 时必填，联系电话'),
      province: z.string().optional().describe('create/update 时必填，省份'),
      city: z.string().optional().describe('create/update 时必填，城市'),
      district: z.string().optional().describe('create/update 时必填，区/县'),
      detail: z.string().optional().describe('create/update 时必填，详细地址'),
    },
    async ({ action, userId, addressId, recipientName, phone, province, city, district, detail }) => {
      try {
        const base = `/users/${userId}/addresses`
        if (action === 'list') {
          const addrs = await userApi('GET', base)
          if (!addrs.length) return ok('该用户暂无收货地址。')
          const lines = addrs.map(formatAddress)
          return ok(lines.join('\n'))
        }
        if (action === 'get') {
          if (!addressId) return err(new Error('get 需要提供 addressId'))
          const addr = await userApi('GET', `${base}/${addressId}`)
          return ok(formatAddress(addr))
        }
        if (action === 'create') {
          const addr = await userApi('POST', base, { recipientName, phone, province, city, district, detail })
          return ok(`新增地址成功：${formatAddress(addr)}`)
        }
        if (action === 'update') {
          if (!addressId) return err(new Error('update 需要提供 addressId'))
          const addr = await userApi('PUT', `${base}/${addressId}`, { recipientName, phone, province, city, district, detail })
          return ok(`修改地址成功：${formatAddress(addr)}`)
        }
        if (action === 'delete') {
          if (!addressId) return err(new Error('delete 需要提供 addressId'))
          await userApi('DELETE', `${base}/${addressId}`)
          return ok(`已删除地址 ID=${addressId}`)
        }
        return err(new Error('未知 action'))
      } catch (e) { return err(e) }
    }
  )
}
