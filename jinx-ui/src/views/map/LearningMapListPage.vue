<template>
  <a-layout class="learning-map-list-layout">
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
            <a-button v-if="hasPermission('map:create')" type="primary" @click="navigateTo('/map/create')">
              <plus-outlined />创建学习地图
            </a-button>
          </div>
        </div>

        <!-- 搜索和筛选区域 -->
        <a-card class="search-card">
          <a-form layout="inline" :model="searchForm" :label-col="{ style: { width: '80px' } }"
            :wrapperCol="{ style: { flex: 1 } }">
            <a-row :gutter="16" style="width: 100%">
              <a-col :xs="{ span: 24 }" :lg="{ span: 12 }" :xl="{ span: 8 }" :xxl="{ span: 6 }">
                <a-form-item label="地图名称">
                  <a-input v-model:value="searchForm.name" placeholder="请输入地图名称" allowClear />
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
                <a-form-item label="更新时间">
                  <a-range-picker v-model:value="dateRange" style="width: 100%" @change="handleDateRangeChange" />
                </a-form-item>
              </a-col>
              <a-col :xs="{ span: 24 }" :lg="{ span: 24 }" :xl="{ span: 16 }" :xxl="{ span: 24 }"
                style="display: flex;justify-content: flex-end">
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
              <a-button v-if="hasPermission('map:batchDelete')" type="danger" :disabled="selectedRowKeys.length === 0"
                @click="handleBatchDelete">
                <delete-outlined />批量删除
              </a-button>
              <a-button v-if="hasPermission('map:batchChangeCreator')" type="primary" :disabled="selectedRowKeys.length === 0"
                @click="showChangeCreatorModal">
                <user-switch-outlined />批量变更创建人
              </a-button>
            </a-space>
          </div>

          <!-- 学习地图列表表格 -->
          <a-table :columns="columns" :data-source="learningMapList" :pagination="pagination" :loading="loading"
            :row-selection="{ selectedRowKeys, onChange: onSelectChange }" :row-key="record => record.id"
            :scroll="{ x: '100%' }" @change="handleTableChange">

            <!-- 封面图列 -->
            <template #bodyCell="{ column, record }">

              <template v-if="column.key === 'cover'">
                <a-image :width="40" :src="record.cover || 'https://via.placeholder.com/40'" />
              </template>

              <!-- 地图名称列 -->
              <template v-if="column.key === 'name'">
                <!-- <a @click="navigateTo(`/map/detail/${record.id}`)">{{ record.name }}</a> -->
                {{ record.name }}
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

              <!-- 学分列 -->
              <template v-if="column.key === 'credit'">
                <span>必修: {{ record.requiredCredit || 0 }}</span>
                <br />
                <span>选修: {{ record.electiveCredit || 0 }}</span>
              </template>

              <!-- 开放时间列 -->
              <template v-if="column.key === 'openTime'">
                <span v-if="record.startTime && record.endTime">
                  {{ record.startTime }} 至 {{ record.endTime }}
                </span>
                <span v-else-if="record.startTime">
                  {{ record.startTime }} 起
                </span>
                <span v-else-if="record.endTime">
                  截止至 {{ record.endTime }}
                </span>
                <span v-else>-</span>
              </template>

              <!-- 操作列 -->
              <template v-if="column.key === 'action'">
                <a-space wrap>
                  <!-- 编辑 -->
                  <a-tooltip v-if="hasPermission('map:edit')">
                    <template #title>编辑</template>
                    <a @click="navigateTo(`/map/edit/${record.id}`)">
                      <edit-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 可见范围 -->
                  <a-tooltip v-if="hasPermission('map:visibility')">
                    <template #title>可见范围</template>
                    <a @click="showVisibilityModal(record)">
                      <eye-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 指派 -->
                  <a-tooltip v-if="hasPermission('map:assign')">
                    <template #title>指派</template>
                    <a @click="showAssignModal(record)">
                      <user-add-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 跟踪 -->
                  <a-tooltip v-if="hasPermission('map:tracking')">
                    <template #title>跟踪</template>
                    <a @click="navigateTo(`/map/tracking/${record.id}`)">
                      <line-chart-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 分类 -->
                  <a-tooltip v-if="hasPermission('map:category')">
                    <template #title>分类</template>
                    <a @click="showCategorySettingModal(record)">
                      <appstore-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 指派记录 -->
                  <a-tooltip v-if="hasPermission('map:assignRecord')">
                    <template #title>指派记录</template>
                    <a @click="showAssignRecordModal(record)">
                      <file-text-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 置顶/取消置顶 -->
                  <!-- <a-tooltip v-if="hasPermission('map:top')">
                    <template #title>{{ record.isTop ? '取消置顶' : '置顶' }}</template>
                    <a @click="handleTopToggle(record)">
                      <vertical-align-top-outlined v-if="!record.isTop" />
                      <vertical-align-bottom-outlined v-else />
                    </a>
                  </a-tooltip> -->

                  <!-- 分享 -->
                  <a-tooltip v-if="hasPermission('map:share')">
                    <template #title>分享</template>
                    <a @click="handleShare(record)">
                      <share-alt-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 日志 -->
                  <a-tooltip v-if="hasPermission('map:log')">
                    <template #title>日志</template>
                    <a @click="showLogModal(record)">
                      <history-outlined />
                    </a>
                  </a-tooltip>

                  <!-- 删除 -->
                  <a-tooltip v-if="hasPermission('map:delete')">
                    <template #title>删除</template>
                    <a-popconfirm title="确定要删除此学习地图吗？" ok-text="确定" cancel-text="取消" @confirm="handleDelete(record)">
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
      <visibility-setting-component v-if="visibilityModalVisible" :biz-id="currentMap.id" biz-type="LEARNING_MAP"
        :selected-range="selectedRange" title="可见范围设置" @success="handleVisibilitySuccess" @cancel="visibilityModalVisible = false" />
    </a-modal>

    <!-- 分类设置弹窗 -->
    <a-modal v-model:visible="categorySettingModalVisible" title="学习地图分类设置" width="600px" :footer="null" destroyOnClose>
      <category-setting-component v-if="categorySettingModalVisible" :biz-id="currentMap.id" biz-type="LEARNING_MAP"
        :current-categories="currentMap.categoryIds?.split(',') || []" @success="handleCategorySettingSuccess"
        @cancel="categorySettingModalVisible = false" />
    </a-modal>

    <!-- 指派弹窗 -->
    <a-modal v-model:visible="assignModalVisible" title="指派学习地图" width="800px" :footer="null" destroyOnClose>
      <assign-page v-if="assignModalVisible" :visible="assignModalVisible" :biz-id="currentMap.id"
        biz-type="LEARNING_MAP" :name="assignType === 'auto' ? '自动指派' : '单次指派'" :assignType="assignType"
        @success="handleAssignSuccess" @cancel="assignModalVisible = false" />
    </a-modal>

    <!-- 指派类型选择弹窗 -->
    <a-modal v-model:visible="assignTypeModalVisible" title="选择指派方式" width="400px" :footer="null" destroyOnClose>
      <div class="assign-type-container">
        <div class="assign-type-item" @click="handleSingleAssign">
          <user-add-outlined class="assign-type-icon" />
          <div class="assign-type-content">
            <div class="assign-type-title">单次指派</div>
            <div class="assign-type-desc">直接指派到人，立即生效</div>
          </div>
        </div>
        <div class="assign-type-item" :class="{ 'disabled': !isAutoAssignEnabled }" @click="handleAutoAssign">
          <team-outlined class="assign-type-icon" />
          <div class="assign-type-content">
            <div class="assign-type-title">自动指派</div>
            <div class="assign-type-desc">指派特定范围，对新加入者也生效</div>
          </div>
        </div>
        <div class="auto-assign-switch">
          <span>自动指派</span>
          <a-tooltip v-if="!hasPermission('map:autoAssign')" title="只有管理员和创建人可以开关自动指派">
            <a-switch v-model:checked="isAutoAssignEnabled" :disabled="true" />
          </a-tooltip>
          <a-switch v-else v-model:checked="isAutoAssignEnabled" :disabled="!hasPermission('map:autoAssign')"
            @change="handleAutoAssignSwitchChange" />
        </div>
      </div>
    </a-modal>

    <!-- 日志弹窗 -->
    <a-modal v-model:visible="logModalVisible" title="操作日志" width="800px" :footer="null" destroyOnClose>
      <div v-if="logModalVisible" class="log-container">
        <a-table :columns="logColumns" :data-source="logList" :pagination="logPagination" :loading="logLoading"
          @change="handleLogTableChange">
        </a-table>
      </div>
    </a-modal>

    <!-- 指派记录弹窗 -->
    <AssignRecordsModalComponent
      v-model:visible="assignRecordModalVisible"
      :type="'LEARNING_MAP'"
      :typeId="currentMap.id"
    />

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
import { message, Modal } from 'ant-design-vue';
import dayjs from 'dayjs';
import {
  getLearningMapList,
  deleteLearningMap,
  batchDeleteLearningMap,
  updateLearningMapTopStatus,
  updateLearningMapAutoAssign,
  batchUpdateCreator
} from '@/api/learningMap';
import { getOperationLogs } from '@/api/operation';
import { getCategoryList } from '@/api/category';
import { hasPermission } from '@/utils/permission';
import VisibilitySettingComponent from '@/components/common/VisibilitySettingComponent.vue';
import CategorySettingComponent from '@/components/common/CategorySettingComponent.vue';
import AssignPage from '@/components/train/AssignPage.vue';
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
  UserAddOutlined,
  LineChartOutlined,
  FileTextOutlined,
  VerticalAlignTopOutlined,
  VerticalAlignBottomOutlined,
  ShareAltOutlined,
  HistoryOutlined,
  TeamOutlined,
  UserSwitchOutlined
} from '@ant-design/icons-vue';
import PageBreadcrumb from '../../components/common/PageBreadcrumb.vue';
import AssignRecordsModalComponent from '@/components/common/AssignRecordsModalComponent.vue';
import UserSelect from '@/components/common/UserSelect.vue';
import { queryRange } from '@/api/common';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect.vue';

