<template>
  <div class="rankings-mobile-page">
    <div class="main-content">
      <!-- 排行榜标签页 -->
      <div class="rankings-tabs">
        <a-tabs v-model:activeKey="activeTabKey" @change="handleTabChange">
          <a-tab-pane key="all" tab="全员排行榜"></a-tab-pane>
          <a-tab-pane key="department" tab="部门排行榜"></a-tab-pane>
        </a-tabs>
      </div>

      <!-- 部门选择器 -->
      <div class="department-selector" v-if="activeTabKey === 'department'">
        <a-select
          v-model:value="selectedDepartmentId"
          placeholder="请选择部门"
          style="width: 100%"
          @change="handleDepartmentChange"
          :loading="departmentsLoading"
        >
          <a-select-option v-for="dept in departments" :key="dept.id" :value="dept.id">
            {{ dept.name }}
          </a-select-option>
        </a-select>
      </div>

      <!-- 排行榜内容 -->
      <div class="rankings-content">
        <!-- 用户自己的排名信息 -->
        <div class="user-rank-card" v-if="rankingsData.userRank">
          <div class="user-rank-title">
            <span>我的排名</span>
          </div>
          <div class="user-rank-info">
            <div class="rank-number">
              <span class="rank-label">当前排名</span>
              <span class="rank-value">{{ rankingsData.userRank.rank }}</span>
            </div>
            <div class="rank-credit">
              <span class="rank-label">学习积分</span>
              <span class="rank-value">{{ rankingsData.userRank.credit }}</span>
            </div>
          </div>
        </div>

        <!-- 排行榜列表 -->
        <div class="rankings-list">
          <a-spin :spinning="loading">
            <div v-if="rankingsData.list && rankingsData.list.length > 0">
              <div
                v-for="(item, index) in rankingsData.list"
                :key="index"
                class="rank-item"
              >
                <div class="rank-avatar-container">
                  <span class="rank-number" :class="getRankClass(item.rank)">{{ item.rank }}</span>
                  <a-avatar :src="item.avatar" :size="40">
                    {{ item.nickname ? item.nickname.substring(0, 1) : 'U' }}
                  </a-avatar>
                </div>
                <div class="rank-user-info">
                  <div class="user-info-top">
                    <span class="user-name">{{ item.nickname }}</span>
                    <span class="user-credit">{{ item.credit }} 积分</span>
                  </div>
                  <div class="user-department">{{ item.department }}</div>
                </div>
              </div>
            </div>
            <EmptyState
              v-else
              title="暂无排行数据"
              description="暂时没有相关的排行榜数据"
            />
          </a-spin>
        </div>
      </div>
    </div>

    <!-- 底部导航栏 -->
    <MobileTabBar active="learning" />
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { getRankings } from '@/api/toc/rankings';
import { getDepartments } from '@/api/org';
import { message } from 'ant-design-vue';

export default defineComponent({
  name: 'RankingsMobilePage',
  components: {
    MobileTabBar,
    EmptyState
  },
  setup() {
    // 状态定义
    const loading = ref(false);
    const departmentsLoading = ref(false);
    const activeTabKey = ref('all');
    const selectedDepartmentId = ref(null);
    const departments = ref([]);
    const rankingsData = reactive({
      type: 'all',
      list: [],
      userRank: null
    });

    // 获取排行榜数据
    const fetchRankings = async (type = 'all', departmentId = null) => {
      loading.value = true;
      try {
        const params = {
          type,
          limit: 20 // 移动端显示更多条目
        };

        if (type === 'department' && departmentId) {
          params.departmentId = departmentId;
        }

        const response = await getRankings(params);
        if (response && response.code === 200) {
          rankingsData.type = response.data.type;
          rankingsData.list = response.data.list || [];
          rankingsData.userRank = response.data.userRank;
        } else {
          message.error(response?.message || '获取排行榜数据失败');
        }
      } catch (error) {
        console.error('获取排行榜数据出错:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取部门列表
    const fetchDepartments = async () => {
      departmentsLoading.value = true;
      try {
        const response = await getDepartments();
        if (response && response.code === 200) {
          departments.value = response.data || [];

          // 如果有部门数据，默认选择第一个
          if (departments.value.length > 0) {
            selectedDepartmentId.value = departments.value[0].id;
            fetchRankings('department', selectedDepartmentId.value);
          }
        } else {
          message.error(response?.message || '获取部门列表失败');
        }
      } catch (error) {
        console.error('获取部门列表出错:', error);
      } finally {
        departmentsLoading.value = false;
      }
    };

    // 处理标签页切换
    const handleTabChange = (key) => {
      activeTabKey.value = key;
      if (key === 'department') {
        // 如果切换到部门排行榜，需要先获取部门列表
        if (departments.value.length === 0) {
          fetchDepartments();
        } else if (selectedDepartmentId.value) {
          // 如果已经选择了部门，则获取该部门的排行榜数据
          fetchRankings('department', selectedDepartmentId.value);
        }
      } else {
        // 获取全员排行榜数据
        fetchRankings('all');
      }
    };

    // 处理部门选择变化
    const handleDepartmentChange = (value) => {
      selectedDepartmentId.value = value;
      fetchRankings('department', value);
    };

    // 获取排名样式类
    const getRankClass = (rank) => {
      if (rank === 1) return 'rank-first';
      if (rank === 2) return 'rank-second';
      if (rank === 3) return 'rank-third';
      return '';
    };

    // 组件挂载时获取全员排行榜数据
    onMounted(() => {
      fetchRankings();
    });

    return {
      loading,
      departmentsLoading,
      activeTabKey,
      selectedDepartmentId,
      departments,
      rankingsData,
      handleTabChange,
      handleDepartmentChange,
      getRankClass
    };
  }
});
</script>

<style scoped>
.rankings-mobile-page {
  padding-bottom: 65px;
}

.main-content {
  padding: 16px;
}

.rankings-tabs {
  margin-bottom: 16px;
}

.department-selector {
  margin-bottom: 16px;
}

.user-rank-card {
  background-color: #f0f7ff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.user-rank-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 12px;
  color: #1890ff;
}

.user-rank-info {
  display: flex;
  justify-content: space-between;
}

.rank-number,
.rank-credit {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rank-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.rank-value {
  font-size: 24px;
  font-weight: 500;
  color: #1890ff;
}

.rankings-list {
  background-color: #fff;
  border-radius: 8px;
  padding: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-avatar-container {
  position: relative;
  margin-right: 16px;
}

.rank-number {
  position: absolute;
  top: -8px;
  left: -8px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: #d9d9d9;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-number.rank-first {
  background-color: #f5a623;
}

.rank-number.rank-second {
  background-color: #a0a0a0;
}

.rank-number.rank-third {
  background-color: #cd7f32;
}

.rank-user-info {
  flex: 1;
}

.user-info-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
}

.user-credit {
  font-size: 14px;
  color: #1890ff;
}

.user-department {
  font-size: 12px;
  color: #666;
}
</style>