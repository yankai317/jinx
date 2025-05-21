<template>
  <div class="learning-map-tracking-page">
    <a-layout>
      <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

      <a-layout-content class="content">
        <a-spin :spinning="loading">
          <!-- 学习地图统计信息 -->
          <a-row :gutter="16">
            <a-col :span="6">
              <a-card class="statistic-card">
                <a-statistic
                  title="学习人数"
                  :value="statistics.learnerCount || 0"
                  :valueStyle="{ color: '#1890ff' }"
                >
                  <template #prefix>
                    <user-outlined />
                  </template>
                </a-statistic>
              </a-card>
            </a-col>
            <a-col :span="6">
              <a-card class="statistic-card">
                <a-statistic
                  title="完成人数"
                  :value="statistics.completionCount || 0"
                  :valueStyle="{ color: '#52c41a' }"
                >
                  <template #prefix>
                    <check-circle-outlined />
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
                    <clock-circle-outlined />
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
                  <template #prefix>
                    <pie-chart-outlined />
                  </template>
                </a-statistic>
              </a-card>
            </a-col>
          </a-row>

          <!-- 阶段完成情况统计 -->
          <a-card title="阶段完成情况" class="stages-card">
            <a-table
              :columns="stageColumns"
              :data-source="statistics.stageCompletionRates || []"
              :pagination="false"
              size="small"
              :rowKey="record => record.name"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'rate'">
                  <a-progress
                    :percent="record.rate * 100"
                    size="small"
                  />
                </template>
              </template>
            </a-table>
          </a-card>

          <!-- 学习人员列表 -->
          <a-card title="学习人员列表" class="learners-card">
            <template #extra>
              <a-space>
                <a-input-search
                  v-model:value="searchKeyword"
                  placeholder="搜索用户名/工号"
                  style="width: 200px"
                  @search="handleSearch"
                />
                <a-select
                  v-model:value="departmentFilter"
                  style="width: 150px"
                  placeholder="选择部门"
                  @change="handleFilterChange"
                  allowClear
                >
                  <a-select-option v-for="dept in departments" :key="dept.id" :value="dept.id">
                    {{ dept.name }}
                  </a-select-option>
                </a-select>
                <a-select
                  v-model:value="sourceFilter"
                  style="width: 120px"
                  placeholder="学习来源"
                  @change="handleFilterChange"
                  allowClear
                >
                  <a-select-option value="all">全部来源</a-select-option>
                  <a-select-option value="ASSIGN">指派</a-select-option>
                  <a-select-option value="SELF">自主</a-select-option>
                </a-select>
                <a-select
                  v-model:value="statusFilter"
                  style="width: 120px"
                  placeholder="学习状态"
                  @change="handleFilterChange"
                  allowClear
                >
                  <a-select-option value="all">全部状态</a-select-option>
                  <a-select-option value="0">学习中</a-select-option>
                  <a-select-option value="1">已完成</a-select-option>
                </a-select>
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
                onChange: handlePageChange,
                onShowSizeChange: handlePageSizeChange
              }"
              :loading="tableLoading"
              rowKey="userId"
            >
              <!-- 用户名列 -->
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'userName'">
                  {{ record.userName }}
                </template>

                <!-- 学习来源列 -->
                <template v-else-if="column.dataIndex === 'source'">
                  <a-tag :color="record.source === 'ASSIGN' ? 'blue' : 'green'">
                    {{ record.source === 'ASSIGN' ? '指派' : '自主' }}
                  </a-tag>
                </template>

                <!-- 状态列 -->
                <template v-else-if="column.dataIndex === 'status'">
                  <a-tag :color="getStatusColor(record.status)">
                    {{ getStudyStatusText(record.status) }}
                  </a-tag>
                </template>

                <!-- 进度列 -->
                <template v-else-if="column.dataIndex === 'progress'">
                  <a-progress
                    :percent="record.progress"
                    size="small"
                    :status="getProgressStatus(record.status)"
                  />
                </template>

                <!-- 操作列 -->
                <template v-else-if="column.dataIndex === 'action'">
                  <a-space wrap>
                    <a @click="showUserDetail(record)">详情</a>
                    <a-divider type="vertical" />
                    <a-dropdown>
                      <a class="ant-dropdown-link" @click.prevent>
                        更多 <down-outlined />
                      </a>
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

    <!-- 指派学习地图弹窗 -->
    <AssignPage
      :visible="assignModalVisible"
      :trainId="mapId"
      bizType="LEARNING_MAP"
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
            <a-descriptions-item label="学习来源">
              <a-tag :color="userDetail.source === 'ASSIGN' ? 'blue' : 'green'">
                {{ userDetail.source === 'ASSIGN' ? '指派' : '自主' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="指派时间">{{ formatDate(userDetail.assignTime) }}</a-descriptions-item>
            <a-descriptions-item label="截止时间">
              {{ userDetail.deadline ? formatDate(userDetail.deadline) : '无截止时间' }}
            </a-descriptions-item>
            <a-descriptions-item label="完成时间">
              {{ userDetail.completionTime ? formatDate(userDetail.completionTime) : '未完成' }}
            </a-descriptions-item>
            <a-descriptions-item label="学习进度">
              <a-progress
                :percent="calculateUserDetailProgress()"
                size="small"
                :status="getProgressStatus(userDetail.status)"
              />
            </a-descriptions-item>
            <a-descriptions-item label="学习时长">
              {{ formatDuration(userDetail.studyDuration) }}
            </a-descriptions-item>
            <a-descriptions-item label="当前阶段">
              {{ userDetail.currentStageName || '未开始' }}
            </a-descriptions-item>
            <a-descriptions-item label="已完成阶段数">
              {{ userDetail.completedStageCount || 0 }} / {{ mapDetail.stages?.length || 0 }}
            </a-descriptions-item>
            <a-descriptions-item label="已获得学分">
              {{ userDetail.earnedCredit || 0 }}
            </a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <a-table
            :title="'学习任务记录'"
            :columns="taskColumns"
            :data-source="userDetail.taskRecords || []"
            :pagination="false"
            rowKey="id"
          >
            <template #bodyCell="{ column, record }">
              <!-- 任务状态列 -->
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="getTaskStatusColor(record.status)">
                  {{ getStudyStatusText(record.status) }}
                </a-tag>
              </template>

              <!-- 学习时长列 -->
              <template v-else-if="column.dataIndex === 'studyDuration'">
                {{ formatDuration(record.studyDuration) }}
              </template>

              <!-- 得分列 -->
              <template v-else-if="column.dataIndex === 'score'">
                <span v-if="record.score">
                  {{ record.score }}
                </span>
                <span v-else>-</span>
              </template>

              <!-- 通过状态列 -->
              <template v-else-if="column.dataIndex === 'passStatus'">
                <a-tag v-if="record.passStatus !== undefined" :color="record.passStatus === 1 ? 'green' : 'red'">
                  {{ record.passStatus === 1 ? '通过' : '未通过' }}
                </a-tag>
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

    <!-- 提醒主管弹窗 -->
    <a-modal
      v-model:visible="remindManagersModalVisible"
      title="提醒部门主管"
      @ok="handleRemindManagersConfirm"
      @cancel="remindManagersModalVisible = false"
      :confirmLoading="remindManagersLoading"
    >
      <a-form :model="remindManagersForm" layout="vertical">
        <a-form-item label="提醒内容">
          <a-textarea
            v-model:value="remindManagersForm.content"
            :rows="4"
            placeholder="请输入提醒内容"
          />
        </a-form-item>
        <a-form-item label="选择部门">
          <a-select
            v-model:value="remindManagersForm.departmentIds"
            mode="multiple"
            style="width: 100%"
            placeholder="请选择需要提醒的部门"
            :options="departments.map(dept => ({ value: dept.id, label: dept.name }))"
          />
        </a-form-item>
      </a-form>
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
  ExportOutlined,
  NotificationOutlined,
  TeamOutlined,
  UserAddOutlined
} from '@ant-design/icons-vue';
import {
  getLearningMapDetail,
  getLearningMapStatistics,
  getLearningMapLearners,
  getLearningMapLearnerDetail,
  sendLearningReminder,
  exportLearningMapData,
  remindAllLearners,
  remindDepartmentManagers,
  resetLearningProgress
} from '@/api/learningMap';
import { getDepartmentList } from '@/api/org';
import { hasPermission } from '@/utils/permission';
import AssignPage from '@/components/train/AssignPage.vue';

export default defineComponent({
  name: 'LearningMapTrackingPage',
  components: {
    HeaderComponent,
    SiderComponent,
    AssignPage,
    UserOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    PieChartOutlined,
    DownOutlined,
    ExportOutlined,
    NotificationOutlined,
    TeamOutlined,
    UserAddOutlined
  },
  setup() {
    // 初始化dayjs语言为中文
    dayjs.locale('zh-cn');

    const route = useRoute();
    const router = useRouter();
    const mapId = ref(parseInt(route.params.id));

    // 状态变量
    const loading = ref(false);
    const tableLoading = ref(false);
    const mapDetail = ref(null);
    const statistics = ref({});
    const learners = ref({ list: [], total: 0 });
    const departments = ref([]);
    const assignModalVisible = ref(false);
    const userDetailVisible = ref(false);
    const userDetailLoading = ref(false);
    const userDetail = ref(null);


    // 搜索和筛选
    const searchKeyword = ref('');
    const departmentFilter = ref(undefined);
    const sourceFilter = ref('all');
    const statusFilter = ref('all');

    // 分页
    const pagination = reactive({
      current: 1,
      pageSize: 10
    });

    // 提醒主管弹窗
    const remindManagersModalVisible = ref(false);
    const remindManagersLoading = ref(false);
    const remindManagersForm = reactive({
      content: '您部门的员工在学习地图中的学习进度较慢，请督促他们及时完成学习任务。',
      departmentIds: []
    });

    // 阶段表格列定义
    const stageColumns = [
      {
        title: '阶段名称',
        dataIndex: 'name',
        key: 'name'
      },
      {
        title: '完成率',
        dataIndex: 'rate',
        key: 'rate'
      }
    ];

    // 学员表格列定义
    const columns = [
      {
        title: '用户名',
        dataIndex: 'userName',
        key: 'userName'
      },
      // {
      //   title: '部门',
      //   dataIndex: 'department',
      //   key: 'department'
      // },
      {
        title: '学习来源',
        dataIndex: 'source',
        key: 'source'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status'
      },
      {
        title: '进度',
        dataIndex: 'progress',
        key: 'progress'
      },
      {
        title: '当前阶段',
        dataIndex: 'currentStageName',
        key: 'currentStageName'
      },
      {
        title: '已完成任务',
        dataIndex: 'completedTasks',
        key: 'completedTasks',
        render: (_, record) => `${record.completedRequiredTaskCount || 0}/${record.completedElectiveTaskCount || 0}`
      },
      {
        title: '获得学分',
        dataIndex: 'earnedCredit',
        key: 'earnedCredit'
      },
      {
        title: '指派时间',
        dataIndex: 'assignTime',
        key: 'assignTime',
        render: (text) => formatDate(text)
      },
      // {
      //   title: '操作',
      //   dataIndex: 'action',
      //   key: 'action',
      //   fixed: 'right',
      //   width: 120
      // }
    ];

    // 任务记录表格列定义
    const taskColumns = [
      {
        title: '阶段',
        dataIndex: 'stageName',
        key: 'stageName'
      },
      {
        title: '任务名称',
        dataIndex: 'taskName',
        key: 'taskName'
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
        title: '学习进度',
        dataIndex: 'progress',
        key: 'progress',
        render: (text) => `${text || 0}%`
      },
      {
        title: '得分',
        dataIndex: 'score',
        key: 'score'
      },
      {
        title: '通过状态',
        dataIndex: 'passStatus',
        key: 'passStatus'
      },
      {
        title: '完成时间',
        dataIndex: 'completionTime',
        key: 'completionTime',
        render: (text) => formatDate(text)
      }
    ];

    // 获取学习地图详情
    const fetchLearningMapDetail = async () => {
      loading.value = true;
      try {
        const res = await getLearningMapDetail(mapId.value);
        if (res.code === 200 && res.data) {
          mapDetail.value = res.data;
        } else {
          message.error('获取学习地图详情失败');
        }
      } catch (error) {
        console.error('获取学习地图详情失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取学习地图统计数据
    const fetchLearningMapStatistics = async () => {
      try {
        const res = await getLearningMapStatistics(mapId.value);
        if (res.code === 200 && res.data) {
          statistics.value = res.data;
        }
      } catch (error) {
        console.error('获取学习地图统计数据失败:', error);
      }
    };

    // 获取学习地图学习人员列表
    const fetchLearningMapLearners = async () => {
      tableLoading.value = true;
      try {
        const params = {
          pageNum: pagination.current,
          pageSize: pagination.pageSize,
          keyword: searchKeyword.value,
          departmentId: departmentFilter.value,
          status: statusFilter.value === 'all' ? '' : statusFilter.value,
          source: sourceFilter.value === 'all' ? '' : sourceFilter.value
        };

        const res = await getLearningMapLearners(mapId.value, params);
        if (res.code === 200 && res.data) {
          learners.value = res.data;
        }
      } catch (error) {
        console.error('获取学习地图学习人员列表失败:', error);
      } finally {
        tableLoading.value = false;
      }
    };

    // 获取部门列表
    const fetchDepartments = async () => {
      try {
        const res = await getDepartmentList();
        if (res.code === 200 && res.data) {
          departments.value = res.data;
        }
      } catch (error) {
        console.error('获取部门列表失败:', error);
      }
    };



    // 检查用户是否有指定权限


    // 格式化日期
    const formatDate = (date) => {
      if (!date) return '';
      return dayjs(date).format('YYYY-MM-DD HH:mm');
    };

    // 格式化时长（分钟转为时分）
    const formatDuration = (minutes) => {
      if (!minutes) return '0分钟';

      const hours = Math.floor(minutes / 60);
      const remainingMinutes = minutes % 60;

      let result = '';
      if (hours > 0) {
        result += `${hours}小时`;
      }
      if (remainingMinutes > 0 || result === '') {
        result += `${remainingMinutes}分钟`;
      }

      return result;
    };

    // 获取学习状态文本
    const getStudyStatusText = (status) => {
      const statusMap = {
        'not_started': '未开始',
        'learning': '学习中',
        'completed': '已完成'
      };
      return statusMap[status?.toString()] || status;
    };

    // 获取状态颜色
    const getStatusColor = (status) => {
      const statusMap = {
        '0': 'processing',
        '1': 'success'
      };
      return statusMap[status?.toString()] || 'default';
    };

    // 获取任务状态颜色
    const getTaskStatusColor = (status) => {
      const statusMap = {
        '0': 'default',
        '1': 'processing',
        '2': 'success'
      };
      return statusMap[status?.toString()] || 'default';
    };

    // 获取进度状态
    const getProgressStatus = (status) => {
      const statusMap = {
        'not_started': 'normal',
        'learning': 'active',
        'completed': 'success'
      };
      return statusMap[status?.toString()] || 'normal';
    };


    // 计算进度
    const calculateProgress = (record) => {
      if (!record) return 0;

      if (record.status === 1) {
        return 100;
      }

      if (!mapDetail.value || !mapDetail.value.stages || mapDetail.value.stages.length === 0) {
        return 0;
      }

      return Math.round((record.completedStageCount / mapDetail.value.stages.length) * 100);
    };

    // 计算用户详情进度
    const calculateUserDetailProgress = () => {
      if (!userDetail.value) return 0;

      if (userDetail.value.status === 1) {
        return 100;
      }

      if (!mapDetail.value || !mapDetail.value.stages || mapDetail.value.stages.length === 0) {
        return 0;
      }

      return Math.round((userDetail.value.completedStageCount / mapDetail.value.stages.length) * 100);
    };

    // 返回上一页
    const goBack = () => {
      router.push(`/map/detail/${mapId.value}`);
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
      fetchLearningMapStatistics();
      fetchLearningMapLearners();
    };

    // 处理搜索
    const handleSearch = () => {
      pagination.current = 1;
      fetchLearningMapLearners();
    };

    // 处理筛选变更
    const handleFilterChange = () => {
      pagination.current = 1;
      fetchLearningMapLearners();
    };

    // 处理页码变更
    const handlePageChange = (page, pageSize) => {
      pagination.current = page;
      pagination.pageSize = pageSize;
      fetchLearningMapLearners();
    };

    // 处理每页条数变更
    const handlePageSizeChange = (current, size) => {
      pagination.current = 1;
      pagination.pageSize = size;
      fetchLearningMapLearners();
    };

    // 显示用户详情
    const showUserDetail = async (record) => {
      userDetailVisible.value = true;
      userDetailLoading.value = true;

      try {
        const res = await getLearningMapLearnerDetail(mapId.value, record.userId);
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
            const res = await sendLearningReminder(mapId.value, record.userId, '请尽快完成学习地图的学习任务');
            if (res.code === 200) {
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
            const res = await resetLearningProgress(mapId.value, record.userId);
            if (res.code === 200) {
              message.success(`已重置 ${record.userName} 的学习进度`);
              // 刷新数据
              fetchLearningMapStatistics();
              fetchLearningMapLearners();
            } else {
              message.error(res.message || '重置学习进度失败');
            }
          } catch (error) {
            console.error('重置学习进度失败:', error);
          }
        }
      });
    };

    // 导出数据
    const handleExport = () => {
      message.success('正在导出数据，请稍候...');

      try {
        exportLearningMapData(mapId.value, {
          departmentId: departmentFilter.value,
          status: statusFilter.value === 'all' ? '' : statusFilter.value,
          source: sourceFilter.value === 'all' ? '' : sourceFilter.value
        }).then(response => {
          // 创建Blob对象
          const blob = new Blob([response.data], { type: 'application/vnd.ms-excel' });
          // 创建下载链接
          const link = document.createElement('a');
          link.href = window.URL.createObjectURL(blob);
          link.download = `学习地图-${mapDetail.value?.name || mapId.value}-学习数据.xlsx`;
          link.click();
          // 释放URL对象
          window.URL.revokeObjectURL(link.href);

          message.success('数据导出成功');
        });
      } catch (error) {
        console.error('导出数据失败:', error);
      }
    };

    // 一键提醒
    const handleRemindAll = () => {
      Modal.confirm({
        title: '一键提醒',
        content: '确定要向所有未完成学习的学员发送提醒吗？',
        onOk: async () => {
          try {
            const res = await remindAllLearners(mapId.value, '请尽快完成学习地图的学习任务');
            if (res.code === 200) {
              message.success('已向所有未完成学习的学员发送提醒');
            } else {
              message.error(res.message || '发送提醒失败');
            }
          } catch (error) {
            console.error('发送提醒失败:', error);
          }
        }
      });
    };

    // 提醒主管
    const handleRemindManagers = () => {
      remindManagersModalVisible.value = true;
      // 默认选择所有部门
      remindManagersForm.departmentIds = departments.value.map(dept => dept.id);
    };

    // 确认提醒主管
    const handleRemindManagersConfirm = async () => {
      if (!remindManagersForm.content) {
        message.error('请输入提醒内容');
        return;
      }

      if (!remindManagersForm.departmentIds || remindManagersForm.departmentIds.length === 0) {
        message.error('请选择需要提醒的部门');
        return;
      }

      remindManagersLoading.value = true;
      try {
        const res = await remindDepartmentManagers(mapId.value, {
          content: remindManagersForm.content,
          departmentIds: remindManagersForm.departmentIds
        });

        if (res.code === 200) {
          message.success('已向所选部门的主管发送提醒');
          remindManagersModalVisible.value = false;
        } else {
          message.error(res.message || '发送提醒失败');
        }
      } catch (error) {
        console.error('发送提醒失败:', error);
      } finally {
        remindManagersLoading.value = false;
      }
    };

    onMounted(() => {
      fetchLearningMapDetail();
      fetchLearningMapStatistics();
      fetchLearningMapLearners();
      fetchDepartments();
    });

    return {
      mapId,
      mapDetail,
      statistics,
      learners,
      departments,
      loading,
      tableLoading,
      stageColumns,
      columns,
      taskColumns,
      pagination,
      searchKeyword,
      departmentFilter,
      sourceFilter,
      statusFilter,
      assignModalVisible,
      userDetailVisible,
      userDetailLoading,
      userDetail,
      remindManagersModalVisible,
      remindManagersLoading,
      remindManagersForm,
      formatDate,
      formatDuration,
      getStudyStatusText,
      getStatusColor,
      getTaskStatusColor,
      getProgressStatus,
      calculateProgress,
      calculateUserDetailProgress,
      goBack,
      showAssignModal,
      handleAssignModalClose,
      handleAssignSuccess,
      handleSearch,
      handleFilterChange,
      handlePageChange,
      handlePageSizeChange,
      showUserDetail,
      sendReminder,
      resetProgress,
      handleExport,
      handleRemindAll,
      handleRemindManagers,
      handleRemindManagersConfirm,
      hasPermission
    };
  }
});
</script>

<style scoped>
.learning-map-tracking-page {
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

.stages-card {
  margin-bottom: 16px;
}

.learners-card {
  margin-top: 16px;
}
</style>
