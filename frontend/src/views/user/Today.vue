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
            <span>연결 계정</span>
            <strong>{{ providerLabel }}</strong>
          </article>
        </div>
      </header>

      <section v-if="showWatchBanner" class="warning-banner">
        <strong>안전 알림</strong>
        <p>프로필 문구에 주의가 필요한 표현이 감지되었습니다. 프로필 탭에서 내용을 다시 확인해 주세요.</p>
      </section>

      <section class="section-card">
        <div class="section-head">
          <div>
            <p class="section-label">Discover</p>
            <h2>오늘의 인연 추천</h2>
          </div>
          <span class="section-caption">기존 추천 보기 기능을 오늘의 인연 탭으로 옮겨두었습니다.</span>
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
            <p>실제 매칭 기능을 붙이기 전까지는 프론트 시뮬레이션으로 반응 수를 보여줍니다.</p>
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
                <strong>{{ userSummary.fraudRiskScore }}</strong>
                <span>현재 안전 점수</span>
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
          <h3>{{ selectedCard.name }}님의 카드</h3>
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
const showWatchBanner = computed(() => userSummary.value?.fraudReviewStatus === "WATCH");

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

const smokingLabel = computed(() => ({
  NON_SMOKER: "비흡연",
  SMOKER: "흡연",
  OCCASIONAL: "가끔 흡연",
}[profile.value?.smokingStatus] || "흡연 정보 미입력"));

const drinkingLabel = computed(() => ({
  NONE: "비음주",
  SOMETIMES: "가끔 음주",
  OFTEN: "자주 음주",
}[profile.value?.drinkingStatus] || "음주 정보 미입력"));

const religionLabel = computed(() => ({
  NONE: "무교",
  CHRISTIAN: "기독교",
  BUDDHIST: "불교",
  CATHOLIC: "천주교",
  OTHER: "기타 종교",
}[profile.value?.religion] || "종교 정보 미입력"));

const heroTitle = computed(() => {
  if (!profile.value) return "오늘의 인연을 준비하고 있어요";
  return `${profile.value.region}에서 어울리는 인연을 추천해 드릴게요`;
});

const heroDescription = computed(() => {
  if (!profile.value) return "프로필을 바탕으로 추천 카드를 구성하고 있습니다.";
  return `${profile.value.mbti ? `${profile.value.mbti} 성향과 ` : ""}${profile.value.introduction ? "자기소개를 기반으로 " : ""}오늘의 추천 카드를 골랐어요.`;
});

const recommendationCards = computed(() => {
  if (!profile.value) return [];

  const region = profile.value.region || "서울";
  const ideal = profile.value.idealType || "대화가 편안한 사람";
  const mbti = profile.value.mbti || "감성형";

  return [
    {
      id: 1,
      name: "서윤",
      age: 29,
      region,
      job: "브랜드 디자이너",
      badge: "오늘 가장 잘 맞는 카드",
      introduction: `${ideal}을 중요하게 보는 분위기를 반영해 차분하게 대화가 이어질 타입으로 구성한 추천 카드예요.`,
      tags: [mbti, "주말 데이트", "카페 좋아해요"],
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
      tags: [drinkingLabel.value, "전시회", "사진 취향"],
      reason: "라이프스타일 정보가 현재 프로필과 비교적 잘 맞는 카드예요.",
      background: "linear-gradient(135deg, #ef8c74 0%, #f3c3b2 100%)",
    },
    {
      id: 3,
      name: "지수",
      age: 27,
      region: "같은 생활권",
      job: "서비스 기획자",
      badge: "프로필 완성도 기반 추천",
      introduction: "직업, MBTI, 자기소개가 채워질수록 이런 메인 카드가 실제 추천 결과로 더 정교해질 수 있어요.",
      tags: [smokingLabel.value, religionLabel.value, "깊은 대화"],
      reason: "현재 프로필 정보가 반영되면 어떤 유형을 추천할지 보여주는 카드입니다.",
      background: "linear-gradient(135deg, #d9826b 0%, #f2b8a3 100%)",
    },
  ];
});

