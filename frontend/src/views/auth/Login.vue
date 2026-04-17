<template>
  <section class="auth-page">
    <div
      v-if="showIntro"
      :class="['intro-stage', { leaving: introLeaving }]"
      role="status"
      aria-live="polite"
    >
      <div :key="currentIntroStep.id" class="intro-copy">
        <p class="intro-kicker">{{ currentIntroStep.label }}</p>
        <h1>{{ currentIntroStep.text }}</h1>
        <p v-if="currentIntroStep.subtext" class="intro-subtext">{{ currentIntroStep.subtext }}</p>
        <span class="intro-line" aria-hidden="true"></span>
      </div>
      <button class="intro-skip" type="button" @click="skipIntro">바로 로그인</button>
    </div>

    <div class="auth-card">
      <p class="eyebrow">Dating App</p>
      <h1>로그인</h1>
      <p class="description">이메일 또는 자체 SMS 전화번호 인증으로 서비스를 시작해 보세요.</p>

      <div class="tab-row" role="tablist" aria-label="로그인 방식 선택">
        <button type="button" :class="['tab-button', { active: loginMode === 'email' }]" @click="loginMode = 'email'">
          이메일
        </button>
        <button type="button" :class="['tab-button', { active: loginMode === 'phone' }]" @click="loginMode = 'phone'">
          전화번호
        </button>
      </div>

      <form v-if="loginMode === 'email'" class="auth-form" @submit.prevent="login">
        <label>
          <span>이메일</span>
          <input ref="emailInput" v-model.trim="form.email" type="email" placeholder="you@example.com" required />
        </label>

        <label>
          <span>비밀번호</span>
          <input ref="passwordInput" v-model="form.password" type="password" placeholder="8자 이상 입력" required />
        </label>

        <button class="primary-button" :disabled="loading">
          {{ loading ? '로그인 중...' : '이메일로 로그인' }}
        </button>
      </form>

      <form v-else class="auth-form" @submit.prevent="verifyPhoneAndLogin">
        <label>
          <span>전화번호</span>
          <input ref="phoneInput" v-model.trim="phoneForm.phone" inputmode="numeric" placeholder="01012345678" required />
        </label>

        <button class="ghost-button full" type="button" :disabled="phoneLoading" @click="requestPhoneCode">
          {{ phoneLoading ? '인증번호 발급 중...' : 'SMS 인증번호 발급' }}
        </button>

        <label>
          <span>인증번호</span>
          <input ref="codeInput" v-model.trim="phoneForm.code" inputmode="numeric" maxlength="6" placeholder="6자리 인증번호" required />
        </label>

        <button class="primary-button" :disabled="loading">
          {{ loading ? '확인 중...' : '전화번호 인증 후 자동 가입/로그인' }}
        </button>
      </form>

      <p v-if="noticeMessage" class="message notice">{{ noticeMessage }}</p>
      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="helper-row">
        <RouterLink to="/reset-password">비밀번호를 잊으셨나요?</RouterLink>
        <RouterLink to="/signup">이메일 회원가입</RouterLink>
      </div>

      <div class="policy-box">
        <strong>자체 SMS 인증 안내</strong>
        <p>외부 문자 발송 서비스 없이 자체 인증 흐름만 사용합니다. 인증번호는 백엔드 콘솔의 [DEV SMS AUTH] 로그에서 확인해 주세요.</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken, setAuthTokens } from "../../utils/auth";

const router = useRouter();
const loading = ref(false);
const phoneLoading = ref(false);
const errorMessage = ref("");
const noticeMessage = ref("");
const loginMode = ref("email");
const emailInput = ref(null);
const passwordInput = ref(null);
const phoneInput = ref(null);
const codeInput = ref(null);
const showIntro = ref(true);
const introLeaving = ref(false);
const introStepIndex = ref(0);
const introTimers = [];
const introSteps = [
  {
    id: "visit",
    label: "질문",
    text: "오늘 들어 왔어요?",
    subtext: "처음 온 마음도 편하게 머물 수 있게요.",
  },
  {
    id: "visit-answer",
    label: "대답",
    text: "네. 오늘은 조금 설레도 괜찮아요.",
    subtext: "좋은 대화는 천천히 시작되니까요.",
  },
  {
    id: "type",
    label: "질문",
    text: "어떤 사람을 만나고 싶나요?",
    subtext: "조건보다 먼저, 마음이 편한 사람.",
  },
  {
    id: "type-answer",
    label: "대답",
    text: "대화가 잘 통하는 사람부터요.",
    subtext: "웃는 타이밍이 비슷하면 더 좋고요.",
  },
  {
    id: "date",
    label: "마지막 질문",
    text: "저랑 데이트 하실래요?",
    subtext: "괜찮다면 로그인하고 이어가요.",
  },
];
const introStepDuration = 1900;

