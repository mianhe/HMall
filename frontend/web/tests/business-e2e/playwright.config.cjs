const { defineConfig } = require('@playwright/test')
const path = require('path')

const port = Number(process.env.BUSINESS_E2E_WEB_PORT || 5191)
const artifactsRoot = path.join(__dirname, 'artifacts')

module.exports = defineConfig({
  testDir: './specs',
  testMatch: '**/*.spec.cjs',
  outputDir: path.join(artifactsRoot, 'results'),
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 90000,
  reporter: [['list'], ['html', { open: 'never', outputFolder: path.join(artifactsRoot, 'report') }]],
  use: {
    baseURL: `http://127.0.0.1:${port}`,
    channel: 'chrome',
    headless: true,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  webServer: {
    command: `npm run dev -- --host 127.0.0.1 --port ${port} --strictPort`,
    url: `http://127.0.0.1:${port}`,
    reuseExistingServer: true,
    timeout: 120000,
    gracefulShutdown: {
      signal: 'SIGKILL',
      timeout: 3000,
    },
  },
})
