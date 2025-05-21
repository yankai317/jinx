
CREATE TABLE `assignment_detail` (
                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                     `assignment_id` bigint NOT NULL COMMENT 'common_visibility中类型为指派的id',
                                     `assign_record_id` bigint DEFAULT NULL COMMENT '关联指派记录ID',
                                     `userid` bigint NOT NULL COMMENT 'user表的用户id',
                                     `biz_id` varchar(255) NOT NULL COMMENT '业务id，根据类型拼接，比如 [type]_[id], train_id,用于处理同一个业务id被指派多次，但是只能通知一次',
                                     `status` varchar(50) NOT NULL COMMENT '状态表',
                                     `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                     `attributes` text COMMENT '扩展属性字段，内容为json',
                                     `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                     `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                     `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                     `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                     `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                     `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                     `finish_time` datetime DEFAULT NULL COMMENT '任务结束时间',
                                     `type` varchar(50) DEFAULT NULL COMMENT '类型:courses[课程],map[地图],train[培训],exam[考试]',
                                     `type_id` bigint DEFAULT NULL COMMENT '关联的id，可以是课程、培训、考试等id',
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='指派明细表'

CREATE TABLE `category` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                            `name` varchar(50) NOT NULL COMMENT '分类名称',
                            `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类id，顶级分类为0',
                            `path` varchar(255) NOT NULL COMMENT '分类路径，格式：1,2,3',
                            `level` int NOT NULL COMMENT '分类层级，从1开始',
                            `sort` int NOT NULL DEFAULT '0' COMMENT '同级分类排序',
                            `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                            `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                            `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                            `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                            `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `attributes` text COMMENT '扩展属性字段，内容为json',
                            `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                            PRIMARY KEY (`id`),
                            KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类表';

CREATE TABLE `certificate` (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                               `name` varchar(100) NOT NULL COMMENT '证书名称',
                               `description` varchar(500) DEFAULT NULL COMMENT '证书描述',
                               `template_url` varchar(255) DEFAULT NULL COMMENT '证书模板URL',
                               `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `expire_time` datetime DEFAULT NULL COMMENT '过期时间，null表示永不过期',
                               `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                               `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                               `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                               `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                               `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               `attributes` text COMMENT '扩展属性字段，内容为json',
                               `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                               PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='证书表';

CREATE TABLE `common_range` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                `model_type` varchar(50) NOT NULL COMMENT '可见性、指派、协同管理',
                                `type_id` bigint NOT NULL COMMENT '关联的id，可以是课程、培训、考试等id',
                                `type` varchar(50) NOT NULL COMMENT '类型:courses[课程],map[地图],train[培训],exam[考试]',
                                `type_biz_id` varchar(255) NOT NULL COMMENT '构建一个业务搜索键',
                                `target_type` varchar(50) DEFAULT NULL COMMENT '目标类型：1-department[部门]，2-role[角色]，3-user[用户]',
                                `target_ids` varchar(5000) DEFAULT NULL COMMENT '目标ids,例[1,2,3,4]，根据 target_type 关联不同表',
                                `start_time` datetime DEFAULT NULL COMMENT '开始时间',
                                `end_time` datetime DEFAULT NULL COMMENT '完成时间',
                                `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `attributes` text COMMENT '扩展属性字段，内容为json',
                                `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                PRIMARY KEY (`id`),
                                FULLTEXT KEY `idx_target_ids` (`target_ids`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考试可见范围表';

CREATE TABLE `Courses` (
                           `id` int unsigned NOT NULL AUTO_INCREMENT COMMENT '课程ID',
                           `title` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程名称',
                           `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程类型,video, document, series, article',
                           `cover_image` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '封面图路径',
                           `instructor_id` int unsigned DEFAULT NULL COMMENT '讲师ID（仅视频/系列课）',
                           `description` text COLLATE utf8mb4_unicode_ci COMMENT '简介（最多500字）',
                           `if_is_citable` INT DEFAULT 0 COMMENT '是否可引用：1-可引用，0-不可引用',
                           `credit` int NOT NULL DEFAULT '0' COMMENT '学分',
                           `category_ids` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '分类ids,格式:1,2,3',
                           `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft' COMMENT '状态, draft, published',
                           `allow_comments` tinyint(1) DEFAULT '1' COMMENT '是否允许评论：1-允许，0-不允许',
                           `is_top` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否置顶',
                           `view_count` int unsigned NOT NULL DEFAULT '0' COMMENT '查看数',
                           `complete_count` int unsigned NOT NULL DEFAULT '0' COMMENT '完成人数',
                           `article` text COLLATE utf8mb4_unicode_ci COMMENT '文章',
                           `appendix_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '附件类型',
                           `appendix_path` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文件存储路径',
                           `is_deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
                           `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
                           `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                           `creator_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人名称',
                           `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                           `updater_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人名称',
                           `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           `attributes` text COLLATE utf8mb4_unicode_ci COMMENT '扩展属性字段，内容为json',
                           `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                           PRIMARY KEY (`id`),
                           KEY `idx_status_type` (`status`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程表';

CREATE TABLE `department` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                              `department_name` varchar(100) NOT NULL COMMENT '部门名称',
                              `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '上级部门ID，顶级部门为0',
                              `department_path` varchar(255) NOT NULL COMMENT '部门路径，格式：1,2,3',
                              `department_level` int NOT NULL COMMENT '部门层级，从1开始',
                              `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号',
                              `external_id` varchar(100) DEFAULT NULL COMMENT '外部系统部门ID',
                              `external_source` varchar(50) DEFAULT NULL COMMENT '外部系统来源，如：dingtalk、feishu',
                              `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                              `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                              `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                              `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                              `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              `attributes` text COMMENT '扩展属性字段，内容为json',
                              `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_external_id` (`external_id`,`external_source`),
                              KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';

CREATE TABLE `department_user` (
                                   `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                   `department_id` bigint NOT NULL COMMENT '部门ID，关联department表',
                                   `department_path` VARCHAR(512) COMMENT '部门路径，格式：1,2,3 表示从顶级部门到当前部门的ID路径',
                                   `user_id` bigint NOT NULL COMMENT '用户ID，关联user表',
                                   `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                   `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                   `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                   `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                   `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                   `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                   `attributes` text COMMENT '扩展属性字段，内容为json',
                                   `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `uk_department_user` (`department_id`,`user_id`),
                                   KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门用户关联表';

CREATE TABLE `function_permission` (
                                       `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                       `permission_name` varchar(50) NOT NULL COMMENT '权限名称',
                                       `permission_code` varchar(50) NOT NULL COMMENT '权限编码',
                                       `permission_type` varchar(20) NOT NULL COMMENT '权限类型：menu-菜单，button-按钮，api-接口',
                                       `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父权限ID，顶级权限为0',
                                       `permission_path` varchar(255) NOT NULL COMMENT '权限路径，格式：1,2,3',
                                       `permission_level` int NOT NULL COMMENT '权限层级，从1开始',
                                       `resource_path` varchar(255) DEFAULT NULL COMMENT '资源路径，如菜单路径、接口路径',
                                       `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                       `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                       `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                       `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                       `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                       `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                       `attributes` text COMMENT '扩展属性字段，内容为json',
                                       `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                       PRIMARY KEY (`id`),
                                       UNIQUE KEY `uk_permission_code` (`permission_code`),
                                       KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='功能权限表';

CREATE TABLE `function_role` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                 `role_name` varchar(50) NOT NULL COMMENT '角色名称',
                                 `role_code` varchar(50) NOT NULL COMMENT '角色编码',
                                 `role_description` varchar(255) DEFAULT NULL COMMENT '角色描述',
                                 `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                 `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                 `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                 `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                 `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `attributes` text COMMENT '扩展属性字段，内容为json',
                                 `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci AUTO_INCREMENT=1000 COMMENT='功能权限角色表';

CREATE TABLE `function_role_permission` (
                                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                            `function_role_id` bigint NOT NULL COMMENT '功能权限角色ID，关联function_role表',
                                            `permission_id` bigint NOT NULL COMMENT '权限ID，关联function_permission表',
                                            `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                            `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                            `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                            `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                            `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                            `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                            `attributes` text COMMENT '扩展属性字段，内容为json',
                                            `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                            PRIMARY KEY (`id`),
                                            UNIQUE KEY `uk_role_permission` (`function_role_id`,`permission_id`),
                                            KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='功能权限角色权限关联表';

CREATE TABLE `function_role_user` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                      `function_role_id` bigint NOT NULL COMMENT '功能权限角色ID，关联function_role表',
                                      `user_id` bigint NOT NULL COMMENT '用户ID，关联user表',
                                      `department_id` bigint DEFAULT NULL COMMENT '部门ID，关联department表，表示用户在该部门下拥有此角色权限',
                                      `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                      `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                      `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                      `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                      `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `attributes` text COMMENT '扩展属性字段，内容为json',
                                      `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_role_user_dept` (`function_role_id`,`user_id`,`department_id`),
                                      KEY `idx_user_id` (`user_id`),
                                      KEY `idx_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='功能权限角色用户关联表';


CREATE TABLE `learning_map` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                `name` varchar(255) NOT NULL COMMENT '学习地图名称',
                                `cover` varchar(255) DEFAULT NULL COMMENT '封面图片地址',
                                `introduction` varchar(600) DEFAULT NULL COMMENT '简介',
                                `credit_rule` tinyint NOT NULL DEFAULT '0' COMMENT '发放学分规则：0-按整个学习地图给分 1-按每个学习阶段给分',
                                `elective_credit` decimal(5,2) DEFAULT '0.00' COMMENT '选修学分',
                                `required_credit` decimal(5,2) DEFAULT '0.00' COMMENT '必修学分',
                                `category_ids` varchar(2048) DEFAULT NULL COMMENT '分类ids 1,2,3,4',
                                `certificate_rule` tinyint NOT NULL DEFAULT '0' COMMENT '发放证书规则：0-不发放证书 1-按整个学习地图发证 2-按每个学习阶段发证',
                                `certificate_id` bigint DEFAULT NULL COMMENT '证书ID',
                                `dingtalk_group` tinyint NOT NULL DEFAULT '0' COMMENT '是否同步创建钉钉培训群：0-否 1-是',
                                `dingtalk_group_id` varchar(100) DEFAULT NULL COMMENT '钉钉群ID',
                                `start_time` datetime DEFAULT NULL COMMENT '开放开始时间',
                                `end_time` datetime DEFAULT NULL COMMENT '开放结束时间',
                                `unlock_mode` tinyint NOT NULL DEFAULT '0' COMMENT '解锁方式：0-按阶段和任务解锁 1-按阶段解锁 2-自由模式',
                                `theme` varchar(50) NOT NULL DEFAULT 'business' COMMENT '地图主题：business-商务简约 tech-动感科技 farm-农场时光 chinese-中国元素 list-列表模式',
                                `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                `attributes` text COMMENT '扩展属性字段，内容为json',
                                `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
                                PRIMARY KEY (`id`),
                                KEY `idx_name` (`name`),
                                KEY `idx_unlock_mode` (`unlock_mode`),
                                KEY `idx_start_time` (`start_time`),
                                KEY `idx_end_time` (`end_time`),
                                KEY `idx_is_del` (`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习地图表';

CREATE TABLE `learning_map_stage` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `map_id` bigint NOT NULL COMMENT '学习地图ID',
                                      `name` varchar(255) NOT NULL COMMENT '阶段名称',
                                      `stage_order` int NOT NULL COMMENT '阶段顺序号',
                                      `open_type` tinyint NOT NULL DEFAULT '0' COMMENT '开放类型：0-不设置开放时间 1-设置固定开放时间 2-设置学员学习期限',
                                      `start_time` datetime DEFAULT NULL COMMENT '固定开放开始时间',
                                      `end_time` datetime DEFAULT NULL COMMENT '固定开放结束时间',
                                      `duration_days` int DEFAULT NULL COMMENT '学习期限(天)',
                                      `credit` decimal(5,2) DEFAULT '0.00' COMMENT '阶段学分(按阶段给分时使用)',
                                      `certificate_id` bigint DEFAULT NULL COMMENT '证书ID(按阶段发证时使用)',
                                      `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                      `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                      `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                      `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                      `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `attributes` text COMMENT '扩展属性字段，内容为json',
                                      `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
                                      PRIMARY KEY (`id`),
                                      KEY `idx_map_id` (`map_id`),
                                      KEY `idx_stage_order` (`stage_order`),
                                      KEY `idx_open_type` (`open_type`),
                                      KEY `idx_start_time` (`start_time`),
                                      KEY `idx_end_time` (`end_time`),
                                      KEY `idx_is_del` (`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习地图阶段表';

CREATE TABLE `org_role` (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                            `role_name` varchar(50) NOT NULL COMMENT '角色名称',
                            `role_description` varchar(255) DEFAULT NULL COMMENT '角色描述',
                            `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类id，顶级分类为0',
                            `external_id` varchar(100) DEFAULT NULL COMMENT '外部系统角色ID',
                            `external_source` varchar(50) DEFAULT NULL COMMENT '外部系统来源，如：dingtalk、feishu',
                            `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                            `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                            `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                            `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                            `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                            `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                            `attributes` text COMMENT '扩展属性字段，内容为json',
                            `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                            PRIMARY KEY (`id`),
                            UNIQUE KEY `uk_external_id` (`external_id`,`external_source`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci AUTO_INCREMENT=1000  COMMENT='组织架构角色表';


CREATE TABLE `org_role_user` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                 `org_role_id` bigint NOT NULL COMMENT '组织架构角色ID，关联org_role表',
                                 `user_id` bigint NOT NULL COMMENT '用户ID，关联user表',
                                 `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                 `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                 `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                 `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                 `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `attributes` text COMMENT '扩展属性字段，内容为json',
                                 `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                 PRIMARY KEY (`id`),
                                 UNIQUE KEY `uk_role_user` (`org_role_id`,`user_id`),
                                 KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织架构角色用户关联表';

CREATE TABLE `train` (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                         `name` varchar(255) NOT NULL COMMENT '培训名称',
                         `cover` varchar(255) DEFAULT NULL COMMENT '封面图片地址',
                         `introduction` text COMMENT '培训简介',
                         `if_is_citable` INT DEFAULT 0 COMMENT '是否可引用：1-可引用，0-不可引用',
                         `credit` int NOT NULL DEFAULT '0' COMMENT '学分',
                         `category_ids` varchar(2048) DEFAULT NULL COMMENT '分类ids, 格式 1,2,3',
                         `allow_comment` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许评论: 0-不允许 1-允许',
                         `certificate_id` bigint DEFAULT NULL COMMENT '证书ID',
                         `status` varchar(32) NOT NULL DEFAULT 'draft' COMMENT '状态,draft, published',
                         `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                         `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                         `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                         `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                         `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                         `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                         `attributes` text COMMENT '扩展属性字段，内容为json',
                         `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
                         PRIMARY KEY (`id`),
                         KEY `idx_name` (`name`),
                         KEY `idx_certificate_id` (`certificate_id`),
                         KEY `idx_is_del` (`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训信息表';

CREATE TABLE `content_relation` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                    `biz_id` bigint NOT NULL COMMENT '业务ID',
                                    `biz_type` varchar(128) NOT NULL COMMENT '业务类型：培训/地图阶段',
                                    `content_type` varchar(50) NOT NULL COMMENT '关联内容类型: EXAM-考试 ASSIGNMENT-作业 SURVEY-调研 CONTENT-普通内容',
                                    `content_id` bigint DEFAULT NULL COMMENT '关联内容ID（外部ID）',
                                    `content_url` varchar(512) DEFAULT NULL COMMENT '关联内容URL地址',
                                    `content_biz_type` varchar(32) DEFAULT NULL COMMENT '业务类型：课程/培训',
                                    `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序序号',
                                    `is_required` tinyint NOT NULL DEFAULT '1' COMMENT '是否必修：0-选修 1-必修',
                                    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                    `attributes` text COMMENT '扩展属性字段，内容为json',
                                    `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                    `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                    `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_biz_id_type` (`biz_id`, `biz_type`),
                                    KEY `idx_content_id_type` (`content_id`, `content_type`),
                                    KEY `idx_is_del` (`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训内容关联表';

CREATE TABLE `operation_log` (
                                 `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                 `biz_id` bigint NOT NULL COMMENT '业务ID',
                                 `biz_Type` varchar(50) NOT NULL COMMENT '业务类型',
                                 `operation_type` varchar(50) NOT NULL COMMENT '操作类型：CREATE-创建培训 UPDATE-修改培训 DELETE-删除培训 ASSIGN-指派培训 CANCEL-取消指派',
                                 `operation_detail` text COMMENT '操作详情JSON，记录变更前后的具体内容',
                                 `operation_summary` text COMMENT '操作概要，用于前端展示',
                                 `operator_id` bigint NOT NULL COMMENT '操作人ID',
                                 `operator_name` varchar(100) NOT NULL COMMENT '操作人姓名',
                                 `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
                                 `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                 `attributes` text COMMENT '扩展属性字段，内容为json',
                                 `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                 `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                 `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                 `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                 `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_biz_id` (`biz_id`),
                                 KEY `idx_operation_type` (`operation_type`),
                                 KEY `idx_operator_id` (`operator_id`),
                                 KEY `idx_operation_time` (`operation_time`),
                                 KEY `idx_is_del` (`is_del`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训操作记录表';


CREATE TABLE `user` (
                        `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                        `user_id` bigint NOT NULL COMMENT '用户id,数字。不连续，随机生成不重复',
                        `employee_no` varchar(50) DEFAULT NULL COMMENT '工号',
                        `nickname` varchar(50) NOT NULL COMMENT '用户昵称',
                        `avatar` varchar(255) DEFAULT NULL COMMENT '用户头像URL',
                        `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                        `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                        `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                        `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                        `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        `attributes` text COMMENT '扩展属性字段，内容为json',
                        `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                        PRIMARY KEY (`id`),
                        KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE `user_certificate` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                    `user_id` bigint NOT NULL COMMENT '用户id',
                                    `certificate_id` bigint NOT NULL COMMENT '证书id',
                                    `source_type` varchar(255) DEFAULT NULL COMMENT '证书来源: 考试、培训、学习地图',
                                    `source_id` bigint NOT NULL COMMENT '关联来源id',
                                    `issue_time` datetime NOT NULL COMMENT '颁发时间',
                                    `expire_time` datetime DEFAULT NULL COMMENT '过期时间，null表示永不过期',
                                    `certificate_no` varchar(100) NOT NULL COMMENT '证书编号',
                                    `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-有效，1-已过期，2-已撤销',
                                    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                    `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                    `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                    `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `attributes` text COMMENT '扩展属性字段，内容为json',
                                    `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                    PRIMARY KEY (`id`),
                                    KEY `idx_certificate_id` (`certificate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户证书表';

CREATE TABLE `user_third_party` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                    `user_id` bigint NOT NULL COMMENT '用户ID，关联user表',
                                    `third_party_type` varchar(20) NOT NULL COMMENT '第三方平台类型，如：dingtalk、feishu',
                                    `third_party_user_id` varchar(100) NOT NULL COMMENT '第三方平台用户唯一标识',
                                    `third_party_username` varchar(100) DEFAULT NULL COMMENT '第三方平台用户名',
                                    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                    `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                    `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                    `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `attributes` text COMMENT '扩展属性字段，内容为json',
                                    `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                    PRIMARY KEY (`id`),
                                    UNIQUE KEY `uk_third_party_user` (`third_party_type`,`third_party_user_id`),
                                    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户第三方授权关联表';


CREATE TABLE `third_crop_info` (
                                    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
                                    `third_id` varchar(255) NOT NULL COMMENT '三方唯一id，比如钉钉就是企业crop_id',
                                    `third_type` varchar(20) NOT NULL COMMENT '第三方平台类型，如：dingtalk、feishu',
                                    `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                    `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                    `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                    `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                    `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    `attributes` text COMMENT '扩展属性字段，内容为json',
                                    `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
                                    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='三方企业信息';

CREATE TABLE `assign_records` (
      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
      `type` varchar(50) NOT NULL COMMENT '类型:courses[课程],map[地图],train[培训],exam[考试]',
      `type_id` bigint NOT NULL COMMENT '关联的id，可以是课程、培训、考试等id',
      `range_ids` varchar(255) NOT NULL COMMENT '范围ids',
      `assign_type` varchar(20) NOT NULL DEFAULT 'once' COMMENT '指派类型：once-单次通知 auto-自动通知',
      `status` varchar(20) NOT NULL DEFAULT 'wait' COMMENT '通知状态：wait-待通知、process-通知中、success-已通知',
      `assign_finished_time_type` varchar(20) DEFAULT NULL COMMENT '通知完成时间类型：custom-自定义时间、one_week-1周、two_week-2周、four_week-4周',
      `custom_finished_time` datetime DEFAULT  NULL COMMENT '自定义结束时间',
      `if_is_notify_exist_user` tinyint(1) DEFAULT  NULL COMMENT '是否通知已存在用户',
      `notify_user_after_join_date` datetime DEFAULT  NULL COMMENT '通知当前时间之后的用户',
      `deadline` datetime DEFAULT  NULL COMMENT '截止时间',
      `assign_finished_time` datetime DEFAULT  NULL COMMENT '自动指派截止时间',
      `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `attributes` text COMMENT '扩展属性字段，内容为json',
      `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
      `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
      `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
      `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
      `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `is_del` int NOT NULL DEFAULT '0' COMMENT '逻辑删除字段: 是否删除 1-已删 0-正常',
      PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='指派记录表';

CREATE TABLE `banner` (
                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自增id',
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
                          `type` varchar(16) DEFAULT 'PC',
                          PRIMARY KEY (`id`),
                          KEY `idx_sort` (`sort`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页轮播图表';

CREATE TABLE `user_learning_task` (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                      `user_id` bigint NOT NULL COMMENT '用户ID',
                                      `source` varchar(50) DEFAULT NULL COMMENT '学习来源：ASSIGN-指派 SELF-自学',
                                      `biz_type` varchar(50) DEFAULT NULL COMMENT '学习业务类型，COURSE-课程，SERIES_COURSE-系列课，LEARNING_MAP-学习地图,MAP_STAGE-学习阶段',
                                      `biz_id` bigint DEFAULT NULL COMMENT '学习业务id，课程id/系列课id/学习地图id/学习阶段id',
                                      `parent_type` varchar(50) DEFAULT NULL COMMENT '父节点业务类型，COURSE-课程，SERIES_COURSE-系列课，LEARNING_MAP-学习地图,MAP_STAGE-学习阶段',
                                      `earned_credit` bigint NOT NULL DEFAULT '0' COMMENT '已获得学分',
                                      `certificate_issued` tinyint NOT NULL DEFAULT '0' COMMENT '是否已发放证书：0-否 1-是',
                                      `certificate_id` bigint default NULL COMMENT '关联证书id',
                                      `parent_id` bigint DEFAULT NULL COMMENT '父节点学习业务id，课程id/系列课id/学习地图id/学习阶段id',
                                      `status` tinyint NOT NULL DEFAULT '0' COMMENT '完成状态：0-未开始 1-学习中 2-已完成,-1-超时未完成',
                                      `study_duration` int NOT NULL DEFAULT '0' COMMENT '学习时长(分钟)',
                                      `progress` int NOT NULL DEFAULT '0' COMMENT '学习进度(百分比)',
                                      `score` decimal(5,2) DEFAULT NULL COMMENT '得分(考试/作业)',
                                      `pass_status` tinyint DEFAULT NULL COMMENT '通过状态：0-未通过 1-已通过',
                                      `assign_time` datetime DEFAULT NULL COMMENT '指派时间',
                                      `start_time` datetime DEFAULT NULL COMMENT '开始学习时间',
                                      `deadline` datetime DEFAULT NULL COMMENT '学习截止时间',
                                      `completion_time` datetime DEFAULT NULL COMMENT '完成时间',
                                      `last_study_time` datetime DEFAULT NULL COMMENT '最后学习时间',
                                      `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `creator_id` bigint DEFAULT NULL COMMENT '创建人id',
                                      `creator_name` varchar(255) DEFAULT NULL COMMENT '创建人名称',
                                      `updater_id` bigint DEFAULT NULL COMMENT '更新人id',
                                      `attributes` text COMMENT '扩展属性字段，内容为json。需要记录已完成阶段数，已完成必修任务数，已完成选修任务数，已获得学分',
                                      `updater_name` varchar(255) DEFAULT NULL COMMENT '更新人名称',
                                      `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `is_del` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除: 0-正常 1-删除',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_user_task` (`user_id`,`biz_type`,`biz_id`),
                                      KEY `idx_parent_type_id` (`parent_type`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户学习记录表';
CREATE UNIQUE INDEX idx_search_key ON user_learning_task(search_key);




create unique index department_user_department_id_user_id_uindex
    on department_user (department_id, user_id);

create unique index org_role_user_org_role_id_user_id_uindex
    on org_role_user (org_role_id, user_id);

ALTER TABLE `courses`  ADD guest_name VARCHAR(128) default NULL;
ALTER TABLE categories AUTO_INCREMENT = 100000;
ALTER TABLE category ADD FULLTEXT KEY idx_path_fulltext (path);


