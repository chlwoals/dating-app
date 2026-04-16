// Firebase Google 로그인 초기화와 팝업 로그인을 담당한다.
import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider, signInWithPopup } from "firebase/auth";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
};

function assertFirebaseConfig() {
  const missingKeys = Object.entries(firebaseConfig)
    .filter(([, value]) => !value)
    .map(([key]) => key);

  if (missingKeys.length > 0) {
    throw new Error("Firebase 설정값이 없습니다. .env.local에 Firebase Web App 설정을 입력해 주세요.");
  }
}

let auth;

function getFirebaseAuth() {
  assertFirebaseConfig();
  if (!auth) {
    const app = initializeApp(firebaseConfig);
    auth = getAuth(app);
    auth.languageCode = "ko";
  }
  return auth;
}

export async function signInWithGooglePopup() {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  const result = await signInWithPopup(getFirebaseAuth(), provider);
  return result.user.getIdToken();
}
