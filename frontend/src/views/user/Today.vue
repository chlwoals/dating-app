<template>
  <section class="today-page">
    <div v-if="profile && userSummary" class="today-shell">
      <header class="hero-card">
        <div class="hero-copy">
          <p class="eyebrow">Today Match</p>
          <span class="status-pill" :class="statusClass">{{ statusLabel }}</span>
          <h1>{{ heroTitle }}</h1>
          <p class="description">{{ heroDescription }}</p>
        </div>

        <div class="hero-stats">
          <article class="stat-card">
            <span>프로필 완성도</span>
            <strong>{{ profileCompletion }}%</strong>
          </article>
          <article class="stat-card">
            <span>등록 사진</span>
            <strong>{{ images.length }}장</strong>
          </article>
          <article class="stat-card">
            <span>계정 방식</span>
            <strong>{{ providerLabel }}</strong>
          </article>
        </div>
      </header>

      <section class="section-card">
        <div class="section-head">
          <div>
            <p class="section-label">Discover</p>
            <h2>오늘의 인연 추천</h2>
          </div>
          <span class="section-caption">추천 기능은 데모용 카드로 먼저 연결되어 있습니다.</span>
        </div>

        <div class="discover-grid">
          <article v-for="card in recommendationCards" :key="card.id" class="discover-card">
            <div class="discover-photo" :style="{ background: card.background }">
              <div class="discover-overlay">
                <span>{{ card.badge }}</span>
                <strong>{{ card.name }}, {{ card.age }}</strong>
              </div>
            </div>

            <div class="discover-body">
              <div class="discover-meta">
                <span>{{ card.region }}</span>
                <span>{{ card.job }}</span>
              </div>
              <p>{{ card.introduction }}</p>
              <div class="tag-row">
                <span v-for="tag in card.tags" :key="tag" class="tag">{{ tag }}</span>
              </div>

              <div class="action-row">
                <button class="action-button pass" @click="markReaction(card.id, 'pass')">패스</button>
                <button class="action-button detail" @click="selectedCard = card">상세 보기</button>
                <button class="action-button like" @click="markReaction(card.id, 'like')">좋아요</button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section class="section-card">
        <div class="section-head compact">
          <div>
            <p class="section-label">Preview</p>
            <h2>반응 현황</h2>
          </div>
        </div>

        <div class="summary-grid">
          <article class="summary-card accent">
            <span class="summary-badge">오늘의 반응</span>
            <strong>{{ likedCount }}명에게 좋아요를 보냈어요</strong>
            <p>실제 매칭 기능이 붙기 전까지는 데모 반응 상태로 보여드립니다.</p>
          </article>

          <article class="summary-card">
            <ul class="queue-list">
              <li>
                <strong>{{ likedCount }}명</strong>
                <span>좋아요 보낸 카드</span>
              </li>
              <li>
                <strong>{{ passedCount }}명</strong>
                <span>패스한 카드</span>
              </li>
              <li>
                <strong>{{ likedCount + passedCount }}</strong>
                <span>오늘 반응 수</span>
              </li>
            </ul>
          </article>
        </div>
      </section>
    </div>

    <div v-else class="loading-shell">
      <div class="loading-card">
        <p class="eyebrow">Loading</p>
        <h2>오늘의 인연을 불러오는 중이에요</h2>
        <p>추천 카드와 프로필 요약을 준비하고 있습니다.</p>
      </div>
    </div>

    <div v-if="selectedCard" class="modal-backdrop" @click.self="selectedCard = null">
      <div class="modal-card">
        <div class="modal-photo" :style="{ background: selectedCard.background }">
          <span>{{ selectedCard.badge }}</span>
          <strong>{{ selectedCard.name }}, {{ selectedCard.age }}</strong>
        </div>
        <div class="modal-body">
          <h3>{{ selectedCard.name }}의 카드</h3>
          <p>{{ selectedCard.introduction }}</p>
          <div class="tag-row spacious">
            <span v-for="tag in selectedCard.tags" :key="tag" class="tag soft">{{ tag }}</span>
          </div>
          <div class="modal-list">
            <div>
              <dt>활동 지역</dt>
              <dd>{{ selectedCard.region }}</dd>
            </div>
            <div>
              <dt>직업</dt>
              <dd>{{ selectedCard.job }}</dd>
            </div>
            <div>
              <dt>추천 이유</dt>
              <dd>{{ selectedCard.reason }}</dd>
            </div>
          </div>
          <div class="action-row modal-actions">
            <button class="action-button pass" @click="markReaction(selectedCard.id, 'pass'); selectedCard = null">패스</button>
            <button class="action-button like" @click="markReaction(selectedCard.id, 'like'); selectedCard = null">좋아요</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import api from "../../api/api";
import { clearToken } from "../../utils/auth";

