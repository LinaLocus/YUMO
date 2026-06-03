import { Button } from '@/components/ui/button';
import { Download, Copy, Volume2 } from 'lucide-react';
import { api } from '@/lib/api';
import { auth } from '@/store/auth';
import { toast } from 'sonner';

interface Props {
  id: number;
  markdown: string;
  onSpeechReady: () => void;
  speaking: boolean;
}

export function Toolbar({ id, markdown, onSpeechReady, speaking }: Props) {
  async function copy() {
    await navigator.clipboard.writeText(markdown);
    toast.success('已复制到剪贴板');
  }

  function downloadMd() {
    const token = auth.getToken() ?? '';
    const a = document.createElement('a');
    a.href = `${api.downloadUrl(id)}?token=${encodeURIComponent(token)}`;
    a.download = `summary-${id}.md`;
    a.click();
  }

  async function speak() {
    try {
      await api.generateSpeech(id);
      toast.success('朗读音频已生成');
      onSpeechReady();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : '朗读生成失败');
    }
  }

  return (
    <div className="flex flex-wrap gap-2">
      <Button variant="accent" onClick={downloadMd}>
        <Download className="h-4 w-4" /> 下载 .md
      </Button>
      <Button variant="ghost" onClick={copy}>
        <Copy className="h-4 w-4" /> 复制
      </Button>
      <Button variant="ghost" onClick={speak} disabled={speaking}>
        <Volume2 className="h-4 w-4" /> {speaking ? '生成中…' : '朗读'}
      </Button>
    </div>
  );
}
