export const TOKEN_KEY = "token";
export const SIGNUP_APPROVAL_NOTICE_KEY = "signupApprovalNoticeShown";

export const getToken = () => localStorage.getItem(TOKEN_KEY);

export const setToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const clearToken = () => {
  localStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(SIGNUP_APPROVAL_NOTICE_KEY);
};

export const hasToken = () => Boolean(getToken());

export const resetSignupApprovalNotice = () => {
  sessionStorage.removeItem(SIGNUP_APPROVAL_NOTICE_KEY);
};

export const hasShownSignupApprovalNotice = () =>
  sessionStorage.getItem(SIGNUP_APPROVAL_NOTICE_KEY) === "true";

export const markSignupApprovalNoticeShown = () => {
  sessionStorage.setItem(SIGNUP_APPROVAL_NOTICE_KEY, "true");
};