const currentIntroStep = computed(() => introSteps[introStepIndex.value]);

const form = reactive({
  email: "",
  password: "",
});

const phoneForm = reactive({
  phone: "",
  code: "",
});

function clearIntroTimers() {
  introTimers.forEach((timer) => window.clearTimeout(timer));
  introTimers.length = 0;
}

function skipIntro() {
  clearIntroTimers();
  showIntro.value = false;
}

function finishIntroWithFade() {
  introLeaving.value = true;
  introTimers.push(window.setTimeout(() => {
    showIntro.value = false;
  }, 1200));
}

onMounted(() => {
  const prefersReducedMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches;
  if (prefersReducedMotion) {
    skipIntro();
    return;
  }

  introSteps.slice(1).forEach((_, index) => {
    introTimers.push(window.setTimeout(() => {
      introStepIndex.value = index + 1;
    }, introStepDuration * (index + 1)));
  });

  introTimers.push(window.setTimeout(
    finishIntroWithFade,
    introStepDuration * introSteps.length + 500,
  ));
});

onUnmounted(() => {
  clearIntroTimers();
});

function moveAfterLogin(data) {
  setAuthTokens(data);

  if (data.user?.status === "ACTIVE") {
    router.push("/home");
    return;
  }

  router.push("/review-pending");
}

async function moveAfterPhoneLogin(data) {
  setAuthTokens(data);

  if (data.user?.status === "ACTIVE") {
    router.push("/home");
    return;
  }

  // 전화번호 자동가입은 프로필 필수 정보가 아직 비어 있을 수 있어 먼저 프로필 보완으로 보낸다.
  const { data: profile } = await api.get("/profile/me");
  const requiredFields = [
    profile.birthDate,
    profile.gender,
    profile.region,
    profile.job,
    profile.mbti,
    profile.personality,
    profile.idealType,
    profile.introduction,
  ];

  if (requiredFields.some((value) => !value || !String(value).trim())) {
    router.push("/profile");
    return;
  }

  router.push("/review-pending");
}

function resolveLoginErrorMessage(error) {
  const status = error.response?.status;
  const serverMessage = error.response?.data?.message;

  if (serverMessage) return serverMessage;
  if (status === 401) return "이메일 또는 비밀번호를 다시 확인해 주세요.";
  if (status === 403) return "현재 로그인할 수 없는 계정 상태입니다. 운영 정책 또는 심사 상태를 확인해 주세요.";
  return "로그인에 실패했습니다. 잠시 후 다시 시도해 주세요.";
}

function normalizePhoneInput() {
  phoneForm.phone = phoneForm.phone.replace(/[^0-9]/g, "");
}

// 로그인 후 계정 상태에 따라 홈 또는 심사 대기 화면으로 보낸다.
const login = async () => {
  loading.value = true;
  errorMessage.value = "";
  noticeMessage.value = "";

  try {
    const { data } = await api.post("/auth/login", form);
    moveAfterLogin(data);
  } catch (error) {
    clearToken();
    const message = resolveLoginErrorMessage(error);
    errorMessage.value = message;
    window.alert(message);

    await nextTick();
    if (!form.email) {
      emailInput.value?.focus();
      return;
    }

    passwordInput.value?.focus();
    passwordInput.value?.select?.();
  } finally {
    loading.value = false;
  }
};

async function requestPhoneCode() {
  normalizePhoneInput();
  errorMessage.value = "";
  noticeMessage.value = "";

  if (!/^010\d{8}$/.test(phoneForm.phone)) {
    errorMessage.value = "전화번호는 01012345678 형식으로 입력해 주세요.";
    await nextTick();
    phoneInput.value?.focus();
    return;
  }

  phoneLoading.value = true;

  try {
    const { data } = await api.post("/auth/phone/request", { phone: phoneForm.phone });
    phoneForm.code = "";
    noticeMessage.value = data.devCode
      ? `개발용 인증번호: ${data.devCode}`
      : data.message || "SMS 인증번호를 발송했습니다.";
    await nextTick();
    codeInput.value?.focus();
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "인증번호 발급에 실패했습니다.";
    window.alert(errorMessage.value);
  } finally {
    phoneLoading.value = false;
  }
}

