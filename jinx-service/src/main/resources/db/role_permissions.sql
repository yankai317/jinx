-- 插入权限管理顶级模块
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
      ('权限管理', 'role', 'menu', 0, '5', 1, '/role', 1, '系统管理员');

-- 插入权限管理子权限
INSERT INTO function_permission (
    permission_name,
    permission_code,
    permission_type,
    parent_id,
    permission_path,
    permission_level,
    resource_path
) VALUES
      ('查看角色列表', 'role:list', 'button', 5, '5,52', 2, '/api/role/list'),
      ('创建角色', 'role:create', 'button', 5, '5,53', 2, '/api/role/create'),
      ('修改角色', 'role:update', 'button', 5, '5,54', 2, '/api/role/update'),
      ('删除角色', 'role:delete', 'button', 5, '5,55', 2, '/api/role/delete'),
      ('给角色添加用户', 'role:addUsers', 'button', 5, '5,56', 2, '/api/role/addUsers'),
      ('获取所有权限', 'permission:getAllPermissions', 'button', 5, '5,57', 2, '/api/permission/getAllPermissions');

-- 创建超级管理员角色
INSERT INTO function_role (
    role_name,
    role_code,
    role_description,
    creator_id,
    creator_name
) VALUES
      ('超级管理员', 'SUPER_ADMIN', '拥有系统所有权限', 1, '系统管理员');

-- 获取超级管理员角色ID
SET @admin_role_id = LAST_INSERT_ID();

-- 获取所有权限ID
SET @permission_ids = (SELECT GROUP_CONCAT(id) FROM function_permission WHERE is_del = 0);

-- 为超级管理员角色分配所有权限
INSERT INTO function_role_permission (
    function_role_id,
    permission_id,
    creator_id,
    creator_name
)
SELECT @admin_role_id, id, 1, '系统管理员'
FROM function_permission
WHERE is_del = 0;

-- 创建普通管理员角色
INSERT INTO function_role (
    role_name,
    role_code,
    role_description,
    creator_id,
    creator_name
) VALUES
      ('普通管理员', 'NORMAL_ADMIN', '拥有部分管理权限', 1, '系统管理员');

-- 获取普通管理员角色ID
SET @normal_admin_role_id = LAST_INSERT_ID();

-- 为普通管理员角色分配部分权限（课程管理和培训管理）
INSERT INTO function_role_permission (
    function_role_id,
    permission_id,
    creator_id,
    creator_name
)
SELECT @normal_admin_role_id, id, 1, '系统管理员'
FROM function_permission
WHERE (parent_id = 1 OR parent_id = 3 OR id = 1 OR id = 3) AND is_del = 0;
