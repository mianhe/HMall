-- 移除已废弃字段 affects_appearance（与领域模型一致，SpecDimension 不再包含该属性）。
-- 若库中该列存在，执行一次即可。PostgreSQL:
ALTER TABLE spec_dimension DROP COLUMN IF EXISTS affects_appearance;
