<template>
  <a-select v-model:value="selectedValue" :options="userList" :filter-option="false" :placeholder="placeholder"
    :allow-clear="allowClear" :show-search="true" :multiple="multiple"
    :dropdown-style="{ maxHeight: '400px', overflow: 'auto' }" style="width: 100%" @search="handleSearch"
    @change="handleChange" v-bind="$attrs" />
</template>

<script>
import { ref, watch, defineComponent } from 'vue';
import { getUserList } from '@/api/user';
import { debounce } from 'throttle-debounce';

export default defineComponent({
  name: 'UserList',
  props: {
    value: {
      type: [String, Number, Array],
      default: undefined
    },
    placeholder: {
      type: String,
      default: '请选择创建人'
    },
    allowClear: {
      type: Boolean,
      default: true
    },
    multiple: {
      type: Boolean,
      default: false
    },
    treeCheckable: {
      type: Boolean,
      default: false
    },
    showCheckedStrategy: {
      type: String,
      default: 'SHOW_ALL'
    },
    treeDefaultExpandAll: {
      type: Boolean,
      default: false
    },
    treeNodeFilterProp: {
      type: String,
      default: 'title'
    },
    // 是否只查询管理员
    queryAdmin: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:value', 'change'],
  setup(props, { emit }) {
    const selectedValue = ref(props.value);
    const userList = ref([]);

    /** 多选回填触发初始数据加载；不建议拉取太多数据 */
    const getFallBackData = ({ userIds }) => {
      return new Promise(async(resolve, reject) => {
        try {
          const res = await getUserList({
            pageNum: 1,
            pageSize: 10,
            userIds,
            containDeleted: true
          });
          if (res.code === 200 && res.data) {
            const fallbackUsers = res.data?.map(user => ({
              label: `${user.nickname} (${user.employeeNo || '无工号'})`,
              value: user.userId
            })) || [];

            // 防止重复添加相同的用户选项
            const existingUserIds = userList.value.map(user => user.value);
            const uniqueFallbackUsers = fallbackUsers.filter(user => !existingUserIds.includes(user.value));

            userList.value = [...userList.value, ...uniqueFallbackUsers];
            resolve(fallbackUsers);
          } else {
            reject(new Error('Failed to get user data'));
          }
        } catch (error) {
          console.error('获取用户回填数据失败:', error);
          reject(error);
        }
      });
    };

    // 获取用户列表
    const fetchUserList = debounce(200, async (keyword) => {
      try {
        const res = await getUserList({
          keyword: keyword,
          pageNum: 1,
          pageSize: 10,
          queryAdmin: props.queryAdmin
        });
        if (res.code === 200 && res.data) {
          console.log('获取用户列表:', res.data);
          userList.value = res.data?.map(user => ({
            label: `${user.nickname} (${user.employeeNo || '无工号'})`,
            value: user.userId
          })) || [];
        }
      } catch (error) {
        console.error('获取用户列表失败:', error);
      }
    });

    // 处理搜索
    const handleSearch = (value) => {
      console.log('搜索:', value);
      fetchUserList(value);
    };

    // 处理选择变更
    const handleChange = (value) => {
      emit('update:value', value);
      emit('change', value);
    };

    // 监听外部value变化
    watch(() => props.value, (newVal) => {
      selectedValue.value = newVal;

      // 当有初始值时，获取对应用户数据进行回填
      if (newVal && ((Array.isArray(newVal) && newVal.length > 0) || (!Array.isArray(newVal) && newVal))) {
        const userIds = Array.isArray(newVal) ? newVal : [newVal];
        getFallBackData({ userIds }).catch(err => {
          console.error('回填用户数据失败:', err);
        });
      }
    }, { immediate: true });

    // 初始化时获取分类数据
    fetchUserList();

    return {
      selectedValue,
      userList,
      handleChange,
      handleSearch
    };
  }
});
</script>

<style scoped>
/* 可以添加自定义样式 */
</style>