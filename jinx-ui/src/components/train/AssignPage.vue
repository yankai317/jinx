<template>
  <div class="assign-page">
    <a-layout>
      <!-- 顶部信息区 -->
      <!-- <a-layout-header class="header">
        <a-page-header :title="trainDetail ? trainDetail.name : '培训指派'"
          :sub-title="trainDetail ? `创建人: ${trainDetail.creatorName}` : ''">
          <template #extra>
            <a-tag v-if="trainDetail && trainDetail.status" color="blue">{{ getStatusText(trainDetail.status) }}</a-tag>
          </template>
        </a-page-header>
      </a-layout-header> -->

      <!-- 内容区 -->
      <a-layout-content class="content">
        <a-spin :spinning="loading">
          <!-- 指派设置表单 -->
          <a-form ref="assignFormRef" :model="formState" :rules="rules" layout="vertical">
            <h2 style="margin-bottom: 16px;">
              {{ assignType === 'once' ? '单次指派' : '自动指派' }}
            </h2>
            <!-- 完成时间设置 -->
            <a-form-item label="完成时间设置" name="assignFinishedTimeType">
              <a-radio-group v-model:value="formState.assignFinishedTimeType" @change="handleDeadlineTypeChange">
                <a-radio value="NONE">不设置</a-radio>
                <a-radio value="ONE_WEEK">1周</a-radio>
                <a-radio value="TWO_WEEK">2周</a-radio>
                <a-radio value="FOUR_WEEK">4周</a-radio>
                <a-radio value="CUSTOM">自定义</a-radio>
              </a-radio-group>

              <div v-if="formState.assignFinishedTimeType === 'CUSTOM'" style="margin-top: 16px;">
                <a-input-number v-model:value="formState.customFinishedDay" :min="1" :max="365" addon-after="天"
                  style="width: 200px;" />
              </div>
            </a-form-item>
            <!-- 学习提醒设置 -->
            <!-- <a-form-item label="学习提醒">
              <a-select v-model:value="formState.reminderType" style="width: 200px;">
                <a-select-option value="none">不提醒</a-select-option>
                <a-select-option value="daily">每天提醒</a-select-option>
                <a-select-option value="weekly">每周提醒</a-select-option>
              </a-select>

              <div v-if="formState.reminderType === 'none'" style="margin-top: 8px;">
                <a-alert type="warning" message="即使不设学习提醒，学员也将立即收到一次指派提醒" show-icon />
              </div>

              <div v-if="formState.reminderType === 'daily'" style="margin-top: 16px;">
                <span>在每天的</span>
                <a-time-picker v-model:value="formState.dailyReminderTime" format="HH:mm" style="margin-left: 8px;" />
              </div>

              <div v-if="formState.reminderType === 'weekly'" style="margin-top: 16px;">
                <a-select v-model:value="formState.weeklyReminderDay" style="width: 120px; margin-right: 8px;">
                  <a-select-option value="1">每周一</a-select-option>
                  <a-select-option value="2">每周二</a-select-option>
                  <a-select-option value="3">每周三</a-select-option>
                  <a-select-option value="4">每周四</a-select-option>
                  <a-select-option value="5">每周五</a-select-option>
                  <a-select-option value="6">每周六</a-select-option>
                  <a-select-option value="7">每周日</a-select-option>
                </a-select>
                <a-time-picker v-model:value="formState.weeklyReminderTime" format="HH:mm" />
              </div>
            </a-form-item> -->

            <!-- 指派对象设置 -->
            <a-form-item label="指派对象" name="assignTargets" required>
              <a-tabs v-model:activeKey="activeTabKey">
                <!-- 按组织架构选择 -->
                <a-tab-pane key="org" tab="按组织架构选择">
                  <a-button type="primary" @click="showOrgSelector">
                    <template #icon>
                      <UserOutlined />
                    </template>
                    选择部门/角色/人员
                  </a-button>

                  <div class="selected-items" v-if="selectedItems.length > 0">
                    <a-divider orientation="left">已选择</a-divider>
                    <a-tag v-for="item in selectedItems" :key="`${item.type}-${item.id}`" closable
                      @close="handleRemoveItem(item)">
                      {{ item.name }} ({{ getTypeName(item.type) }})
                    </a-tag>
                  </div>
                </a-tab-pane>

                <template v-if="assignType !== 'auto'">
                  <!-- 批量导入用户 -->
                  <a-tab-pane key="import" tab="批量导入用户">
                    <a-form-item label="导入类型">
                      <a-radio-group v-model:value="formState.importType">
                        <a-radio value="workNo">工号</a-radio>
                        <a-radio value="dingId">钉钉UserId</a-radio>
                      </a-radio-group>
                    </a-form-item>
                    <a-form-item >
                      <a-textarea v-model:value.trim="formState.ids" placeholder="请输入用户工号/UserID，使用换行符或逗号隔开" :rows="6"
                        @change="handleUserIdsChange" />
                    </a-form-item>
                  </a-tab-pane>
                </template>
              </a-tabs>
            </a-form-item>
                  <div class="selected-items" v-if="notifiedItems.length > 0">
                    <a-divider orientation="left">已通知</a-divider>
                    <a-tag v-for="item in notifiedItems" :key="`${item.type}-${item.id}`">
                      {{ item.name }} ({{ getTypeName(item.type) }})
                    </a-tag>
                  </div>

            <!-- 显示已指派的用户 -->
            <!-- <a-form-item label="已指派用户"
              v-if="assignmentDetail && assignmentDetail.assignUsers && assignmentDetail.assignUsers.length > 0">
              <div class="assigned-users">
                <a-list item-layout="horizontal" :data-source="assignmentDetail.assignUsers">
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <a-list-item-meta>
                        <template #avatar>
                          <a-avatar :src="item.avatar" v-if="item.avatar">
                            {{ !item.avatar && item.userName ? item.userName.substring(0, 1) : '' }}
                          </a-avatar>
                        </template>
                        <template #title>{{ item.userName }}</template>
                        <template #description>用户ID: {{ item.userId }}</template>
                      </a-list-item-meta>
                    </a-list-item>
                  </template>
                </a-list>
              </div>
            </a-form-item> -->

            <a-form-item label="指派规则" v-if="assignType === 'auto'" required>

              <div style="margin-top: 16px;">
                <!-- <a-checkbox v-model:checked="formState.filterByJoinDate"> -->
                指派给入职时间在
                <a-date-picker v-model:value="formState.notifyUserAfterJoinDate" :disabled-date="disabledDate"
                  style="margin: 0 8px;" :allowClear="false" />
                之后的员工
                <a-tooltip title="请在钉钉通讯录中维护员工的入职时间字段">
                  <InfoCircleOutlined style="margin-left: 4px;" />
                </a-tooltip>
                <!-- </a-checkbox> -->
              </div>
              <div style="margin-top: 16px;">
                <a-checkbox v-model:checked="formState.ifIsNotifyExistUser">
                  同时指派给已在所选的部门/角色/职位/用户组内目前在职的员工
                </a-checkbox>
              </div>

            </a-form-item>
          </a-form>
        </a-spin>
      </a-layout-content>

      <!-- 底部按钮区 -->
      <a-layout-footer class="footer">
        <a-space>
          <a-button @click="handleCancel">取消</a-button>
          <a-button type="primary" :loading="submitting" @click="handleSubmit"
            :disabled="!hasPermission('train:assign')">
            提交
          </a-button>
        </a-space>
      </a-layout-footer>
    </a-layout>

    <!-- 组织架构选择器弹窗 -->
    <a-modal v-model:visible="orgSelectorVisible" title="选择指派对象" width="800px" :footer="null" :destroyOnClose="true">
      <OrgUserComponent :selectionType="'multiple'" :onConfirm="handleOrgSelectorConfirm"
        :onCancel="handleOrgSelectorCancel" :selectedIds="assignType === 'auto' ? formattedSelectedItems : []"
        :bizId="bizId" :bizType="bizType" />
    </a-modal>

    <!-- 指派结果弹窗 -->
    <a-modal v-model:visible="resultVisible" title="指派结果" :footer="null" @cancel="handleResultClose">
      <template #footer>
        <a-button key="close" @click="handleResultClose">关闭</a-button>
        <a-button key="track" type="primary" @click="goToTrackingPage">查看培训跟踪</a-button>
      </template>
      <div class="assign-result">
        <a-result :status="assignResult.failed > 0 ? 'warning' : 'success'" :title="getResultTitle()">
          <template #extra>
            <div v-if="assignResult.failed > 0" class="failed-users">
              <a-alert type="error" message="以下用户指派失败：" />
              <div class="failed-list">
                <p v-for="(userId, index) in assignResult.failedUsers" :key="index">
                  用户ID: {{ userId }}
                </p>
              </div>
            </div>
          </template>
        </a-result>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, computed, onMounted, watch } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import dayjs from 'dayjs';
