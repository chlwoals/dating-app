<template>
  <section class="auth-page">
    <div class="auth-card">
      <p class="eyebrow">Create Account</p>
      <h1>회원가입</h1>
      <p class="description">
        기본 정보 입력 후 사진 심사 단계로 이동합니다. 가입 승인은 운영자 심사가
        끝난 뒤 확정됩니다.
      </p>

      <form class="auth-form" @submit.prevent="signup">
        <div class="section-title">기본 계정 정보</div>

        <label>
          <span>닉네임</span>
          <input v-model.trim="form.nickname" type="text" placeholder="서비스에 표시될 이름" required />
        </label>

        <label>
          <span>이메일</span>
          <input v-model.trim="form.email" type="email" placeholder="you@example.com" required />
        </label>

        <label>
          <span>비밀번호</span>
          <input v-model="form.password" type="password" placeholder="8자 이상 입력" required minlength="8" />
        </label>

        <div class="section-title">기본 프로필 정보</div>

        <label>
          <span>생년월일</span>
          <input v-model="form.birthDate" type="date" required />
        </label>

        <label>
          <span>성별</span>
          <select v-model="form.gender" required>
            <option value="MALE">남성</option>
            <option value="FEMALE">여성</option>
          </select>
        </label>

        <label>
          <span>거주 지역</span>
          <input v-model.trim="form.region" type="text" placeholder="예: 서울 강남구" required />
        </label>

        <label>
          <span>직업</span>
          <input v-model.trim="form.job" type="text" placeholder="예: 개발자" />
        </label>

        <label>
          <span>MBTI</span>
          <select v-model="form.mbti">
            <option value="">선택 안 함</option>
            <option v-for="item in mbtiOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>

        <label>
          <span>흡연 여부</span>
          <select v-model="form.smokingStatus" required>
            <option value="NON_SMOKER">비흡연</option>
            <option value="OCCASIONAL">가끔 흡연</option>
            <option value="SMOKER">흡연</option>
          </select>
        </label>

        <label>
          <span>음주 여부</span>
          <select v-model="form.drinkingStatus" required>
            <option value="NONE">안 함</option>
            <option value="SOMETIMES">가끔</option>
            <option value="OFTEN">자주</option>
          </select>
        </label>

        <label>
          <span>종교</span>
          <select v-model="form.religion" required>
            <option value="NONE">없음</option>
            <option value="CHRISTIAN">기독교</option>
            <option value="CATHOLIC">천주교</option>
            <option value="BUDDHIST">불교</option>
            <option value="OTHER">기타</option>
          </select>
        </label>

        <label>
          <span>내 성격</span>
          <textarea v-model.trim="form.personality" rows="3" placeholder="성격을 간단히 적어주세요."></textarea>
        </label>

        <label>
          <span>이상형</span>
          <textarea v-model.trim="form.idealType" rows="3" placeholder="어떤 분을 만나고 싶은지 적어주세요."></textarea>
        </label>

        <label>
          <span>자기소개</span>
          <textarea v-model.trim="form.introduction" rows="4" placeholder="첫인상에 보일 자기소개를 적어주세요."></textarea>
        </label>

        <label class="checkbox-row">
          <input v-model="form.agreedToTerms" type="checkbox" />
          <span>만 19세 이상이며 서비스 이용약관과 개인정보 처리에 동의합니다.</span>
        </label>

        <button class="primary-button" :disabled="loading">
          {{ loading ? "다음 단계 준비 중..." : "다음: 사진 업로드" }}
        </button>
      </form>

      <p v-if="errorMessage" class="message error">{{ errorMessage }}</p>

      <div class="policy-box">
        <strong>가입 절차 안내</strong>
        <p>1. 기본 정보 입력</p>
        <p>2. 프로필 사진 업로드</p>
        <p>3. 운영자 사진 심사 대기</p>
        <p>4. 승인 알림 후 가입 완료</p>
      </div>

      <p class="helper-text">
        이미 계정이 있나요?
        <RouterLink to="/">로그인</RouterLink>
      </p>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from "vue";
import { RouterLink, useRouter } from "vue-router";
import api from "../api/api";
import { resetSignupApprovalNotice, setToken } from "../utils/auth";

const mbtiOptions = [
  "INTJ", "INTP", "ENTJ", "ENTP",
  "INFJ", "INFP", "ENFJ", "ENFP",
  "ISTJ", "ISFJ", "ESTJ", "ESFJ",
  "ISTP", "ISFP", "ESTP", "ESFP",
];

const router = useRouter();
const loading = ref(false);
const errorMessage = ref("");
const form = reactive({
  nickname: "",
  email: "",
  password: "",
  birthDate: "",
  gender: "MALE",
  region: "",
  job: "",
  mbti: "",
  personality: "",
  idealType: "",
  introduction: "",
  smokingStatus: "NON_SMOKER",
  drinkingStatus: "NONE",
  religion: "NONE",
  agreedToTerms: false,
});

// 가입 정보 저장이 끝나면 바로 가입 완료가 아니라 사진 심사 단계로 보낸다.
const signup = async () => {
  loading.value = true;
  errorMessage.value = "";

  try {
    const { data } = await api.post("/auth/signup", form);
    resetSignupApprovalNotice();
    setToken(data.token);
    router.push("/review-pending");
  } catch (error) {
    errorMessage.value = error.response?.data?.message || "회원가입에 실패했습니다. 입력값을 다시 확인해주세요.";
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #fff7e7 0%, #ffe0b8 45%, #ffd2b0 100%);
}

.auth-card {
  width: min(100%, 560px);
  padding: 32px;
  border-radius: 24px;
  background: rgba(255, 252, 247, 0.94);
  box-shadow: 0 18px 45px rgba(109, 70, 36, 0.14);
}

.eyebrow {
  margin: 0 0 10px;
  color: #b35a28;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #35231a;
}

.description {
  margin: 12px 0 24px;
  color: #6a5246;
  line-height: 1.5;
}

.section-title {
  margin-top: 4px;
  color: #8d4b27;
  font-size: 14px;
  font-weight: 800;
}

.auth-form {
  display: grid;
  gap: 16px;
}

label {
  display: grid;
  gap: 8px;
  color: #4d382d;
  font-weight: 600;
}

input,
select,
textarea {
  border: 1px solid #ebc7a9;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  background: #fff;
  font-family: inherit;
}

textarea {
  resize: vertical;
}

.checkbox-row {
  grid-template-columns: auto 1fr;
  align-items: start;
  gap: 12px;
}

.checkbox-row input {
  margin-top: 3px;
}

.primary-button {
  margin-top: 8px;
  width: 100%;
  border: none;
  border-radius: 14px;
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  background: #cd6d2d;
  color: #fff;
}

.message {
  margin-top: 16px;
}

.error {
  color: #b72f2f;
}

.policy-box {
  margin-top: 20px;
  padding: 16px;
  border-radius: 16px;
  background: #fff7ed;
  border: 1px solid #f0d8c4;
  color: #6a5246;
}

.policy-box strong {
  display: block;
  margin-bottom: 8px;
  color: #3b281f;
}

.policy-box p {
  margin: 0;
  line-height: 1.6;
}

.helper-text {
  margin-top: 20px;
  color: #6a5246;
}

.helper-text a {
  color: #a3461a;
  font-weight: 700;
}
</style>
