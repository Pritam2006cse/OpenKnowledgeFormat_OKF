import { ArrowDown, CheckCircle2, FileText, FileType2, RotateCw, X, AlertTriangle } from "lucide-react";
import type { QueuedFile } from "@/lib/okf/types";
import { formatBytes, okf } from "@/lib/okf/store";
import { cn } from "@/lib/utils";

function Progress({ value, active }: { value: number; active?: boolean }) {
  return (
    <div className="relative h-1.5 overflow-hidden rounded-full bg-muted">
      <div className="h-full rounded-full bg-primary transition-all duration-200" style={{ width: `${value}%` }} />
      {active && <div className="absolute inset-y-0 w-1/3 bg-primary-foreground/40 animate-scan" />}
    </div>
  );
}

export function FileCard({ f }: { f: QueuedFile }) {
  const isMd = ["converted", "processing", "processed"].includes(f.stage);
  const failed = f.stage === "upload_failed" || f.stage === "process_failed";
  const removable = !["processing", "processed"].includes(f.stage);

  return (
    <li className={cn("lift animate-fade-up rounded-2xl border bg-card p-4 shadow-soft", failed && "border-destructive/40")}>
      <div className="flex items-start gap-3">
        <div className={cn("relative grid h-11 w-11 shrink-0 place-items-center rounded-xl transition-colors duration-300", isMd ? "bg-accent text-accent-foreground" : "bg-muted text-muted-foreground")}>
          {isMd ? <FileText className="h-5 w-5 animate-pop" /> : <FileType2 className="h-5 w-5" />}
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex items-center gap-2">
            <p className="truncate font-medium">{isMd ? f.markdownName : f.name}</p>
            <span className="rounded-md bg-muted px-1.5 py-0.5 font-mono text-[10px] uppercase text-muted-foreground">
              {isMd ? "md" : f.ext}
            </span>
          </div>
          <p className="mt-0.5 truncate text-xs text-muted-foreground">
            {isMd ? <>from {f.name} · {formatBytes(f.size)}</> : formatBytes(f.size)}
          </p>
        </div>
        {removable && (
          <button
            onClick={() => okf.remove(f.id)}
            aria-label={`Remove ${f.name}`}
            className="rounded-lg p-1.5 text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
          >
            <X className="h-4 w-4" />
          </button>
        )}
      </div>

      <div className="mt-3 space-y-2">
        {f.stage === "uploading" && (
          <>
            <p className="text-xs text-muted-foreground">Uploading… {f.progress}%</p>
            <Progress value={f.progress} />
          </>
        )}
        {f.stage === "converting" && (
          <>
            <div className="flex items-center gap-2 font-mono text-xs text-muted-foreground">
              <span className="truncate">{f.name}</span>
              <ArrowDown className="h-3 w-3 -rotate-90 text-primary" />
              <span className="truncate text-foreground">{f.name.replace(/\.[^.]+$/, "")}.md</span>
            </div>
            <p className="text-xs font-medium text-primary">Converting to Markdown…</p>
            <Progress value={f.progress} active />
          </>
        )}
        {f.stage === "converted" && (
          <p className="flex items-center gap-1.5 text-xs font-medium text-primary">
            <CheckCircle2 className="h-3.5 w-3.5" /> Converted to Markdown · ready to process
          </p>
        )}
        {f.stage === "processing" && <p className="text-xs text-muted-foreground">Processing knowledge…</p>}
        {f.stage === "processed" && (
          <p className="flex items-center gap-1.5 text-xs font-medium text-primary">
            <CheckCircle2 className="h-3.5 w-3.5" /> Knowledge ready
          </p>
        )}
        {failed && (
          <div className="flex items-center justify-between gap-2">
            <p className="flex items-center gap-1.5 text-xs font-medium text-destructive">
              <AlertTriangle className="h-3.5 w-3.5" /> {f.stage === "upload_failed" ? "Upload failed" : "Processing failed"}: {f.error}
            </p>
            {f.stage === "upload_failed" && (
              <button
                onClick={() => okf.retry(f.id)}
                className="inline-flex items-center gap-1 rounded-lg border px-2.5 py-1 text-xs font-medium transition-all hover:-translate-y-px hover:bg-muted"
              >
                <RotateCw className="h-3 w-3" /> Retry
              </button>
            )}
          </div>
        )}
      </div>
    </li>
  );
}

export function FileQueue({ files }: { files: QueuedFile[] }) {
  if (!files.length) return null;
  return (
    <ul className="grid gap-3 sm:grid-cols-2">
      {files.map((f) => (
        <FileCard key={f.id} f={f} />
      ))}
    </ul>
  );
}
