<template>
  <div class="search-mobile-page">
    <!-- 搜索区域 -->
    <div class="search-container">
      <div class="search-input-wrapper">
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索课程/培训/学习地图"
          enter-button
          @search="handleSearch"
        />
      </div>
      <div class="search-filters">
        <a-radio-group v-model:value="searchType" @change="handleTypeChange" size="small">
          <a-radio-button value="all">全部</a-radio-button>
          <a-radio-button value="COURSE">课程</a-radio-button>
          <a-radio-button value="TRAIN">培训</a-radio-button>
          <a-radio-button value="LEARNING_MAP">学习地图</a-radio-button>
        </a-radio-group>
      </div>
    </div>

    <!-- 搜索结果区域 -->
    <div class="search-results">
      <div class="search-stats" v-if="total > 0">
        找到 {{ total }} 条与 "{{ keyword }}" 相关的结果
      </div>

      <a-spin :spinning="loading">
        <!-- 搜索结果列表 -->
        <div class="result-list" v-if="searchResults.length > 0">
          <div
            v-for="item in searchResults"
            :key="item.id"
            class="result-item"
            @click="navigateToDetail(item)"
          >
            <div class="result-cover">
              <img v-if="item.coverImage" :src="item.coverImage" :alt="item.title" />
              <div v-else class="cover-placeholder">
                <file-image-outlined />
              </div>
            </div>
            <div class="result-content">
              <h3 class="result-title" v-html="highlightKeyword(item.title, keyword)"></h3>
              <p class="result-description" v-html="highlightKeyword(item.description, keyword)"></p>
              <div class="result-meta">
                <span class="result-type">{{ getTypeText(item.type) }}</span>
                <span class="result-credit" v-if="item.credit">{{ item.credit }} 学分</span>
                <span class="result-date" v-if="item.createdAt">{{ formatDate(item.createdAt) }}</span>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <div class="pagination-container" v-if="total > 0">
            <a-pagination
              v-model:current="pageNum"
              :total="total"
              :pageSize="pageSize"
              @change="handlePageChange"
              simple
            />
          </div>
        </div>

        <!-- 空状态展示 -->
        <EmptyState
          v-if="total === 0 && !isFirstSearch"
          title="未找到相关内容"
          description="尝试使用其他关键词或筛选条件进行搜索"
        >
          <template #action>
            <a-button type="primary" @click="goToLearningCenter">
              浏览学习中心
            </a-button>
          </template>
        </EmptyState>

        <!-- 首次访问提示 -->
        <EmptyState
          v-if="isFirstSearch"
          title="搜索课程、培训和学习地图"
          description="输入关键词开始搜索"
        >
          <template #icon>
            <search-outlined style="font-size: 48px; color: #d1d5db;" />
          </template>
        </EmptyState>
      </a-spin>
    </div>

    <!-- 底部导航栏 -->
    <MobileTabBar active="learning" />
  </div>
</template>

