import { createRouter, createWebHistory } from "vue-router";
import { hasToken } from "../utils/auth";
import Home from "../views/Home.vue";
import Login from "../views/Login.vue";
import AdminReviews from "../views/AdminReviews.vue";
import ReviewPending from "../views/ReviewPending.vue";
import ResetPassword from "../views/ResetPassword.vue";
import SignupComplete from "../views/SignupComplete.vue";
import Signup from "../views/Signup.vue";
import SocialLogin from "../views/SocialLogin.vue";

const routes = [
  { path: "/", component: Login, meta: { guestOnly: true } },
  { path: "/admin/reviews", component: AdminReviews },
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

// 로그인 여부에 따라 게스트 전용 페이지와 인증 필요 페이지 접근을 나눈다.
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
