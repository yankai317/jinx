<template>
  <div class="train-edit-page">
    <a-layout>
      <!-- 使用 HeaderComponent -->
      <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />
      <a-layout>
        <!-- 使用 SiderComponent -->
        <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
          @category-modal-show="showCategoryModal" />

        <a-layout-content class="content">
          <!-- 标题栏 -->
          <div class="content-header">
            <PageBreadcrumb />
          </div>
          <div class="form-container">
            <a-spin :spinning="loading">
              <a-form ref="trainFormRef" :model="formData" :rules="formRules" class="form"
                :label-col="{ style: { width: '120px' } }" :wrapper-col="{ span: 12 }">
                <!-- 基本信息 -->
                <a-divider orientation="left">基本信息</a-divider>
                <a-form-item label="培训名称" name="name">
                  <a-input v-model:value="formData.name" placeholder="请输入培训名称" :maxlength="50" show-count />
                </a-form-item>
                <a-form-item label="封面图" name="cover">
                  <ImageCropper
                    v-model:value="formData.cover"
                    :width="640"
                    :height="360"
                    label="选择图片"
                    tip="支持JPG、PNG格式，裁剪为 640x360 像素"
                    extra="请上传清晰美观的封面图，作为培训的主要视觉元素"
                    @crop-success="handleCropSuccess"
                  />
                </a-form-item>
                <a-form-item label="培训简介" name="introduction">
                  <a-textarea v-model:value="formData.introduction" placeholder="请输入培训简介" :rows="4" :maxlength="500"
                    show-count />
                </a-form-item>
                <a-form-item label="学分" name="credit">
                  <a-input-number v-model:value="formData.credit" :min="0" :max="100" placeholder="请输入学分" />
                </a-form-item>
                <a-form-item label="分类" name="categoryIds">
                  <CategoryTreeSelect v-model:value="formData.categoryIds" />
                </a-form-item>
                <!-- <a-form-item label="评论权限" name="allowComment">
                  <a-radio-group v-model:value="formData.allowComment">
                    <a-radio :value="true">允许评论</a-radio>
                    <a-radio :value="false">禁止评论</a-radio>
                  </a-radio-group>
                </a-form-item> -->
                <a-form-item label="关联证书" name="certificateId">
                  <a-select v-model:value="formData.certificateId" placeholder="请选择关联证书" style="width: 100%"
                    allow-clear>
                    <a-select-option v-for="certificate in certificateList" :key="certificate.id"
                      :value="certificate.id">
                      {{ certificate.name }}
                    </a-select-option>
                  </a-select>
                </a-form-item>

                <!-- 培训内容 -->
                <a-divider orientation="left">培训内容</a-divider>
                <a-form-item label="课程">
                  <!-- <div class="content-actions"> -->
                    <a-button type="primary" @click="showCourseSelectModal">添加已有课程</a-button>
                    <!-- <a-button style="margin-left: 8px" @click="showFileUploadModal">本地上传文件</a-button>
                  </div> -->

                  <a-table :columns="contentColumns" :data-source="formData.contents" :pagination="false"
                    :row-key="record => record.id || record.tempId" class="content-table">
                    <template #bodyCell="{ column, record, index }">
                      <template v-if="column.key === 'type'">
                        {{ getContentTypeName(record.type) }}
                      </template>
                      <template v-if="column.key === 'isRequired'">
                        <a-switch v-model:checked="record.isRequired" />
                      </template>
                      <template v-if="column.key === 'action'">
                        <a-space>
                          <a-button type="link" @click="moveUp(index)" :disabled="index === 0">
                            <arrow-up-outlined />
                          </a-button>
                          <a-button type="link" @click="moveDown(index)"
                            :disabled="index === formData.contents.length - 1">
                            <arrow-down-outlined />
                          </a-button>
                          <a-button type="link" danger @click="removeContent(index)">
                            <delete-outlined />
                          </a-button>
                        </a-space>
                      </template>
                    </template>
                  </a-table>
                </a-form-item>

                <!-- 可见范围 -->
                <a-divider orientation="left">可见范围</a-divider>
                <a-form-item label="可见范围" name="visibility.type">
                  <a-radio-group v-model:value="formData.visibility.type"
                    :options="Object.values(VISIBILITY_TYPE).map(item => ({ label: item.name, value: item.key }))"
                    @change="formData.visibility.targets = []">
                  </a-radio-group>
                  <div v-if="formData.visibility.type === 'PART'" style="margin-top: 16px">
                    <a-button type="primary" @click="showVisibilitySelectModal">
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
                <a-divider orientation="left">协同设置</a-divider>
                <a-form-item label="协同编辑" name="collaborationEditType">
                  <a-radio-group v-model:value="formData.collaborators.editorType"
                    :options="Object.values(COLLABORATOR_TYPE).map(item => ({ label: item.name, value: item.key }))"
                    @change="formData.collaborators.editors = []">
                  </a-radio-group>
                  <div v-if="formData.collaborators.editorType === 'PART'" style="margin-top: 16px">
                    <UserSelect v-model:value="formData.collaborators.editors" mode="multiple" placeholder="请选择协同编辑人员"
                      style="width: 80%; margin-right: 16px" query-admin />
                    <a-button type="primary" @click="showEditorsSelectModal">
                      <team-outlined />
                      批量选择
                    </a-button>
                  </div>
                </a-form-item>

                <a-form-item label="支持引用" name="ifIsCitable">
                  <a-radio-group v-model:value="formData.ifIsCitable">
                    <a-radio :value="true">是</a-radio>
                    <a-radio :value="false">否</a-radio>
                  </a-radio-group>
                </a-form-item>

              </a-form>
            </a-spin>
          </div>
          <!-- 底部操作按钮区域 -->
          <div class="footer-actions">
            <a-space>
              <a-button @click="handleCancel">取消</a-button>
              <a-button type="primary" :loading="savingDraft" @click="handleSaveDraft"
                v-if="hasPermission('train:edit:draft')">
                保存草稿
              </a-button>
              <a-button type="primary" :loading="publishing" @click="handlePublish"
                v-if="hasPermission('train:edit:publish')">
                发布
              </a-button>
            </a-space>
          </div>
        </a-layout-content>
      </a-layout>
    </a-layout>

    <!-- 课程选择弹窗 -->
    <a-modal v-model:visible="courseSelectModalVisible" title="选择课程" width="800px" @ok="handleCourseSelectOk"
      @cancel="handleCourseSelectCancel">
      <div class="course-selection-modal">
        <div class="course-search">
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

    <!-- 文件上传弹窗 -->
    <a-modal v-model:visible="fileUploadModalVisible" title="上传文件" @ok="handleFileUploadOk"
      @cancel="handleFileUploadCancel">
      <a-upload v-model:file-list="uploadFileList" :before-upload="beforeFileUpload" :customRequest="handleFileUpload"
        :multiple="true">
        <a-button>
          <upload-outlined />
          选择文件
        </a-button>
      </a-upload>
      <div class="upload-tip">支持视频、文档等格式，单个文件大小不超过500MB</div>
    </a-modal>

    <!-- 可见范围选择弹窗 -->
    <a-modal v-model:visible="visibilitySelectModalVisible" title="选择可见范围" width="800px"
      @cancel="handleVisibilitySelectCancel" :footer="null">
      <org-user-component v-if="visibilitySelectModalVisible" :selection-type="'multiple'"
        @confirm="handleVisibilitySelectConfirm" @cancel="handleVisibilitySelectCancel" :selected-ids="selectedRange" />
    </a-modal>

    <!-- 协同编辑人员选择弹窗 -->
    <CollaboratorSelectionModal v-model:visible="editorsSelectModalVisible" @cancel="handleEditorsSelectCancel"
      @confirm="handleEditorsSelectionConfirm" />
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed, watch } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { message } from 'ant-design-vue';
import { useRouter, useRoute } from 'vue-router';
import {
  UploadOutlined,
  TeamOutlined,
  DeleteOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import OrgUserComponent from '@/components/common/OrgUserComponent.vue';
import { getTrainDetail, updateTrain } from '@/api/train';
import { getCategoryList } from '@/api/category';
import { getCertificateList } from '@/api/certificate';
import { getCourseList } from '@/api/course';
import { getUserList } from '@/api/user';
import { hasPermission } from '@/utils/permission';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';
import CollaboratorSelectionModal from '@/components/common/CollaboratorSelectionModal.vue';
import { queryRange } from '@/api/common';
import { COLLABORATOR_TYPE, VISIBILITY_TYPE } from '@/utils/constants';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';
import ImageCropper from '@/components/common/ImageCropper.vue';
import UserSelect from '@/components/common/UserSelect.vue';

// 初始化dayjs中文
dayjs.locale('zh-cn');

export default defineComponent({
  name: 'TrainEditPage',
  components: {
    HeaderComponent,
    SiderComponent,
    UploadOutlined,
    TeamOutlined,
    DeleteOutlined,
    ArrowUpOutlined,
    ArrowDownOutlined,
    OrgUserComponent,
    PageBreadcrumb,
    CollaboratorSelectionModal,
    CategoryTreeSelect,
    ImageCropper,
    UserSelect
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const trainFormRef = ref(null);
    const collapsed = ref(false);
    const selectedKeys = ref(['trainList']);
    const openKeys = ref(['learning']);
    const trainId = ref(parseInt(route.params.id));
    const loading = ref(false);

    // 权限控制


    // 表单数据
    const formData = reactive({
      id: null,
      name: '',
      cover: '',
      introduction: '',
      credit: 0,
      categoryIds: '',
      allowComment: true,
      certificateId: null,
      status: 'draft',
      contents: [],
      deleteContentIds: [],
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

    /** 已选范围详情 */
    const selectedRange = ref([
      // { id: 1, type: 'department' },
      // { id: 2, type: 'role' },
      // { id: 3, type: 'user' }
    ]);

    // 表单校验规则
    const formRules = {
      name: [
        { required: true, message: '请输入培训名称', trigger: 'blur' },
        { max: 50, message: '培训名称最多50个字符', trigger: 'blur' }
      ],
      introduction: [
        { max: 500, message: '培训简介最多500个字符', trigger: 'blur' }
      ],
      credit: [
        { type: 'number', min: 0, max: 100, message: '学分范围为0-100', trigger: 'change' }
      ]
    };

    // 文件上传相关
    const uploadFileList = ref([]);

    // 分类列表
    const categoryList = ref([]);

    // 证书列表
    const certificateList = ref([]);

    // 用户列表
    const userList = ref([]);
    const userOptions = computed(() => {
      return userList.value.map(user => ({
        label: `${user.nickname} (${user.employeeNo || '无工号'})`,
        value: user.userId
      }));
    });

    // 课程相关
    const courseList = ref([]);
    const courseLoading = ref(false);
    const courseSearchKeyword = ref('');
    const selectedCourseKeys = ref([]);
    const courseSelectModalVisible = ref(false);

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

    // 内容表格列定义
    const contentColumns = [
      {
        title: '序号',
        dataIndex: 'sortOrder',
        key: 'sortOrder',
        width: 80
      },
      {
        title: '内容名称',
        dataIndex: 'title',
        key: 'title'
      },
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
        width: 120
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
        width: 150
      }
    ];


    // 可见范围选择
    const selectedVisibilityItems = ref([]);

    // 文件上传
    const fileUploadModalVisible = ref(false);

    // 可见范围选择
    const visibilitySelectModalVisible = ref(false);

    // 协同编辑人员选择
    const editorsSelectModalVisible = ref(false);

    // 保存和发布状态
    const savingDraft = ref(false);
    const publishing = ref(false);

    // 获取培训详情
    const fetchTrainDetail = async () => {
      loading.value = true;
      try {
        const res = await getTrainDetail(trainId.value);
        if (res.code === 200 && res.data) {
          const detail = res.data;

          // 填充表单数据
          formData.id = detail.id;
          formData.name = detail.title;
          formData.cover = detail.cover;
          formData.introduction = detail.introduction;
          formData.credit = detail.credit;
          formData.categoryIds = detail.categoryIds || '';
          formData.allowComment = detail.allowComment;
          formData.certificateId = detail.certificateId;
          formData.status = detail.status;
          formData.contents = detail.contents || [];
          formData.ifIsCitable = detail.ifIsCitable;


          getRange();
          // 处理可见范围
          if (detail.visibility) {
            formData.visibility.type = detail.visibility.type || 'ALL';

            // 转换可见范围目标格式并添加到已选择项
            selectedVisibilityItems.value = [];

            // 重置targets数组
            formData.visibility.targets = [];

          }

          // 处理协同设置
          formData.collaborators = detail.collaborators || { editorType: 'ALL', editors: [] };
        } else {
          message.error('获取培训详情失败');
        }
      } catch (error) {
        console.error('获取培训详情失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取分类列表
    const fetchCategoryList = async () => {
      try {
        const res = await getCategoryList();
        if (res.code === 200) {
          categoryList.value = res.data || [];
        }
      } catch (error) {
        console.error('获取分类列表失败:', error);
      }
    };

    // 获取证书列表
    const fetchCertificateList = async () => {
      try {
        const res = await getCertificateList({
          pageNum: 1,
          pageSize: 100
        });
        if (res.code === 200 && res.data) {
          certificateList.value = res.data.list || [];
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



    // 检查用户是否有特定权限


    // 文件上传前校验
    const beforeFileUpload = (file) => {
      const isLt500M = file.size / 1024 / 1024 < 500;
      if (!isLt500M) {
        message.error('文件大小不能超过500MB!');
      }
      return isLt500M;
    };

    // 处理文件上传
    const handleFileUpload = ({ file, onSuccess, onError }) => {
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
            onSuccess({
              id: res.data.id || Date.now(),
              url: res.data.url,
              name: file.name,
              type: getFileTypeText(file.type),
              size: file.size
            });
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
          const existingCourseIds = formData.contents
            .filter(content => content.type === 'COURSE')
            .map(content => content.contentId);

          selectedCourseKeys.value = existingCourseIds;
        }
      } catch (error) {
        console.error('获取课程列表失败:', error);
      } finally {
        courseLoading.value = false;
      }
    };

    // 显示课程选择弹窗
    const showCourseSelectModal = () => {
      courseSelectModalVisible.value = true;
      searchCourses();
    };

    // 处理课程选择变更
    const handleCourseSelectionChange = (selectedRowKeys) => {
      selectedCourseKeys.value = selectedRowKeys;
    };

    // 处理课程选择确认
    const handleCourseSelectOk = () => {
      // 移除已取消选择的课程
      formData.contents = formData.contents.filter(content => {
        if (content.type !== 'COURSE') return true;
        if (!selectedCourseKeys.value.includes(content.contentId)) {
          // 如果是已有内容，添加到删除列表
          if (content.id) {
            formData.deleteContentIds.push(content.id);
          }
          return false;
        }
        return true;
      });

      // 添加新选择的课程
      selectedCourseKeys.value.forEach(courseId => {
        const existingContent = formData.contents.find(content =>
          content.type === 'COURSE' && content.contentId === courseId
        );

        if (!existingContent) {
          const course = courseList.value.find(c => c.id === courseId);
          if (course) {
            formData.contents.push({
              type: 'COURSE',
              contentId: courseId,
              title: course.name,
              isRequired: true,
              sortOrder: formData.contents.length + 1,
              tempId: Date.now() + Math.random().toString(36).substr(2, 5)
            });
          }
        }
      });

      // 重新排序
      formData.contents = formData.contents.map((content, index) => ({
        ...content,
        sortOrder: index + 1
      }));

      courseSelectModalVisible.value = false;
    };

    // 处理课程选择取消
    const handleCourseSelectCancel = () => {
      courseSelectModalVisible.value = false;
    };

    // 显示文件上传弹窗
    const showFileUploadModal = () => {
      fileUploadModalVisible.value = true;
      uploadFileList.value = [];
    };

    // 处理文件上传确认
    const handleFileUploadOk = () => {
      // 添加上传的文件到内容列表
      uploadFileList.value.forEach(file => {
        if (file.status === 'done' && file.response) {
          formData.contents.push({
            type: 'FILE',
            contentId: file.response.id || null,
            title: file.name,
            contentUrl: file.response.url || '',
            isRequired: true,
            sortOrder: formData.contents.length + 1,
            tempId: Date.now() + Math.random().toString(36).substr(2, 5)
          });
        }
      });

      // 重新排序
      formData.contents = formData.contents.map((content, index) => ({
        ...content,
        sortOrder: index + 1
      }));

      fileUploadModalVisible.value = false;
      uploadFileList.value = [];
    };

    // 处理文件上传取消
    const handleFileUploadCancel = () => {
      fileUploadModalVisible.value = false;
      uploadFileList.value = [];
    };

    // 移除内容
    const removeContent = (index) => {
      const content = formData.contents[index];

      // 如果是已有内容，添加到删除列表
      if (content.id) {
        formData.deleteContentIds.push(content.id);
      }

      formData.contents.splice(index, 1);

      // 重新排序
      formData.contents = formData.contents.map((content, index) => ({
        ...content,
        sortOrder: index + 1
      }));
    };

    // 上移内容
    const moveUp = (index) => {
      if (index > 0) {
        const temp = formData.contents[index];
        formData.contents[index] = formData.contents[index - 1];
        formData.contents[index - 1] = temp;

        // 更新排序
        formData.contents.forEach((content, idx) => {
          content.sortOrder = idx + 1;
        });
      }
    };

    // 下移内容
    const moveDown = (index) => {
      if (index < formData.contents.length - 1) {
        const temp = formData.contents[index];
        formData.contents[index] = formData.contents[index + 1];
        formData.contents[index + 1] = temp;

        // 更新排序
        formData.contents.forEach((content, idx) => {
          content.sortOrder = idx + 1;
        });
      }
    };

    // 获取内容类型名称
    const getContentTypeName = (type) => {
      const typeMap = {
        'COURSE': '课程',
        'FILE': '文件',
        'EXAM': '考试',
        'ASSIGNMENT': '作业',
        'SURVEY': '问卷'
      };
      return typeMap[type] || type;
    };

    // 显示可见范围选择弹窗
    const showVisibilitySelectModal = () => {
      visibilitySelectModalVisible.value = true;
    };

    // 处理可见范围选择确认
    const handleVisibilitySelectConfirm = (result) => {
      // 更新已选择项
      selectedVisibilityItems.value = result.items || [];

      // 更新表单数据
      formData.visibility.targets = [];

      // 处理部门
      if (result.departments && result.departments.length > 0) {
        formData.visibility.targets.push({
          type: 'department',
          ids: result.departments.map(dept => dept.id)
        });
      }

      // 处理角色
      if (result.roles && result.roles.length > 0) {
        formData.visibility.targets.push({
          type: 'role',
          ids: result.roles.map(role => role.id)
        });
      }

      // 处理用户
      if (result.users && result.users.length > 0) {
        formData.visibility.targets.push({
          type: 'user',
          ids: result.users.map(user => user.id)
        });
      }

      visibilitySelectModalVisible.value = false;
    };

    /** 获取范围回显 */
    const getRange = () => {
      queryRange({
        businessType: 'TRAIN',
        businessId: formData.id,
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
        formData.visibility.targets = targets;
        selectedRange.value = tempArray;
      });
    };


    // 处理可见范围选择取消
    const handleVisibilitySelectCancel = () => {
      visibilitySelectModalVisible.value = false;
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

    // 显示协同编辑人员选择弹窗
    const showEditorsSelectModal = () => {
      editorsSelectModalVisible.value = true;
    };

    // 处理协同编辑人员选择确认
    const handleEditorsSelectionConfirm = (selectedUsers) => {
      if (selectedUsers && selectedUsers.length > 0) {
        formData.collaborators.editors = selectedUsers.map(user => user.id);
      } else {
        formData.collaborators.editors = [];
      }
      editorsSelectModalVisible.value = false;
    };

    // 处理协同编辑人员选择取消
    const handleEditorsSelectCancel = () => {
      editorsSelectModalVisible.value = false;
    };

    // 保存为草稿
    const handleSaveDraft = async () => {
      formData.status = 'draft';
      await submitForm();
    };

    // 发布
    const handlePublish = async () => {
      formData.status = 'published';
      await submitForm();
    };

    // 提交表单
    const submitForm = async () => {
      try {
        if (formData.status === 'draft') {
          savingDraft.value = true;
        } else {
          publishing.value = true;
        }

        // 验证表单
        await trainFormRef.value.validate();

        // 验证培训内容
        if (formData.contents.length === 0) {
          message.error('请至少添加一项培训内容');
          savingDraft.value = false;
          publishing.value = false;
          return;
        }

        // 验证可见范围
        if (formData.visibility.type === 'PART' && formData.visibility.targets.length === 0) {
          message.error('请选择可见范围');
          savingDraft.value = false;
          publishing.value = false;
          return;
        }

        // 验证协同设置
        if (formData.collaborators.editorType === 'PART' && formData.collaborators.editors.length === 0) {
          message.error('请选择协同编辑人员');
          savingDraft.value = false;
          publishing.value = false;
          return;
        }

        // 准备提交数据，将name映射到title字段
        const submitData = {
          ...formData,
          title: formData.name
        };

        // 调用更新培训API
        const res = await updateTrain(submitData);

        if (res.code === 200) {
          message.success(formData.status === 'draft' ? '保存草稿成功' : '发布成功');
          // 跳转到培训列表页
          router.push('/train/list');
        } else {
          message.error(res.message || '操作失败');
        }
      } catch (error) {
        console.error('提交表单失败:', error);
      } finally {
        savingDraft.value = false;
        publishing.value = false;
      }
    };

    // 取消
    const handleCancel = () => {
      router.push('/train/list');
    };

    // 处理裁剪成功
    const handleCropSuccess = (imageUrl, data) => {
      console.log('裁剪成功:', imageUrl, data);
      // 封面图片URL已通过v-model自动更新到formData.cover
      trainFormRef.value.validateFields(['cover']);
    };

    onMounted(() => {
      // 获取培训详情
      fetchTrainDetail();

      // 获取分类列表
      fetchCategoryList();

      // 获取证书列表
      fetchCertificateList();

      // 获取用户列表
      fetchUserList();


    });

    return {
      trainFormRef,
      formData,
      formRules,
      collapsed,
      selectedKeys,
      openKeys,
      loading,

      uploadFileList,
      categoryList,
      certificateList,
      contentColumns,
      courseSelectModalVisible,
      courseSearchKeyword,
      courseList,
      courseLoading,
      selectedCourseKeys,
      courseSelectionColumns,
      fileUploadModalVisible,
      visibilitySelectModalVisible,
      selectedVisibilityItems,
      editorsSelectModalVisible,
      userOptions,
      savingDraft,
      publishing,

      beforeFileUpload,
      handleFileUpload,
      showCourseSelectModal,
      searchCourses,
      handleCourseSelectionChange,
      handleCourseSelectOk,
      handleCourseSelectCancel,
      showFileUploadModal,
      handleFileUploadOk,
      handleFileUploadCancel,
      removeContent,
      moveUp,
      moveDown,
      getContentTypeName,
      showVisibilitySelectModal,
      handleVisibilitySelectConfirm,
      handleVisibilitySelectCancel,
      getTypeName,
      showEditorsSelectModal,
      handleEditorsSelectionConfirm,
      handleEditorsSelectCancel,
      handleSaveDraft,
      handlePublish,
      handleCancel,
      hasPermission,
      formatFileSize,
      selectedRange,
      handleCropSuccess,
      COLLABORATOR_TYPE,
      VISIBILITY_TYPE
    };
  }
});
</script>

<style scoped>
.train-edit-page {
  background-color: #f0f2f5;
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

.header {
  background-color: #fff;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  height: 64px;
}

.header-title {
  font-size: 18px;
  font-weight: 500;
}


.content-actions {
  margin-bottom: 16px;
}

.content-table {
  margin-top: 16px;
}

.selected-targets {
  margin-top: 16px;
}

.upload-tip {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  margin-top: 8px;
}

.course-selection-modal .course-search {
  margin-bottom: 16px;
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
