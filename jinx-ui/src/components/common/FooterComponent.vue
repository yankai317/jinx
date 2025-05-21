
<template>
  <div class="footer">
    <!-- <div class="footer-content">
      <div class="footer-logo">
        <img src="@/assets/logo.png" alt="系统Logo" />
        <h3>银泰商业集团 数智化学习平台</h3>
      </div>
      <div class="footer-links">
        <div class="link-group">
          <h4>关于我们</h4>
          <a href="#">公司简介</a>
          <a href="#">联系方式</a>
          <a href="#">加入我们</a>
        </div>
        <div class="link-group">
          <h4>帮助中心</h4>
          <a href="#">使用指南</a>
          <a href="#">常见问题</a>
          <a href="#">意见反馈</a>
        </div>
        <div class="link-group">
          <h4>友情链接</h4>
          <a href="#">企业官网</a>
          <a href="#">人力资源</a>
          <a href="#">内部系统</a>
        </div>
      </div>
    </div> -->
    <div class="footer-bottom">
      <p>© {{ currentYear }} 银泰商业集团 数智化学习平台 版权所有</p>
    </div>
  </div>
</template>

<script>
import { defineComponent, computed } from 'vue';

export default defineComponent({
  name: 'FooterComponent',
  setup() {
    // 获取当前年份
    const currentYear = computed(() => new Date().getFullYear());

    return {
      currentYear
    };
  }
});
</script>

<style scoped>
.footer {
  background-color: #f9fafb;
  padding: 40px 0 20px;
  border-top: 1px solid #e5e7eb;
}

.footer-content {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  padding: 0 24px;
}

.footer-logo {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.footer-logo img {
  height: 40px;
  margin-bottom: 12px;
}

.footer-logo h3 {
  margin: 0;
  font-size: 16px;
  color: #374151;
}

.footer-links {
  display: flex;
  gap: 60px;
}

.link-group {
  display: flex;
  flex-direction: column;
}

.link-group h4 {
  margin: 0 0 16px;
  font-size: 16px;
  color: #374151;
}

.link-group a {
  margin-bottom: 8px;
  color: #6b7280;
  text-decoration: none;
  font-size: 14px;
}

.link-group a:hover {
  color: #1890FF;
}

.footer-bottom {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 24px 0;
  /* border-top: 1px solid #e5e7eb; */
  margin-top: 40px;
  text-align: center;
}

.footer-bottom p {
  margin: 0;
  font-size: 14px;
  color: #9ca3af;
}

@media (max-width: 768px) {
  .footer-content {
    flex-direction: column;
  }

  .footer-logo {
    margin-bottom: 24px;
  }

  .footer-links {
    flex-direction: column;
    gap: 24px;
  }
}
</style>
