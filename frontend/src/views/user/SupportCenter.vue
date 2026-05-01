<template>
  <section class="support-page">
    <div class="support-shell">
      <header class="page-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Support</p>
            <h1>고객센터</h1>
            <p class="description">
              문의 방법, 약관, 개인정보 처리방침, 사업자 정보, 프로그램 버전 정보와 최근 문의 상태를 한곳에서 확인할 수 있습니다.
            </p>
          </div>
          <button class="ghost-button" type="button" @click="goBack">돌아가기</button>
        </div>
      </header>

      <section v-if="answeredInquiryCount > 0" class="page-card alert-card">
        <p class="eyebrow">Answered</p>
        <h2>답변이 등록된 문의가 있어요</h2>
        <p class="description">
          최근 문의 중 {{ answeredInquiryCount }}건에 운영자 답변이 등록되었습니다. 아래 최근 문의 내역에서 바로 확인해 주세요.
        </p>
      </section>

      <section class="page-card">
        <div class="section-head compact">
          <div>
            <p class="section-label">Contact</p>
            <h2>문의하기</h2>
          </div>
        </div>
        <div class="info-grid">
          <div class="info-card">
            <strong>이메일 문의</strong>
            <p>support@dating-app.local</p>
          </div>
          <div class="info-card">
            <strong>운영 시간</strong>
            <p>평일 10:00 - 18:00</p>
          </div>
          <div class="info-card full">
            <strong>안내</strong>
            <p>
              해결되지 않는 문의는 위 메일 주소로 보내 주세요. 가입 이메일, 닉네임, 문제 화면, 재현 순서를 함께 적어주시면 더 빠르게 확인할 수 있습니다.
            </p>
          </div>
        </div>
      </section>

      <section class="page-card">
        <div class="section-head compact">
          <div>
            <p class="section-label">Recent</p>
            <h2>최근 문의 내역</h2>
          </div>
        </div>
        <div v-if="loadingInquiries" class="content-card">
          <p>최근 문의 내역을 불러오는 중입니다.</p>
        </div>
        <div v-else-if="inquiries.length === 0" class="content-card">
          <p>아직 문의 내역이 없습니다. 해결되지 않는 문의는 위 메일 주소로 보내 주세요.</p>
        </div>
        <div v-else class="inquiry-list">
          <article v-for="inquiry in inquiries" :key="inquiry.id" class="inquiry-card">
            <div class="inquiry-head">
              <div>
                <strong>{{ categoryLabel(inquiry.category) }}</strong>
                <span>{{ formatDateTime(inquiry.createdAt) }}</span>
              </div>
              <span class="status-pill" :class="{ resolved: Boolean(inquiry.adminReply) }">
                {{ statusLabel(inquiry.status) }}
              </span>
            </div>
            <p class="inquiry-question">{{ inquiry.question }}</p>
            <p v-if="inquiry.adminReply" class="inquiry-reply">답변: {{ inquiry.adminReply }}</p>
            <p v-else class="inquiry-waiting">아직 운영자 답변이 등록되지 않았습니다.</p>
          </article>
        </div>
      </section>

      <section class="page-card">
        <div class="section-head compact">
          <div>
            <p class="section-label">Terms</p>
            <h2>이용약관</h2>
          </div>
        </div>
        <div class="content-card">
          <p>회원은 실명에 준하는 정확한 정보를 바탕으로 서비스를 이용해야 하며, 타인의 정보를 도용해서는 안 됩니다.</p>
          <p>불법 촬영물, 사기 유도, 금전 요구, 욕설, 혐오 표현, 타인의 권리를 침해하는 콘텐츠는 등록할 수 없습니다.</p>
          <p>운영 정책 위반 시 게시물 삭제, 심사 반려, 서비스 이용 제한, 계정 정지 또는 영구 차단이 적용될 수 있습니다.</p>
        </div>
      </section>

      <section class="page-card">
        <div class="section-head compact">
          <div>
            <p class="section-label">Privacy</p>
            <h2>개인정보 처리방침</h2>
          </div>
        </div>
        <div class="content-card">
          <p>서비스 운영을 위해 이메일, 전화번호, 프로필 정보, 심사용 사진, 접속 기록과 같은 최소한의 개인정보를 수집할 수 있습니다.</p>
          <p>수집된 정보는 회원 식별, 서비스 제공, 심사 운영, 안전 모니터링, 고객 문의 대응 목적에 한해 사용됩니다.</p>
          <p>수집된 고객 문의 내용은 문의 해결과 운영 이력 확인 목적으로 보관될 수 있으며, 관련 법령과 내부 정책에 따라 관리됩니다.</p>
        </div>
      </section>

      <section class="page-card">
        <div class="section-head compact">
          <div>
            <p class="section-label">Business</p>
            <h2>사업자 정보</h2>
          </div>
        </div>
        <div class="info-grid">
          <div class="info-card">
            <strong>서비스명</strong>
            <p>Dating App</p>
          </div>
          <div class="info-card">
            <strong>사업자 등록 상태</strong>
            <p>현재 개발 및 테스트 단계입니다.</p>
          </div>
          <div class="info-card full">
            <strong>안내</strong>
            <p>정식 배포 전 사업자 정보, 통신판매업 신고 정보, 고객센터 연락처를 실제 운영 정보로 교체해야 합니다.</p>
          </div>
        </div>
      </section>

      <section class="page-card">
        <div class="section-head compact">
          <div>
            <p class="section-label">Program</p>
            <h2>프로그램 정보</h2>
          </div>
        </div>
        <div class="info-grid">
          <div class="info-card">
            <strong>현재 앱 버전</strong>
            <p>{{ currentVersion }}</p>
          </div>
          <div class="info-card">
            <strong>최신 앱 버전</strong>
            <p>{{ latestVersion }}</p>
          </div>
          <div class="info-card full">
            <strong>버전 안내</strong>
            <p>{{ versionMessage }}</p>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import packageJson from "../../../package.json";
