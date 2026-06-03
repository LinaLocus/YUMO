/**
 * 打开 SSE 概括流。每个 chunk 通过 onChunk 回调；done 时 resolve；error 时 reject。
 * token 通过 query 传递（EventSource 不能设 header）。
 */
export function streamSummary(
  id: number,
  token: string,
  onChunk: (chunk: string) => void,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const url = `/api/summaries/${id}/stream?token=${encodeURIComponent(token)}`;
    const es = new EventSource(url);

    es.addEventListener('chunk', (e: MessageEvent) => {
      onChunk(e.data);
    });
    es.addEventListener('done', () => {
      es.close();
      resolve();
    });
    es.addEventListener('error', (e: MessageEvent) => {
      es.close();
      const msg = typeof e.data === 'string' && e.data ? e.data : '生成中断';
      reject(new Error(msg));
    });
  });
}