async function verifyPhoneAndLogin() {
  normalizePhoneInput();
  errorMessage.value = "";
  noticeMessage.value = "";

  if (!/^010\d{8}$/.test(phoneForm.phone)) {
    errorMessage.value = "전화번호는 01012345678 형식으로 입력해 주세요.";
    await nextTick();
    phoneInput.value?.focus();
    return;
  }

  if (!/^\d{6}$/.test(phoneForm.code)) {
    errorMessage.value = "인증번호 6자리를 입력해 주세요.";
    await nextTick();
    codeInput.value?.focus();
    return;
  }

  loading.value = true;

  try {
    const { data } = await api.post("/auth/phone/verify", {
      phone: phoneForm.phone,
      code: phoneForm.code,
    });
    await moveAfterPhoneLogin(data);
  } catch (error) {
    clearToken();
    const serverMessage = error.response?.data?.message;
    errorMessage.value = serverMessage
      ? `${serverMessage} 새 인증번호를 발급한 뒤 다시 입력해 주세요.`
      : "전화번호 인증에 실패했습니다. 새 인증번호를 발급한 뒤 다시 입력해 주세요.";
    window.alert(errorMessage.value);
    await nextTick();
    codeInput.value?.focus();
    codeInput.value?.select?.();
  } finally {
    loading.value = false;
  }
}

</script>

<style scoped>
.auth-page {
  position: relative;
  overflow: hidden;
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: max(16px, env(safe-area-inset-top)) 16px max(20px, env(safe-area-inset-bottom));
  background: transparent;
}

.intro-stage {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 28px;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 12%, rgba(255, 255, 255, 0.92) 0 11%, transparent 12%),
    radial-gradient(circle at 86% 78%, rgba(255, 247, 240, 0.9) 0 14%, transparent 15%),
    linear-gradient(135deg, #ffe3d6 0%, #ffaf95 52%, #f3775f 100%);
}

.intro-stage.leaving {
  animation: introExit 1200ms ease forwards;
}

.intro-stage::before,
.intro-stage::after {
  position: absolute;
  content: "";
  width: min(54vw, 320px);
  height: min(54vw, 320px);
  border: 1px solid rgba(255, 255, 255, 0.56);
  transform: rotate(12deg);
}

.intro-stage::before {
  top: -92px;
  left: -72px;
  background: rgba(255, 255, 255, 0.18);
}

.intro-stage::after {
  right: -96px;
  bottom: -88px;
  background: rgba(96, 39, 27, 0.08);
}

.intro-copy {
  position: relative;
  z-index: 1;
  width: min(100%, 560px);
  color: #331b16;
  text-align: left;
  animation: introScene 1900ms ease both;
}

.intro-kicker {
  width: fit-content;
  margin: 0 0 18px;
  padding: 9px 13px;
  border: 1px solid rgba(72, 35, 27, 0.18);
  border-radius: 8px;
  color: #7b3325;
  background: rgba(255, 255, 255, 0.44);
  font-size: 15px;
  font-weight: 900;
}

.intro-copy h1 {
  margin: 0;
  max-width: 11ch;
  color: #311912;
  font-size: clamp(2.75rem, 12vw, 5.5rem);
  line-height: 0.98;
  word-break: keep-all;
}

.intro-subtext {
  max-width: 32ch;
  margin: 20px 0 0;
  color: #5d3027;
  font-size: clamp(1rem, 4vw, 1.25rem);
  font-weight: 800;
  line-height: 1.55;
  word-break: keep-all;
}

.intro-line {
  display: block;
  width: min(72vw, 360px);
  height: 4px;
  margin-top: 24px;
  border-radius: 999px;
  background: #311912;
  transform-origin: left center;
  animation: introLineIn 640ms cubic-bezier(0.22, 1, 0.36, 1) 240ms both;
}

.intro-skip {
  position: absolute;
  right: 18px;
  bottom: max(18px, env(safe-area-inset-bottom));
  z-index: 2;
  border: 1px solid rgba(67, 31, 24, 0.2);
  border-radius: 8px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.44);
  color: #4d2118;
  font-weight: 900;
  cursor: pointer;
}

