const TOKEN_KEY = 'vn_token';
const USER_KEY = 'vn_user';

export const auth = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },
  getUsername(): string | null {
    return localStorage.getItem(USER_KEY);
  },
  set(token: string, username: string) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, username);
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
  isLoggedIn(): boolean {
    return !!localStorage.getItem(TOKEN_KEY);
  },
};
