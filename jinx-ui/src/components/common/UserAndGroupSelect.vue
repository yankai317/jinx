<template>
    <div class="selection-content">
      <div class="selection-tabs">
        <div
          class="tab-item"
          :class="{ active: activeKey === 'org' }"
          @click="activeKey = 'org'"
        >
          按组织架构
        </div>
        <div
          class="tab-item"
          :class="{ active: activeKey === 'role' }"
          @click="activeKey = 'role'"
        >
          按角色
        </div>
      </div>

      <div class="main-content">
        <!-- Left Selection Area -->
        <div class="left-area">
          <div class="search-bar">
            <a-input
              v-model:value="searchKeyword"
              placeholder="搜索"
              @input="handleSearch"
              @keyup="handleSearch"
              allowClear
            >
              <template #prefix>
                <search-outlined />
              </template>
            </a-input>
          </div>

          <a-breadcrumb class="breadcrumb-nav">
            <a-breadcrumb-item v-for="(item, index) in currentPath" :key="index">
              <a @click="navigateToBreadcrumb(index)">{{ item.name }}</a>
            </a-breadcrumb-item>
          </a-breadcrumb>

          <div class="selection-list">
            <div
              v-for="item in currentItems"
              :key="item.key"
              class="selection-item"
              :class="{ selected: isItemSelected(item) }"
            >
              <div class="item-content" @click="toggleSelectItem(item)">
                <div v-if="hasAvatar(item)" class="item-avatar" :style="{ backgroundImage: `url(${item.avatar})` }">
                </div>
                <div v-else class="item-avatar" :class="getItemClass(item)">
                  {{ getItemAvatarText(item) }}
                </div>
                <div class="item-name">{{ item.title }}</div>
              </div>
              <a-button
                v-if="hasChildren(item)"
                type="link"
                class="next-level-btn"
                @click="navigateToChildren(item)"
              >
                下级
              </a-button>
            </div>
          </div>
        </div>

        <!-- Right Selected Area -->
        <div class="right-area">
          <div class="selected-header">
            <div class="selected-title">已选择</div>
            <div class="selected-count">{{ selectedTargets.length }} 项</div>
          </div>
          <div class="selected-list">
            <div
              v-for="item in selectedTargets"
              :key="item.id"
              class="selected-item"
            >
              <div v-if="hasAvatar(item)" class="selected-item-avatar" :style="{ backgroundImage: `url(${item.avatar})` }">
              </div>
              <div v-else class="selected-item-avatar" :class="getItemClass(item)">
                {{ getItemAvatarText(item) }}
              </div>
              <div class="selected-item-name">{{ item.name }}</div>
              <a-button type="link" class="selected-item-remove" @click="removeTarget(item)">
                <close-outlined />
              </a-button>
            </div>
          </div>
        </div>
      </div>

      <!-- Action buttons for standalone mode -->
      <div v-if="standalone" class="action-buttons">
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" @click="handleConfirm">确定</a-button>
      </div>
    </div>
</template>

<script>
import { ref, computed, onMounted, defineComponent, watch } from 'vue';
import { message } from 'ant-design-vue';
import { PlusOutlined, DeleteOutlined, SearchOutlined, CloseOutlined } from '@ant-design/icons-vue';
import { updateRange, queryOrgTree, queryRoles, queryUsers, queryOrgUsers } from '@/api/common';
import { addUsersToRole } from '@/api/role';
import { throttle } from 'throttle-debounce';

