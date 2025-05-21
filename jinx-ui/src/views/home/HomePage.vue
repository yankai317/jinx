<template>
  <a-layout class="home-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

    <a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent
        v-model:collapsed="collapsed"
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal"
      />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <div class="welcome-section">
          <h2>欢迎，{{ userInfo.nickname || '用户' }}！</h2>
          <p>今天是 {{ currentDate }}，祝您工作愉快！</p>
        </div>

        <!-- 统计数据展示 -->
        <div class="statistics-section">
          <a-row :gutter="16">
            <a-col :span="6" v-if="hasPermission('course')">
              <a-card hoverable @click="navigateTo('/course/list')">
                <template #cover>
                  <div class="card-icon course-icon">
                    <read-outlined />
                  </div>
                </template>
                <div class="card-title">课程列表</div>
<!--                <a-statistic-->
<!--                  title="课程总数"-->
<!--                  :value="statistics.courseCount"-->
<!--                  :precision="0"-->
<!--                  style="text-align: center;"-->
<!--                />-->
                <div class="card-footer">
                  <a-button type="link" @click.stop="navigateTo('/course/list')">查看详情</a-button>
                </div>
              </a-card>
            </a-col>
            <a-col :span="6" v-if="hasPermission('train')">
              <a-card hoverable @click="navigateTo('/train/list')">
                <template #cover>
                  <div class="card-icon train-icon">
                    <solution-outlined />
                  </div>
                </template>
                <div class="card-title">培训列表</div>
<!--                <a-statistic-->
<!--                  title="培训完成率"-->
<!--                  :value="statistics.trainCompletionRate"-->
<!--                  :precision="2"-->
<!--                  :suffix="'%'"-->
<!--                  style="text-align: center;"-->
<!--                />-->
                <div class="card-footer">
                  <a-button type="link" @click.stop="navigateTo('/train/list')">查看详情</a-button>
                </div>
              </a-card>
            </a-col>
            <a-col :span="6" v-if="hasPermission('map')">
              <a-card hoverable @click="navigateTo('/map/list')">
                <template #cover>
                  <div class="card-icon map-icon">
                    <cluster-outlined />
                  </div>
                </template>
                <div class="card-title">学习地图</div>
<!--                <a-statistic-->
<!--                  title="学习地图"-->
<!--                  :value="statistics.mapCount"-->
<!--                  :precision="0"-->
<!--                  style="text-align: center;"-->
<!--                />-->
                <div class="card-footer">
                  <a-button type="link" @click.stop="navigateTo('/map/list')">查看详情</a-button>
                </div>
              </a-card>
            </a-col>
            <a-col :span="6" v-if="hasPermission('certificate')">
              <a-card hoverable @click="navigateTo('/certificate/list')">
                <template #cover>
                  <div class="card-icon certificate-icon">
                    <trophy-outlined />
                  </div>
                </template>
                <div class="card-title">证书列表</div>
<!--                <a-statistic-->
<!--                  title="获得证书"-->
<!--                  :value="statistics.certificateCount"-->
<!--                  :precision="0"-->
<!--                  style="text-align: center;"-->
<!--                />-->
                <div class="card-footer">
                  <a-button type="link" @click.stop="navigateTo('/certificate/list')">查看详情</a-button>
                </div>
              </a-card>
            </a-col>
          </a-row>
        </div>

        <!-- 快捷入口 -->
        <div class="shortcuts-section">
          <h3>快捷操作</h3>
          <a-row :gutter="16">
            <a-col :span="6" v-if="hasPermission('course:create')">
              <a-button type="primary" block @click="navigateTo('/course/create')">
                <plus-outlined />创建课程
              </a-button>
            </a-col>
            <a-col :span="6" v-if="hasPermission('train:create')">
              <a-button type="primary" block @click="navigateTo('/train/create')">
                <plus-outlined />创建培训
              </a-button>
            </a-col>
            <a-col :span="6" v-if="hasPermission('map:create')">
              <a-button type="primary" block @click="navigateTo('/map/create')">
                <plus-outlined />创建学习地图
              </a-button>
            </a-col>
            <a-col :span="6" v-if="hasPermission('category:manage')">
              <a-button type="primary" block @click="showCategoryModal">
                <appstore-outlined />类目管理
              </a-button>
            </a-col>
          </a-row>
        </div>

        <!-- 最近学习 -->
