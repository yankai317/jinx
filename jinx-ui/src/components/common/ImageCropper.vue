<template>
  <div class="image-cropper-container">
    <a-upload
      v-model:file-list="fileList"
      :show-upload-list="false"
      :before-upload="beforeUpload"
      @change="handleImageChange"
    >
      <a-button v-if="!imageUrl && !value">
        <upload-outlined />
        {{ label || '选择图片' }}
      </a-button>
    </a-upload>

    <a-modal
      v-model:visible="cropperVisible"
      :title="modalTitle || '裁剪图片'"
      width="800px"
      :footer="null"
      :maskClosable="false"
      :destroyOnClose="true"
    >
      <div class="cropper-container">
        <div class="cropper-wrapper">
          <div class="cropper-dimension-tip">裁剪尺寸: {{ width }} × {{ height }}</div>
          <img ref="cropperImage" :src="imageUrl" alt="待裁剪图片" style="display: none;" />
        </div>
        <div class="cropper-instructions">
          <ul>
            <li><zoom-in-outlined /> 滚轮向上或按 <kbd>+</kbd> 放大图片</li>
            <li><zoom-out-outlined /> 滚轮向下或按 <kbd>-</kbd> 缩小图片</li>
            <li><drag-outlined /> 按住并拖动图片调整位置</li>
          </ul>
        </div>
        <div class="extra-info" v-if="extra">
          <slot name="extra">{{ extra }}</slot>
        </div>
        <div class="cropper-toolbar">
          <a-button @click="cancelCrop">取消</a-button>
          <a-button type="primary" @click="cropImage">确认裁剪</a-button>
        </div>
      </div>
    </a-modal>

    <div class="image-preview" v-if="value">
      <img :src="value" alt="图片预览" />
      <div class="preview-actions">
        <a-button type="primary" size="small" @click="showCropper">
          <scissor-outlined />
          重新裁剪
        </a-button>
        <a-button type="danger" size="small" @click="removeImage">
          <delete-outlined />
          删除
        </a-button>
      </div>
    </div>

    <div class="upload-tip" v-if="tip">{{ tip }}</div>
  </div>
</template>

<script>
import { ref, defineComponent, watch, nextTick, onBeforeUnmount } from 'vue';
import { message } from 'ant-design-vue';
import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.css';
import {
  UploadOutlined,
  ScissorOutlined,
  DeleteOutlined,
  ZoomInOutlined,
  ZoomOutlined,
  DragOutlined
} from '@ant-design/icons-vue';

