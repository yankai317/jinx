<template>
  <div class="assign-record-detail-container">
    <a-card title="指派记录明细" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="goBack">返回</a-button>
      </template>

      <a-descriptions title="指派记录信息" bordered>
        <a-descriptions-item label="指派记录ID">{{ recordId }}</a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <a-table
        :columns="columns"
        :data-source="detailsList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { queryAssignRecordDetails } from '@/api/assignment';

export default defineComponent({
  name: 'AssignRecordDetailPage',
  setup() {
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const detailsList = ref([]);
    const recordId = ref(null);
    const queryParams = reactive({
      assignRecordId: null,
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
        title: '用户ID',
        dataIndex: 'userId',
        key: 'userId',
        width: 100
      },
      {
        title: '用户名称',
        dataIndex: 'userName',
        key: 'userName',
        width: 150
      },
      {
        title: '通知状态',
        dataIndex: 'status',
        key: 'status',
        width: 120
      },
      {
        title: '失败原因',
        dataIndex: 'failReason',
        key: 'failReason',
        width: 200
      },
      {
        title: '创建时间',
        dataIndex: 'gmtCreate',
        key: 'gmtCreate',
        width: 180
      },
      {
        title: '更新时间',
        dataIndex: 'gmtModified',
        key: 'gmtModified',
        width: 180
      }
    ];

    // 获取指派记录明细列表
    const fetchDetailsList = async () => {
      if (!queryParams.assignRecordId) {
        message.warning('指派记录ID不能为空');
        return;
      }

      loading.value = true;
      try {
        const response = await queryAssignRecordDetails(queryParams);
        if (response.success) {
          detailsList.value = response.data || [];
          pagination.total = response.total || 0;
        } else {
          message.error(response.message || '查询指派记录明细失败');
        }
      } catch (error) {
        console.error('查询指派记录明细出错:', error);
      } finally {
        loading.value = false;
      }
    };

    // 表格分页、排序、筛选变化时触发
    const handleTableChange = (pag) => {
      pagination.current = pag.current;
      queryParams.pageNum = pag.current;
      queryParams.pageSize = pag.pageSize;
      fetchDetailsList();
    };

    // 返回上一页
    const goBack = () => {
      router.back();
    };

    // 获取状态颜色
    const getStatusColor = (status) => {
      const statusMap = {
        '0': 'orange',
        '1': 'green',
        '2': 'red'
      };
      return statusMap[status] || 'default';
    };

    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        '0': '待通知',
        '1': '通知成功',
        '2': '通知失败'
      };
      return statusMap[status] || status;
    };

    onMounted(() => {
      // 从路由参数中获取指派记录ID
      const id = route.query.id;
      if (id) {
        recordId.value = id;
        queryParams.assignRecordId = parseInt(id);
        fetchDetailsList();
      } else {
        message.error('指派记录ID不能为空');
      }
    });

    return {
      loading,
      detailsList,
      recordId,
      pagination,
      columns,
      handleTableChange,
      goBack,
      getStatusColor,
      getStatusText
    };
  }
});
</script>

<style scoped>
.assign-record-detail-container {
  padding: 24px;
}
</style>
