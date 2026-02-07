# 商品限定上下文 - 两个调用场景图

> 用 Mermaid 画的调用顺序图，可在支持 Mermaid 的 Markdown 预览中查看（如 VS Code、GitHub、Cursor）。

---

## 场景一：应用启动时做了什么（调用顺序）

应用启动时的大致顺序：加载配置 → 连接数据库 → Hibernate 建表/更新表 → 注册 Bean → 启动 Web 容器。

```mermaid
sequenceDiagram
    participant User as 用户/运维
    participant Main as HmallApplication
    participant Spring as Spring Boot
    participant Config as application.yml
    participant DS as DataSource
    participant DB as PostgreSQL
    participant JPA as JPA/Hibernate
    participant Entities as Entity 扫描
    participant Beans as Bean 容器
    participant Tomcat as Tomcat

    User->>Main: 运行 main()
    Main->>Spring: SpringApplication.run(...)
    Spring->>Config: 加载 application.yml
    Config-->>Spring: datasource, jpa.hibernate.ddl-auto

    Spring->>DS: 创建 DataSource Bean
    DS->>DB: 建立连接 (jdbc:postgresql://localhost:5432/hmall)
    DB-->>DS: 连接就绪

    Spring->>JPA: 初始化 JPA / Hibernate
    JPA->>Entities: 扫描 @Entity（CategoryEntity, SpuEntity）
    Entities-->>JPA: 元数据
    JPA->>DB: ddl-auto: update → 建表或更新表（category, spu 等）
    DB-->>JPA: 表就绪

    Spring->>Beans: 扫描并创建 Bean
    Note over Beans: Controller, ApplicationService,<br/>RepositoryImpl, JpaRepository
    Beans-->>Spring: 依赖注入完成

    Spring->>Tomcat: 启动内嵌 Tomcat
    Tomcat->>Tomcat: 监听 8080
    Spring-->>User: 应用就绪，可接收请求
```

---

## 场景二：调用「创建商品」接口时的调用关系

假设客户端发送：`POST /api/products`，body 为 `{ "categoryId": 1, "name": "商品A", "description": "..." }`。

各层之间的调用顺序与模块关系如下。

```mermaid
sequenceDiagram
    participant Client as 客户端
    participant ProductController as ProductController<br/>(API 层)
    participant SpuApplicationService as SpuApplicationService<br/>(应用层)
    participant CategoryRepository as CategoryRepository<br/>(领域接口)
    participant SpuRepository as SpuRepository<br/>(领域接口)
    participant CategoryRepositoryImpl as CategoryRepositoryImpl<br/>(基础设施)
    participant SpuRepositoryImpl as SpuRepositoryImpl<br/>(基础设施)
    participant CategoryJpaRepository as CategoryJpaRepository
    participant SpuJpaRepository as SpuJpaRepository
    participant DB as PostgreSQL

    Client->>ProductController: POST /api/products + ProductCreateDto
    ProductController->>SpuApplicationService: create(categoryId, name, description)
    Note over SpuApplicationService: @Transactional 开启事务

    SpuApplicationService->>CategoryRepository: findById(categoryId)
    Note over CategoryRepository: 实际注入的是 CategoryRepositoryImpl
    CategoryRepository->>CategoryRepositoryImpl: findById(categoryId)
    CategoryRepositoryImpl->>CategoryJpaRepository: findById(categoryId)
    CategoryJpaRepository->>DB: SELECT FROM category WHERE id=?
    DB-->>CategoryJpaRepository: CategoryEntity
    CategoryJpaRepository-->>CategoryRepositoryImpl: CategoryEntity
    CategoryRepositoryImpl->>CategoryRepositoryImpl: toDomain(entity)
    CategoryRepositoryImpl-->>SpuApplicationService: Optional<Category>

    SpuApplicationService->>SpuApplicationService: orElseThrow (不存在则 404)
    SpuApplicationService->>CategoryRepository: existsByParentId(category.getId())
    CategoryRepository->>CategoryRepositoryImpl: existsByParentId(...)
    CategoryRepositoryImpl->>CategoryJpaRepository: existsByParentId(...)
    CategoryJpaRepository->>DB: SELECT 是否存在子类别
    DB-->>CategoryJpaRepository: true/false
    CategoryJpaRepository-->>CategoryRepositoryImpl: boolean
    CategoryRepositoryImpl-->>SpuApplicationService: boolean

    SpuApplicationService->>SpuApplicationService: 若存在子类别 → 抛 NotLeafCategoryException (400)
    SpuApplicationService->>SpuApplicationService: new Spu(categoryId, name, description)
    SpuApplicationService->>SpuRepository: save(spu)
    Note over SpuRepository: 实际注入的是 SpuRepositoryImpl
    SpuRepository->>SpuRepositoryImpl: save(spu)
    SpuRepositoryImpl->>SpuRepositoryImpl: toEntity(spu) → SpuEntity
    SpuRepositoryImpl->>SpuJpaRepository: save(spuEntity)
    SpuJpaRepository->>DB: INSERT INTO spu (...)
    DB-->>SpuJpaRepository: SpuEntity (含生成 id)
    SpuJpaRepository-->>SpuRepositoryImpl: SpuEntity
    SpuRepositoryImpl->>SpuRepositoryImpl: toDomain(entity)
    SpuRepositoryImpl-->>SpuApplicationService: Spu

    SpuApplicationService-->>ProductController: Spu
    Note over SpuApplicationService: 事务提交
    ProductController->>ProductController: toDto(created) → ProductDto
    ProductController-->>Client: 201 Created + ProductDto (JSON)
```

---

## 模块归属小结（场景二）

| 步骤 | 所在层 | 说明 |
|------|--------|------|
| 接收 DTO、调 ApplicationService、转 DTO 返回 | API (catalog.api) | Controller |
| 事务、校验（类别存在、叶子）、new Spu、调 Repository | 应用 (catalog.application) | SpuApplicationService |
| Category / Spu、Repository 接口 | 领域 (catalog.domain) | 无直接「调用」，由应用层使用 |
| findById / existsByParentId / save 的实现、Entity ↔ Domain 转换 | 基础设施 (catalog.infrastructure) | *RepositoryImpl |
| 真正执行 SQL | 基础设施 (JpaRepository + Hibernate) | 访问 PostgreSQL |

如需导出为图片，可使用 [Mermaid Live Editor](https://mermaid.live) 或 VS Code 的 Mermaid 插件。
