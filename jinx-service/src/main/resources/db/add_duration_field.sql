
-- 添加duration字段到Courses表
ALTER TABLE `Courses` 
ADD COLUMN `duration` int DEFAULT 0 COMMENT '视频时长（秒）' 
AFTER `appendix_path`;
