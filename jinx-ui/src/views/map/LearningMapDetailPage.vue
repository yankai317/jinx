<template>
  <a-layout class="learning-map-detail-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

    <a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal" />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <!-- 标题栏 -->
        <div class="content-header">
          <PageBreadcrumb />
        </div>

        <!-- 顶部信息区 -->
        <a-page-header :title="mapDetail.name" class="page-header">
          <template #extra>
            <a-space>
              <a-button v-if="hasPermission('map:assign')" type="primary" @click="handleAssign">
                <user-add-outlined />指派
              </a-button>
              <a-button v-if="hasPermission('map:edit')" type="primary" @click="handleEdit">
                <edit-outlined />编辑
              </a-button>
              <a-button @click="navigateTo('/map/list')">
                <rollback-outlined />返回
              </a-button>
            </a-space>
          </template>
          <template #tags>
            <a-tag v-for="(category, index) in mapDetail.categoryNames" :key="index" color="blue">
              {{ category }}
            </a-tag>
          </template>
          <a-descriptions size="small" :column="4">
            <a-descriptions-item label="创建人">{{ mapDetail.creatorName }}</a-descriptions-item>
            <a-descriptions-item label="开放时间" :span="2">
              {{ mapDetail.startTime || '-' }} 至 {{ mapDetail.endTime || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="解锁方式" :span="1">
              {{ getUnlockModeName(mapDetail.unlockMode) }}
            </a-descriptions-item>
          </a-descriptions>
        </a-page-header>

        <a-row :gutter="16" class="map-content">
          <!-- 左侧内容区 -->
          <a-col :span="18">
            <!-- 封面图区 -->
            <a-card class="cover-card">
              <a-image :src="mapDetail.cover || 'https://via.placeholder.com/800x200?text=学习地图封面'" :preview="false"
                class="cover-image" />
            </a-card>

            <!-- 地图简介 -->
            <a-card title="地图简介" class="intro-card">
              <p>{{ mapDetail.introduction || '暂无简介' }}</p>
            </a-card>

            <!-- 阶段展示区 -->
            <a-card title="学习阶段" class="stages-card">
              <!-- 商务简约主题 -->
              <div v-if="mapDetail.theme === 'business'" class="business-theme">
                <a-steps :current="currentStageIndex" type="navigation">
                  <a-step v-for="(stage, index) in mapDetail.stages" :key="stage.id" :title="stage.name"
                    :status="getStageStatus(stage, index)" @click="selectStage(index)" />
                </a-steps>
              </div>

              <!-- 动感科技主题 -->
              <div v-else-if="mapDetail.theme === 'tech'" class="tech-theme">
                <div class="tech-stages">
                  <div v-for="(stage, index) in mapDetail.stages" :key="stage.id"
                    :class="['tech-stage', { 'active': currentStageIndex === index, 'completed': isStageCompleted(stage), 'locked': isStageUnlocked(stage, index) === false }]"
                    @click="selectStage(index)">
                    <div class="tech-stage-icon">
                      <check-circle-filled v-if="isStageCompleted(stage)" />
                      <lock-filled v-else-if="isStageUnlocked(stage, index) === false" />
                      <number-outlined v-else />
                    </div>
                    <div class="tech-stage-name">{{ stage.name }}</div>
                  </div>
                </div>
              </div>

              <!-- 农场时光主题 -->
              <div v-else-if="mapDetail.theme === 'farm'" class="farm-theme">
                <div class="farm-stages">
                  <div v-for="(stage, index) in mapDetail.stages" :key="stage.id"
                    :class="['farm-stage', { 'active': currentStageIndex === index, 'completed': isStageCompleted(stage), 'locked': isStageUnlocked(stage, index) === false }]"
                    @click="selectStage(index)">
                    <div class="farm-stage-icon">
                      <check-circle-filled v-if="isStageCompleted(stage)" />
                      <lock-filled v-else-if="isStageUnlocked(stage, index) === false" />
                      <plant-outlined v-else />
                    </div>
                    <div class="farm-stage-name">{{ stage.name }}</div>
                  </div>
                </div>
              </div>

              <!-- 中国元素主题 -->
              <div v-else-if="mapDetail.theme === 'chinese'" class="chinese-theme">
                <div class="chinese-stages">
                  <div v-for="(stage, index) in mapDetail.stages" :key="stage.id"
                    :class="['chinese-stage', { 'active': currentStageIndex === index, 'completed': isStageCompleted(stage), 'locked': isStageUnlocked(stage, index) === false }]"
                    @click="selectStage(index)">
                    <div class="chinese-stage-icon">
                      <check-circle-filled v-if="isStageCompleted(stage)" />
                      <lock-filled v-else-if="isStageUnlocked(stage, index) === false" />
                      <bank-outlined v-else />
                    </div>
                    <div class="chinese-stage-name">{{ stage.name }}</div>
                  </div>
                </div>
              </div>

              <!-- 列表模式 -->
              <div v-else class="list-theme">
                <a-collapse v-model:activeKey="activeCollapseKeys">
                  <a-collapse-panel v-for="(stage, index) in mapDetail.stages" :key="stage.id"
                    :header="getStageHeader(stage, index)" :disabled="isStageUnlocked(stage, index) === false">
                    <div class="stage-info">
                      <p v-if="stage.openType === 1">
                        开放时间：{{ stage.startTime || '-' }} 至 {{ stage.endTime || '-' }}
                      </p>
                      <p v-else-if="stage.openType === 2">
                        学习期限：{{ stage.durationDays || 0 }} 天
                      </p>
                      <p v-if="mapDetail.creditRule === 1">
                        阶段学分：{{ stage.credit || 0 }}
                      </p>
                      <p v-if="mapDetail.certificateRule === 2 && stage.certificateName">
                        阶段证书：{{ stage.certificateName }}
                      </p>
                    </div>
                    <task-list :task-list="stage.tasks" @view-detail="handleViewTaskDetail" />
                  </a-collapse-panel>
                </a-collapse>
              </div>
            </a-card>

            <!-- 当前阶段任务列表 -->
            <a-card v-if="mapDetail.theme !== 'list' && currentStage" :title="`${currentStage.name} - 任务列表`"
              class="tasks-card">
              <div class="stage-info">
                <p v-if="currentStage.openType === 1">
                  开放时间：{{ currentStage.startTime || '-' }} 至 {{ currentStage.endTime || '-' }}
                </p>
                <p v-else-if="currentStage.openType === 2">
                  学习期限：{{ currentStage.durationDays || 0 }} 天
                </p>
                <p v-if="mapDetail.creditRule === 1">
                  阶段学分：{{ currentStage.credit || 0 }}
                </p>
                <p v-if="mapDetail.certificateRule === 2 && currentStage.certificateName">
                  阶段证书：{{ currentStage.certificateName }}
                </p>
              </div>

              <task-list :task-list="currentStage.tasks" @view-detail="handleViewTaskDetail" />
            </a-card>
          </a-col>

          <!-- 右侧信息侧边栏 -->
          <a-col :span="6">
            <a-card title="学习地图信息" class="info-card">
              <a-descriptions layout="vertical" :column="1">
                <a-descriptions-item label="学分规则">
                  {{ mapDetail.creditRule === 0 ? '整体发放' : '按阶段发放' }}
                </a-descriptions-item>
                <a-descriptions-item label="必修学分">
                  {{ mapDetail.requiredCredit || 0 }}
                </a-descriptions-item>
                <a-descriptions-item label="选修学分">
                  {{ mapDetail.electiveCredit || 0 }}
                </a-descriptions-item>
                <a-descriptions-item label="证书规则">
                  {{ getCertificateRuleName(mapDetail.certificateRule) }}
                </a-descriptions-item>
                <a-descriptions-item v-if="mapDetail.certificateRule > 0 && mapDetail.certificateName" label="证书">
                  {{ mapDetail.certificateName }}
                </a-descriptions-item>
              </a-descriptions>

              <a-divider />

              <div class="progress-section">
                <h4>学习进度</h4>
                <a-progress :percent="learningProgress.percent" :status="learningProgress.status"
                  :format="percent => `${percent}%`" />
                <div class="progress-info">
                  <p>已完成：{{ learningProgress.completed }} / {{ learningProgress.total }}</p>
                  <p>学习时长：{{ learningProgress.duration || 0 }} 分钟</p>
                </div>
              </div>

              <a-divider />

              <div class="action-section">
                <a-button type="primary" block @click="handleContinueStudy">
                  继续学习
                </a-button>
              </div>
            </a-card>

            <!-- 学习记录卡片 -->
            <a-card title="最近学习记录" class="records-card">
              <a-list :data-source="learningRecords" :render-item="renderRecordItem" item-layout="horizontal"
                size="small">
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-list-item-meta>
                      <template #title>
                        <a @click="viewTaskDetail(item)">
                          {{ item.title }}
                        </a>
                      </template>
                    </a-list-item-meta>
                  </a-list-item>
                </template>
              </a-list>
            </a-card>
          </a-col>
        </a-row>

      </a-layout-content>
    </a-layout>

    <!-- 任务详情弹窗 -->
    <a-modal v-model:visible="taskDetailVisible" :title="currentTask.title" width="800px" @cancel="closeTaskDetail"
      :footer="null">
      <div v-if="taskDetailVisible" class="task-detail-container">
        <a-descriptions :column="1">
          <a-descriptions-item label="任务类型">
            {{ getTaskTypeName(currentTask.type) }}
          </a-descriptions-item>
          <a-descriptions-item label="是否必修">
            {{ currentTask.isRequired ? '必修' : '选修' }}
          </a-descriptions-item>
        </a-descriptions>

        <div class="task-content">
          <!-- 课程内容 -->
          <div v-if="currentTask.type === 'CONTENT'" class="content-preview">
            <div v-if="currentTask.contentType === 'video'" class="video-container">
              <video-player :src="currentTask.contentUrl" />
            </div>
            <div v-else-if="currentTask.contentType === 'document'" class="document-container">
              <a-button type="primary" @click="downloadDocument(currentTask)">
                <download-outlined />下载文档
              </a-button>
              <a-button style="margin-left: 8px;" @click="previewDocument(currentTask)">
                <eye-outlined />预览文档
              </a-button>
            </div>
            <div v-else class="article-container">
              <div class="article-content" v-html="taskContent"></div>
            </div>
          </div>

          <!-- 考试内容 -->
          <div v-else-if="currentTask.type === 'EXAM'" class="exam-preview">
            <a-result title="考试任务" sub-title="点击下方按钮开始考试">
              <template #icon>
                <form-outlined style="color: #1890ff" />
              </template>
              <template #extra>
                <a-button type="primary" @click="startExam(currentTask)">
                  开始考试
                </a-button>
              </template>
            </a-result>
          </div>

          <!-- 作业内容 -->
          <div v-else-if="currentTask.type === 'ASSIGNMENT'" class="assignment-preview">
            <a-result title="作业任务" sub-title="点击下方按钮查看作业详情">
              <template #icon>
                <solution-outlined style="color: #1890ff" />
              </template>
              <template #extra>
                <a-button type="primary" @click="viewAssignment(currentTask)">
                  查看作业
                </a-button>
              </template>
            </a-result>
          </div>

          <!-- 问卷内容 -->
          <div v-else-if="currentTask.type === 'SURVEY'" class="survey-preview">
            <a-result title="问卷调查" sub-title="点击下方按钮开始填写问卷">
              <template #icon>
                <file-text-outlined style="color: #1890ff" />
              </template>
              <template #extra>
                <a-button type="primary" @click="startSurvey(currentTask)">
                  开始填写
                </a-button>
              </template>
            </a-result>
          </div>
        </div>

        <div class="task-actions">
          <a-button type="primary" @click="completeTask(currentTask)">
            完成学习
          </a-button>
          <a-button style="margin-left: 8px;" @click="closeTaskDetail">
            关闭
          </a-button>
        </div>
      </div>
    </a-modal>
  </a-layout>
</template>

<script>
import { ref, reactive, computed, onMounted, defineComponent, watch, h } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { getLearningMapDetail, getLearningMapLearnerDetail } from '@/api/learningMap';
import { hasPermission } from '@/utils/permission';
import VideoPlayer from '@/components/common/VideoPlayer.vue';
import TaskList from '@/components/map/TaskList.vue';
import {
  UserOutlined,
  SettingOutlined,
  LogoutOutlined,
  DownOutlined,
  DashboardOutlined,
  ReadOutlined,
  SolutionOutlined,
  ClusterOutlined,
  TrophyOutlined,
  AppstoreOutlined,
  EditOutlined,
  RollbackOutlined,
  CheckCircleFilled,
  LockFilled,
  NumberOutlined,
  PlantOutlined,
  BankOutlined,
  DownloadOutlined,
  EyeOutlined,
  FormOutlined,
  FileTextOutlined,
  UserAddOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';

export default defineComponent({
  name: 'LearningMapDetailPage',
  components: {
    HeaderComponent,
    SiderComponent,
    UserOutlined,
    SettingOutlined,
    LogoutOutlined,
    DownOutlined,
    DashboardOutlined,
    ReadOutlined,
    SolutionOutlined,
    ClusterOutlined,
    TrophyOutlined,
    AppstoreOutlined,
    EditOutlined,
    RollbackOutlined,
    CheckCircleFilled,
    LockFilled,
    NumberOutlined,
    PlantOutlined,
    BankOutlined,
    DownloadOutlined,
    EyeOutlined,
    FormOutlined,
    FileTextOutlined,
    UserAddOutlined,
    VideoPlayer,
    TaskList,
    PageBreadcrumb
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const route = useRoute();
    const mapId = ref(parseInt(route.params.id));
    const loading = ref(false);
    const collapsed = ref(false);
    const selectedKeys = ref(['mapList']);
    const openKeys = ref(['learning']);

    // 用户信息
    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      employeeNo: '',
      departments: [],
      roles: [],
      permissions: []
    });

    // 权限列表


    // 学习地图详情
    const mapDetail = ref({
      id: null,
      name: '',
      cover: '',
      introduction: '',
      creditRule: 0,
      requiredCredit: 0,
      electiveCredit: 0,
      categoryIds: [],
      categoryNames: [],
      certificateRule: 0,
      certificateId: null,
      certificateName: '',
      dingtalkGroup: 0,
      dingtalkGroupId: '',
      startTime: '',
      endTime: '',
      unlockMode: 0,
      theme: 'business',
      creatorId: null,
      creatorName: '',
      stages: []
    });

    // 当前阶段索引
    const currentStageIndex = ref(0);

    // 当前阶段
    const currentStage = computed(() => {
      if (mapDetail.value.stages && mapDetail.value.stages.length > 0) {
        return mapDetail.value.stages[currentStageIndex.value];
      }
      return null;
    });

    // 折叠面板活动的key
    const activeCollapseKeys = ref([]);

    // 学习进度
    const learningProgress = reactive({
      percent: 0,
      status: 'active',
      completed: 0,
      total: 0,
      duration: 0
    });

    // 学习记录
    const learningRecords = ref([]);

    // 任务详情弹窗
    const taskDetailVisible = ref(false);
    const currentTask = ref({});
    const taskContent = ref('');

    // 获取用户信息
    const getUserInfo = () => {
      const userInfoStr = sessionStorage.getItem('userInfo');
      if (userInfoStr) {
        try {
          userInfo.value = JSON.parse(userInfoStr);
        } catch (error) {
          console.error('解析用户信息失败:', error);
        }
      }
    };





    // 获取学习地图详情
    const fetchLearningMapDetail = async () => {
      loading.value = true;
      try {
        const res = await getLearningMapDetail(mapId.value);
        if (res.code === 200 && res.data) {
          mapDetail.value = res.data;

          // 设置默认活动的折叠面板
          if (mapDetail.value.stages && mapDetail.value.stages.length > 0) {
            activeCollapseKeys.value = [mapDetail.value.stages[0].id];
          }

          // 获取学习进度
          fetchLearningProgress();
        } else {
          message.error(res.message || '获取学习地图详情失败');
          router.push('/map/list');
        }
      } catch (error) {
        console.error('获取学习地图详情失败:', error);
        router.push('/map/list');
      } finally {
        loading.value = false;
      }
    };

    // 获取学习进度
    const fetchLearningProgress = async () => {
      try {
        if (!userInfo.value.userId) return;

        const res = await getLearningMapLearnerDetail(mapId.value, userInfo.value.userId);
        if (res.code === 200 && res.data) {
          const progressData = res.data;

          // 计算总任务数
          let totalTasks = 0;
          mapDetail.value.stages.forEach(stage => {
            if (stage.tasks) {
              totalTasks += stage.tasks.length;
            }
          });

          // 设置学习进度
          learningProgress.completed = progressData.completedTaskCount || 0;
          learningProgress.total = totalTasks;
          learningProgress.percent = totalTasks > 0 ? Math.round((learningProgress.completed / totalTasks) * 100) : 0;
          learningProgress.status = learningProgress.percent === 100 ? 'success' : 'active';
          learningProgress.duration = progressData.learningDuration || 0;

          // 设置学习记录
          if (progressData.recentRecords) {
            learningRecords.value = progressData.recentRecords;
          } else {
            // 模拟数据
            learningRecords.value = [
              {
                taskId: 1,
                taskName: '入门课程学习',
                learnTime: '2023-06-10 10:30:00',
                duration: 30
              },
              {
                taskId: 2,
                taskName: '基础知识测试',
                learnTime: '2023-06-11 14:20:00',
                duration: 45
              }
            ];
          }
        }
      } catch (error) {
        console.error('获取学习进度失败:', error);
      }
    };

    // 选择阶段
    const selectStage = (index) => {
      // 检查阶段是否解锁
      if (isStageUnlocked(mapDetail.value.stages[index], index)) {
        currentStageIndex.value = index;
      } else {
        message.warning('该阶段尚未解锁');
      }
    };

    // 获取阶段状态
    const getStageStatus = (stage, index) => {
      if (isStageCompleted(stage)) {
        return 'finish';
      } else if (currentStageIndex.value === index) {
        return 'process';
      } else if (isStageUnlocked(stage, index)) {
        return 'wait';
      } else {
        return 'wait';
      }
    };

    // 判断阶段是否完成
    const isStageCompleted = (stage) => {
      // 这里应该根据实际业务逻辑判断阶段是否完成
      // 暂时使用模拟数据
      return false;
    };

    // 判断阶段是否解锁
    const isStageUnlocked = (stage, index) => {
      // 根据解锁规则判断阶段是否解锁
      const unlockMode = mapDetail.value.unlockMode;

      // 自由模式，所有阶段都解锁
      if (unlockMode === 2) {
        return true;
      }

      // 第一个阶段始终解锁
      if (index === 0) {
        return true;
      }

      // 按阶段解锁，前一个阶段完成后解锁
      if (unlockMode === 1) {
        const prevStage = mapDetail.value.stages[index - 1];
        return isStageCompleted(prevStage);
      }

      // 按阶段和任务解锁，前一个阶段的所有任务完成后解锁
      if (unlockMode === 0) {
        const prevStage = mapDetail.value.stages[index - 1];
        return isStageCompleted(prevStage);
      }

      return false;
    };

    // 获取阶段标题
    const getStageHeader = (stage, index) => {
      let status = '';
      if (isStageCompleted(stage)) {
        status = '(已完成)';
      } else if (isStageUnlocked(stage, index) === false) {
        status = '(未解锁)';
      }

      return `${index + 1}. ${stage.name} ${status}`;
    };

    // 渲染列表项
    const renderListItem = (item) => {
      const taskTypeIcon = {
        'CONTENT': 'read-outlined',
        'EXAM': 'form-outlined',
        'ASSIGNMENT': 'solution-outlined',
        'SURVEY': 'file-text-outlined'
      };

      return h('a-list-item', {}, [
        h('a-list-item-meta', {
          avatar: h(taskTypeIcon[item.type], { style: 'font-size: 20px; color: #1890ff;' }),
          title: h('a', {
            onClick: () => viewTaskDetail(item)
          }, [
            item.title,
            item.isRequired
              ? h('a-tag', { color: 'red', style: 'margin-left: 8px;' }, '必修')
              : h('a-tag', { color: 'blue', style: 'margin-left: 8px;' }, '选修')
          ]),
          description: `类型：${getTaskTypeName(item.type)}`
        }),
        h('a-button', {
          type: 'link',
          onClick: () => viewTaskDetail(item)
        }, '查看')
      ]);
    };

    // 渲染记录项
    const renderRecordItem = (item) => {
      return h('a-list-item', {}, [
        h('a-list-item-meta', {
          title: item.taskName,
          description: `学习时间：${item.learnTime}，时长：${item.duration}分钟`
        })
      ]);
    };

    // 查看任务详情
    const viewTaskDetail = (task) => {
      currentTask.value = task;
      taskDetailVisible.value = true;

      // 获取任务内容
      if (task.type === 'CONTENT' && task.contentType === 'article') {
        // 这里应该调用API获取文章内容
        // 暂时使用模拟数据
        taskContent.value = '<h3>文章内容</h3><p>这是一篇示例文章，实际内容应该从API获取。</p>';
      }
    };

    // 关闭任务详情
    const closeTaskDetail = () => {
      taskDetailVisible.value = false;
      currentTask.value = {};
      taskContent.value = '';
    };

    // 下载文档
    const downloadDocument = (task) => {
      // 这里应该调用API下载文档
      message.success('开始下载文档');
      window.open(task.contentUrl, '_blank');
    };

    // 预览文档
    const previewDocument = (task) => {
      // 这里应该调用API预览文档
      message.success('开始预览文档');
      window.open(task.contentUrl, '_blank');
    };

    // 开始考试
    const startExam = (task) => {
      // 这里应该跳转到考试页面
      message.success('即将开始考试');
      window.open(task.contentUrl, '_blank');
    };

    // 查看作业
    const viewAssignment = (task) => {
      // 这里应该跳转到作业页面
      message.success('查看作业详情');
      window.open(task.contentUrl, '_blank');
    };

    // 开始问卷
    const startSurvey = (task) => {
      // 这里应该跳转到问卷页面
      message.success('开始填写问卷');
      window.open(task.contentUrl, '_blank');
    };

    // 完成任务
    const completeTask = (task) => {
      // 这里应该调用API标记任务完成
      message.success('任务已完成');
      closeTaskDetail();

      // 刷新学习进度
      fetchLearningProgress();
    };

    // 继续学习
    const handleContinueStudy = () => {
      // 查找第一个未完成的任务
      for (let i = 0; i < mapDetail.value.stages.length; i++) {
        const stage = mapDetail.value.stages[i];
        if (stage.tasks && stage.tasks.length > 0) {
          for (let j = 0; j < stage.tasks.length; j++) {
            const task = stage.tasks[j];
            // 这里应该根据实际业务逻辑判断任务是否完成
            // 暂时假设所有任务都未完成
            selectStage(i);
            viewTaskDetail(task);
            return;
          }
        }
      }

      message.info('您已完成所有任务');
    };

    // 编辑学习地图
    const handleEdit = () => {
      router.push(`/map/edit/${mapId.value}`);
    };

    // 指派学习地图
    const handleAssign = () => {
      router.push(`/map/assign/${mapId.value}`);
    };

    // 获取解锁方式名称
    const getUnlockModeName = (mode) => {
      const modeMap = {
        0: '按阶段和任务',
        1: '按阶段',
        2: '自由模式'
      };
      return modeMap[mode] || '未知';
    };

    // 获取证书规则名称
    const getCertificateRuleName = (rule) => {
      const ruleMap = {
        0: '不发放',
        1: '整体发放',
        2: '按阶段发放'
      };
      return ruleMap[rule] || '未知';
    };

    // 获取任务类型名称
    const getTaskTypeName = (type) => {
      const typeMap = {
        'CONTENT': '课程',
        'EXAM': '考试',
        'ASSIGNMENT': '作业',
        'SURVEY': '问卷'
      };
      return typeMap[type] || type;
    };

    // 退出登录
    const handleLogout = () => {
      // 清除登录信息
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('userInfo');
      localStorage.removeItem('rememberMe');

      message.success('已退出登录');

      // 跳转到登录页
      router.push('/login');
    };

    // 页面跳转
    const navigateTo = (path) => {
      router.push(path);
    };

    const handleViewTaskDetail = (task) => {
      // 处理查看任务详情的逻辑
      console.log('查看任务详情:', task)
    }

    onMounted(() => {
      // 获取用户信息
      getUserInfo();
      // 获取学习地图详情
      fetchLearningMapDetail();
    });

    return {
      mapId,
      loading,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,

      mapDetail,
      currentStageIndex,
      currentStage,
      activeCollapseKeys,
      learningProgress,
      learningRecords,
      taskDetailVisible,
      currentTask,
      taskContent,
      hasPermission,
      selectStage,
      getStageStatus,
      isStageCompleted,
      isStageUnlocked,
      getStageHeader,
      renderListItem,
      renderRecordItem,
      viewTaskDetail,
      closeTaskDetail,
      downloadDocument,
      previewDocument,
      startExam,
      viewAssignment,
      startSurvey,
      completeTask,
      handleContinueStudy,
      handleEdit,
      handleAssign,
      getUnlockModeName,
      getCertificateRuleName,
      getTaskTypeName,
      handleLogout,
      navigateTo,
      handleViewTaskDetail
    };
  }
});
</script>

<style scoped>
.learning-map-detail-layout {
  min-height: 100vh;
}



.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: auto;
}

