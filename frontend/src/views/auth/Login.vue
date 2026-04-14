<template>
  <section class="auth-page">
    <div class="auth-card">
      <p class="eyebrow">Dating App</p>
      <h1>로그인</h1>
      <p class="description">이메일 또는 개발용 전화번호 인증으로 서비스를 시작해 보세요.</p>

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
          {{ phoneLoading ? '인증번호 발급 중...' : '개발용 인증번호 발급' }}
        </button>

        <label>
          <span>인증번호</span>
          <input ref="codeInput" v-model.trim="phoneForm.code" inputmode="numeric" maxlength="6" placeholder="서버 로그의 6자리 번호" required />
        </label>

        <button class="primary-button" :disabled="loading">
          {{ loading ? '확인 중...' : '전화번호로 자동 가입/로그인' }}
        </button>
      </form>

      <p v-if="noticeMessage" class="message notice">{{ noticeMessage }}</p>
      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="helper-row">
        <RouterLink to="/reset-password">비밀번호를 잊으셨나요?</RouterLink>
        <RouterLink to="/signup">이메일 회원가입</RouterLink>
      </div>

      <div class="policy-box">
        <strong>개발용 전화 인증 안내</strong>
        <p>실제 SMS 비용이 들지 않도록 인증번호는 백엔드 콘솔 로그에만 출력됩니다. `bootRun` 터미널에서 [DEV PHONE AUTH] 로그를 확인해 주세요.</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { nextTick, reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken, setToken } from "../../utils/auth";

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

const form = reactive({
  email: "",
  password: "",
});

const phoneForm = reactive({
  phone: "",
  code: "",
});

function moveAfterLogin(data) {
  setToken(data.token);

  if (data.user?.status === "ACTIVE") {
    router.push("/home");
    return;
  }

  router.push("/review-pending");
}

async function moveAfterPhoneLogin(data) {
  setToken(data.token);

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
    await moveAfterPhoneLogin(data);
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
    noticeMessage.value = data.message || "인증번호를 서버 로그에 기록했습니다.";
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
    moveAfterLogin(data);
  } catch (error) {
    clearToken();
    errorMessage.value = error.response?.data?.message || "전화번호 인증에 실패했습니다.";
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
  min-height: 100vh;
  display: flex;
  align-items: stretch;
  justify-content: center;
  padding: max(16px, env(safe-area-inset-top)) 16px max(20px, env(safe-area-inset-bottom));
  background: transparent;
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
</style>
