<template>
  <a-modal v-model:visible="visible" title="指派记录" width="800px" :footer="null" destroyOnClose>
    <div v-if="visible" class="assign-record-container">
      <a-table :columns="assignRecordColumns" :data-source="assignRecordList" :pagination="assignRecordPagination"
        :loading="loading" @change="handleTableChange" :scroll="{ x: '100%' }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'targetType'">
            <a-tag :color="getTargetTypeColor(record.targetType)">
              {{ getTargetTypeText(record.targetType) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'assignRange'">
            <div>
              指派人员{{ record.users?.length || 0 }}个，<br/>
              指派部门{{ record.departments?.length || 0 }}个，<br/>
              指派角色{{ record.roles?.length || 0 }}个
            </div>
          </template>
          <template v-if="column.key === 'detail'">
            <a @click="showDetailModal(record)">查看详情</a>
          </template>
        </template>
      </a-table>

      <!-- 详情弹窗 -->
      <a-modal v-model:visible="detailModalVisible" title="指派详情" width="800px" :footer="null" destroyOnClose>
        <div v-if="detailModalVisible" class="detail-container">
          <a-spin :spinning="detailLoading">
            <a-table :columns="detailColumns" :data-source="detailList" :pagination="false" />
          </a-spin>
        </div>
      </a-modal>
    </div>
  </a-modal>
</template>

<script>
import { ref, reactive, defineComponent, watch, computed } from 'vue';
import { message } from 'ant-design-vue';
import { queryAssignRecords } from '@/api/operation';
import { queryAssignRecordDetails } from '@/api/assignment';

export default defineComponent({
  name: 'AssignRecordsModalComponent',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    type: {
      type: String,
      required: true,
    },
    typeId: {
      type: [String, Number],
      required: true,
    }
  },
  emits: ['update:visible'],
  setup(props, { emit }) {
    // 指派记录列表数据
    const assignRecordList = ref([]);
    const loading = ref(false);

    // 分页配置
    const assignRecordPagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条记录`
    });

    // 表格列定义
    const assignRecordColumns = [
      {
        title: '操作人',
        dataIndex: 'creatorName',
        key: 'creatorName',
        width: 100
      },
      {
        title: '操作范围',
        dataIndex: 'assignRange',
        key: 'assignRange',
        width: 100
      },
      {
        title: '完成时间',
        dataIndex: 'deadline',
        key: 'deadline',
        width: 160
      },
      {
        title: '提醒',
        dataIndex: 'assignerName',
        key: 'assignerName',
        width: 100
      },
      {
        title: '指派对象类型',
        dataIndex: 'targetType',
        key: 'targetType',
        width: 100
      },
      {
        title: '指派对象',
        dataIndex: 'targetName',
        key: 'targetName',
        width: 100
      },
      {
        title: '操作时间',
        dataIndex: 'gmtCreate',
        key: 'gmtCreate',
        width: 160
      },
      {
        title: '详情',
        dataIndex: 'detail',
        fixed: 'right',
        key: 'detail',
        width: 100
      }
    ];

    // 详情弹窗相关
    const detailModalVisible = ref(false);
    const detailLoading = ref(false);
    const detailList = ref([]);
    const currentRecord = ref(null);

    // 详情表格列定义
    const detailColumns = [
      {
        title: '用户ID',
        dataIndex: 'userId',
        key: 'userId'
      },
      {
        title: '用户名称',
        dataIndex: 'userName',
        key: 'userName'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        customRender: ({ text }) => {
          return text === '1' ? '成功' : '失败';
        }
      },
      {
        title: '失败原因',
        dataIndex: 'failReason',
        key: 'failReason'
      },
      {
        title: '创建时间',
        dataIndex: 'gmtCreate',
        key: 'gmtCreate'
      }
    ];

    // 获取指派记录数据
    const fetchAssignRecords = async () => {
      if (!props.typeId) return;

      loading.value = true;
      try {
        const res = await queryAssignRecords({
          type: props.type,
          typeId: props.typeId,
          pageNum: assignRecordPagination.current,
          pageSize: assignRecordPagination.pageSize
        });

        if (res.code === 200 && res.data) {
          assignRecordList.value = res.data || [];
          assignRecordPagination.total = res.total || 0;
        } else {
          message.error(res.message || '获取指派记录失败');
        }
      } catch (error) {
        console.error('获取指派记录失败:', error);
        assignRecordList.value = [];
      } finally {
        loading.value = false;
      }
    };

    // 处理表格变更
    const handleTableChange = (pag) => {
      assignRecordPagination.current = pag.current;
      assignRecordPagination.pageSize = pag.pageSize;
      fetchAssignRecords();
    };

    // 获取指派对象类型文本
    const getTargetTypeText = (type) => {
      const typeMap = {
        'user': '用户',
        'department': '部门',
        'role': '角色'
      };
      return typeMap[type] || type;
    };

    // 获取指派对象类型颜色
    const getTargetTypeColor = (type) => {
      const colorMap = {
        'user': 'blue',
        'department': 'green',
        'role': 'purple'
      };
      return colorMap[type] || 'default';
    };

    // 显示详情弹窗
    const showDetailModal = (record) => {
      currentRecord.value = record;
      detailModalVisible.value = true;
      fetchAssignRecordDetails(record.id);
    };

    // 获取指派记录详情
    const fetchAssignRecordDetails = async (assignRecordId) => {
      if (!assignRecordId) return;

      detailLoading.value = true;
      try {
        const res = await queryAssignRecordDetails({
          assignRecordId: assignRecordId
        });

        if (res.code === 200) {
          detailList.value = res.data || [];
        } else {
          message.error(res.message || '获取指派记录详情失败');
        }
      } catch (error) {
        console.error('获取指派记录详情失败:', error);
        detailList.value = [];
      } finally {
        detailLoading.value = false;
      }
    };

    // 监听visible变化
    watch(() => props.visible, (newVal) => {
      if (newVal) {
        fetchAssignRecords();
      }
    });

    // 监听typeId变化
    watch(() => props.typeId, (newVal) => {
      if (props.visible && newVal) {
        fetchAssignRecords();
      }
    });

    return {
      assignRecordList,
      loading,
      assignRecordPagination,
      assignRecordColumns,
      detailModalVisible,
      detailLoading,
      detailList,
      detailColumns,
      currentRecord,
      handleTableChange,
      getTargetTypeText,
      getTargetTypeColor,
      showDetailModal,
      visible: computed({
        get: () => props.visible,
        set: (val) => emit('update:visible', val)
      })
    };
  }
});
</script>

<style scoped>
.assign-record-container,
.detail-container {
  padding: 16px;
}
</style>