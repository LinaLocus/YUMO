import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { SummaryView } from '@/components/SummaryView';

describe('SummaryView', () => {
  it('renders markdown headings', () => {
    render(<SummaryView markdown={'# 会议纪要\n\n- 要点一'} streaming={false} />);
    expect(screen.getByRole('heading', { name: '会议纪要' })).toBeInTheDocument();
    expect(screen.getByText('要点一')).toBeInTheDocument();
  });

  it('shows streaming cursor when streaming', () => {
    render(<SummaryView markdown={'# 标题'} streaming={true} />);
    expect(screen.getByTestId('stream-cursor')).toBeInTheDocument();
  });

  it('hides cursor when not streaming', () => {
    render(<SummaryView markdown={'# 标题'} streaming={false} />);
    expect(screen.queryByTestId('stream-cursor')).not.toBeInTheDocument();
  });
});
