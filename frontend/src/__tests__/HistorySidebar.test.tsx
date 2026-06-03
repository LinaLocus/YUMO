import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, describe, it, expect } from 'vitest';
import { HistorySidebar } from '@/components/HistorySidebar';
import type { ListItem } from '@/types';

const items: ListItem[] = [
  { id: 1, originalFilename: 'a.mp3', status: 'DONE', template: 'MEETING', createdAt: '2026-06-03T00:00:00Z' },
  { id: 2, originalFilename: 'b.mp3', status: 'TRANSCRIBING', template: 'GENERAL', createdAt: '2026-06-03T00:00:00Z' },
];

describe('HistorySidebar', () => {
  it('renders items with status labels', () => {
    render(<HistorySidebar items={items} activeId={1} onSelect={() => {}} onDelete={() => {}} onNew={() => {}} />);
    expect(screen.getByText('a.mp3')).toBeInTheDocument();
    expect(screen.getByText('完成')).toBeInTheDocument();
    expect(screen.getByText('转写中')).toBeInTheDocument();
  });

  it('calls onDelete without selecting', async () => {
    const onSelect = vi.fn();
    const onDelete = vi.fn();
    render(<HistorySidebar items={items} activeId={null} onSelect={onSelect} onDelete={onDelete} onNew={() => {}} />);
    await userEvent.click(screen.getAllByLabelText('删除')[0]);
    expect(onDelete).toHaveBeenCalledWith(1);
    expect(onSelect).not.toHaveBeenCalled();
  });
});