export default defineComponent({
  name: 'UserAndGroupSelect',
  components: {
    PlusOutlined,
    DeleteOutlined,
    SearchOutlined,
    CloseOutlined
  },
  props: {
    bizId: {
      type: Number,
      required: false,
      default: null
    },
    bizType: {
      type: String,
      default: 'course'
    },
    title: {
      type: String,
      default: '可见范围设置'
    },
    // 预先选择的项目，用于回显
    preSelectedItems: {
      type: Array,
      default: () => []
    },
    // 是否为独立模式（不需要保存到后端，只需要返回选择结果）
    standalone: {
      type: Boolean,
      default: false
    }
  },
  emits: ['success', 'cancel', 'change'],

  setup(props, { emit }) {
    const loading = ref(false);
    const activeKey = ref('org');
    const searchKeyword = ref('');

    // Navigation path
    const currentPath = ref([{ name: '根目录', key: 'root' }]);
    const currentItems = ref([]);

    // Store all items for search
    const allItems = ref({
      org: [],
      role: [],
      position: [],
      user: []
    });

    // Store enriched department tree (with users)
    const enrichedOrgTree = ref([]);

    // Cache for department users to avoid redundant API calls
    const departmentUsersCache = ref({});

    // Selected items
    const selectedTargets = ref([]);

    // Current navigation stack
    const navigationStack = ref([]);

    // 初始化预选项
    const initPreSelectedItems = () => {
      console.log('initPreSelectedItems called, preSelectedItems:', props.preSelectedItems);
      if (props.preSelectedItems && props.preSelectedItems.length > 0) {
        // 处理回显数据，确保avatar属性正确
        selectedTargets.value = props.preSelectedItems.map(item => {
          // 创建新对象，避免修改原始数据
          const newItem = { ...item };
          // 如果是用户类型且有头像信息，确保avatar属性存在
          if (item.type === 'user' && (item.avatar)) {
            newItem.avatar = item.avatar; // 兼容可能使用headImg的情况
          }
          return newItem;
        });
        console.log('selectedTargets initialized:', selectedTargets.value);
      }
    };

    // Check if an item has children
    const hasChildren = (item) => {
      // 部门可能有子部门或用户
      if (item.type === 'department') {
        return item.hasChildren;
      };
      return false;
    };

    // Check if an item is selected
    const isItemSelected = (item) => {
      return selectedTargets.value.some(target =>
        target.id === item.id && target.type === getItemType(item)
      );
    };

    // Navigate to children items
    const navigateToChildren = async (item) => {
      if (item.type === 'department') {
        // For departments, need to load both sub-departments and users
        try {
          loading.value = true;

          // Validate department ID
          if (!item.id) {
            console.error('部门ID不能为空');
            message.error('部门数据异常，无法加载子项');
            loading.value = false;
            return;
          }

          console.log('正在加载部门:', item.title, '(ID:', item.id, ')');

          // Remove parent from selected items if it exists
          removeTarget(item);

          // First, update navigation path
          currentPath.value.push({
            name: item.title,
            key: item.key,
            id: item.id
          });

          // Save current state to navigation stack
          navigationStack.value.push([...currentItems.value]);

          // 使用新的loadOrgTree方法加载子部门和用户
          const { departments, users } = await loadOrgTree(item.id);

          console.log('加载结果 - 部门:', departments.length, '用户:', users.length);

          // 缓存该部门下的用户
          if (users && users.length > 0) {
            cacheDepartmentUsers(item.id, users);
            item.hasUsers = true;
          }

          // 合并子部门和用户
          let childItems = [...departments];
          if (users && users.length > 0) {
            childItems = [...childItems, ...users];
            console.log('合并后的子项数量:', childItems.length);
          }

          // 更新当前显示的项目
          currentItems.value = childItems;

          // 保存完整的子项列表（包括部门和用户）
          item.allChildren = childItems;

        } catch (error) {
          console.error('加载部门下级和用户失败:', error);
        } finally {
          loading.value = false;
        }
      } else if (hasChildren(item)) {
        // For other item types with children
        // Remove parent from selected items if it exists
        removeTarget(item);

        // Update navigation path
        currentPath.value.push({
          name: item.title,
          key: item.key
        });

        // Save current state to navigation stack
        navigationStack.value.push([...currentItems.value]);

        // Update current items to show children
        currentItems.value = item.children;
      }
    };

    // Navigate using breadcrumb
    const navigateToBreadcrumb = async (index) => {
      // 如果点击的是当前路径的最后一项，不做任何操作
      if (index >= currentPath.value.length - 1) return;

      // 获取点击的面包屑项目
      const clickedItem = currentPath.value[index];

      console.log('点击面包屑导航:', clickedItem.name, '(索引:', index, ')');

      // 如果点击的是根目录
      if (index === 0) {
        // 加载初始项目
        loadInitialItems();
        return;
      }

      try {
        loading.value = true;

        // 保留当前路径到点击的项
        currentPath.value = currentPath.value.slice(0, index + 1);

        // 保存当前状态到导航堆栈
        navigationStack.value = navigationStack.value.slice(0, index);

        console.log('加载部门:', clickedItem.name, '(ID:', clickedItem.id, ')');

        // 使用新的loadOrgTree方法加载子部门和用户
        const { departments, users } = await loadOrgTree(clickedItem.id);

        console.log('面包屑导航加载结果 - 部门:', departments.length, '用户:', users.length);

        // 缓存该部门下的用户
        if (users && users.length > 0) {
          cacheDepartmentUsers(clickedItem.id, users);
          // 标记该部门有用户
          clickedItem.hasUsers = true;
        }

        // 合并子部门和用户
        let childItems = [...departments];
        if (users && users.length > 0) {
          childItems = [...childItems, ...users];
          console.log('面包屑导航 - 合并后的子项数量:', childItems.length);
        }

        // 更新当前显示的项目
        currentItems.value = childItems;

        // 检查每个子部门是否有子部门
        departments.forEach(dept => {
          // 检查该部门是否有子部门
          const hasSubDepts = dept.hasChildren
          if (hasSubDepts) {
            dept.hasChildren = true;
            dept.hasUsers = true; // 如果有子部门，先假设有用户，点击后再验证
          } else {
            dept.hasChildren = false;
            // 不再默认设置hasUsers为true，需要等到实际加载该部门时再确认
            // 这样可以确保只有真正有子部门或用户的部门才显示"下级"按钮
            dept.hasUsers = false;
          }
        });

      } catch (error) {
        console.error('加载部门下级和用户失败:', error);

        // 如果加载失败，回退到原来的行为
        if (index === 0) {
          // 根级 - 从allItems恢复
          loadInitialItems();
        } else {
          currentItems.value = navigationStack.value[index - 1];
          navigationStack.value = navigationStack.value.slice(0, index);
        }
      } finally {
        loading.value = false;
      }
    };

    // Toggle item selection
    const toggleSelectItem = (item) => {
      const index = selectedTargets.value.findIndex(target =>
        target.id === item.id && target.type === getItemType(item)
      );

      if (index === -1) {
        // Add item to selected targets
        selectedTargets.value.push({
          id: item.id,
          name: item.title,
          type: getItemType(item),
          key: item.key,
          avatar: item.avatar// 添加头像信息，同时考虑avatar和headImg
        });
      } else {
        // Remove item from selected targets
        selectedTargets.value.splice(index, 1);
      }

      emit('change', selectedTargets.value)
    };

    // Get item type based on activeKey and item properties
    const getItemType = (item) => {
      if (item.type) return item.type;
      switch (activeKey.value) {
        case 'org': return 'department';
        case 'role': return 'role';
        case 'position': return 'position';
        case 'user': return 'user';
        default: return 'unknown';
      }
    };
    // Load initial items based on active key
    const loadInitialItems = async () => {
      loading.value = true;
      try {
        console.log('加载初始项目, 当前标签:', activeKey.value);

        let items = [];
        switch (activeKey.value) {
          case 'org':
            // 加载根级部门和用户
            const { departments, users } = await loadOrgTree();

            console.log('初始加载 - 部门:', departments.length, '用户:', users.length);

            // 将根级用户缓存起来
            if (users && users.length > 0) {
              departmentUsersCache.value['root'] = users;
            }

            // 合并部门和用户
            items = [...departments];
            if (users && users.length > 0) {
              items = [...items, ...users];
              console.log('初始加载 - 合并后的项目数量:', items.length);
            }

            // 检查每个部门是否有子部门
            departments.forEach(dept => {
              // 检查该部门是否有子部门
              const hasSubDepts = dept.hasChildren;
              if (hasSubDepts) {
                dept.hasChildren = true;
                dept.hasUsers = true; // 如果有子部门，先假设有用户，点击后再验证
              } else {
                dept.hasChildren = false;
                // 不再默认设置hasUsers为true，需要等到实际加载该部门时再确认
                // 这样可以确保只有真正有子部门或用户的部门才显示"下级"按钮
                dept.hasUsers = false;
              }
            });

            break;
          case 'role':
            items = await loadRoles();
            console.log('加载角色:', items.length);
            break;
          case 'position':
            items = await loadPositions();
            console.log('加载职位:', items.length);
            break;
          case 'user':
            items = await loadUsers();
            console.log('加载用户:', items.length);
            break;
        }

        allItems.value[activeKey.value] = items;
        currentItems.value = items;

        // Reset navigation
        currentPath.value = [{ name: '根目录', key: 'root' }];
        navigationStack.value = [];

      } catch (error) {
        console.error('加载数据失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 加载组织结构树（按需加载）
    const loadOrgTree = async (departmentId = null) => {
      try {
        // 构建请求参数
        const params = {
          includeUsers: true
        };

        // 如果指定了部门ID，则加载该部门下的子部门和用户
        if (departmentId !== null) {
          params.id = departmentId;
        }

        // 请求后端API
        const res = await queryOrgTree(params);

        if (res.code !== 200) {
          console.error('加载组织树失败:', res.message);
          message.error('加载组织架构失败');
          return { departments: [], users: [] };
        }

        console.log('API返回数据:', res);

        // 获取部门列表
        const departments = formatDepartmentData(res.data.children || []);
        console.log("$$$$$", departments, res.data.children)
        // 获取用户列表
        let users = [];

        // 从res.item.users获取用户列表
        if (res.data && res.data.users) {
          users = res.data.users.map(user => ({
            key: `user-${user.userId || user.id}`,
            id: user.userId || user.id,
            title: user.nickname || user.username,
            type: 'user',
            avatar: user.avatar
          }));
          console.log('从res.data.users获取到用户:', users.length);
        }

        // 如果没有从item中获取到用户，尝试从res.users获取
        if (users.length === 0 && res.users) {
          users = res.users.map(user => ({
            key: `user-${user.userId || user.id}`,
            id: user.userId || user.id,
            title: user.nickname || user.username,
            type: 'user',
            avatar: user.avatar
          }));
          console.log('从res.users获取到用户:', users.length);
        }

        // 如果是根级查询，将部门添加到enrichedOrgTree
        if (departmentId === null) {
          enrichedOrgTree.value = departments;
        }

        // 检查每个部门是否有子部门，如果没有则设置hasChildren为false
        departments.forEach(dept => {
          // 检查该部门是否有子部门
          // dept.hasChildren = dept.hasChildren
          // 默认设置hasUsers为false，除非后续确认有用户
          dept.hasUsers = false;

          // 如果有子部门，先假设有用户，点击后再验证
          if (dept.hasChildren) {
            dept.hasUsers = true;
          }
        });

        // 标记部门是否有用户
        if (users && users.length > 0) {
          // 如果有用户，则标记当前查询的部门有用户
          if (departmentId !== null) {
            // 对于子部门查询，标记父部门有用户
            const parentDept = currentItems.value.find(item =>
              item.type === 'department' && item.id === departmentId
            );
            if (parentDept) {
              parentDept.hasUsers = true;
              console.log('标记部门有用户:', parentDept.title);
            }
          }
        }

        console.log(`加载部门 ${departmentId || 'root'} 的数据:`, {
          departments: departments.length,
          users: users.length
        });

        return { departments, users };
      } catch (error) {
        console.error('加载组织树失败:', error);
        return { departments: [], users: [] };
      }
    };

    // 缓存部门用户
    const cacheDepartmentUsers = (departmentId, users) => {
      if (!users || users.length === 0) return;

      // 缓存当前部门的用户
      departmentUsersCache.value[departmentId] = users;
    };

    // Watch for active key changes
    watch(activeKey, () => {
      loadInitialItems();
    });

    // Watch for preSelectedItems changes
    watch(() => props.preSelectedItems, (newVal) => {
      console.log('preSelectedItems changed:', newVal);
      if (newVal && newVal.length > 0) {
        // 处理回显数据，确保avatar属性正确
        selectedTargets.value = newVal.map(item => {
          // 创建新对象，避免修改原始数据
          const newItem = { ...item };
          // 如果是用户类型且有头像信息，确保avatar属性存在
          if (item.type === 'user' && item.avatar) {
            newItem.avatar = item.avatar; // 兼容可能使用headImg的情况
          }
          return newItem;
        });
        console.log('selectedTargets updated from watch:', selectedTargets.value);
      }
    }, { deep: true });

    // Format departments data
    const formatDepartmentData = (departments) => {
      console.log("&&&&&", departments)
      return departments.map(dept => {
        // Log the hasChildren value to debug
        console.log(`Department ${dept.name} (ID: ${dept.id}) hasChildren: ${dept.hasChildren}`);

        // Create the department object with the original hasChildren value
        return {
          key: `dept-${dept.id}`,
          id: dept.id,
          title: dept.name,
          type: 'department',
          hasChildren: dept.hasChildren, // Keep the original value from API
          children: dept.children
        };
      });
    };

    // Load roles
    const loadRoles = async () => {
      const res = await queryRoles();
      return res.code === 200 ? (res.data || []).map(role => ({
        key: `role-${role.id}`,
        id: role.id,
        title: role.name,
        type: 'role'
      })) : [];
    };

    // Load positions (implement according to your API)
    const loadPositions = async () => {
      // Implement position loading logic
      return [];
    };

    // Load users
    const loadUsers = async () => {
      const res = await queryUsers();
      return res.code === 200 ? (res.data || []).map(user => ({
        key: `user-${user.userId || user.id}`,
        id: user.userId || user.id, // Prefer userId, fall back to id if not present
        title: user.nickname || user.username,
        type: 'user',
        avatar: user.avatar
      })) : [];
    };

    // Remove target
    const removeTarget = (item) => {
      const type = item.type || getItemType(item);
      const index = selectedTargets.value.findIndex(
        target => target.id === item.id && target.type === type
      );

      if (index !== -1) {
        selectedTargets.value.splice(index, 1);
      }


      emit('change', selectedTargets.value)
    };

    // Get item class
    const getItemClass = (item) => {
      const type = item.type || getItemType(item);
      switch (type) {
        case 'department': return 'org-avatar';
        case 'role': return 'role-avatar';
        case 'position': return 'position-avatar';
        case 'user': return 'user-avatar';
        default: return '';
      }
    };

    // Get item avatar text
    const getItemAvatarText = (item) => {
      const type = item.type || getItemType(item);
      switch (type) {
        case 'department': return '组';
        case 'role': return '角';
        case 'position': return '职';
        case 'user': return (item.name || item.title || '').charAt(0);
        default: return '';
      }
    };

    // Check if item has avatar
    const hasAvatar = (item) => {
      const type = item.type || getItemType(item);
      // 确保avatar或headImg存在且不为空字符串
      return type === 'user' && (
        (item.avatar && item.avatar !== '')
      );
    };

    // 原始搜索处理函数
    const handleSearchOriginal = async () => {
      // 如果搜索关键词为空，则恢复到当前路径的显示
      if (!searchKeyword.value || searchKeyword.value.trim() === '') {
        // 如果当前在根目录，则加载初始项目
        if (currentPath.value.length === 1) {
          loadInitialItems();
        } else {
          // 否则重新加载当前目录的内容
          const currentPathItem = currentPath.value[currentPath.value.length - 1];
          if (currentPathItem.id) {
            const { departments, users } = await loadOrgTree(currentPathItem.id);
            // 合并部门和用户
            let items = [...departments];
            if (users && users.length > 0) {
              items = [...items, ...users];
            }
            currentItems.value = items;
          } else {
            loadInitialItems();
          }
        }
        return;
      }

      // 根据当前激活的标签页执行不同的搜索逻辑
      if (activeKey.value === 'org') {
        try {
          loading.value = true;

          // 构建请求参数，添加关键词
          const params = {
            keyword: searchKeyword.value.trim(),
            includeUsers: true
          };

          // 如果当前不在根目录，则限制在当前部门下搜索
          if (currentPath.value.length > 1) {
            const currentPathItem = currentPath.value[currentPath.value.length - 1];
            if (currentPathItem.id) {
              params.id = currentPathItem.id;
            }
          }

          console.log('执行搜索，参数:', params);

          // 调用后端API进行搜索
          const res = await queryOrgTree(params);

          if (res.code !== 200) {
            console.error('搜索失败:', res.message);
            message.error('搜索失败');
            return;
          }

          console.log('搜索结果:', res);

          // 处理搜索结果
          let searchResults = [];

          // 获取部门列表
          const departments = formatDepartmentData(res.data.children || []);

          // 获取用户列表
          let users = [];

          // 从res.item.users获取用户列表
          if (res.data && res.data.users) {
            users = res.data.users.map(user => ({
              key: `user-${user.userId || user.id}`,
              id: user.userId || user.id,
              title: user.nickname || user.username,
              type: 'user',
              avatar: user.avatar
            }));
            console.log('搜索结果 - 从res.data.users获取到用户:', users.length);
          }

          // 合并部门和用户到搜索结果
          searchResults = [...departments];
          if (users && users.length > 0) {
            searchResults = [...searchResults, ...users];
          }

          console.log('搜索结果 - 合并后的项目数量:', searchResults.length);

          // 更新当前显示的项目为搜索结果
          currentItems.value = searchResults;

        } catch (error) {
          console.error('搜索失败:', error);
        } finally {
          loading.value = false;
        }
      } else if (activeKey.value === 'role') {
        // 角色搜索逻辑
        // 从allItems中过滤符合条件的角色
        const keyword = searchKeyword.value.trim().toLowerCase();
        currentItems.value = allItems.value.role.filter(item =>
          item.title.toLowerCase().includes(keyword)
        );
      } else if (activeKey.value === 'user') {
        // 用户搜索逻辑
        // 从allItems中过滤符合条件的用户
        const keyword = searchKeyword.value.trim().toLowerCase();
        currentItems.value = allItems.value.user.filter(item =>
          item.title.toLowerCase().includes(keyword)
        );
      }
    };

    // 使用节流函数包装原始搜索函数，节流时间为1秒
    const handleSearch = throttle(1000, handleSearchOriginal);

    // Handle confirm
    const handleConfirm = async () => {
      if (selectedTargets.value.length === 0) {
        message.warning('请至少选择一个可见范围');
        return;
      }

      // 如果是独立模式或没有业务ID，则只返回选择结果
      if (props.standalone || !props.bizId) {
        const result = {
          departments: selectedTargets.value
            .filter(item => item.type === 'department')
            .map(item => ({ id: item.id, name: item.name })),
          roles: selectedTargets.value
            .filter(item => item.type === 'role')
            .map(item => ({ id: item.id, name: item.name })),
          users: selectedTargets.value
            .filter(item => item.type === 'user')
            .map(item => ({ id: item.id, name: item.name })),
          items: selectedTargets.value
        };
        emit('success', result);
        return;
      }

      loading.value = true;
      try {
        // 如果是角色管理，需要使用专门的API添加用户到角色
        if (props.bizType === 'role') {
          // 提取所有用户ID
          const userIds = [];
          selectedTargets.value.forEach(target => {
            if (target.type === 'user') {
              userIds.push(target.id);
            }
          });

          if (userIds.length === 0) {
            message.warning('请至少选择一个用户');
            loading.value = false;
            return;
          }

          // 调用添加用户到角色的API
          const res = await addUsersToRole({
            roleId: props.bizId,
            userIds: userIds
          });

          if (res.code === 200) {
            message.success('添加用户到角色成功');
            emit('success');
          } else {
            message.error(res.message || '添加用户到角色失败');
          }
        } else {
          // 其他业务类型使用通用的updateRange API
          const requestData = {
            modelType: 'visibility',
            type: props.bizType,
            typeId: props.bizId,
            targetTypeAndIds: {}
          };

          // Group selected targets by type
          selectedTargets.value.forEach(target => {
            if (!requestData.targetTypeAndIds[target.type]) {
              requestData.targetTypeAndIds[target.type] = [];
            }
            requestData.targetTypeAndIds[target.type].push(target.id);
          });

          const res = await updateRange(requestData);

          if (res.code === 200) {
            message.success('可见范围设置成功');
            emit('success');
          } else {
            message.error(res.message || '可见范围设置失败');
          }
        }
      } catch (error) {
        console.error('操作失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // Handle cancel
    const handleCancel = () => {
      emit('cancel');
    };

    onMounted(() => {
      loadInitialItems();
      initPreSelectedItems();
    });

    return {
      loading,
      activeKey,
      searchKeyword,
      currentPath,
      currentItems,
      selectedTargets,
      hasChildren,
      isItemSelected,
      navigateToChildren,
      navigateToBreadcrumb,
      toggleSelectItem,
      handleSearch,
      removeTarget,
      getItemClass,
      getItemAvatarText,
      hasAvatar,
      handleConfirm,
      handleCancel
    };
  }
});
</script>

<style scoped>
.selection-content {
  height: calc(100vh - 420px);
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.selection-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.tab-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  border-bottom: 2px solid transparent;
}

.tab-item.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
}

.left-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 16px;
}

.right-area {
  width: 280px;
  display: flex;
  flex-direction: column;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 16px;
}

.search-bar {
  margin-bottom: 16px;
}

.breadcrumb-nav {
  margin-bottom: 16px;
}

.selection-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.selection-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.selection-item:hover {
  background-color: #f5f5f5;
}

.selection-item.selected {
  background-color: #e6f7ff;
}

.item-content {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.item-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  margin-right: 8px;
  flex-shrink: 0;
  background-size: cover;
  background-position: center;
}

.item-name {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.next-level-btn {
  padding: 4px 8px;
  color: #1890ff;
}

.selected-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.selected-title {
  font-size: 14px;
  font-weight: 500;
}

.selected-count {
  font-size: 14px;
  color: #999;
}

.selected-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.selected-item {
  display: flex;
  align-items: center;
  background-color: #f5f5f5;
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 8px;
}

.selected-item:last-child {
  margin-bottom: 0;
}

.selected-item-avatar {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  margin-right: 8px;
  flex-shrink: 0;
  background-size: cover;
  background-position: center;
}

.selected-item-name {
  flex: 1;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.selected-item-remove {
  padding: 0 4px;
  color: #999;
}

.org-avatar {
  background-color: #1890ff;
}

.role-avatar {
  background-color: #52c41a;
}

.position-avatar {
  background-color: #fa8c16;
}

.user-avatar {
  background-color: #722ed1;
}

.action-buttons {
  margin-top: 24px;
  text-align: right;
}

.action-buttons .ant-btn + .ant-btn {
  margin-left: 8px;
}
</style>
