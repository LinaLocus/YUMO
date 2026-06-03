import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { motion } from 'framer-motion';

export function SummaryView({ markdown, streaming }: { markdown: string; streaming: boolean }) {
  return (
    <div aria-live="polite" className="prose-like leading-relaxed text-[15px]">
      <ReactMarkdown remarkPlugins={[remarkGfm]}>{markdown}</ReactMarkdown>
      {streaming && (
        <motion.span
          data-testid="stream-cursor"
          className="inline-block h-4 w-[2px] translate-y-[2px] bg-primary"
          animate={{ opacity: [1, 0, 1] }}
          transition={{ duration: 0.9, repeat: Infinity }}
        />
      )}
    </div>
  );
}
