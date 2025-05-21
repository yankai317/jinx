
<template>
  <div class="mobile-header-component" :class="{ 'has-fixed-header': isHeaderFixed }">
    <!-- Logo 和搜索框区域 -->
    <div v-if="showLogoSearch" class="logo-search-container" :class="{ 'fixed-search': isHeaderFixed }">
      <div v-if="showLogo" class="logo-container">
        <img class="header-logo" src="@/assets/logo-l.png" alt="Logo" />
      </div>
      <div v-if="showSearch" class="search-input-container" :class="{ 'expanded': isSearchExpanded }">
        <a-input-search
          v-model:value="searchKeyword"
          :placeholder="searchPlaceholder"
          @search="handleSearch"
          @focus="expandSearchInput"
          @blur="collapseSearchInput"
          @keyup.enter="handleSearch(searchKeyword)"
          :loading="searchLoading"
        >
          <template #prefix>
            <search-outlined />
          </template>
        </a-input-search>
      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div class="search-results-container" v-if="showSearchResults">
      <div class="search-filters" v-if="showSearchTypeFilter">
        <a-radio-group v-model:value="searchType" button-style="solid" size="small" @change="handleSearchTypeChange">
          <a-radio-button value="all">全部</a-radio-button>
          <a-radio-button value="COURSE">课程</a-radio-button>
          <a-radio-button value="TRAIN">培训</a-radio-button>
          <a-radio-button value="LEARNING_MAP">学习地图</a-radio-button>
        </a-radio-group>
      </div>

      <div class="search-results-list" v-if="searchResults.length > 0">
        <div
          v-for="item in searchResults"
          :key="item.id"
          class="search-result-item"
          @click="navigateToDetail(item)"
        >
          <div class="result-cover">
            <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" />
            <div v-else class="cover-placeholder">
              <file-image-outlined />
            </div>
          </div>
          <div class="result-content">
            <h3 class="result-title">{{ item.title }}</h3>
            <div class="result-info">
              <div class="info-item">
                <tag-outlined />
                <span>{{ getTypeText(item.type) }}</span>
              </div>
              <div class="info-item" v-if="item.credit">
                <trophy-outlined />
                <span>{{ item.credit }} 学分</span>
              </div>
            </div>
          </div>
        </div>

        <div class="search-more" @click="goToSearch">
          查看更多搜索结果 <right-outlined />
        </div>
      </div>
      <div class="search-empty" v-else>
        <empty-outlined />
        <p>未找到相关内容</p>
        <p class="empty-desc">尝试使用其他关键词进行搜索</p>
        <a-button type="primary" size="small" @click="closeSearchResults">返回</a-button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  LeftOutlined,
  RightOutlined,
  SearchOutlined,
  FileImageOutlined,
  TagOutlined,
  TrophyOutlined,
  EmptyOutlined
} from '@ant-design/icons-vue';
import { searchContent } from '@/api/toc/search';

