import { motion, AnimatePresence } from 'framer-motion';
import { Trash2, Plus } from 'lucide-react';
import type { ListItem, TranscriptionStatus } from '@/types';
import { cn } from '@/lib/utils';

const STATUS_LABEL: Record<TranscriptionStatus, string> = {
  UPLOADED: '待转写', TRANSCRIBING: '转写中', TRANSCRIBED: '待概括',
  SUMMARIZING: '概括中', DONE: '完成', FAILED: '失败',
};

interface Props {
  items: ListItem[];
  activeId: number | null;
  onSelect: (id: number) => void;
  onDelete: (id: number) => void;
  onNew: () => void;
}

export function HistorySidebar({ items, activeId, onSelect, onDelete, onNew }: Props) {
  return (
    <aside className="flex w-60 flex-col gap-2 border-r border-[#99F6E4]/40 p-3">
      <button
        onClick={onNew}
        className="flex items-center justify-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-medium text-primary-fg hover:opacity-90 cursor-pointer"
      >
        <Plus className="h-4 w-4" /> 新建
      </button>
      <h2 className="mt-2 px-1 text-xs font-semibold uppercase opacity-50">历史记录</h2>
      <div className="flex-1 space-y-1 overflow-y-auto">
        <AnimatePresence initial={false}>
          {items.map((it, i) => (
            <motion.div
              key={it.id}
              layout
              initial={{ opacity: 0, x: -8 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -8 }}
              transition={{ delay: i * 0.03 }}
              className={cn(
                'group flex items-center justify-between rounded-lg px-3 py-2 text-sm cursor-pointer',
                activeId === it.id ? 'bg-muted' : 'hover:bg-muted/60',
              )}
              onClick={() => onSelect(it.id)}
            >
              <div className="min-w-0">
                <p className="truncate">{it.originalFilename}</p>
                <p className="text-xs opacity-50">{STATUS_LABEL[it.status]}</p>
              </div>
              <button
                aria-label="删除"
                onClick={(e) => { e.stopPropagation(); onDelete(it.id); }}
                className="opacity-0 group-hover:opacity-100 text-destructive cursor-pointer"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </aside>
  );
}
