import request from '@/utils/request';
import { message } from 'ant-design-vue';
import * as dd from 'dingtalk-jsapi'; // 此方式为整体加载，也可按需进行加载

export const ddAuth = {
  init: () => {
  },
  auth: (query) => {
    console.log('auth', '11111111111111', dd.env, dd.runtime.permission.requestAuthCode)

    const corpId = query.corpId;
    return new Promise((resolve, reject) => {
      dd.ready(function () {
        console.log('auth', '22222222222222', corpId)

        dd.runtime.permission.requestAuthCode({
          corpId: corpId,
          onSuccess: function (info) {
            const code = info.code // Fixed: added const declaration
            resolve({ code, corpId })
            console.log('info', info, code, corpId)
          },
          onFail: function (err) {
            console.log('err', err)
            reject(err)
          },
        });
      });
    })
  },
  login: (authRes) => {
    return new Promise((resolve, reject) => {
      // 调用登录接口
      request.post('/api/auth/login', {
        thirdPartyType: 'dingtalk',
        corpId: authRes.corpId,
        thirdPartyCode: authRes.code
      })
        .then(response => {
          if (response.code === 200) {
            // 登录成功
            const { token, userInfo, permissions } = response.data;

            // 存储token、用户信息和权限信息
            sessionStorage.setItem('token', token);
            sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
            sessionStorage.setItem('permissions', JSON.stringify(permissions || []));

            resolve()
          } else {
            // 登录失败
            errorMessage.value = response.message || '登录失败，请重试';
            reject(error)
          }
        })
        .catch(error => {
          console.error('登录异常:', error);
          errorMessage.value = '登录异常，请稍后重试';
          reject(error)
        })
    })
  }
}


