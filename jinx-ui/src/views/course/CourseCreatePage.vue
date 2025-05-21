<template>
  <a-layout class="course-create-layout">
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
          <a-form ref="courseFormRef" :key="courseForm.type" class="form" :model="courseForm" :rules="rules"
            :label-col="{ style: { width: '120px' } }" :wrapper-col="{ span: 12 }">
            <!-- 课程类型选择 -->
            <div class="course-type-selection">
              <div class="course-type-card" :class="{ active: courseForm.type === 'article' }"
                @click="selectCourseType('article')">
                <file-text-outlined class="type-icon" />
                <div class="type-name">文章</div>
                <div class="type-desc">适合文字内容为主的课程</div>
              </div>
              <div class="course-type-card" :class="{ active: courseForm.type === 'video' }"
                @click="selectCourseType('video')"><video-camera-outlined class="type-icon" />
                <div class="type-name">视频</div>
                <div class="type-desc">适合单个视频内容的课程</div>
              </div>
              <div class="course-type-card" :class="{ active: courseForm.type === 'document' }"
                @click="selectCourseType('document')"><file-pdf-outlined class="type-icon" />
                <div class="type-name">文档</div>
                <div class="type-desc">适合PDF、Word等文档类课程</div>
              </div>
              <div class="course-type-card" :class="{ active: courseForm.type === 'series' }"
                @click="selectCourseType('series')">
                <folder-outlined class="type-icon" />
                <div class="type-name">系列课</div>
                <div class="type-desc">适合多个视频或文档组合的课程</div>
              </div>
            </div>

            <!-- 基本信息 -->
            <div class="section-title">基本信息</div>
            <a-form-item label="课程名称" name="title"><a-input v-model:value="courseForm.title" placeholder="请输入课程名称"
                :maxlength="100" show-count /></a-form-item>
            <a-form-item label="学分" name="credit">
              <a-input-number v-model:value="courseForm.credit" :min="1" :max="100" style="width: 100%" />
            </a-form-item>

            <a-form-item label="分类" name="categoryIds">
              <CategoryTreeSelect v-model:value="courseForm.categoryIds" />
            </a-form-item>
            <!--            <a-form-item label="评论权限" name="allowComments">-->
            <!--              <a-radio-group v-model:value="courseForm.allowComments"><a-radio :value="true">允许评论</a-radio>-->
            <!--                <a-radio :value="false">禁止评论</a-radio>-->
            <!--              </a-radio-group>-->
            <!--            </a-form-item>-->

            <template  v-if="courseForm.type === 'video' || courseForm.type === 'series'">
                <a-form-item label="讲师类型" name="instructorType">
                  <a-radio-group v-model:value="courseForm.instructorType" @change="handleInstructorTypeChange">
                    <a-radio :value="1">讲师</a-radio>
                    <a-radio :value="2">嘉宾</a-radio>
                  </a-radio-group>
                </a-form-item>
                <a-form-item label="讲师" name="instructorId" v-if="courseForm.instructorType === 1">
                  <UserSelect v-model:value="courseForm.instructorId" />
                </a-form-item>
                <a-form-item label="嘉宾" name="guestName" v-if="courseForm.instructorType === 2">
                  <a-input v-model:value="courseForm.guestName" placeholder="请输入嘉宾名称" />
                </a-form-item>
            </template>

            <a-form-item label="课程简介" name="description">
              <a-textarea v-model:value="courseForm.description" placeholder="请输入课程简介" :rows="4" :maxlength="500"
                show-count />
            </a-form-item>

            <a-form-item label="封面图" name="coverImage">
              <ImageCropper
                v-model:value="courseForm.coverImage"
                :width="640"
                :height="360"
                label="选择图片"
                tip="支持JPG、PNG格式，裁剪为 640x360 像素"
                extra="请上传清晰美观的封面图，作为课程展示的主要视觉元素"
                @crop-success="handleCropSuccess"
              />
            </a-form-item>

            <!-- 课程内容 -->
            <div class="section-title">课程内容</div>

            <!-- 文章类型内容 -->
            <template v-if="courseForm.type === 'article'">
              <a-form-item label="文章内容" name="appendixPath">
              <!-- <a-tabs v-model:activeKey="articleTabKey">
                      <a-tab-pane key="editor" tab="富文本编辑">
                        <div class="rich-editor">
                          <a-textarea v-model:value="courseForm.article" placeholder="请输入文章内容" :rows="15" />
                          &lt;!&ndash; 这里可以替换为真正的富文本编辑器组件 &ndash;&gt;
                          <div class="editor-tip">注：此处应为富文本编辑器，当前为简化版</div>
                        </div>
                      </a-tab-pane>
                  <a-tab-pane key="url" tab="URL链接"> -->
                <a-input v-model:value="courseForm.appendixPath" placeholder="请输入文章URL链接" />
                <div class="url-tip">支持公网可访问的文章链接</div>
                <!-- </a-tab-pane>
                </a-tabs> -->
              </a-form-item>
            </template>

            <!-- 视频类型内容 -->
            <template v-if="courseForm.type === 'video'">
              <a-form-item label="上传视频" name="appendixPath">
                <a-upload v-model:file-list="videoFileList" :show-upload-list="true" :before-upload="beforeVideoUpload"
                  :customRequest="handleVideoUpload" :maxCount="1">
                  <a-button>
                    <upload-outlined />
                    选择视频文件
                  </a-button>
                </a-upload>
                <div class="upload-tip">支持格式：MP4、AVI、MOV等，大小限制：8GB</div>
              </a-form-item>
            </template>

            <!-- 文档类型内容 -->
            <template v-if="courseForm.type === 'document'">
              <a-form-item label="上传文档" name="appendixPath">
                <a-upload v-model:file-list="documentFileList" :show-upload-list="true"
                  :before-upload="beforeDocumentUpload" :customRequest="handleDocumentUpload" :maxCount="1">
                  <a-button>
                    <upload-outlined />
                    选择文档文件</a-button>
                </a-upload>
                <div class="upload-tip">支持格式：PDF、DOC、DOCX、PPT、PPTX等，大小限制：300MB</div>
              </a-form-item>
            </template>

            <!-- 系列课类型内容 -->
            <template v-if="courseForm.type === 'series'">
              <a-form-item label="上传课程文件" name="appendixPath">
                <a-upload v-model:file-list="seriesFileList" :show-upload-list="true"
                  :before-upload="beforeSeriesFileUpload" :customRequest="handleSeriesFileUpload"
                  :multiple="true"><a-button><upload-outlined />
                    选择文件
                  </a-button>
                </a-upload>
                <div class="upload-tip">支持视频和文档格式，最多150个文件</div>

                <a-table v-if="seriesFileList.length > 0" :columns="seriesFileColumns" :data-source="seriesFileList"
                  :pagination="false" size="small" style="margin-top: 16px">
                  <template #bodyCell="{ column, record }">
                    <template v-if="column.key === 'type'">
                      <span>{{ getFileTypeText(record.type) }}</span>
                    </template>
                    <template v-if="column.key === 'action'"><a-button type="link" danger
                        @click="removeSeriesFile(record)">
                        <delete-outlined /></a-button>
                    </template>
                  </template>
                </a-table>
              </a-form-item>
            </template>

            <!-- 可见范围设置 -->
            <div class="section-title">可见范围设置</div>
            <a-form-item name="visibility" label="可见范围">
              <a-radio-group v-model:value="courseForm.visibility.type"
                :options="Object.values(VISIBILITY_TYPE).map(item => ({ label: item.name, value: item.key }))"
                @change="courseForm.visibility.targets = []">
              </a-radio-group>
              <div v-if="courseForm.visibility.type === 'PART'" style="margin-top: 16px">
                <a-button type="primary" @click="showOrgUserModal">
                  <team-outlined />
                  从组织架构选择
                </a-button>
                <div v-if="selectedRange.length > 0" class="selected-targets"><a-divider
                    orientation="left">已选择的范围</a-divider><a-tag v-for="item in selectedRange"
                    :key="`${item.type}-${item.id}`">
                    {{ item.name }} ({{ getTypeName(item.type) }})</a-tag>
                </div>
              </div>
            </a-form-item>

            <!-- 协同设置 -->
            <div class="section-title">协同设置</div>

            <!-- 协同编辑 -->
            <a-form-item label="协同编辑" name="collaboratorEditType">
              <a-radio-group v-model:value="courseForm.collaborators.editorType"
                :options="Object.values(COLLABORATOR_TYPE).map(item => ({ label: item.name, value: item.key }))"
                @change="courseForm.collaborators.editors = []">
              </a-radio-group>
            </a-form-item>

            <a-form-item v-if="courseForm.collaborators.editorType === 'PART'" label=" " :colon="false"
              name="collaborators.editors">
              <UserSelect v-model:value="courseForm.collaborators.editors" query-admin mode="multiple"
                placeholder="请选择协同编辑人员" style="width: 80%; margin-right: 16px">
              </UserSelect>
              <a-form-item-rest>
                <a-button type="primary" @click="showEditorSelectionModal">
                  <team-outlined />
                  批量选择
                </a-button>
              </a-form-item-rest>
            </a-form-item>

            <!-- 支持引用 -->
            <a-form-item label="支持引用" name="ifIsCitable">
              <a-radio-group v-model:value="courseForm.ifIsCitable">
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
          <a-button style="margin-left: 8px" @click="handleSaveDraft" v-if="hasPermission('course:create')">
            保存草稿
          </a-button>
          <a-button type="primary" style="margin-left: 8px" @click="handlePublish"
            v-if="hasPermission('course:create')">
            发布课程
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
  </a-layout>
