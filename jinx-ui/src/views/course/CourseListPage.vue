<template>
  <a-layout class="course-list-layout">
    <!-- 使用 HeaderComponent -->
    <HeaderComponent activeKey="MANAGEMENT_CENTER" :showSearch="false" />

    <a-layout>
      <!-- 使用 SiderComponent -->
      <SiderComponent v-model:collapsed="collapsed" v-model:selectedKeys="selectedKeys" v-model:openKeys="openKeys"
        @category-modal-show="showCategoryModal" />

      <!-- 内容区 -->
      <a-layout-content class="content">
        <div class="content-header">
          <PageBreadcrumb />
          <div class="content-header-right">
            <a-button v-if="hasPermission('course:create')" type="primary" @click="navigateTo('/course/create')">
              <plus-outlined />创建课程
            </a-button>
          </div>
        </div>

        <!-- 搜索和筛选区域 -->
        <a-card class="search-card">
          <a-form layout="inline" :model="searchForm" :label-col="{ style: { width: '80px' } }"
            :wrapperCol="{ style: { flex: 1 } }">
            <a-row :gutter="16" style="width: 100%">
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="课程名称" style="width: 100%;">
                  <a-input v-model:value="searchForm.title" placeholder="请输入课程名称" allowClear />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="课程类型">
                  <a-select v-model:value="searchForm.type" placeholder="请选择课程类型" allowClear>
                    <a-select-option value="video">视频</a-select-option>
                    <a-select-option value="document">文档</a-select-option>
                    <a-select-option value="series">系列课</a-select-option>
                    <a-select-option value="article">文章</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="发布状态">
                  <a-select v-model:value="searchForm.status" placeholder="请选择发布状态" allowClear>
                    <a-select-option value="published">已发布</a-select-option>
                    <a-select-option value="draft">未发布</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="分类">
                  <CategoryTreeSelect multiple v-model:value="searchForm.categoryIds" />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="创建人">
                  <UserSelect v-model:value="searchForm.creatorId" :show-search="true" placeholder="请选择创建人" allowClear />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="创建时间">
                  <a-range-picker v-model:value="dateRange" style="width: 100%" @change="handleDateRangeChange" />
                </a-form-item>
              </a-col>
              <a-col :span="24" :xxl="{ span: 12 }" style="display: flex;justify-content: flex-end">
                <a-form-item>
                  <a-checkbox v-model:checked="searchForm.onlyMine">只看我创建的</a-checkbox>
                  <a-space>
                    <a-button type="primary" @click="handleSearch">
                      <search-outlined />搜索
                    </a-button>
                    <a-button @click="handleReset">
                      <reload-outlined />重置
                    </a-button>
                  </a-space>
                </a-form-item>
              </a-col>
            </a-row>
          </a-form>
        </a-card>

        <!-- 表格操作区域 -->
        <a-card class="table-card">
          <div class="table-operations">
            <a-space>
              <a-button v-if="hasPermission('course:batchDelete')" type="danger"
                :disabled="selectedRowKeys.length === 0" @click="handleBatchDelete">
                <delete-outlined />批量删除
              </a-button>
              <a-button v-if="hasPermission('course:batchChangeCreator')" :disabled="selectedRowKeys.length === 0"
                @click="showChangeCreatorModal">
                <user-switch-outlined />批量变更创建人
              </a-button>
            </a-space>
          </div>

          <!-- 课程列表表格 -->
          <a-table :columns="columns" :data-source="courseList" :pagination="pagination" :loading="loading"
            :row-selection="{ selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id"
            :scroll="{ x: '100%' }" @change="handleTableChange">
            <!-- 封面图列 -->
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'coverImage'">
                <a-image :width="40" :src="record.coverImage || 'https://via.placeholder.com/40'" />
              </template>

              <!-- 课程名称列 -->
              <template v-if="column.key === 'title'">
                <!-- <a @click="navigateTo(`/course/detail/${record.id}`)">{{ record.title }}</a> -->
                {{ record.title }}
              </template>

              <!-- 课程类型列 -->
              <template v-if="column.key === 'type'">
                <a-tag :color="getTypeColor(record.type)">{{ getTypeText(record.type) }}</a-tag>
              </template>

              <!-- 分类列 -->
              <template v-if="column.key === 'categoryNames'">
                <template v-if="record.categoryNames && record.categoryNames.length">
                  <a-tag v-for="(category, index) in record.categoryNames" :key="index" color="blue">
                    {{ category }}
                  </a-tag>
                </template>
                <span v-else>-</span>
              </template>

              <!-- 状态列 -->
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === 'published' ? 'green' : 'orange'">
                  {{ record.status === 'published' ? '已发布' : '未发布' }}
                </a-tag>
              </template>

              <!-- 操作列 -->
              <template v-if="column.key === 'action'">
                <a-space wrap>
                  <!-- 发布/取消发布 -->
                  <a-tooltip v-if="hasPermission('course:publish')">
                    <template #title>{{ record.status === 'published' ? '取消发布' : '发布' }}</template>
                    <a @click="handlePublishToggle(record)">
                      <check-circle-outlined v-if="record.status !== 'published'" />
                      <stop-outlined v-else />
                    </a>
                  </a-tooltip>

                  <!-- 编辑 -->
                  <a-tooltip v-if="hasPermission('course:edit')">
                    <template #title>编辑</template>
                    <a @click="navigateTo(`/course/edit/${record.id}`)">
                      <edit-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 可见范围 -->
                  <a-tooltip v-if="hasPermission('course:visibility')">
                    <template #title>可见范围</template>
                    <a @click="showVisibilityModal(record)">
                      <eye-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 分类 -->
                  <a-tooltip v-if="hasPermission('course:category')">
                    <template #title>分类</template>
                    <a @click="showCategorySettingModal(record)">
                      <appstore-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 置顶/取消置顶 -->
                  <!-- <a-tooltip v-if="hasPermission('course:top')">
                    <template #title>{{ record.isTop ? '取消置顶' : '置顶' }}</template>
                    <a @click="handleTopToggle(record)">
                      <vertical-align-top-outlined v-if="!record.isTop" />
                      <vertical-align-bottom-outlined v-else />
                    </a>
                  </a-tooltip> -->

                  <!-- 学完明细 -->
                  <a-tooltip v-if="hasPermission('course:learners')">
                    <template #title>学完明细</template>
                    <a @click="showLearnersModal(record)">
                      <team-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 查看关联项 -->
                  <a-tooltip v-if="hasPermission('course:related')">
                    <template #title>查看关联项</template>
                    <a @click="showRelatedModal(record)">
                      <link-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 删除 -->
                  <a-tooltip v-if="hasPermission('course:delete')">
                    <template #title>删除</template>
                    <a-popconfirm title="确定要删除此课程吗？" ok-text="确定" cancel-text="取消" @confirm="handleDelete(record)">
                      <a class="danger-link"><delete-outlined /></a>
                    </a-popconfirm>
                  </a-tooltip>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-layout-content>
    </a-layout>

    <!-- 可见范围设置弹窗 -->
    <a-modal v-model:visible="visibilityModalVisible" title="可见范围设置" width="800px" :footer="null" destroyOnClose>
      <visibility-setting-component v-if="visibilityModalVisible" :biz-id="currentCourse.id" business-type="COURSE"
        :selected-range="selectedRange" @success="handleVisibilitySuccess" @cancel="visibilityModalVisible = false" />
    </a-modal>

    <!-- 分类设置弹窗 -->
    <a-modal v-model:visible="categorySettingModalVisible" title="课程分类设置" width="600px" :footer="null" destroyOnClose>
      <category-setting-component v-if="categorySettingModalVisible" :biz-id="currentCourse.id" biz-type="COURSE"
        :current-categories="currentCourse.categoryIds?.split(',') || []" @success="handleCategorySettingSuccess"
        @cancel="categorySettingModalVisible = false" />
    </a-modal>

    <!-- 学习人员明细弹窗 -->
    <a-modal v-model:visible="learnersModalVisible" title="学习人员明细" width="800px" :footer="null" destroyOnClose>
      <div v-if="learnersModalVisible" class="learners-container">
        <a-form layout="inline" :model="learnersSearchForm">
          <a-form-item label="姓名">
            <UserSelect v-model:value="learnersSearchForm.userId" style="width: 220px" />
          </a-form-item>
          <a-form-item label="完成状态">
            <a-select v-model:value="learnersSearchForm.status" placeholder="请选择状态" style="width: 120px">
              <a-select-option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item><a-button type="primary" @click="fetchLearners">
              <search-outlined />搜索
            </a-button>
          </a-form-item>
        </a-form>

        <a-table ref="tableRef" :columns="learnersColumns" :data-source="learnersList" :pagination="learnersPagination"
          :loading="learnersLoading" @change="handleLearnersTableChange" style="margin-top: 16px">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'progress'">
              <a-progress :percent="record.progress" size="small" style="width: 100px" />
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">  {{ getStatusText(record.status) }}</a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>

    <!-- 关联项目弹窗 -->
    <relate-component v-model:visible="relatedModalVisible" :biz-id="currentCourse.id" biz-type="COURSE" />

    <!-- 批量变更创建人弹窗 -->
    <a-modal v-model:visible="changeCreatorModalVisible" title="批量变更创建人" @ok="handleChangeCreator"
      @cancel="changeCreatorModalVisible = false" :confirmLoading="changeCreatorLoading">
      <a-form :model="changeCreatorForm" layout="vertical">
        <a-form-item label="新创建人" name="newCreatorId" :rules="[{ required: true, message: '请选择新创建人' }]">
          <UserSelect v-model:value="changeCreatorForm.newCreatorId" />
        </a-form-item>
      </a-form>
    </a-modal>

  </a-layout>
