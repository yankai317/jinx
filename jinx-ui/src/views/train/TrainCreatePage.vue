<template>
  <a-layout class="train-create-layout">
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

        <!-- 表单区域 -->
        <div class="form-container">
          <a-form class="form" ref="trainFormRef" :model="trainForm" :rules="rules"
            :label-col="{ style: { width: '120px' } }" :wrapper-col="{ span: 12 }">
            <!-- <div class="train-type-selection">
              <a-row :gutter="16">
                <a-col :span="6">
                  <div class="train-type-card" :class="{ active: trainForm.type === 'online' }"
                    @click="selectTrainType('online')">
                    <laptop-outlined class="type-icon" />
                    <div class="type-name">线上培训</div>
                    <div class="type-desc">适合远程学习的培训课程</div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="train-type-card" :class="{ active: trainForm.type === 'offline' }"
                    @click="selectTrainType('offline')">
                    <team-outlined class="type-icon" />
                    <div class="type-name">线下培训</div>
                    <div class="type-desc">适合面对面授课的培训</div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="train-type-card" :class="{ active: trainForm.type === 'mixed' }"
                    @click="selectTrainType('mixed')">
                    <interaction-outlined class="type-icon" />
                    <div class="type-name">混合培训</div>
                    <div class="type-desc">线上线下相结合的培训</div>
                  </div>
                </a-col>
                <a-col :span="6">
                  <div class="train-type-card" :class="{ active: trainForm.type === 'series' }"
                    @click="selectTrainType('series')">
                    <schedule-outlined class="type-icon" />
                    <div class="type-name">系列培训</div>
                    <div class="type-desc">多个培训内容组合的系列课程</div>
                  </div>
                </a-col>
              </a-row>
            </div> -->

            <!-- 基本信息 -->
            <div class="section-title">基本信息</div>
            <a-form-item label="培训名称" name="name">
              <a-input v-model:value="trainForm.title" placeholder="请输入培训名称" :maxlength="100" show-count />
            </a-form-item>
            <a-form-item label="学分" name="credit">
              <a-input-number v-model:value="trainForm.credit" :min="1" :max="100" style="width: 100%" />
            </a-form-item>

            <a-form-item label="分类" name="categoryIds">
              <CategoryTreeSelect v-model:value="trainForm.categoryIds" />
            </a-form-item>
            <!-- <a-form-item label="评论权限" name="allowComment">
              <a-radio-group v-model:value="trainForm.allowComment">
                <a-radio :value="true">允许评论</a-radio>
                <a-radio :value="false">禁止评论</a-radio>
              </a-radio-group>
            </a-form-item> -->

            <a-form-item label="关联证书" name="certificateId">
              <a-select v-model:value="trainForm.certificateId" placeholder="请选择关联证书" style="width: 100%" allowClear>
                <a-select-option v-for="certificate in certificates" :key="certificate.id" :value="certificate.id">
                  {{ certificate.name }}
                </a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="培训简介" name="introduction">
              <a-textarea v-model:value="trainForm.introduction" placeholder="请输入培训简介" :rows="4" :maxlength="500"
                show-count />
            </a-form-item>

            <a-form-item label="封面图" name="cover">
              <ImageCropper
                v-model:value="trainForm.cover"
                :width="640"
                :height="360"
                label="选择图片"
                tip="支持JPG、PNG格式，裁剪为 640x360 像素"
                extra="请上传清晰美观的封面图，作为培训的主要视觉元素"
                @crop-success="handleCropSuccess"
              />
            </a-form-item>

            <!-- 培训内容 -->
            <div class="section-title">培训内容</div>
            <a-form-item name="contents" label="课程">
              <!-- <a-tabs v-model:activeKey="contentTabKey">
                <a-tab-pane key="course" tab="添加已有课程"> -->
                  <div class="content-selection">
                    <a-button type="primary" @click="showCourseSelectionModal">
                      <plus-outlined />
                      添加课程
                    </a-button>
                    <a-table v-if="selectedCourses.length > 0" :columns="courseColumns" :data-source="selectedCourses"
                      :pagination="false" size="small" style="margin-top: 16px">
                      <template #bodyCell="{ column, record, index }">
                        <template v-if="column.key === 'required'">
                          <a-checkbox v-model:checked="record.isRequired"></a-checkbox>
                        </template>
                        <template v-else-if="column.key === 'action'">
                          <a-space>
                            <a-button type="link" @click="moveUp(index)" :disabled="index === 0">
                              <arrow-up-outlined />
                            </a-button>
                            <a-button type="link" @click="moveDown(index)"
                              :disabled="index === selectedCourses.length - 1">
                              <arrow-down-outlined />
                            </a-button>
                            <a-button type="link" danger @click="removeCourse(record)">
                              <delete-outlined />
                            </a-button>
                          </a-space>
                        </template>
                      </template>
                    </a-table>
                    <div v-else class="empty-content">
                      <empty-outlined />
                      <p>暂无添加的课程，请点击"添加课程"按钮</p>
                    </div>
                  </div>
                <!-- </a-tab-pane>
                <a-tab-pane key="upload" tab="本地上传文件">
                  <div class="upload-content">
                    <a-upload v-model:file-list="contentFileList" :show-upload-list="true"
                      :before-upload="beforeContentFileUpload" :customRequest="handleContentFileUpload"
                      :multiple="true">
                      <a-button>
                        <upload-outlined />
                        选择文件
                      </a-button>
                    </a-upload>
                    <div class="upload-tip">支持视频、文档等格式，单个文件大小不超过500MB</div><a-table v-if="contentFileList.length > 0"
                      :columns="fileColumns" :data-source="contentFileList" :pagination="false" size="small"
                      style="margin-top: 16px">
                      <template #bodyCell="{ column, record, index }">
                        <template v-if="column.key === 'required'">
                          <a-checkbox v-model:checked="record.isRequired"></a-checkbox>
                        </template>
                        <template v-else-if="column.key === 'action'">
                          <a-space>
                            <a-button type="link" @click="moveUpFile(index)" :disabled="index === 0">
                              <arrow-up-outlined />
                            </a-button>
                            <a-button type="link" @click="moveDownFile(index)"
                              :disabled="index === contentFileList.length - 1">
                              <arrow-down-outlined />
                            </a-button>
                            <a-button type="link" danger @click="removeFile(record)">
                              <delete-outlined /></a-button>
                          </a-space>
                        </template>
                      </template>
                    </a-table>
                  </div>
                </a-tab-pane>
              </a-tabs> -->
            </a-form-item>

            <!-- 可见范围设置 -->
            <div class="section-title">可见范围设置</div>
            <a-form-item name="visibility" label="可见范围">
              <a-radio-group v-model:value="trainForm.visibility.type" label="可见范围"
                @change="trainForm.visibility.targets = []" :options="Object.values(VISIBILITY_TYPE).map(item => ({ label: item.name, value: item.key }))">
              </a-radio-group>
              <div v-if="trainForm.visibility.type === 'PART'" style="margin-top: 16px">
                <a-button type="primary" @click="showOrgUserModal">
                  <team-outlined />
                  从组织架构选择
                </a-button>
                <div v-if="selectedRange.length > 0" class="selected-targets">
                  <a-divider orientation="left">已选择的范围</a-divider>
                  <a-tag v-for="item in selectedRange" :key="`${item.type}-${item.id}`">
                    {{ item.name }} ({{ getTypeName(item.type) }})
                  </a-tag>
                </div>
              </div>
            </a-form-item>

            <!-- 协同设置 -->
            <div class="section-title">协同设置</div>
            <a-form-item label="协同编辑" name="collaborators.editors">
              <a-radio-group v-model:value="trainForm.collaborators.editorType"
                :options="Object.values(COLLABORATOR_TYPE).map(item => ({ label: item.name, value: item.key }))"
                @change="trainForm.collaborators.editors = []">
              </a-radio-group>
              <div v-if="trainForm.collaborators.editorType === 'PART'" style="margin-top: 16px">
                <UserSelect v-model:value="trainForm.collaborators.editors" mode="multiple" placeholder="请选择协同编辑人员"
                  style="width: 80%; margin-right: 16px" query-admin />
                <a-button type="primary" @click="showEditorSelectionModal">
                  <team-outlined />
                  批量选择
                </a-button>
              </div>
            </a-form-item>
            <a-form-item label="支持引用" name="ifIsCitable">
              <a-radio-group v-model:value="trainForm.ifIsCitable">
                <a-radio :value="true">是</a-radio>
                <a-radio :value="false">否</a-radio>
              </a-radio-group>
            </a-form-item>

            <!-- 协同人员选择弹窗 - 协同编辑 -->
            <CollaboratorSelectionModal v-model:visible="editorSelectionModalVisible"
              @confirm="handleEditorSelectionConfirm" @cancel="handleEditorSelectionCancel" />
          </a-form>
        </div>

        <!-- 底部操作按钮区域 -->
        <div class="footer-actions">
          <a-button @click="handleCancel">取消</a-button>
          <a-button style="margin-left: 8px" @click="handleSaveDraft" v-if="hasPermission('train:create')">
            保存草稿
          </a-button>
          <a-button type="primary" style="margin-left: 8px" @click="handlePublish" v-if="hasPermission('train:create')">
            发布培训
          </a-button>
        </div>
      </a-layout-content>
    </a-layout>

    <!-- 组织架构选择弹窗 - 可见范围 -->
    <a-modal v-model:visible="orgUserModalVisible" title="选择可见范围" width="700px" @ok="handleOrgUserOk"
      @cancel="handleOrgUserCancel" :footer="null">
      <org-user-component v-if="orgUserModalVisible" :selection-type="'multiple'" @confirm="handleOrgUserConfirm"
        @cancel="handleOrgUserCancel" :selected-ids="selectedRange" />
    </a-modal>

    <!-- 课程选择弹窗 -->
    <a-modal v-model:visible="courseSelectionModalVisible" title="选择课程" width="800px" @ok="handleCourseSelectionOk"
      @cancel="handleCourseSelectionCancel">
      <div class="train-selection-modal">
        <div class="train-search">
          <a-input-search v-model:value="courseSearchKeyword" placeholder="搜索课程名称" @search="searchCourses"
            style="width: 300px; margin-bottom: 16px;" />
        </div>
        <a-table :columns="courseSelectionColumns" :data-source="courseList" :pagination="{ pageSize: 5 }"
          :row-selection="{
            selectedRowKeys: selectedCourseKeys,
            onChange: handleCourseSelectionChange
          }" :loading="courseLoading" size="small" :row-key="record => record.id" />
      </div>
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
import { createTrain } from '@/api/train';
import { getCategoryList } from '@/api/category';
import { getCertificateList } from '@/api/certificate';
import { getCourseList } from '@/api/course';
import { getUserList } from '@/api/user';
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
  LaptopOutlined,
  TeamOutlined,
  InteractionOutlined,
  ScheduleOutlined,
  UploadOutlined,
  DeleteOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  EmptyOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';
