# Step 1 功能验证

环境已就绪：**db**、**activity-service**、**bff-web**、**frontend-admin** 已启动（或已重启以加载新代码）。

---

## 在哪里测

**浏览器打开**：<http://127.0.0.1:5173/activity>

（若端口被占用，Vite 可能用了 5174 等，看终端输出或 `./scripts/hmall.sh status` 里的 frontend-admin 端口。）

---

## 看什么

1. **页面有「多维查询」区块**  
   在统计卡片下方，有四个输入框：**订单 ID**、**用户 ID**、**SPU ID**、**SKU ID**，和一个 **「查询」** 按钮。

2. **功能正常**  
   - 任选一个输入框填一个数字（例如订单 ID 填 `1`），点「查询」。  
   - **正常**：要么出现表格（时间、事件类型、Topic、订单 ID、用户 ID），要么提示「无匹配记录」；且不出现「查询失败」。

3. **（可选）元数据新字段**  
   接口里事件类型已带 `origin`、`processRoles`，前端若以后要展示流程归属，可直接用。  
   命令行快速验证：  
   `curl -s http://127.0.0.1:8085/api/activities/event-metadata | head -c 500`  
   能看到 `"origin":"domain"`、`"processRoles"` 即表示接口正常。

---

## 若前端未启动

执行：

```bash
./scripts/hmall.sh start frontend-admin
```

再看 status 里 frontend-admin 的端口，用浏览器访问 `http://127.0.0.1:<端口>/activity`。
