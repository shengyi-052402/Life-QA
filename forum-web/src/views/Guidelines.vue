<template>
  <div class="guidelines-page">
    <section class="guidelines-hero">
      <p class="eyebrow">Community Guidelines</p>
      <h1>社区规则</h1>
      <p class="hero-text">
        为了让问答、经验分享和运营反馈都能被认真看见，请在发帖和互动前了解以下规则。
      </p>
    </section>

    <section class="rule-grid">
      <article class="rule-card">
        <span class="rule-index">01</span>
        <h2>发帖规范</h2>
        <ul>
          <li>标题应清楚描述问题或观点，避免空泛标题和刻意夸张。</li>
          <li>正文尽量补充背景、已尝试的方法、期望结果和必要截图。</li>
          <li>请选择合适分类；反馈产品体验、运营活动或社区治理问题时，可使用“运营反馈”。</li>
          <li>转载内容需注明来源，引用他人观点时保持上下文完整。</li>
        </ul>
      </article>

      <article class="rule-card">
        <span class="rule-index">02</span>
        <h2>互动规范</h2>
        <ul>
          <li>讨论聚焦问题本身，避免人身攻击、嘲讽、贴标签或带节奏。</li>
          <li>回答问题时尽量给出可验证的步骤、依据或经验边界。</li>
          <li>不同观点可以充分讨论，但需要保持尊重和基本事实准确。</li>
          <li>发现违规内容请通过反馈或举报说明原因，不要在评论区扩大冲突。</li>
        </ul>
      </article>

      <article class="rule-card">
        <span class="rule-index">03</span>
        <h2>禁止内容</h2>
        <ul>
          <li>违法违规、暴力、色情、赌博、诈骗、黑产或侵害隐私的内容。</li>
          <li>恶意刷屏、广告引流、重复灌水、批量发布低质量内容。</li>
          <li>冒充他人、伪造信息、传播谣言或诱导用户进行高风险操作。</li>
          <li>上传恶意文件，或尝试破坏平台、绕过权限、攻击其他用户。</li>
        </ul>
      </article>

      <article class="rule-card">
        <span class="rule-index">04</span>
        <h2>反馈与处理</h2>
        <ul>
          <li>运营反馈类帖子请尽量说明问题场景、复现步骤、影响范围和建议方案。</li>
          <li>管理员会根据内容严重程度进行提醒、隐藏、删除、限制账号等处理。</li>
          <li>如对处理结果有疑问，可补充事实依据后再次反馈。</li>
          <li>社区规则会随产品和用户规模调整，重大变化会在站内说明。</li>
        </ul>
      </article>
    </section>

    <section class="notice-panel">
      <h2>共同维护高质量社区</h2>
      <p>
        这里鼓励真实经验、认真提问、建设性建议和友善互助。每一次清楚表达和理性回应，都会降低他人获取信息的成本。
      </p>
      <el-button type="primary" @click="router.push('/explore')">返回帖子主页</el-button>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.guidelines-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.guidelines-hero {
  padding: 34px;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.14), rgba(103, 194, 58, 0.12));
  border: 1px solid var(--border-color);
}

.eyebrow {
  margin: 0 0 8px;
  color: var(--primary-color);
  font-weight: 700;
  letter-spacing: 0;
}

.guidelines-hero h1 {
  margin: 0;
  font-size: 2.2rem;
  color: var(--text-primary);
}

.hero-text {
  max-width: 720px;
  margin: 12px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.rule-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.rule-card {
  min-height: 260px;
  padding: 24px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  background: var(--card-bg);
  box-shadow: var(--shadow-sm);
}

.rule-index {
  color: var(--primary-color);
  font-size: 0.9rem;
  font-weight: 700;
}

.rule-card h2 {
  margin: 8px 0 14px;
  color: var(--text-primary);
  font-size: 1.25rem;
}

.rule-card ul {
  margin: 0;
  padding-left: 18px;
  color: var(--text-secondary);
  line-height: 1.8;
}

.rule-card li {
  margin-bottom: 8px;
}

.notice-panel {
  padding: 26px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  background: var(--card-bg);
  text-align: center;
}

.notice-panel h2 {
  margin: 0 0 10px;
  color: var(--text-primary);
}

.notice-panel p {
  max-width: 760px;
  margin: 0 auto 18px;
  color: var(--text-secondary);
  line-height: 1.7;
}

@media (max-width: 900px) {
  .rule-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .guidelines-hero {
    padding: 24px;
  }

  .guidelines-hero h1 {
    font-size: 1.8rem;
  }
}
</style>
