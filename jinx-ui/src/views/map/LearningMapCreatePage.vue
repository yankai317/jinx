<template>
  <a-layout class="learning-map-create-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

    <a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal" />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <div class="content-header">
          <PageBreadcrumb />
        </div>

        <!-- 表单区域 -->
        <div class="form-container">
          <a-form ref="formRef" :model="formData" class="form" :rules="rules" :label-col="{ style: { width: '120px' } }"
            :wrapper-col="{ span: 12 }">
            <!-- 基本信息 -->
            <div class="section-title">基本信息</div>
            <a-form-item label="地图名称" name="name">
              <a-input v-model:value="formData.name" placeholder="请输入地图名称" />
            </a-form-item>
            <a-form-item label="分类" name="categoryIds">
              <CategoryTreeSelect v-model:value="formData.categoryIds" />
            </a-form-item>
            <a-form-item label="封面图" name="cover">
              <ImageCropper v-model:value="formData.cover" :width="640" :height="360" label="选择图片"
                tip="支持JPG、PNG格式，裁剪为 640x360 像素" extra="请上传清晰美观的封面图，作为学习地图的主要视觉元素" @crop-success="handleCropSuccess" />
            </a-form-item>


            <a-form-item label="地图简介" name="introduction">
              <a-textarea v-model:value="formData.introduction" placeholder="请输入地图简介" :rows="4" />
            </a-form-item>

            <a-form-item label="学分规则" name="creditRule">
              <a-radio-group v-model:value="formData.creditRule">
                <a-radio :value="0">整体发放</a-radio>
                <a-radio :value="1">按阶段发放</a-radio>
              </a-radio-group>
            </a-form-item>
            <template v-if="formData.creditRule === 0">
              <a-form-item label="必修学分" name="requiredCredit">
                <a-input-number v-model:value="formData.requiredCredit" :min="0" :precision="1" style="width: 100%" />
              </a-form-item>
              <a-form-item label="选修学分" name="electiveCredit"><a-input-number v-model:value="formData.electiveCredit"
                  :min="0" :precision="1" style="width: 100%" />
              </a-form-item>
            </template>
            <a-form-item label="证书规则" name="certificateRule"><a-radio-group v-model:value="formData.certificateRule">
                <a-radio :value="0">不发放</a-radio>
                <a-radio :value="1">整体发放</a-radio><a-radio :value="2">按阶段发放</a-radio></a-radio-group>
            </a-form-item>
            <template v-if="formData.certificateRule === 1">
              <a-form-item label="证书" name="certificateId"><a-select v-model:value="formData.certificateId"
                  placeholder="请选择证书" style="width: 100%">
                  <a-select-option v-for="certificate in certificates" :key="certificate.id" :value="certificate.id">
                    {{ certificate.name }}
                  </a-select-option></a-select></a-form-item>

            </template>


            <!-- 学习规则 -->
            <div class="section-title">学习规则</div>
            <a-form-item label="开放时间" name="openTime">
              <a-range-picker v-model:value="dateRange" style="width: 100%" @change="handleDateRangeChange" show-time
                format="YYYY-MM-DD HH:mm:ss" />
            </a-form-item>
            <a-form-item label="解锁方式" name="unlockMode">
              <a-radio-group v-model:value="formData.unlockMode"><a-radio :value="0">按阶段和任务</a-radio>
                <a-radio :value="1">按阶段</a-radio><a-radio :value="2">自由模式</a-radio>
              </a-radio-group></a-form-item>



            <!-- <a-form-item label="主题皮肤" name="theme">
              <a-radio-group v-model:value="formData.theme" button-style="solid"><a-radio-button
                  value="business">商务简约</a-radio-button><a-radio-button value="tech">动感科技</a-radio-button>
                <a-radio-button value="farm">农场时光</a-radio-button><a-radio-button
                  value="chinese">中国元素</a-radio-button><a-radio-button
                  value="list">列表模式</a-radio-button></a-radio-group>
            </a-form-item> -->
            <a-form-item label="钉钉群" name="dingtalkGroup">
              <a-radio-group v-model:value="formData.dingtalkGroup"><a-radio :value="0">不创建</a-radio>
                <a-radio :value="1">创建钉钉群</a-radio>
              </a-radio-group>
            </a-form-item>

            <!-- 阶段管理 -->

            <!-- 阶段管理 -->
            <div class="section-title">
              阶段管理
              <a-button type="primary" size="small" style="margin-left: 16px;" @click="addStage">
                <plus-outlined />添加阶段
              </a-button>
            </div>

            <div class="stage-list">
              <div v-if="formData.stages.length === 0" class="empty-stage">
                <a-empty description="暂无阶段，请添加阶段" />
              </div>
              <div v-else>
                <a-collapse v-model:activeKey="activeStageKeys"><a-collapse-panel
                    v-for="(stage, stageIndex) in formData.stages" :key="stageIndex"
                    :header="stage.name || `阶段${stageIndex + 1}`">
                    <template #extra>
                      <a-space>
                        <a-button type="text" @click.stop="moveStage(stageIndex, -1)" :disabled="stageIndex === 0">
                          <arrow-up-outlined /></a-button><a-button type="text" @click.stop="moveStage(stageIndex, 1)"
                          :disabled="stageIndex === formData.stages.length - 1">
                          <arrow-down-outlined /></a-button><a-button type="text" danger
                          @click.stop="removeStage(stageIndex)">
                          <delete-outlined /></a-button></a-space>
                    </template>

                    <!-- 阶段表单 -->
                    <a-form>
                      <a-row :gutter="16">
                        <a-col :span="12">
                          <a-form-item label="阶段名称" required>
                            <a-input v-model:value="stage.name" placeholder="请输入阶段名称" />
                          </a-form-item></a-col><a-col :span="12"><a-form-item label="开放类型" required><a-radio-group
                              v-model:value="stage.openType"><a-radio :value="0">不设置</a-radio>
                              <a-radio :value="1">固定时间</a-radio>
                              <a-radio :value="2">学习期限</a-radio>
                            </a-radio-group></a-form-item>
                        </a-col>
                      </a-row><a-row :gutter="16" v-if="stage.openType === 1"><a-col :span="24">
                          <a-form-item label="开放时间" required>
                            <a-range-picker v-model:value="stage.dateRange" style="width: 100%"
                              @change="(dates) => handleStageTimeChange(stageIndex, dates)" show-time
                              format="YYYY-MM-DD HH:mm:ss" />
                          </a-form-item></a-col></a-row><a-row :gutter="16" v-if="stage.openType === 2"><a-col
                          :span="12">
                          <a-form-item label="学习期限(天)" required><a-input-number v-model:value="stage.durationDays"
                              :min="1" style="width: 100%" />
                          </a-form-item></a-col></a-row>

                      <a-row :gutter="16" v-if="formData.creditRule === 1">
                        <a-col :span="12">
                          <a-form-item label="阶段学分" required><a-input-number v-model:value="stage.credit" :min="0"
                              :precision="1" style="width: 100%" /></a-form-item>
                        </a-col>
                      </a-row>

                      <a-row :gutter="16" v-if="formData.certificateRule === 2">
                        <a-form-item label="阶段证书" required><a-select v-model:value="stage.certificateId"
                            placeholder="请选择证书" style="width: 100%">
                            <a-select-option v-for="certificate in certificates" :key="certificate.id"
                              :value="certificate.id">
                              {{ certificate.name }}
                            </a-select-option></a-select></a-form-item>

                      </a-row>

                      <!-- 任务管理 -->
                      <div class="task-management">
                        <div class="task-header">
                          <h4>任务管理</h4>
                          <a-button type="primary" size="small" @click="showAddTaskModal(stageIndex)">
                            <plus-outlined />添加任务</a-button>
                        </div>

                        <div v-if="stage.tasks && stage.tasks.length > 0" class="task-list">
                          <a-table :dataSource="stage.tasks" :columns="taskColumns" :pagination="false" size="small">
                            <template #bodyCell="{ column, record, index }">
                              <template v-if="column.key === 'type'">
                                <a-tag :color="getTaskTypeColor(record.type)">
                                  {{ getTaskTypeName(record.type) }}</a-tag>
                              </template>
                              <template v-if="column.key === 'isRequired'">
                                <a-switch v-model:checked="record.isRequired" :checkedValue="true"
                                  :unCheckedValue="false" size="small" /></template>
                              <template v-if="column.key === 'action'">
                                <a-space>
                                  <a-button type="text" size="small" @click="moveTask(stageIndex, index, -1)"
                                    :disabled="index === 0">
                                    <arrow-up-outlined /></a-button>
                                  <a-button type="text" size="small" @click="moveTask(stageIndex, index, 1)"
                                    :disabled="index === stage.tasks.length - 1">
                                    <arrow-down-outlined /></a-button>
                                  <a-button type="text" dangersize="small" @click="removeTask(stageIndex, index)">
                                    <delete-outlined />
                                  </a-button></a-space>
                              </template>
                            </template></a-table>
                        </div>
                        <div v-else class="empty-task"><a-empty description="暂无任务，请添加任务" />
                        </div>
                      </div>
                    </a-form></a-collapse-panel>
                </a-collapse>
              </div>
            </div>

            <!-- 可见范围设置 -->
            <div class="section-title">可见范围设置</div>
            <a-form-item name="visibility.type" label="可见范围">
              <a-radio-group v-model:value="formData.visibility.type"
                :options="Object.values(VISIBILITY_TYPE).map(item => ({ label: item.name, value: item.key }))"
                @change="formData.visibility.targets = []">
              </a-radio-group>
            </a-form-item>

            <div v-if="formData.visibility.type === 'PART'" class="visibility-part">
              <a-button type="primary" @click="showOrgUserModal('visibility')">
                <team-outlined />
                从组织架构选择
              </a-button>
              <div v-if="selectedTargets.length > 0" class="selected-targets">
                <a-divider orientation="left">已选择的范围</a-divider>
                <a-tag v-for="target in selectedTargets" :key="`${target.type}-${target.id}`">
                  {{ target.name }} ({{ getTargetTypeName(target.type) }})
                </a-tag>
              </div>
            </div>


            <!-- 协同管理 -->
            <div class="form-section">
              <div class="section-title">协同设置</div>
              <a-form-item label="协同编辑" name="collaborationEditType">
                <a-radio-group v-model:value="formData.collaborators.editorType"
                  :options="Object.values(COLLABORATOR_TYPE).map(item => ({ label: item.name, value: item.key }))"
                  @change="formData.collaborators.editors = []">
                </a-radio-group>
                <div v-if="formData.collaborators.editorType === 'PART'" style="margin-top: 16px">
                  <UserSelect v-model:value="formData.collaborators.editors" mode="multiple" placeholder="请选择协同编辑人员"
                    style="width: 80%; margin-right: 16px" query-admin />
                  <a-button type="primary" @click="showEditorSelectionModal">
                    <template #icon><team-outlined /></template>
                    批量选择
                  </a-button>
                </div>
              </a-form-item>
            </div>

            <!-- 协同人员选择弹窗 - 协同编辑 -->
            <CollaboratorSelectionModal v-model:visible="editorSelectionModalVisible"
              @confirm="handleEditorSelectionConfirm" @cancel="handleEditorSelectionCancel" />

          </a-form>
        </div>
        <!-- 底部操作按钮区域 -->
        <div class="footer-actions">
          <a-button @click="handleCancel">取消</a-button>
          <a-button type="primary" @click="handleSubmit" :loading="submitting">保存</a-button>
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 添加任务弹窗 -->
    <a-modal v-model:visible="taskModalVisible" title="添加任务" @ok="handleAddTask" @cancel="taskModalVisible = false"
      :maskClosable="false"><a-form :model="taskForm" layout="vertical">
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
            placeholder="请输入课程名称搜索" style="width: 100%" :loading="contentLoading"
            @search="handleContentSearch">
          </a-select>
        </a-form-item>

        <a-form-item label="是否必修"><a-switch v-model:checked="taskForm.isRequired" :checkedValue="true"
            :unCheckedValue="false" /></a-form-item>
      </a-form>
    </a-modal>

    <!-- 组织架构选择弹窗 -->
    <a-modal v-model:visible="orgUserModalVisible" title="选择范围" width="800px" :footer="null" :maskClosable="false"
      destroyOnClose>
      <OrgUserComponent v-if="orgUserModalVisible" :selection-type="'multiple'" :selected-ids="selectedTargets"
        :on-confirm="handleOrgUserConfirm" :on-cancel="() => orgUserModalVisible = false" />
    </a-modal>
  </a-layout>