export default {
  name: 'MobileHeader',
  components: {
    LeftOutlined,
    RightOutlined,
    SearchOutlined,
    FileImageOutlined,
    TagOutlined,
    TrophyOutlined,
    EmptyOutlined
  },
  props: {
    // 标题
    title: {
      type: String,
      default: '主页'
    },
    // 是否显示返回按钮
    showBackIcon: {
      type: Boolean,
      default: true
    },
    // 是否显示Logo和搜索区域
    showLogoSearch: {
      type: Boolean,
      default: true
    },
    // 是否显示Logo
    showLogo: {
      type: Boolean,
      default: true
    },
    // Logo URL
    logoUrl: {
      type: String,
      default: 'https://hsfdkhdfk.oss-cn-hangzhou.aliyuncs.com/imtime_log2.png'
    },
    // 是否显示搜索框
    showSearch: {
      type: Boolean,
      default: true
    },
    // 搜索框占位符
    searchPlaceholder: {
      type: String,
      default: '搜索课程/培训/学习地图'
    },
    // 是否显示搜索类型过滤器
    showSearchTypeFilter: {
      type: Boolean,
      default: true
    },
    // 是否启用吸顶效果
    sticky: {
      type: Boolean,
      default: true
    }
  },
  emits: ['search', 'search-type-change', 'navigate-to-detail'],
  setup(props, { emit }) {
    const router = useRouter();

    // 搜索相关数据
    const searchKeyword = ref('');
    const searchType = ref('all');
    const searchResults = ref([]);
    const searchLoading = ref(false);
    const showSearchResults = ref(false);
    const isSearchExpanded = ref(false);

    // 控制吸顶效果的变量
    const scrollTop = ref(0);
    const isHeaderFixed = ref(false);

    // 滚动事件处理函数，更新scrollTop值并控制吸顶状态
    const handleScroll = () => {
      scrollTop.value = window.pageYOffset || document.documentElement.scrollTop;

      // 当滚动距离超过一定值时（例如100px），设置header为固定状态
      if (scrollTop.value > 100) {
        isHeaderFixed.value = true;
      } else {
        isHeaderFixed.value = false;
      }
    };

    // 组件挂载时添加滚动事件监听
    onMounted(() => {
      window.addEventListener('scroll', handleScroll);
      // 初始化时执行一次
      handleScroll();
    });

    // 组件卸载时移除滚动事件监听
    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll);
    });

    // 返回上一页
    const goBack = () => {
      router.go(-1);
    };

    // 处理搜索操作
    const handleSearch = async (value) => {
      if (!value.trim()) {
        message.warning('请输入搜索关键词');
        return;
      }

      searchLoading.value = true;
      showSearchResults.value = true;

      try {
        const params = {
          keyword: value,
          type: searchType.value,
          pageNum: 1,
          pageSize: 5 // 移动端只显示少量结果
        };

        // 触发搜索事件
        emit('search', params);

        const res = await searchContent(params);
        if (res.code === 200) {
          searchResults.value = res.data.list || [];
        } else {
          message.error(res.message || '搜索失败');
          searchResults.value = [];
        }
      } catch (error) {
        console.error('搜索异常:', error);
        searchResults.value = [];
      } finally {
        searchLoading.value = false;
      }
    };

    // 处理搜索类型变化
    const handleSearchTypeChange = () => {
      emit('search-type-change', searchType.value);

      if (searchKeyword.value.trim()) {
        handleSearch(searchKeyword.value);
      }
    };

    // 关闭搜索结果
    const closeSearchResults = () => {
      showSearchResults.value = false;
      searchKeyword.value = '';
      isSearchExpanded.value = false;
    };

    // 展开搜索框
    const expandSearchInput = () => {
      isSearchExpanded.value = true;
    };

    // 收起搜索框
    const collapseSearchInput = () => {
      // 如果没有输入内容，则收起搜索框
      if (!searchKeyword.value.trim() && !showSearchResults.value) {
        isSearchExpanded.value = false;
      }
    };

    // 获取类型文本
    const getTypeText = (type) => {
      switch (type) {
        case 'COURSE':
          return '课程';
        case 'TRAIN':
          return '培训';
        case 'LEARNING_MAP':
          return '学习地图';
        case 'EXAM':
          return '考试';
        default:
          return '内容';
      }
    };

    // 跳转到搜索页面
    const goToSearch = () => {
      if (searchKeyword.value.trim()) {
        router.push({
          path: '/toc/mobile/search',
          query: {
            q: searchKeyword.value,
            type: searchType.value
          }
        });
      } else {
        router.push('/toc/mobile/search');
      }
    };

    // 导航到详情页
    const navigateToDetail = (item) => {
      if (!item || !item.id) return;

      emit('navigate-to-detail', item);

      let route = '';
      if (item.type === 'COURSE') {
        route = `/toc/mobile/course/${item.id}`;
      } else if (item.type === 'LEARNING_MAP') {
        route = `/toc/mobile/map/${item.id}`;
      } else if (item.type === 'EXAM') {
        // 假设考试详情页路由
        route = `/toc/mobile/exam/${item.id}`;
      }

      if (route) {
        router.push(route);
      }
    };

    return {
      searchKeyword,
      searchType,
      searchResults,
      searchLoading,
      showSearchResults,
      isSearchExpanded,
      isHeaderFixed, // 添加吸顶状态变量
      goBack,
      handleSearch,
      handleSearchTypeChange,
      closeSearchResults,
      expandSearchInput,
      collapseSearchInput,
      getTypeText,
      goToSearch,
      navigateToDetail,
      onUnmounted
    };
  }
};
</script>

<style scoped>
.mobile-header-component {
  position: relative;
  z-index: 100;
  width: 100%;
}