<!--        <div class="recent-section">-->
<!--          <a-tabs default-active-key="1">-->
<!--            <a-tab-pane key="1" tab="最近课程">-->
<!--              <a-list-->
<!--                :data-source="recentData.courses"-->
<!--                :loading="loading.courses"-->
<!--                item-layout="horizontal"-->
<!--              >-->
<!--                <template #renderItem="{ item }"><a-list-item>-->
<!--                    <a-list-item-meta-->
<!--                      :title="item.name"-->
<!--                      :description="`学习进度: ${item.progress}%`"-->
<!--                    ><template #avatar><a-avatar><template #icon><read-outlined /></template>-->
<!--                        </a-avatar></template></a-list-item-meta><template #actions><a @click="navigateTo(`/course/detail/${item.id}`)">继续学习</a>-->
<!--                    </template></a-list-item>-->
<!--                </template><template #empty>-->
<!--                  <div class="empty-data">暂无学习记录</div>-->
<!--                </template>-->
<!--              </a-list>-->
<!--            </a-tab-pane><a-tab-pane key="2" tab="最近培训"><a-list-->
<!--                :data-source="recentData.trains"-->
<!--                :loading="loading.trains"-->
<!--                item-layout="horizontal"-->
<!--              >-->
<!--                <template #renderItem="{ item }">-->
<!--                  <a-list-item>-->
<!--                    <a-list-item-meta-->
<!--                      :title="item.name"-->
<!--                      :description="`学习进度: ${item.progress}%`"-->
<!--                    >-->
<!--                      <template #avatar>-->
<!--                        <a-avatar><template #icon><solution-outlined /></template>-->
<!--                        </a-avatar>-->
<!--                      </template>-->
<!--                    </a-list-item-meta>-->
<!--                    <template #actions>-->
<!--                      <a @click="navigateTo(`/train/detail/${item.id}`)">继续学习</a>-->
<!--                    </template>-->
<!--                  </a-list-item>-->
<!--                </template>-->
<!--                <template #empty>-->
<!--                  <div class="empty-data">暂无培训记录</div>-->
<!--                </template>-->
<!--              </a-list>-->
<!--            </a-tab-pane>-->
<!--            <a-tab-pane key="3" tab="最近地图">-->
<!--              <a-list-->
<!--                :data-source="recentData.maps"-->
<!--                :loading="loading.maps"-->
<!--                item-layout="horizontal"-->
<!--              >-->
<!--                <template #renderItem="{ item }">-->
<!--                  <a-list-item>-->
<!--                    <a-list-item-meta-->
<!--                      :title="item.name"-->
<!--                      :description="`学习进度: ${item.progress}%`"-->
<!--                    >-->
<!--                      <template #avatar>-->
<!--                        <a-avatar>-->
<!--                          <template #icon><cluster-outlined /></template>-->
<!--                        </a-avatar>-->
<!--                      </template>-->
<!--                    </a-list-item-meta>-->
<!--                    <template #actions><a @click="navigateTo(`/map/detail/${item.id}`)">继续学习</a>-->
<!--                    </template>-->
<!--                  </a-list-item>-->
<!--                </template>-->
<!--                <template #empty>-->
<!--                  <div class="empty-data">暂无地图学习记录</div>-->
<!--                </template>-->
<!--              </a-list>-->
<!--            </a-tab-pane>-->
<!--          </a-tabs>-->
<!--        </div>-->
      </a-layout-content>
    </a-layout>

    <!-- 类目管理弹窗 -->
    <category-manage-page v-model:visible="categoryModalVisible" @refresh="fetchCategories" />
  </a-layout>
</template>

<script>
import { ref, reactive, onMounted, defineComponent } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import request from '@/utils/request';
import { hasPermission } from '@/utils/permission';
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
  PlusOutlined
} from '@ant-design/icons-vue';

export default {
  name: 'HomePage',
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
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const collapsed = ref(false);
    const selectedKeys = ref(['dashboard']);
    const openKeys = ref(['dashboard']);
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




    // 加载状态
    const loading = reactive({
      userInfo: false,
      statistics: false,
      courses: false,
      trains: false,
      maps: false,
    });

    // 当前日期
    const currentDate = dayjs().format('YYYY年MM月DD日');

    // 获取用户信息
    const getUserInfo = async () => {
      loading.userInfo = true;
      try {
        const res = await request.get('/api/user/info');
        if (res.code === 200 && res.data) {
          userInfo.value = res.data;
          // 存储用户信息到sessionStorage
          sessionStorage.setItem('userInfo', JSON.stringify(res.data));
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
      } finally {
        loading.userInfo = false;
      }
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



    onMounted(() => {
      // 获取用户信息
      getUserInfo();


    });

    return {
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      loading,
      currentDate,
      categoryModalVisible,

      hasPermission,
      handleLogout,
      navigateTo,
      showCategoryModal
    };
  }
};
</script>

<style scoped>
.home-layout {
  min-height: 100vh;
}

.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: auto;
}

.welcome-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.welcome-section h2 {
  margin-top: 0;
  color: #1890ff;
}

.banner-section {
  margin-bottom: 24px;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.banner-slide {
  height: 300px;
  position: relative;
  overflow: hidden;
}

.banner-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-title {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
  padding: 10px 20px;
  font-size: 16px;
}

.empty-banner {
  background-color: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-banner-text {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.45);
}

.statistics-section {
  margin-bottom: 24px;
}

.card-icon {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 120px;
  font-size: 48px;
}

.course-icon {
  background-color: #e6f7ff;
  color: #1890ff;
}

.train-icon {
  background-color: #f6ffed;
  color: #52c41a;
}

.map-icon {
  background-color: #fff7e6;
  color: #fa8c16;
}

.certificate-icon {
  background-color: #fff1f0;
  color: #f5222d;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #333;
  margin-bottom: 12px;
  text-align: center;
}

.card-footer {
  text-align: center;
  margin-top: 12px;
}

.shortcuts-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
}

.shortcuts-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
}

.recent-section {
  background-color: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.empty-data {
  text-align: center;
  padding: 24px 0;
  color: rgba(0, 0, 0, 0.45);
}
</style>
