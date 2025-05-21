<template>
  <div class="assign-records-container">
    <a-card title="指派记录列表" :bordered="false">
      <a-form layout="inline" :model="queryParams" @finish="handleQuery">
        <a-form-item label="业务类型" name="type">
          <a-select v-model:value="queryParams.type" placeholder="请选择业务类型" style="width: 200px">
            <a-select-option value="courses">课程</a-select-option>
            <a-select-option value="map">学习地图</a-select-option>
            <a-select-option value="train">培训</a-select-option>
            <a-select-option value="exam">考试</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="业务ID" name="typeId">
          <a-input-number v-model:value="queryParams.typeId" placeholder="请输入业务ID" style="width: 200px" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetQuery">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="recordsList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'assignType'">
            <a-tag :color="getAssignTypeColor(record.assignType)">{{ getAssignTypeText(record.assignType) }}</a-tag>
          </template>
          <template v-if="column.key === 'rangeInfo'">
            <div v-if="record.departments && record.departments.length > 0">
              <div>部门：{{ getDepartmentNames(record.departments) }}</div>
            </div>
            <div v-if="record.roles && record.roles.length > 0">
              <div>角色：{{ getRoleNames(record.roles) }}</div>
            </div>
            <div v-if="record.users && record.users.length > 0">
              <div>人员：{{ getUserNames(record.users) }}</div>
            </div>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" @click="viewDetail(record)">查看明细</a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { queryAssignRecords } from '@/api/assignment';

export default defineComponent({
  name: 'AssignRecordsListPage',
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const recordsList = ref([]);
    const queryParams = reactive({
      type: undefined,
      typeId: undefined,
      pageNum: 1,
      pageSize: 10
    });
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条记录`
    });

    const columns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80
      },
      {
        title: '业务类型',
        dataIndex: 'type',
        key: 'type',
        width: 100
      },
      {
        title: '业务ID',
        dataIndex: 'typeId',
        key: 'typeId',
        width: 100
      },
      {
        title: '指派类型',
        dataIndex: 'assignType',
        key: 'assignType',
        width: 120
      },
      {
        title: '通知状态',
        dataIndex: 'status',
        key: 'status',
        width: 120
      },
      {
        title: '指派范围',
        key: 'rangeInfo',
        width: 300
      },
      {
        title: '创建人',
        dataIndex: 'creatorName',
        key: 'creatorName',
        width: 120
      },
      {
        title: '创建时间',
        dataIndex: 'gmtCreate',
        key: 'gmtCreate',
        width: 180
      },
      {
        title: '截止时间',
        dataIndex: 'deadline',
        key: 'deadline',
        width: 180
      },
      {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 120
      }
    ];

    // 获取指派记录列表
    const fetchRecordsList = async () => {
      if (!queryParams.type || !queryParams.typeId) {
        message.warning('请选择业务类型并输入业务ID');
        return;
      }

      loading.value = true;
      try {
        const response = await queryAssignRecords(queryParams);
        if (response.success) {
          recordsList.value = response.data || [];
          pagination.total = response.total || 0;
        } else {
          message.error(response.message || '查询指派记录失败');
        }
      } catch (error) {
        console.error('查询指派记录出错:', error);
      } finally {
        loading.value = false;
      }
    };

    // 查询按钮点击事件
    const handleQuery = () => {
      queryParams.pageNum = 1;
      pagination.current = 1;
      fetchRecordsList();
    };

    // 重置查询条件
    const resetQuery = () => {
      queryParams.type = undefined;
      queryParams.typeId = undefined;
      queryParams.pageNum = 1;
      pagination.current = 1;
    };

    // 表格分页、排序、筛选变化时触发
    const handleTableChange = (pag) => {
      pagination.current = pag.current;
      queryParams.pageNum = pag.current;
      queryParams.pageSize = pag.pageSize;
      fetchRecordsList();
    };

    // 查看明细
    const viewDetail = (record) => {
      router.push({
        path: '/assignment/detail',
        query: { id: record.id }
      });
    };

    // 获取状态颜色
    const getStatusColor = (status) => {
      const statusMap = {
        'wait': 'orange',
        'process': 'blue',
        'success': 'green'
      };
      return statusMap[status] || 'default';
    };

    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'wait': '待通知',
        'process': '通知中',
        'success': '已通知'
      };
      return statusMap[status] || status;
    };

    // 获取指派类型颜色
    const getAssignTypeColor = (assignType) => {
      const typeMap = {
        'once': 'purple',
        'auto': 'cyan'
      };
      return typeMap[assignType] || 'default';
    };

    // 获取指派类型文本
    const getAssignTypeText = (assignType) => {
      const typeMap = {
        'once': '单次通知',
        'auto': '自动通知'
      };
      return typeMap[assignType] || assignType;
    };

    // 获取部门名称列表
    const getDepartmentNames = (departments) => {
      return departments.map(item => item.departmentName).join(', ');
    };

    // 获取角色名称列表
    const getRoleNames = (roles) => {
      return roles.map(item => item.roleName).join(', ');
    };

    // 获取用户名称列表
    const getUserNames = (users) => {
      return users.map(item => item.userName).join(', ');
    };

    onMounted(() => {
      // 如果URL中有查询参数，则自动查询
      const query = router.currentRoute.value.query;
      if (query.type && query.typeId) {
        queryParams.type = query.type;
        queryParams.typeId = parseInt(query.typeId);
        fetchRecordsList();
      }
    });

    return {
      loading,
      recordsList,
      queryParams,
      pagination,
      columns,
      handleQuery,
      resetQuery,
      handleTableChange,
      viewDetail,
      getStatusColor,
      getStatusText,
      getAssignTypeColor,
      getAssignTypeText,
      getDepartmentNames,
      getRoleNames,
      getUserNames
    };
  }
});
</script>

<style scoped>
.assign-records-container {
  padding: 24px;
}
</style>
