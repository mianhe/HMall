#!/usr/bin/env node
/**
 * 生产环境 URL 配置审计
 *
 * 目的：找出所有默认 localhost/127.0.0.1、在生产环境需覆盖的配置，避免漏扫。
 *
 * 为何之前漏掉 mockPayBaseUrl：
 * 1. 概念范围过窄：只关注「服务间调用的 base-url」，忽略了「客户端可见 URL」
 * 2. 命名差异：mockPayBaseUrl 通过 @ConfigurationProperties 注入，不在 @Value 里
 * 3. 用途不同：用于生成 payUrl 返回给前端跳转，不是服务间调用
 *
 * 正确分类：
 * - Type A 服务间调用：Service A → HTTP → Service B，prod 用 docker 服务名
 * - Type B 客户端可见 URL：服务生成 URL 供浏览器访问，prod 用公网地址
 *
 * 用法：node scripts/audit-prod-urls.mjs
 */

import { readFileSync, readdirSync, existsSync } from 'fs'
import { join } from 'path'

const servicesDir = join(process.cwd(), 'services')
const results = []

// 1. 扫描所有 application*.yml（排除 test）中含 localhost/127.0.0.1 的配置
// 2. 扫描 Java 中 @Value / ConfigurationProperties 含 localhost 的默认值
// 3. 检查 application-prod.yml 是否覆盖

function* walkYaml(dir, prefix = '') {
  try {
    for (const name of readdirSync(dir, { withFileTypes: true })) {
      const p = join(dir, name.name)
      if (name.isDirectory() && !name.name.startsWith('.') && name.name !== 'target') {
        yield* walkYaml(p, prefix + name.name + '/')
      } else if (name.name.endsWith('.yml') || name.name.endsWith('.yaml')) {
        yield { path: p, relative: prefix + name.name }
      }
    }
  } catch (_) {}
}

function* walkJava(dir, prefix = '') {
  try {
    for (const name of readdirSync(dir, { withFileTypes: true })) {
      const p = join(dir, name.name)
      if (name.isDirectory() && !name.name.startsWith('.') && name.name !== 'target') {
        yield* walkJava(p, prefix + name.name + '/')
      } else if (name.name.endsWith('.java')) {
        yield { path: p, relative: prefix + name.name }
      }
    }
  } catch (_) {}
}

const localhostRe = /localhost|127\.0\.0\.1/
const prodYamlRe = /application-prod\.yml$/
const testYamlRe = /(application-test|test\/.*\.yml)/
const testJavaRe = /\/test\//

// 收集 main 资源下的 yml 中含 localhost 的 key
const yamlFindings = []
for (const { path: fp, relative } of walkYaml(servicesDir)) {
  if (testYamlRe.test(relative)) continue
  const content = readFileSync(fp, 'utf8')
  const lines = content.split('\n')
  let inProd = prodYamlRe.test(relative)
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (localhostRe.test(line)) {
      if (line.trim().startsWith('- ')) continue // 列表项如 allowed-origins，通常整块在 prod 覆盖
      const service = relative.split('/')[0]
      const key = line.trim().match(/^([a-z0-9.-]+)\s*:/)?.[1] || '?'
      yamlFindings.push({
        service,
        file: relative,
        key,
        line: line.trim(),
        inProd,
      })
    }
  }
}

// 收集 Java 中含 localhost 默认值的 @Value / 字段
const javaFindings = []
for (const { path: fp, relative } of walkJava(servicesDir)) {
  if (testJavaRe.test(relative)) continue
  const content = readFileSync(fp, 'utf8')
  if (!localhostRe.test(content)) continue
  const service = relative.split('/')[0]
  const lines = content.split('\n')
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (localhostRe.test(line)) {
      // @Value 或 private String xxxUrl = "http://localhost..."
      const match = line.match(/@Value\([^)]+\)|(private\s+\w+\s+\w+(?:Url|BaseUrl|baseUrl|Base|endpoint)\s*=\s*"[^"]*localhost[^"]*")/i)
      if (match || /localhost|127\.0\.0\.1/.test(line)) {
        const trimmed = line.trim()
        if (trimmed.startsWith('//') || trimmed.startsWith('*')) continue
        javaFindings.push({ service, file: relative, line: trimmed })
      }
    }
  }
}

