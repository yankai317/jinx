
<template>
  <div class="search-page">
    <!-- 顶部导航区域 -->
    <HeaderComponent activeKey="LEARNING_CENTER" />

    <!-- 搜索区域 -->
    <div class="search-container">
      <div class="search-header">
        <h1 class="search-title">搜索结果</h1>
        <div class="search-input-wrapper">
          <a-input-search
              v-model:value="keyword"
              placeholder="搜索课程/培训/学习地图"
              enter-button
              size="large"
              @search="handleSearch"
          />
        </div>
        <div class="search-filters">
          <a-radio-group v-model:value="searchType" @change="handleTypeChange">
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
        </div><a-row :gutter="[24, 24]">
        <a-col :span="8" v-for="item in searchResults" :key="item.id">
          <course-card :item="convertToCardItem(item)" />
        </a-col></a-row>

        <!-- 分页 -->
        <div class="pagination-container" v-if="total > 0">
          <a-pagination
              v-model:current="pageNum"
              :total="total"
              :pageSize="pageSize"
              @change="handlePageChange"
              show-quick-jumper
              show-size-changer
              :pageSizeOptions="['10', '20', '30', '50']"
              @showSizeChange="handlePageSizeChange"
          />
        </div><!-- 空状态展示 -->
        <empty-state
            v-if="total === 0 && !isFirstSearch"
            title="未找到相关内容"
            description="尝试使用其他关键词或筛选条件进行搜索"
        >
          <template #action>
            <a-button type="primary" @click="goToLearningCenter">浏览学习中心
            </a-button>
          </template>
        </empty-state><!-- 首次访问提示 -->
        <empty-state
            v-if="isFirstSearch"
            title="搜索课程、培训和学习地图"
            description="输入关键词开始搜索"
        >
          <template #icon>
            <search-outlined style="font-size: 48px; color: #d1d5db;" />
          </template>
        </empty-state>
      </div>
    </div>

    <!-- 底部区域 -->
    <footer-component />
  </div>
</template>

<script>
import { ref, reactive, onMounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';
import CourseCard from '@/components/common/CourseCard.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { searchContent } from '@/api/toc/search';
import { SearchOutlined } from '@ant-design/icons-vue';

export default {
  name: 'SearchPage',
  components: {
    HeaderComponent,
    FooterComponent,
    CourseCard,
    EmptyState,
    SearchOutlined
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
        path: '/toc/search',
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
        path: '/toc/search',
        query: {
          q: keyword.value,
          type: searchType.value,
          page: pageNum.value
        }
      });fetchSearchResults();
    };

    // 页码变化
    const handlePageChange = (page) => {
      pageNum.value = page;// 更新URL参数
      router.push({
        path: '/toc/search',
        query: {
          q: keyword.value,
          type: searchType.value,
          page: pageNum.value
        }
      });

      fetchSearchResults();
    };

    // 每页条数变化
    const handlePageSizeChange = (current, size) => {
      pageSize.value = size;
      pageNum.value = 1; // 重置页码

      // 更新URL参数
      router.push({
        path: '/toc/search',
        query: {
          q: keyword.value,
          type: searchType.value,
          page: pageNum.value
        }
      });fetchSearchResults();
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

    // 转换搜索结果为卡片组件需要的格式
    const convertToCardItem = (item) => {
      // 高亮处理标题和描述
      const highlightedTitle = highlightKeyword(item.title, keyword.value);
      const highlightedDescription = highlightKeyword(item.description, keyword.value);

      return {
        id: item.id,
        name: highlightedTitle,
        type: item.type.toUpperCase(),
        cover: item.coverImage,
        contentType: item.contentType,
        introduction: highlightedDescription,
        credit: item.credit,
        progress: item.progress || 0,
        // 添加原始标题和描述，用于不需要高亮的场景
        rawName: item.title,
        rawIntroduction: item.description
      };
    };

    // 跳转到学习中心
    const goToLearningCenter = () => {
      router.push('/toc/learning');
    };

    return {
      keyword,
      searchType,
      pageNum,
      pageSize,
      searchResults,
      total,
      isFirstSearch,
      handleSearch,
      handleTypeChange,
      handlePageChange,
      handlePageSizeChange,
      convertToCardItem,
      goToLearningCenter
    };
  }
};
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background-color: #f9fafb;
  display: flex;
  flex-direction: column;
}

.search-container {
  flex: 1;
  padding: 24px 32px;
  max-width: 1440px;
  margin: 0 auto;
  width: 100%;
}

.search-header {
  margin-bottom: 32px;
}

.search-title {
  font-size: 24px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 16px;
}

.search-input-wrapper {
  max-width: 600px;
  margin-bottom: 16px;
}

.search-filters {
  margin-bottom: 24px;
}

.search-stats {
  margin-bottom: 16px;
  color: #6b7280;
  font-size: 14px;
}

.search-results {
  margin-bottom: 32px;
}

.pagination-container {
  margin-top: 32px;
  text-align: center;
}

/* 关键词高亮样式 */
:deep(.highlight-keyword) {
  background-color: #fffbdd;
  color: #d97706;
  font-weight: bold;
  padding: 0 2px;
  border-radius: 2px;
}
</style>
