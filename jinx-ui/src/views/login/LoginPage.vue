<template>
  <div class="login-container">
    <!-- 左侧品牌区域 -->
    <div class="brand-area">
      <div class="brand-content">
        <div class="logo">
          <img src="@/assets/logo.png" alt="系统Logo" />
        </div>
        <div class="slogan">
          <h1>企业学习管理系统</h1>
          <p>赋能员工成长，助力企业发展</p>
        </div>
      </div>
    </div>

    <!-- 右侧登录表单区域 -->
    <div class="login-form-area">
      <div class="login-form-container">
        <h2 class="login-title">用户登录</h2>

        <!-- 账号密码登录表单 -->
        <div v-if="loginType === 'password'" class="login-form">
          <a-form
            :model="passwordForm"
            @finish="handlePasswordLogin"
            layout="vertical"
          >
            <a-form-item
              name="username"
              :rules="[{ required: true, message: '请输入用户名/工号' }]"
            >
              <a-input
                v-model:value="passwordForm.username"
                placeholder="请输入用户名/工号"
                size="large"
              >
                <template #prefix>
                  <user-outlined />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item
              name="password"
              :rules="[{ required: true, message: '请输入密码' }]"
            >
              <a-input-password
                v-model:value="passwordForm.password"
                placeholder="请输入密码"
                size="large"
              >
                <template #prefix>
                  <lock-outlined />
                </template>
              </a-input-password>
            </a-form-item>

            <div class="login-options">
              <a-checkbox v-model:checked="rememberMe">记住我</a-checkbox>
            </div>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                :loading="loading"
                class="login-button"
                size="large"
              >
                登录
              </a-button>
            </a-form-item>
          </a-form>

          <div class="login-switch">
            <a @click="switchLoginType('dingtalk')">使用钉钉扫码登录</a>
          </div>
        </div>

        <!-- 钉钉扫码登录 -->
        <div v-else class="dingtalk-login">
          <div class="qrcode-container">
            <div id="dingtalk-qrcode" class="qrcode"></div>
            <p class="qrcode-tip">请使用钉钉APP扫码登录</p>
          </div>

          <div class="login-switch">
            <a @click="switchLoginType('password')">使用账号密码登录</a>
          </div>
        </div>

        <!-- 错误提示 -->
        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue';
import request from '@/utils/request';

