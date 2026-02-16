# HMall 商城（消费者端）

Vue 3 + Vite 消费者端，端口 5174。管理后台见 `frontend-admin`。API 经 Vite 代理到 BFF（端口 8085）。

## 启动

```bash
npm install
npm run dev
```

需先启动 BFF 及 Catalog、User、Order 服务（可执行 `./scripts/hmall.sh start`）。访问 http://localhost:5174。