const router = useRouter();
const userSummary = ref(null);
const profile = ref(null);
const images = ref([]);
const selectedCard = ref(null);
const reactions = ref({});

const likedCount = computed(() => Object.values(reactions.value).filter((value) => value === "like").length);
const passedCount = computed(() => Object.values(reactions.value).filter((value) => value === "pass").length);

const profileCompletion = computed(() => {
  if (!profile.value) return 0;
  const fields = [profile.value.job, profile.value.mbti, profile.value.personality, profile.value.idealType, profile.value.introduction];
  const completed = fields.filter((value) => value && String(value).trim()).length;
  return Math.round((completed / fields.length) * 100);
});

const providerLabel = computed(() => {
  const provider = profile.value?.provider;
  if (provider === "BOTH") return "이메일 + Google";
  if (provider === "GOOGLE") return "Google";
  return "이메일";
});

const statusLabel = computed(() => {
  const status = profile.value?.status;
  if (status === "ACTIVE") return "이용 가능";
  if (status === "PENDING_REVIEW") return "심사 진행 중";
  if (status === "REJECTED") return "보완 필요";
  if (status === "SUSPENDED") return "이용 제한";
  return "상태 확인 중";
});

const statusClass = computed(() => {
  const status = profile.value?.status;
  if (status === "ACTIVE") return "status-active";
  if (status === "PENDING_REVIEW") return "status-pending";
  if (status === "REJECTED") return "status-rejected";
  if (status === "SUSPENDED") return "status-suspended";
  return "";
});

const heroTitle = computed(() => {
  if (!profile.value) return "오늘의 인연을 준비하고 있어요";
  return `${profile.value.region || '내 주변'}에서 어울리는 인연을 추천드릴게요`;
});

const heroDescription = computed(() => {
  if (!profile.value) return "프로필을 바탕으로 추천 카드를 구성하고 있습니다.";
  return `${profile.value.mbti ? `${profile.value.mbti} 성향과 ` : ''}${profile.value.introduction ? '자기소개를 기준으로 ' : ''}오늘의 추천 카드를 준비했어요.`;
});

const recommendationCards = computed(() => {
  if (!profile.value) return [];
  const region = profile.value.region || "서울";
  const ideal = profile.value.idealType || "대화가 잘 통하는 사람";
  const mbti = profile.value.mbti || "감성파";

  return [
    {
      id: 1,
      name: "서윤",
      age: 29,
      region,
      job: "브랜드 디자이너",
      badge: "오늘 잘 맞는 카드",
      introduction: `${ideal}을 중요하게 보는 분위기를 반영한 차분한 대화형 추천 카드예요.`,
      tags: [mbti, "주말 데이트", "카페 취향"],
      reason: "이상형 문장과 자기소개 결이 비슷해서 첫 카드로 배치했어요.",
      background: "linear-gradient(135deg, #f5b490 0%, #f8d6be 100%)",
    },
    {
      id: 2,
      name: "민지",
      age: 31,
      region: `${region.split(" ")[0]} 근교`,
      job: "콘텐츠 마케터",
      badge: "대화 템포가 잘 맞아요",
      introduction: "자기소개 길이와 생활 패턴 기준으로 부담 없이 대화가 가능한 카드로 준비했어요.",
      tags: ["가벼운 한잔", "전시회", "사진 취향"],
      reason: "라이프스타일 정보가 현재 프로필과 비교적 잘 맞는 카드예요.",
      background: "linear-gradient(135deg, #ef8c74 0%, #f3c3b2 100%)",
    },
    {
      id: 3,
      name: "지우",
      age: 27,
      region: "같은 생활권",
      job: "서비스 기획자",
      badge: "프로필 완성도 기반 추천",
      introduction: "직업, MBTI, 자기소개가 채워질수록 이런 메인 카드가 실제 추천 결과로 더 정교해질 수 있어요.",
      tags: ["비흡연", "무교", "깊은 대화"],
      reason: "현재 프로필 정보가 반영되면 어떤 유형의 추천이 보이는지 보여주는 카드입니다.",
      background: "linear-gradient(135deg, #d9826b 0%, #f2b8a3 100%)",
    },
  ];
});

async function fetchProfileWithFallback() {
  try {
    const { data } = await api.get("/profile/me");
    return data;
  } catch (error) {
    if (error.response?.status !== 404) {
      throw error;
    }

    const { data } = await api.get("/user/me");
    return {
      ...data,
      region: "",
      job: "",
      mbti: "",
      personality: "",
      idealType: "",
      introduction: "",
      smokingStatus: "NON_SMOKER",
      drinkingStatus: "NONE",
      religion: "NONE",
    };
  }
}

