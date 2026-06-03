import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import { Toolbar } from '@/components/Toolbar';
import { api } from '@/lib/api';

describe('Toolbar', () => {
  beforeEach(() => vi.restoreAllMocks());

  it('generates speech with selected voice and notifies', async () => {
    vi.spyOn(api, 'generateSpeech').mockResolvedValue({ id: 1, audioUrl: '/x' });
    const onReady = vi.fn();
    render(<Toolbar id={1} markdown="# x" onSpeechReady={onReady} speaking={false} />);
    await userEvent.click(screen.getByText('朗读'));
    expect(api.generateSpeech).toHaveBeenCalledWith(1, expect.any(String));
    expect(onReady).toHaveBeenCalled();
  });

  it('disables speak button while speaking', () => {
    render(<Toolbar id={1} markdown="# x" onSpeechReady={() => {}} speaking={true} />);
    expect(screen.getByText('生成中…').closest('button')).toBeDisabled();
  });
});
