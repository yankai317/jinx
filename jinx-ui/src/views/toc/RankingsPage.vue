<template>
  <div class="rankings-page">
    <a-card :bordered="false" title="学习排行榜">
      <a-tabs v-model:activeKey="activeTabKey" @change="handleTabChange">
        <a-tab-pane key="all" tab="全员排行榜">
          <div class="rankings-content">
            <!-- 用户自己的排名信息 -->
            <a-card class="user-rank-card" v-if="rankingsData.userRank">
              <template #title>
                <div class="user-rank-title">
                  <span>我的排名</span>
                </div>
              </template>
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
            </a-card>

            <!-- 排行榜列表 -->
            <a-list
              class="rankings-list"
              :dataSource="rankingsData.list"
              :loading="loading"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #avatar>
                      <div class="rank-avatar-container">
                        <span class="rank-number" :class="getRankClass(item.rank)">{{ item.rank }}</span>
                        <a-avatar :src="item.avatar" :size="40">
                          {{ item.nickname ? item.nickname.substring(0, 1) : 'U' }}
                        </a-avatar>
                      </div>
                    </template>
                    <template #title>
                      <div class="user-info">
                        <span class="user-name">{{ item.nickname }}</span>
                        <span class="user-department">{{ item.department }}</span>
                      </div>
                    </template>
                    <template #description>
                      <div class="user-credit">
                        <span>学习积分: {{ item.credit }}</span>
                      </div>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
              <template #empty>
                <div class="empty-data">
                  <a-empty description="暂无排行数据" />
                </div>
              </template>
            </a-list>
          </div>
        </a-tab-pane>
        <a-tab-pane key="department" tab="部门排行榜">
          <div class="department-selector">
            <a-select
              v-model:value="selectedDepartmentId"
              placeholder="请选择部门"
              style="width: 200px"
              @change="handleDepartmentChange"
              :loading="departmentsLoading"
            >
              <a-select-option v-for="dept in departments" :key="dept.id" :value="dept.id">
                {{ dept.name }}
              </a-select-option>
            </a-select>
          </div>
          <div class="rankings-content">
            <!-- 用户自己的排名信息 -->
            <a-card class="user-rank-card" v-if="rankingsData.userRank">
              <template #title>
                <div class="user-rank-title">
                  <span>我的排名</span>
                </div>
              </template>
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
            </a-card>

            <!-- 排行榜列表 -->
            <a-list
              class="rankings-list"
              :dataSource="rankingsData.list"
              :loading="loading"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #avatar>
                      <div class="rank-avatar-container">
                        <span class="rank-number" :class="getRankClass(item.rank)">{{ item.rank }}</span>
                        <a-avatar :src="item.avatar" :size="40">
                          {{ item.nickname ? item.nickname.substring(0, 1) : 'U' }}
                        </a-avatar>
                      </div>
                    </template>
                    <template #title>
                      <div class="user-info">
                        <span class="user-name">{{ item.nickname }}</span>
                        <span class="user-department">{{ item.department }}</span>
                      </div>
                    </template>
                    <template #description>
                      <div class="user-credit">
                        <span>学习积分: {{ item.credit }}</span>
                      </div>
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
              <template #empty>
                <div class="empty-data">
                  <a-empty description="暂无排行数据" />
                </div>
              </template>
            </a-list>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue';
import { getRankings } from '@/api/toc/rankings';
import { getDepartments } from '@/api/org';

export default defineComponent({
  name: 'RankingsPage',
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
          limit: 10
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
          console.error('获取排行榜数据失败:', response);
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
          }
        } else {
          console.error('获取部门列表失败:', response);
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
        }
        
        // 如果已经选择了部门，则获取该部门的排行榜数据
        if (selectedDepartmentId.value) {
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
.rankings-page {
  padding: 24px;
}

.rankings-content {
  margin-top: 16px;
}

.user-rank-card {
  margin-bottom: 24px;
  background-color: #f5f7fa;
}

.user-rank-title {
  font-size: 16px;
  font-weight: 500;
}

.user-rank-info {
  display: flex;
  justify-content: space-around;
  padding: 16px 0;
}

.rank-number, .rank-credit {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.rank-label {
  font-size: 14px;
  color: #8c8c8c;
  margin-bottom: 8px;
}

.rank-value {
  font-size: 24px;
  font-weight: 500;
  color: #1890ff;
}

.rankings-list {
  background-color: #fff;
}

.rank-avatar-container {
  position: relative;
  display: flex;
  align-items: center;
}

.rank-number {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  background-color: #d9d9d9;
  color: #fff;
  margin-right: 8px;
}

.rank-first {
  background-color: #f5222d;
}

.rank-second {
  background-color: #fa8c16;
}

.rank-third {
  background-color: #faad14;
}

.user-info {
  display: flex;
  align-items: center;
}

.user-name {
  font-weight: 500;
  margin-right: 8px;
}

.user-department {
  font-size: 12px;
  color: #8c8c8c;
}

.user-credit {
  color: #1890ff;
  font-weight: 500;
}

.department-selector {
  margin-bottom: 16px;
}

.empty-data {
  padding: 32px 0;
  text-align: center;
}
</style>
