# 用户限定上下文 - 领域模型

与需求、契约对照；实现以本文档为准。

---

## 模型图（PlantUML）

修改模型时改此处即可；渲染见 [PlantUML](https://www.plantuml.com/plantuml) 或 IDE 插件。

```plantuml
@startuml user-domain
skinparam classAttributeIconSize 0
skinparam linetype ortho
left to right direction

title 用户限定上下文 - 领域模型

class User <<聚合根>> {
  - userId: Long
  - username: String * {唯一}
  - passwordHash: String *
  --
  不变式: username 必填、唯一；passwordHash 必填
}

class Address <<实体>> {
  - addressId: Long
  - userId: Long
  - recipientName: String *
  - phone: String *
  - province: String *
  - city: String *
  - district: String *
  - detail: String *
  --
  不变式: 归属 userId；各字段必填
}

User "1" *-- "0..*" Address : 拥有

note right of User
  登录：应用层调用 User.verifyPassword(plainPassword)
  校验通过后生成 JWT（基础设施），不在领域模型内
end note
@enduml
```

---

## 实体与属性说明

### User（用户）— 聚合根

| 属性       | 类型   | 说明 |
|------------|--------|------|
| UserID     | Long   | 唯一标识 |
| Username   | String | 必填，全局唯一，用于登录 |
| PasswordHash | String | 必填，加密后的密码，用于登录校验 |

**不变式**：Username 必填、全局唯一；PasswordHash 必填。

**领域方法**：
- `verifyPassword(plainPassword: String): boolean` — 校验明文密码是否与 passwordHash 匹配，由应用层在登录时调用。

### Address（收货地址）— 实体

| 属性         | 类型   | 说明 |
|--------------|--------|------|
| addressId    | Long   | 唯一标识 |
| userId       | Long   | 归属用户 |
| recipientName| String | 收件人姓名，必填 |
| phone        | String | 手机号，必填 |
| province     | String | 省/直辖市，必填 |
| city         | String | 城市，必填 |
| district     | String | 区/县，必填 |
| detail       | String | 详细地址，必填 |

**不变式**：归属 userId；各字段必填、非空。与 Order ShippingAddress 字段一一对应，便于结账时引用。

---

## 实体与表

| 模型   | 表名         |
|--------|--------------|
| User   | user_account |
| Address| user_address |

不变式由应用层校验。
