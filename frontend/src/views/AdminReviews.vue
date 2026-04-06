<template>
  <section class="admin-page">
    <div class="admin-card">
      <div class="header-row">
        <div>
          <p class="eyebrow">Admin Review</p>
          <h1>회원가입 심사 관리</h1>
        </div>
      </div>

      <div class="toolbar">
        <label>
          <span>운영자 키</span>
          <input v-model.trim="adminKey" type="password" placeholder="X-Admin-Key 입력" />
        </label>

        <label>
          <span>목록 상태</span>
          <select v-model="statusFilter">
            <option value="PENDING_REVIEW">심사 대기</option>
            <option value="REJECTED">반려</option>
            <option value="ACTIVE">승인 완료</option>
          </select>
        </label>

        <button class="primary-button" @click="loadCandidates" :disabled="loading">
          {{ loading ? "불러오는 중..." : "목록 조회" }}
        </button>
      </div>

      <p v-if="message" class="message success">{{ message }}</p>
      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div v-if="candidates.length" class="candidate-list">
        <article v-for="candidate in candidates" :key="candidate.userId" class="candidate-card">
          <div class="candidate-head">
            <div>
              <strong>{{ candidate.nickname }}</strong>
              <p>{{ candidate.email }}</p>
            </div>
            <span class="status-badge">{{ candidate.status }}</span>
          </div>

          <div class="info-grid">
            <div><span>지역</span><strong>{{ candidate.region || '-' }}</strong></div>
            <div><span>직업</span><strong>{{ candidate.job || '-' }}</strong></div>
            <div><span>MBTI</span><strong>{{ candidate.mbti || '-' }}</strong></div>
            <div><span>성별/생년월일</span><strong>{{ candidate.gender }} / {{ candidate.birthDate }}</strong></div>
          </div>

          <p class="intro">{{ candidate.introduction || '자기소개 없음' }}</p>

          <div class="review-rule" :class="{ incomplete: !hasMinimumImages(candidate) }">
            <strong>심사 사진 수</strong>
            <p>{{ candidate.images.length }}장 등록됨 / 승인 기준은 최소 2장입니다.</p>
          </div>

          <div class="image-list" v-if="candidate.images.length">
            <div v-for="image in candidate.images" :key="image.id" class="image-item">
              <img :src="toAbsoluteImageUrl(image.imageUrl)" alt="profile review" />
              <span>{{ image.imageOrder }}번 사진{{ image.isMain ? ' · 대표' : '' }}</span>
            </div>
          </div>

          <p v-else class="empty-image-text">아직 등록된 사진이 없습니다.</p>

          <label class="reject-box">
            <span>반려 사유</span>
            <textarea
              v-model.trim="rejectComments[candidate.userId]"
              rows="3"
              placeholder="반려 시 사용자에게 보여줄 사유를 입력해주세요."
            ></textarea>
          </label>

          <div class="action-row">
            <button class="approve-button" :disabled="!hasMinimumImages(candidate)" @click="approveCandidate(candidate.userId)">
              승인
            </button>
            <button class="reject-button" @click="rejectCandidate(candidate.userId)">반려</button>
          </div>

          <p v-if="!hasMinimumImages(candidate)" class="approval-help">
            사진이 2장 이상 등록되어야 승인할 수 있습니다.
          </p>
        </article>
      </div>

      <p v-else-if="loaded" class="empty-state">조회된 회원이 없습니다.</p>
    </div>
  </section>
</template>

<script setup>
import { ref } from "vue";
import axios from "axios";

const baseURL = "http://localhost:8080/api";
const assetBaseURL = "http://localhost:8080";

const adminKey = ref(localStorage.getItem("adminReviewKey") || "");
const statusFilter = ref("PENDING_REVIEW");
const candidates = ref([]);
const rejectComments = ref({});
const loading = ref(false);
const loaded = ref(false);
const message = ref("");
const errorMessage = ref("");

const getHeaders = () => ({
  "X-Admin-Key": adminKey.value,
});

const persistAdminKey = () => {
  localStorage.setItem("adminReviewKey", adminKey.value);
};

const hasMinimumImages = (candidate) => candidate.images.length >= 2;

