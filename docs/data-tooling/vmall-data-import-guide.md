# Vmall 商品数据采集与导入 HMall 指南

> 目标读者：AI Agent  
> 适用场景：从华为商城 (vmall.com) 采集商品数据，通过 HMall Prod MCP 导入到 HMall 系统

---

## 概述

本指南描述一套经过验证的方法，用于从华为商城 (Vmall) 提取商品的类目、名称、规格维度、颜色选项、价格、图片等结构化数据，并批量导入 HMall。

核心原理：Vmall 使用 Next.js SSR，所有产品数据以 JSON 形式嵌入 HTML 的 `<script id="__NEXT_DATA__">` 标签中。通过 `curl` 获取 HTML 后用 Python 解析该 JSON 即可拿到完整的结构化数据。

---

## 前置条件

- 可用的 HMall Prod MCP（工具前缀 `catalog_*`）
- 可用的 Browser MCP（`cursor-ide-browser`，用于获取 prdId）
- Shell 环境中有 `curl` 和 `python3`

---

## 第 1 阶段：获取产品 prdId

每个 Vmall 产品有唯一的 `prdId`（数字），是后续所有操作的入口。

### 方法 A：浏览器导航（推荐）

Vmall 的搜索接口有反爬保护，直接 curl 搜索页会返回空白或跳转。推荐使用 Browser MCP：

```
1. browser_navigate → https://www.vmall.com/
2. browser_snapshot → 查看页面结构，找到产品分类入口
3. browser_click → 点击目标类目/产品
4. browser_tabs (action=list) → 从新标签页 URL 中提取 prdId
```

URL 格式：
```
https://www.vmall.com/product/comdetail/index.html?prdId={prdId}&sbomCode={sbomCode}
```

只需要 `prdId`，`sbomCode` 可忽略（它只决定默认选中哪个 SKU）。

### 方法 B：已知 prdId 列表

如果已经有 prdId（从文档、之前的采集记录中获取），可跳过此步骤。部分已知的 prdId：

| 产品 | prdId |
|---|---|
| HUAWEI Mate 80 | 10086133363559 |
| HUAWEI nova 15 Ultra | 10086970282543 |
| HUAWEI Pura X | 10086061975812 |
| HUAWEI Pura 80 Pro+ | 10086153236238 |
| HUAWEI Pura 70 Pro | 10086821546239 |
| HUAWEI Mate XTs 非凡大师 | 10086754422315 |
| HUAWEI Mate 70 Pro | 10086259366534 |

### 注意事项

- **不要用 curl 访问搜索页**（`/search?keyword=...`），会被拦截返回 ~166 字节的空白页。
- **不要用 Python urllib/requests**，会被反爬机制阻断（挂起/超时）。
- 浏览器导航时可能需要等待页面加载（3-5 秒），用 `browser_snapshot` 确认内容就绪。

---

## 第 2 阶段：下载并解析产品数据

### 2.1 下载产品页 HTML

```bash
curl -s --compressed \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36" \
  "https://www.vmall.com/product/comdetail/index.html?prdId={prdId}" \
  -o /tmp/vmall_{product_name}.html
```

**必须包含的参数：**
- `--compressed`：Vmall 返回 gzip 压缩内容，不加会得到乱码
- `-H "User-Agent: ..."`：需要浏览器 UA，否则可能被拦截

**验证下载成功：** 文件大小应在 500KB-1MB 范围。如果只有几十 KB，说明可能被反爬拦截。

### 2.2 解析 `__NEXT_DATA__` JSON

```python
import json, re

with open('/tmp/vmall_{product_name}.html') as f:
    html = f.read()

# 提取 __NEXT_DATA__ 中的 JSON
matches = re.findall(r'__NEXT_DATA__.*?</script>', html, re.DOTALL)
json_match = re.search(r'>(\{.*?\})<', matches[0], re.DOTALL)
data = json.loads(json_match.group(1))

# 核心数据入口
page_props = data['props']['pageProps']
main_data = page_props['mainData']
current = main_data['current']
```

