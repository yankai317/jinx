-- 为department表的department_path字段创建全文索引SQL脚本

-- 1. 确保department表的department_path字段适合全文索引
-- 通常department_path存储格式为 "1,2,3,4" 表示部门路径
-- 确保字段长度足够且字符集为utf8mb4或utf8
ALTER TABLE department MODIFY COLUMN department_path VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

-- 2. 检查是否已存在全文索引
SELECT 
    INDEX_NAME
FROM 
    INFORMATION_SCHEMA.STATISTICS
WHERE 
    TABLE_SCHEMA = DATABASE() AND 
    TABLE_NAME = 'department' AND 
    INDEX_NAME = 'idx_department_path_fulltext';

-- 3. 创建全文索引 (如果不存在)
-- 注意: 使用InnoDB引擎的表需要MySQL 5.6+才支持全文索引
CREATE FULLTEXT INDEX idx_department_path_fulltext ON department(department_path);
ALTER TABLE category ADD FULLTEXT INDEX idx_path_fulltext (path);
-- 4. 也可以使用ALTER TABLE语法
-- ALTER TABLE department ADD FULLTEXT INDEX idx_department_path_fulltext (department_path);

-- 5. 为了进一步优化性能，给常用查询条件添加普通索引
ALTER TABLE department ADD INDEX idx_is_del (is_del);
ALTER TABLE department_user ADD INDEX idx_is_del (is_del);
ALTER TABLE department_user ADD INDEX idx_department_id (department_id);
ALTER TABLE department_user ADD INDEX idx_user_id (user_id);

-- 6. 修改后的优化查询示例（在Mapper XML中使用）:
/*
SELECT DISTINCT du.user_id
FROM department_user du
JOIN department d ON du.department_id = d.id
WHERE du.is_del = 0
AND d.is_del = 0
AND (
    d.id IN (1, 2, 3)  -- 直接部门ID
    OR 
    MATCH(d.department_path) AGAINST('+1* +2* +3*' IN BOOLEAN MODE)  -- 使用全文索引查询子部门
);
*/

-- 7. 全文索引配置参数，可以在MySQL配置文件中添加
/*
[mysqld]
# 最小索引词长度
ft_min_word_len=2
# 停用词文件
ft_stopword_file='/path/to/stopwords.txt'
*/

-- 8. 在创建或修改全文索引参数后，需要重建索引
-- REPAIR TABLE department QUICK;

-- 注意：全文索引对于LIKE语句的优化有限，使用MATCH AGAINST可获得更好性能
-- MATCH AGAINST语法在布尔模式下可以使用通配符和逻辑操作符