import CollaboratorSelectionModal from '@/components/common/CollaboratorSelectionModal.vue';
import { COLLABORATOR_TYPE, VISIBILITY_TYPE } from '@/utils/constants';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';
import ImageCropper from '@/components/common/ImageCropper.vue';
import UserSelect from '@/components/common/UserSelect.vue';

export default defineComponent({
  name: 'TrainCreatePage',
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
    LaptopOutlined,
    TeamOutlined,
    InteractionOutlined,
    ScheduleOutlined,
    UploadOutlined,
    DeleteOutlined,
    ArrowUpOutlined,
    ArrowDownOutlined,
    EmptyOutlined,
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
    const trainFormRef = ref(null);
    const collapsed = ref(false);
    const selectedKeys = ref(['trainList']);
    const openKeys = ref(['learning']);
    const categoryModalVisible = ref(false);

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


    // 分类列表
    const categories = ref([]);

    // 证书列表
    const certificates = ref([]);

    // 用户列表
    const userList = ref([]);
    const userOptions = computed(() => {
      return userList.value.map(user => ({
        label: `${user.nickname} (${user.employeeNo || '无工号'})`,
        value: user.userId
      }));
    });

    // 内容标签页
    const contentTabKey = ref('course');

    // 文件列表
    const contentFileList = ref([]);

    // 课程选择相关
    const courseSelectionModalVisible = ref(false);
    const courseSearchKeyword = ref('');
    const courseList = ref([]);
    const courseLoading = ref(false);
    const selectedCourseKeys = ref([]);
    const selectedCourses = ref([]);

    // 课程表格列
    const courseColumns = [
      {
        title: '课程名称',
        dataIndex: 'title',
        key: 'title'
      },
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
        customRender: ({ text }) => {
          const typeMap = {
            'article': '文章',
            'video': '视频',
            'document': '文档',
            'series': '系列课'
          };
          return typeMap[text] || text;
        }
      },
      {
        title: '学分',
        dataIndex: 'credit',
        key: 'credit',
        width: 80
      },
      {
        title: '必修',
        key: 'required',
        width: 80
      },
      {
        title: '操作',
        key: 'action',
        width: 150
      }
    ];

    // 课程选择表格列
    const courseSelectionColumns = [
      {
        title: '课程名称',
        dataIndex: 'title',
        key: 'title'
      },
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
        customRender: ({ text }) => {
          const typeMap = {
            'article': '文章',
            'video': '视频',
            'document': '文档',
            'series': '系列课'
          };
          return typeMap[text] || text;
        }
      },
      {
        title: '学分',
        dataIndex: 'credit',
        key: 'credit',
        width: 80
      },
      {
        title: '创建人',
        dataIndex: 'creatorName',
        key: 'creatorName',
        width: 100
      }
    ];

    // 文件表格列
    const fileColumns = [
      {
        title: '文件名',
        dataIndex: 'name',
        key: 'name'
      },
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type'
      },
      {
        title: '大小',
        dataIndex: 'size',
        key: 'size',
        customRender: ({ text }) => {
          return formatFileSize(text);
        }
      },
      {
        title: '必修',
        key: 'required',
        width: 80
      },
      {
        title: '操作',
        key: 'action',
        width: 150
      }
    ];

    // 组织架构选择弹窗
    const orgUserModalVisible = ref(false);
    const editorSelectionModalVisible = ref(false);
    const userSelectionModalVisible = ref(false);

    // 培训表单
    const trainForm = reactive({
      title: '',
      type: 'online', // 默认为线上培训
      cover: '',
      introduction: '',
      credit: 1,
      categoryIds: '',
      allowComment: true,
      certificateId: undefined,
      status: 'draft',
      contents: [],
      visibility: {
        type: 'ALL',
        targets: []
      },
      collaborators: {
        editorType: 'ALL',
        editors: []
      },
      ifIsCitable: true
    });

    // 表单验证规则
    const rules = {
      title: [
        { required: true, message: '请输入培训名称', trigger: 'blur' },
        { max: 100, message: '培训名称最多100个字符', trigger: 'blur' }
      ],
      type: [
        { required: true, message: '请选择培训类型', trigger: 'change' }
      ],
      credit: [
        { required: true, message: '请输入学分', trigger: 'blur' },
        { type: 'number', min: 1, max: 100, message: '学分范围为1-100', trigger: 'blur' }
      ],
      introduction: [
        { max: 500, message: '培训简介最多500个字符', trigger: 'blur' }
      ],
      cover: [
        { required: true, message: '请上传封面图', trigger: 'change' }
      ],
      contents: [
        {
          validator: (rule, value) => {
            if (selectedCourses.value.length === 0 && contentFileList.value.length === 0) {
              return Promise.reject('请添加培训内容');
            }
            return Promise.resolve();
          },
          trigger: 'change'
        }
      ]
    };

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
        const res = await getCertificateList({
          pageNum: 1,
          pageSize: 100
        });
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

    // 选择培训类型
    const selectTrainType = (type) => {
      trainForm.type = type;
    };

    // 内容文件上传前校验
    const beforeContentFileUpload = (file) => {
      const isLt500M = file.size / 1024 / 1024 < 500;
      if (!isLt500M) {
        message.error('文件大小不能超过500MB!');
        return false;
      }

      return true;
    };

    // 处理内容文件上传
    const handleContentFileUpload = ({ file, onSuccess, onError }) => {
      // 创建FormData对象
      const formData = new FormData();
      formData.append('file', file);

      // 调用上传接口
      fetch('/api/file/upload', {
        method: 'POST',
        body: formData,
        headers: {
          'Authorization': `Bearer ${sessionStorage.getItem('token')}`
        }
      })
        .then(response => response.json())
        .then(res => {
          if (res.code === 200 && res.data) {
            // 上传成功
            // 添加到文件列表
            const fileType = getFileTypeText(file.type);
            contentFileList.value.push({
              uid: file.uid,
              name: file.name,
              status: 'done',
              url: res.data.url,
              type: fileType,
              size: file.size,
              isRequired: true,
              sortOrder: contentFileList.value.length + 1
            });

            // 更新培训内容
            updateTrainContents();

            onSuccess(res, file);
          } else {
            // 上传失败
            onError(new Error(res.message || '上传失败'));
          }
        })
        .catch(error => {
          console.error('上传失败:', error);
          onError(error);
        });
    };

    // 获取文件类型文本
    const getFileTypeText = (mimeType) => {
      if (mimeType.startsWith('video/')) {
        return '视频';
      } else if (mimeType === 'application/pdf') {
        return 'PDF';
      } else if (mimeType === 'application/msword' || mimeType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') {
        return 'Word';
      } else if (mimeType === 'application/vnd.ms-powerpoint' || mimeType === 'application/vnd.openxmlformats-officedocument.presentationml.presentation') {
        return 'PPT';
      } else {
        return '文档';
      }
    };

    // 格式化文件大小
    const formatFileSize = (size) => {
      if (size < 1024) {
        return size + ' B';
      } else if (size < 1024 * 1024) {
        return (size / 1024).toFixed(2) + ' KB';
      } else if (size < 1024 * 1024 * 1024) {
        return (size / 1024 / 1024).toFixed(2) + ' MB';
      } else {
        return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB';
      }
    };

    // 显示课程选择弹窗
    const showCourseSelectionModal = () => {
      courseSelectionModalVisible.value = true;
      searchCourses();
    };

    // 搜索课程
    const searchCourses = async () => {
      courseLoading.value = true;
      try {
        const params = {
          pageNum: 1,
          pageSize: 100,
          title: courseSearchKeyword.value,
          status: 'published', // 只显示已发布的课程
          ifIsCitable: true
        };

        const res = await getCourseList(params);
        if (res.code === 200 && res.data) {
          courseList.value = res.data.list || [];

          // 设置已选择的课程
          selectedCourseKeys.value = selectedCourses.value.map(course => course.id);
        }
      } catch (error) {
        console.error('获取课程列表失败:', error);
      } finally {
        courseLoading.value = false;
      }
    };

    // 处理课程选择变更
    const handleCourseSelectionChange = (selectedRowKeys, selectedRows) => {
      selectedCourseKeys.value = selectedRowKeys;
    };

    // 处理课程选择确定
    const handleCourseSelectionOk = () => {
      // 获取选中的课程
      const newSelectedCourses = courseList.value.filter(course => selectedCourseKeys.value.includes(course.id));

      // 添加到已选择的课程列表，避免重复添加
      newSelectedCourses.forEach(course => {
        if (!selectedCourses.value.some(c => c.id === course.id)) {
          selectedCourses.value.push({
            ...course,
            isRequired: true,
            sortOrder: selectedCourses.value.length + 1
          });
        }
      });

      // 更新培训内容
      updateTrainContents();

      // 关闭弹窗
      courseSelectionModalVisible.value = false;
    };

    // 处理课程选择取消
    const handleCourseSelectionCancel = () => {
      courseSelectionModalVisible.value = false;
    };

    // 移除课程
    const removeCourse = (course) => {
      selectedCourses.value = selectedCourses.value.filter(c => c.id !== course.id);

      // 更新排序
      selectedCourses.value.forEach((course, index) => {
        course.sortOrder = index + 1;
      });

      // 更新培训内容
      updateTrainContents();
    };

    // 移除文件
    const removeFile = (file) => {
      contentFileList.value = contentFileList.value.filter(f => f.uid !== file.uid);

      // 更新排序
      contentFileList.value.forEach((file, index) => {
        file.sortOrder = index + 1;
      });

      // 更新培训内容
      updateTrainContents();
    };

    // 上移课程
    const moveUp = (index) => {
      if (index > 0) {
        const temp = selectedCourses.value[index];
        selectedCourses.value[index] = selectedCourses.value[index - 1];
        selectedCourses.value[index - 1] = temp;

        // 更新排序
        selectedCourses.value.forEach((course, idx) => {
          course.sortOrder = idx + 1;
        });

        // 更新培训内容
        updateTrainContents();
      }
    };

    // 下移课程
    const moveDown = (index) => {
      if (index < selectedCourses.value.length - 1) {
        const temp = selectedCourses.value[index];
        selectedCourses.value[index] = selectedCourses.value[index + 1];
        selectedCourses.value[index + 1] = temp;

        // 更新排序
        selectedCourses.value.forEach((course, idx) => {
          course.sortOrder = idx + 1;
        });

        // 更新培训内容
        updateTrainContents();
      }
    };

    // 上移文件
    const moveUpFile = (index) => {
      if (index > 0) {
        const temp = contentFileList.value[index];
        contentFileList.value[index] = contentFileList.value[index - 1];
        contentFileList.value[index - 1] = temp;

        // 更新排序
        contentFileList.value.forEach((file, idx) => {
          file.sortOrder = idx + 1;
        });

        // 更新培训内容
        updateTrainContents();
      }
    };

    // 下移文件
    const moveDownFile = (index) => {
      if (index < contentFileList.value.length - 1) {
        const temp = contentFileList.value[index];
        contentFileList.value[index] = contentFileList.value[index + 1];
        contentFileList.value[index + 1] = temp;

        // 更新排序
        contentFileList.value.forEach((file, idx) => {
          file.sortOrder = idx + 1;
        });

        // 更新培训内容
        updateTrainContents();
      }
    };

    // 更新培训内容
    const updateTrainContents = () => {
      const contents = [];

      // 添加课程内容
      selectedCourses.value.forEach(course => {
        contents.push({
          type: 'COURSE',
          subType: course.type,
          contentId: course.id,
          isRequired: course.isRequired,
          sortOrder: course.sortOrder
        });
      });

      // 添加文件内容
      contentFileList.value.forEach(file => {
        contents.push({
          type: 'COURSE',
          contentId: file.url, // 使用文件URL作为内容ID
          isRequired: file.isRequired,
          sortOrder: file.sortOrder
        });
      });

      trainForm.contents = contents;
    };

    // 显示组织架构选择弹窗 - 可见范围
    const showOrgUserModal = () => {
      orgUserModalVisible.value = true;
    };

    // 选择范围
    const selectedRange = ref([]);

    // 处理组织架构选择确认 - 可见范围
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
      trainForm.visibility.targets = targets;

      // 更新选择范围
      selectedRange.value = tempArray;

      // 关闭弹窗
      orgUserModalVisible.value = false;
    };

    // 处理组织架构选择确定 - 可见范围
    const handleOrgUserOk = () => {
      orgUserModalVisible.value = false;
    };

    // 处理组织架构选择取消 - 可见范围
    const handleOrgUserCancel = () => {
      orgUserModalVisible.value = false;
    };

    // 显示组织架构选择弹窗 - 协同编辑
    const showEditorSelectionModal = () => {
      editorSelectionModalVisible.value = true;
    };

    // 处理协同人员选择确认 - 协同编辑
    const handleEditorSelectionConfirm = (result) => {
      // 确保result是数组并且有效
      if (!result || !Array.isArray(result)) {
        console.warn('无效的协同编辑人员数据', result);
        return;
      }

      // 提取用户ID
      const editorIds = result
        .filter(user => user && user.id) // 过滤无效的用户
        .map(user => user.id);

      // 更新表单数据
      trainForm.collaborators.editors = editorIds;

      // 关闭弹窗
      editorSelectionModalVisible.value = false;
    };

    // 处理组织架构选择取消 - 协同编辑
    const handleEditorSelectionCancel = () => {
      editorSelectionModalVisible.value = false;
    };

    // 获取类型名称
    const getTypeName = (type) => {
      const typeMap = {
        'department': '部门',
        'role': '角色',
        'user': '人员'
      };
      return typeMap[type] || type;
    };

    // 处理保存草稿
    const handleSaveDraft = () => {
      trainFormRef.value.validate().then(() => {
        // 设置状态为草稿
        trainForm.status = 'draft';

        // 设置创建人信息
        trainForm.creatorId = userInfo.value.userId;
        trainForm.creatorName = userInfo.value.nickname;

        // 提交表单
        submitForm();
      }).catch(error => {
        console.log('表单验证失败:', error);
      });
    };

    // 处理发布
    const handlePublish = () => {
      trainFormRef.value.validate().then(() => {
        // 设置状态为已发布
        trainForm.status = 'published';

        // 设置创建人信息
        trainForm.creatorId = userInfo.value.userId;
        trainForm.creatorName = userInfo.value.nickname;

        // 提交表单
        submitForm();
      }).catch(error => {
        console.log('表单验证失败:', error);
      });
    };


    // 提交表单
    const submitForm = async () => {
      try {
        const res = await createTrain(trainForm);
        if (res.code === 200) {
          message.success(trainForm.status === 'published' ? '培训发布成功' : '培训保存成功');
          // 跳转到培训列表页
          router.push('/train/list');
        } else {
          message.error(res.message || '操作失败');
        }
      } catch (error) {
        console.error('创建培训失败:', error);
      }
    };

    // 处理取消
    const handleCancel = () => {
      // 跳转到培训列表页
      router.push('/train/list');
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
      // 封面图片URL已通过v-model自动更新到trainForm.cover
      trainFormRef.value.validateFields(['cover']);
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
      trainFormRef,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      categories,
      certificates,
      userList,
      userOptions,
      contentTabKey,
      contentFileList,
      courseSelectionModalVisible,
      courseSearchKeyword,
      courseList,
      courseLoading,
      selectedCourseKeys,
      selectedCourses,
      courseColumns,
      courseSelectionColumns,
      fileColumns,
      orgUserModalVisible,
      editorSelectionModalVisible,
      categoryModalVisible,
      trainForm,
      rules,
      selectedRange,
      hasPermission,
      selectTrainType,
      beforeContentFileUpload,
      handleContentFileUpload,
      showCourseSelectionModal,
      searchCourses,
      handleCourseSelectionChange,
      handleCourseSelectionOk,
      handleCourseSelectionCancel,
      removeCourse,
      removeFile,
      moveUp,
      moveDown,
      moveUpFile,
      moveDownFile,
      formatFileSize,
      showOrgUserModal,
      handleOrgUserConfirm,
      handleOrgUserOk,
      handleOrgUserCancel,
      showEditorSelectionModal,
      handleEditorSelectionConfirm,
      handleEditorSelectionCancel,
      getTypeName,
      handleSaveDraft,
      handlePublish,
      handleCancel,
      handleLogout,
      navigateTo,
      showCategoryModal,
      fetchCategories,
      handleCropSuccess,
      COLLABORATOR_TYPE,
      VISIBILITY_TYPE
    };
  }
});
</script>

<style scoped>
.train-create-layout {
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

.train-type-selection {
  position: sticky;
  top: 0;
  background-color: #fff;
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  border-bottom: 1px solid #d9d9d9;
  padding: 0 24px;
  z-index: 1;
}

.train-type-card {
  border: 1px solid #d9d9d9;
  border-radius: 4px 4px 0 0;
  padding: 12px 0;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: center;
  margin-bottom: -1px;
  background-color: rgba(5, 5, 5, 0.03);
}

.train-type-card:hover {
  color: #1890ff;
  background-color: transparent;
}

.train-type-card:hover .type-desc {
  color: #1890ff;
}


.train-type-card.active {
  color: #1890ff;
  border-bottom: 1px solid #fff;
  background-color: transparent;
}


.train-type-card.active .type-desc {
  color: #1890ff;
}

.type-icon {
  font-size: 24px;
}

.type-name {
  font-size: 16px;
  font-weight: 500;
}

.type-desc {
  width: 100%;
  ;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.upload-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 8px;
}

.editor-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 8px;
  text-align: center;
}

.url-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 8px;
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