### 2.3 数据结构地图

```
data['props']['pageProps']
├── mainData
│   ├── current                                ← 当前产品的核心数据
│   │   ├── name: str                          ← 产品名称（如 "HUAWEI Mate 80"）
│   │   ├── briefName: str                     ← 简称（如 "Mate 80"）
│   │   ├── brandName: str                     ← 品牌名（"华为"）
│   │   │
│   │   ├── colorSelectItem: list[dict]        ← ★ 颜色选项列表
│   │   │   └── { attrValue, sbomCode, url, surrportSbomCodes }
│   │   │
│   │   ├── base: dict[sbomCode → dict]        ← ★ 每个 SKU 的详细数据
│   │   │   └── [sbomCode]
│   │   │       ├── photoName: str             ← 该颜色主图文件名
│   │   │       ├── photoPath: str             ← CDN 路径前缀
│   │   │       ├── groupPhotoList: list[dict] ← ★ 该颜色的多角度图片集
│   │   │       │   └── { photoName, photoPath }
│   │   │       ├── price: str                 ← 价格（如 "5499"）
│   │   │       └── name: str                  ← SKU 全名
│   │   │
│   │   ├── productOptions
│   │   │   ├── gbomAttrDisplayList: list[str] ← 维度名称列表（如 ["颜色","版本"]）
│   │   │   ├── gbomAttrMappings: dict         ← ★ 维度-选项-SKU 完整映射
│   │   │   │   └── [维度名]: list[dict]
│   │   │   │       └── { attrName, attrValue, sbomCode, attrValueCode }
│   │   │   └── sbomList: dict[sbomCode → dict]← SKU 级别营销/库存数据
│   │   │
│   │   ├── optionsImage: list[str]            ← 各颜色缩略图 URL 列表（428x428）
│   │   ├── disPrdId: int                      ← prdId
│   │   └── sbomsCodeArr: list[str]            ← 所有 sbomCode 列表
│   │
│   └── galleryData: list[dict]                ← 默认图片集（不随颜色变化！）
│       └── { imgUri, maxSize }
│
└── extData
    └── productRelDetailResp
        ├── querySkuSpecificResp               ← 规格参数
        │   └── majorSpecificationList[]
        │       └── { attrName, attrValue }    ← 如"屏幕尺寸: 6.75英寸"
        └── querySkuPicDetailResp              ← 详情页图文（富文本 HTML）
```

---

## 第 3 阶段：提取关键数据

### 3.1 提取颜色列表及对应图片

```python
CDN = "https://res.vmallres.com/pimages"

color_images = {}
for ci in current['colorSelectItem']:
    color_name = ci['attrValue']           # "曜石黑"
    sbom_code = ci['sbomCode']             # "2601010586317"
    sku = current['base'][sbom_code]

    main_img = f"{CDN}{sku['photoPath']}800_800_{sku['photoName']}"
    gallery = [f"{CDN}{g['photoPath']}800_800_{g['photoName']}"
               for g in sku.get('groupPhotoList', [])]

    color_images[color_name] = {
        'main': main_img,
        'gallery': gallery,         # 通常 10-12 张不同角度
        'sbomCode': sbom_code,
    }
```

**CDN URL 尺寸规则：**

```
https://res.vmallres.com/pimages/{photoPath}{WIDTH}_{HEIGHT}_{photoName}
```

| 尺寸 | 用途 |
|---|---|
| `142_142` | 颜色选择器缩略图 |
| `428_428` | 图片列表中图 |
| `800_800` | 产品大图（推荐） |

### 3.2 提取维度与选项映射

```python
options = current['productOptions']
dimensions = options['gbomAttrDisplayList']   # ["颜色", "版本", "CPU型号"]
mappings = options['gbomAttrMappings']

for dim_name in dimensions:
    attrs = mappings[dim_name]
    # 去重（同一选项值会对应多个 sbomCode）
    unique_values = []
    seen = set()
    for attr in attrs:
        val = attr['attrValue']
        if val not in seen:
            unique_values.append(val)
            seen.add(val)
    print(f"维度: {dim_name}, 选项: {unique_values}")
```