import { UserOutlined, InfoCircleOutlined } from '@ant-design/icons-vue';
import OrgUserComponent from '../common/OrgUserComponent.vue';
import { assignTrain, getAssignmentDetail } from '@/api/train';
import { hasPermission } from '@/utils/permission';

export default defineComponent({
  name: 'AssignPage',
  components: {
    OrgUserComponent,
    UserOutlined,
    InfoCircleOutlined
  },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    bizId: {
      type: Number,
      required: true
    },
    bizType: {
      type: String,
      default: 'TRAIN',
    },
    // once-单次通知，auto-自动通知
    assignType: {
      type: String,
      default: 'once'
    },
  },
  emits: ['success'],
  setup(props, { emit }) {
    // 初始化dayjs语言为中文
    dayjs.locale('zh-cn');

    const router = useRouter();
    const assignFormRef = ref(null);

    // 状态变量
    const loading = ref(false);
    const submitting = ref(false);
    const trainDetail = ref(null);
    const activeTabKey = ref('org');
    const orgSelectorVisible = ref(false);
    // 已选择
    const selectedItems = ref([]);
    // 已通知
    const notifiedItems = ref([]);
    const resultVisible = ref(false);
    const assignResult = ref({
      success: 0,
      failed: 0,
      failedUsers: []
    });
    const assignmentDetail = ref(null);

    // 表单状态
    const formState = reactive({
      assignFinishedTimeType: 'NONE',
      customDays: null,
      userIds: '',
      reminderType: 'none',
      dailyReminderTime: null,
      weeklyReminderDay: null,
      weeklyReminderTime: null,
      assignToExistingMembers: false,
      filterByJoinDate: false,
      joinDate: dayjs(),
      notifyUserAfterJoinDate: dayjs(),
      ifIsNotifyExistUser: false,
      customFinishedDay: null,
      importType: 'workNo',
      ids: ''
    });

    // 表单校验规则
    const rules = {
      assignTargets: [
        { required: true, validator: validateAssignTargets, trigger: 'change' }
      ]
    };

    // 计算属性：解析用户ID
    const parsedUserIds = computed(() => {
      if (!formState.userIds) return [];
      return formState.userIds
        .split('\n')
        .map(id => id.trim())
        .filter(id => id && /^\d+$/.test(id))
        .map(id => parseInt(id));
    });

    // 计算属性：格式化已选项用于传递给OrgUserComponent
    const formattedSelectedItems = computed(() => {
      if (!assignmentDetail.value) return [];

      const items = [];

      // 添加部门
      if (assignmentDetail.value.departments && assignmentDetail.value.departments.length > 0) {
        assignmentDetail.value.departments.forEach(dept => {
          items.push({
            id: dept.departmentId,
            name: dept.departmentName,
            type: 'department'
          });
        });
      }

      // 添加角色
      if (assignmentDetail.value.roles && assignmentDetail.value.roles.length > 0) {
        assignmentDetail.value.roles.forEach(role => {
          items.push({
            id: role.roleId,
            name: role.roleName,
            type: 'role'
          });
        });
      }

      // 添加用户
      if (assignmentDetail.value.assignUsers && assignmentDetail.value.assignUsers.length > 0) {
        assignmentDetail.value.assignUsers.forEach(user => {
          items.push({
            id: user.userId,
            name: user.userName,
            type: 'user'
          });
        });
      }

      return items;
    });

    // 校验指派对象
    function validateAssignTargets(rule, value) {
      if (activeTabKey.value === 'org' && selectedItems.value.length === 0) {
        return Promise.reject('请选择至少一个指派对象');
      }

      if (activeTabKey.value === 'import' && !formState.ids) {
        return Promise.reject('请输入至少一个有效的用户ID');
      }

      return Promise.resolve();
    }

    // 禁用过去的日期
    function disabledDate(current) {
      return current && current < dayjs().startOf('day');
    }

    // 获取指派详情
    async function fetchAssignmentDetail() {
      loading.value = true;
      try {
        // 调用指派详情接口
        const params = {
          bizId: props.bizId,
          bizType: props.bizType,
          assignType: props.assignType
        };

        const res = await getAssignmentDetail(params);

        if (res.code === 200 && res.data) {
          assignmentDetail.value = res.data;

          // 如果是自动指派模式，且有已存在的数据，将已选择的部门、角色和用户添加到selectedItems中
          if (props.assignType === 'auto') {
            // 使用计算属性来设置selectedItems
            selectedItems.value = formattedSelectedItems.value;
          }
          // 如果是单次指派模式，将已通知的部门、角色和用户添加到notifiedItems中
          if (props.assignType === 'once') {
            notifiedItems.value = formattedSelectedItems.value;
          }
        } else {
          message.error(res.message || '获取指派详情失败');
        }
      } catch (error) {
        console.error('获取指派详情失败:', error);
      } finally {
        loading.value = false;
      }
    }

    // 处理截止时间类型变更
    function handleDeadlineTypeChange(e) {
      const type = e.target.value;
      if (type !== 'custom') {
        formState.customDays = null;
      }
    }

    // 处理用户ID变更
    function handleUserIdsChange() {
      // 触发表单验证
      if (assignFormRef.value) {
        assignFormRef.value.validateFields(['assignTargets']);
      }
    }

    // 显示组织架构选择器
    function showOrgSelector() {
      orgSelectorVisible.value = true;
    }

    // 处理组织架构选择器确认
    function handleOrgSelectorConfirm(result) {
      console.log('Selected items from OrgUserComponent:', result);
      selectedItems.value = result.items;
      orgSelectorVisible.value = false;

      // 触发表单验证
      if (assignFormRef.value) {
        assignFormRef.value.validateFields(['assignTargets']);
      }
    }

    // 处理组织架构选择器取消
    function handleOrgSelectorCancel() {
      orgSelectorVisible.value = false;
    }

    // 移除已选择项
    function handleRemoveItem(item) {
      selectedItems.value = selectedItems.value.filter(i => !(i.id === item.id && i.type === item.type));

      // 触发表单验证
      if (assignFormRef.value) {
        assignFormRef.value.validateFields(['assignTargets']);
      }
    }

    // 获取类型名称
    function getTypeName(type) {
      const typeMap = {
        'department': '部门',
        'role': '角色',
        'user': '人员'
      };
      return typeMap[type] || type;
    }

    // 获取状态文本
    function getStatusText(status) {
      const statusMap = {
        'draft': '草稿',
        'published': '已发布',
        'unpublished': '未发布',
        'archived': '已归档'
      };
      return statusMap[status] || status;
    }

    /**
     * 根据type类型将数组分成三个子数组
     * @param {Array} data - 要分类的原始数组
     * @returns {Object} 包含三个分类数组id的对象
     */
    function categorizeByType(data) {
      // 初始化三个空数组
      const result = {
        departmentIds: [],
        userIds: [],
        roleIds: []
      };

      // 遍历数组并根据type分类
      data.forEach(item => {
        switch (item.type) {
          case 'department':
            result.departmentIds.push(item.id);
            break;
          case 'user':
            result.userIds.push(item.id);
            break;
          case 'role':
            result.roleIds.push(item.id);
            break;
          // 可以添加default处理不符合类型的元素
        }
      });

      return result;
    }

    // 提交表单
    async function handleSubmit() {
      try {
        // 表单验证
        await assignFormRef.value.validate();

        console.log(selectedItems.value, activeTabKey.value, categorizeByType(selectedItems.value));
        let departmentIds = [];
        let userIds = [];
        let roleIds = [];
        if (activeTabKey.value === 'org') {
          const splitData = categorizeByType(selectedItems.value);
          departmentIds = splitData.departmentIds;
          userIds = splitData.userIds;
          roleIds = splitData.roleIds;
        } else {
          userIds = parsedUserIds.value
        }



        // 构建请求参数
        const params = {
          bizId: props.bizId,
          bizType: props.bizType,
          assignRecordId: assignmentDetail.value.autoAssignRecordId,
          reminderType: formState.reminderType,
          dailyReminderTime: formState.dailyReminderTime,
          weeklyReminderDay: formState.weeklyReminderDay,
          weeklyReminderTime: formState.weeklyReminderTime,
          assignType: props.assignType,
          assignFinishedTimeType: formState.assignFinishedTimeType,
          customFinishedDay: formState.customFinishedDay,
          notifyUserAfterJoinDate: dayjs(formState.notifyUserAfterJoinDate).format('YYYY-MM-DD 00:00:00'),
          ifIsNotifyExistUser: formState.ifIsNotifyExistUser
        };
        // 组织架构指派
        if (activeTabKey.value === 'org') {
          if (!departmentIds.length && !userIds.length && !roleIds.length) {
            message.error('请选择指派对象');
            return;
          }
          params.departmentIds = departmentIds;
          params.userIds = userIds;
          params.roleIds = roleIds;
        }
        // 导入指派
        if(activeTabKey.value === 'import') {
          params.thirtyType = 'dingtalk';
          if(formState.importType === 'workNo') {
            // 使用；或;或，或,或\n或\r\n或\r分割
            params.workNos = formState.ids.split(/[；;，,\n\r]/).map(id => id.trim());
          } else {
            params.thirtyUserIds = formState.ids.split(/[；;，,\n\r]/).map(id => id.trim());
          }
        }
        // 提交请求
        submitting.value = true;
        const res = await assignTrain(params);
        submitting.value = false;

        if (res.code === 200) {
          // 保存指派结果
          assignResult.value = res.data;
          // 显示结果弹窗
          resultVisible.value = true;
        } else {
          message.error(res.message || '指派失败');
        }
      } catch (error) {
        console.error('表单验证或提交失败:', error);
        submitting.value = false;
      }
    }

    // 取消
    function handleCancel() {
      Modal.confirm({
        title: '确认取消',
        content: '取消后已填写的内容将丢失，确认取消吗？',
        okText: '确认',
        cancelText: '取消',
        onOk: () => {
          emit('cancel');
        }
      });
    }

    // 获取结果标题
    function getResultTitle() {
      if (assignResult.code === "200") {
        return '指派失败';
      } else {
        return '指派成功';
      }
    }

    // 关闭结果弹窗
    function handleResultClose() {
      resultVisible.value = false;

      console.log('assignResult.value', assignResult.value);
      emit('success', assignResult.value);
    }

    // 跳转到跟踪页面
    function goToTrackingPage() {
      resultVisible.value = false;
      const path = props.bizType === 'train' ? '/train/tracking/' : '/map/tracking/';
      router.push(`${path}${props.trainId}`);
    }

    // 监听visible属性变化
    watch(() => props.visible, (newVal) => {
      console.log('newVal', newVal);
      if (newVal) {
        // 当弹窗显示时，获取指派详情
        fetchAssignmentDetail();
      }
    });

    onMounted(() => {
      if (props.visible) {
        fetchAssignmentDetail();
      }
    });

    return {
      loading,
      submitting,
      trainDetail,
      formState,
      rules,
      assignFormRef,
      activeTabKey,
      selectedItems,
      notifiedItems,
      orgSelectorVisible,
      parsedUserIds,
      resultVisible,
      assignResult,
      assignmentDetail,
      formattedSelectedItems,
      disabledDate,
      handleDeadlineTypeChange,
      handleUserIdsChange,
      showOrgSelector,
      handleOrgSelectorConfirm,
      handleOrgSelectorCancel,
      handleRemoveItem,
      getTypeName,
      getStatusText,
      handleSubmit,
      handleCancel,
      getResultTitle,
      handleResultClose,
      goToTrackingPage,
      hasPermission
    };
  }
});
</script>

<style scoped>
.assign-page {
  height: 100%;
  background-color: #f0f2f5;
}

.header {
  background: #fff;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
  height: auto;
  line-height: normal;
}

.content {
  padding: 24px;
  background: #fff;
  margin: 16px;
  border-radius: 4px;
  min-height: 500px;
  height: calc(100vh - 420px);
}

.footer {
  text-align: center;
  background: #fff;
  padding: 16px;
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.05);
}

.selected-items {
  margin-top: 16px;
}

.import-tip {
  color: rgba(0, 0, 0, 0.45);
  margin-top: 8px;
}

.assign-result {
  padding: 24px 0;
}

.failed-users {
  margin-top: 16px;
  width: 100%;
}

.failed-list {
  max-height: 200px;
  overflow-y: auto;
  margin-top: 8px;
  padding: 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.assigned-users {
  margin-top: 8px;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  padding: 8px;
}
</style>