export default {
  name: 'LoginPage',
  components: {
    UserOutlined,
    LockOutlined
  },
  setup() {
    const router = useRouter();
    const loginType = ref('password'); // 'password' 或 'dingtalk'
    const loading = ref(false);
    const errorMessage = ref('');
    const rememberMe = ref(false);

    // 账号密码登录表单
    const passwordForm = reactive({
      username: '',
      password: ''
    });

    // 切换登录方式
    const switchLoginType = (type) => {
      loginType.value = type;
      errorMessage.value = '';

      if (type === 'dingtalk') {
        // 在组件挂载后初始化钉钉二维码
        setTimeout(() => {
          initDingTalkQRCode();
        }, 100);
      }
    };

    // 钉钉扫码登录相关变量
    let dingTalkTimer = null;

    // 初始化钉钉二维码
    const initDingTalkQRCode = () => {
      // 清除之前的定时器
      if (dingTalkTimer) {
        clearInterval(dingTalkTimer);
      }

      // 获取钉钉二维码容器
      const qrcodeContainer = document.getElementById('dingtalk-qrcode');
      if (!qrcodeContainer) return;

      // 清空容器
      qrcodeContainer.innerHTML = '';

      // 加载钉钉扫码登录脚本
      if (!window.DDLogin) {
        const script = document.createElement('script');
        script.src = 'https://g.alicdn.com/dingding/dinglogin/0.0.5/ddLogin.js';
        script.async = true;
        script.onload = createDingTalkQRCode;
        document.head.appendChild(script);
      } else {
        createDingTalkQRCode();
      }
    };

    // 创建钉钉二维码
    const createDingTalkQRCode = () => {
      if (!window.DDLogin) {
        message.error('钉钉扫码登录组件加载失败');
        return;
      }

      const qrcodeContainer = document.getElementById('dingtalk-qrcode');

      // 配置钉钉扫码登录
      // 注意：实际项目中需要替换为真实的appId和redirectUri
      const ddLoginOptions = {
        id: 'dingtalk-qrcode',
        goto: encodeURIComponent('https://oapi.dingtalk.com/connect/oauth2/sns_authorize'),
        style: 'border:none;background-color:#FFFFFF;',
        width: '300',
        height: '300'
      };

      // 创建钉钉二维码
      const ddInstance = new window.DDLogin(ddLoginOptions);

      // 监听钉钉扫码事件
      if (typeof window.addEventListener != 'undefined') {
        window.addEventListener('message', handleDingTalkMessage, false);
      } else if (typeof window.attachEvent != 'undefined') {
        window.attachEvent('onmessage', handleDingTalkMessage);
      }
    };

    // 处理钉钉扫码消息
    const handleDingTalkMessage = (event) => {
      const origin = event.origin;
      if (origin === 'https://login.dingtalk.com') {
        const loginTmpCode = event.data;

        // 获取到临时授权码后，调用后端接口进行登录
        if (loginTmpCode && loginTmpCode.loginTmpCode) {
          handleDingTalkLogin(loginTmpCode.loginTmpCode);
        }
      }
    };

    // 账号密码登录
    const handlePasswordLogin = async () => {
      try {
        loading.value = true;
        errorMessage.value = '';

        // 调用登录接口
        const response = await request.post('/api/auth/login', {
          username: passwordForm.username,
          password: passwordForm.password
        });

        if (response.code === 200) {
          // 登录成功
          const { token, userInfo, permissions } = response.data;

          // 存储token、用户信息和权限信息
          sessionStorage.setItem('token', token);
          sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
          sessionStorage.setItem('permissions', JSON.stringify(permissions || []));

          // 如果选择了记住我，可以使用localStorage
          if (rememberMe.value) {
            localStorage.setItem('rememberMe', 'true');
          }

          message.success('登录成功');

          // 跳转到首页
          router.push('/toc/home');
        } else {
          // 登录失败
          errorMessage.value = response.data.message || '登录失败，请检查用户名和密码';
        }
      } catch (error) {
        console.error('登录异常:', error);
        errorMessage.value = '登录异常，请稍后重试';
      } finally {
        loading.value = false;
      }
    };

    // 钉钉扫码登录回调
    const handleDingTalkLogin = (code) => {
      loading.value = true;
      errorMessage.value = '';

      // 调用登录接口
      request.post('/api/auth/login', {
        thirdPartyType: 'dingtalk',
        thirdPartyCode: code
      })
        .then(response => {
          if (response.code === 200) {
            // 登录成功
            const { token, userInfo, permissions } = response.data;

            // 存储token、用户信息和权限信息
            sessionStorage.setItem('token', token);
            sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
            sessionStorage.setItem('permissions', JSON.stringify(permissions || []));

            message.success('登录成功');

            // 跳转到首页
            router.push('/toc/home');
          } else {
            // 登录失败
            errorMessage.value = response.message || '登录失败，请重试';
          }
        })
        .catch(error => {
          console.error('登录异常:', error);
          errorMessage.value = '登录异常，请稍后重试';
        })
        .finally(() => {
          loading.value = false;
        });
    };

    onMounted(() => {
      // 检查是否有记住我的设置
      const remembered = localStorage.getItem('rememberMe');
      if (remembered === 'true') {
        rememberMe.value = true;
      }

      // 如果初始登录方式是钉钉扫码，则初始化钉钉二维码
      if (loginType.value === 'dingtalk') {
        initDingTalkQRCode();
      }
    });

    // 组件卸载前清理资源
    onBeforeUnmount(() => {
      // 清除钉钉扫码登录的定时器
      if (dingTalkTimer) {
        clearInterval(dingTalkTimer);
      }

      // 移除钉钉扫码登录的消息监听
      if (typeof window.removeEventListener != 'undefined') {
        window.removeEventListener('message', handleDingTalkMessage, false);
      } else if (typeof window.detachEvent != 'undefined') {
        window.detachEvent('onmessage', handleDingTalkMessage);
      }
    });

    return {
      loginType,
      passwordForm,
      rememberMe,
      loading,
      errorMessage,
      switchLoginType,
      handlePasswordLogin,
      handleDingTalkLogin,
      initDingTalkQRCode
    };
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  height: 100vh;
  width: 100%;
}

/* 左侧品牌区域 */
.brand-area {
  width: 40%;
  background-color: #F5F7FA;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-content {
  text-align: center;
}

.logo {
  margin-bottom: 20px;
}

.logo img {
  max-width: 120px;
}

.slogan h1 {
  font-size: 28px;
  color: #1890FF;
  margin-bottom: 10px;
}

.slogan p {
  font-size: 16px;
  color: #666;
}

/* 右侧登录表单区域 */
.login-form-area {
  width: 60%;
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-form-container {
  width: 320px;
}

.login-title {
  font-size: 24px;
  text-align: center;
  margin-bottom: 30px;
}

/* 表单项样式 */
:deep(.ant-input),
:deep(.ant-input-password) {
  height: 40px;
  border-radius: 4px;
  border-color: #EEEEEE;
}

/* 登录按钮 */
.login-button {
  width: 100%;
  height: 44px;
  background-color: #1890FF;
  border-color: #1890FF;
  border-radius: 4px;
  font-size: 16px;
  margin-top: 20px;
}

/* 登录选项 */
.login-options {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

/* 切换登录方式 */
.login-switch {
  text-align: center;
  margin-top: 20px;
}

.login-switch a {
  color: #1890FF;
  text-decoration: none;
}

/* 钉钉扫码登录 */
.dingtalk-login {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qrcode-container {
  margin-bottom: 20px;
  text-align: center;
}

.qrcode {
  width: 300px;
  height: 300px;
  margin: 0 auto;
  background-color: #FFFFFF;
  display: flex;
  align-items: center;
  justify-content: center;
}

.qrcode-tip {
  margin-top: 10px;
  color: #666;
}

/* 错误提示 */
.error-message {
  color: #FF4D4F;
  font-size: 12px;
  text-align: center;
  margin-top: 20px;
}
</style>
