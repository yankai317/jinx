<template>
  <a-layout class="certificate-create-layout">
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

        <!-- 证书创建表单 -->
        <a-card class="form-card">
          <a-form ref="certificateFormRef" class="form" :model="certificateForm" :rules="rules"
            :label-col="{ style: { width: '120px' } }" :wrapper-col="{ span: 12 }">
            <a-form-item label="证书名称" name="name">
              <a-input v-model:value="certificateForm.name" placeholder="请输入证书名称" :maxLength="50" />
            </a-form-item>

            <a-form-item label="证书描述" name="description">
              <a-textarea v-model:value="certificateForm.description" placeholder="请输入证书描述" :rows="4"
                :maxLength="200" /></a-form-item>

            <a-form-item label="证书模板" name="templateUrl">
              <a-upload v-model:file-list="fileList" name="file" :action="uploadUrl" :headers="uploadHeaders"
                :before-upload="beforeUpload" :show-upload-list="true" :max-count="1" @change="handleUploadChange">
                <a-button>
                  <upload-outlined />
                  选择图片
                </a-button><template #itemRender="{ file }">
                  <div class="upload-item">
                    <img v-if="file.url || file.thumbUrl" :src="file.url || file.thumbUrl" class="upload-preview" />
                    <div class="upload-info">
                      <div class="upload-name">{{ file.name }}</div>
                      <div class="upload-status">
                        <loading-outlined v-if="file.status === 'uploading'" />
                        <check-circle-outlined v-else-if="file.status === 'done'" style="color: #52c41a" />
                        <close-circle-outlined v-else-if="file.status === 'error'" style="color: #f5222d" /><a
                          v-if="file.status !== 'uploading'" @click="handleRemove(file)">删除</a>
                      </div>
                    </div>
                  </div>
                </template>
              </a-upload>
              <div class="upload-tip">支持jpg、png、jpeg格式，建议尺寸800x600px，大小不超过2MB</div>
            </a-form-item>

            <!-- 证书预览区 -->
            <a-form-item label="证书预览">
              <a-card v-if="certificateForm.templateUrl" class="preview-card">
                <div class="preview-header">证书预览</div>
                <div class="preview-content">
                  <img :src="certificateForm.templateUrl" alt="证书预览" class="preview-image" />
                  <div class="preview-info">
                    <h3>{{ certificateForm.name || '证书名称' }}</h3>
                    <p>{{ certificateForm.description || '证书描述' }}</p>
                    <p v-if="certificateForm.expireTime">有效期至: {{ certificateForm.expireTime }}</p>
                    <p v-else>永久有效</p>
                  </div>
                </div>
              </a-card>
              <a-empty v-else description="上传证书模板后可预览效果" />
            </a-form-item>

            <a-form-item label="过期时间" name="expireTime">
              <a-date-picker v-model:value="expireDate" style="width: 100%" :show-time="{ format: 'HH:mm:ss' }"
                format="YYYY-MM-DD HH:mm:ss" placeholder="请选择过期时间" @change="handleExpireDateChange" />
            </a-form-item>

            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSubmit" :disabled="!hasCreatePermission"
                  v-if="hasCreatePermission">创建证书</a-button>
                <a-tooltip v-else title="您没有创建证书的权限">
                  <a-button type="primary" disabled>创建证书</a-button>
                </a-tooltip>
                <a-button @click="navigateTo('/certificate/list')">取消</a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import { ref, reactive, onMounted, defineComponent } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import { createCertificate } from '@/api/certificate';
