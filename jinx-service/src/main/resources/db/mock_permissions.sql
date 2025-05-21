-- 插入顶级模块（假设id=1~4）
INSERT INTO function_permission (
    permission_name,
    permission_code,
    permission_type,
    parent_id,
    permission_path,
    permission_level,
    resource_path,
    creator_id,
    creator_name
) VALUES
      ('课程管理', 'course', 'menu', 0, '1', 1, '/course', 1, '系统管理员'),
      ('学习地图管理', 'map', 'menu', 0, '2', 1, '/map', 1, '系统管理员'),
      ('培训管理', 'train', 'menu', 0, '3', 1, '/train', 1, '系统管理员'),
      ('证书管理', 'certificate', 'menu', 0, '4', 1, '/certificate', 1, '系统管理员');

-- 插入子权限（假设id从5开始自增）
-- 课程管理(course)子权限
INSERT INTO function_permission (
    permission_name,
    permission_code,
    permission_type,
    parent_id,
    permission_path,
    permission_level,
    resource_path
) VALUES
      ('创建课程', 'course:create', 'button', 1, '1,5', 2, '/api/course/create'),
      ('批量删除课程', 'course:batchDelete', 'button', 1, '1,6', 2, '/api/course/batch-delete'),
      ('批量修改创建者', 'course:batchChangeCreator', 'button', 1, '1,7', 2, '/api/course/change-creator'),
      ('发布课程', 'course:publish', 'button', 1, '1,8', 2, '/api/course/publish'),
      ('编辑课程', 'course:edit', 'button', 1, '1,9', 2, '/api/course/edit'),
      ('可见性设置', 'course:visibility', 'button', 1, '1,10', 2, '/api/course/visibility'),
      ('分类管理', 'course:category', 'button', 1, '1,11', 2, '/api/course/category'),
      ('置顶课程', 'course:top', 'button', 1, '1,12', 2, '/api/course/top'),
      ('学员管理', 'course:learners', 'button', 1, '1,13', 2, '/api/course/learners'),
      ('关联内容', 'course:related', 'button', 1, '1,14', 2, '/api/course/related'),
      ('删除课程', 'course:delete', 'button', 1, '1,15', 2, '/api/course/delete'),
      ('更新课程', 'course:update', 'button', 1, '1,16', 2, '/api/course/update');

-- 学习地图管理(map)子权限
INSERT INTO function_permission (
    permission_name,
    permission_code,
    permission_type,
    parent_id,
    permission_path,
    permission_level,
    resource_path
) VALUES
      ('分配地图', 'map:assign', 'button', 2, '2,17', 2, '/api/map/assign'),
      ('创建地图', 'map:create', 'button', 2, '2,18', 2, '/api/map/create'),
      ('批量删除地图', 'map:batchDelete', 'button', 2, '2,19', 2, '/api/map/batch-delete'),
      ('编辑地图', 'map:edit', 'button', 2, '2,20', 2, '/api/map/edit'),
      ('可见性设置', 'map:visibility', 'button', 2, '2,21', 2, '/api/map/visibility'),
      ('学习追踪', 'map:tracking', 'button', 2, '2,22', 2, '/api/map/tracking'),
      ('分类管理', 'map:category', 'button', 2, '2,23', 2, '/api/map/category'),
      ('分配记录', 'map:assignRecord', 'button', 2, '2,24', 2, '/api/map/assign-record'),
      ('置顶地图', 'map:top', 'button', 2, '2,25', 2, '/api/map/top'),
      ('分享地图', 'map:share', 'button', 2, '2,26', 2, '/api/map/share'),
      ('操作日志', 'map:log', 'button', 2, '2,27', 2, '/api/map/logs'),
      ('删除地图', 'map:delete', 'button', 2, '2,28', 2, '/api/map/delete'),
      ('创建阶段', 'map:stage:create', 'button', 2, '2,29', 2, '/api/map/stage/create'),
      ('创建任务', 'map:task:create', 'button', 2, '2,30', 2, '/api/map/task/create'),
      ('编辑任务', 'map:task:edit', 'button', 2, '2,31', 2, '/api/map/task/edit'),
      ('删除任务', 'map:task:delete', 'button', 2, '2,32', 2, '/api/map/task/delete'),
      ('删除阶段', 'map:stage:delete', 'button', 2, '2,33', 2, '/api/map/stage/delete'),
      ('编辑阶段', 'map:stage:edit', 'button', 2, '2,34', 2, '/api/map/stage/edit');

-- 培训管理(train)子权限
INSERT INTO function_permission (
    permission_name,
    permission_code,
    permission_type,
    parent_id,
    permission_path,
    permission_level,
    resource_path
) VALUES
      ('创建草稿培训', 'train:create:draft', 'button', 3, '3,35', 2, '/api/train/draft-create'),
      ('发布培训', 'train:create:publish', 'button', 3, '3,36', 2, '/api/train/publish'),
      ('分配培训', 'train:assign', 'button', 3, '3,37', 2, '/api/train/assign'),
      ('创建培训', 'train:create', 'button', 3, '3,38', 2, '/api/train/create'),
      ('删除培训', 'train:delete', 'button', 3, '3,39', 2, '/api/train/delete'),
      ('编辑培训', 'train:edit', 'button', 3, '3,40', 2, '/api/train/edit'),
      ('培训追踪', 'train:tracking', 'button', 3, '3,41', 2, '/api/train/tracking'),
      ('发布培训', 'train:publish', 'button', 3, '3,42', 2, '/api/train/publish'),
      ('取消发布', 'train:unpublish', 'button', 3, '3,43', 2, '/api/train/unpublish'),
      ('草稿编辑', 'train:edit:draft', 'button', 3, '3,44', 2, '/api/train/edit-draft'),
      ('发布编辑', 'train:edit:publish', 'button', 3, '3,45', 2, '/api/train/edit-publish');

-- 证书管理(certificate)子权限
INSERT INTO function_permission (
    permission_name,
    permission_code,
    permission_type,
    parent_id,
    permission_path,
    permission_level,
    resource_path
) VALUES
      ('撤销证书', 'certificate:revoke', 'button', 4, '4,46', 2, '/api/certificate/revoke'),
      ('创建证书', 'certificate:create', 'button', 4, '4,47', 2, '/api/certificate/create'),
      ('批量删除证书', 'certificate:batchDelete', 'button', 4, '4,48', 2, '/api/certificate/batch-delete'),
      ('编辑证书', 'certificate:edit', 'button', 4, '4,49', 2, '/api/certificate/edit'),
      ('用户证书管理', 'certificate:users', 'button', 4, '4,50', 2, '/api/certificate/users'),
      ('删除证书', 'certificate:delete', 'button', 4, '4,51', 2, '/api/certificate/delete');
