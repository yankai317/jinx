
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './assets/styles/global.css'
import './assets/styles/fixes.css'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import SvgIcon from './components/common/SvgIcon.vue'
import './icons' // 导入SVG图标
import VConsole from 'vconsole';

const vConsole = new VConsole();

// 配置dayjs中文
dayjs.locale('zh-cn')

const app = createApp(App)

// 全局注册SvgIcon组件
app.component('svg-icon', SvgIcon)

app.use(router).use(Antd, { locale: zhCN }).mount('#app')
