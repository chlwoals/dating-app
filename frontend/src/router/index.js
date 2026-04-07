import { createRouter, createWebHistory } from "vue-router";
import { hasToken } from "../utils/auth";
import AdminReviews from "../views/admin/AdminReviews.vue";
import ScamMonitor from "../views/admin/ScamMonitor.vue";
import Login from "../views/auth/Login.vue";
import ResetPassword from "../views/auth/ResetPassword.vue";
import Signup from "../views/auth/Signup.vue";
import SocialLogin from "../views/auth/SocialLogin.vue";
import Home from "../views/user/Home.vue";
import ReviewPending from "../views/user/ReviewPending.vue";
import SignupComplete from "../views/user/SignupComplete.vue";

const routes = [
  { path: "/", component: Login, meta: { guestOnly: true } },
  { path: "/admin/reviews", component: AdminReviews },
  { path: "/admin/safety", component: ScamMonitor },
  { path: "/signup", component: Signup, meta: { guestOnly: true } },
  { path: "/reset-password", component: ResetPassword, meta: { guestOnly: true } },
  { path: "/home", component: Home, meta: { requiresAuth: true } },
  { path: "/review-pending", component: ReviewPending, meta: { requiresAuth: true } },
  { path: "/signup-complete", component: SignupComplete, meta: { requiresAuth: true } },
  { path: "/social-login", component: SocialLogin },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 로그인 상태에 따라 게스트 전용 페이지와 인증 필요 페이지 접근을 나눈다.
router.beforeEach((to) => {
  if (to.meta.requiresAuth && !hasToken()) {
    return "/";
  }

  if (to.meta.guestOnly && hasToken()) {
    return "/home";
  }

  return true;
});

export default router;
