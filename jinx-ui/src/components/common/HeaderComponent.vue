
<template>
  <div class="toc-header">
    <div class="header-content">
      <!-- Logo 和系统名称 -->
      <div class="logo-container">
        <img src="@/assets/logo.png" alt="系统Logo" />
        <div class="logo-text">
          <span class="company-name">银泰商业集团</span>
          <span class="platform-name">数智化学习平台</span>
        </div>
      </div>

      <!-- 导航菜单 -->
      <nav class="nav-menu">
        <router-link to="/toc/home" class="nav-item" :class="{ active: activeKey==='HOME' }">首页</router-link>
        <router-link to="/toc/learning/center" class="nav-item" :class="{ active: activeKey==='LEARNING_CENTER' }">学习中心</router-link>
        <router-link to="/toc/personal/center" class="nav-item" :class="{ active: activeKey==='PERSONAL_CENTER' }">个人中心</router-link>
        <router-link to="/home" class="nav-item" :class="{ active: activeKey==='MANAGEMENT_CENTER' }" v-if="managementCenterEntry">管理中心</router-link>
      </nav>

      <!-- 搜索框 -->
      <div class="search-container" v-if="showSearch" ref="searchContainerRef">
        <input
          type="text"
          class="search-input"
          placeholder="搜索课程/地图/任务/考试"
          @keyup.enter="handleSearch"
          @input="handleInput"
          @focus="showSearchResults = true"
          @blur="handleBlur"
          v-model="searchKeyword"
        />
        <div class="search-icon" @click="handleSearch">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M13 9.5Q12.9688 7.3125 11.75 5.65625L15.7188 1.71875Q16 1.40625 16 1Q16 0.59375 15.7188 0.28125Q15.4062 0 15 0Q14.5938 0 14.2812 0.28125L10.3438 4.25Q8.6875 3.03125 6.5 3Q3.75 3.0625 1.90625 4.90625Q0.0625 6.75 0 9.5Q0.0625 12.25 1.90625 14.0938Q3.75 15.9375 6.5 16Q9.25 15.9375 11.0938 14.0938Q12.9375 12.25 13 9.5ZM6.5 5Q7.71875 5 8.75 5.59375Q9.78125 6.1875 10.4062 7.25Q11 8.3125 11 9.5Q11 10.6875 10.4062 11.75Q9.78125 12.8125 8.75 13.4062Q7.71875 14 6.5 14Q5.28125 14 4.25 13.4062Q3.21875 12.8125 2.59375 11.75Q2 10.6875 2 9.5Q2 8.3125 2.59375 7.25Q3.21875 6.1875 4.25 5.59375Q5.28125 5 6.5 5Z" fill="#1890FF"/>
          </svg>
        </div>

        <!-- 搜索结果下拉框 -->
        <div class="search-results" v-if="showSearchResults && searchResults.length > 0">
          <div class="search-results-header">
            <span>搜索结果</span>
            <a @click="viewAllResults">查看全部</a>
          </div>
          <div class="search-results-list">
            <div
              v-for="item in searchResults"
              :key="item.id"
              class="search-result-item"
              @click="handleResultClick(item)"
            >
              <div class="result-type" :class="getTypeClass(item.type)">{{ getTypeText(item.type) }}</div>
              <div class="result-content">
                <div class="result-title">{{ item.title }}</div>
                <div class="result-category" v-if="item.categoryNames">{{ item.categoryNames }}</div>
                <div class="result-description" v-if="item.description">{{ truncateText(item.description, 50) }}</div>
                <div class="result-info" v-if="item.credit || item.viewCount">
                  <span v-if="item.credit" class="result-credit">{{ item.credit }} 学分</span>
                  <span v-if="item.viewCount" class="result-view-count">{{ item.viewCount }} 次查看</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 搜索中加载状态 -->
        <div class="search-loading" v-if="isSearching && searchKeyword">
          <div class="loading-spinner"></div>
          <span>搜索中...</span>
        </div>
      </div>
      <!-- 占位，保证样式一致 -->
      <div class="search-container" v-else>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { searchContent } from '@/api/toc/search';

