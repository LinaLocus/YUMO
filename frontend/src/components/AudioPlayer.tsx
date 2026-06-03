import { motion } from 'framer-motion';
import { Button } from '@/components/ui/button';
import { Download } from 'lucide-react';
import { auth } from '@/store/auth';

/**
 * 播放 TTS 音频。后端 GET /api/summaries/{id}/speech 需带 token；
 * <audio> 不能设 header，这里通过查询参数传 token（JwtAuthFilter 支持）。
 */
export function AudioPlayer({ id, version = 0 }: { id: number; version?: number }) {
  const token = auth.getToken() ?? '';
  // version 在每次重新生成（换音色）后递增，作为缓存破坏参数，强制 <audio> 拉取新音频
  const src = `/api/summaries/${id}/speech?token=${encodeURIComponent(token)}&v=${version}`;
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2 }}
      className="mt-4 flex items-center gap-3 rounded-lg border border-[#99F6E4]/60 p-3"
    >
      <audio key={version} controls src={src} className="h-9 flex-1" data-testid="tts-audio" />
      <a href={src} download={`summary-${id}.mp3`}>
        <Button variant="ghost" aria-label="下载音频">
          <Download className="h-4 w-4" /> mp3
        </Button>
      </a>
    </motion.div>
  );
}
