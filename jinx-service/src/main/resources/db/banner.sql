-- 创建banner表
CREATE TABLE `banner` (`id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
    `title` varchar(100) NOT NULL COMMENT '轮播图标题',
    `image_url` varchar(255) NOT NULL COMMENT '轮播图片URL',
    `link_url` varchar(255) NOT NULL COMMENT '链接URL',
    `sort` int NOT NULL DEFAULT '0' COMMENT '排序序号',
    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
    `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
    `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
    `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `attributes` text COMMENT '扩展属性字段，内容为json',
    `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页轮播图表';

-- 插入示例数据
INSERT INTO `banner` (`title`, `image_url`, `link_url`, `sort`, `creator_id`, `creator_name`)
VALUES 
('新员工培训', 'https://example.com/banner1.jpg', 'https://example.com/course/1', 1, 1, 'admin'),
('职业发展地图', 'https://example.com/banner2.jpg', 'https://example.com/map/1', 2, 1, 'admin'),
('热门课程推荐', 'https://example.com/banner3.jpg', 'https://example.com/course/featured', 3, 1, 'admin');
