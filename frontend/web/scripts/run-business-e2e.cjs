#!/usr/bin/env node

const { existsSync } = require('fs')
const { resolve } = require('path')
const { spawnSync } = require('child_process')

const candidates = [
  resolve(__dirname, '../node_modules/@playwright/test/cli.js'),
  resolve(__dirname, '../../../node_modules/@playwright/test/cli.js'),
]

const cliPath = candidates.find((p) => existsSync(p))
if (!cliPath) {
  console.error('[business-e2e] 未找到 @playwright/test CLI。')
  console.error('[business-e2e] 请先在仓库根目录或 frontend/web 执行 npm install。')
  process.exit(1)
}

const args = [
  cliPath,
  'test',
  '-c',
  'tests/business-e2e/playwright.config.cjs',
  ...process.argv.slice(2),
]

const result = spawnSync(process.execPath, args, {
  stdio: 'inherit',
  cwd: resolve(__dirname, '..'),
  env: process.env,
})

process.exit(result.status ?? 1)
