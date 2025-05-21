import axios from 'axios';
import { message } from 'ant-design-vue';
import router from '../router';

// 创建axios实例
const request = axios.create({
  baseURL: process.env.VUE_APP_API_BASE_URL || '', // 从环境变量获取API基础URL
  timeout: 15000, // 请求超时时间
});

// 请求拦截器
request.interceptors.request.use(
  config => {
    // 从sessionStorage获取token
    const token = sessionStorage.getItem('token');

    // 如果存在token，则添加到请求头
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    // 添加DEBUG日志 - API请求
    if (config.url.includes('queryUserOrgs') || config.url.includes('org')) {
      console.log('🚀 部门相关API请求:', {
        url: config.url,
        method: config.method,
        data: config.data
      });
    }

    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  response => {
    // 添加DEBUG日志 - API响应
    if (response.config.url.includes('queryUserOrgs') || response.config.url.includes('org')) {
      console.log('📣 部门相关API响应:', {
        url: response.config.url,
        status: response.status,
        data: response.data
      });
    }

    // 如果返回的是文件流，直接返回
    if (response.config.responseType === 'blob') {
      return response;
    }

    const res = response.data;

    // 如果返回的状态码不是200，则判断为错误
    if (res.code !== 200) {
      message.error(res.message || '请求失败');

      // 401: 未登录或token过期
      if (res.code === 401) {
        // 清除登录信息
        sessionStorage.removeItem('token');
        sessionStorage.removeItem('userInfo');

        // 跳转到登录页
        router.push('/login');
      }

      return Promise.reject(new Error(res.message || '请求失败'));
    }

    return res;
  },
  error => {
    console.error('响应错误:', error);

    // 处理网络错误
    if (error.message.includes('Network Error')) {
      message.error('网络异常，请检查网络连接');
    } else if (error.message.includes('timeout')) {
      message.error('请求超时，请稍后重试');
    } else {
      // 处理HTTP状态码错误
      const status = error.response?.status;

      switch (status) {
        case 401:
          message.error('未登录或登录已过期，请重新登录');
          // 清除登录信息
          sessionStorage.removeItem('token');
          sessionStorage.removeItem('userInfo');
          // 跳转到登录页
          router.push('/login');
          break;
        case 403:
          message.error('没有权限访问该资源');
          break;
        case 404:
          message.error('请求的资源不存在');
          break;
        case 500:
          message.error('服务器错误，请稍后重试');
          break;
        default:
          message.error(error.response?.data?.message || '请求失败，请稍后重试');
      }
    }

    return Promise.reject(error);
  }
);

export default request;