/* 当header固定时，为组件添加上边距，避免内容被遮挡 */
.mobile-header-component.has-fixed-header {
  padding-top: 120px; /* 顶部导航栏(56px) + logo搜索区域(约64px) 的总高度 */
}

/* 顶部导航栏 - 吸顶效果 */
.mobile-header {
  height: 56px;
  background-color: #FFFFFF;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: -webkit-sticky !important; /* Safari */
  position: sticky !important; /* 使用!important确保吸顶效果不被其他样式覆盖 */
  top: 0 !important; /* 确保始终吸附在顶部 */
  z-index: 1000; /* 提高z-index确保在最上层 */
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15) !important; /* 增强阴影效果 */
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background-color: rgba(255, 255, 255, 0.98);
  padding: 0 16px;
  width: 100%;
  left: 0;
  right: 0;
  transition: all 0.3s ease;
}

.header-left, .header-right {
  width: 40px;
  display: flex;
  align-items: center;
}

.back-icon {
  font-size: 18px;
  color: #000000;
  cursor: pointer;
}

.header-title {
  font-size: 16px;
  font-weight: 500;
  color: #000000;
  text-align: center;
  flex: 1;
}

/* Logo 和搜索框区域 - 与顶部导航栏协同吸顶 */
.logo-search-container {
  padding: 12px 16px;
  background-color: rgba(255, 255, 255, 0.98);
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15) !important; /* 增强阴影效果 */
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: -webkit-sticky !important; /* Safari */
  position: sticky !important; /* 使用!important确保吸顶效果不被其他样式覆盖 */
  top: 0px !important; /* 顶部导航栏的高度，确保始终在导航栏下方吸附 */
  z-index: 999; /* 比顶部导航栏低一级，但仍然很高 */
  width: 100%;
  left: 0;
  right: 0;
  transition: all 0.3s ease;
}

.logo-container {
  display: flex;
  align-items: center;
}

.header-logo {
  height: 32px;
  width: auto;
}

.search-input-container {
  width: 120px;
  transition: width 0.3s ease;
}

.search-input-container.expanded {
  width: 70%;
}

/* 搜索结果区域 */
.search-results-container {
  background-color: #FFFFFF;
  padding: 16px;
  min-height: calc(100vh - 120px);
  position: fixed;
  top: 56px;
  left: 0;
  right: 0;
  z-index: 998; /* 低于header和search区域，但高于其他内容 */
  width: 100%;
  overflow-y: auto;
  max-height: calc(100vh - 120px);
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
}

.search-filters {
  margin-bottom: 16px;
  overflow-x: auto;
  white-space: nowrap;
  padding-bottom: 8px;
}

.search-results-list {
  display: flex;
  flex-direction: column;
}

.search-result-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #E5E7EB;
}

.search-result-item:last-child {
  border-bottom: none;
}

.result-cover {
  width: 80px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  background-color: #E5E7EB;
  margin-right: 12px;
  flex-shrink: 0;
}

.result-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9CA3AF;
  font-size: 24px;
}

.result-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.result-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 500;
  color: #000000;
  line-height: 1.3;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.result-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #6B7280;
}

.info-item .anticon {
  margin-right: 4px;
  font-size: 12px;
}

.search-more {
  text-align: center;
  color: #1890FF;
  padding: 12px 0;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  color: #9CA3AF;
}

.search-empty .anticon {
  font-size: 40px;
  margin-bottom: 12px;
}

.search-empty p {
  margin: 0;
  font-size: 14px;
}

.search-empty .empty-desc {
  font-size: 12px;
  margin: 4px 0 16px;
}

/*
  固定吸顶样式，当滚动到一定距离后应用
*/
.fixed-header {
  position: fixed !important;
  top: 0 !important;
  left: 0;
  right: 0;
  width: 100%;
  z-index: 1001;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15) !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background-color: rgba(255, 255, 255, 0.98);
  animation: slideDown 0.3s ease;
}

.fixed-search {
  position: fixed !important;
  top: 0px !important; /* 顶部导航栏的高度 */
  left: 0;
  right: 0;
  width: 100%;
  z-index: 1000;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15) !important;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background-color: rgba(255, 255, 255, 0.98);
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 保留旧类名以保持向后兼容性 */
.sticky-header {
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background-color: rgba(255, 255, 255, 0.98);
}

.sticky-search {
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background-color: rgba(255, 255, 255, 0.98);
}
</style>
