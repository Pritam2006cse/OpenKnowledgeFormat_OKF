import { Brain } from "lucide-react";
import { okf, useOkf } from "@/lib/okf/store";
import { UploadDropzone } from "./UploadDropzone";
import { FileQueue } from "./FileCard";
import { ProcessingPipeline, ProcessingResult } from "./ProcessingPipeline";

export function KnowledgeUpload() {
  const queue = useOkf((s) => s.queue);
  const pipeline = useOkf((s) => s.pipeline);
  const processing = useOkf((s) => s.processing);
  const lastRun = useOkf((s) => s.lastRun);
  const ready = queue.filter((f) => f.stage === "converted").length;
  const busy = queue.some((f) => f.stage === "uploading" || f.stage === "converting");

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">Upload your knowledge</h1>
          <p className="mt-2 text-muted-foreground">Add documents to transform them into structured, searchable knowledge.</p>
        </div>
        {queue.length > 0 && (
          <span className="rounded-full border bg-card px-3 py-1 text-sm font-medium shadow-soft">
            {queue.length} {queue.length === 1 ? "file" : "files"} · {ready} ready
          </span>
        )}
      </div>

      {!lastRun && <UploadDropzone onFiles={okf.addFiles} />}

      {queue.length > 0 && !processing && !lastRun && (
        <section className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-sm font-semibold uppercase tracking-widest text-muted-foreground">Upload queue</h2>
            <button
              disabled={!ready || busy}
              onClick={() => okf.processAll()}
              className="lift inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground disabled:pointer-events-none disabled:opacity-40"
            >
              <Brain className="h-4 w-4" /> Process Knowledge
            </button>
          </div>
          <FileQueue files={queue} />
        </section>
      )}

      {processing && <ProcessingPipeline runs={pipeline} queue={queue} />}
      {lastRun && <ProcessingResult result={lastRun} />}
    </div>
  );
}