export default defineComponent({
  name: 'ImageCropper',
  components: {
    UploadOutlined,
    ScissorOutlined,
    DeleteOutlined,
    ZoomInOutlined,
    ZoomOutlined,
    DragOutlined
  },
  props: {
    value: {
      type: String,
      default: ''
    },
    width: {
      type: Number,
      default: 640
    },
    height: {
      type: Number,
      default: 360
    },
    aspectRatio: {
      type: Number,
      default: null
    },
    label: {
      type: String,
      default: '选择图片'
    },
    tip: {
      type: String,
      default: ''
    },
    modalTitle: {
      type: String,
      default: '裁剪图片'
    },
    extra: {
      type: String,
      default: ''
    },
    uploadApi: {
      type: String,
      default: '/api/file/upload'
    },
    maxSize: {
      type: Number,
      default: 10 // 单位：MB
    }
  },
  emits: ['update:value', 'crop-success', 'crop-cancel', 'crop-error'],
  setup(props, { emit }) {
    // 文件列表和图片URL
    const fileList = ref([]);
    const imageUrl = ref('');
    const cropperVisible = ref(false);
    const cropperImage = ref(null);
    const cropper = ref(null);

    // 监听value变化，更新预览图
    watch(() => props.value, (newVal) => {
      if (newVal) {
        fileList.value = [{
          uid: '-1',
          name: '已上传图片',
          status: 'done',
          url: newVal
        }];
      } else {
        fileList.value = [];
      }
    }, { immediate: true });

    // 选择图片处理
    const handleImageChange = (info) => {
      if (info.file.status === 'done') {
        if (info.file.response && info.file.response.code === 200) {
          // 上传成功，保存原始图片URL并显示裁剪框
          imageUrl.value = info.file.response.data.url;
          showCropper();
        } else {
          message.error(`${info.file.name} 上传失败: ${info.file.response?.message || '未知错误'}`);
        }
      } else if (info.file.status === 'error') {
        message.error(`${info.file.name} 上传失败`);
      }
    };

    // 显示裁剪框
    const showCropper = () => {
      if (!imageUrl.value && !props.value) {
        return;
      }

      // 如果是重新裁剪，使用当前值作为裁剪源
      if (!imageUrl.value && props.value) {
        imageUrl.value = props.value;
      }

      cropperVisible.value = true;

      // 等待DOM更新后初始化裁剪组件
      nextTick(() => {
        if (cropper.value) {
          cropper.value.destroy();
        }

        const aspectRatio = props.aspectRatio || props.width / props.height;

        cropper.value = new Cropper(cropperImage.value, {
          aspectRatio: aspectRatio,
          viewMode: 1,
          dragMode: 'move',
          cropBoxResizable: false,
          cropBoxMovable: true,
          autoCropArea: 1,
          minContainerWidth: 600,
          minContainerHeight: 400,
          background: true,
          movable: true,
          zoomable: true,
          guides: true,
          ready() {
            // 裁剪框准备好后，设置固定尺寸
            const containerData = cropper.value.getContainerData();

            // 计算裁剪框大小，保持指定宽高比
            const width = Math.min(containerData.width * 0.8, props.width);
            const height = width * (props.height / props.width);

            // 设置裁剪框位置和大小
            cropper.value.setCropBoxData({
              left: (containerData.width - width) / 2,
              top: (containerData.height - height) / 2,
              width: width,
              height: height
            });
          }
        });
      });
    };

    // 取消裁剪
    const cancelCrop = () => {
      cropperVisible.value = false;
      if (cropper.value) {
        cropper.value.destroy();
        cropper.value = null;
      }

      // 如果是第一次上传，需要清空选择的文件
      if (!props.value) {
        fileList.value = [];
        imageUrl.value = '';
      }

      emit('crop-cancel');
    };

    // 确认裁剪
    const cropImage = () => {
      if (!cropper.value) return;

      // 显示加载中状态
      const loadingKey = 'cropLoading';
      message.loading({ content: '正在处理图片...', key: loadingKey, duration: 0 });

      // 获取裁剪后的Canvas
      const canvas = cropper.value.getCroppedCanvas({
        width: props.width,
        height: props.height,
        imageSmoothingQuality: 'high'
      });

      if (!canvas) {
        message.error({ content: '裁剪失败，请重试', key: loadingKey });
        emit('crop-error', '裁剪失败');
        return;
      }

      // 将canvas转换为blob
      canvas.toBlob((blob) => {
        // 创建FormData对象
        const formData = new FormData();
        formData.append('file', blob, 'cropped-image.png');

        // 显示上传中
        message.loading({ content: '正在上传裁剪后的图片...', key: loadingKey, duration: 0 });

        // 调用上传接口
        fetch(props.uploadApi, {
          method: 'POST',
          body: formData,
          headers: {
            'Authorization': sessionStorage.getItem('token')
          }
        })
          .then(response => response.json())
          .then(res => {
            if (res.code === 200 && res.data) {
              // 上传成功
              const imageUrl = res.data.url;

              // 更新组件值
              emit('update:value', imageUrl);

              // 更新文件列表
              fileList.value = [{
                uid: '-1',
                name: '已裁剪图片',
                status: 'done',
                url: imageUrl
              }];

              message.success({ content: '图片裁剪成功', key: loadingKey });
              emit('crop-success', imageUrl, res.data);
            } else {
              // 上传失败
              message.error({ content: res.message || '裁剪图片上传失败', key: loadingKey });
              emit('crop-error', res.message || '裁剪图片上传失败');
            }
          })
          .catch(error => {
            console.error('裁剪图片上传失败:', error);
            emit('crop-error', error);
          })
          .finally(() => {
            // 关闭裁剪框
            cropperVisible.value = false;
            if (cropper.value) {
              cropper.value.destroy();
              cropper.value = null;
            }
          });
      }, 'image/png');
    };

    // 删除图片
    const removeImage = () => {
      emit('update:value', '');
      fileList.value = [];
      imageUrl.value = '';
    };

    // 图片上传前校验
    const beforeUpload = (file) => {
      const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png';
      if (!isJpgOrPng) {
        message.error('只能上传JPG或PNG格式的图片!');
        return false;
      }

      const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize;
      if (!isLtMaxSize) {
        message.error(`图片大小不能超过${props.maxSize}MB!`);
        return false;
      }

      // 使用自定义上传
      const formData = new FormData();
      formData.append('file', file);

      // 调用上传接口
      fetch(props.uploadApi, {
        method: 'POST',
        body: formData,
        headers: {
          'Authorization': sessionStorage.getItem('token')
        }
      })
        .then(response => response.json())
        .then(res => {
          if (res.code === 200 && res.data) {
            // 获取上传后的临时URL，用于裁剪
            imageUrl.value = res.data.url;
            // 显示裁剪框
            showCropper();
          } else {
            message.error(res.message || '上传失败');
            emit('crop-error', res.message || '上传失败');
          }
        })
        .catch(error => {
          console.error('上传失败:', error);
          emit('crop-error', error);
        });

      return false; // 阻止默认上传行为
    };

    // 组件销毁前清理
    onBeforeUnmount(() => {
      if (cropper.value) {
        cropper.value.destroy();
        cropper.value = null;
      }
    });

    return {
      fileList,
      imageUrl,
      cropperVisible,
      cropperImage,
      showCropper,
      cancelCrop,
      cropImage,
      removeImage,
      handleImageChange,
      beforeUpload
    };
  }
});
</script>