</template>

<script>
import { ref, reactive, computed, onMounted, defineComponent, watch } from 'vue';
import HeaderComponent from '@/components/common/HeaderComponent.vue';
import SiderComponent from '@/components/common/SiderComponent.vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  getCourseList,
  deleteCourse,
  batchDeleteCourse,
  publishCourse,
  unpublishCourse,
  updateCourse,
  getCourseLearners,
  batchUpdateCreator
} from '@/api/course';
import { getCategoryList } from '@/api/category';
import { hasPermission } from '@/utils/permission';
import VisibilitySettingComponent from '@/components/common/VisibilitySettingComponent.vue';
import CategorySettingComponent from '@/components/common/CategorySettingComponent.vue';
import RelateComponent from '@/components/common/RelateComponent.vue';
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
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  StopOutlined,
  TeamOutlined,
  LinkOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignBottomOutlined,
  UserSwitchOutlined
} from '@ant-design/icons-vue';
import { nextTick } from 'vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';
import UserSelect from '@/components/common/UserSelect.vue';
import { queryRange } from '@/api/common';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';

export default defineComponent({
  name: 'CourseListPage',
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
    PlusOutlined,
    SearchOutlined,
    ReloadOutlined,
    DeleteOutlined,
    EditOutlined,
    EyeOutlined,
    CheckCircleOutlined,
    StopOutlined,
    TeamOutlined,
    LinkOutlined,
    VerticalAlignTopOutlined,
    VerticalAlignBottomOutlined,
    UserSwitchOutlined,
    VisibilitySettingComponent,
    CategorySettingComponent,
    RelateComponent,
    PageBreadcrumb,
    UserSelect,
    CategoryTreeSelect
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const collapsed = ref(false);
    const selectedKeys = ref(['courseList']);
    const openKeys = ref(['learning']);
    const categoryModalVisible = ref(false);


    const statusOptions = [
      { value: '', label: '全部' ,color:'blue'},
      { value: 'not_started', label: '未开始' ,color:'default'},
      { value: 'learning', label: '学习中' ,color:'blue'},
      { value: 'completed', label: '已完成' ,color:'green'},
      { value: 'expired', label: '已过期' ,color: 'red'}
    ];

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

    const courseList = ref([]);
    const loading = ref(false);
    const selectedRowKeys = ref([]);

    const tableRef = ref();

    // 分类列表
    const categories = ref([]);


    // 日期范围
    const dateRange = ref([]);

    // 搜索表单
    const searchForm = reactive({
      title: '',
      type: undefined,
      status: undefined,
      categoryIds: [],
      creatorId: undefined,
      startTime: '',
      endTime: '',
      onlyMine: false,
      pageNum: 1,
      pageSize: 10,
      sortField: 'gmtCreate',
      sortOrder: 'desc'
    });

    // 分页配置
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showQuickJumper: true,
      showTotal: (total) => `共 ${total} 条记录`
    });

    // 表格列定义
    const columns = [
      {
        title: 'id',
        dataIndex: 'id',
        key: 'id',
        fixed: 'left',
        width: 60
      },
      {
        title: '封面',
        dataIndex: 'coverImage',
        key: 'coverImage',
        fixed: 'left',
        width: 60
      },
      {
        title: '课程名称',
        dataIndex: 'title',
        key: 'title',
        ellipsis: true,
        fixed: 'left',
        width: 200
      },
      {
        title: '类型',
        dataIndex: 'type',
        key: 'type',
        width: 80
      },
      {
        title: '分类',
        dataIndex: 'categoryNames',
        key: 'categoryNames',
        width: 150
      },
      {
        title: '学分',
        dataIndex: 'credit',
        key: 'credit',
        width: 60
      },
      {
        title: '创建人',
        dataIndex: 'creatorName',
        key: 'creatorName',
        width: 100
      },
      {
        title: '查看数',
        dataIndex: 'viewCount',
        key: 'viewCount',
        width: 80
      },
      {
        title: '完成人数',
        dataIndex: 'completeCount',
        key: 'completeCount',
        width: 80
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status',
        width: 80
      },
      {
        title: '创建时间',
        dataIndex: 'gmtCreate',
        key: 'gmtCreate',
        width: 150
      },
      {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 200
      }
    ];

    // 当前选中的课程
    const currentCourse = ref({});

    // 可见范围弹窗
    const visibilityModalVisible = ref(false);// 分类设置弹窗
    const categorySettingModalVisible = ref(false);

    // 学习人员明细弹窗
    const learnersModalVisible = ref(false);
    const learnersLoading = ref(false);
    const learnersList = ref([]);
    const learnersSearchForm = reactive({
      userId: undefined,
      status: 'all',
      pageNum: 1,
      pageSize: 10
    });

    const learnersPagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条记录`
    });

    const learnersColumns = [
      {
        title: '姓名',
        dataIndex: 'userName',
        key: 'userName'
      },
      {
        title: '工号',
        dataIndex: 'employeeNo',
        key: 'employeeNo'
      },
      {
        title: '学习进度',
        dataIndex: 'progress',
        key: 'progress'
      },
      {
        title: '状态',
        dataIndex: 'status',
        key: 'status'
      },
      {
        title: '开始时间',
        dataIndex: 'startTime',
        key: 'startTime'
      },
      {
        title: '完成时间',
        dataIndex: 'completionTime',
        key: 'completionTime'
      }
    ];

    // 关联项目弹窗
    const relatedModalVisible = ref(false);

    // 批量变更创建人弹窗
    const changeCreatorModalVisible = ref(false);
    const changeCreatorLoading = ref(false);
    const changeCreatorForm = reactive({
      newCreatorId: undefined
    });

    // 获取状态颜色
    const getStatusColor = (status) => {
      return statusOptions.find(option => option.value === status)?.color || 'default';
    };

    // 获取状态文本
    const getStatusText = (status) => {
      return statusOptions.find(option => option.value === status)?.label || '';
    };


    // 获取课程列表
    const fetchCourseList = async () => {
      loading.value = true;
      try {
        const res = await getCourseList(searchForm);
        if (res.code === 200 && res.data) {
          courseList.value = res.data.list || [];
          pagination.total = res.data.total || 0;
          pagination.current = searchForm.pageNum;
          pagination.pageSize = searchForm.pageSize;

          /** 尝试解决固定列阴影问题，但是不生效 */
          nextTick(() => {
            // 可以尝试调用表格的方法重新布局
            tableRef.value?.reload();
          });
        } else {
          message.error(res.message || '获取课程列表失败');
        }
      } catch (error) {
        console.error('获取课程列表失败:', error);
        message.error('获取课程列表失败');
      } finally {
        loading.value = false;
      }
    };

    // 获取分类列表
    const fetchCategories = async () => {
      try {
        const res = await getCategoryList();
        if (res.code === 200) {
          categories.value = res.data || [];
        }
      } catch (error) {
        console.error('获取分类列表失败:', error);
      }
    };

    // 处理日期范围变更
    const handleDateRangeChange = (dates) => {
      if (dates && dates.length === 2) {
        searchForm.startTime = dates[0].format('YYYY-MM-DD');
        searchForm.endTime = dates[1].format('YYYY-MM-DD');
      } else {
        searchForm.startTime = '';
        searchForm.endTime = '';
      }
    };

    // 处理搜索
    const handleSearch = () => {
      searchForm.pageNum = 1;
      fetchCourseList();
    };

    // 处理重置
    const handleReset = () => {
      // 重置搜索表单
      searchForm.title = '';
      searchForm.type = undefined;
      searchForm.status = undefined;
      searchForm.categoryIds = [];
      searchForm.creatorId = undefined;
      searchForm.startTime = '';
      searchForm.endTime = '';
      searchForm.onlyMine = false;
      searchForm.pageNum = 1;
      searchForm.sortField = 'gmtCreate';
      searchForm.sortOrder = 'desc';

      // 重置日期范围
      dateRange.value = [];

      // 重新获取数据
      fetchCourseList();
    };

    // 处理表格变更
    const handleTableChange = (pag, filters, sorter) => {
      // 处理分页
      searchForm.pageNum = pag.current;
      searchForm.pageSize = pag.pageSize;

      // 处理排序
      if (sorter && sorter.field) {
        searchForm.sortField = sorter.field;
        searchForm.sortOrder = sorter.order === 'ascend' ? 'asc' : 'desc';
      }

      // 重新获取数据
      fetchCourseList();
    };

    // 处理选择变更
    const onSelectChange = (keys) => {
      selectedRowKeys.value = keys;
    };

    // 获取课程类型文本
    const getTypeText = (type) => {
      const typeMap = {
        video: '视频',
        document: '文档',
        series: '系列课',
        article: '文章'
      };
      return typeMap[type] || type;
    };

    // 获取课程类型颜色
    const getTypeColor = (type) => {
      const colorMap = {
        video: 'blue',
        document: 'green',
        series: 'purple',
        article: 'orange'
      };
      return colorMap[type] || 'default';
    };

    // 处理删除课程
    const handleDelete = async (record) => {
      try {
        const res = await deleteCourse(record.id);
        if (res.code === 200) {
          message.success('删除成功');
          // 重新获取数据
          fetchCourseList();
        } else {
          message.error(res.message || '删除失败');
        }
      } catch (error) {
        console.error('删除课程失败:', error);
      }
    };

    // 处理批量删除
    const handleBatchDelete = async () => {
      if (selectedRowKeys.value.length === 0) {
        message.warning('请选择要删除的课程');
        return;
      }

      try {
        const res = await batchDeleteCourse(selectedRowKeys.value);
        if (res.code === 200) {
          message.success(`成功删除 ${selectedRowKeys.value.length} 个课程`);

          // 清空选择
          selectedRowKeys.value = [];
          // 重新获取数据
          fetchCourseList();
        } else {
          message.error(res.message || '批量删除失败');
        }
      } catch (error) {
        console.error('批量删除课程失败:', error);
      }
    };

    // 处理发布/取消发布
    const handlePublishToggle = async (record) => {
      try {
        let res;
        if (record.status === 'published') {
          // 取消发布
          res = await unpublishCourse(record.id);
          if (res.code === 200) {
            message.success('取消发布成功');
          } else {
            message.error(res.message || '取消发布失败');
          }
        } else {
          // 发布
          res = await publishCourse(record.id);
          if (res.code === 200) {
            message.success('发布成功');
          } else {
            message.error(res.message || '发布失败');
          }
        }

        // 重新获取数据
        fetchCourseList();
      } catch (error) {
        console.error('操作失败:', error);
      }
    };

    // 处理置顶/取消置顶
    const handleTopToggle = async (record) => {
      try {
        const res = await updateCourse({
          id: record.id,
          isTop: record.isTop ? 0 : 1
        });

        if (res.code === 200) {
          message.success(record.isTop ? '取消置顶成功' : '置顶成功');
          // 重新获取数据
          fetchCourseList();
        } else {
          message.error(res.message || '操作失败');
        }
      } catch (error) {
        console.error('操作失败:', error);
      }
    };

    // 可见范围
    const selectedRange = ref([]);
    /** 获取范围回显 */
    const getRange = (id) => {
      queryRange({
        businessType: 'COURSE',
        businessId: id,
        functionType: 'visibility'
      }).then(res => {
        console.log(res, '范围配置')

        const tempArray = [];

        const { departmentInfos, roleInfos, userInfos } = res.data;
        departmentInfos.forEach(item => {
          tempArray.push({ id: item.departmentId, type: 'department', name: item.departmentName });
        })
        roleInfos.forEach(item => {
          tempArray.push({ id: item.roleId, type: 'role', name: item.roleName });
        })
        userInfos.forEach(item => {
          tempArray.push({ id: item.userId, type: 'user', name: item.userName });
        })

        selectedRange.value = tempArray;
      });
    };

    // 显示可见范围弹窗
    const showVisibilityModal = (record) => {
      currentCourse.value = record;
      getRange(record.id);
      visibilityModalVisible.value = true;
    };

    // 处理可见范围设置成功
    const handleVisibilitySuccess = () => {
      visibilityModalVisible.value = false;
    };

    // 显示分类设置弹窗
    const showCategorySettingModal = (record) => {
      currentCourse.value = record;
      categorySettingModalVisible.value = true;
    };

    // 处理分类设置成功
    const handleCategorySettingSuccess = () => {
      categorySettingModalVisible.value = false;
      // 重新获取数据
      fetchCourseList();
    };

    // 显示学习人员明细弹窗
    const showLearnersModal = (record) => {
      currentCourse.value = record;
      learnersModalVisible.value = true;
      // 重置搜索表单
      learnersSearchForm.userId = undefined;
      learnersSearchForm.status = 'all';
      learnersSearchForm.pageNum = 1;
      learnersSearchForm.pageSize = 10;
      // 获取学习人员数据
      fetchLearners();
    };

    // 获取学习人员数据
    const fetchLearners = async () => {
      if (!currentCourse.value.id) return;

      learnersLoading.value = true;
      try {
        const res = await getCourseLearners(currentCourse.value.id, learnersSearchForm);
        if (res.code === 200 && res.data) {
          learnersList.value = res.data.list || [];
          learnersPagination.total = res.data.total || 0;
          learnersPagination.current = learnersSearchForm.pageNum;
          learnersPagination.pageSize = learnersSearchForm.pageSize;
        } else {
          message.error(res.message || '获取学习人员列表失败');
        }
      } catch (error) {
        console.error('获取学习人员列表失败:', error);
      } finally {
        learnersLoading.value = false;
      }
    };

    // 处理学习人员表格变更
    const handleLearnersTableChange = (pag) => {
      learnersSearchForm.pageNum = pag.current;
      learnersSearchForm.pageSize = pag.pageSize;
      fetchLearners();
    };

    // 显示关联项目弹窗
    const showRelatedModal = (record) => {
      currentCourse.value = record;
      relatedModalVisible.value = true;
    };

    // 显示批量变更创建人弹窗
    const showChangeCreatorModal = () => {
      if (selectedRowKeys.value.length === 0) {
        message.warning('请选择要操作的课程');
        return;
      }

      changeCreatorForm.newCreatorId = undefined;
      changeCreatorModalVisible.value = true;
    };

    // 处理批量变更创建人
    const handleChangeCreator = async () => {
      if (!changeCreatorForm.newCreatorId) {
        message.warning('请选择新创建人');
        return;
      }

      changeCreatorLoading.value = true;
      try {
        const res = await batchUpdateCreator({
          courseIds: selectedRowKeys.value,
          newCreatorId: changeCreatorForm.newCreatorId
        });

        if (res.code === 200) {
          message.success('批量变更创建人成功');
          changeCreatorModalVisible.value = false;

          // 清空选择
          selectedRowKeys.value = [];

          // 重新获取数据
          fetchCourseList();
        } else {
          message.error(res.message || '批量变更创建人失败');
        }
      } catch (error) {
        console.error('批量变更创建人失败:', error);
      } finally {
        changeCreatorLoading.value = false;
      }
    };

    // 过滤用户选项
    const filterUserOption = (input, option) => {
      return (
        option.children[0].toLowerCase().indexOf(input.toLowerCase()) >= 0 ||
        option.children[2].toLowerCase().indexOf(input.toLowerCase()) >= 0
      );
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

    // 显示类目管理弹窗
    const showCategoryModal = () => {
      categoryModalVisible.value = true;
    };


    onMounted(() => {
      // 获取分类列表
      fetchCategories();

      // 获取课程列表
      fetchCourseList();
    });

    return {
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      courseList,
      loading,
      selectedRowKeys,
      categories,
      dateRange,
      searchForm,
      pagination,
      columns,
      currentCourse,
      visibilityModalVisible,
      categorySettingModalVisible,
      learnersModalVisible,
      learnersLoading,
      learnersList,
      learnersSearchForm,
      learnersPagination,
      learnersColumns,
      relatedModalVisible,
      changeCreatorModalVisible,
      changeCreatorLoading,
      changeCreatorForm,
      categoryModalVisible,
      selectedRange,
      statusOptions,
      getStatusColor,
      getStatusText,
      hasPermission,
      handleDateRangeChange,
      handleSearch,
      handleReset,
      handleTableChange,
      onSelectChange,
      getTypeText,
      getTypeColor,
      handleDelete,
      handleBatchDelete,
      handlePublishToggle,
      handleTopToggle,
      showVisibilityModal,
      handleVisibilitySuccess,
      showCategorySettingModal,
      handleCategorySettingSuccess,
      showLearnersModal,
      fetchLearners,
      handleLearnersTableChange,
      showRelatedModal,
      showChangeCreatorModal,
      handleChangeCreator,
      filterUserOption,
      handleLogout,
      navigateTo,
      showCategoryModal
    };
  }
});
</script>

<style scoped>
.course-list-layout {
  min-height: 100vh;
}

.content {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: 280px;
  overflow: auto;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.content-header h2 {
  margin: 0;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  background-color: #fff;
}

.table-operations {
  margin-bottom: 16px;
}

.danger-link {
  color: #ff4d4f;
}

.danger-link:hover {
  color: #ff7875;
}

.learners-container {
  padding: 16px;
}
</style>
