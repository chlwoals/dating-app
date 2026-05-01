<template>
  <div
    class="chatbot-anchor"
    :style="anchorStyle"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
  >
    <transition name="panel-fade">
      <section v-if="open" class="chatbot-panel">
        <header class="chatbot-head" @pointerdown="onPointerDown">
          <div>
            <p class="eyebrow">Support Bot</p>
            <h3>실시간 문의 도우미</h3>
          </div>
          <button class="close-button" type="button" @click="open = false">닫기</button>
        </header>

        <div ref="messageBox" class="chatbot-messages">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message-bubble"
            :class="message.role"
          >
            <p>{{ message.text }}</p>
          </article>
        </div>

        <div class="quick-actions">
          <button
            v-for="item in quickPrompts"
            :key="item"
            class="chip-button"
            type="button"
            @click="askQuick(item)"
          >
            {{ item }}
          </button>
          <button class="chip-button accent" type="button" @click="goToSupport">고객센터 이동</button>
        </div>

        <form class="chatbot-form" @submit.prevent="submitQuestion">
          <input
            v-model.trim="draft"
            type="text"
            maxlength="120"
            placeholder="궁금한 점을 입력해 주세요."
          />
          <button class="send-button" :disabled="!draft">보내기</button>
        </form>

        <div class="guide-box">
          <strong>해결되지 않는 문의가 있나요?</strong>
          <p>
            고객센터로 이동한 뒤 문의하기에 있는 메일 주소로 문의해 주세요.
            계정 이메일, 닉네임, 문제 화면과 상황을 함께 적어주시면 더 빠르게 확인할 수 있습니다.
          </p>
          <button class="guide-button" type="button" @click="goToSupport">메일 문의 안내 보기</button>
        </div>
      </section>
    </transition>

    <button
      class="chatbot-fab"
      type="button"
      @pointerdown="onPointerDown"
      @click="toggleChatbot"
    >
      <svg class="fab-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M4.5 5.25h15a1.5 1.5 0 0 1 1.5 1.5v8.25a1.5 1.5 0 0 1-1.5 1.5h-6.03l-4.68 3.12a.75.75 0 0 1-1.17-.62V16.5H4.5A1.5 1.5 0 0 1 3 15V6.75a1.5 1.5 0 0 1 1.5-1.5Zm2.25 4.13a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Zm5.25 0a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Zm5.25 0a.88.88 0 1 0 0 1.75.88.88 0 0 0 0-1.75Z"
        />
      </svg>
      <span>문의</span>
    </button>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const positionStorageKey = "support-chatbot-position";
const quickPrompts = [
  "사진 심사는 얼마나 걸리나요?",
  "전화번호 인증은 어떻게 하나요?",
  "프로필 수정은 어디서 하나요?",
  "로그인이 안 돼요.",
];

const open = ref(false);
const draft = ref("");
const messageBox = ref(null);
const messages = ref([
  {
    id: 1,
    role: "bot",
    text: "안녕하세요. 실시간 문의 도우미예요. 사진 심사, 전화번호 인증, 프로필 수정, 로그인 문제처럼 자주 묻는 질문을 먼저 빠르게 안내해드릴게요.",
  },
]);

const position = reactive({
  x: 0,
  y: 0,
});

const dragState = reactive({
  dragging: false,
  moved: false,
  startX: 0,
  startY: 0,
  originX: 0,
  originY: 0,
});

const anchorStyle = ref({});

function clampPosition() {
  const width = window.innerWidth;
  const height = window.innerHeight;
  const panelWidth = open.value ? 360 : 84;
  const panelHeight = open.value ? 560 : 84;
  position.x = Math.min(Math.max(12, position.x), Math.max(12, width - panelWidth - 12));
  position.y = Math.min(Math.max(12, position.y), Math.max(12, height - panelHeight - 12));
  anchorStyle.value = {
    left: `${position.x}px`,
    top: `${position.y}px`,
  };
}

function savePosition() {
  localStorage.setItem(positionStorageKey, JSON.stringify({ x: position.x, y: position.y }));
}

function loadPosition() {
  const saved = localStorage.getItem(positionStorageKey);
  if (saved) {
    try {
      const parsed = JSON.parse(saved);
      position.x = parsed.x;
      position.y = parsed.y;
      clampPosition();
      return;
    } catch {
      localStorage.removeItem(positionStorageKey);
    }
  }

  position.x = Math.max(12, window.innerWidth - 96);
  position.y = Math.max(12, window.innerHeight - 180);
  clampPosition();
}

function onPointerDown(event) {
  dragState.dragging = true;
  dragState.moved = false;
  dragState.startX = event.clientX;
  dragState.startY = event.clientY;
  dragState.originX = position.x;
  dragState.originY = position.y;
  event.currentTarget.setPointerCapture?.(event.pointerId);
}

function onPointerMove(event) {
  if (!dragState.dragging) {
    return;
  }

  const deltaX = event.clientX - dragState.startX;
  const deltaY = event.clientY - dragState.startY;

  if (Math.abs(deltaX) > 4 || Math.abs(deltaY) > 4) {
    dragState.moved = true;
  }

  position.x = dragState.originX + deltaX;
  position.y = dragState.originY + deltaY;
  clampPosition();
}

function onPointerUp() {
  if (!dragState.dragging) {
    return;
  }

  dragState.dragging = false;
  savePosition();
}

function toggleChatbot() {
  if (dragState.moved) {
    return;
  }
  open.value = !open.value;
}

function pushMessage(role, text) {
  messages.value.push({
    id: Date.now() + Math.random(),
    role,
    text,
  });
}