### 3.3 提取价格

```python
for sbom_code, sku_data in current['base'].items():
    price = sku_data.get('price', 'N/A')
    name = sku_data.get('name', '')
    print(f"SKU {sbom_code}: {name} = ¥{price}")
```

### 3.4 提取规格参数

```python
ext = page_props['extData']
specs = ext['productRelDetailResp']['querySkuSpecificResp']
for spec in specs.get('majorSpecificationList', []):
    print(f"{spec['attrName']}: {spec['attrValue']}")
```

---

## 第 4 阶段：导入 HMall

使用 HMall Prod MCP 的 `catalog_*` 系列工具，按以下顺序创建数据。

### 4.1 创建类目树

```
catalog_categories action=create, name="手机", parentId=null     → 得到 categoryId
catalog_categories action=create, name="Mate 系列", parentId={上层ID}  → 得到 leafCategoryId
```

规则：产品只能挂在叶子类目下。

### 4.2 创建产品 (SPU)

```
catalog_products action=create, name="HUAWEI Mate 80 Pro",
    categoryId={leafCategoryId},
    description="..."                                           → 得到 spuId
```

### 4.3 创建维度与选项

```
catalog_dimensions action=create, spuId={spuId},
    name="颜色", required=true                                  → 得到 dimensionId

catalog_dimensions action=add_option, spuId={spuId},
    dimensionId={dimensionId}, value="曜石黑"                   → 得到 optionId
```

### 4.4 创建 SKU

```
catalog_skus action=create, spuId={spuId},
    priceCents=549900,
    options=[{dimensionId: 1, optionId: 1}, {dimensionId: 2, optionId: 4}]
```

`priceCents` 为分，如 ¥5499 = 549900。

### 4.5 上传产品级图片

```
catalog_product_images action=add, spuId={spuId},
    imageUrl="https://res.vmallres.com/pimages/...", sortOrder=0
```

### 4.6 上传颜色选项图片（关键！）

这是实现"切换颜色时图片跟着变"的关键：

```
catalog_option_images action=add,
    spuId={spuId},
    dimensionId={颜色维度ID},
    optionId={具体颜色选项ID},
    imageUrl="https://res.vmallres.com/pimages/...",
    sortOrder=0
```

每个颜色选项建议上传 3 张图（主图 + 2 张角度图），足够展示差异。

**注意：只能用 `imageUrl`（公开 URL），不能用 `localPath`。** HMall MCP 运行在远端服务器上，无法访问本地文件系统。

---

## 重要约束与已知问题

### 反爬机制

| 场景 | 表现 | 应对 |
|---|---|---|
| 不加 `--compressed` | 返回乱码二进制 | 必须加 `--compressed` |
| 不加 User-Agent | 小概率被拦截 | 始终带浏览器 UA |
| curl 访问搜索页 | 返回 ~166 字节空白页 | 不要用搜索接口 |
| Python urllib/requests | 请求挂起/超时 | 只用 curl |
| 短时间大量请求 | 可能触发限流 | 适度间隔，每次下载一个页面 |

### 数据注意点

| 场景 | 说明 |
|---|---|
| `galleryData` 不随颜色变化 | SSR 固定返回默认 SKU 的图片，不要用它做颜色图 |
| `sbomCode` 对应关系 | 一个颜色有多个 sbomCode（对应不同存储版本），用 `colorSelectItem` 里的代表 sbomCode |
| 图片 URL 可能失效 | Vmall CDN 图片是公开的，但不保证永久可用 |
| 产品页可能改版 | `__NEXT_DATA__` 结构可能随 Vmall 前端升级而变化 |

### `galleryData` vs `groupPhotoList` 的区别

这是最容易踩的坑：