// 按服务汇总，检查 application-prod 是否覆盖
const byService = new Map()
function add(service, type, key, defaultValue, prodOverride, note) {
  if (!byService.has(service)) byService.set(service, [])
  byService.get(service).push({ type, key, defaultValue, prodOverride, note })
}

for (const f of yamlFindings) {
  if (f.inProd) continue // prod 里出现 localhost 一般是 ${VAR:localhost}，可接受
  const prodFile = join(servicesDir, f.service, 'src/main/resources/application-prod.yml')
  let prodOverride = false
  let prodVal = ''
  try {
    const prod = readFileSync(prodFile, 'utf8')
    const keyBase = f.key.split('.')[0]
    if (prod.includes(f.key) || prod.includes(keyBase + ':') || prod.includes(keyBase.split('-')[0])) {
      prodOverride = true
      const m = prod.match(new RegExp(`${f.key.replace('.', '\\.')}\\s*:\\s*(.+)`))
      if (m) prodVal = m[1].trim()
    }
  } catch (_) {}
  const type = f.key.includes('base-url') ? (f.key.includes('mock-pay') ? 'B' : 'A') : '?'
  add(f.service, type, f.key, f.line, prodOverride ? `✓ ${prodVal}` : '❌ 未覆盖', '')
}

// Java 中典型的需 prod 覆盖项
const javaProdKeys = [
  { pattern: /mockPayBaseUrl|mock-pay-base-url/i, type: 'B', key: 'payment.mock-pay-base-url' },
]
for (const f of javaFindings) {
  for (const { pattern, type, key } of javaProdKeys) {
    if (pattern.test(f.line)) {
      const prodFile = join(servicesDir, f.service, 'src/main/resources/application-prod.yml')
      let prodOverride = false
      try {
        const prod = readFileSync(prodFile, 'utf8')
        prodOverride = prod.includes('mock-pay-base-url') || prod.includes('mockPayBaseUrl')
      } catch (_) {}
      add(f.service, type, key, f.line.slice(0, 80), prodOverride ? '✓' : '❌ 未覆盖', 'Java 配置类')
      break
    }
  }
}

// 显式清单：已知的 Type B（客户端可见 URL），易被漏扫
const KNOWN_CLIENT_FACING = [
  {
    service: 'payment-service',
    key: 'payment.mock-pay-base-url',
    type: 'B',
    desc: '生成 payUrl 供浏览器跳转模拟支付页',
    prodKey: 'mock-pay-base-url',
  },
]
for (const item of KNOWN_CLIENT_FACING) {
  const prodPath = join(servicesDir, item.service, 'src/main/resources/application-prod.yml')
  let ok = false
  if (existsSync(prodPath)) {
    const prod = readFileSync(prodPath, 'utf8')
    ok = prod.includes(item.prodKey) && (prod.includes('PUBLIC_HOST') || prod.includes('47.') || !/http:\/\/localhost/.test(prod))
  }
  if (!byService.has(item.service)) byService.set(item.service, [])
  byService.get(item.service).push({
    type: item.type,
    key: item.key,
    defaultValue: '(ConfigurationProperties 默认 http://localhost:8084)',
    prodOverride: ok ? '✓' : '❌ 未覆盖',
    note: item.desc,
  })
}

// 输出
console.log('=== 生产环境 URL 配置审计 ===\n')
console.log('分类：A=服务间调用(docker 服务名)  B=客户端可见 URL(公网地址)\n')

for (const [svc, items] of [...byService.entries()].sort()) {
  const uninsured = items.filter((i) => !i.prodOverride || i.prodOverride.startsWith('❌'))
  if (uninsured.length === 0 && items.length > 0) {
    console.log(`【${svc}】 均已覆盖`)
    continue
  }
  console.log(`【${svc}】`)
  for (const it of items) {
    console.log(`  [${it.type}] ${it.key}`)
    console.log(`    默认: ${it.defaultValue}`)
    console.log(`    prod: ${it.prodOverride} ${it.note ? '(' + it.note + ')' : ''}`)
  }
  console.log('')
}
