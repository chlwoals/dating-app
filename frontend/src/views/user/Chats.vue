<template>
  <section class="simple-page">
    <div class="simple-card">
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
  width: min(100%, 520px);
  margin: auto 0;
  padding: 32px 22px;
  border-radius: 32px;
  background: rgba(255, 252, 249, 0.9);
  border: 1px solid rgba(239, 208, 193, 0.78);
  box-shadow: 0 24px 48px rgba(109, 57, 41, 0.08);
  text-align: center;
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
  margin: 14px 0 0;
  color: #6f564d;
  line-height: 1.7;
}
</style>
