import { useRef, useState } from 'react';
import { motion } from 'framer-motion';
import { UploadCloud } from 'lucide-react';
import { cn } from '@/lib/utils';

const ALLOWED = ['mp3', 'wav', 'm4a'];
const MAX_BYTES = 200 * 1024 * 1024;

function validate(file: File): string | null {
  const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
  if (!ALLOWED.includes(ext)) return '仅支持 mp3/wav/m4a 格式';
  if (file.size > MAX_BYTES) return '文件过大，最大 200MB';
  return null;
}

export function Dropzone({ onFile, disabled }: { onFile: (f: File) => void; disabled: boolean }) {
  const [dragOver, setDragOver] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  function handle(file: File | undefined) {
    if (!file) return;
    const err = validate(file);
    if (err) { setError(err); return; }
    setError(null);
    onFile(file);
  }

  return (
    <div>
      <motion.div
        animate={{ scale: dragOver ? 1.02 : 1 }}
        transition={{ type: 'spring', stiffness: 300, damping: 20 }}
        onDragOver={(e) => { e.preventDefault(); if (!disabled) setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setDragOver(false);
          if (!disabled) handle(e.dataTransfer.files?.[0]);
        }}
        onClick={() => !disabled && inputRef.current?.click()}
        className={cn(
          'flex flex-col items-center justify-center gap-3 rounded-2xl border-2 border-dashed',
          'px-8 py-16 text-center cursor-pointer transition-colors duration-200',
          dragOver ? 'border-accent bg-accent/5' : 'border-[#99F6E4] bg-muted/40',
          disabled && 'opacity-50 pointer-events-none',
        )}
      >
        <motion.div animate={{ y: dragOver ? -4 : 0 }}>
          <UploadCloud className="h-10 w-10 text-primary" />
        </motion.div>
        <p className="text-lg font-medium">拖拽音频到这里，或点击选择文件</p>
        <p className="text-sm opacity-60">支持 mp3 / wav / m4a · 最大 200MB</p>
        <input
          ref={inputRef}
          data-testid="file-input"
          type="file"
          accept=".mp3,.wav,.m4a,audio/*"
          className="hidden"
          onChange={(e) => handle(e.target.files?.[0])}
        />
      </motion.div>
      {error && <p className="mt-2 text-sm text-destructive" role="alert">{error}</p>}
    </div>
  );
}