onMounted(async () => {
  try {
    const [{ data: me }, myProfile, { data: myImages }] = await Promise.all([
      api.get("/user/me"),
      fetchProfileWithFallback(),
      api.get("/profile-images/me"),
    ]);

    if (me.status === "SUSPENDED") {
      window.alert("운영 정책상 이용이 제한되었습니다.");
      clearToken();
      router.replace("/");
      return;
    }

    if (me.status !== "ACTIVE") {
      router.replace("/review-pending");
      return;
    }

    userSummary.value = me;
    profile.value = myProfile;
    images.value = myImages;
  } catch (error) {
    clearToken();
    router.replace("/");
  }
});

function markReaction(cardId, type) {
  reactions.value = {
    ...reactions.value,
    [cardId]: type,
  };
}
</script>

<style scoped>
.today-page { min-height: 100vh; padding: max(12px, env(safe-area-inset-top)) 12px 24px; }
.today-shell { width: min(100%, 480px); margin: 0 auto; }
.hero-card, .section-card, .summary-card, .discover-card, .modal-card {
  border-radius: 30px; background: rgba(255, 252, 249, 0.9); border: 1px solid rgba(239, 208, 193, 0.78); box-shadow: 0 22px 44px rgba(109, 57, 41, 0.08);
}
.hero-card, .section-card { padding: 20px 16px; }
.hero-card { display: grid; gap: 16px; }
.eyebrow, .section-label { margin: 0 0 10px; color: #af5f42; font-size: 11px; font-weight: 800; letter-spacing: 0.16em; text-transform: uppercase; }
.status-pill { display: inline-flex; width: fit-content; padding: 8px 12px; border-radius: 999px; font-size: 12px; font-weight: 800; }
.status-active { background: #e8f7ee; color: #1d7a46; }
.status-pending { background: #fff3df; color: #9b6112; }
.status-rejected { background: #ffe8e1; color: #b34b36; }
.status-suspended { background: #f1e5df; color: #7f5448; }
.description { color: #6f564d; line-height: 1.7; }
.hero-stats, .discover-grid, .summary-grid { display: grid; gap: 12px; }
.hero-stats { grid-template-columns: repeat(3, minmax(0, 1fr)); }
.stat-card, .summary-card { padding: 16px; border-radius: 22px; background: rgba(255,255,255,0.88); }
.stat-card span, .summary-badge, .section-caption { color: #8b675b; font-size: 12px; }
.stat-card strong { display: block; margin-top: 8px; font-size: 1.2rem; color: #2f211d; }
.section-head, .discover-meta, .action-row, .modal-list { display: flex; gap: 10px; justify-content: space-between; align-items: center; }
.section-head { margin-bottom: 14px; }
.section-head.compact { margin-bottom: 10px; }
.discover-grid { grid-template-columns: 1fr; }
.discover-card { overflow: hidden; }
.discover-photo, .modal-photo { min-height: 180px; padding: 14px; display: flex; align-items: end; }
.discover-overlay, .modal-photo { color: #fff8f4; font-weight: 700; }
.discover-body, .modal-body { padding: 16px; }
.discover-meta { justify-content: flex-start; flex-wrap: wrap; color: #8b675b; font-size: 13px; }
.tag-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.tag { padding: 7px 10px; border-radius: 999px; background: #fff2ea; color: #8e5b4c; font-size: 12px; }
.tag.soft { background: #fff7f2; }
.action-button { border: none; border-radius: 999px; padding: 12px 16px; font-weight: 700; cursor: pointer; }
.action-button.pass { background: #f3e2db; color: #7a5247; }
.action-button.detail { background: #fff3ea; color: #9f633f; }
.action-button.like { background: linear-gradient(135deg, #d56f4e 0%, #ed9f79 100%); color: #fff; }
.queue-list { list-style: none; padding: 0; margin: 0; display: grid; gap: 12px; }
.queue-list li { display: flex; justify-content: space-between; gap: 12px; color: #6f564d; }
.loading-shell { min-height: 100vh; display: grid; place-items: center; padding: 20px; }
.loading-card { width: min(100%, 420px); padding: 28px 22px; border-radius: 28px; background: rgba(255,252,249,0.9); border: 1px solid rgba(239,208,193,0.78); box-shadow: 0 24px 48px rgba(109,57,41,0.1); text-align:center; }
.modal-backdrop { position: fixed; inset: 0; display: grid; place-items: center; padding: 20px; background: rgba(36, 20, 16, 0.36); }
.modal-card { width: min(100%, 420px); overflow: hidden; }
.modal-list { display: grid; gap: 10px; margin-top: 16px; }
.modal-actions { margin-top: 18px; }
@media (max-width: 720px) { .hero-stats { grid-template-columns: 1fr; } .section-head, .action-row { flex-direction: column; align-items: flex-start; } .action-row { width: 100%; } .action-button { width: 100%; } }
</style>