import { hasPermission } from '@/utils/permission';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import {
  UploadOutlined,
  LoadingOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';

export default defineComponent({
  name: 'CertificateCreatePage',
  components: {
    HeaderComponent,
    SiderComponent,
    UploadOutlined,
    LoadingOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
    PageBreadcrumb,
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');
    const router = useRouter();
    const certificateFormRef = ref(null);
    const collapsed = ref(false);
    const selectedKeys = ref(['certificate']);
    const openKeys = ref(['operation']);
    const categoryModalVisible = ref(false);
    const fileList = ref([]);
    const expireDate = ref(null);

    // 权限控制
    const hasCreatePermission = ref(false);

    // 用户信息 - 移到了HeaderComponent中
    const userInfo = ref({
      userId: null,
      nickname: '',
      avatar: '',
      employeeNo: '',
      departments: [],
      roles: [],
      permissions: []
    });

    // 证书表单
    const certificateForm = reactive({
      name: '',
      description: '',
      templateUrl: '',
      expireTime: ''
    });
    // 表单验证规则
    const rules = {
      name: [
        { required: true, message: '请输入证书名称', trigger: 'blur' },
        { max: 50, message: '证书名称不能超过50个字符', trigger: 'blur' }
      ],
      description: [
        { max: 200, message: '证书描述不能超过200个字符', trigger: 'blur' }
      ],
      templateUrl: [
        { required: true, message: '请上传证书模板', trigger: 'change' }
      ]
    };

    // 上传配置
    const uploadUrl = '/api/file/upload';
    const uploadHeaders = {
      Authorization: 'Bearer ' + sessionStorage.getItem('token')
    };

    // 获取用户信息 - 移到了HeaderComponent中
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

    // 上传前校验
    const beforeUpload = (file) => {
      // 检查文件类型
      const isImage = file.type === 'image/jpeg' || file.type === 'image/png';
      if (!isImage) {
        message.error('只能上传JPG/PNG格式的图片!');
        return false;
      }

      // 检查文件大小
      const isLt2M = file.size / 1024 / 1024 < 100;
      if (!isLt2M) {
        message.error('图片大小不能超过100MB!');
        return false;
      }

      return true;
    };

    // 处理上传状态变化
    const handleUploadChange = (info) => {
      fileList.value = info.fileList.slice(-1);

      if (info.file.status === 'done') {
        if (info.file.response && info.file.response.code === 200) {
          message.success(`${info.file.name} 上传成功`);
          certificateForm.templateUrl = info.file.response.data.url;
        } else {
          message.error(`${info.file.name} 上传失败: ${info.file.response?.message || '未知错误'}`);
        }
      } else if (info.file.status === 'error') {
        message.error(`${info.file.name} 上传失败`);
      }
    };

    // 处理移除文件
    const handleRemove = () => {
      fileList.value = [];
      certificateForm.templateUrl = '';
    };

    // 处理过期时间变更
    const handleExpireDateChange = (date) => {
      if (date) {
        certificateForm.expireTime = date.format('YYYY-MM-DD HH:mm:ss');
      } else {
        certificateForm.expireTime = '';
      }
    };

    // 处理提交
    const handleSubmit = async () => {
      try {
        await certificateFormRef.value.validate();

        // 创建证书
        const res = await createCertificate(certificateForm);
        if (res.code === 200) {
          message.success('证书创建成功');
          router.push('/certificate/list');
        } else {
          message.error(res.message || '证书创建失败');
        }
      } catch (error) {
        console.error('证书创建失败:', error);
      }
    };

    // 退出登录 - 移到了HeaderComponent中
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

    onMounted(() => {
      // 检查创建权限
      hasCreatePermission.value = hasPermission('certificate:create');
    });

    return {
      certificateFormRef,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      certificateForm,
      rules,
      fileList,
      uploadUrl,
      uploadHeaders,
      expireDate,
      categoryModalVisible,
      hasCreatePermission,
      beforeUpload,
      handleUploadChange,
      handleRemove,
      handleExpireDateChange,
      handleSubmit,
      navigateTo,
      showCategoryModal
    };
  }
});
</script>

<style scoped>
.certificate-create-layout {
  min-height: 100vh;
}

.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: auto;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}


.form-card {
  background-color: #fff;
  padding: 24px;
}

.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}

.upload-item {
  display: flex;
  align-items: center;
}

.upload-preview {
  width: 80px;
  height: 60px;
  object-fit: cover;
  margin-right: 12px;
  border-radius: 4px;
}

.upload-info {
  flex: 1;
}

.upload-name {
  margin-bottom: 4px;
}

.upload-status {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-card {
  margin-top: 16px;
  border: 1px solid #f0f0f0;
}

.preview-header {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #1890ff;
}

.preview-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.preview-image {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
  margin-bottom: 16px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.preview-info {
  width: 100%;
  padding: 16px;
  background-color: #f9f9f9;
  border-radius: 4px;
}
</style>
