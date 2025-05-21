<template>
  <div class="train-tracking-page">
    <a-layout>
      <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

      <a-layout-content class="content">
        <a-spin :spinning="loading">
          <!-- 培训统计信息 -->
          <a-row :gutter="16">
            <a-col :span="6">
              <a-card class="statistic-card">
                <a-statistic
                  title="学习人数"
                  :value="statistics.learnerCount || 0"
                  :valueStyle="{ color: '#1890ff' }"
                >
                  <template #prefix>
                    <UserOutlined />
                  </template>
                </a-statistic>
              </a-card>
            </a-col>
            <a-col :span="6">
              <a-card class="statistic-card">
                <a-statistic
                  title="完成人数"
                  :value="statistics.completionCount || 0" :valueStyle="{ color: '#52c41a' }"
                >
                  <template #prefix>
                    <CheckCircleOutlined />
                  </template>
                </a-statistic>
              </a-card>
            </a-col>
            <a-col :span="6">
              <a-card class="statistic-card">
                <a-statistic
                  title="进行中人数"
                  :value="(statistics.learnerCount || 0) - (statistics.completionCount || 0)"
                  :valueStyle="{ color: '#faad14' }"
                >
                  <template #prefix>
                    <ClockCircleOutlined />
                  </template>
                </a-statistic>
              </a-card>
            </a-col>
            <a-col :span="6">
              <a-card class="statistic-card">
                <a-statistic
                  title="完成率"
                  :value="(statistics.completionRate || 0) * 100"
                  :precision="2"
                  suffix="%"
                  :valueStyle="{ color: '#722ed1' }"
                >
                  <template #prefix><PieChartOutlined />
                  </template>
                </a-statistic>
              </a-card>
            </a-col>
          </a-row>

          <!-- 学习人员列表 -->
          <a-card title="学习人员列表" class="learners-card">
            <template #extra>
              <a-space>
                <UserSelect v-model:value="userId" :show-search="true" placeholder="请选择创建人" allowClear @change="handleSearch" style="width: 200px" />
                <a-select
                  v-model:value="statusFilter"
                  style="width: 120px"
                  placeholder="学习状态"
                  @change="handleStatusFilterChange"
                >
                  <a-select-option value="">全部状态</a-select-option><a-select-option value="learning">学习中</a-select-option>
                  <a-select-option value="completed">已完成</a-select-option><a-select-option value="not_started">未开始</a-select-option></a-select>
              </a-space>
            </template>

            <a-table
              :columns="columns"
              :data-source="learners.list"
              :pagination="{
                current: pagination.current,
                pageSize: pagination.pageSize,
                total: learners.total,
                showSizeChanger: true,
                showQuickJumper: true,
                showTotal: total => `共 ${total} 条记录`,
                onChange: handlePageChange,onShowSizeChange: handlePageSizeChange
              }"
              :loading="tableLoading"
              rowKey="id">
              <!-- 用户名列 -->
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'userName'">
                  <a @click="showUserDetail(record)">{{ record.userName }}</a>
                </template>
                <!-- 状态列 -->
                <template v-else-if="column.dataIndex === 'status'">
                  <a-tag :color="getStatusColor(record.status)">
                    {{ getStudyStatusText(record.status) }}</a-tag>
                </template>

                <!-- 进度列 -->
                <template v-else-if="column.dataIndex === 'progress'"><a-progress
                    :percent="record.progress"
                    size="small"
                    :status="getProgressStatus(record.status)"
                  />
                </template>

                <!-- 操作列 -->
                <template v-else-if="column.dataIndex === 'action'">
                  <a-space wrap>
                    <a @click="showUserDetail(record)">详情</a><a-divider type="vertical" />
                    <a-dropdown>
                      <a class="ant-dropdown-link" @click.prevent>
                        更多 <DownOutlined /></a>
                      <template #overlay>
                        <a-menu>
                          <a-menu-item key="remind" @click="sendReminder(record)">
                            发送提醒
                          </a-menu-item>
                          <a-menu-item key="reset" @click="resetProgress(record)">
                            重置进度
                          </a-menu-item>
                        </a-menu>
                      </template>
                    </a-dropdown>
                  </a-space>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-spin>
      </a-layout-content>
    </a-layout>

    <!-- 指派培训弹窗 -->
    <AssignPage
      :visible="assignModalVisible"
      :bizId="trainId"
      bizType="TRAIN"
      :onClose="handleAssignModalClose"
      :onSuccess="handleAssignSuccess"
    />

    <!-- 用户学习详情弹窗 -->
    <a-modal
      v-model:visible="userDetailVisible"
      title="学习详情"
      width="800px"
      :footer="null"
    >
      <a-spin :spinning="userDetailLoading">
        <template v-if="userDetail">
          <a-descriptions title="用户信息" bordered>
            <a-descriptions-item label="用户名">{{ userDetail.userName }}</a-descriptions-item>
            <a-descriptions-item label="部门">{{ userDetail.department || '未知' }}</a-descriptions-item>
            <a-descriptions-item label="学习状态">
              <a-tag :color="getStatusColor(userDetail.status)">
                {{ getStudyStatusText(userDetail.status) }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="指派时间">{{ formatDate(userDetail.assignTime) }}</a-descriptions-item>
            <a-descriptions-item label="截止时间">
              {{ userDetail.deadline ? formatDate(userDetail.deadline) : '无截止时间' }}
            </a-descriptions-item>
            <a-descriptions-item label="完成时间">
              {{ userDetail.completionTime ? formatDate(userDetail.completionTime) : '未完成' }}
            </a-descriptions-item>
            <a-descriptions-item label="学习进度"><a-progress
                :percent="calculateUserDetailProgress(userDetail.progress)"
                size="small"
                :status="getProgressStatus(userDetail.status)"
              />
            </a-descriptions-item>
            <a-descriptions-item label="学习时长">
              {{ formatDuration(userDetail.studyDuration) }}
            </a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <a-table
            :title="'学习内容记录'"
            :columns="contentColumns"
            :data-source="userDetail.records"
            :pagination="false"
            rowKey="id"
          >
            <template #bodyCell="{ column, record }">
              <!-- 内容状态列 -->
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="getContentStatusColor(record.status)">
                  {{ getContentStatusText(record.status) }}
                </a-tag>
              </template>

              <!-- 学习时长列 -->
              <template v-else-if="column.dataIndex === 'studyDuration'">
                {{ formatDuration(record.studyDuration) }}
              </template>

              <!-- 得分列 -->
              <template v-else-if="column.dataIndex === 'score'">
                <span v-if="record.contentType === 'EXAM' || record.contentType === 'ASSIGNMENT'">
                  {{ record.score }}
                </span>
                <span v-else>-</span>
              </template>
            </template>
          </a-table>
        </template>
        <template v-else>
          <a-empty description="暂无学习详情" />
        </template>
      </a-spin>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRoute, useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  UserOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  PieChartOutlined,
  DownOutlined,
  BarChartOutlined
} from '@ant-design/icons-vue';
import {
  getTrainDetail,
  getTrainStatistics,
  getTrainLearners,
  getTrainLearnerDetail,
  sendLearningReminder,
  resetLearningProgress
} from '@/api/train';
import { hasPermission } from '@/utils/permission';
import AssignPage from '@/components/train/AssignPage.vue';
import UserSelect from '@/components/common/UserSelect.vue';

