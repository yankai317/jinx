
const { defineConfig } = require('@vue/cli-service')
const path = require('path')

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: path.resolve(__dirname, '../starter/src/main/resources/static'),
  assetsDir: '',
  publicPath: '/',
  lintOnSave: false,
  devServer: {
    proxy: {
      '/api': {  // 你要代理的路径前缀
        target: 'http://localhost:8088',  // 目标服务器地址
        changeOrigin: true,  // 改变请求源
      }
    }
  },
  chainWebpack: config => {
    // 配置SVG图标
    config.module
      .rule('svg')
      .exclude.add(path.resolve(__dirname, './src/assets/icons'))
      .end();

    config.module
      .rule('icons')
      .test(/\.svg$/)
      .include.add(path.resolve(__dirname, './src/assets/icons'))
      .end()
      .use('svg-sprite-loader')
      .loader('svg-sprite-loader')
      .options({
        symbolId: 'icon-[name]'
      })
      .end();
  }
})