export default defineComponent({
  name: 'HeaderComponent',
  props: {
    activeKey: {
      type: [String, Number, Array],
      default: 'HOME',
      required: true,
      validator: (value) => {
        return ['HOME', 'LEARNING_CENTER', 'PERSONAL_CENTER', 'MANAGEMENT_CENTER'].includes(value);
      }
    },
    showSearch: {
      type: Boolean,
      default: true
    }
  },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const searchContainerRef = ref(null);

    const searchKeyword = ref('');
    const searchResults = ref([]);
    const showSearchResults = ref(false);
    const isSearching = ref(false);
    const searchTimeout = ref(null);

    const managementCenterEntry = computed(() => {
      try {
        const permissions = JSON.parse(sessionStorage.getItem('permissions'));
        return permissions.length > 0;
      } catch (error) {
        return false;
      }
    });

    // 判断当前是否在个人中心相关页面
    const isPersonalActive = computed(() => {
      return route.path.includes('/personal') ||
             route.path.includes('/learning/tasks') ||
             route.path.includes('/certificates');
    });

    // 处理搜索框输入
    const handleInput = () => {
      // 清除之前的定时器
      if (searchTimeout.value) {
        clearTimeout(searchTimeout.value);
      }

      // 如果搜索关键词为空，清空搜索结果
      if (!searchKeyword.value.trim()) {
        searchResults.value = [];
        isSearching.value = false;
        return;
      }

      // 设置搜索中状态
      isSearching.value = true;

      // 设置防抖定时器，300ms后执行搜索
      searchTimeout.value = setTimeout(() => {
        fetchSearchResults();
      }, 300);
    };

    // 获取搜索结果
    const fetchSearchResults = async () => {
      if (!searchKeyword.value.trim()) {
        searchResults.value = [];
        isSearching.value = false;
        return;
      }

      try {
        const params = {
          keyword: searchKeyword.value.trim(),
          type: 'all',
          pageNum: 1,
          pageSize: 20 // 获取更多搜索结果，以便全部显示
        };

        const res = await searchContent(params);
        if (res.code === 200) {
          searchResults.value = res.data.list || [];
        } else {
          console.error('搜索失败:', res.message);
          searchResults.value = [];
        }
      } catch (error) {
        console.error('搜索异常:', error);
        searchResults.value = [];
      } finally {
        isSearching.value = false;
      }
    };

    // 处理搜索按钮点击或回车
    const handleSearch = () => {
      if (searchKeyword.value.trim()) {
        router.push({
          path: '/toc/search',
          query: { q: searchKeyword.value.trim(), type: 'all', page: 1 }
        });
        showSearchResults.value = false;
      }
    };

    // 查看全部搜索结果
    const viewAllResults = () => {
      if (searchKeyword.value.trim()) {
        router.push({
          path: '/toc/search',
          query: { q: searchKeyword.value.trim(), type: 'all', page: 1 }
        });
        showSearchResults.value = false;
      }
    };

    // 处理搜索结果项点击
    const handleResultClick = (item) => {
      // 确保类型值是小写的，以处理可能的大小写不一致
      const normalizedType = item.type?.toLowerCase();

      const typeRouteMap = {
        'course': `/toc/course/detail/${item.id}`,
        'train': `/toc/train/detail/${item.id}`,
        'learning_map': `/toc/map/detail/${item.id}`,
        'map': `/toc/map/detail/${item.id}`, // 添加map类型的映射
        // 添加其他可能的类型路由映射
        'exam': `/toc/exam/detail/${item.id}`,
        'task': `/toc/task/detail/${item.id}`
      };

      const route = typeRouteMap[normalizedType];
      if (route) {
        router.push(route);
        showSearchResults.value = false;
        searchKeyword.value = ''; // 清空搜索框
      } else {
        console.warn('未找到匹配的路由:', normalizedType, item.id);
      }
    };

    // 处理失去焦点事件
    const handleBlur = (event) => {
      // 使用setTimeout延迟关闭，以便点击结果项时能够触发点击事件
      setTimeout(() => {
        showSearchResults.value = false;
      }, 200);
    };

    // 获取类型样式
    const getTypeClass = (type) => {
      // 确保类型值是小写的，以处理可能的大小写不一致
      const normalizedType = type?.toLowerCase();
      const typeMap = {
        'course': 'type-course',      // 课程 - 蓝色
        'train': 'type-train',        // 培训 - 绿色
        'learning_map': 'type-map',   // 学习地图 - 橙色
        'exam': 'type-exam',          // 考试 - 红色
        'task': 'type-task'           // 任务 - 紫色
      };
      return typeMap[normalizedType] || 'type-course'; // 默认为课程样式
    };

    // 获取类型文本
    const getTypeText = (type) => {
      // 确保类型值是小写的，以处理可能的大小写不一致
      const normalizedType = type?.toLowerCase();
      const typeMap = {
        'course': '课程',
        'train': '培训',
        'learning_map': '学习地图',
        'exam': '考试',
        'task': '任务'
      };
      return typeMap[normalizedType] || '课程'; // 默认为课程
    };

    // 截断文本
    const truncateText = (text, maxLength) => {
      if (!text) return '';
      return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
    };

    // 点击外部关闭搜索结果
    const handleClickOutside = (event) => {
      if (searchContainerRef.value && !searchContainerRef.value.contains(event.target)) {
        showSearchResults.value = false;
      }
    };

    // 监听点击事件
    onMounted(() => {
      document.addEventListener('click', handleClickOutside);
    });

    // 移除事件监听
    onUnmounted(() => {
      document.removeEventListener('click', handleClickOutside);
      if (searchTimeout.value) {
        clearTimeout(searchTimeout.value);
      }
    });

    return {
      searchKeyword,
      searchResults,
      showSearchResults,
      isSearching,
      isPersonalActive,
      searchContainerRef,
      handleSearch,
      handleInput,
      handleBlur,
      handleResultClick,
      viewAllResults,
      getTypeClass,
      getTypeText,
      truncateText,
      managementCenterEntry
    };
  }
});
</script>