<style scoped>
.image-cropper-container {
  position: relative;
}

.image-preview {
  position: relative;
  margin-top: 8px;
  max-width: 640px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  overflow: hidden;
}

.image-preview img {
  width: 100%;
  height: auto;
  display: block;
}

.preview-actions {
  position: absolute;
  top: 0;
  right: 0;
  padding: 8px;
  display: flex;
  gap: 8px;
  background-color: rgba(0, 0, 0, 0.4);
  border-radius: 0 0 0 4px;
}

.preview-actions button {
  margin-left: 8px;
}

.cropper-container {
  display: flex;
  flex-direction: column;
  position: relative;
}

.cropper-wrapper {
  width: 100%;
  height: 350px;
  position: relative;
  background-color: #000;
  overflow: hidden;
}

.cropper-instructions {
  padding: 12px;
  background-color: #fafafa;
  border-bottom: 1px solid #e8e8e8;
}

.cropper-instructions ul {
  list-style-type: none;
  padding-left: 0;
  margin: 0;
  display: flex;
  justify-content: space-around;
}

.cropper-instructions li {
  margin-bottom: 0;
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
}

.cropper-instructions .anticon {
  margin-right: 4px;
  color: #1890ff;
}

.cropper-instructions kbd {
  background-color: #f0f2f5;
  border: 1px solid #d9d9d9;
  border-radius: 3px;
  box-shadow: 0 1px 1px rgba(0,0,0,.2);
  padding: 1px 4px;
  margin: 0 2px;
  font-size: 11px;
}

.cropper-toolbar {
  display: flex;
  justify-content: flex-end;
  padding: 12px;
  background-color: #f0f2f5;
  border-top: 1px solid #e8e8e8;
}

.cropper-toolbar button {
  margin-left: 8px;
}

.cropper-dimension-tip {
  position: absolute;
  top: 10px;
  left: 10px;
  background-color: rgba(0, 0, 0, 0.5);
  color: #fff;
  padding: 4px 8px;
  border-radius: 2px;
  font-size: 12px;
  z-index: 100;
}

.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 8px;
}

.extra-info {
  padding: 12px;
  background-color: #f6f6f6;
  font-size: 12px;
  color: #666;
  border-bottom: 1px solid #e8e8e8;
}
</style>