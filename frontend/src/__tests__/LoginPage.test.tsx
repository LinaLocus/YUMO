import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import LoginPage from '@/pages/LoginPage';
import { api } from '@/lib/api';
import { auth } from '@/store/auth';

describe('LoginPage', () => {
  beforeEach(() => { auth.clear(); vi.restoreAllMocks(); });

  it('toggles to register mode', async () => {
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await userEvent.click(screen.getByText('没有账户？去注册'));
    expect(screen.getByText('注册')).toBeInTheDocument();
  });

  it('shows error on failed login', async () => {
    vi.spyOn(api, 'login').mockRejectedValue(new Error('用户名或密码错误'));
    render(<MemoryRouter><LoginPage /></MemoryRouter>);
    await userEvent.type(screen.getByPlaceholderText('用户名'), 'a');
    await userEvent.type(screen.getByPlaceholderText('密码'), 'bbbbbb');
    await userEvent.click(screen.getByRole('button', { name: '登录' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('用户名或密码错误');
  });
});