import api from "../../api/api";

const router = useRouter();
const currentVersion = packageJson.version;
const latestVersion = packageJson.version;
const versionMessage = currentVersion === latestVersion
  ? "현재 설치된 앱이 최신 버전입니다."
  : "더 새로운 앱 버전이 있어 업데이트가 필요합니다.";

const loadingInquiries = ref(true);
const inquiries = ref([]);

const answeredInquiryCount = computed(() =>
  inquiries.value.filter((inquiry) => Boolean(inquiry.adminReply)).length,
);

function goBack() {
  router.push("/profile");
}

function categoryLabel(category) {
  return {
    GENERAL: "일반 문의",
    LOGIN: "로그인/회원가입",
    REVIEW: "사진 심사",
    PROFILE: "프로필 수정",
    SAFETY: "스캠/신고",
  }[category] || "문의";
}

function statusLabel(status) {
  return {
    OPEN: "접수 대기",
    IN_PROGRESS: "확인 중",
    RESOLVED: "답변 완료",
    CLOSED: "종료",
  }[status] || status;
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  return new Date(value).toLocaleString("ko-KR");
}

async function loadInquiries() {
  loadingInquiries.value = true;
  try {
    const { data } = await api.get("/support/inquiries/me");
    inquiries.value = data;
  } finally {
    loadingInquiries.value = false;
  }
}

onMounted(loadInquiries);
</script>

<style scoped>
.support-page { min-height: 100vh; padding: max(16px, env(safe-area-inset-top)) 16px 24px; display: flex; justify-content: center; }
.support-shell { width: min(100%, 720px); display: grid; gap: 16px; }
.page-card {
  padding: 24px 20px;
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.08);
}
.alert-card {
  background: linear-gradient(180deg, rgba(255, 247, 241, 0.96) 0%, rgba(251, 234, 222, 0.96) 100%);
}
.eyebrow, .section-label { margin: 0 0 10px; color: #af5f42; font-size: 11px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; }
h1, h2 { margin: 0; color: #31211d; }
.description { margin: 12px 0 0; color: #6f564d; line-height: 1.7; }
.section-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }
.section-head.compact { margin-bottom: 14px; }
.info-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.info-card, .content-card, .inquiry-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 247, 242, 0.9);
  color: #5d4640;
  line-height: 1.7;
}
.info-card strong { display: block; margin-bottom: 8px; color: #2f211d; }
.info-card p, .content-card p { margin: 0; }
.content-card { display: grid; gap: 10px; }
.full { grid-column: 1 / -1; }
.ghost-button {
  border: 1px solid rgba(207, 150, 128, 0.36);
  background: rgba(255, 248, 243, 0.82);
  color: #6a4137;
  border-radius: 999px;
  padding: 12px 18px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}
.inquiry-list {
  display: grid;
  gap: 12px;
}
.inquiry-head {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 10px;
}
.inquiry-head strong {
  display: block;
  color: #2f211d;
}
.inquiry-head span:first-of-type {
  display: block;
  color: #8b675b;
  font-size: 13px;
}
.status-pill {
  padding: 8px 12px;
  border-radius: 999px;
  background: #fff1e8;
  color: #9d5a43;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}
.status-pill.resolved {
  background: rgba(230, 244, 235, 0.94);
  color: #2f6b48;
}
.inquiry-question,
.inquiry-reply,
.inquiry-waiting {
  margin: 0;
}
.inquiry-reply {
  margin-top: 10px;
  color: #3f5d4b;
}
.inquiry-waiting {
  margin-top: 10px;
  color: #8b675b;
}
@media (max-width: 760px) {
  .section-head,
  .inquiry-head { flex-direction: column; align-items: flex-start; }
  .info-grid { grid-template-columns: 1fr; }
  .full { grid-column: auto; }
}
</style>
