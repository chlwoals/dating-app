<template>
  <div>
    <h2>로그인 페이지</h2>

    <button @click="googleLogin">구글 로그인</button>
  </div>
</template>

<script setup>
const googleLogin = () => {
  window.location.href = "http://localhost:8080/oauth2/authorization/google";
};
</script>