</template>

<script>
import { ref, reactive, computed, onMounted, defineComponent, watch } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { createCourse } from '@/api/course';
import { getUserList } from '@/api/user';
import { hasPermission } from '@/utils/permission';
import OrgUserComponent from '@/components/common/OrgUserComponent.vue';
import {
  UserOutlined, SettingOutlined,
  LogoutOutlined, DownOutlined,
  DashboardOutlined,
  ReadOutlined,
  SolutionOutlined,
  ClusterOutlined,
  TrophyOutlined,
  AppstoreOutlined,
  PlusOutlined,
  FileTextOutlined,
  VideoCameraOutlined,
  FilePdfOutlined,
  FolderOutlined,
  UploadOutlined,
  TeamOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '@/components/common/PageBreadcrumb.vue'
import CollaboratorSelectionModal from '@/components/common/CollaboratorSelectionModal.vue'
import { COLLABORATOR_TYPE, VISIBILITY_TYPE } from '@/utils/constants';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';
import UserSelect from '@/components/common/UserSelect.vue';
import ImageCropper from '@/components/common/ImageCropper.vue';

export default defineComponent({
  name: 'CourseCreatePage',
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
    FileTextOutlined,
    VideoCameraOutlined,
    FilePdfOutlined,
    FolderOutlined,
    UploadOutlined,
    TeamOutlined,
    DeleteOutlined,
    OrgUserComponent,
    PageBreadcrumb,
    CollaboratorSelectionModal,
    CategoryTreeSelect,
    UserSelect,
    ImageCropper
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const courseFormRef = ref(null);
    const collapsed = ref(false);
    const selectedKeys = ref(['courseList']);
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

    // 用户列表
    const userList = ref([]);
    const userOptions = computed(() => {
      return userList.value.map(user => ({
        label: `${user.nickname} (${user.employeeNo || '无工号'})`,
        value: user.userId
      }));
    });

    // 文章标签页
    const articleTabKey = ref('editor');

    // 文件列表
    const videoFileList = ref([]);
    const documentFileList = ref([]);
    const seriesFileList = ref([]);

    // 选择范围
    const selectedRange = ref({
      departments: [],
      roles: [],
      users: []
    });

    // 系列课文件表格列
    const seriesFileColumns = [
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
        title: '操作',
        key: 'action',
        width: 80
      }
    ];

    // 组织架构选择弹窗
    const orgUserModalVisible = ref(false);
    const editorSelectionModalVisible = ref(false);
    const userSelectionModalVisible = ref(false);// 课程表单
    const courseForm = reactive({
      title: '',
      type: 'article', // 默认为文章类型
      coverImage: '',
      instructorType: 1,
      instructorId: undefined,
      guestName: '',
      description: '',
      credit: 1,
      categoryIds: undefined,
      allowComments: true,
      isTop: false,
      appendixType: 'article',
      appendixPath: '',
      status: 'draft',
      duration: 0, // 视频时长（秒）
      visibility: {
        type: 'ALL',
        targets: []
      },
      collaborators: {
        editorType: 'ALL',
        editors: [],
      },
      ifIsCitable: true
    });// 表单验证规则
    const rules = {
      title: [
        { required: true, message: '请输入课程名称', trigger: 'blur' },
        { max: 100, message: '课程名称最多100个字符', trigger: 'blur' }
      ],
      type: [
        { required: true, message: '请选择课程类型', trigger: 'change' }
      ],
      credit: [
        { required: true, message: '请输入学分', trigger: 'blur' },
        { type: 'number', min: 1, max: 100, message: '学分范围为1-100', trigger: 'blur' }
      ],
      description: [
        { max: 500, message: '课程简介最多500个字符', trigger: 'blur' }
      ],
      coverImage: [
        { required: true, message: '请上传封面图', trigger: 'change' }
      ],
      appendixPath: [
        {
          required: true,
          trigger: 'change',
          validator: (rule, value) => {
            if (courseForm.type === 'article') {
              if (!value) {
                return Promise.reject('请输入文章URL链接');
              }
              // 验证是否是有效的URL
              const urlPattern = /^(https?:\/\/)([\da-z.-]+)\.([a-z.]{2,6})([/\w.-]*)*\/?$/;
              if (!urlPattern.test(value)) {
                return Promise.reject('请输入有效的URL链接');
              }
              return Promise.resolve();
            }
            if (!value && courseForm.type !== 'series') {
              return Promise.reject('请上传课程内容');
            }
            if (courseForm.type === 'series' && seriesFileList.value.length === 0) {
              return Promise.reject('请上传至少一个系列课文件');
            }
            if (courseForm.type === 'series' && !!seriesFileList.value.filter(i => i.status !== 'done').length) {
              return Promise.reject('请等待系列课文件都上传完成并移除错误文件');
            }
            return Promise.resolve();
          }
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

    // 选择课程类型
    const selectCourseType = (type) => {
      courseForm.type = type;
      // 重置相关字段
      if (type === 'article') {
        courseForm.appendixType = '';
        courseForm.appendixPath = '';
        videoFileList.value = [];
        documentFileList.value = [];
        seriesFileList.value = [];
      } else if (type === 'video') {
        courseForm.article = '';
        courseForm.articleUrl = ''; courseForm.appendixType = 'video';
        courseForm.appendixPath = '';
        documentFileList.value = [];
        seriesFileList.value = [];
      } else if (type === 'document') {
        courseForm.article = '';
        courseForm.articleUrl = '';
        courseForm.appendixType = 'document';
        courseForm.appendixPath = '';
        videoFileList.value = [];
        seriesFileList.value = [];
      } else if (type === 'series') {
        courseForm.article = '';
        courseForm.articleUrl = '';
        courseForm.appendixType = 'series';
        courseForm.appendixPath = '';
        videoFileList.value = [];
        documentFileList.value = [];
      }
    };


    // 视频上传前校验
    const beforeVideoUpload = (file) => {
      const isVideo = file.type.startsWith('video/');
      if (!isVideo) {
        message.error('请上传视频格式的文件!');
        return false;
      }

      const isLt8G = file.size / 1024 / 1024 / 1024 < 8;
      if (!isLt8G) {
        message.error('视频大小不能超过8GB!');
        return false;
      }

      return true;
    };

    // 文档上传前校验
    const beforeDocumentUpload = (file) => {
      const isDocument =
        file.type === 'application/pdf' ||
        file.type === 'application/msword' ||
        file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
        file.type === 'application/vnd.ms-powerpoint' ||
        file.type === 'application/vnd.openxmlformats-officedocument.presentationml.presentation';

      if (!isDocument) {
        message.error('请上传PDF、DOC、DOCX、PPT或PPTX格式的文件!');
        return false;
      }

      const isLt300M = file.size / 1024 / 1024 < 300;
      if (!isLt300M) {
        message.error('文档大小不能超过300MB!');
        return false;
      }

      return true;
    };

    // 系列课文件上传前校验
    const beforeSeriesFileUpload = (file) => {
      if (seriesFileList.value.length >= 150) {
        message.error('最多只能上传150个文件!');
        return false;
      }

      const isVideoOrDocument =
        file.type.startsWith('video/') ||
        file.type === 'application/pdf' ||
        file.type === 'application/msword' ||
        file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' ||
        file.type === 'application/vnd.ms-powerpoint' ||
        file.type === 'application/vnd.openxmlformats-officedocument.presentationml.presentation';

      if (!isVideoOrDocument) {
        message.error('请上传视频或文档格式的文件!');
        return false;
      }

      if (file.type.startsWith('video/')) {
        const isLt8G = file.size / 1024 / 1024 / 1024 < 8;
        if (!isLt8G) {
          message.error('视频大小不能超过8GB!');
          return false;
        }
      } else {
        const isLt300M = file.size / 1024 / 1024 < 300;
        if (!isLt300M) {
          message.error('文档大小不能超过300MB!');
          return false;
        }
      }

      return true;
    };


    // 处理视频上传
    const handleVideoUpload = ({ file, onSuccess, onError }) => {
      // 创建FormData对象
      const formData = new FormData();
      formData.append('file', file);

      // 调用上传接口
      fetch('/api/file/upload', {
        method: 'POST',
        body: formData,
        headers: {
          'Authorization': sessionStorage.getItem('token')
        }
      })
        .then(response => response.json())
        .then(res => {
          if (res.code === 200 && res.data) {
            // 上传成功
            courseForm.appendixPath = res.data.url;

            // 获取视频时长
            getVideoDuration(res.data.url)
              .then(duration => {
                // 设置视频时长（秒）
                courseForm.duration = Math.round(duration);
                console.log('视频时长（秒）:', courseForm.duration);
              })
              .catch(err => {
                console.error('获取视频时长失败:', err);
                // 获取时长失败时，设置默认值0
                courseForm.duration = 0;
              });

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

    // 处理讲师类型变化
    const handleInstructorTypeChange = (value) => {
      if (value === 2) {
        courseForm.instructorId = undefined;
      } else {
        courseForm.guestName = '';
      }
    };

    // 获取视频时长
    const getVideoDuration = (videoUrl) => {
      return new Promise((resolve, reject) => {
        const video = document.createElement('video');
        video.preload = 'metadata';

        video.onloadedmetadata = () => {
          // 视频元数据加载完成，可以获取时长
          resolve(video.duration);
          // 清理资源
          video.remove();
        };

        video.onerror = () => {
          // 加载失败
          reject('视频加载失败');
          // 清理资源
          video.remove();
        };

        // 设置视频源
        video.src = videoUrl;
      });
    };

    // 处理文档上传
    const handleDocumentUpload = ({ file, onSuccess, onError }) => {
      // 创建FormData对象
      const formData = new FormData();
      formData.append('file', file);

      // 调用上传接口
      fetch('/api/file/upload', {
        method: 'POST',
        body: formData,
        headers: {
          'Authorization': sessionStorage.getItem('token')
        }
      })
        .then(response => response.json())
        .then(res => {
          if (res.code === 200 && res.data) {
            // 上传成功
            courseForm.appendixPath = res.data.url;
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

    // 处理系列课文件上传
    const handleSeriesFileUpload = ({ file, onSuccess, onError }) => {
      // 创建FormData对象
      const formData = new FormData();
      formData.append('file', file);

      // 调用上传接口
      fetch('/api/file/upload', {
        method: 'POST',
        body: formData,
        headers: {
          'Authorization': sessionStorage.getItem('token')
        }
      })
        .then(response => response.json())
        .then(res => {
          console.log(res, '上传结果')
          if (res.code === 200 && res.data) {

            // 如果是视频文件，获取时长
            if (file.type.startsWith('video/')) {
              getVideoDuration(res.data.url)
                .then(duration => {
                  // 将时长添加到文件信息中
                  const fileInfo = {
                    uid: file.uid,
                    name: file.name,
                    status: 'done',
                    response: res,
                    url: res.data.url,
                    type: file.type,
                    bizType: getBizType(file.type),
                    size: file.size,
                    duration: Math.round(duration) // 添加时长信息（秒）
                  };

                  // 更新文件列表
                  const index = seriesFileList.value.findIndex(item => item.uid === file.uid);
                  if (index !== -1) {
                    seriesFileList.value[index] = fileInfo;
                  }

                  console.log('视频时长（秒）:', Math.round(duration));
                })
                .catch(err => {
                  console.error('获取视频时长失败:', err);
                });
            }

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

    // 更新系列课附件路径
    const updateSeriesAppendixPath = () => {
      console.log(seriesFileList.value, '系列课文件列表')
      const files = seriesFileList.value.map(file => ({
        name: file.name,
        url: file.response?.data?.url || file.url,
        type: file.type,
        bizType: file.bizType,
        duration: file.duration
      }));

      courseForm.appendixPath = JSON.stringify(files);
    };

    // 移除系列课文件
    const removeSeriesFile = (file) => {
      seriesFileList.value = seriesFileList.value.filter(item => item.uid !== file.uid);
      updateSeriesAppendixPath();
    };

    // 获取文件类型文本
    const getBizType = (mimeType) => {
      if (mimeType.startsWith('video/')) {
        return 'video';
      } else if (mimeType === 'application/pdf') {
        return 'document';
      } else if (mimeType === 'application/msword' || mimeType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document') {
        return 'document';
      } else if (mimeType === 'application/vnd.ms-powerpoint' || mimeType === 'application/vnd.openxmlformats-officedocument.presentationml.presentation') {
        return 'document';
      } else {
        return 'document';
      }
    };

    // 获取文件类型文本
    const getFileTypeText = (mimeType) => {
      console.log(mimeType, 'mimeType')
      if (mimeType?.startsWith('video/')) {
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

    // 显示组织架构选择弹窗 - 可见范围
    const showOrgUserModal = () => {
      orgUserModalVisible.value = true;
    };

    // 处理组织架构选择确认- 可见范围
    const handleOrgUserConfirm = (result) => {
      // 处理选择结果
      const tempArray = [];
      // 处理targets字段，将后端的TargetsDTO格式转换为前端期望的List<TargetDTO>格式
      const targets = [];

      const { departments, roles, users } = result;
      departments.forEach(item => {
        tempArray.push({ id: item.id, type: 'department', name: item.name });
      })
      roles.forEach(item => {
        tempArray.push({ id: item.id, type: 'role', name: item.name });
      })
      users.forEach(item => {
        tempArray.push({ id: item.id, type: 'user', name: item.name });
      })

      // 处理targets字段，将后端的TargetsDTO格式转换为前端期望的List<TargetDTO>格式
      targets.push({
        type: 'department',
        ids: departments.map(item => item.id)
      })
      targets.push({
        type: 'role',
        ids: roles.map(item => item.id)
      })
      targets.push({
        type: 'user',
        ids: users.map(item => item.id)
      })
      // 更新表单数据
      courseForm.visibility.targets = targets;

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
    };// 处理协同人员选择确认 - 协同编辑
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
      courseForm.collaborators.editors = editorIds;

      // 关闭弹窗
      editorSelectionModalVisible.value = false;
    };

    // 处理组织架构选择确定 - 协同编辑
    const handleEditorSelectionOk = () => {
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

    // 过滤用户选项
    const filterUserOption = (input, option) => {
      return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
    };

    // 处理保存草稿
    const handleSaveDraft = () => {
      courseFormRef.value.validate().then(() => {
        // 设置状态为草稿
        courseForm.status = 'draft';// 设置创建人信息
        courseForm.creatorId = userInfo.value.userId;
        courseForm.creatorName = userInfo.value.nickname;

        // 提交表单
        submitForm();
      }).catch(error => {
        console.log('表单验证失败:', error);
      });
    };

    // 处理发布
    const handlePublish = () => {
      courseFormRef.value.validate().then(() => {
        // 设置状态为已发布
        courseForm.status = 'published';

        // 设置创建人信息
        courseForm.creatorId = userInfo.value.userId;
        courseForm.creatorName = userInfo.value.nickname;


        // 提交表单
        submitForm();
      }).catch(error => {
        console.log('表单验证失败:', error);
      });
    };


    // 提交表单
    const submitForm = async () => {
      console.log(courseForm, '提交表单', seriesFileList)

      // 处理系列课文件
      if (courseForm.type === 'series') {
        // 计算系列课总时长（所有视频时长之和）
        let totalDuration = 0;

        courseForm.appendixFiles = seriesFileList.value.map(file => {
          console.log(file, '系列课文件')
          const fileObj = {
            name: file.name,
            duration: file.duration,
            url: file?.response?.data?.url,
            type: file.type,
            bizType: getBizType(file.type)
          };

          // 如果是视频文件且有时长信息，累加到总时长
          if (fileObj.type === 'video' && file.duration) {
            totalDuration += file.duration;
          }

          return fileObj;
        });

        // 设置课程总时长
        courseForm.duration = totalDuration;
      }

      // 确保单个视频课程的时长已设置
      if (courseForm.type === 'video' && !courseForm.duration) {
        // 如果没有获取到时长，设置默认值0
        courseForm.duration = 0;
      }

      // 非视频类型课程，时长设为0
      if (courseForm.type === 'article' || courseForm.type === 'document') {
        courseForm.duration = 0;
      }

      try {
        const res = await createCourse(courseForm);
        if (res.code === 200) {
          message.success(courseForm.status === 'published' ? '课程发布成功' : '课程保存成功');
          // 跳转到课程列表页
          router.push('/course/list');
        } else {
          message.error(res.message || '操作失败');
        }
      } catch (error) {
        console.error('创建课程失败:', error);
      }
    };

    // 处理取消
    const handleCancel = () => {
      // 跳转到课程列表页
      router.push('/course/list');
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
      // 额外的处理逻辑可以在这里添加
      courseFormRef.value.validateFields(['coverImage']);
    };

    onMounted(() => {
      // 获取用户信息
      getUserInfo();

      // 获取用户列表
      fetchUserList();
    });

    return {
      courseFormRef,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      userList,
      userOptions,
      articleTabKey,
      videoFileList,
      documentFileList,
      seriesFileList,
      seriesFileColumns,
      orgUserModalVisible,
      editorSelectionModalVisible,
      categoryModalVisible,
      courseForm,
      rules,
      hasPermission,
      selectCourseType,
      beforeVideoUpload,
      beforeDocumentUpload,
      beforeSeriesFileUpload,
      handleVideoUpload,
      handleDocumentUpload,
      handleSeriesFileUpload,
      removeSeriesFile,
      showOrgUserModal,
      handleOrgUserConfirm,
      handleOrgUserOk,
      handleOrgUserCancel,
      showEditorSelectionModal,
      handleEditorSelectionConfirm,
      handleEditorSelectionOk,
      handleEditorSelectionCancel,
      getTypeName,
      filterUserOption,
      handleSaveDraft,
      handlePublish,
      handleCancel,
      handleLogout,
      navigateTo,
      showCategoryModal,
      selectedRange,
      COLLABORATOR_TYPE,
      VISIBILITY_TYPE,
      getFileTypeText,
      handleInstructorTypeChange,
      handleCropSuccess
    };
  }
});
</script>

<style scoped>
.course-create-layout {
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

.course-type-selection {
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

.course-type-card {
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

.course-type-card:hover {
  color: #1890ff;
  background-color: transparent;
}

.course-type-card:hover .type-desc {
  color: #1890ff;
}


.course-type-card.active {
  color: #1890ff;
  border-bottom: 1px solid #fff;
  background-color: transparent;
}


.course-type-card.active .type-desc {
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