<style scoped>
.toc-header {
  width: 100%;
  height: 64px;
  background-color: #FFFFFF;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1440px;
  height: 100%;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-container {
  display: flex;
  align-items: center;
}

.logo-container img {
  width: 34px;
  height: 34px;
  margin-right: 7px;
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.company-name {
  font-size: 14px;
  font-weight: 500;
  color: #3D3D3D;
  line-height: 18px;
}

.platform-name {
  font-size: 9px;
  font-weight: 500;
  color: #6B7280;
  line-height: 12px;
}

.nav-menu {
  display: flex;
  gap: 28px;
}

.nav-item {
  font-size: 18px;
  color: #000000;
  text-decoration: none;
  line-height: 24px;
}

.nav-item.active {
  color: #1890FF;
  font-weight: 700;
}

.search-container {
  position: relative;
  width: 320px;
}

.search-input {
  width: 100%;
  height: 40px;
  padding: 8px 40px 8px 16px;
  border: 1px solid #E5E7EB;
  border-radius: 4px;
  font-size: 16px;
  color: #000000;
  background-color: #FFFFFF;
}

.search-input::placeholder {
  color: #9CA3AF;
}

.search-icon {
  position: absolute;
  right: 12px;
  top: 10px;
  transform: rotate(90deg);
  cursor: pointer;
}

/* 搜索结果样式 */
.search-results {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  max-height: 400px;
  overflow-y: auto;
  background-color: #FFFFFF;
  border-radius: 4px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  margin-top: 4px;
  z-index: 200;
}

.search-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #E5E7EB;
}

.search-results-header span {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.search-results-header a {
  font-size: 14px;
  color: #1890FF;
  cursor: pointer;
}

.search-results-list {
  padding: 8px 0;
}

.search-result-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.search-result-item:hover {
  background-color: #F9FAFB;
}

.result-type {
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  color: #FFFFFF;
  margin-right: 12px;
  white-space: nowrap;
}

/* 搜索结果类型标签颜色 */
.type-course {
  background-color: #1890FF; /* 课程 - 蓝色 */
}

.type-train {
  background-color: #10B981; /* 培训 - 绿色 */
}

.type-map {
  background-color: #F59E0B; /* 学习地图 - 橙色 */
}

.type-exam {
  background-color: #EF4444; /* 考试 - 红色 */
}

.type-task {
  background-color: #8B5CF6; /* 任务 - 紫色 */
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-title {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-category {
  font-size: 12px;
  color: #4B5563;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-description {
  font-size: 12px;
  color: #6B7280;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 4px;
}

.result-info {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: #6B7280;
  font-size: 12px;
}

.result-credit {
  color: #1890FF;
}

/* 搜索加载状态 */
.search-loading {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  padding: 12px;
  background-color: #FFFFFF;
  border-radius: 4px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #E5E7EB;
  border-top-color: #1890FF;
  border-radius: 50%;
  margin-right: 8px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1200px) {
  .nav-menu {
    gap: 16px;
  }

  .search-container {
    width: 240px;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 16px 12px;
  }

  .nav-menu {
    display: none;
  }

  .search-container {
    width: 200px;
  }
}
</style>