export default defineComponent({
  name: 'LearningMapListPage',
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
    UserAddOutlined,
    LineChartOutlined,
    FileTextOutlined,
    VerticalAlignTopOutlined,
    VerticalAlignBottomOutlined,
    ShareAltOutlined,
    HistoryOutlined,
    VisibilitySettingComponent,
    CategorySettingComponent,
    AssignPage,
    PageBreadcrumb,
    TeamOutlined,
    AssignRecordsModalComponent,
    UserSelect,
    CategoryTreeSelect,
    UserSwitchOutlined
  },
  setup() {
    // 初始化dayjs中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const collapsed = ref(false);
    const selectedKeys = ref(['mapList']);
    const openKeys = ref(['learning']);
    const categoryModalVisible = ref(false);

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

    // 学习地图列表数据
    const learningMapList = ref([]);
    const loading = ref(false);
    const selectedRowKeys = ref([]);

    // 分类列表
    const categories = ref([]);

    // 日期范围
    const dateRange = ref([]);

    // 搜索表单
    const searchForm = reactive({
      name: '',
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
        dataIndex: 'cover',
        key: 'cover',
        fixed: 'left',
        width: 60
      },
      {
        title: '地图名称',
        dataIndex: 'name',
        key: 'name',
        ellipsis: true,
        fixed: 'left',
        width: 200
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
        width: 100
      },
      {
        title: '创建人',
        dataIndex: 'creatorName',
        key: 'creatorName',
        width: 100
      },
      {
        title: '更新时间',
        dataIndex: 'gmtCreate',
        key: 'gmtCreate',
        width: 150
      },
      {
        title: '开放时间',
        dataIndex: 'openTime',
        key: 'openTime',
        width: 200
      },
      {
        title: '阶段任务数',
        dataIndex: 'stageCount',
        key: 'stageCount',
        width: 100
      },
      {
        title: '操作',
        key: 'action',
        fixed: 'right',
        width: 250
      }
    ];

    // 当前选中的学习地图
    const currentMap = ref({});

    // 可见范围弹窗
    const visibilityModalVisible = ref(false);

    // 分类设置弹窗
    const categorySettingModalVisible = ref(false);

    // 指派弹窗
    const assignModalVisible = ref(false);

    // 指派类型选择弹窗
    const assignTypeModalVisible = ref(false);
    const isAutoAssignEnabled = ref(false);
    const assignType = ref('once');

    // 指派记录弹窗
    const assignRecordModalVisible = ref(false);

    // 日志弹窗
    const logModalVisible = ref(false);
    const logLoading = ref(false);
    const logList = ref([]);
    const logPagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: (total) => `共 ${total} 条记录`
    });
    const logColumns = [
      {
        title: '操作时间',
        dataIndex: 'time',
        width: 180,
        key: 'time'
      },
      {
        title: '操作人',
        dataIndex: 'operator',
        key: 'operator'
      },
      {
        title: '操作类型',
        dataIndex: 'operateType',
        key: 'operateType'
      },
      {
        title: '操作内容',
        dataIndex: 'message',
        key: 'message',
        width: 360,
        ellipsis: true
      }
    ];

    // 批量变更创建人弹窗
    const changeCreatorModalVisible = ref(false);
    const changeCreatorLoading = ref(false);
    const changeCreatorForm = reactive({
      newCreatorId: undefined
    });

    // 获取学习地图列表
    const fetchLearningMapList = async () => {
      loading.value = true;
      try {
        const res = await getLearningMapList(searchForm);
        if (res.code === 200 && res.data) {
          learningMapList.value = res.data.list || [];
          pagination.total = res.data.total || 0;
          pagination.current = searchForm.pageNum;
          pagination.pageSize = searchForm.pageSize;
        } else {
          message.error(res.message || '获取学习地图列表失败');
        }
      } catch (error) {
        console.error('获取学习地图列表失败:', error);
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
      fetchLearningMapList();
    };

    // 处理重置
    const handleReset = () => {
      // 重置搜索表单
      searchForm.name = '';
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
      fetchLearningMapList();
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
      fetchLearningMapList();
    };

    // 处理选择变更
    const onSelectChange = (keys) => {
      selectedRowKeys.value = keys;
    };

    // 处理删除学习地图
    const handleDelete = async (record) => {
      try {
        const res = await deleteLearningMap(record.id);
        if (res.code === 200) {
          message.success('删除成功');
          // 重新获取数据
          fetchLearningMapList();
        } else {
          message.error(res.message || '删除失败');
        }
      } catch (error) {
        console.error('删除学习地图失败:', error);
      }
    };

    // 处理批量删除
    const handleBatchDelete = async () => {
      if (selectedRowKeys.value.length === 0) {
        message.warning('请选择要删除的学习地图');
        return;
      }

      try {
        const res = await batchDeleteLearningMap(selectedRowKeys.value);
        if (res.code === 200) {
          message.success(`成功删除 ${selectedRowKeys.value.length} 个学习地图`);

          // 清空选择
          selectedRowKeys.value = [];

          // 重新获取数据
          fetchLearningMapList();
        } else {
          message.error(res.message || '批量删除失败');
        }
      } catch (error) {
        console.error('批量删除学习地图失败:', error);
      }
    };

    // 处理置顶/取消置顶
    const handleTopToggle = async (record) => {
      try {
        const res = await updateLearningMapTopStatus({
          id: record.id,
          isTop: record.isTop ? 0 : 1
        });

        if (res.code === 200) {
          message.success(record.isTop ? '取消置顶成功' : '置顶成功');
          // 重新获取数据
          fetchLearningMapList();
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
        businessType: 'LEARNING_MAP',
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
      currentMap.value = record;
      getRange(record.id);
      visibilityModalVisible.value = true;
    };

    // 处理可见范围设置成功
    const handleVisibilitySuccess = () => {
      visibilityModalVisible.value = false;
    };

    // 显示分类设置弹窗
    const showCategorySettingModal = (record) => {
      currentMap.value = record;
      categorySettingModalVisible.value = true;
    };

    // 处理分类设置成功
    const handleCategorySettingSuccess = () => {
      categorySettingModalVisible.value = false;
      // 重新获取数据
      fetchLearningMapList();
    };

    // 显示指派类型选择弹窗
    const showAssignTypeModal = (record) => {
      currentMap.value = record;
      assignTypeModalVisible.value = true;
      // 使用地图的enableAutoAssign属性设置开关状态
      isAutoAssignEnabled.value = record.enableAutoAssign || false;
    };

    // 处理单次指派
    const handleSingleAssign = () => {
      assignTypeModalVisible.value = false;
      assignType.value = 'once';
      assignModalVisible.value = true;
    };

    // 处理自动指派
    const handleAutoAssign = () => {
      if (!isAutoAssignEnabled.value) {
        message.warning('请先开启自动指派功能');
        return;
      }
      assignTypeModalVisible.value = false;
      assignType.value = 'auto';
      assignModalVisible.value = true;
    };

    // 处理自动指派开关变更
    const handleAutoAssignSwitchChange = async (checked) => {
      if (!checked) {
        // 显示二次确认对话框
        Modal.confirm({
          title: '确认关闭自动指派',
          content: '关闭后不再继续发送指派，不影响已发送的员工，关闭后一天内不可以再次开启',
          okText: '确认关闭',
          cancelText: '取消',
          onOk: async () => {
            try {
              const res = await updateLearningMapAutoAssign({
                mapId: currentMap.value.id,
                enableAutoAssign: false
              });

              if (res.code === 200) {
                message.success('已关闭自动指派');
                isAutoAssignEnabled.value = false;
                // 更新列表中的数据
                const mapIndex = learningMapList.value.findIndex(item => item.id === currentMap.value.id);
                if (mapIndex !== -1) {
                  learningMapList.value[mapIndex].enableAutoAssign = false;
                }
              } else {
                message.error(res.message || '操作失败');
                isAutoAssignEnabled.value = true;
              }
            } catch (error) {
              console.error('关闭自动指派失败:', error);
              isAutoAssignEnabled.value = true;
            }
          },
          onCancel: () => {
            isAutoAssignEnabled.value = true;
          }
        });
      } else {
        // 开启自动指派
        try {
          const res = await updateLearningMapAutoAssign({
            mapId: currentMap.value.id,
            enableAutoAssign: true
          });

          if (res.code === 200) {
            message.success('已开启自动指派');
            // 更新列表中的数据
            const mapIndex = learningMapList.value.findIndex(item => item.id === currentMap.value.id);
            if (mapIndex !== -1) {
              learningMapList.value[mapIndex].enableAutoAssign = true;
            }
          } else {
            message.error(res.message || '操作失败');
            isAutoAssignEnabled.value = false;
          }
        } catch (error) {
          console.error('开启自动指派失败:', error);
          isAutoAssignEnabled.value = false;
        }
      }
    };

    // 修改原有的showAssignModal方法
    const showAssignModal = (record) => {
      showAssignTypeModal(record);
    };

    // 处理指派成功
    const handleAssignSuccess = () => {
      assignModalVisible.value = false;
      message.success('指派成功');
    };

    // 显示指派记录弹窗
    const showAssignRecordModal = (record) => {
      currentMap.value = record;
      assignRecordModalVisible.value = true;
    };

    // 获取日志数据
    const fetchLogs = async () => {
      if (!currentMap.value.id) return;

      logLoading.value = true;
      try {
        // 这里假设有一个获取日志的接口
        const res = await getOperationLogs({
          businessType: 'LEARNING_MAP',
          businessId: currentMap.value.id,
          pageNum: logPagination.current,
          pageSize: logPagination.pageSize
        });

        if (res.code === 200 && res.data) {
          logList.value = res.data.logs || [];
          logPagination.total = res.data.total || 0;
        } else {
          message.error(res.message || '获取日志失败');
        }
      } catch (error) {
        console.error('获取日志失败:', error);
        // 模拟数据
        logList.value = [];
      } finally {
        logLoading.value = false;
      }
    };

    // 处理日志表格变更
    const handleLogTableChange = (pag) => {
      logPagination.current = pag.current;
      logPagination.pageSize = pag.pageSize;
      fetchLogs();
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

    // 处理分享
    const handleShare = (record) => {
      // 生成分享链接
      const shareUrl = `${window.location.origin}/map/detail/${record.id}?share=true`;

      // 复制到剪贴板
      const textarea = document.createElement('textarea');
      textarea.value = shareUrl;
      document.body.appendChild(textarea);
      textarea.select();
      document.execCommand('copy');
      document.body.removeChild(textarea);

      message.success('分享链接已复制到剪贴板');
    };

    // 显示日志弹窗
    const showLogModal = (record) => {
      currentMap.value = record;
      logModalVisible.value = true;
      // 获取日志数据
      fetchLogs();
    };

    // 显示批量变更创建人弹窗
    const showChangeCreatorModal = () => {
      if (selectedRowKeys.value.length === 0) {
        message.warning('请选择要操作的学习地图');
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
          mapIds: selectedRowKeys.value,
          newCreatorId: changeCreatorForm.newCreatorId
        });

        if (res.code === 200) {
          message.success('批量变更创建人成功');
          changeCreatorModalVisible.value = false;

          // 清空选择
          selectedRowKeys.value = [];

          // 重新获取数据
          fetchLearningMapList();
        } else {
          message.error(res.message || '批量变更创建人失败');
        }
      } catch (error) {
        console.error('批量变更创建人失败:', error);
      } finally {
        changeCreatorLoading.value = false;
      }
    };

    onMounted(() => {
      // 获取分类列表
      fetchCategories();

      // 获取学习地图列表
      fetchLearningMapList();
    });

    return {
      collapsed,
      selectedKeys,
      openKeys,
      userInfo,
      learningMapList,
      loading,
      selectedRowKeys,
      categories,
      dateRange,
      searchForm,
      pagination,
      columns,
      currentMap,
      visibilityModalVisible,
      categorySettingModalVisible,
      assignModalVisible,
      assignTypeModalVisible,
      isAutoAssignEnabled,
      assignType,
      logModalVisible,
      logLoading,
      logList,
      logPagination,
      logColumns,
      categoryModalVisible,
      assignRecordModalVisible,
      selectedRange,

      hasPermission,
      handleDateRangeChange,
      handleSearch,
      handleReset,
      handleTableChange,
      onSelectChange,
      handleDelete,
      handleBatchDelete,
      handleTopToggle,
      showVisibilityModal,
      handleVisibilitySuccess,
      showCategorySettingModal,
      handleCategorySettingSuccess,
      showAssignModal,
      handleAssignSuccess,
      showAssignRecordModal,
      handleShare,
      showLogModal,
      handleLogTableChange,
      handleLogout,
      navigateTo,
      showCategoryModal,
      handleSingleAssign,
      handleAutoAssign,
      handleAutoAssignSwitchChange,
      showChangeCreatorModal,
      changeCreatorModalVisible,
      changeCreatorLoading,
      changeCreatorForm,
      handleChangeCreator
    };
  }
});
</script>

<style scoped>
.learning-map-list-layout {
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

.assign-record-container,
.log-container {
  padding: 16px;
}

.assign-type-container {
  padding: 16px;
}

.assign-type-item {
  display: flex;
  align-items: center;
  padding: 16px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

.assign-type-item:hover {
  border-color: #1890ff;
  background-color: #f0f7ff;
}

.assign-type-item.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.assign-type-icon {
  font-size: 24px;
  margin-right: 16px;
  color: #1890ff;
}

.assign-type-content {
  flex: 1;
}

.assign-type-title {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}

.assign-type-desc {
  color: #666;
  font-size: 14px;
}

.auto-assign-switch {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.auto-assign-switch span {
  margin-right: 8px;
}
</style>
