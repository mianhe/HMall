#!/usr/bin/env node

const { rmSync, existsSync } = require('fs')
const { resolve } = require('path')

const targets = [
  resolve(__dirname, '../tests/smoke-e2e/artifacts'),
  // 兼容清理历史遗留默认目录
  resolve(__dirname, '../test-results'),
]

for (const dir of targets) {
  if (existsSync(dir)) {
    rmSync(dir, { recursive: true, force: true })
    console.log(`[smoke-e2e] removed: ${dir}`)
  }
}
