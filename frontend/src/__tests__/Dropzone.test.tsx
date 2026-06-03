import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect } from 'vitest';
import { Dropzone } from '@/components/Dropzone';

describe('Dropzone', () => {
  it('rejects unsupported type and does not call onFile', async () => {
    const onFile = vi.fn();
    render(<Dropzone onFile={onFile} disabled={false} />);
    const input = screen.getByTestId('file-input') as HTMLInputElement;
    const bad = new File(['x'], 'notes.txt', { type: 'text/plain' });
    await userEvent.upload(input, bad, { applyAccept: false });
    expect(onFile).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent('mp3');
  });

  it('accepts mp3 and calls onFile', async () => {
    const onFile = vi.fn();
    render(<Dropzone onFile={onFile} disabled={false} />);
    const input = screen.getByTestId('file-input') as HTMLInputElement;
    const good = new File(['x'], 'meeting.mp3', { type: 'audio/mpeg' });
    await userEvent.upload(input, good);
    expect(onFile).toHaveBeenCalledWith(good);
  });
});
