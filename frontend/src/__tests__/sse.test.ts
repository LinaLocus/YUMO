import { describe, it, expect, vi } from 'vitest';
import { streamSummary } from '@/lib/sse';

class FakeEventSource {
  static instances: FakeEventSource[] = [];
  listeners: Record<string, ((e: MessageEvent) => void)[]> = {};
  closed = false;
  constructor(public url: string) {
    FakeEventSource.instances.push(this);
  }
  addEventListener(type: string, cb: (e: MessageEvent) => void) {
    (this.listeners[type] ??= []).push(cb);
  }
  emit(type: string, data: string) {
    (this.listeners[type] ?? []).forEach((cb) =>
      cb(new MessageEvent(type, { data })),
    );
  }
  close() {
    this.closed = true;
  }
}

describe('streamSummary', () => {
  it('accumulates chunks and resolves on done', async () => {
    vi.stubGlobal('EventSource', FakeEventSource as unknown as typeof EventSource);
    const chunks: string[] = [];
    const promise = streamSummary(7, 'tok', (c) => chunks.push(c));

    const es = FakeEventSource.instances.at(-1)!;
    expect(es.url).toContain('/api/summaries/7/stream');
    expect(es.url).toContain('token=tok');

    es.emit('chunk', '# 概括\n');
    es.emit('chunk', '- 要点');
    es.emit('done', '[DONE]');

    await promise;
    expect(chunks).toEqual(['# 概括\n', '- 要点']);
    expect(es.closed).toBe(true);
  });

  it('rejects on error event', async () => {
    vi.stubGlobal('EventSource', FakeEventSource as unknown as typeof EventSource);
    const promise = streamSummary(8, 'tok', () => {});
    const es = FakeEventSource.instances.at(-1)!;
    es.emit('error', '概括失败');
    await expect(promise).rejects.toThrow('概括失败');
  });
});
