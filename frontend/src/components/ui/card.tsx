import { cn } from '@/lib/utils';
import { HTMLAttributes } from 'react';

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        'rounded-xl border border-[#99F6E4]/60 bg-white/80 dark:bg-white/5 dark:border-white/10 p-5',
        className,
      )}
      {...props}
    />
  );
}
