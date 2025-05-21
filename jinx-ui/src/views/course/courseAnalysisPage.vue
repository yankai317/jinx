
<template>
  <a-layout class="course-analysis-layout">
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
        <!-- 页面头部 -->
        <a-page-header
          :title="courseDetail.title"
          sub-title="学习数据分析"
          @back="goBack"
        >
          <template #tags>
            <a-tag :color="courseDetail.status === 'published' ? 'green' : 'orange'">
              {{ courseDetail.status === 'published' ? '已发布' : '未发布' }}
            </a-tag>
          </template>
          <template #extra>
            <a-button @click="goBack">
              <rollback-outlined />返回课程详情
            </a-button>
          </template>
          <a-descriptions size="small" :column="3">
            <a-descriptions-item label="创建人">{{ courseDetail.creatorName }}</a-descriptions-item>
            <a-descriptions-item label="发布时间">{{ courseDetail.publishTime }}</a-descriptions-item>
            <a-descriptions-item label="学分">{{ courseDetail.credit }}</a-descriptions-item>
          </a-descriptions>
        </a-page-header>

        <!-- 筛选区域 -->
        <a-card class="filter-card" :bordered="false">
          <a-form layout="inline" :model="filterForm" ref="filterFormRef">
            <a-form-item label="部门" name="departmentId">
              <a-select
                v-model:value="filterForm.departmentId"
                placeholder="选择部门"
                style="width: 200px"
                allowClear
                @change="handleFilterChange"
              >
                <a-select-option v-for="dept in departments" :key="dept.id" :value="dept.id">
                  {{ dept.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="时间范围" name="dateRange">
              <a-range-picker
                v-model:value="filterForm.dateRange"
                format="YYYY-MM-DD"
                :placeholder="['开始日期', '结束日期']"
                @change="handleFilterChange"
              /></a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleFilterChange">
                <search-outlined />查询
              </a-button>
              <a-button style="margin-left: 8px" @click="resetFilter">
                <reload-outlined />重置
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>

        <!-- 数据概览区 --><a-row :gutter="16" class="statistics-row">
          <a-col :span="8">
            <a-card>
              <a-statistic
                title="查看人数"
                :value="statistics.viewCount"
                :precision="0"
                style="margin-right: 50px"
              >
                <template #prefix>
                  <eye-outlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col><a-col :span="8">
            <a-card>
              <a-statistic
                title="完成人数"
                :value="statistics.completeCount"
                :precision="0"
              >
                <template #prefix>
                  <check-circle-outlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="8">
            <a-card>
              <a-statistic
                title="平均学习时长(分钟)"
                :value="statistics.avgDuration"
                :precision="0"
              >
                <template #prefix>
                  <clock-circle-outlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
        </a-row>

        <!-- 图表展示区 -->
        <a-row :gutter="16" class="chart-row">
          <a-col :span="12">
            <a-card title="学习趋势" :bordered="false">
              <div ref="timeChartRef" style="height: 300px"></div>
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card title="部门学习情况" :bordered="false">
              <div ref="deptChartRef" style="height: 300px"></div>
            </a-card>
          </a-col>
        </a-row>

        <!-- 学员列表区 -->
        <a-card title="学员学习详情" :bordered="false" class="learner-card">
          <div class="table-operations">
            <a-form layout="inline">
              <a-form-item label="完成状态">
                <a-select
                  v-model:value="learnerFilter.status"
                  style="width: 120px"
                  @change="handleLearnerFilterChange"
                >
                  <a-select-option value="all">全部</a-select-option>
                  <a-select-option value="completed">已完成</a-select-option>
                  <a-select-option value="learning">学习中</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="关键词">
                <a-input
                  v-model:value="learnerFilter.keyword"
                  placeholder="姓名/工号"
                  style="width: 200px"
                  @pressEnter="handleLearnerFilterChange"
                />
              </a-form-item>
              <a-form-item>
                <a-button type="primary" @click="handleLearnerFilterChange">
                  <search-outlined />查询
                </a-button>
              </a-form-item>
            </a-form>
          </div>
          <a-table
            :columns="learnerColumns"
            :data-source="learners.list"
            :pagination="{
              current: learnerFilter.pageNum,
              pageSize: learnerFilter.pageSize,
              total: learners.total,
              onChange: handlePageChange,
              showSizeChanger: true,
              showTotal: total => `共 ${total} 条记录`
            }"
            :loading="loading"
            rowKey="userId"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'completed'">
                <a-tag :color="record.completed ? 'green' : 'orange'">
                  {{ record.completed ? '已完成' : '学习中' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'userName'">
                <a @click="viewLearnerDetail(record)">{{ record.userName }}</a>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script>
import { ref, reactive, onMounted, defineComponent, nextTick } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import * as echarts from 'echarts';
import { getCourseDetail, getCourseStatistics, getCourseLearnerStatistics } from '@/api/course';
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
  RollbackOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'courseAnalysisPage',
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
    RollbackOutlined,
    SearchOutlined,
    ReloadOutlined,
    EyeOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const route = useRoute();
    const courseId = ref(parseInt(route.params.id) || 0);
    const loading = ref(false);
    const collapsed = ref(false);
    const selectedKeys = ref(['courseList']);
    const openKeys = ref(['learning']);

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

    // 课程详情
    const courseDetail = ref({
      id: 0,
      title: '',
      type: '',
      coverImage: '',
      instructorId: null,
      description: '',
      credit: 0,
      categoryIds: [],
      categoryNames: [],
      status: '',
      allowComments: true,
      isTop: false,
      viewCount: 0,
      completeCount: 0,
      article: '',
      appendixType: '',
      appendixPath: '',
      publishTime: '',
      creatorId: null,
      creatorName: ''
    });

    // 统计数据
    const statistics = ref({
      viewCount: 0,
      completeCount: 0,
      avgDuration: 0,
      totalDuration: 0,
      departmentStats: [],
      timeDistribution: []
    });

    // 部门列表
    const departments = ref([]);

    // 筛选表单
    const filterFormRef = ref(null);
    const filterForm = reactive({
      departmentId: undefined,
      dateRange: []
    });

    // 学员列表筛选
    const learnerFilter = reactive({
      departmentId: undefined,
      status: 'all',
      keyword: '',
      pageNum: 1,
      pageSize: 10
    });

    // 学员列表
    const learners = ref({
      total: 0,
      list: []
    });

    // 学员列表列定义
    const learnerColumns = [
      {
        title: '姓名',
        dataIndex: 'userName',
        key: 'userName',
        width: '15%'
      },
      {
        title: '工号',
        dataIndex: 'employeeNo',
        key: 'employeeNo',
        width: '15%'
      },
      {
        title: '部门',
        dataIndex: 'department',
        key: 'department',
        width: '20%'
      },
      {
        title: '学习时长(分钟)',
        dataIndex: 'studyDuration',
        key: 'studyDuration',
        width: '15%',
        sorter: (a, b) => a.studyDuration - b.studyDuration
      },
      {
        title: '完成状态',
        dataIndex: 'completed',
        key: 'completed',
        width: '15%'
      },
      {
        title: '最后学习时间',
        dataIndex: 'lastStudyTime',
        key: 'lastStudyTime',
        width: '20%',
        sorter: (a, b) => dayjs(a.lastStudyTime).valueOf() - dayjs(b.lastStudyTime).valueOf()
      }
    ];// 图表引用
    const timeChartRef = ref(null);
    const deptChartRef = ref(null);
    let timeChart = null;
    let deptChart = null;

    // 获取用户信息
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

    // 获取课程详情
    const fetchCourseDetail = async () => {
      if (!courseId.value) {
        message.error('课程ID不存在');
        router.push('/course/list');
        return;
      }

      loading.value = true;
      try {
        const res = await getCourseDetail(courseId.value);
        if (res.code === 200 && res.data) {
          courseDetail.value = res.data;
        } else {
          message.error(res.message || '获取课程详情失败');
          router.push('/course/list');
        }
      } catch (error) {
        console.error('获取课程详情失败:', error);
        router.push('/course/list');
      } finally {
        loading.value = false;
      }
    };

    // 获取统计数据
    const fetchStatistics = async () => {
      if (!courseId.value) return;

      loading.value = true;
      try {
        const res = await getCourseStatistics(courseId.value);
        if (res.code === 200 && res.data) {
          statistics.value = res.data;

          // 初始化图表
          nextTick(() => {
            initTimeChart();
            initDeptChart();
          });
        } else {
          message.error(res.message || '获取统计数据失败');
        }
      } catch (error) {
        console.error('获取统计数据失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取学员列表
    const fetchLearners = async () => {
      if (!courseId.value) return;

      loading.value = true;
      try {
        const params = {
          departmentId: learnerFilter.departmentId,
          status: learnerFilter.status === 'all' ? undefined : learnerFilter.status,
          keyword: learnerFilter.keyword || undefined,
          pageNum: learnerFilter.pageNum,
          pageSize: learnerFilter.pageSize
        };

        const res = await getCourseLearnerStatistics(courseId.value, params);
        if (res.code === 200 && res.data) {
          learners.value = res.data;
        } else {
          message.error(res.message || '获取学员列表失败');
        }
      } catch (error) {
        console.error('获取学员列表失败:', error);
      } finally {
        loading.value = false;
      }
    };

    // 获取部门列表
    const fetchDepartments = async () => {
      // 这里应该调用获取部门列表的API
      // 由于没有提供相关API，这里模拟一些部门数据
      departments.value = [
        { id: 1, name: '技术部' },
        { id: 2, name: '市场部' },
        { id: 3, name: '人力资源部' },
        { id: 4, name: '财务部' },
        { id: 5, name: '销售部' }
      ];
    };

    // 初始化时间趋势图表
    const initTimeChart = () => {
      if (!timeChartRef.value) return;

      if (timeChart) {
        timeChart.dispose();
      }

      timeChart = echarts.init(timeChartRef.value);

      const dates = statistics.value.timeDistribution.map(item => item.date);
      const counts = statistics.value.timeDistribution.map(item => item.count);

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: dates,
          axisLabel: {
            interval: 0,
            rotate: 30
          }
        },
        yAxis: {
          type: 'value',
          name: '学习人数'
        },
        series: [
          {
            name: '学习人数',
            type: 'line',
            data: counts,
            smooth: true,
            itemStyle: {
              color: '#1890ff'
            },
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [
                  {
                    offset: 0,
                    color: 'rgba(24, 144, 255, 0.6)'
                  },
                  {
                    offset: 1,
                    color: 'rgba(24, 144, 255, 0.1)'
                  }
                ]
              }
            }
          }
        ]
      };

      timeChart.setOption(option);

      // 窗口大小变化时重新调整图表大小
      window.addEventListener('resize', () => {
        timeChart && timeChart.resize();
      });
    };

    // 初始化部门统计图表
    const initDeptChart = () => {
      if (!deptChartRef.value) return;

      if (deptChart) {
        deptChart.dispose();
      }

      deptChart = echarts.init(deptChartRef.value);

      const deptNames = statistics.value.departmentStats.map(item => item.name);
      const deptCounts = statistics.value.departmentStats.map(item => item.count);

      const option = {
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          right: 10,
          top: 'center'
        },
        series: [
          {
            name: '部门学习人数',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '18',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: deptNames.map((name, index) => ({
              value: deptCounts[index],
              name: name
            }))
          }
        ]
      };

      deptChart.setOption(option);

      // 窗口大小变化时重新调整图表大小
      window.addEventListener('resize', () => {
        deptChart && deptChart.resize();
      });
    };

    // 处理筛选变化
    const handleFilterChange = () => {
      // 根据筛选条件重新获取统计数据
      fetchStatistics();

      // 更新学员列表筛选条件
      if (filterForm.departmentId !== undefined) {
        learnerFilter.departmentId = filterForm.departmentId;
      }

      // 重新获取学员列表
      learnerFilter.pageNum = 1;
      fetchLearners();
    };

    // 重置筛选条件
    const resetFilter = () => {
      filterFormRef.value.resetFields();
      handleFilterChange();
    };

    // 处理学员列表筛选变化
    const handleLearnerFilterChange = () => {
      learnerFilter.pageNum = 1;
      fetchLearners();
    };

    // 处理分页变化
    const handlePageChange = (page, pageSize) => {
      learnerFilter.pageNum = page;
      learnerFilter.pageSize = pageSize;
      fetchLearners();
    };

    // 查看学员详情
    const viewLearnerDetail = (record) => {
      message.info(`查看学员 ${record.userName} 的学习记录详情`);
      // 这里可以跳转到学员学习记录详情页面
      // router.push(`/course/learner/${courseId.value}/${record.userId}`);
    };

    // 返回课程详情页
    const goBack = () => {
      router.push(`/course/detail/${courseId.value}`);
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

    onMounted(() => {
      // 获取用户信息
      getUserInfo();

      // 获取课程详情
      fetchCourseDetail();

      // 获取部门列表
      fetchDepartments();

      // 获取统计数据
      fetchStatistics();

      // 获取学员列表
      fetchLearners();
    });

    return {
      courseId,
      loading,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      courseDetail,
      statistics,
      departments,
      filterFormRef,
      filterForm,
      learnerFilter,
      learners,
      learnerColumns,
      timeChartRef,
      deptChartRef,
      handleFilterChange,
      resetFilter,
      handleLearnerFilterChange,
      handlePageChange,
      viewLearnerDetail,
      goBack,
      handleLogout,
      navigateTo
    };
  }
});
</script>

<style scoped>
.course-analysis-layout {
  min-height: 100vh;
}



.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: auto;
}

.filter-card {
  margin-bottom: 24px;
}

.statistics-row {
  margin-bottom: 24px;
}

.chart-row {
  margin-bottom: 24px;
}

.learner-card {
  margin-bottom: 24px;
}

.table-operations {
  margin-bottom: 16px;
}
</style>
