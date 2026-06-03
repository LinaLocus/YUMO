import { describe, it, expect, vi, beforeEach } from 'vitest';
import { api } from '@/lib/api';
import { auth } from '@/store/auth';

describe('api', () => {
  beforeEach(() => {
    auth.clear();
    vi.restoreAllMocks();
  });

  it('attaches bearer token', async () => {
    auth.set('tok123', 'alice');
    const spy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      }),
    );
    await api.list();
    const init = spy.mock.calls[0][1] as RequestInit;
    const headers = init.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer tok123');
  });

  it('clears auth and throws on 401', async () => {
    auth.set('tok', 'alice');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 401 }));
    await expect(api.list()).rejects.toThrow('登录已过期');
    expect(auth.isLoggedIn()).toBe(false);
  });

  it('surfaces server error message', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ error: '用户名已存在' }), {
        status: 409,
        headers: { 'content-type': 'application/json' },
      }),
    );
    await expect(api.register('a', 'b')).rejects.toThrow('用户名已存在');
  });
});