.auth-card {
  width: min(100%, 460px);
  min-height: min(860px, calc(100vh - 36px));
  margin: auto 0;
  padding: 28px 22px 24px;
  border-radius: 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.76), rgba(255, 252, 250, 0.96)),
    rgba(255, 252, 250, 0.92);
  border: 1px solid rgba(240, 205, 190, 0.88);
  box-shadow:
    0 26px 50px rgba(98, 49, 34, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(18px);
}

.eyebrow {
  margin: 0 0 10px;
  color: #9d4b32;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #33211d;
  font-size: clamp(1.9rem, 7vw, 2.4rem);
  line-height: 1.08;
}

.description {
  margin: 12px 0 22px;
  color: #6e534c;
  line-height: 1.62;
}

.tab-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  margin-bottom: 18px;
  padding: 6px;
  border-radius: 18px;
  background: #fff2eb;
}

.tab-button {
  border: none;
  border-radius: 14px;
  padding: 11px 12px;
  background: transparent;
  color: #8a5b4c;
  font-weight: 800;
  cursor: pointer;
}

.tab-button.active {
  background: #ffffff;
  color: #9d4b32;
  box-shadow: 0 8px 18px rgba(98, 49, 34, 0.1);
}

.auth-form {
  display: grid;
  gap: 14px;
}

label {
  display: grid;
  gap: 8px;
  color: #503731;
  font-weight: 600;
}

input {
  border: 1px solid #e8c7bb;
  border-radius: 18px;
  padding: 15px 16px;
  font-size: 16px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 1px 2px rgba(80, 55, 49, 0.04);
}

.primary-button,
.ghost-button {
  width: 100%;
  border-radius: 18px;
  padding: 16px;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.primary-button {
  border: none;
  margin-top: 10px;
  background: linear-gradient(135deg, #d45d38 0%, #ea8f64 100%);
  color: #fff;
  box-shadow: 0 16px 28px rgba(212, 93, 56, 0.26);
}

.ghost-button {
  border: 1px solid rgba(207, 150, 128, 0.42);
  background: #fff8f3;
  color: #8b503e;
}

.message {
  margin-top: 16px;
  line-height: 1.55;
}

.error {
  color: #b72f2f;
}

.notice {
  color: #276749;
}

.primary-button:disabled,
.ghost-button:disabled {
  cursor: not-allowed;
  opacity: 0.68;
}

.helper-row {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.helper-row a {
  color: #a54123;
  font-weight: 700;
}

.policy-box {
  margin-top: 22px;
  padding: 16px;
  border-radius: 22px;
  background: linear-gradient(180deg, #fff7f1 0%, #fff3ec 100%);
  border: 1px solid #f0d0c3;
  color: #6a4a41;
}

.policy-box strong {
  display: block;
  margin-bottom: 8px;
  color: #3d251f;
}

.policy-box p {
  margin: 0;
  line-height: 1.6;
}

@media (min-width: 768px) {
  .auth-page {
    padding: 28px;
  }

  .auth-card {
    min-height: auto;
    padding: 36px 34px 28px;
  }
}

@media (max-width: 420px) {
  .auth-card {
    border-radius: 28px;
    padding: 24px 18px 20px;
  }

  .helper-row {
    flex-direction: column;
  }
}

@media (prefers-reduced-motion: reduce) {
  .intro-stage,
  .intro-copy,
  .intro-kicker,
  .intro-copy h1,
  .intro-line {
    animation: none;
  }
}

@keyframes introScene {
  0% {
    opacity: 0;
    filter: blur(14px);
    transform: translateY(18px) scale(0.985);
  }
  18% {
    opacity: 1;
    filter: blur(0);
    transform: translateY(0) scale(1);
  }
  72% {
    opacity: 1;
    filter: blur(0);
    transform: translateY(0) scale(1);
  }
  100% {
    opacity: 0;
    filter: blur(14px);
    transform: translateY(-16px) scale(1.01);
  }
}

@keyframes introLineIn {
  from {
    transform: scaleX(0);
  }
  to {
    transform: scaleX(1);
  }
}

@keyframes introExit {
  0% {
    opacity: 1;
    filter: blur(0);
  }
  100% {
    opacity: 0;
    filter: blur(10px);
    visibility: hidden;
  }
}
</style>