- **`galleryData`**（在 `mainData` 下）：SSR 渲染时固定的默认图片集，**不随 `sbomCode` 参数变化**，即使 URL 带了不同颜色的 sbomCode，返回的 galleryData 仍然相同。
- **`groupPhotoList`**（在 `current.base[sbomCode]` 下）：每个 SKU 独立的图片集，**按颜色区分**，这才是正确的颜色图片数据源。

---

## 完整示例：采集一个产品的全部数据

```bash
# 1. 下载
curl -s --compressed \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36" \
  "https://www.vmall.com/product/comdetail/index.html?prdId=10086133363559" \
  -o /tmp/vmall_mate80.html
```

```python
# 2. 解析
import json, re

with open('/tmp/vmall_mate80.html') as f:
    html = f.read()

matches = re.findall(r'__NEXT_DATA__.*?</script>', html, re.DOTALL)
json_match = re.search(r'>(\{.*?\})<', matches[0], re.DOTALL)
data = json.loads(json_match.group(1))
cur = data['props']['pageProps']['mainData']['current']

CDN = "https://res.vmallres.com/pimages"

# 3. 产品信息
print(f"产品: {cur['name']}")

# 4. 维度与选项
for dim in cur['productOptions']['gbomAttrDisplayList']:
    attrs = cur['productOptions']['gbomAttrMappings'][dim]
    values = list(dict.fromkeys(a['attrValue'] for a in attrs))
    print(f"  维度 [{dim}]: {values}")

# 5. 每种颜色的图片
for ci in cur['colorSelectItem']:
    color = ci['attrValue']
    sku = cur['base'][ci['sbomCode']]
    main = f"{CDN}{sku['photoPath']}800_800_{sku['photoName']}"
    gallery = [f"{CDN}{g['photoPath']}800_800_{g['photoName']}"
               for g in sku.get('groupPhotoList', [])]
    print(f"  颜色 [{color}]: 主图={main}, 角度图×{len(gallery)}")

# 6. 价格
for code, sku in cur['base'].items():
    print(f"  SKU {code}: ¥{sku.get('price', '?')}")
```

输出示例：
```
产品: HUAWEI Mate 80
  维度 [颜色]: ['曜石黑', '雪域白', '晨曦金', '云杉绿']
  维度 [版本]: ['12GB+256GB', '12GB+512GB', '16GB+512GB']
  维度 [CPU型号]: ['麒麟9020']
  颜色 [曜石黑]: 主图=https://res...800_800_7359...png, 角度图×10
  颜色 [雪域白]: 主图=https://res...800_800_E3CC...png, 角度图×10
  颜色 [晨曦金]: 主图=https://res...800_800_F6AC...png, 角度图×12
  颜色 [云杉绿]: 主图=https://res...800_800_28D4...png, 角度图×12
  SKU 2601010586321: ¥5499
  SKU 2601010586317: ¥4999
  ...
```

---

## 操作清单（Checklist）

按以下顺序执行，可完成一个产品从采集到上线的全流程：

- [ ] 通过浏览器导航获取 `prdId`
- [ ] `curl --compressed` 下载产品页 HTML（验证文件 >500KB）
- [ ] Python 解析 `__NEXT_DATA__` → 提取产品名称、维度、选项、价格
- [ ] Python 提取每种颜色的图片 URL（从 `base[sbomCode].groupPhotoList`）
- [ ] MCP `catalog_categories` → 创建/确认类目
- [ ] MCP `catalog_products` → 创建 SPU
- [ ] MCP `catalog_dimensions` → 创建维度 + 选项
- [ ] MCP `catalog_skus` → 创建 SKU（组合选项 + 价格）
- [ ] MCP `catalog_product_images` → 上传产品级图片（取默认颜色前 3 张）
- [ ] MCP `catalog_option_images` → 上传每种颜色的选项图片（每色 3 张）
- [ ] 验证：`catalog_option_images action=list` 确认图片已关联