onMounted(async () => {
  try {
    const [{ data: me }, { data: myProfile }, { data: myImages }] = await Promise.all([
      api.get("/user/me"),
      api.get("/profile/me"),
      api.get("/profile-images/me"),
    ]);

    if (me.status === "SUSPENDED" || me.fraudReviewStatus === "HIGH_RISK") {
      window.alert("계정 이용이 제한되었습니다. 다시 로그인해 주세요.");
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
.today-page {
  min-height: 100vh;
  padding: max(12px, env(safe-area-inset-top)) 12px 24px;
}

.today-shell {
  width: min(100%, 480px);
  margin: 0 auto;
}

.hero-card,
.section-card,
.summary-card,
.discover-card,
.modal-card,
.warning-banner {
  border-radius: 30px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 22px 44px rgba(109, 57, 41, 0.08);
}

.hero-card,
.section-card {
  padding: 20px 16px;
}

.hero-card {
  display: grid;
  gap: 16px;
}

.eyebrow,
.section-label {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  width: fit-content;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 14px;
}

.status-active { background: rgba(179, 233, 201, 0.8); color: #176540; }
.status-pending { background: rgba(255, 225, 179, 0.85); color: #7a4e0b; }
.status-rejected { background: rgba(255, 204, 204, 0.88); color: #902d2d; }
.status-suspended { background: rgba(255, 220, 220, 0.95); color: #8c2f2f; }

h1,
h2,
h3,
strong {
  color: #2f211d;
}

h1 {
  margin: 0;
  font-size: clamp(2rem, 8vw, 2.7rem);
  line-height: 1.06;
}

h2 {
  margin: 0;
  font-size: clamp(1.35rem, 5vw, 1.8rem);
}

.description,
.section-caption,
.discover-body p,
.summary-card p {
  color: #6f564d;
  line-height: 1.68;
}

.hero-stats,
.discover-grid,
.summary-grid,
.tag-row,
.action-row {
  display: grid;
  gap: 12px;
}

.hero-stats,
.discover-grid,
.summary-grid {
  grid-template-columns: 1fr;
}

.stat-card,
.summary-card,
.discover-body {
  padding: 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(235, 209, 199, 0.85);
}

.stat-card span,
.summary-badge,
.discover-meta {
  color: #916456;
  font-size: 13px;
  font-weight: 700;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  font-size: 1.35rem;
}

.warning-banner {
  margin-top: 14px;
  padding: 18px 16px;
  background: #fff3ea;
}

.warning-banner strong {
  display: block;
  margin-bottom: 8px;
}

.section-card {
  margin-top: 14px;
}

.section-head {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.discover-card {
  overflow: hidden;
}

.discover-photo {
  min-height: 236px;
  padding: 14px;
  display: flex;
  align-items: flex-end;
}

.discover-overlay {
  width: 100%;
  padding: 18px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(52, 33, 29, 0.08) 0%, rgba(52, 33, 29, 0.82) 100%);
}

.discover-overlay span {
  display: inline-flex;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(255, 244, 239, 0.18);
  color: #fff7f2;
  font-size: 12px;
  font-weight: 700;
}

.discover-overlay strong {
  display: block;
  margin-top: 12px;
  color: #fff;
  font-size: 1.5rem;
}

.discover-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.tag-row {
  grid-template-columns: repeat(auto-fit, minmax(0, max-content));
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  background: #f8ede7;
  color: #7d5a50;
}

.tag.soft {
  background: #fff6f1;
}

.action-row {
  grid-template-columns: 1fr;
  margin-top: 16px;
}

.action-button {
  border: none;
  border-radius: 999px;
  padding: 14px 16px;
  font-weight: 800;
  cursor: pointer;
}

.action-button.pass { background: #f3ebe5; color: #654b43; }
.action-button.detail { background: #fff3ec; color: #a6593e; }
.action-button.like {
  background: linear-gradient(135deg, #d56f4e 0%, #eea27d 100%);
  color: #fff;
  box-shadow: 0 14px 24px rgba(213, 111, 78, 0.22);
}

.summary-card.accent {
  background: linear-gradient(180deg, rgba(255, 244, 238, 0.96), rgba(255, 252, 249, 0.92));
}

.queue-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.queue-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(235, 209, 199, 0.85);
}

.queue-list li:last-child {
  border-bottom: none;
}

.loading-shell {
  min-height: 100vh;
  display: grid;
  place-items: center;
}

.loading-card {
  width: min(100%, 420px);
  padding: 28px 22px;
  border-radius: 30px;
  background: rgba(255, 251, 248, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.72);
  text-align: center;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(34, 22, 18, 0.55);
  display: grid;
  place-items: center;
  padding: 20px;
  z-index: 40;
}

.modal-card {
  width: min(760px, 100%);
  overflow: hidden;
  box-shadow: 0 30px 80px rgba(40, 24, 20, 0.28);
}

.modal-photo {
  min-height: 260px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  color: #fff;
}

.modal-photo span {
  display: inline-flex;
  width: fit-content;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 12px;
  font-weight: 700;
}

.modal-photo strong {
  margin-top: 14px;
  color: #fff;
  font-size: 2rem;
}

.modal-body {
  padding: 24px;
}

.modal-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.spacious {
  margin-top: 14px;
}

@media (min-width: 768px) {
  .today-page {
    padding: 24px 20px 40px;
  }

  .today-shell {
    width: min(1180px, 100%);
  }

  .hero-card,
  .section-card {
    padding: 26px;
  }

  .hero-stats,
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .summary-grid {
    grid-template-columns: 1.2fr 0.8fr;
  }

  .action-row {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (min-width: 1024px) {
  .discover-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
