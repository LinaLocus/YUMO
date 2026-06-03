import { motion } from 'framer-motion';

export function TranscribeProgress() {
  return (
    <div role="status" aria-live="polite" className="space-y-4">
      <div className="flex items-center gap-3">
        <motion.span
          className="inline-block h-3 w-3 rounded-full bg-primary"
          animate={{ opacity: [1, 0.3, 1] }}
          transition={{ duration: 1.2, repeat: Infinity }}
        />
        <span className="text-sm font-medium">转写中，请稍候…</span>
      </div>
      {/* 骨架屏 shimmer */}
      <div className="space-y-2">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-4 w-full overflow-hidden rounded bg-muted">
            <motion.div
              className="h-full w-1/3 bg-gradient-to-r from-transparent via-white/60 to-transparent"
              animate={{ x: ['-100%', '300%'] }}
              transition={{ duration: 1.4, repeat: Infinity, delay: i * 0.15 }}
            />
          </div>
        ))}
      </div>
    </div>
  );
}