.page-header {
  background-color: transparent;
  padding: 16px 24px;
  border-radius: 2px;
}

.map-content {
  height: calc(100% - 140px);
  overflow-y: auto;
  margin-top: 16px;
}

.cover-card,
.intro-card,
.stages-card,
.tasks-card,
.info-card,
.records-card {
  margin-bottom: 16px;
}


.cover-image {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
}

.stage-info {
  margin-bottom: 16px;
  padding: 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.progress-section {
  margin: 16px 0;
}

.progress-info {
  margin-top: 8px;
  color: rgba(0, 0, 0, 0.45);
}

.action-section {
  margin-top: 16px;
}

/* 商务简约主题 */
.business-theme {
  padding: 16px 0;
}

/* 动感科技主题 */
.tech-theme {
  padding: 16px 0;
}

.tech-stages {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}

.tech-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 8px;
  padding: 16px;
  border-radius: 8px;
  background-color: #f0f7ff;
  cursor: pointer;
  transition: all 0.3s;
  width: 120px;
  height: 120px;
  justify-content: center;
}

.tech-stage.active {
  background-color: #1890ff;
  color: #fff;
}

.tech-stage.completed {
  background-color: #52c41a;
  color: #fff;
}

.tech-stage.locked {
  background-color: #f5f5f5;
  color: #d9d9d9;
  cursor: not-allowed;
}

