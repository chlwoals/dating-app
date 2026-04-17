<template>
  <section class="simple-page">
    <div class="simple-card">
      <div class="chat-emoji" aria-hidden="true">
        <span class="bubble bubble-left"></span>
        <span class="bubble bubble-right"></span>
        <span class="heart">♥</span>
      </div>
      <p class="eyebrow">Chat</p>
      <h1>채팅</h1>
      <p class="description">
        채팅 화면은 자리만 먼저 만들어두었습니다. 이후 매칭 목록이나 대화방 리스트를 여기 붙이면 됩니다.
      </p>
    </div>
  </section>
</template>

<style scoped>
.simple-page {
  min-height: 100vh;
  padding: max(16px, env(safe-area-inset-top)) 16px 24px;
  display: flex;
  justify-content: center;
}

.simple-card {
  position: relative;
  width: min(100%, 520px);
  margin: auto 0;
  padding: 38px 22px 34px;
  border-radius: 32px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.08);
  text-align: center;
  overflow: hidden;
}

.simple-card::before {
  position: absolute;
  content: "";
  inset: auto -20% -48% -20%;
  height: 150px;
  background: radial-gradient(circle, rgba(231, 132, 94, 0.18), transparent 68%);
  pointer-events: none;
}

.chat-emoji {
  position: relative;
  z-index: 1;
  width: 116px;
  height: 88px;
  margin: 0 auto 20px;
}

.bubble {
  position: absolute;
  display: block;
  border-radius: 26px;
  box-shadow: 0 14px 28px rgba(148, 82, 59, 0.12);
}

.bubble::after {
  position: absolute;
  content: "";
  width: 18px;
  height: 18px;
  border-radius: 0 0 6px 0;
  transform: rotate(45deg);
}

.bubble-left {
  left: 0;
  bottom: 8px;
  width: 70px;
  height: 50px;
  background: linear-gradient(135deg, #fff8f2 0%, #f4d7ca 100%);
  border: 1px solid rgba(215, 169, 150, 0.75);
}

.bubble-left::after {
  left: 14px;
  bottom: -5px;
  background: #f4d7ca;
}

.bubble-right {
  right: 0;
  top: 0;
  width: 78px;
  height: 54px;
  background: linear-gradient(135deg, #d66a49 0%, #ef9b74 100%);
}

.bubble-right::after {
  right: 16px;
  bottom: -5px;
  background: #e98f69;
}

.bubble-right::before {
  position: absolute;
  content: "···";
  inset: 0;
  display: grid;
  place-items: center;
  color: #fff7f2;
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 2px;
  transform: translateY(-5px);
}

.heart {
  position: absolute;
  left: 44px;
  top: 38px;
  z-index: 2;
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: #fffaf7;
  color: #d25d3f;
  box-shadow: 0 12px 22px rgba(135, 66, 46, 0.16);
  font-size: 19px;
  line-height: 1;
}

.eyebrow {
  margin: 0 0 10px;
  color: #af5f42;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #31211d;
  font-size: clamp(2rem, 7vw, 2.5rem);
}

.description {
  position: relative;
  z-index: 1;
  margin: 14px 0 0;
  color: #6f564d;
  line-height: 1.7;
}

.eyebrow,
h1 {
  position: relative;
  z-index: 1;
}
</style>
