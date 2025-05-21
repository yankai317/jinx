<template>
  <div class="learning-center-mobile-page">
    <!-- 使用移动端头部组件 -->
    <MobileHeader
      title="学习中心"
      :showBackIcon="false"
      :showLogoSearch="true"
      :showLogo="true"
      :showSearch="true"
      :searchPlaceholder="'请输入课程关键词'"
      :showSearchTypeFilter="false"
      :sticky="true"
      @search="handleSearch"
    />

    <!-- 分类筛选 -->
    <div class="category-filter-container">
      <div class="category-tabs">
        <div
          v-for="item in contentTypeOptions"
          :key="item.value"
          class="category-tab"
          :class="{ active: contentType === item.value }"
          @click="handleContentTypeChange(item.value)"
        >
          {{ item.label }}
        </div>
      </div>
      <div class="category-indicator" :style="indicatorStyle"></div>
    </div>

    <!-- 分类选择 -->
    <div class="category-selection">
      <CategoryTree
        :categories="categories"
        :selected-path="selectedCategoryPath"
        @select="handleCategoryChange"
      />
    </div>

    <!-- 内容形式筛选 -->
    <div class="format-filter" v-if="contentType === 'COURSE'">
      <div
        class="format-item"
        :class="{ active: isFormatSelected('video') }"
        @click="toggleContentFormat('video')"
      >
        <video-camera-outlined />
        <span>视频</span>
      </div>
      <div
        class="format-item"
        :class="{ active: isFormatSelected('article') }"
        @click="toggleContentFormat('article')"
      >
        <file-text-outlined />
        <span>文章</span>
      </div>
      <div
        class="format-item"
        :class="{ active: isFormatSelected('document') }"
        @click="toggleContentFormat('document')"
      >
        <file-text-outlined />
        <span>文档</span>
      </div>
      <div
        class="format-item"
        :class="{ active: isFormatSelected('series') }"
        @click="toggleContentFormat('series')"
      >
        <play-square-outlined />
        <span>系列课</span>
      </div>
    </div>

    <!-- 内容列表 -->
    <div class="content-list">
      <div v-if="loading" class="loading-container">
        <a-spin size="large" />
      </div>
      <template v-else>
        <div v-if="contentList.length > 0" class="content-items">
          <div
            v-for="item in contentList"
            :key="`${item.type}-${item.id}`"
            class="content-item"
            @click="handleItemClick(item)"
          >
            <div class="item-cover">
              <img v-if="item.cover" :src="item.cover" :alt="item.name" />
              <div v-else class="cover-placeholder">
                <file-image-outlined />
              </div><div v-if="item.isTop" class="top-tag">置顶</div>
            </div>
            <div class="item-content">
              <h3 class="item-title">{{ item.name }}</h3>
              <div class="item-info">
                <div class="info-item">
                  <video-camera-outlined />
                  <span>{{ item.lessonCount || 1 }} 门课</span>
                </div>
                <span class="info-separator">+</span>
                <div class="info-item">
                  <edit-outlined />
                  <span>考试</span>
                </div>
                <span v-if="item.hasSurvey" class="info-separator">+</span>
                <div v-if="item.hasSurvey" class="info-item">
                  <file-text-outlined />
                  <span>调研</span>
                </div>
              </div>
              <div class="item-credit" v-if="item.credit">
                学分：{{ item.credit }}分
              </div>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <empty-outlined />
          <p>暂无内容</p>
          <p class="empty-desc">没有找到符合条件的学习内容</p>
        </div>
      </template>

      <!-- 加载更多 -->
      <div v-if="contentList.length > 0 && hasMore && loadingMore" class="loading-more">
        <a-spin size="small" />
        <span>加载中...</span>
      </div>
    </div>

    <!-- 分类抽屉已移除，直接在页面上展示分类选择组件 -->

    <!-- 底部导航栏 --><MobileTabBar active="learning" />

    <!-- 返回顶部按钮 -->
    <div class="back-to-top" v-show="showBackToTop" @click="scrollToTop">
      <up-outlined />
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, watch, reactive, onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import {
  SearchOutlined,
  VideoCameraOutlined,
  PlaySquareOutlined,
  FileTextOutlined,
  FileImageOutlined,
  EditOutlined,
  EmptyOutlined,
  UpOutlined
} from '@ant-design/icons-vue';

import CategoryTree from '@/components/common/CategoryTree.vue';
import MobileTabBar from '@/components/mobile/MobileTabBar.vue';
import MobileHeader from '@/components/mobile/MobileHeader.vue';
import { getLearningCenter } from '@/api/toc/learning';
import { getCategoryList } from '@/api/category';

