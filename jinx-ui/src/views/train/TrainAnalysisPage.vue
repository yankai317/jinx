
<template>
  <a-layout class="train-analysis-layout">
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
          :title="trainDetail.name"
          sub-title="培训数据分析"
          @back="goBack"
        >
          <template #tags>
            <a-tag :color="trainDetail.status === 'published' ? 'green' : 'orange'">
              {{ getStatusText(trainDetail.status) }}
            </a-tag>
          </template>
          <template #extra>
            <a-button @click="goBack">
              <rollback-outlined />返回培训跟踪
            </a-button>
            <a-button type="primary" @click="exportToExcel">
              <download-outlined />导出数据
            </a-button>
          </template>
          <a-descriptions size="small" :column="3">
            <a-descriptions-item label="创建人">{{ trainDetail.creatorName }}</a-descriptions-item>
            <a-descriptions-item label="发布时间">{{ trainDetail.publishTime }}</a-descriptions-item>
            <a-descriptions-item label="培训类型">{{ trainDetail.type }}</a-descriptions-item>
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
              />
            </a-form-item>
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

        <!-- 数据概览区 -->
        <a-row :gutter="16" class="statistics-row">
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="学习人数"
                :value="statistics.learnerCount || 0"
                :precision="0"
              >
                <template #prefix>
                  <user-outlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="完成人数"
                :value="statistics.completionCount || 0"
                :precision="0"
              >
                <template #prefix>
                  <check-circle-outlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="完成率"
                :value="statistics.completionRate || 0"
                :precision="2"
                suffix="%"
              >
                <template #prefix>
                  <pie-chart-outlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="平均学习时长(分钟)"
                :value="statistics.avgDuration || 0"
                :precision="0"
              >
                <template #prefix>
                  <clock-circle-outlined />
                </template>
              </a-statistic>
            </a-card>
          </a-col>
        </a-row>

        <!-- 内容完成率概览 -->
        <a-row :gutter="16" class="statistics-row">
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="课程完成率"
                :value="statistics.courseCompletionRate || 0"
                :precision="2"
                suffix="%"
                :valueStyle="{ color: '#1890ff' }"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="考试完成率"
                :value="statistics.examCompletionRate || 0"
                :precision="2"
                suffix="%"
                :valueStyle="{ color: '#52c41a' }"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="作业完成率"
                :value="statistics.assignmentCompletionRate || 0"
                :precision="2"
                suffix="%"
                :valueStyle="{ color: '#faad14' }"
              />
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card>
              <a-statistic
                title="调研完成率"
                :value="statistics.surveyCompletionRate || 0"
                :precision="2"
                suffix="%"
                :valueStyle="{ color: '#722ed1' }"
              />
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

        <!-- 考试统计区 -->
        <a-card title="考试统计" :bordered="false" class="exam-card">
          <a-row :gutter="16">
            <a-col :span="12">
              <div ref="examScoreChartRef" style="height: 300px"></div>
            </a-col>
            <a-col :span="12">
              <a-table
                :columns="examColumns"
                :data-source="examData"
                :pagination="false"
                size="small"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'passRate'">
                    <a-progress
                      :percent="record.passRate"
                      size="small"
                      :status="record.passRate >= 60 ? 'success' : 'exception'"
                    />
                  </template>
                </template>
              </a-table>
            </a-col>
          </a-row>
        </a-card>

        <!-- 调研统计区 -->
        <a-card title="调研统计" :bordered="false" class="survey-card">
          <a-empty v-if="!hasSurveyData" description="暂无调研数据" />
          <div v-else ref="surveyChartRef" style="height: 300px"></div>
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
import * as XLSX from 'xlsx';
import { getTrainDetail, getTrainStatistics } from '@/api/train';
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
  DownloadOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  PieChartOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'TrainAnalysisPage',
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
    DownloadOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    PieChartOutlined
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const route = useRoute();
    const trainId = ref(parseInt(route.params.id) || 0);
    const loading = ref(false);
    const collapsed = ref(false);
    const selectedKeys = ref(['trainList']);
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

    // 培训详情
    const trainDetail = ref({
      id: 0,
      name: '',
      type: '',
      description: '',
      status: '',
      publishTime: '',
      creatorId: null,
      creatorName: ''
    });

    // 统计数据
    const statistics = ref({
      learnerCount: 0,
      completionCount: 0,
      completionRate: 0,
      avgDuration: 0,
      totalDuration: 0,
      courseCompletionRate: 0,
      examCompletionRate: 0,
      assignmentCompletionRate: 0,
      surveyCompletionRate: 0,
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

    // 考试数据
    const examData = ref([
      { key: '1', examName: '期中考试', avgScore: 85, maxScore: 98, minScore: 65, passRate: 90 },
      { key: '2', examName: '期末考试', avgScore: 78, maxScore: 95, minScore: 60, passRate: 85 }
    ]);

    // 考试表格列定义
    const examColumns = [
      {
        title: '考试名称',
        dataIndex: 'examName',
        key: 'examName'
      },
      {
        title: '平均分',
        dataIndex: 'avgScore',
        key: 'avgScore'
      },
      {
        title: '最高分',
        dataIndex: 'maxScore',
        key: 'maxScore'
      },
      {
        title: '最低分',
        dataIndex: 'minScore',
        key: 'minScore'
      },
      {
        title: '通过率',
        dataIndex: 'passRate',
        key: 'passRate'
      }
    ];

    // 是否有调研数据
    const hasSurveyData = ref(true);

    // 图表引用
    const timeChartRef = ref(null);
    const deptChartRef = ref(null);
    const examScoreChartRef = ref(null);
    const surveyChartRef = ref(null);
    let timeChart = null;
    let deptChart = null;
    let examScoreChart = null;
    let surveyChart = null;

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

    // 获取培训详情
    const fetchTrainDetail = async () => {
      if (!trainId.value) {
        message.error('培训ID不存在');
        router.push('/train/list');
        return;
      }

      loading.value = true;
      try {
        const res = await getTrainDetail(trainId.value);
        if (res.code === 200 && res.data) {
          trainDetail.value = res.data;
        } else {
          message.error(res.message || '获取培训详情失败');
          router.push('/train/list');
        }
      } catch (error) {
        console.error('获取培训详情失败:', error);
        router.push('/train/list');
      } finally {
        loading.value = false;
      }
    };

    // 获取统计数据
    const fetchStatistics = async () => {
      if (!trainId.value) return;

      loading.value = true;
      try {
        const res = await getTrainStatistics(trainId.value);
        if (res.code === 200 && res.data) {
          statistics.value = res.data;

          // 初始化图表
          nextTick(() => {
            initTimeChart();
            initDeptChart();
            initExamScoreChart();
            initSurveyChart();
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
      const deptCompletionCounts = statistics.value.departmentStats.map(item => item.completionCount);

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['学习人数', '完成人数']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: deptNames,
          axisLabel: {
            interval: 0,
            rotate: 30
          }
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '学习人数',
            type: 'bar',
            data: deptCounts,
            itemStyle: {
              color: '#1890ff'
            }
          },
          {
            name: '完成人数',
            type: 'bar',
            data: deptCompletionCounts,
            itemStyle: {
              color: '#52c41a'
            }
          }
        ]
      };

      deptChart.setOption(option);

      // 窗口大小变化时重新调整图表大小
      window.addEventListener('resize', () => {
        deptChart && deptChart.resize();
      });
    };

    // 初始化考试分数分布图表
    const initExamScoreChart = () => {
      if (!examScoreChartRef.value) return;

      if (examScoreChart) {
        examScoreChart.dispose();
      }

      examScoreChart = echarts.init(examScoreChartRef.value);

      const option = {
        title: {
          text: '考试分数分布',
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [
          {
            name: '分数段',
            type: 'pie',
            radius: '50%',
            data: [
              { value: 15, name: '90-100分' },
              { value: 30, name: '80-89分' },
              { value: 25, name: '70-79分' },
              { value: 20, name: '60-69分' },
              { value: 10, name: '60分以下' }
            ],
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      };

      examScoreChart.setOption(option);

      // 窗口大小变化时重新调整图表大小
      window.addEventListener('resize', () => {
        examScoreChart && examScoreChart.resize();
      });
    };

    // 初始化调研统计图表
    const initSurveyChart = () => {
      if (!surveyChartRef.value || !hasSurveyData.value) return;

      if (surveyChart) {
        surveyChart.dispose();
      }

      surveyChart = echarts.init(surveyChartRef.value);

      const option = {
        title: {
          text: '调研问题回答分布',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['非常满意', '满意', '一般', '不满意', '非常不满意'],
          top: 'bottom'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['培训内容', '讲师水平', '培训形式', '培训时长', '培训效果']
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '非常满意',
            type: 'bar',
            stack: 'total',
            emphasis: {
              focus: 'series'
            },
            data: [30, 25, 20, 15, 28]
          },
          {
            name: '满意',
            type: 'bar',
            stack: 'total',
            emphasis: {
              focus: 'series'
            },
            data: [40, 35, 30, 25, 32]
          },
          {
            name: '一般',
            type: 'bar',
            stack: 'total',
            emphasis: {
              focus: 'series'
            },
            data: [20, 25, 30, 35, 25]
          },
          {
            name: '不满意',
            type: 'bar',
            stack: 'total',
            emphasis: {
              focus: 'series'
            },
            data: [8, 10, 15, 18, 10]
          },
          {
            name: '非常不满意',
            type: 'bar',
            stack: 'total',
            emphasis: {
              focus: 'series'
            },
            data: [2, 5, 5, 7, 5]
          }
        ]
      };

      surveyChart.setOption(option);

      // 窗口大小变化时重新调整图表大小
      window.addEventListener('resize', () => {
        surveyChart && surveyChart.resize();
      });
    };

    // 处理筛选变化
    const handleFilterChange = () => {
      // 根据筛选条件重新获取统计数据
      fetchStatistics();
    };

    // 重置筛选条件
    const resetFilter = () => {
      filterFormRef.value.resetFields();
      handleFilterChange();
    };

    // 获取状态文本
    const getStatusText = (status) => {
      const statusMap = {
        'draft': '草稿',
        'published': '已发布',
        'unpublished': '未发布',
        'archived': '已归档'
      };
      return statusMap[status] || status;
    };

    // 导出数据到Excel
    const exportToExcel = () => {
      try {
        // 创建工作簿
        const wb = XLSX.utils.book_new();

        // 创建培训基本信息工作表
        const basicInfoData = [
          ['培训名称', trainDetail.value.name],
          ['培训类型', trainDetail.value.type],
          ['创建人', trainDetail.value.creatorName],
          ['发布时间', trainDetail.value.publishTime],
          ['状态', getStatusText(trainDetail.value.status)]
        ];
        const basicInfoWs = XLSX.utils.aoa_to_sheet(basicInfoData);
        XLSX.utils.book_append_sheet(wb, basicInfoWs, '培训基本信息');

        // 创建统计数据工作表
        const statsData = [
          ['指标', '数值'],
          ['学习人数', statistics.value.learnerCount],
          ['完成人数', statistics.value.completionCount],
          ['完成率', `${statistics.value.completionRate}%`],
          ['平均学习时长(分钟)', statistics.value.avgDuration],
          ['总学习时长(分钟)', statistics.value.totalDuration],
          ['课程完成率', `${statistics.value.courseCompletionRate}%`],
          ['考试完成率', `${statistics.value.examCompletionRate}%`],
          ['作业完成率', `${statistics.value.assignmentCompletionRate}%`],
          ['调研完成率', `${statistics.value.surveyCompletionRate}%`]
        ];
        const statsWs = XLSX.utils.aoa_to_sheet(statsData);
        XLSX.utils.book_append_sheet(wb, statsWs, '统计数据');

        // 创建部门统计工作表
        const deptHeaders = ['部门', '学习人数', '完成人数', '完成率'];
        const deptData = [deptHeaders];
        statistics.value.departmentStats.forEach(dept => {
          const completionRate = dept.count > 0 ? ((dept.completionCount / dept.count) * 100).toFixed(2) : '0.00';
          deptData.push([dept.name, dept.count, dept.completionCount, `${completionRate}%`]);
        });
        const deptWs = XLSX.utils.aoa_to_sheet(deptData);
        XLSX.utils.book_append_sheet(wb, deptWs, '部门统计');

        // 创建时间分布工作表
        const timeHeaders = ['日期', '学习人数'];
        const timeData = [timeHeaders];
        statistics.value.timeDistribution.forEach(item => {
          timeData.push([item.date, item.count]);
        });
        const timeWs = XLSX.utils.aoa_to_sheet(timeData);
        XLSX.utils.book_append_sheet(wb, timeWs, '时间分布');

        // 创建考试统计工作表
        const examHeaders = ['考试名称', '平均分', '最高分', '最低分', '通过率'];
        const examData = [examHeaders];
        examData.value.forEach(exam => {
          examData.push([exam.examName, exam.avgScore, exam.maxScore, exam.minScore, `${exam.passRate}%`]);
        });
        const examWs = XLSX.utils.aoa_to_sheet(examData);
        XLSX.utils.book_append_sheet(wb, examWs, '考试统计');

        // 导出Excel文件
        const fileName = `培训数据分析_${trainDetail.value.name}_${dayjs().format('YYYYMMDD')}.xlsx`;
        XLSX.writeFile(wb, fileName);

        message.success('导出成功');
      } catch (error) {
        console.error('导出Excel失败:', error);
      }
    };

    // 返回培训跟踪页面
    const goBack = () => {
      router.push(`/train/tracking/${trainId.value}`);
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

      // 获取培训详情
      fetchTrainDetail();

      // 获取部门列表
      fetchDepartments();

      // 获取统计数据
      fetchStatistics();
    });

    return {
      trainId,
      loading,
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      trainDetail,
      statistics,
      departments,
      filterFormRef,
      filterForm,
      examData,
      examColumns,
      hasSurveyData,
      timeChartRef,
      deptChartRef,
      examScoreChartRef,
      surveyChartRef,
      handleFilterChange,
      resetFilter,
      getStatusText,
      exportToExcel,
      goBack,
      handleLogout,
      navigateTo
    };
  }
});
</script>

<style scoped>
.train-analysis-layout {
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

.exam-card {
  margin-bottom: 24px;
}

.survey-card {
  margin-bottom: 24px;
}
</style>