</template>

<script>
import { ref, reactive, computed, onMounted, defineComponent, watch } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  createLearningMap
} from '@/api/learningMap';
import { getCategoryList } from '@/api/category';
import { getCertificateList } from '@/api/certificate';
import { getCourseList } from '@/api/course';
import { getTrainList } from '@/api/train';
import { hasPermission } from '@/utils/permission';
import OrgUserComponent from '@/components/common/OrgUserComponent.vue';
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
  PlusOutlined,
  DeleteOutlined,
  EditOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  TeamOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';
import CollaboratorSelectionModal from '@/components/common/CollaboratorSelectionModal.vue';
import { getUserList } from '@/api/user';
import { COLLABORATOR_TYPE, VISIBILITY_TYPE } from '@/utils/constants';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';
import ImageCropper from '@/components/common/ImageCropper.vue';
import UserSelect from '@/components/common/UserSelect.vue';

export default defineComponent({
  name: 'LearningMapCreatePage',
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
    EditOutlined,
    ArrowUpOutlined,
    ArrowDownOutlined,
    TeamOutlined,
    OrgUserComponent,
    PageBreadcrumb,
    CollaboratorSelectionModal,
    CategoryTreeSelect,
    ImageCropper,
    UserSelect
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const formRef = ref(null);
    const collapsed = ref(false);
    const selectedKeys = ref(['mapList']);
    const openKeys = ref(['learning']);
    const categoryModalVisible = ref(false);
    const submitting = ref(false);

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

    // 用户数据
    const userList = ref([]);
    const userOptions = computed(() => {
      return userList.value.map(user => ({
        label: `${user.nickname} (${user.employeeNo || '无工号'})`,
        value: user.userId
      }));
    });

    const categories = ref([]);

    // 证书列表
    const certificates = ref([]);

    // 日期范围
    const dateRange = ref([]);

    // 表单数据
    const formData = reactive({
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
      name: [{ required: true, message: '请输入地图名称', trigger: 'blur' }],
      cover: [
        { required: true, message: '请上传封面图', trigger: 'change' }
      ],
      categoryIds: [{ required: true, message: '请选择分类', trigger: 'change' }],
      certificateId: [{
        required: true,
        message: '请选择证书',
        trigger: 'change',
        validator: (rule, value) => {
          if (formData.certificateRule === 1 && !value) {
            return Promise.reject(rule.message);
          }
          return Promise.resolve();
        }
      }]
    };

    //阶段展开的key
    const activeStageKeys = ref([]);

    // 任务表格列
    const taskColumns = [
      {
        title: '任务类型',
        dataIndex: 'type',
        key: 'type',
        width: 100
      },
      {
        title: '任务名称',
        dataIndex: 'title',
        key: 'title',
        ellipsis: true
      },
      {
        title: '必修',
        dataIndex: 'isRequired',
        key: 'isRequired',
        width: 80
      },
      {
        title: '操作',
        key: 'action',
        width: 120
      }
    ];

    // 任务弹窗
    const taskModalVisible = ref(false);
    const currentStageIndex = ref(-1);
    const taskForm = reactive({
      type: 'COURSE',
      contentId: null,
      isRequired: true,
      name: '',
      sortOrder: 0,
      subType: ''
    });

    // 内容列表
    const contentList = ref([]);
    const contentLoading = ref(false);
    // 组织架构选择弹窗
    const orgUserModalVisible = ref(false);
    const currentSelectType = ref('');
    // 已选择的目标
    const selectedTargets = ref([]);
    const selectedEditors = ref([]);
    const selectedUsers = ref([]);

    // 协同编辑弹窗
    const editorSelectionModalVisible = ref(false);
    const userSelectionModalVisible = ref(false);

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
      try {
        const res = await getCertificateList({ pageNum: 1, pageSize: 100 });
        if (res.code === 200 && res.data) {
          certificates.value = res.data.list || [];
        }
      } catch (error) {
        console.error('获取证书列表失败:', error);
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

    // 处理日期范围变更
    const handleDateRangeChange = (dates) => {
      if (dates && dates.length === 2) {
        formData.startTime = dates[0].format('YYYY-MM-DD HH:mm:ss');
        formData.endTime = dates[1].format('YYYY-MM-DD HH:mm:ss');
      } else {
        formData.startTime = '';
        formData.endTime = '';
      }
    };

    // 处理阶段时间变更
    const handleStageTimeChange = (stageIndex, dates) => {
      if (dates && dates.length === 2) {
        formData.stages[stageIndex].startTime = dates[0].format('YYYY-MM-DD HH:mm:ss');
        formData.stages[stageIndex].endTime = dates[1].format('YYYY-MM-DD HH:mm:ss');
      } else {
        formData.stages[stageIndex].startTime = '';
        formData.stages[stageIndex].endTime = '';
      }
    };

    // 添加阶段
    const addStage = () => {
      const newStage = {
        name: `阶段${formData.stages.length + 1}`,
        stageOrder: formData.stages.length,
        openType: 0,
        startTime: '',
        endTime: '',
        durationDays: 7,
        credit: 0,
        certificateId: null,
        dateRange: [],
        tasks: []
      };
      formData.stages.push(newStage);
      activeStageKeys.value.push(formData.stages.length - 1);
    };// 移动阶段
    const moveStage = (index, direction) => {
      const newIndex = index + direction;
      if (newIndex < 0 || newIndex >= formData.stages.length) {
        return;
      }
      const temp = formData.stages[index];
      formData.stages[index] = formData.stages[newIndex];
      formData.stages[newIndex] = temp;
      // 更新顺序
      formData.stages.forEach((stage, idx) => {
        stage.stageOrder = idx;
      });
      // 更新展开的key
      const activeKeys = [...activeStageKeys.value];
      const indexOfCurrent = activeKeys.indexOf(index);
      const indexOfTarget = activeKeys.indexOf(newIndex);

      if (indexOfCurrent !== -1) {
        activeKeys[indexOfCurrent] = newIndex;
      }
      if (indexOfTarget !== -1) {
        activeKeys[indexOfTarget] = index;
      }
      activeStageKeys.value = activeKeys;
    };

    // 删除阶段
    const removeStage = (index) => {
      formData.stages.splice(index, 1);
      // 更新顺序
      formData.stages.forEach((stage, idx) => {
        stage.stageOrder = idx;
      });// 更新展开的key
      const activeKeys = activeStageKeys.value.filter(key => key !== index).map(key => key > index ? key - 1 : key);
      activeStageKeys.value = activeKeys;
    };

    // 显示添加任务弹窗
    const showAddTaskModal = (stageIndex) => {
      currentStageIndex.value = stageIndex;
      taskForm.type = 'COURSE';
      taskForm.contentId = null;
      taskForm.isRequired = true;
      taskForm.name = '';
      taskForm.sortOrder = formData.stages[stageIndex].tasks.length;
      taskModalVisible.value = true;
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

    // 处理添加任务
    const handleAddTask = () => {
      if (!taskForm.contentId) {
        message.error('请选择内容');
        return;
      }// 查找选中的内容名称
      const selectedContent = contentList.value.find(item => item.value === taskForm.contentId);

      console.log(taskForm, 'taskForm', contentList.value, selectedContent)
      // 设置 subType
      let subType = '';
      if (taskForm.type === 'COURSE' && selectedContent) {
        subType = selectedContent.type || '';
      } else if (taskForm.type === 'TRAIN') {
        subType = 'TRAIN';
      }

      const newTask = {
        type: taskForm.type,
        contentId: taskForm.contentId,
        isRequired: taskForm.isRequired,
        title: selectedContent.label,
        sortOrder: taskForm.sortOrder,
        subType: subType
      };

      formData.stages[currentStageIndex.value].tasks.push(newTask);

      fetchContentList('');
      taskModalVisible.value = false;
    };

    // 移动任务
    const moveTask = (stageIndex, taskIndex, direction) => {
      const tasks = formData.stages[stageIndex].tasks;
      const newIndex = taskIndex + direction; if (newIndex < 0 || newIndex >= tasks.length) {
        return;
      } const temp = tasks[taskIndex];
      tasks[taskIndex] = tasks[newIndex];
      tasks[newIndex] = temp;

      // 更新顺序
      tasks.forEach((task, idx) => {
        task.sortOrder = idx;
      });
    };

    // 删除任务
    const removeTask = (stageIndex, taskIndex) => {
      formData.stages[stageIndex].tasks.splice(taskIndex, 1);

      // 更新顺序
      formData.stages[stageIndex].tasks.forEach((task, idx) => {
        task.sortOrder = idx;
      });
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
    };// 获取任务类型颜色
    const getTaskTypeColor = (type) => {
      const colorMap = {
        'COURSE': 'blue',
        'TRAIN': 'purple',
        'EXAM': 'red',
        'ASSIGNMENT': 'green',
        'SURVEY': 'orange'
      };
      return colorMap[type] || 'default';
    };

    // 显示组织架构选择弹窗
    const showOrgUserModal = (type) => {
      currentSelectType.value = type;
      orgUserModalVisible.value = true;
    };

    // 处理组织架构选择确认
    const handleOrgUserConfirm = (result) => {
      if (currentSelectType.value === 'visibility') {
        const tempArray = [];
        const targets = [];

        // 处理部门
        if (result.departments && result.departments.length > 0) {
          targets.push({
            type: 'department',
            ids: result.departments.map(dept => dept.id)
          });

          result.departments.forEach(item => {
            tempArray.push({ id: item.id, type: 'department', name: item.name });
          });
        }

        // 处理角色
        if (result.roles && result.roles.length > 0) {
          targets.push({
            type: 'role',
            ids: result.roles.map(role => role.id)
          });

          result.roles.forEach(item => {
            tempArray.push({ id: item.id, type: 'role', name: item.name });
          });
        }

        // 处理用户
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
        formData.visibility.targets = targets;

        // 更新选择范围
        selectedTargets.value = tempArray;
      } else if (currentSelectType.value === 'editors') {
        selectedEditors.value = result.users.map(user => ({
          id: user.id,
          name: user.name
        }));

        // 更新表单数据
        formData.collaborators.editors = selectedEditors.value.map(editor => editor.id);
      } else if (currentSelectType.value === 'users') {
        selectedUsers.value = result.users.map(user => ({
          id: user.id,
          name: user.name
        }));
      }

      orgUserModalVisible.value = false;
    };


    // 移除编辑者
    const removeEditor = (index) => {
      selectedEditors.value.splice(index, 1);

      // 更新表单数据
      formData.collaborators.editors = selectedEditors.value.map(editor => editor.id);
    };

    // 获取目标类型名称
    const getTargetTypeName = (type) => {
      const typeMap = {
        'department': '部门',
        'role': '角色',
        'user': '人员'
      };
      return typeMap[type] || type;
    };

    // 处理提交
    const handleSubmit = async () => {
      try {
        await formRef.value.validate();

        submitting.value = true;


        // 发送请求
        const res = await createLearningMap(formData);

        if (res.code === 200) {
          message.success('创建学习地图成功');
          router.push('/map/list');
        } else {
          message.error(res.message || '创建学习地图失败');
        }
      } catch (error) {
        console.error('创建学习地图失败:', error);
      } finally {
        submitting.value = false;
      }
    };

    // 处理取消
    const handleCancel = () => {
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

    // 显示协同编辑人员选择弹窗
    const showEditorSelectionModal = () => {
      editorSelectionModalVisible.value = true;
    };

    // 处理协同编辑人员选择确认
    const handleEditorSelectionConfirm = (selectedUsers) => {
      if (selectedUsers && selectedUsers.length > 0) {
        // 更新选中的编辑者
        selectedEditors.value = selectedUsers.map(user => ({
          id: user.id,
          name: user.name
        }));
        // 更新表单数据
        formData.collaborators.editors = selectedUsers.map(user => user.id);
      }
      editorSelectionModalVisible.value = false;
    };

    // 处理协同编辑人员选择取消
    const handleEditorSelectionCancel = () => {
      editorSelectionModalVisible.value = false;
    };

    // 处理裁剪成功
    const handleCropSuccess = (imageUrl, data) => {
      console.log('裁剪成功:', imageUrl, data);
      // 封面图片URL已通过v-model自动更新到formData.cover
      formRef.value.validateFields(['cover']);
    };

    onMounted(() => {
      // 获取用户信息
      getUserInfo();
      // 获取分类列表
      fetchCategories();

      // 获取证书列表
      fetchCertificates();
      // 获取用户列表
      fetchUserList();
    });

    return {
      formRef,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      userList,
      userOptions,
      categories,
      certificates,
      dateRange,
      formData,
      rules,
      activeStageKeys,
      taskColumns,
      taskModalVisible,
      currentStageIndex,
      taskForm,
      contentList,
      contentLoading,
      orgUserModalVisible,
      currentSelectType,
      selectedTargets,
      selectedEditors,
      selectedUsers,
      categoryModalVisible,
      submitting,
      hasPermission,
      handleDateRangeChange,
      handleStageTimeChange,
      addStage,
      moveStage,
      removeStage,
      showAddTaskModal,
      fetchContentList,
      handleContentSearch,
      handleAddTask,
      moveTask,
      removeTask,
      getTaskTypeName,
      getTaskTypeColor,
      showOrgUserModal,
      handleOrgUserConfirm,
      removeEditor,
      getTargetTypeName,
      handleSubmit,
      handleCancel,
      handleLogout,
      navigateTo,
      showCategoryModal,
      editorSelectionModalVisible,
      showEditorSelectionModal,
      handleEditorSelectionConfirm,
      handleEditorSelectionCancel,
      handleCropSuccess,
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

.task-management {
  margin-top: 16px;
  border-top: 1px dashed #e8e8e8;
  padding-top: 16px;
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