function resolveBotReply(question) {
  const normalized = question.toLowerCase();

  if (normalized.includes("사진") || normalized.includes("심사")) {
    return "사진 심사는 관리자 검토 후 승인 또는 반려로 처리됩니다. 현재 앱에서는 사진 2장 이상 등록 후 사진 심사 화면에서 신청하면 되고, 반려 시 보완 후 다시 재심사할 수 있어요.";
  }

  if (normalized.includes("전화") || normalized.includes("번호") || normalized.includes("인증")) {
    return "전화번호 인증은 프로필 화면 상단의 전화번호 인증 버튼에서 진행할 수 있어요. 인증번호 요청 후 개발 환경에서는 서버 로그의 [DEV SMS AUTH] 항목에서 코드를 확인하고 입력하면 계정에 저장됩니다.";
  }

  if (normalized.includes("프로필") || normalized.includes("수정") || normalized.includes("mbti") || normalized.includes("지역")) {
    return "프로필 수정은 프로필 화면 상단의 프로필 수정 버튼에서 할 수 있어요. 거주 지역은 시/도와 구/군을 선택하고, MBTI도 선택형으로 입력하도록 연결되어 있습니다.";
  }

  if (normalized.includes("로그인") || normalized.includes("비밀번호") || normalized.includes("회원가입")) {
    return "로그인 문제가 있으면 이메일 형식, 비밀번호 길이, 가입 이력을 먼저 확인해 주세요. 회원가입 이력이 없으면 가입 안내 문구가 나오고, 비밀번호가 맞지 않으면 다시 확인해 달라는 문구가 표시되도록 되어 있어요.";
  }

  if (normalized.includes("사기") || normalized.includes("스캠") || normalized.includes("신고")) {
    return "스캠이나 의심 행위는 관리자 모니터링과 신고 처리 대상으로 관리됩니다. 상대방이 금전 요구, 외부 메신저 유도, 신분 위장처럼 보이면 바로 대화를 중단하고 고객센터 안내에 따라 문의해 주세요.";
  }

  return "이 문의는 개인 계정 상태나 운영 확인이 함께 필요할 수 있어요. 고객센터로 이동해서 문의하기에 있는 메일 주소로 문의해 주세요.";
}

async function scrollToBottom() {
  await nextTick();
  messageBox.value?.scrollTo({
    top: messageBox.value.scrollHeight,
    behavior: "smooth",
  });
}

async function askQuick(question) {
  draft.value = question;
  await submitQuestion();
}

async function submitQuestion() {
  if (!draft.value) {
    return;
  }

  const question = draft.value;
  draft.value = "";
  pushMessage("user", question);
  pushMessage("bot", resolveBotReply(question));
  await scrollToBottom();
}

function goToSupport() {
  router.push("/support");
}

watch(open, async () => {
  clampPosition();
  if (open.value) {
    await scrollToBottom();
  }
});

function handleResize() {
  clampPosition();
}

onMounted(() => {
  loadPosition();
  window.addEventListener("resize", handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize);
});
</script>

<style scoped>
.chatbot-anchor {
  position: fixed;
  z-index: 40;
  touch-action: none;
}

.chatbot-panel {
  width: min(360px, calc(100vw - 24px));
  margin-bottom: 12px;
  padding: 18px;
  border-radius: 28px;
  background: rgba(255, 252, 249, 0.97);
  border: 1px solid rgba(239, 208, 193, 0.9);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.18);
  backdrop-filter: blur(12px);
}

.chatbot-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
  cursor: grab;
}

.eyebrow {
  margin: 0 0 8px;
  color: #af5f42;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.chatbot-head h3 {
  margin: 0;
  color: #31211d;
  font-size: 20px;
}

.close-button {
  border: none;
  background: transparent;
  color: #8b675b;
  font-weight: 700;
  cursor: pointer;
}

.chatbot-messages {
  display: grid;
  gap: 10px;
  max-height: 220px;
  overflow-y: auto;
  padding-right: 4px;
  margin-bottom: 14px;
}

.message-bubble {
  max-width: 88%;
  padding: 12px 14px;
  border-radius: 18px;
  line-height: 1.6;
  font-size: 14px;
}

.message-bubble p {
  margin: 0;
}

.message-bubble.bot {
  background: rgba(255, 244, 236, 0.92);
  color: #5d4640;
}

.message-bubble.user {
  margin-left: auto;
  background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%);
  color: #fff;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.chip-button {
  border: 1px solid rgba(207, 150, 128, 0.36);
  background: rgba(255, 248, 243, 0.82);
  color: #6a4137;
  border-radius: 999px;
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.chip-button.accent {
  background: #f3dfd7;
}

.chatbot-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-bottom: 14px;
}

.chatbot-form input {
  border: 1px solid #e5c2b2;
  border-radius: 16px;
  padding: 12px 14px;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.95);
  font-family: inherit;
}

.send-button,
.guide-button {
  border: none;
  border-radius: 16px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.send-button:disabled {
  opacity: 0.5;
  cursor: default;
}

.guide-box {
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 247, 242, 0.9);
  color: #5d4640;
}

.guide-box strong {
  display: block;
  margin-bottom: 8px;
  color: #31211d;
}

.guide-box p {
  margin: 0 0 12px;
  line-height: 1.7;
}

.chatbot-fab {
  display: grid;
  place-items: center;
  gap: 6px;
  width: 72px;
  height: 72px;
  border: none;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(234, 143, 100, 0.96), rgba(213, 111, 78, 0.96));
  color: #fff8f5;
  box-shadow: 0 18px 40px rgba(109, 57, 41, 0.22);
  cursor: pointer;
}

.fab-icon {
  width: 26px;
  height: 26px;
  fill: currentColor;
}

.chatbot-fab span {
  font-size: 12px;
  font-weight: 800;
}

.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 760px) {
  .chatbot-panel {
    width: min(340px, calc(100vw - 24px));
  }
}
</style>