<script>
import { defineComponent, ref, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { searchContent } from '@/api/toc/search';
import {
  SearchOutlined,
  FileImageOutlined
} from '@ant-design/icons-vue';
import dayjs from 'dayjs';

export default defineComponent({
  name: 'SearchMobilePage',
  components: {
    MobileTabBar,
    EmptyState,
    SearchOutlined,
    FileImageOutlined
  },
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 搜索参数
    const keyword = ref('');
    const searchType = ref('all');
    const pageNum = ref(1);
    const pageSize = ref(10);
    const isFirstSearch = ref(true);
    const loading = ref(false);

    // 搜索结果
    const searchResults = ref([]);
    const total = ref(0);

    // 从URL参数中获取搜索条件
    onMounted(() => {
      const { q, type, page } = route.query;
      if (q) {
        keyword.value = q;
        searchType.value = type || 'all';
        pageNum.value = parseInt(page) || 1;
        fetchSearchResults();
      }
    });

    // 监听路由变化
    watch(
      () => route.query,
      (newQuery) => {
        const { q, type, page } = newQuery;
        if (q && q !== keyword.value) {
          keyword.value = q;
          searchType.value = type || 'all';
          pageNum.value = parseInt(page) || 1;
          fetchSearchResults();
        }
      }
    );

    // 执行搜索
    const fetchSearchResults = async () => {
      if (!keyword.value) return;

      try {
        loading.value = true;
        isFirstSearch.value = false;

        const params = {
          keyword: keyword.value,
          type: searchType.value,
          pageNum: pageNum.value,
          pageSize: pageSize.value
        };

        const res = await searchContent(params);
        if (res.code === 200) {
          searchResults.value = res.data.list || [];
          total.value = res.data.total || 0;
        } else {
          message.error(res.message || '搜索失败');
        }
      } catch (error) {
        console.error('搜索异常:', error);
      } finally {
        loading.value = false;
      }
    };

    // 搜索按钮点击
    const handleSearch = () => {
      if (!keyword.value.trim()) {
        message.warning('请输入搜索关键词');
        return;
      }

      pageNum.value = 1; // 重置页码

      // 更新URL参数
      router.push({
        path: '/toc/mobile/search',
        query: {
          q: keyword.value,
          type: searchType.value,
          page: pageNum.value
        }
      });

      fetchSearchResults();
    };

    // 内容类型切换
    const handleTypeChange = () => {
      pageNum.value = 1; // 重置页码

      // 更新URL参数
      router.push({
        path: '/toc/mobile/search',
        query: {
          q: keyword.value,
          type: searchType.value,
          page: pageNum.value
        }
      });

      fetchSearchResults();
    };

    // 页码变化
    const handlePageChange = (page) => {
      pageNum.value = page;

      // 更新URL参数
      router.push({
        path: '/toc/mobile/search',
        query: {
          q: keyword.value,
          type: searchType.value,
          page: pageNum.value
        }
      });

      fetchSearchResults();
    };

    // 高亮关键词
    const highlightKeyword = (text, keyword) => {
      if (!text || !keyword) return text;

      // 转义正则表达式中的特殊字符
      const escapeRegExp = (string) => {
        return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      };

      const regex = new RegExp(`(${escapeRegExp(keyword)})`, 'gi');
      return text.replace(regex, '<span class="highlight-keyword">$1</span>');
    };

    // 获取内容类型文本
    const getTypeText = (type) => {
      const typeMap = {
        'COURSE': '课程',
        'TRAIN': '培训',
        'LEARNING_MAP': '学习地图',
        'EXAM': '考试'
      };
      return typeMap[type] || type;
    };

    // 格式化日期
    const formatDate = (dateStr) => {
      if (!dateStr) return '';
      return dayjs(dateStr).format('YYYY-MM-DD');
    };

    // 导航到详情页
    const navigateToDetail = (item) => {
      const typeRouteMap = {
        'COURSE': `/toc/mobile/course/${item.id}`,
        'TRAIN': `/toc/mobile/train/${item.id}`,
        'LEARNING_MAP': `/toc/mobile/map/${item.id}`,
        'EXAM': `/toc/mobile/exam/${item.id}`
      };

      const route = typeRouteMap[item.type];
      if (route) {
        router.push(route);
      } else {
        message.warning('暂不支持查看此类型的内容');
      }
    };

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/mobile/learning/center');
    };

    return {
      keyword,
      searchType,
      pageNum,
      pageSize,
      isFirstSearch,
      loading,
      searchResults,
      total,
      handleSearch,
      handleTypeChange,
      handlePageChange,
      highlightKeyword,
      getTypeText,
      formatDate,
      navigateToDetail,
      goToLearningCenter
    };
  }
});
</script>

<style scoped>
.search-mobile-page {
  padding-bottom: 65px;
}

.search-container {
  padding: 16px;
  background-color: #fff;
}

.search-input-wrapper {
  margin-bottom: 16px;
}

.search-filters {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.search-results {
  padding: 0 16px 16px;
}

.search-stats {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-item {
  display: flex;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.result-cover {
  width: 100px;
  height: 100px;
  flex-shrink: 0;
}

.result-cover img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #999;
}

.cover-placeholder .anticon {
  font-size: 24px;
}

.result-content {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
}

.result-title {
  font-size: 16px;
  font-weight: 500;
  margin: 0 0 8px 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.result-description {
  font-size: 14px;
  color: #666;
  margin: 0 0 8px 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: auto;
}

.result-type,
.result-credit,
.result-date {
  font-size: 12px;
  color: #999;
  padding: 2px 8px;
  background-color: #f5f5f5;
  border-radius: 10px;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.highlight-keyword {
  color: #1890ff;
  font-weight: 500;
}
</style>