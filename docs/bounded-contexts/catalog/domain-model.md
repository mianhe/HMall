# 商品限定上下文 - 领域模型

与需求、契约对照；实现以本文档为准。

---

## 模型图（PlantUML）

修改模型时改此处即可；渲染见 [PlantUML](https://www.plantuml.com/plantuml) 或 IDE 插件。

```plantuml
@startuml catalog-domain
skinparam classAttributeIconSize 0
skinparam linetype ortho
left to right direction

title 商品限定上下文 - 领域模型

class Category <<聚合根>> {
  - categoryId: Long
  - parentId: Long {null=根}
  - name: String *
  - description: String
  --
  不变式: 名称必填；父为 null 或已存在；仅叶子可挂 SPU
}

class Spu <<聚合根>> {
  - spuId: Long
  - categoryId: Long *
  - name: String *
  - description: String
  --
  不变式: 名称、categoryId 必填
}

class SpecDimension <<实体>> {
  - specDimensionId: Long
  - spuId: Long
  - name: String * {同SPU内唯一}
  - required: boolean
  - sortOrder: Integer
  --
  不变式: 名称必填，同 SPU 内唯一
}

class SpecOption <<实体>> {
  - specOptionId: Long
  - specDimensionId: Long
  - optionValue: String * {同维度内唯一}
  - sortOrder: Integer
  --
  不变式: 选项值必填
}

class ProductImage <<值对象>> {
  - productImageId: Long
  - spuId: Long
  - specOptionId: Long {null=产品级展示图}
  - imageUrl: String *
  - sortOrder: Integer {越小越靠前}
  --
  不变式: imageUrl 必填；specOptionId 为空表示产品级，不为空表示该选项的展示图
}

class Sku <<实体>> {
  - skuId: Long
  - spuId: Long *
  - displayName: String
  - priceCents: Long * {>=0}
  --
  不变式: spuId、价格必填，价格≥0
}

class SkuSpecValue <<值对象>> {
  - specOptionId: Long
  --
  不变式: 须属该 SPU 下某维度；每个必填维度恰好一条
}

Category "0..*" -- "0..1" Category : parent >
Spu "0..*" --> "0..1" Category : categoryId
Spu "1" *-- "0..*" SpecDimension : 组合
Spu "1" *-- "0..*" Sku : 组合
SpecDimension "1" *-- "0..*" SpecOption : 组合
Spu "1" *-- "0..*" ProductImage : 组合（展示图）
ProductImage --> SpecOption : specOptionId {0..1 可选}
Sku "1" *-- "0..*" SkuSpecValue : 组合
SkuSpecValue --> SpecOption : specOptionId

note bottom of ProductImage
  specOptionId 为空：产品级展示图；
  不为空：该选项的展示图
end note
@enduml
```

---

## 实体与属性说明

### 类目（Category）— 聚合根

| 属性       | 类型   | 说明 |
|------------|--------|------|
| CategoryID | Long   | 唯一标识 |
| 父类目 ID  | Long   | 根为 null，形成树 |
| 名称       | String | 必填 |
| 描述       | String | 可选 |

**不变式**：名称必填；父类目 ID 为 null（根）或已存在类目；只有叶子类目才可以加入 SPU（创建 SPU 时校验）。

### SPU — 聚合根

| 属性       | 类型   | 说明 |
|------------|--------|------|
| SpuID      | Long   | 唯一标识 |
| CategoryID | Long   | 所属类目，必填 |
| 名称       | String | 必填 |
| 描述       | String | 可选 |

组合 SpecDimension、SKU。**不变式**：名称、CategoryID 必填；描述可选。

#### SpecDimension — 实体

| 属性            | 类型    | 说明 |
|-----------------|---------|------|
| SpecDimensionID | Long    | 唯一标识 |
| SpuID           | Long    | 所属 SPU |
| 维度名称        | String  | 必填，同 SPU 内唯一 |
| 是否必填        | boolean | 创建 SKU 时是否必选 |
| 排序            | Integer | 可选 |

组合 SpecOption（0..*）。**不变式**：维度名称必填、同 SPU 内唯一。

#### SpecOption — 实体

| 属性            | 类型   | 说明 |
|-----------------|--------|------|
| SpecOptionID    | Long   | 唯一标识 |
| SpecDimensionID | Long   | 所属维度 |
| 选项值          | String | 必填，同维度内唯一 |
| 排序            | Integer| 可选 |

**不变式**：选项值必填、同维度内唯一。

#### ProductImage — 值对象

| 属性            | 类型    | 说明 |
|-----------------|---------|------|
| ProductImageID  | Long    | 唯一标识 |
| SpuID           | Long    | 所属产品（SPU），必填 |
| SpecOptionID    | Long    | 可选；为空表示产品级展示图，不为空表示该选项的展示图 |
| 图片 URL        | String  | 必填，展示图地址 |
| 排序            | Integer | 可选，越小越靠前 |

产品展示图。**不变式**：图片 URL 必填；SpuID 必填；SpecOptionID 为空则为产品级展示图，不为空则须属该 SPU 下某选项。

#### SKU — 实体

| 属性       | 类型   | 说明 |
|------------|--------|------|
| SkuID      | Long   | 唯一标识，全局唯一 |
| SpuID      | Long   | 所属 SPU，必填 |
| 规格展示名 | String | 可选 |
| 价格       | Long   | 单位：分，必填、≥0 |

组合 SKUSpecValue（0..*）。**不变式**：SpuID、价格必填，价格≥0。

#### SKUSpecValue — 值对象

| 属性         | 类型 | 说明 |
|--------------|------|------|
| SpecOptionID | Long | 选中的 SpecOption |

**不变式**：SpecOptionID 须属该 SPU 下某 SpecDimension；每个必填维度恰好一条。

---

## 实体与表

| 模型 | 表名 |
|------|------|
| Category | category |
| Spu | spu |
| SpecDimension | spec_dimension |
| SpecOption | spec_option |
| ProductImage | product_image |
| Sku | sku |
| SKUSpecValue | sku_id + spec_option_id |

不变式由应用层校验。
