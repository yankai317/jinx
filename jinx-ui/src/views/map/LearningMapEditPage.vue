<template>
  <a-layout class="learning-map-edit-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" /><a-layout><!-- 使用 SiderComponent -->
      <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal" />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <!-- 标题栏 -->
        <div class="content-header">
          <PageBreadcrumb />
        </div>

        <!-- 表单区域 -->
        <div class="form-container">
          <a-spin :spinning="loading">
            <a-form ref="mapFormRef" class="form" :model="mapForm" :rules="rules"
              :label-col="{ style: { width: '120px' } }" :wrapper-col="{ span: 12 }">
              <!-- 基本信息 -->
              <div class="section-title">基本信息</div>
              <a-row :gutter="16">
                <a-col :span="16">
                  <a-form-item label="地图名称" name="name"><a-input v-model:value="mapForm.name" placeholder="请输入地图名称"
                      :maxlength="100" show-count /></a-form-item>
                </a-col>
              </a-row><a-form-item label="封面图" name="cover">
                <ImageCropper v-model:value="mapForm.cover" :width="640" :height="360" label="选择图片"
                  tip="支持JPG、PNG格式，裁剪为 640x360 像素" extra="请上传清晰美观的封面图，作为学习地图的主要视觉元素"
                  @crop-success="handleCropSuccess" />
              </a-form-item>

              <a-form-item label="地图简介" name="introduction">
                <a-textarea v-model:value="mapForm.introduction" placeholder="请输入地图简介" :rows="4" :maxlength="500"
                  show-count />
              </a-form-item>

              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="分类" name="categoryIds">
                    <CategoryTreeSelect v-model:value="mapForm.categoryIds" />
                  </a-form-item>
                </a-col>
              </a-row>

              <!-- 学分规则 -->
              <div class="section-title">学分规则</div>
              <a-form-item name="creditRule">
                <a-radio-group v-model:value="mapForm.creditRule">
                  <a-radio :value="0">整体发放</a-radio>
                  <a-radio :value="1">按阶段发放</a-radio>
                </a-radio-group>
              </a-form-item>

              <a-row :gutter="16" v-if="mapForm.creditRule === 0">
                <a-col :span="8">
                  <a-form-item label="必修学分" name="requiredCredit">
                    <a-input-number v-model:value="mapForm.requiredCredit" :min="0" :max="100" :precision="1"
                      style="width: 100%" />
                  </a-form-item>
                </a-col>
                <a-col :span="8">
                  <a-form-item label="选修学分" name="electiveCredit">
                    <a-input-number v-model:value="mapForm.electiveCredit" :min="0" :max="100" :precision="1"
                      style="width: 100%" />
                  </a-form-item>
                </a-col>
              </a-row>

              <!-- 证书规则 -->
              <div class="section-title">证书规则</div>
              <a-form-item name="certificateRule">
                <a-radio-group v-model:value="mapForm.certificateRule">
                  <a-radio :value="0">不发放</a-radio>
                  <a-radio :value="1">整体发放</a-radio>
                  <a-radio :value="2">按阶段发放</a-radio>
                </a-radio-group>
              </a-form-item>

              <a-form-item v-if="mapForm.certificateRule === 1" label="证书" name="certificateId">
                <a-select v-model:value="mapForm.certificateId" placeholder="请选择证书" style="width: 100%"
                  :loading="certificateLoading">
                  <a-select-option v-for="certificate in certificates" :key="certificate.id" :value="certificate.id">
                    {{ certificate.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>

              <!-- 学习规则 -->
              <div class="section-title">学习规则</div>
              <a-row :gutter="16">
                <a-col :span="12">
                  <a-form-item label="开放时间" name="openTime">
                    <a-range-picker v-model:value="dateRange" :show-time="{ format: 'HH:mm:ss' }"
                      format="YYYY-MM-DD HH:mm:ss" style="width: 100%" @change="handleDateRangeChange" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="解锁方式" name="unlockMode">
                    <a-select v-model:value="mapForm.unlockMode" placeholder="请选择解锁方式" style="width: 100%">
                      <a-select-option :value="0">按阶段和任务</a-select-option>
                      <a-select-option :value="1">按阶段</a-select-option>
                      <a-select-option :value="2">自由模式</a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>

              <!-- <a-form-item label="主题皮肤" name="theme">
                <a-radio-group v-model:value="mapForm.theme" button-style="solid">
                  <a-radio-button value="business">商务简约</a-radio-button>
                  <a-radio-button value="tech">动感科技</a-radio-button>
                  <a-radio-button value="farm">农场时光</a-radio-button>
                  <a-radio-button value="chinese">中国元素</a-radio-button>
                  <a-radio-button value="list">列表模式</a-radio-button>
                </a-radio-group>
              </a-form-item> -->

              <a-form-item label="钉钉群" name="dingtalkGroup">
                <a-radio-group v-model:value="mapForm.dingtalkGroup">
                  <a-radio :value="0">不创建</a-radio>
                  <a-radio :value="1">创建钉钉群</a-radio>
                </a-radio-group>
              </a-form-item>

              <!-- 阶段管理 -->
              <div class="section-title">
                阶段管理
                <a-button type="primary" size="small" style="margin-left: 16px;" @click="handleAddStage">
                  <plus-outlined />添加阶段
                </a-button>
              </div>

              <div v-if="mapForm.stages.length === 0" class="empty-stages">
                <a-empty description="暂无阶段，请添加阶段" />
              </div>

              <div v-else class="stages-container">
                <a-collapse v-model:activeKey="activeStageKeys">
                  <a-collapse-panel v-for="(stage, stageIndex) in mapForm.stages"
                    :key="stage.key || stage.id || `new-${stageIndex}`" :header="getStageHeader(stage, stageIndex)">
                    <template #extra>
                      <a-space>
                        <a-button type="primary" size="small" @click.stop="handleAddTask(stageIndex)">
                          <plus-outlined />添加任务
                        </a-button>
                        <a-button type="primary" size="small" danger @click.stop="handleDeleteStage(stageIndex)">
                          <delete-outlined />删除阶段
                        </a-button>
                      </a-space>
                    </template>

                    <!-- 阶段表单 -->
                    <a-form>
                      <a-row :gutter="16">
                        <a-col :span="12">
                          <a-form-item label="阶段名称" required>
                            <a-input v-model:value="stage.name" placeholder="请输入阶段名称" />
                          </a-form-item>
                        </a-col>
                        <a-col :span="12">
                          <a-form-item label="阶段顺序">
                            <a-input-number v-model:value="stage.stageOrder" :min="1" style="width: 100%" />
                          </a-form-item>
                        </a-col>
                      </a-row>

                      <a-form-item label="开放类型">
                        <a-radio-group v-model:value="stage.openType">
                          <a-radio :value="0">不设置</a-radio>
                          <a-radio :value="1">固定时间</a-radio>
                          <a-radio :value="2">学习期限</a-radio>
                        </a-radio-group>
                      </a-form-item>

                      <a-form-item v-if="stage.openType === 1" label="开放时间">
                        <a-range-picker v-model:value="stageTimeRanges[stageIndex]" :show-time="{ format: 'HH:mm:ss' }"
                          format="YYYY-MM-DD HH:mm:ss" style="width: 100%"
                          @change="(dates) => handleStageTimeChange(dates, stageIndex)" />
                      </a-form-item>

                      <a-form-item v-if="stage.openType === 2" label="学习期限(天)">
                        <a-input-number v-model:value="stage.durationDays" :min="1" style="width: 100%" />
                      </a-form-item><a-form-item v-if="mapForm.creditRule === 1" label="阶段学分">
                        <a-input-number v-model:value="stage.credit" :min="0" :precision="1"
                          style="width: 100%" /></a-form-item>

                      <a-form-item v-if="mapForm.certificateRule === 2" label="阶段证书">
                        <a-select v-model:value="stage.certificateId" placeholder="请选择证书" style="width: 100%"
                          :loading="certificateLoading"><a-select-option v-for="certificate in certificates"
                            :key="certificate.id" :value="certificate.id">
                            {{ certificate.name }}</a-select-option>
                        </a-select>
                      </a-form-item>

                      <!-- 任务列表 -->
                      <div class="tasks-section">
                        <div class="section-subtitle">任务列表</div>
                        <div v-if="!stage.tasks || stage.tasks.length === 0" class="empty-tasks"><a-empty
                            description="暂无任务，请添加任务" />
                        </div>
                        <a-table v-else :dataSource="stage.tasks" :columns="taskColumns" :pagination="false"
                          size="small" :rowKey="record => record.key || record.id || record.contentId">
                          <template #bodyCell="{ column, record, index }">
                            <template v-if="column.key === 'type'">
                              {{ getTaskTypeName(record.type) }}
                            </template>
                            <template v-if="column.key === 'isRequired'">
                              <a-switch v-model:checked="record.isRequired" :checkedChildren="'必修'"
                                :unCheckedChildren="'选修'" />
                            </template>
                            <template v-if="column.key === 'sortOrder'"><a-input-number v-model:value="record.sortOrder"
                                :min="1" size="small" />
                            </template>
                            <template v-if="column.key === 'action'">
                              <a-button type="link" danger @click="handleDeleteTask(stageIndex, index)">
                                <delete-outlined />
                              </a-button>
                            </template>
                          </template></a-table>
                      </div>
                    </a-form>
                  </a-collapse-panel>
                </a-collapse>
              </div>

              <!-- 可见范围设置 -->
              <div class="section-title">可见范围设置</div>
              <a-form-item name="visibility" label="可见范围">
                <a-radio-group v-model:value="mapForm.visibility.type"
                  :options="Object.values(VISIBILITY_TYPE).map(item => ({ label: item.name, value: item.key }))">
                </a-radio-group>
                <div v-if="mapForm.visibility.type === 'PART'" style="margin-top: 16px">
                  <a-button type="primary" @click="showOrgUserModal">
                    <team-outlined />
                    从组织架构选择
                  </a-button>
                  <div v-if="selectedRange.length > 0" class="selected-targets"><a-divider
                      orientation="left">已选择的范围</a-divider><a-tag v-for="item in selectedRange"
                      :key="`${item.type}-${item.id}`">
                      {{ item.name }} ({{ getTypeName(item.type) }})</a-tag></div>
                </div>
              </a-form-item>

              <!-- 协同管理 -->
              <div class="form-section">
                <div class="section-title">协同设置</div>
                <a-form-item label="协同编辑" name="collaborationEditType">
                  <a-radio-group v-model:value="mapForm.collaborators.editorType"
                    :options="Object.values(COLLABORATOR_TYPE).map(item => ({ label: item.name, value: item.key }))"
                    @change="mapForm.collaborators.editors = []">
                  </a-radio-group>
                  <div v-if="mapForm.collaborators.editorType === 'PART'" style="margin-top: 16px">
                    <UserSelect v-model:value="mapForm.collaborators.editors" mode="multiple" placeholder="请选择协同编辑人员"
                      style="width: 80%; margin-right: 16px" query-admin />
                    <a-button type="primary" @click="showEditorSelectionModal">
                      <template #icon><team-outlined /></template>
                      批量选择
                    </a-button>
                  </div>
                </a-form-item>
              </div>
            </a-form>
          </a-spin>
        </div>

        <!-- 底部操作按钮区域 -->
        <div class="footer-actions">
          <a-button @click="handleCancel">取消</a-button>
          <a-button type="primary" style="margin-left: 8px" @click="handleSave" v-if="hasPermission('map:edit')">
            保存修改
          </a-button>
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 组织架构选择弹窗 - 可见范围 -->
    <a-modal v-model:visible="orgUserModalVisible" title="选择可见范围" width="800px" :footer="null">
      <org-user-component v-if="orgUserModalVisible" selection-type="multiple" @confirm="handleOrgUserConfirm"
        @cancel="handleOrgUserCancel" :selected-ids="selectedRange" />
    </a-modal>

    <!-- 协同人员选择弹窗 - 协同编辑 -->
    <CollaboratorSelectionModal v-model:visible="editorSelectionModalVisible" @cancel="handleEditorSelectionCancel"
      @confirm="handleEditorSelectionConfirm" />

    <!-- 添加任务弹窗 -->
    <a-modal v-model:visible="taskModalVisible" title="添加任务" width="700px" @ok="handleTaskModalOk"
      @cancel="handleTaskModalCancel">
      <a-form :model="taskForm" layout="vertical">
        <a-form-item label="任务类型" required>
          <a-radio-group v-model:value="taskForm.type">
            <a-radio value="COURSE">课程</a-radio>
            <a-radio value="TRAIN">培训</a-radio>
            <!-- <a-radio value="EXAM">考试</a-radio>
            <a-radio value="ASSIGNMENT">作业</a-radio>
            <a-radio value="SURVEY">调研</a-radio> -->
          </a-radio-group>
        </a-form-item>

        <a-form-item label="选择内容" required>
          <a-select v-model:value="taskForm.contentId" :options="contentList" show-search :filter-option="false"
            placeholder="请输入课程名称搜索" style="width: 100%" :loading="contentLoading" @search="handleContentSearch">
          </a-select>
        </a-form-item>

        <a-form-item label="是否必修"><a-switch v-model:checked="taskForm.isRequired" :checkedChildren="'必修'"
            :unCheckedChildren="'选修'" />
        </a-form-item>

        <a-form-item label="排序序号">
          <a-input-number v-model:value="taskForm.sortOrder" :min="1" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>

  </a-layout>
</template>

<script>
import { ref, reactive, computed, onMounted, defineComponent, watch } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  getLearningMapDetail,
  updateLearningMap
} from '@/api/learningMap';
import { getCategoryList } from '@/api/category';
import { getCertificateList } from '@/api/certificate';
import { getCourseList } from '@/api/course';
import { getUserList } from '@/api/user';
import { hasPermission } from '@/utils/permission';
import OrgUserComponent from '@/components/common/OrgUserComponent.vue';
import CollaboratorSelectionModal from '@/components/common/CollaboratorSelectionModal.vue';
import {
  UserOutlined, SettingOutlined,
  LogoutOutlined,
  DownOutlined,
  DashboardOutlined,
  ReadOutlined,
  SolutionOutlined,
  ClusterOutlined,
  TrophyOutlined,
  AppstoreOutlined,
  PlusOutlined,
  DeleteOutlined,
  TeamOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';
import { queryRange } from '@/api/common';
import { COLLABORATOR_TYPE, VISIBILITY_TYPE } from '@/utils/constants';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';
import ImageCropper from '@/components/common/ImageCropper.vue';
import { getTrainList } from '@/api/train';
import UserSelect from '@/components/common/UserSelect.vue';

export default defineComponent({
  name: 'LearningMapEditPage',
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
    PlusOutlined,
    DeleteOutlined,
    TeamOutlined,
    OrgUserComponent,
    CollaboratorSelectionModal,
    CategoryTreeSelect,
    PageBreadcrumb,
    ImageCropper,
    UserSelect
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');
    const router = useRouter();
    const route = useRoute();
    const mapId = ref(parseInt(route.params.id));
    const mapFormRef = ref(null);
    const collapsed = ref(false);
    const selectedKeys = ref(['mapList']);
    const openKeys = ref(['learning']);
    const loading = ref(false);
    const categoryModalVisible = ref(false);


    // 用户数据
    const userList = ref([]);
    const userOptions = computed(() => {
      return userList.value.map(user => ({
        label: `${user.nickname} (${user.employeeNo || '无工号'})`,
        value: user.userId
      }));
    });

    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      employeeNo: '',
      departments: [],
      roles: [],
      permissions: []
    });

    // 证书列表
    const certificates = ref([]);
    const certificateLoading = ref(false);

    // 分类列表
    const categories = ref([]);

    // 内容列表
    const contentList = ref([]);

    const contentLoading = ref(false);

    // 日期范围
    const dateRange = ref([]);

    // 阶段时间范围
    const stageTimeRanges = ref([]);

    // 活动的阶段key
    const activeStageKeys = ref([]);

    // 组织架构选择弹窗
    const orgUserModalVisible = ref(false);
    const editorOrgUserModalVisible = ref(false);
    const userOrgUserModalVisible = ref(false);

    // 协同人员选择弹窗
    const editorSelectionModalVisible = ref(false);
    const userSelectUserSelectionModalVisible = ref(false);

    // 任务弹窗
    const taskModalVisible = ref(false);
    const currentStageIndex = ref(0);
    const taskForm = reactive({
      type: 'COURSE',
      contentId: null,
      isRequired: true,
      sortOrder: 1
    });

    // 任务表格列
    const taskColumns = [
      {
        title: '任务类型',
        dataIndex: 'type',
        key: 'type',
        width: 100
      },
      {
        title: '任务内容',
        dataIndex: 'title',
        key: 'title',
        ellipsis: true
      },
      {
        title: '是否必修',
        dataIndex: 'isRequired',
        key: 'isRequired',
        width: 100
      },
      {
        title: '排序',
        dataIndex: 'sortOrder',
        key: 'sortOrder',
        width: 80
      },
      {
        title: '操作',
        key: 'action',
        width: 80
      }
    ];

    // 学习地图表单
    const mapForm = reactive({
      id: null,
      name: '',
      cover: '',
      introduction: '',
      creditRule: 0,
      requiredCredit: 0,
      electiveCredit: 0,
      categoryIds: '',
      certificateRule: 0,
      certificateId: null,
      dingtalkGroup: 0,
      startTime: '',
      endTime: '',
      unlockMode: 0,
      theme: 'business',
      stages: [],
      deleteStageIds: [],
      deleteTaskIds: [],
      visibility: {
        type: 'ALL',
        targets: []
      },
      collaborators: {
        editorType: 'ALL',
        editors: []
      }
    });

    // 表单验证规则
    const rules = {
      name: [
        { required: true, message: '请输入地图名称', trigger: 'blur' },
        { max: 100, message: '地图名称最多100个字符', trigger: 'blur' }
      ],
      cover: [
        { required: true, message: '请上传封面图', trigger: 'change' }
      ],
      introduction: [
        { max: 500, message: '地图简介最多500个字符', trigger: 'blur' }
      ],
      certificateId: [
        {
          required: true,
          message: '请选择证书',
          trigger: 'change',
          validator: (rule, value) => {
            if (mapForm.certificateRule === 1 && !value) {
              return Promise.reject('请选择证书');
            }
            return Promise.resolve();
          }
        }
      ]
    };

    // 选择范围
    const selectedRange = ref([]);

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
          const mapDetail = res.data;

          // 填充表单数据
          mapForm.id = mapDetail.id;
          mapForm.name = mapDetail.name;
          mapForm.cover = mapDetail.cover;
          mapForm.introduction = mapDetail.introduction;
          mapForm.creditRule = mapDetail.creditRule;
          mapForm.requiredCredit = mapDetail.requiredCredit;
          mapForm.electiveCredit = mapDetail.electiveCredit;
          mapForm.categoryIds = mapDetail.categoryIds || '';
          mapForm.certificateRule = mapDetail.certificateRule;
          mapForm.certificateId = mapDetail.certificateId;
          mapForm.dingtalkGroup = mapDetail.dingtalkGroup;
          mapForm.unlockMode = mapDetail.unlockMode;
          mapForm.theme = mapDetail.theme || 'business';
          mapForm.collaborators = mapDetail.collaborators;
          mapForm.visibility = mapDetail.visibility;

          // 设置开放时间
          if (mapDetail.startTime && mapDetail.endTime) {
            mapForm.startTime = mapDetail.startTime;
            mapForm.endTime = mapDetail.endTime;
            dateRange.value = [
              dayjs(mapDetail.startTime),
              dayjs(mapDetail.endTime)
            ];
          }

          // 设置阶段
          if (mapDetail.stages && mapDetail.stages.length > 0) {
            mapForm.stages = mapDetail.stages.map(stage => {
              // 处理阶段时间
              if (stage.startTime && stage.endTime) {
                const stageIndex = stageTimeRanges.value.length;
                stageTimeRanges.value.push([
                  dayjs(stage.startTime),
                  dayjs(stage.endTime)
                ]);
              }

              // 处理任务
              const tasks = stage.tasks ? stage.tasks.map(task => ({
                id: task.id,
                type: task.type,
                contentId: task.contentId,
                title: task.title,
                contentType: task.contentType,
                isRequired: task.isRequired,
                sortOrder: task.sortOrder
              })) : [];

              return {
                id: stage.id,
                name: stage.name,
                stageOrder: stage.stageOrder,
                openType: stage.openType,
                startTime: stage.startTime,
                endTime: stage.endTime,
                durationDays: stage.durationDays,
                credit: stage.credit,
                certificateId: stage.certificateId,
                tasks: tasks
              };
            });

            // 设置默认展开第一个阶段
            if (mapForm.stages.length > 0) {
              activeStageKeys.value = [mapForm.stages[0].id || 'new-0'];
            }
          }

          getRange();
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

    // 获取分类列表
    const fetchCategories = async () => {
      try {
        const res = await getCategoryList();
        if (res.code === 200) {
          categories.value = res.data || [];
        }
      } catch (error) {
        console.error('获取分类列表失败:', error);
      }
    };

    // 获取证书列表
    const fetchCertificates = async () => {
      certificateLoading.value = true;
      try {
        const res = await getCertificateList({
          pageNum: 1,
          pageSize: 100
        });
        if (res.code === 200 && res.data) {
          certificates.value = res.data.list || [];
        }
      } catch (error) {
        console.error('获取证书列表失败:', error);
      } finally {
        certificateLoading.value = false;
      }
    };

    // 获取用户列表
    const fetchUserList = async () => {
      try {
        const res = await getUserList({
          pageNum: 1,
          pageSize: 100
        });
        if (res.code === 200 && res.data) {
          userList.value = res.data || [];
        }
      } catch (error) {
        console.error('获取用户列表失败:', error);
      }
    };

    const handleContentSearch = (value) => {
      console.log(value, 'value');
      fetchContentList(value);
    };

    // 获取内容列表
    const fetchContentList = async (title) => {
      contentLoading.value = true;
      try {
        let res = null;
        if (taskForm.type === 'COURSE') {
          // 获取课程列表
          res = await getCourseList({
            pageNum: 1,
            pageSize: 10,
            ifIsCitable: true,
            title: title
          });
        } else if (taskForm.type === 'TRAIN') {
          // 获取培训列表
          res = await getTrainList({
            pageNum: 1,
            pageSize: 10,
            ifIsCitable: true,
            title: title
          });
        }
        if (res.code === 200 && res.data) {
          console.log(res.data, 'res.data');
          contentList.value = res.data.list.map(item => ({
            label: item.title,
            value: item.id
          })) || [];
        }
      } catch (error) {
        console.error('获取内容列表失败:', error);
      } finally {
        contentLoading.value = false;
      }
    };

    // 处理日期范围变更
    const handleDateRangeChange = (dates) => {
      if (dates && dates.length === 2) {
        mapForm.startTime = dates[0].format('YYYY-MM-DD HH:mm:ss');
        mapForm.endTime = dates[1].format('YYYY-MM-DD HH:mm:ss');
      } else {
        mapForm.startTime = '';
        mapForm.endTime = '';
      }
    };

    // 处理阶段时间变更
    const handleStageTimeChange = (dates, stageIndex) => {
      if (dates && dates.length === 2) {
        mapForm.stages[stageIndex].startTime = dates[0].format('YYYY-MM-DD HH:mm:ss');
        mapForm.stages[stageIndex].endTime = dates[1].format('YYYY-MM-DD HH:mm:ss');
      } else {
        mapForm.stages[stageIndex].startTime = '';
        mapForm.stages[stageIndex].endTime = '';
      }
    };

    // 获取阶段标题
    const getStageHeader = (stage, index) => {
      return `${index + 1}. ${stage.name || '未命名阶段'}`;
    };

    // 添加阶段
    const handleAddStage = () => {
      const newStage = {
        key: `new-${Date.now()}`,
        name: `阶段${mapForm.stages.length + 1}`,
        stageOrder: mapForm.stages.length + 1,
        openType: 0,
        startTime: '',
        endTime: '',
        durationDays: 7,
        credit: 0,
        certificateId: null,
        tasks: []
      };

      mapForm.stages.push(newStage);
      activeStageKeys.value = [newStage.key];
    };

    // 删除阶段
    const handleDeleteStage = (index) => {
      const stage = mapForm.stages[index];
      // 如果是已有阶段，添加到删除列表
      if (stage.id) {
        mapForm.deleteStageIds.push(stage.id);
        // 添加阶段下的任务到删除列表
        if (stage.tasks && stage.tasks.length > 0) {
          stage.tasks.forEach(task => {
            if (task.id) {
              mapForm.deleteTaskIds.push(task.id);
            }
          });
        }
      }

      // 从阶段列表中移除
      mapForm.stages.splice(index, 1);
    };

    // 添加任务
    const handleAddTask = (stageIndex) => {
      currentStageIndex.value = stageIndex;// 重置任务表单
      taskForm.type = 'COURSE';
      taskForm.contentId = null;
      taskForm.isRequired = true;
      taskForm.sortOrder = mapForm.stages[stageIndex].tasks.length + 1;

      fetchContentList('');
      // 显示任务弹窗
      taskModalVisible.value = true;
    };

    // 处理任务弹窗确认
    const handleTaskModalOk = () => {
      // 验证表单
      if (!taskForm.type) {
        message.error('请选择任务类型');
        return;
      }
      if (!taskForm.contentId) {
        message.error('请选择内容');
        return;
      }

      // 获取内容标题
      let contentTitle = '';

      const course = contentList.value?.find(item => item.value === taskForm.contentId);
      if (course) {
        contentTitle = course?.label;
      }


      // 创建新任务
      const newTask = {
        key: `new-${Date.now()}`,
        type: taskForm.type,
        contentId: taskForm.contentId,
        title: contentTitle,
        isRequired: taskForm.isRequired,
        sortOrder: taskForm.sortOrder
      };

      // 添加到阶段任务列表
      mapForm.stages[currentStageIndex.value].tasks.push(newTask);// 关闭弹窗
      taskModalVisible.value = false;
    };// 处理任务弹窗取消
    const handleTaskModalCancel = () => {
      taskModalVisible.value = false;
    };

    // 删除任务
    const handleDeleteTask = (stageIndex, taskIndex) => {
      const task = mapForm.stages[stageIndex].tasks[taskIndex];

      // 如果是已有任务，添加到删除列表
      if (task.id) {
        mapForm.deleteTaskIds.push(task.id);
      }

      // 从任务列表中移除
      mapForm.stages[stageIndex].tasks.splice(taskIndex, 1);
    };

    // 获取任务类型名称
    const getTaskTypeName = (type) => {
      const typeMap = {
        'COURSE': '课程',
        'TRAIN': '培训',
        'EXAM': '考试',
        'ASSIGNMENT': '作业',
        'SURVEY': '调研'
      };
      return typeMap[type] || type;
    };

    /** 获取范围回显 */
    const getRange = () => {
      queryRange({
        businessType: 'LEARNING_MAP',
        businessId: mapForm.id,
        functionType: 'visibility'
      }).then(res => {
        console.log(res, '范围配置')

        const tempArray = [];
        // 处理targets字段，将后端的TargetsDTO格式转换为前端期望的List<TargetDTO>格式
        const targets = [];
        const { departmentInfos, roleInfos, userInfos } = res.data;
        departmentInfos.forEach(item => {
          tempArray.push({ id: item.departmentId, type: 'department', name: item.departmentName });
        })
        roleInfos.forEach(item => {
          tempArray.push({ id: item.roleId, type: 'role', name: item.roleName });
        })
        userInfos.forEach(item => {
          tempArray.push({ id: item.userId, type: 'user', name: item.userName });
        })
        // 处理targets字段，将后端的TargetsDTO格式转换为前端期望的List<TargetDTO>格式
        targets.push({
          type: 'department',
          ids: departmentInfos.map(item => item.departmentId)
        })
        targets.push({
          type: 'role',
          ids: roleInfos.map(item => item.roleId)
        })
        targets.push({
          type: 'user',
          ids: userInfos.map(item => item.userId)
        })
        // 更新表单数据
        mapForm.visibility.targets = targets;
        selectedRange.value = tempArray;
      });
    };

    // 显示组织架构选择弹窗 - 可见范围
    const showOrgUserModal = () => {
      orgUserModalVisible.value = true;
    };

    // 处理组织架构选择确认- 可见范围
    const handleOrgUserConfirm = (result) => {
      // 处理选择结果
      const tempArray = [];
      const targets = [];

      // 添加部门目标
      if (result.departments && result.departments.length > 0) {
        targets.push({
          type: 'department',
          ids: result.departments.map(dept => dept.id)
        });

        result.departments.forEach(item => {
          tempArray.push({ id: item.id, type: 'department', name: item.name });
        });
      }

      // 添加角色目标
      if (result.roles && result.roles.length > 0) {
        targets.push({
          type: 'role',
          ids: result.roles.map(role => role.id)
        });

        result.roles.forEach(item => {
          tempArray.push({ id: item.id, type: 'role', name: item.name });
        });
      }

      // 添加用户目标
      if (result.users && result.users.length > 0) {
        targets.push({
          type: 'user',
          ids: result.users.map(user => user.id)
        });

        result.users.forEach(item => {
          tempArray.push({ id: item.id, type: 'user', name: item.name });
        });
      }

      // 更新表单数据
      mapForm.visibility.targets = targets;

      // 更新选择范围
      selectedRange.value = tempArray;

      // 关闭弹窗
      orgUserModalVisible.value = false;
    };

    // 处理组织架构选择取消 - 可见范围
    const handleOrgUserCancel = () => {
      orgUserModalVisible.value = false;
    };

    // 显示协同编辑人员选择弹窗
    const showEditorSelectionModal = () => {
      editorSelectionModalVisible.value = true;
    };

    // 处理协同编辑人员选择确认
    const handleEditorSelectionConfirm = (selectedUsers) => {
      if (selectedUsers && selectedUsers.length > 0) {
        mapForm.collaborators.editors = selectedUsers.map(user => user.id);
      }
      editorSelectionModalVisible.value = false;
    };

    // 处理协同编辑人员选择取消
    const handleEditorSelectionCancel = () => {
      editorSelectionModalVisible.value = false;
    };

    // 是否有已选择的可见范围目标
    const hasSelectedVisibilityTargets = computed(() => {
      return selectedRange.value.length > 0;
    });

    // 获取类型名称
    const getTypeName = (type) => {
      const typeMap = {
        'department': '部门',
        'role': '角色',
        'user': '人员'
      };
      return typeMap[type] || type;
    };

    // 过滤内容选项
    const filterContentOption = (input, option) => {
      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
    };

    // 处理保存
    const handleSave = () => {
      mapFormRef.value.validate().then(() => {

        // 提交表单
        submitForm();
      }).catch(error => {
        console.log('表单验证失败:', error);
      });
    };

    // 提交表单
    const submitForm = async () => {
      loading.value = true;
      try {
        const res = await updateLearningMap(mapForm);
        if (res.code === 200) {
          message.success('学习地图更新成功');
          // 跳转到学习地图列表页
          router.push('/map/list');
        } else {
          message.error(res.message || '更新失败');
        }
      } catch (error) {
        console.error('更新学习地图失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 处理取消
    const handleCancel = () => {
      // 跳转到学习地图列表页
      router.push('/map/list');
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

    // 显示类目管理弹窗
    const showCategoryModal = () => {
      categoryModalVisible.value = true;
    };

    // 处理裁剪成功
    const handleCropSuccess = (imageUrl, data) => {
      console.log('裁剪成功:', imageUrl, data);
      // 封面图片URL已通过v-model自动更新到mapForm.cover
      mapFormRef.value.validateFields(['cover']);
    };

    onMounted(() => {
      // 获取用户信息
      getUserInfo();

      // 获取学习地图详情
      fetchLearningMapDetail();

      // 获取分类列表
      fetchCategories();

      // 获取证书列表
      fetchCertificates();

      // 获取用户列表
      fetchUserList();
    });

    return {
      mapId,
      mapFormRef,
      collapsed,
      selectedKeys,
      openKeys,
      loading,
      userInfo,
      categories,
      certificates,
      certificateLoading,
      userList,
      userOptions,
      contentList,
      contentLoading,
      dateRange,
      stageTimeRanges,
      activeStageKeys,
      orgUserModalVisible,
      editorOrgUserModalVisible,
      userOrgUserModalVisible,
      editorSelectionModalVisible,
      userSelectUserSelectionModalVisible,
      taskModalVisible,
      currentStageIndex,
      taskForm,
      taskColumns,
      mapForm,
      rules,
      categoryModalVisible,
      selectedRange,
      hasSelectedVisibilityTargets,
      hasPermission,
      handleDateRangeChange,
      handleStageTimeChange,
      getStageHeader,
      handleAddStage,
      handleDeleteStage,
      handleAddTask,
      handleTaskModalOk,
      handleTaskModalCancel,
      handleDeleteTask,
      getTaskTypeName,
      showOrgUserModal,
      handleOrgUserConfirm,
      handleOrgUserCancel,
      showEditorSelectionModal,
      handleEditorSelectionConfirm,
      handleEditorSelectionCancel,
      getTypeName,
      filterContentOption,
      handleSave,
      handleCancel,
      handleLogout,
      navigateTo,
      showCategoryModal,
      handleCropSuccess,
      handleContentSearch,
      COLLABORATOR_TYPE,
      VISIBILITY_TYPE
    };
  }
});
</script>

<style scoped>
.learning-map-edit-layout {
  min-height: 100vh;
}

.content {
  position: relative;
  padding: 24px 24px 80px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: auto;
}

.content-header {
  padding-bottom: 12px;
}

.form-container {
  height: calc(100% - 36px);
  overflow-y: auto;
  background-color: #FFFFFF;
  padding: 24px;
  border-radius: 4px;
}

.form-container .form {
  height: 100%;
  overflow-y: auto;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.section-subtitle {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 16px;
}

.upload-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 8px;
}

.empty-stages,
.empty-tasks {
  padding: 24px;
  text-align: center;
}

.stages-container {
  margin-bottom: 24px;
}

.tasks-section {
  margin-top: 24px;
}

.selected-targets {
  margin-top: 16px;
}

.footer-actions {
  text-align: left;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
  position: absolute;
  bottom: 0;
  background-color: #fff;
  width: calc(100% - 48px);
  padding: 24px;
}
</style>
