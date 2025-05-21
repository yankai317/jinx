<template>
  <div class="learning-center-page">
    <!-- 顶部导航栏 -->
    <HeaderComponent activeKey="LEARNING_CENTER" />

    <div class="main-content">
      <!-- 筛选区域 -->
      <div class="filter-container" :class="{ 'sticky': isFilterSticky }">
        <!-- 内容类型筛选 -->
        <div class="content-type-filter">
          <div v-for="item in contentTypeOptions" :key="item.value" class="filter-tab"
            :class="{ 'active': contentType === item.value }" @click="handleContentTypeChange(item.value)">
            {{ item.label }}
          </div>
        </div>

        <!-- 分类筛选 -->
        <div class="category-filter">
          <CategoryTree
            :categories="categories"
            :selected-path="selectedCategoryPath"
          @select="handleCategoryChange"
          />
        </div>

        <!-- 内容形式筛选 -->
        <div class="content-format-filter" v-if="contentType === 'COURSE'">
          <a-button class="format-btn" :class="{ 'format-btn-active': isFormatSelected('video') }"
            @click="toggleContentFormat('video')">
            <video-camera-outlined />
            视频
          </a-button>
          <a-button class="format-btn" :class="{ 'format-btn-active': isFormatSelected('article') }"
            @click="toggleContentFormat('article')">
            <file-text-outlined />
            文章
          </a-button>
          <a-button class="format-btn" :class="{ 'format-btn-active': isFormatSelected('document') }"
            @click="toggleContentFormat('document')">
            <file-text-outlined />
            文档
          </a-button>
          <a-button class="format-btn" :class="{ 'format-btn-active': isFormatSelected('series') }"
            @click="toggleContentFormat('series')">
            <snippets-outlined />
            系列课
          </a-button>
        </div>
      </div>

      <!-- 内容列表 -->
      <div class="content-list">
        <div v-if="loading" class="loading-container">
          <a-spin size="large" />
        </div>
        <template v-else>
          <div v-if="contentList.length > 0" class="content-grid">
            <div v-for="item in contentList" :key="`${item.type}-${item.id}`" class="content-item">
              <CourseCard :item="item" />
            </div>
          </div>
          <EmptyState v-else title="暂无内容" description="没有找到符合条件的学习内容" />
        </template>

        <!-- 分页 -->
        <div v-if="contentList.length > 0" class="pagination-container">
          <a-pagination v-model:current="currentPage" :total="total" :page-size="pageSize" show-quick-jumper
            @change="handlePageChange" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  VideoCameraOutlined,
  PlaySquareOutlined,
  FileTextOutlined,
  SnippetsOutlined
} from '@ant-design/icons-vue';

import HeaderComponent from '@/components/common/HeaderComponent.vue';
import CourseCard from '@/components/common/CourseCard.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import FooterComponent from '@/components/common/FooterComponent.vue';

import { getLearningCenter } from '@/api/toc/learning';
import { getCategoryList } from '@/api/category';
import CategoryTree from '@/components/common/CategoryTree.vue';

