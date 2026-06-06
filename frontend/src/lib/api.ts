import { auth } from '@/store/auth';
import type { AuthResponse, Detail, ListItem, SummaryTemplate, SummaryLanguage } from '@/types';

class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  const token = auth.getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const res = await fetch(path, { ...init, headers });
  if (res.status === 401) {
    auth.clear();
    throw new ApiError(401, '登录已过期');
  }
  if (!res.ok) {
    let msg = `请求失败 (${res.status})`;
    try {
      const body = await res.json();
      if (body?.error) msg = body.error;
    } catch {
      // 忽略非 JSON 响应
    }
    throw new ApiError(res.status, msg);
  }
  const ct = res.headers.get('content-type') ?? '';
  return (ct.includes('application/json') ? await res.json() : (undefined as T));
}

export const api = {
  ApiError,
  register: (username: string, password: string) =>
    request<AuthResponse>('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }),
  login: (username: string, password: string) =>
    request<AuthResponse>('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }),
  upload: (file: File, template: SummaryTemplate, language: SummaryLanguage) => {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('template', template);
    fd.append('language', language);
    return request<{ id: number; status: string }>('/api/transcriptions', {
      method: 'POST',
      body: fd,
    });
  },
  transcribe: (id: number) =>
    request<Detail>(`/api/transcriptions/${id}/transcribe`, { method: 'POST' }),
  detail: (id: number) => request<Detail>(`/api/transcriptions/${id}`),
  list: () => request<ListItem[]>('/api/transcriptions'),
  remove: (id: number) =>
    request<void>(`/api/transcriptions/${id}`, { method: 'DELETE' }),
  generateSpeech: (id: number, voice?: string) =>
    request<{ id: number; audioUrl: string }>(
      `/api/summaries/${id}/speech${voice ? `?voice=${encodeURIComponent(voice)}` : ''}`,
      { method: 'POST' },
    ),
  downloadUrl: (id: number) => `/api/transcriptions/${id}/download`,
  speechUrl: (id: number) => `/api/summaries/${id}/speech`,
};
