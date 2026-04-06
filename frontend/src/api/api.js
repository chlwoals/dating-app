import axios from "axios";
import { clearToken, getToken } from "../utils/auth";

function resolveApiBaseUrl() {
  const envBaseUrl = import.meta.env.VITE_API_BASE_URL;

  if (envBaseUrl) {
    return envBaseUrl;
  }

  const hostname = window.location.hostname || "localhost";
  return `http://${hostname}:8080/api`;
}

const api = axios.create({
  baseURL: resolveApiBaseUrl(),
});

// 저장된 JWT가 있으면 모든 API 요청에 Authorization 헤더를 붙인다.
api.interceptors.request.use((config) => {
  const token = getToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});

// 인증 만료(401) 시 프론트에 남아 있는 로그인 상태를 정리한다.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearToken();
    }

    return Promise.reject(error);
  }
);

export default api;