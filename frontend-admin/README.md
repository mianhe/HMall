# HMall 管理后台

Vue 3 + Vite + Vue Router + Tailwind CSS。用于配置类目、商品、规格与 SKU，对接后端 Catalog API。消费者端见 `frontend-web`。

## 前端有哪些功能？

见 **[docs/requirements.md](docs/requirements.md)**：已实现的页面/路由、后端已就绪但前端待开发的能力、与后端的对应关系。

## 开发

```bash
npm install
npm run dev
```

开发时代理将 `/api` 指向后端 `http://localhost:8080`（见 vite.config.js）。