export default defineComponent({
  name: 'LearningCenterPage',
  components: {
    HeaderComponent,
    CourseCard,
    EmptyState,
    FooterComponent,
    VideoCameraOutlined,
    PlaySquareOutlined,
    FileTextOutlined,
    SnippetsOutlined,
    CategoryTree
  },
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 状态变量
    const loading = ref(false);
    const contentList = ref([]);
    const categories = ref([]);
    const total = ref(0);
    const currentPage = ref(1);
    const pageSize = ref(12);
    const contentType = ref('COURSE'); // 默认显示培训
    const selectedCategoryPath = ref([]); // 选中的分类路径
    const selectedContentFormats = ref([]); // 内容形式筛选
    const isFilterSticky = ref(false); // 筛选区域是否吸顶

    // 内容类型选项
    const contentTypeOptions = [
      { label: '课程', value: 'COURSE' },
      { label: '培训', value: 'TRAIN' },
      { label: '学习地图', value: 'LEARNING_MAP' },
    ];

    // 监听滚动事件，实现筛选区域吸顶
    const handleScroll = () => {
      const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
      isFilterSticky.value = scrollTop > 72; // 72px是顶部导航栏的高度
    };

    // 获取分类列表
    const fetchCategories = async () => {
      try {
        const res = await getCategoryList();
        if (res.code === 200 && res.data) {
          categories.value = res.data;
        } else {
          message.error('获取分类列表失败');
        }
      } catch (error) {
        console.error('获取分类列表异常:', error);
      }
    };

    // 获取学习中心内容
    const fetchLearningCenter = async () => {
      loading.value = true;
      try {
        const params = {
          type: contentType.value,
          pageNum: currentPage.value,
          pageSize: pageSize.value
        };

        // 添加分类筛选
        if (selectedCategoryPath.value.length > 0) {
          const lastCategory = selectedCategoryPath.value[selectedCategoryPath.value.length - 1];
          params.categoryId = lastCategory.id;
        }

        // 添加内容形式筛选
        if (selectedContentFormats.value.length > 0) {
          params.contentTypes = selectedContentFormats.value.join(',');
        }

        const res = await getLearningCenter(params);
        if (res.code === 200 && res.data) {
          contentList.value = res.data.list.map(item => ({
            ...item,
            name: item.title, // CourseCard组件使用name字段
            cover: item.coverImage, // CourseCard组件使用cover字段
            rawName: item.title // 保存原始标题
          }));
          total.value = res.data.total;

          // 更新分类列表
          if (res.data.categories && res.data.categories.length > 0) {
            categories.value = res.data.categories;
          }
        } else {
          message.error('获取学习中心数据失败');
        }
      } catch (error) {
        console.error('获取学习中心数据异常:', error);
      } finally {
        loading.value = false;
      }
    };

    // 处理内容类型切换
    const handleContentTypeChange = (type) => {
      contentType.value = type;
      currentPage.value = 1; // 重置页码
      /** 清空分类和内容形式 */
      selectedCategoryPath.value = [];
      selectedContentFormats.value = [];
      fetchLearningCenter();
    };

    // 处理分类切换
    const handleCategoryChange = (path) => {
      selectedCategoryPath.value = path;
      currentPage.value = 1; // 重置页码
      fetchLearningCenter();
    };

    // 处理内容形式筛选
    const toggleContentFormat = (format) => {
      const index = selectedContentFormats.value.indexOf(format);
      if (index === -1) {
        selectedContentFormats.value.push(format);
      } else {
        selectedContentFormats.value.splice(index, 1);
      }
      currentPage.value = 1; // 重置页码
      fetchLearningCenter();
    };

    // 判断内容形式是否被选中
    const isFormatSelected = (format) => {
      return selectedContentFormats.value.includes(format);
    };

    // 处理分页切换
    const handlePageChange = (page) => {
      currentPage.value = page;
      fetchLearningCenter();
    };

    // 组件挂载时
    onMounted(() => {
      const newQuery = route.query;

      if (newQuery.type) {
        contentType.value = newQuery.type;
      }
      if (newQuery.categoryId) {
        selectedCategoryPath.value = JSON.parse(newQuery.categoryId);
      }
      // 获取分类列表
      fetchCategories();

      // 获取学习中心内容
      fetchLearningCenter();

      // 添加滚动事件监听
      window.addEventListener('scroll', handleScroll);
    });

    // 组件卸载时
    onUnmounted(() => {
      // 移除滚动事件监听
      window.removeEventListener('scroll', handleScroll);
    });

    return {
      loading,
      contentList,
      categories,
      total,
      currentPage,
      pageSize,
      contentType,
      selectedCategoryPath,
      selectedContentFormats,
      isFilterSticky,
      contentTypeOptions,
      handleContentTypeChange,
      handleCategoryChange,
      toggleContentFormat,
      isFormatSelected,
      handlePageChange
    };
  }
});
</script>

<style scoped>
.learning-center-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #F9FAFB;
}

.main-content {
  flex: 1;
  padding: 24px 32px;
  max-width: 1440px;
  margin: 0 auto;
  width: 100%;
}

.filter-container {
  background-color: #FFFFFF;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;
  z-index: 10;
  width: 100%;
}

.filter-container.sticky {
  position: sticky;
  top: 72px;
}

.content-type-filter {
  display: flex;
  border-bottom: 1px solid #E5E7EB;
}

.filter-tab {
  padding: 16px 24px;
  cursor: pointer;
  font-size: 16px;
  color: #4B5563;
  position: relative;
}

.filter-tab.active {
  color: #1890FF;
  font-weight: 700;
  border-bottom: 2px solid #1890FF;
}

.category-filter {
  padding: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  border-bottom: 1px solid #E5E7EB;
}

.category-btn {
  margin-right: 8px;
  margin-bottom: 8px;
}

.content-format-filter {
  padding: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.format-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 50px;
}

.format-btn-active {
  background-color: #EFF6FF;
  color: #1890FF;
}

.content-list {
  margin-top: 24px;
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

.content-item {
  height: 100%;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.pagination-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}
</style>