const loadCandidates = async () => {
  loading.value = true;
  message.value = "";
  errorMessage.value = "";

  try {
    persistAdminKey();
    const { data } = await axios.get(`${baseURL}/admin/reviews`, {
      headers: getHeaders(),
      params: { status: statusFilter.value },
    });
    candidates.value = data;
    loaded.value = true;
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "심사 목록 조회에 실패했습니다.";
  } finally {
    loading.value = false;
  }
};

const approveCandidate = async (userId) => {
  try {
    persistAdminKey();
    const { data } = await axios.post(`${baseURL}/admin/reviews/${userId}/approve`, null, {
      headers: getHeaders(),
    });
    message.value = data.message;
    await loadCandidates();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "승인 처리에 실패했습니다.";
  }
};

const rejectCandidate = async (userId) => {
  try {
    persistAdminKey();
    const reviewComment = rejectComments.value[userId];
    if (!reviewComment) {
      errorMessage.value = "반려 사유를 먼저 입력해주세요.";
      return;
    }

    const { data } = await axios.post(
      `${baseURL}/admin/reviews/${userId}/reject`,
      { reviewComment },
      { headers: getHeaders() }
    );
    message.value = data.message;
    await loadCandidates();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "반려 처리에 실패했습니다.";
  }
};

const toAbsoluteImageUrl = (path) => {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }
  return `${assetBaseURL}${path}`;
};
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 24px;
  background: linear-gradient(135deg, #fff4eb 0%, #ffd9c8 100%);
}

.admin-card {
  width: min(100%, 1100px);
  margin: 0 auto;
  padding: 32px;
  border-radius: 28px;
  background: rgba(255, 251, 247, 0.96);
  box-shadow: 0 20px 50px rgba(99, 52, 35, 0.14);
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eyebrow {
  margin: 0 0 10px;
  color: #aa5131;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #36231c;
}

.toolbar {
  margin-top: 24px;
  display: grid;
  grid-template-columns: 2fr 1fr auto;
  gap: 16px;
  align-items: end;
}

label {
  display: grid;
  gap: 8px;
  color: #503731;
  font-weight: 600;
}

input,
select,
textarea {
  border: 1px solid #e6c1b4;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
  font-family: inherit;
}

textarea {
  resize: vertical;
}

.primary-button,
.approve-button,
.reject-button {
  border: none;
  border-radius: 14px;
  padding: 14px 18px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.primary-button,
.approve-button {
  background: #cd6d2d;
  color: #fff;
}

.approve-button:disabled {
  cursor: not-allowed;
  background: #d9c5b8;
  color: #7c665a;
}

.reject-button {
  background: #fff;
  color: #7c2d24;
  border: 1px solid #e0b4a8;
}

.message {
  margin-top: 16px;
}

.success {
  color: #1f7a43;
}

.error {
  color: #b72f2f;
}

.candidate-list {
  margin-top: 24px;
  display: grid;
  gap: 20px;
}

.candidate-card {
  padding: 20px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #efd5ca;
}

.candidate-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.candidate-head p {
  margin: 6px 0 0;
  color: #7a5d55;
}

.status-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: #fff1e9;
  color: #8d462a;
  font-size: 13px;
  font-weight: 700;
}

.info-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.info-grid span {
  display: block;
  margin-bottom: 6px;
  color: #906759;
  font-size: 13px;
}

.info-grid strong {
  color: #3a251e;
}

.intro {
  margin-top: 16px;
  color: #5f473f;
  line-height: 1.6;
}

.review-rule {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #fff7ef;
  border: 1px solid #efd5ca;
  color: #6f544c;
}

.review-rule strong {
  display: block;
  margin-bottom: 6px;
  color: #3a251e;
}

.review-rule p {
  margin: 0;
}

.review-rule.incomplete {
  background: #fff2f0;
  border-color: #efc1b8;
}

.image-list {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.image-item {
  display: grid;
  gap: 8px;
}

.image-item img {
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 16px;
  border: 1px solid #efd5ca;
  background: #fff6f1;
}

.image-item span {
  color: #6f544c;
  font-size: 13px;
}

.empty-image-text {
  margin-top: 16px;
  color: #8b6d64;
}

.reject-box {
  margin-top: 16px;
}

.action-row {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

.approval-help {
  margin-top: 12px;
  color: #b15a39;
  font-size: 14px;
  font-weight: 600;
}

.empty-state {
  margin-top: 24px;
  color: #7a5d55;
}

@media (max-width: 900px) {
  .toolbar {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>