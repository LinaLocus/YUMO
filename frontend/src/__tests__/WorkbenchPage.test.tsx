import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import WorkbenchPage from '@/pages/WorkbenchPage';
import { api } from '@/lib/api';
import * as sse from '@/lib/sse';
import { auth } from '@/store/auth';

describe('WorkbenchPage flow', () => {
  beforeEach(() => {
    auth.set('tok', 'alice');
    vi.restoreAllMocks();
    vi.spyOn(api, 'list').mockResolvedValue([]);
  });

  it('runs upload -> transcribe -> stream summary', async () => {
    vi.spyOn(api, 'upload').mockResolvedValue({ id: 9, status: 'UPLOADED' });
    vi.spyOn(api, 'transcribe').mockResolvedValue({} as never);
    vi.spyOn(api, 'detail').mockResolvedValue({
      id: 9, originalFilename: 'a.mp3', status: 'DONE',
      transcriptText: 't', summaryMarkdown: '# 概括', hasTtsAudio: false,
      template: 'MEETING', errorMessage: null, createdAt: '2026-06-03T00:00:00Z',
    });
    vi.spyOn(sse, 'streamSummary').mockImplementation(async (_id, _tok, onChunk) => {
      onChunk('# 概括\n');
      onChunk('- 要点');
    });

    render(<MemoryRouter><WorkbenchPage /></MemoryRouter>);

    const input = screen.getByTestId('file-input') as HTMLInputElement;
    const file = new File(['x'], 'a.mp3', { type: 'audio/mpeg' });
    await userEvent.upload(input, file);

    await waitFor(() => expect(sse.streamSummary).toHaveBeenCalled());
    expect(await screen.findByRole('heading', { name: '概括' })).toBeInTheDocument();
    expect(screen.getByText('要点')).toBeInTheDocument();
    // 完成后出现工具栏「朗读」
    expect(await screen.findByText('朗读')).toBeInTheDocument();
  });

  it('shows error toast when transcribe fails', async () => {
    vi.spyOn(api, 'upload').mockResolvedValue({ id: 9, status: 'UPLOADED' });
    vi.spyOn(api, 'transcribe').mockRejectedValue(new Error('转写失败'));

    render(<MemoryRouter><WorkbenchPage /></MemoryRouter>);
    const input = screen.getByTestId('file-input') as HTMLInputElement;
    await userEvent.upload(input, new File(['x'], 'a.mp3', { type: 'audio/mpeg' }));

    // 失败后回到 idle，dropzone 文案再次出现
    expect(await screen.findByText(/拖拽音频到这里/)).toBeInTheDocument();
  });
});
