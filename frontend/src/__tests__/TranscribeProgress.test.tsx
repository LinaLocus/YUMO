import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { TranscribeProgress } from '@/components/TranscribeProgress';

describe('TranscribeProgress', () => {
  it('shows transcribing label', () => {
    render(<TranscribeProgress />);
    expect(screen.getByText(/转写中/)).toBeInTheDocument();
  });

  it('has aria-live for accessibility', () => {
    render(<TranscribeProgress />);
    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