export default defineComponent({
  name: 'LearningCenterMobilePage',
  components: {
    SearchOutlined,
    VideoCameraOutlined,
    PlaySquareOutlined,
    FileTextOutlined,
    FileImageOutlined,
    EditOutlined,
    EmptyOutlined,
    UpOutlined,
    CategoryTree,
    MobileTabBar,
    MobileHeader
  },
  setup() {
    const router = useRouter();
    const route = useRoute();

    // 状态变量
    const loading = ref(false);
    const loadingMore = ref(false);
    const contentList = ref([]);
    const categories = ref([]);
    const total = ref(0);
    const currentPage = ref(1);
    const pageSize = ref(10);
    const contentType = ref('COURSE'); // 默认显示培训
    const selectedCategoryPath = ref([]); // 选中的分类路径
    const selectedContentFormats = ref([]); // 内容形式筛选
    const keyword = ref(''); // 搜索关键词
    const isScrollLoading = ref(false); // 是否正在滚动加载
    const showBackToTop = ref(false); // 是否显示返回顶部按钮
    const contentTypeCounts = reactive({
      COURSE: 0,
      LEARNING_MAP: 0,
      EXAM: 0,
      SURVEY: 0,
      PRACTICE: 0
    });

    // 计算属性：是否有更多数据
    const hasMore = computed(() => {
      return contentList.value.length < total.value;
    });

    // 内容类型选项
    const contentTypeOptions = [
      { label: '课程', value: 'COURSE' },
      { label: '培训', value: 'TRAIN' },
      { label: '学习地图', value: 'LEARNING_MAP' },
      // { label: '考试', value: 'EXAM' },
      // { label: '调研', value: 'SURVEY' },
      // { label: '练习', value: 'PRACTICE' }
    ];

    // 计算属性：分类指示器样式
    const indicatorStyle = computed(() => {
      const index = contentTypeOptions.findIndex(item => item.value === contentType.value);
      const width = 100 / contentTypeOptions.length;
      return {
        left: `${index * width}%`,
        width: `${width}%`
      };
    });

    // 获取分类列表
    const fetchCategories = async () => {
      try {
        const res = await getCategoryList();
        if (res.code === 200&& res.data) {
          categories.value = res.data;
        } else {
          message.error('获取分类列表失败');
        }
      } catch (error) {
        console.error('获取分类列表异常:', error);message.error('获取分类列表异常');
      }
    };

    // 获取学习中心内容
    const fetchLearningCenter = async (isLoadMore = false) => {
      if (!isLoadMore) {
        loading.value = true;
      } else {
        loadingMore.value = true;
      }

      try {
        const params = {
          type: contentType.value,
          pageNum: isLoadMore ? currentPage.value : 1,
          pageSize: pageSize.value,
          keyword: keyword.value
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
          const formattedList = res.data.list.map(item => ({
            ...item,
            name: item.title, // CourseCard组件使用name字段
            cover: item.coverImage, // CourseCard组件使用cover字段
            rawName: item.title, // 保存原始标题
            isTop: item.isTop || false // 是否置顶
          }));

          if (isLoadMore) {
            contentList.value = [...contentList.value, ...formattedList];
          } else {
            contentList.value = formattedList;
          }
          total.value = res.data.total;

          // 更新分类列表
          if (res.data.categories && res.data.categories.length > 0) {
            categories.value = res.data.categories;
          }

          // 更新内容类型计数
          if (res.data.counts) {
            Object.keys(res.data.counts).forEach(key => {
              contentTypeCounts[key] = res.data.counts[key] || 0;
            });
          }
        } else {
          message.error('获取学习中心数据失败');
        }
      } catch (error) {
        console.error('获取学习中心数据异常:', error);
      } finally {
        loading.value = false;
        loadingMore.value = false;
      }
    };

    // 处理内容类型切换
    const handleContentTypeChange = (type) => {
      contentType.value = type;
      currentPage.value = 1; // 重置页码
      selectedContentFormats.value = []; // 清空内容形式筛选
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

    // 处理搜索
    const handleSearch = () => {
      currentPage.value = 1; // 重置页码
      fetchLearningCenter();
    };

    // 监听页面滚动，实现触底加载更多
    const handleScroll = () => {
      // 获取页面滚动位置
      const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
      // 获取页面可见区域高度
      const windowHeight = window.innerHeight;
      // 获取页面总高度
      const documentHeight = document.documentElement.scrollHeight;

      // 检测是否滚动到底部(距离底部100px时就开始加载)
      if (scrollTop + windowHeight >= documentHeight - 100) {
        loadMore();
      }

      // 当滚动超过一定距离时，显示返回顶部按钮
      if (scrollTop > 100) {
        showBackToTop.value = true;
      } else {
        showBackToTop.value = false;
      }
    };

    // 滚动到顶部
    const scrollToTop = () => {
      window.scrollTo({
        top: 0,
        behavior: 'smooth'
      });
    };

    // 加载更多
    const loadMore = () => {
      if (loadingMore.value || !hasMore.value) return;
      loadingMore.value = true;
      currentPage.value += 1;
      fetchLearningCenter(true);
    };

    // 组件挂载时
    onMounted(() => {
      const newQuery = route.query;
      if (newQuery.type) {
        contentType.value = newQuery.type;
      }if (newQuery.categoryId) {
        selectedCategoryPath.value = JSON.parse(newQuery.categoryId);
      }
      // 获取分类列表
      fetchCategories();

      // 获取学习中心内容
      fetchLearningCenter();

      // 添加滚动事件监听
      window.addEventListener('scroll', handleScroll);
    });

    // 组件卸载时，移除滚动事件监听
    onUnmounted(() => {
      window.removeEventListener('scroll', handleScroll);
    });

    // 处理列表项点击
    const handleItemClick = (item) => {
      console.log('点击列表项:', item);

      // 根据内容类型导航到不同的详情页面
      switch(item.type) {
        case 'COURSE':
          router.push(`/toc/mobile/course/${item.id}`);
          break;
        case 'TRAIN':
          router.push(`/toc/mobile/train/${item.id}`);
          break;
        case 'LEARNING_MAP':
          router.push(`/toc/mobile/map/${item.id}`);
          break;
        case 'EXAM':
          router.push(`/toc/mobile/exam/${item.id}`);
          break;
        case 'SURVEY':
          router.push(`/toc/mobile/survey/${item.id}`);
          break;
        case 'PRACTICE':
          router.push(`/toc/mobile/practice/${item.id}`);
          break;
        default:
          console.warn('未知的内容类型:', item.type);
      }
    };

    return {
      loading,
      loadingMore,
      contentList,
      categories,
      total,
      currentPage,
      pageSize,
      contentType,
      selectedCategoryPath,
      selectedContentFormats,
      keyword,
      contentTypeCounts,
      hasMore,
      indicatorStyle,
      contentTypeOptions,
      handleContentTypeChange,
      handleCategoryChange,
      toggleContentFormat,
      isFormatSelected,
      handleSearch,
      loadMore,
      handleItemClick,
      showBackToTop,
      scrollToTop
    };
  }
});
</script>

<style scoped>
.learning-center-mobile-page {
  min-height: 100vh;
  background-color: #F9FAFB;
  padding-bottom: 80px; /* 为底部导航栏留出空间 */
}



/* 分类筛选 */
.category-filter-container {
  padding-top: 12px;
  position: relative;
  background-color: #FFFFFF;
  border-bottom: 1px solid #E5E7EB;
}

.category-tabs {
  display: flex;
  justify-content: space-between;
  padding: 0 16px;
}

.category-tab {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  font-size: 14px;
  color: #9CA3AF;
  position: relative;
}

.category-tab.active {
  color: #1890FF;
  font-weight: 500;
}

.category-indicator {
  position: absolute;
  bottom: 0;
  height: 2px;
  background-color: #1890FF;
  transition: all 0.3s ease;
}

/* 分类选择 */
.category-selection {
  padding: 12px 16px;
  background-color: #FFFFFF;
  border-bottom: 1px solid #E5E7EB;
  overflow-x: auto;
}

/* 调整 CategoryTree 在移动端的样式 */
.category-selection :deep(.category-tree) {
  width: 100%;
}

.category-selection :deep(.category-level) {
  display: flex;
  flex-wrap: nowrap;
  overflow-x: auto;
  padding-bottom: 8px;
  -webkit-overflow-scrolling: touch;
}

.category-selection :deep(.category-btn) {
  flex-shrink: 0;
  margin-right: 8px;
  white-space: nowrap;
}

/* 内容形式筛选 */
.format-filter {
  display: flex;
  padding: 16px;
  background-color: #FFFFFF;
  border-bottom: 1px solid #E5E7EB;gap: 8px;
}

.format-item {
  display: flex;
  align-items: center;
  padding: 4px 12px;
  background-color: #F3F4F6;
  border-radius: 9999px;
  font-size: 14px;
  color: #4B5563;gap: 4px;
}

.format-item.active {
  background-color: #EFF6FF;
  color: #4A90E2;
}

/* 内容列表 */
.content-list {
  padding: 16px;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.content-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.content-item {
  background-color: #FFFFFF;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
  display: flex;
  padding: 12px;
}

.item-cover {
  position: relative;
  width: 120px;
  height: 68px;
  border-radius: 4px;
  overflow: hidden;background-color: #E5E7EB;
  margin-right: 12px;
  flex-shrink: 0;
}

.item-cover img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;color: #9CA3AF;
  font-size: 24px;
}

.top-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 4px 8px;
  background-color: #F5A623;
  border-radius: 4px;
  font-size: 12px;
  color: #FFFFFF;
}

.item-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.item-title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
  color: #000000;
  line-height: 20px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-info {
  display: flex;
  align-items: center;
  margin-bottom: 4px;
  font-size: 12px;
  color: #6B7280;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-separator {
  margin: 0 4px;
}

.item-credit {
  font-size: 12px;
  color: #4A90E2;
  margin-top: auto;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #9CA3AF;
}

.empty-state .anticon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

.empty-state .empty-desc {
  font-size: 14px;
  margin-top: 8px;
}

/* 正在加载更多 */
.loading-more {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 0;
  gap: 8px;
  color: #9CA3AF;
  font-size: 14px;
}

/* 返回顶部按钮 */
.back-to-top {
  position: fixed;
  right: 16px;
  bottom: 80px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #1890FF;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  z-index: 10;
}
</style>
