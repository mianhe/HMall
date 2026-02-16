# 华为商城手机示例数据（Pura 系列 + Mate 系列）

来源：华为商城 [vmall.com](https://www.vmall.com/) 手机类目下的 **Pura 系列**、**Mate 系列**，用于配置类目与商品表示例数据。

说明：因当前通过网页抓取（无浏览器 hover/点击），仅能直接打开商品详情页。以下为已抓取的 **2 个 Pura 商品 + 1 个 Mate 商品**（第二个 Mate 商品页抓取超时，可后续在商城手动选一个补充）。

---

## 1. 类目结构（与商城对应）

| 一级类目 | 二级类目   | 说明     |
|----------|------------|----------|
| 手机     | Pura 系列  | 华为 Pura 系列手机 |
| 手机     | Mate 系列  | 华为 Mate 系列手机 |

---

## 2. 商品与规格维度、选项

### 2.1 商品概览

| 系列       | 商品名称           | 传播名               | 详情页 prdId     | 价格（参考） |
|------------|--------------------|----------------------|------------------|--------------|
| Pura 系列  | HUAWEI Pura 80 Pro+ | HUAWEI Pura 80 Pro+  | 10086153236238   | ¥6499        |
| Pura 系列  | HUAWEI Pura 70 Pro  | HUAWEI Pura 70 Pro   | 10086821546239   | ¥6999        |
| Mate 系列  | HUAWEI Mate 70 Pro   | HUAWEI Mate 70 Pro   | 10086259366534   | ¥5199        |

### 2.2 维度（Dimension）与选项（Option）

**Pura 80 Pro+（prdId=10086153236238）**

| 维度名称 | 选项（option） |
|----------|----------------|
| 颜色     | 釉青、釉红、釉白、釉黑 |
| 版本     | 16GB+512GB、16GB+1TB |

**Pura 70 Pro（prdId=10086821546239）**

| 维度名称 | 选项（option） |
|----------|----------------|
| 颜色     | 羽砂黑、雪域白、罗兰紫 |
| 版本     | 12GB+256GB、12GB+512GB、12GB+1TB |

**Mate 70 Pro（prdId=10086259366534）**

| 维度名称 | 选项（option） |
|----------|----------------|
| 颜色     | 曜石黑、雪域白、云杉绿、风信紫 |
| 版本     | 12GB+256GB、12GB+512GB、12GB+1TB、16GB+512GB、12GB+512GB 鸿蒙NEXT先锋版、12GB+1TB 鸿蒙NEXT先锋版 |

---

## 3. 商品参数属性（demand 属性）汇总表

以下为各商品「主要参数」中与选配相关的属性，便于建表或做筛选。

| 属性名         | Pura 80 Pro+ | Pura 70 Pro | Mate 70 Pro |
|----------------|--------------|-------------|-------------|
| 传播名         | HUAWEI Pura 80 Pro+ | HUAWEI Pura 70 Pro | HUAWEI Mate 70 Pro |
| 系列           | Pura系列     | Pura系列    | Mate系列    |
| 型号           | LMR-AL10     | HBN-AL00    | PLR-AL00/PLR-AL30 |
| 上市时间       | 2025年6月    | 2024年4月   | 2024年11月  |
| 操作系统       | HarmonyOS 6.0 | HarmonyOS 4.2 | HarmonyOS 4.3 |
| 屏幕尺寸       | 6.8英寸      | 6.8英寸     | 6.9英寸     |
| 屏幕类型       | OLED，1–120Hz LTPO | OLED，1–120Hz LTPO | OLED，1–120Hz LTPO |
| 玻璃材质       | 第二代玄武钢化昆仑玻璃 | 第二代昆仑玻璃 | 第二代昆仑玻璃 |
| 运行内存（RAM）| 16GB         | 12GB        | 12GB        |
| 机身内存（ROM）| 512GB        | 512GB       | 256GB       |
| 电池容量       | 5700mAh      | 5050mAh     | 5500mAh     |
| 机身重量       | 约219克      | 约220克     | 约221克     |
| 防尘防水       | IP68、IP69   | IP68        | IP68、IP69  |

---

## 4. 商品图片与文件名

图片已下载到目录：`docs/bounded-contexts/catalog/sample-data/vmall-images/`。

### 4.1 按商品列出的图片文件名

| 商品           | 图片用途说明     | 文件名 |
|----------------|------------------|--------|
| HUAWEI Mate 70 Pro | 主图 800×800   | mate70pro_800_main.png |
| HUAWEI Mate 70 Pro | 详情图 428×428 1 | mate70pro_428_1.png |
| HUAWEI Mate 70 Pro | 详情图 428×428 2 | mate70pro_428_2.png |
| HUAWEI Mate 70 Pro | 详情图 428×428 3 | mate70pro_428_3.png |
| HUAWEI Pura 80 Pro+ | 主图/详情图     | （未抓取到 URL，可从详情页补） |
| HUAWEI Pura 70 Pro | 主图/详情图     | （未抓取到 URL，可从详情页补） |

### 4.2 图片文件名与商品、维度选项对应（示例）

用于「选项图」或「SKU 图」时，可与维度选项对应，例如：

| 商品           | 维度   | 选项     | 图片文件名（示例） |
|----------------|--------|----------|--------------------|
| Mate 70 Pro    | 主图   | -        | mate70pro_800_main.png |
| Mate 70 Pro    | 颜色   | 雪域白   | mate70pro_428_1.png |
| Mate 70 Pro    | 颜色   | 曜石黑   | mate70pro_428_2.png |
| Mate 70 Pro    | 颜色   | 云杉绿   | mate70pro_428_3.png |
| Pura 80 Pro+   | 颜色   | 釉黑/釉白等 | （待补充） |
| Pura 70 Pro    | 颜色   | 罗兰紫/羽砂黑等 | （待补充） |

---

## 5. 用于导入类目/商品表的结构化数据（JSON 摘要）

便于直接贴入配置或脚本的摘要结构如下（仅示意，可按你方表结构再改）。

```json
{
  "categories": [
    { "name": "手机", "children": [
      { "name": "Pura系列" },
      { "name": "Mate系列" }
    ]}
  ],
  "products": [
    {
      "name": "HUAWEI Pura 80 Pro+",
      "category": "Pura系列",
      "prdId": "10086153236238",
      "dimensions": [
        { "name": "颜色", "options": ["釉青", "釉红", "釉白", "釉黑"] },
        { "name": "版本", "options": ["16GB+512GB", "16GB+1TB"] }
      ],
      "imageFiles": []
    },
    {
      "name": "HUAWEI Pura 70 Pro",
      "category": "Pura系列",
      "prdId": "10086821546239",
      "dimensions": [
        { "name": "颜色", "options": ["羽砂黑", "雪域白", "罗兰紫"] },
        { "name": "版本", "options": ["12GB+256GB", "12GB+512GB", "12GB+1TB"] }
      ],
      "imageFiles": []
    },
    {
      "name": "HUAWEI Mate 70 Pro",
      "category": "Mate系列",
      "prdId": "10086259366534",
      "dimensions": [
        { "name": "颜色", "options": ["曜石黑", "雪域白", "云杉绿", "风信紫"] },
        { "name": "版本", "options": ["12GB+256GB", "12GB+512GB", "12GB+1TB", "16GB+512GB", "12GB+512GB 鸿蒙NEXT先锋版", "12GB+1TB 鸿蒙NEXT先锋版"] }
      ],
      "imageFiles": ["mate70pro_800_main.png", "mate70pro_428_1.png", "mate70pro_428_2.png", "mate70pro_428_3.png"]
    }
  ]
}
```

---

## 6. 数据来源链接

| 商品           | 华为商城详情页 |
|----------------|----------------|
| Pura 80 Pro+   | https://www.vmall.com/product/comdetail/index.html?prdId=10086153236238&sbomCode=2601010565823 |
| Pura 70 Pro    | https://www.vmall.com/product/comdetail/index.html?prdId=10086821546239 |
| Mate 70 Pro    | https://www.vmall.com/product/comdetail/index.html?prdId=10086259366534&sbomCode=2601010519505 |

如需补全「第二个 Mate 系列商品」或 Pura/Mate 的主图，可在浏览器中打开上述链接，从页面中获取图片 URL 后下载，并更新本表中的 `imageFiles` 与「图片文件名」表。
