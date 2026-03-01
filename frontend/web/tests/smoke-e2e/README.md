# Frontend Smoke E2E

这里是 `frontend/web` 的最小冒烟测试实现目录（Playwright）。

- 设计与流程说明：`docs/frontend/web/testing.md`
- 用例编排：`specs/`
- 页面对象：`pages/`
- 夹具与数据准备：`fixtures/`、`helpers/`

测试基础设施与技术：

- 测试框架：`@playwright/test`（E2E 执行、断言、报告）。
- 浏览器通道：`channel: "chrome"`（复用本机 Chrome，减少额外安装依赖）。
- 启动方式：`playwright.config.cjs` 通过 `webServer` 启动 Vite（`--strictPort`）。
- 产物目录：`artifacts/report`（HTML 报告）+ `artifacts/results`（trace、截图等）。
- 执行入口：`scripts/run-smoke-e2e.cjs`（自动兼容根目录/子目录依赖路径）。

常用命令：

```bash
npm run test:smoke:e2e
npm run test:smoke:e2e:list
npm run test:smoke:e2e:report
npm run test:smoke:e2e:clean
```
