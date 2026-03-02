---
name: integration
description: 跨 BC 或跨系统集成：在调用方实现出站端口的真实适配器，对接被调用方已有 API；通用流程，适用于任意上下游 BC 对接。触发词：集成、integration、对接、打通、BC 对接、与 xx 对接。
---

# 跨 BC / 跨系统集成

在**调用方**将出站端口（Port）从桩/占位替换为**真实适配器**，通过 HTTP 等调用**被调用方**已有 API。通用：不限定具体 BC。

| 阶段 | 目标 |
|------|------|
| 一、API 契约与上下文地图 | 确认被调用方 API 契约（api.yaml）与调用方 Port、上下文地图（ContextMap）一致 |
| 二、适配器与配置 | 实现真实适配器，配置下游 base URL，Profile/条件 Bean 区分测试与运行 |
| 三、验证 | 验收仍用 Stub 全绿；可选集成测试或联调 |

参考：`docs/context-map.md`；契约：`docs/bounded-contexts/<被调用方>/api.yaml`。

---

## 前置条件

- 被调用方已实现且契约见 `api.yaml`。
- 调用方已定义 Port、当前为 NoOp/Stub，验收已用桩覆盖。

---

## 步骤 To-Do

### 阶段一：API 契约与上下文地图

| 步骤 | 动作 |
|------|------|
| 1 | 读上下文地图（`docs/context-map.md`），确认调用方→被调用方及集成方式（REST/事件等）。 |
| 2 | 读被调用方 API 契约（`api.yaml`），确认 path/method/request/response 与 Port 语义一致；不一致则先对齐契约或 Port。 |

### 阶段二：适配器与配置

| 步骤 | 动作 |
|------|------|
| 3 | 在调用方新增 Port 的**真实适配器**（如 `RestXxxAdapter`）：RestTemplate/WebClient 调 base URL + path，按 api.yaml 序列化请求/解析响应；失败按契约抛异常。**若被调用方返回的 payload 比 Port 语义更宽**（如返回多种类型但 Port 只关心其中一种），在适配器中做过滤/映射，确保 Port 返回值严格符合其语义契约。 |
| 4 | 调用方增加配置（base URL 等）。用 **Profile 或条件 Bean**：验收注入 Stub，本地/生产注入真实适配器。 |

### 阶段三：验证

| 步骤 | 动作 |
|------|------|
| 5 | 验收测试仍注入 Stub，保持全绿。 |
| 6 | 可选：集成测试或联调步骤。 |
| 7 | 更新 `docs/project-status.md`（若适用）。 |

---

## 约定

| 约定 | 说明 |
|------|------|
| 契约以被调用方为准 | Adapter 只做转换与调用，不发明新字段。 |
| 不因集成改领域 | 仅替换端口实现；Port 与契约不匹配时优先在适配层做 DTO 转换。 |
| 验收用 Stub | 集成/联调验证真实调用。 |
| Stub 与真实响应对齐 | 适配器中若新增了过滤/映射逻辑，需同步检查验收 Stub 的 JSON 响应是否包含适配器依赖的字段（如 `productType`），避免 Stub 缺字段导致测试假绿。 |

---

## 检查清单

- [ ] context-map 与 api.yaml 已确认；Port 与契约一致
- [ ] 真实适配器已实现并配置（base URL、Profile/条件 Bean）；若有响应过滤/映射，已确认 Port 语义严格满足
- [ ] 验收仍用 Stub 全绿；Stub JSON 已与适配器依赖的字段对齐；可选集成/联调
- [ ] project-status 已更新（若适用）

---

## 参考

- `docs/context-map.md`
- `docs/bounded-contexts/<被调用方>/api.yaml`

