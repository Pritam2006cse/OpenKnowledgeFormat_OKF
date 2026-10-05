// Small client store shared across screens. Swap reads for GET /knowledge when the backend exists.
import { useSyncExternalStore } from "react";
import { convertFile, processFile, uploadFile } from "./api";
import type { KnowledgeItem, QueuedFile, SourceDocument } from "./types";

interface State {
  queue: QueuedFile[];
  pipeline: { fileId: string; step: number }[];
  documents: SourceDocument[];
  items: KnowledgeItem[];
  lastRun: { files: string[]; items: number; terms: number } | null;
  processing: boolean;
}

let state: State = { queue: [], pipeline: [], documents: [], items: [], lastRun: null, processing: false };
const listeners = new Set<() => void>();
const set = (fn: (s: State) => Partial<State>) => {
  state = { ...state, ...fn(state) };
  listeners.forEach((l) => l());
};
const patchFile = (id: string, p: Partial<QueuedFile>) =>
  set((s) => ({ queue: s.queue.map((f) => (f.id === id ? { ...f, ...p } : f)) }));

export function useOkf<T>(sel: (s: State) => T): T {
  return useSyncExternalStore(
    (l) => (listeners.add(l), () => listeners.delete(l)),
    () => sel(state),
    () => sel(state),
  );
}

async function ingest(q: QueuedFile) {
  try {
    patchFile(q.id, { stage: "uploading", progress: 0, error: undefined });
    await uploadFile(q.file, (progress) => patchFile(q.id, { progress }));
  } catch (e) {
    patchFile(q.id, { stage: "upload_failed", error: (e as Error).message });
    return;
  }
  patchFile(q.id, { stage: "converting", progress: 0 });
  const { markdownName, markdown } = await convertFile(q.file, (progress) => patchFile(q.id, { progress }));
  patchFile(q.id, { stage: "converted", progress: 100, markdownName, markdown });
}

export const okf = {
  addFiles(files: File[]) {
    const added: QueuedFile[] = files.map((file) => ({
      id: Math.random().toString(36).slice(2),
      file,
      name: file.name,
      size: file.size,
      ext: file.name.split(".").pop()?.toLowerCase() ?? "",
      stage: "uploading",
      progress: 0,
    }));
    set((s) => ({ queue: [...s.queue, ...added], lastRun: null }));
    added.forEach(ingest);
  },
  retry(id: string) {
    const f = state.queue.find((x) => x.id === id);
    if (f) ingest(f);
  },
  remove(id: string) {
    set((s) => ({ queue: s.queue.filter((f) => f.id !== id) }));
  },
  reset() {
    set(() => ({ queue: [], pipeline: [], lastRun: null }));
  },
  async processAll() {
    const ready = state.queue.filter((f) => f.stage === "converted");
    if (!ready.length) return;
    set(() => ({ processing: true, pipeline: ready.map((f) => ({ fileId: f.id, step: 0 })) }));
    let items = 0;
    let terms = 0;
    for (const f of ready) {
      patchFile(f.id, { stage: "processing" });
      const placeholder: SourceDocument = {
        id: `pending-${f.id}`, originalName: f.name, markdownName: f.markdownName!, markdown: f.markdown!,
        status: "processing", createdAt: new Date().toISOString(),
      };
      set((s) => ({ documents: [placeholder, ...s.documents] }));
      try {
        const res = await processFile(f.name, f.markdownName!, f.markdown!, (step) =>
          set((s) => ({ pipeline: s.pipeline.map((p) => (p.fileId === f.id ? { ...p, step } : p)) })),
        );
        items += res.items.length;
        terms += res.termsIndexed;
        set((s) => ({
          documents: s.documents.map((d) => (d.id === placeholder.id ? res.document : d)),
          items: [...res.items, ...s.items],
        }));
        patchFile(f.id, { stage: "processed" });
      } catch (e) {
        set((s) => ({
          documents: s.documents.map((d) =>
            d.id === placeholder.id ? { ...d, status: "invalid", error: (e as Error).message } : d,
          ),
        }));
        patchFile(f.id, { stage: "process_failed", error: (e as Error).message });
      }
    }
    set(() => ({ processing: false, lastRun: { files: ready.map((f) => f.name), items, terms } }));
  },
};

export function formatBytes(n: number) {
  if (n < 1024) return `${n} B`;
  if (n < 1024 ** 2) return `${(n / 1024).toFixed(1)} KB`;
  return `${(n / 1024 ** 2).toFixed(1)} MB`;
}
