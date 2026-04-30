<template>
  <section class="support-page">
    <div class="support-shell">
      <header class="page-card">
        <div class="section-head">
          <div>
            <p class="eyebrow">Support</p>
            <h1>고객센터</h1>
            <p class="description">
              문의 방법, 약관, 개인정보 처리방침, 사업자 정보, 프로그램 버전 정보를 한곳에서 확인할 수 있습니다.
            </p>
          </div>
          <button class="ghost-button" type="button" @click="goBack">돌아가기</button>
        </div>
      </header>

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
            <p>문의 메일에는 가입 이메일, 닉네임, 문제 화면, 재현 순서를 함께 적어주시면 더 빠르게 확인할 수 있습니다.</p>
          </div>
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
          <p>법령상 보관 의무가 없는 개인정보는 회원 탈퇴 또는 처리 목적 달성 후 지체 없이 삭제하거나 비식별 처리합니다.</p>
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
import { useRouter } from "vue-router";
import packageJson from "../../../package.json";

const router = useRouter();
const currentVersion = packageJson.version;
const latestVersion = packageJson.version;
const versionMessage = currentVersion === latestVersion
  ? "현재 설치된 앱이 최신 버전입니다."
  : "더 새로운 앱 버전이 있어 업데이트가 필요합니다.";

function goBack() {
  router.push("/profile");
}
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
.eyebrow, .section-label { margin: 0 0 10px; color: #af5f42; font-size: 11px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; }
h1, h2 { margin: 0; color: #31211d; }
.description { margin: 12px 0 0; color: #6f564d; line-height: 1.7; }
.section-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; margin-bottom: 16px; }
.section-head.compact { margin-bottom: 14px; }
.info-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.info-card, .content-card {
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
@media (max-width: 760px) {
  .section-head { flex-direction: column; align-items: flex-start; }
  .info-grid { grid-template-columns: 1fr; }
  .full { grid-column: auto; }
}
</style>