export default defineComponent({
  name: 'TrainTrackingPage',
  components: {
    HeaderComponent,
    SiderComponent,
    AssignPage,
    UserOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    PieChartOutlined,
    DownOutlined,
    BarChartOutlined,
    UserSelect
  },
  setup() {
    // 初始化dayjs语言为中文
    dayjs.locale('zh-cn');

    const route = useRoute();
    const router = useRouter();
    const trainId = ref(parseInt(route.params.id));

    // 状态变量
    const loading = ref(false);
    const tableLoading = ref(false);
    const trainDetail = ref(null);
    const statistics = ref({});
    const learners = ref({ list: [], total: 0 });
    const assignModalVisible = ref(false);
    const userDetailVisible = ref(false);
    const userDetailLoading = ref(false);
    const userDetail = ref(null);

    const userId = ref();
    const statusFilter = ref('');

    // 分页
    const pagination = reactive({
      current: 1,
      pageSize: 10
    });

    // 表格列定义
    const columns = [
      {
        title: '用户名',
        dataIndex: 'userName',
        key: 'userName'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status'
      },
      {
        title: '进度',
        dataIndex: 'progress',
        key: 'progress',
        width: 220
      },
      {
        title: '指派时间',
        dataIndex: 'assignTime',
        key: 'assignTime',
        render: (text) => formatDate(text)
      },
      {
        title: '截止时间',
        dataIndex: 'deadline',
        key: 'deadline',
        render: (text) => text ? formatDate(text) : '无截止时间'
      }
      // {
      //   title: '操作',
      //   dataIndex: 'action',
      //   key: 'action'
      // }
    ];

    // 内容记录表格列定义
    const contentColumns = [
      {
        title: '内容名称',
        dataIndex: 'title',
        key: 'title'
      },
      {
        title: '类型',
        dataIndex: 'contentType',
        key: 'contentType',
        render: (text) => getContentTypeText(text)
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status'
      },
      {
        title: '学习时长',
        dataIndex: 'studyDuration',
        key: 'studyDuration'
      },
      {
        title: '得分',
        dataIndex: 'score',
        key: 'score'
      }
    ];// 获取培训详情
    const fetchTrainDetail = async () => {
      loading.value = true;
      try {
        const res = await getTrainDetail(trainId.value);
        if (res.code === 200&& res.data) {
          trainDetail.value = res.data;
        } else {
          message.error('获取培训详情失败');
        }
      } catch (error) {
        console.error('获取培训详情失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取培训统计数据
    const fetchTrainStatistics = async () => {
      try {
        const res = await getTrainStatistics(trainId.value);
        if (res.code === 200 && res.data) {
          statistics.value = res.data;
        }
      } catch (error) {
        console.error('获取培训统计数据失败:', error);
      }
    };

    // 获取培训学习人员列表
    const fetchTrainLearners = async () => {
      tableLoading.value = true;
      try {
        const params = {
          pageNum: pagination.current,
          pageSize: pagination.pageSize,
          userId: userId.value,
          status: statusFilter.value
        };

        const res = await getTrainLearners(trainId.value, params);
        if (res.code === 200 && res.data) {
          learners.value = res.data;
        }
      } catch (error) {
        console.error('获取培训学习人员列表失败:', error);
      } finally {
        tableLoading.value = false;
      }
    };

    // 检查用户是否有指定权限


    // 格式化日期
    const formatDate = (date) => {
      if (!date) return '';
      return dayjs(date).format('YYYY-MM-DD HH:mm');
    };

    // 格式化时长（秒转为时分秒）
    const formatDuration = (seconds) => {
      if (!seconds) return '0秒';

      const hours = Math.floor(seconds / 3600);
      const minutes = Math.floor((seconds % 3600) / 60);
      const remainingSeconds = seconds % 60;

      let result = '';
      if (hours > 0) {
        result += `${hours}小时`;
      }
      if (minutes > 0) {
        result += `${minutes}分钟`;
      }
      if (remainingSeconds > 0 || result === '') {
        result += `${remainingSeconds}秒`;
      }

      return result;
    };

    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'draft': '草稿',
        'published': '已发布',
        'unpublished': '未发布',
        'archived': '已归档'
      };
      return statusMap[status] || status;
    };

    // 获取学习状态文本
    const getStudyStatusText = (status) => {
      const statusMap = {
        'expired': '超时未完成',
        'not_started': '未开始',
        'learning': '学习中',
        'completed': '已完成'
      };
      return statusMap[status.toString()] || status;
    };

    // 获取状态颜色
    const getStatusColor = (status) => {
      const statusMap = {
        'not_started': 'default',
        'learning': 'processing',
        'completed': 'success'
      };
      return statusMap[status.toString()] || 'default';
    };

    // 获取进度状态
    const getProgressStatus = (status) => {
      const statusMap = {
        'not_started': 'normal',
        'learning': 'active',
        'completed': 'success'
      };
      return statusMap[status.toString()] || 'normal';
    };

    // 获取内容类型文本
    const getContentTypeText = (type) => {
      const typeMap = {
        'COURSE': '课程',
        'EXAM': '考试',
        'ASSIGNMENT': '作业',
        'SURVEY': '调研'
      };
      return typeMap[type] || type;
    };

    // 获取内容状态颜色
    const getContentStatusColor = (status) => {
      const statusMap = {
        '-1': 'default',
        '0': 'processing',
        '1': 'success',
        '2': 'warning',
        '3': 'error'
      };
      return statusMap[status.toString()] || 'default';
    };

    // 获取内容状态文本
    const getContentStatusText = (status) => {
      const statusMap = {
        '-1': '未开始',
        '0': '进行中',
        '1': '已完成',
        '2': '未通过',
        '3': '已过期'
      };
      return statusMap[status.toString()] || status;
    };


    // 计算用户详情进度
    const calculateUserDetailProgress = (progress) => {
      if (!userDetail.value) return 0;

      if (userDetail.status === 1) {
        return 100;
      }

      if (userDetail.value.requiredTaskTotal === 0) {
        return 0;
      }

      return Math.round((userDetail.value.requiredTaskFinished / userDetail.value.requiredTaskTotal) * 100);
    };

    // 返回上一页
    const goBack = () => {
      router.push(`/train/detail/${trainId.value}`);
    };

    // 跳转到数据分析页面
    const goToAnalysisPage = () => {
      router.push(`/train/analysis/${trainId.value}`);
    };

    // 显示指派弹窗
    const showAssignModal = () => {
      assignModalVisible.value = true;
    };

    // 关闭指派弹窗
    const handleAssignModalClose = () => {
      assignModalVisible.value = false;
    };

    // 指派成功回调
    const handleAssignSuccess = (result) => {
      assignModalVisible.value = false;
      message.success(`成功指派给 ${result.success} 名用户`);
      // 刷新数据
      fetchTrainStatistics();
      fetchTrainLearners();
    };

    // 处理搜索
    const handleSearch = () => {
      pagination.current = 1;
      fetchTrainLearners();
    };

    // 处理状态筛选变更
    const handleStatusFilterChange = () => {
      pagination.current = 1;
      fetchTrainLearners();
    };

    // 处理页码变更
    const handlePageChange = (page, pageSize) => {
      pagination.current = page;
      pagination.pageSize = pageSize;
      fetchTrainLearners();
    };

    // 处理每页条数变更
    const handlePageSizeChange = (current, size) => {
      pagination.current = 1;
      pagination.pageSize = size;
      fetchTrainLearners();
    };

    // 显示用户详情
    const showUserDetail = async (record) => {
      userDetailVisible.value = true;
      userDetailLoading.value = true;

      try {
        const res = await getTrainLearnerDetail(trainId.value, record.userId);
        if (res.code === 200 && res.data) {
          userDetail.value = res.data;
        } else {
          message.error('获取学员学习详情失败');
        }
      } catch (error) {
        console.error('获取学员学习详情失败:', error);
      } finally {
        userDetailLoading.value = false;
      }
    };

    // 发送提醒
    const sendReminder = (record) => {
      Modal.confirm({
        title: '发送提醒',
        content: `确定要向 ${record.userName} 发送学习提醒吗？`,
        onOk: async () => {
          try {
            const res = await sendLearningReminder(trainId.value, record.userId);
            if (res.code === 200 && res.data) {
              message.success(`已向 ${record.userName} 发送学习提醒`);
            } else {
              message.error(res.message || '发送学习提醒失败');
            }
          } catch (error) {
            console.error('发送学习提醒失败:', error);
          }
        }
      });
    };

    // 重置进度
    const resetProgress = (record) => {
      Modal.confirm({
        title: '重置进度',
        content: `确定要重置 ${record.userName} 的学习进度吗？此操作不可恢复！`,
        okType: 'danger',
        onOk: async () => {
          try {
            const res = await resetLearningProgress(trainId.value, record.userId);
            if (res.code === 200 && res.data) {
              message.success(`已重置 ${record.userName} 的学习进度`);
              // 刷新数据
              fetchTrainStatistics();
              fetchTrainLearners();
            } else {
              message.error(res.message || '重置学习进度失败');
            }
          } catch (error) {
            console.error('重置学习进度失败:', error);
          }
        }
      });
    };

    onMounted(() => {
      fetchTrainDetail();
      fetchTrainStatistics();
      fetchTrainLearners();
    });

    return {
      trainId,
      trainDetail,
      statistics,
      learners,
      loading,
      tableLoading,
      columns,
      contentColumns,
      pagination,
      userId,
      statusFilter,
      assignModalVisible,
      userDetailVisible,
      userDetailLoading,
      userDetail,
      formatDate,
      formatDuration,
      getStatusText,
      getStudyStatusText,
      getStatusColor,
      getProgressStatus,
      getContentTypeText,
      getContentStatusColor,
      getContentStatusText,
      calculateUserDetailProgress,
      goBack,
      goToAnalysisPage,
      showAssignModal,
      handleAssignModalClose,
      handleAssignSuccess,
      handleSearch,
      handleStatusFilterChange,
      handlePageChange,
      handlePageSizeChange,
      showUserDetail,
      sendReminder,
      resetProgress,
      hasPermission
    };
  }
});
</script>

<style scoped>
.train-tracking-page {
  min-height: 100vh;
  background-color: #f0f2f5;
}

.header {
  background: #fff;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  height: auto;
  line-height: normal;
}

.content {
  padding: 24px;
}

.statistic-card {
  margin-bottom: 16px;
}

.learners-card {
  margin-top: 16px;
}
</style>
