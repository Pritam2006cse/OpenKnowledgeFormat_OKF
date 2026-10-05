import { Check, Loader2 } from "lucide-react";
import { Link } from "@tanstack/react-router";
import { PIPELINE_STEPS, type QueuedFile } from "@/lib/okf/types";
import { okf } from "@/lib/okf/store";
import { cn } from "@/lib/utils";

export function ProcessingPipeline({ runs, queue }: { runs: { fileId: string; step: number }[]; queue: QueuedFile[] }) {
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      {runs.map((r) => {
        const f = queue.find((q) => q.id === r.fileId);
        if (!f) return null;
        return (
          <div key={r.fileId} className="animate-fade-up rounded-2xl border bg-card p-5 shadow-soft">
            <p className="text-xs uppercase tracking-widest text-muted-foreground">Processing</p>
            <p className="mt-1 truncate font-medium">{f.name}</p>
            <ol className="mt-4 space-y-2.5">
              {PIPELINE_STEPS.map((s, i) => {
                const done = i < r.step;
                const active = i === r.step;
                return (
                  <li key={s} className={cn("flex items-center gap-3 text-sm transition-colors duration-300", done ? "text-foreground" : active ? "text-primary font-medium" : "text-muted-foreground")}>
                    <span className={cn("grid h-5 w-5 place-items-center rounded-full border transition-all duration-300", done && "border-primary bg-primary text-primary-foreground", active && "border-primary")}>
                      {done ? <Check className="h-3 w-3 animate-pop" /> : active ? <Loader2 className="h-3 w-3 animate-spin" /> : null}
                    </span>
                    {s}
                  </li>
                );
              })}
            </ol>
          </div>
        );
      })}
    </div>
  );
}

export function ProcessingResult({ result }: { result: { files: string[]; items: number; terms: number } }) {
  return (
    <div className="animate-fade-up rounded-3xl border border-primary/30 bg-card p-6 shadow-soft sm:p-8">
      <div className="flex items-center gap-3">
        <div className="grid h-10 w-10 place-items-center rounded-full bg-primary text-primary-foreground animate-pop">
          <Check className="h-5 w-5" />
        </div>
        <h2 className="font-display text-xl font-semibold">Knowledge processed successfully</h2>
      </div>
      <p className="mt-3 truncate text-sm text-muted-foreground">{result.files.join(", ")}</p>
      <dl className="mt-6 grid grid-cols-3 gap-3">
        {[
          ["Knowledge items", result.items],
          ["Sources", result.files.length],
          ["Terms indexed", result.terms],
        ].map(([k, v]) => (
          <div key={k} className="rounded-xl bg-muted p-3">
            <dd className="font-display text-2xl font-semibold">{v}</dd>
            <dt className="text-xs text-muted-foreground">{k}</dt>
          </div>
        ))}
      </dl>
      <div className="mt-6 flex flex-wrap gap-3">
        <Link to="/knowledge" className="lift inline-flex items-center rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground">
          View Knowledge →
        </Link>
        <button onClick={() => okf.reset()} className="lift inline-flex items-center rounded-xl border bg-card px-5 py-2.5 text-sm font-semibold">
          Upload More
        </button>
      </div>
    </div>
  );
}