.tech-stage-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.tech-stage-name {
  text-align: center;
  font-weight: 500;
}

/* 农场时光主题 */
.farm-theme {
  padding: 16px 0;
}

.farm-stages {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}

.farm-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 8px;
  padding: 16px;
  border-radius: 50%;
  background-color: #f6ffed;
  cursor: pointer;
  transition: all 0.3s;
  width: 120px;
  height: 120px;
  justify-content: center;
  border: 2px solid #52c41a;
}

.farm-stage.active {
  background-color: #52c41a;
  color: #fff;
}

.farm-stage.completed {
  background-color: #b7eb8f;
  color: #135200;
}

.farm-stage.locked {
  background-color: #f5f5f5;
  color: #d9d9d9;
  border-color: #d9d9d9;
  cursor: not-allowed;
}

.farm-stage-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.farm-stage-name {
  text-align: center;
  font-weight: 500;
}

/* 中国元素主题 */
.chinese-theme {
  padding: 16px 0;
}

.chinese-stages {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}

.chinese-stage {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 8px;
  padding: 16px;
  border-radius: 4px;
  background-color: #fff1f0;
  cursor: pointer;
  transition: all 0.3s;
  width: 120px;
  height: 120px;
  justify-content: center;
  border: 2px solid #ff4d4f;
}

.chinese-stage.active {
  background-color: #ff4d4f;
  color: #fff;
}

.chinese-stage.completed {
  background-color: #ffccc7;
  color: #820014;
}

.chinese-stage.locked {
  background-color: #f5f5f5;
  color: #d9d9d9;
  border-color: #d9d9d9;
  cursor: not-allowed;
}

.chinese-stage-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.chinese-stage-name {
  text-align: center;
  font-weight: 500;
}

/* 列表主题 */
.list-theme {
  padding: 16px 0;
}

/* 任务详情 */
.task-detail-container {
  padding: 16px;
}

.task-content {
  margin: 24px 0;
  min-height: 200px;
}

.video-container {
  width: 100%;
  height: 400px;
}

.document-container {
  text-align: center;
  padding: 40px 0;
}

.article-container {
  padding: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}

.article-content {
  min-height: 200px;
}

.task-actions {
  margin-top: 24px;
  text-align: center;
}
</style>
