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
    <div className="animate-fade-up w-full rounded-3xl border border-white/10 bg-slate-900/75 p-5 shadow-soft backdrop-blur-xl sm:p-6">

      {/* Success heading */}
      <div className="flex items-center gap-3">
        <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-emerald-700 text-white animate-pop">
          <Check className="h-5 w-5" />
        </div>

        <h2 className="font-display text-lg font-semibold text-white sm:text-xl">
          Knowledge processed successfully
        </h2>
      </div>

      <p className="mt-2 truncate text-sm text-slate-300">
        {result.files.join(", ")}
      </p>

      {/* Compact statistics */}
      <dl className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        {[
          ["Knowledge items", result.items],
          ["Sources", result.files.length],
          ["Terms indexed", result.terms],
        ].map(([k, v]) => (
          <div
            key={k}
            className="min-w-0 rounded-xl border border-white/10 bg-slate-800/80 px-4 py-3"
          >
            <dd className="font-display text-2xl font-semibold leading-tight text-white">
              {v}
            </dd>
            <dt className="mt-1 text-sm text-slate-300">
              {k}
            </dt>
          </div>
        ))}
      </dl>

      {/* Actions */}
      <div className="mt-5 flex flex-wrap gap-3">
        <Link
          to="/knowledge"
          className="lift inline-flex items-center rounded-xl bg-emerald-700 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-emerald-600"
        >
          View Knowledge →
        </Link>

        <button
          onClick={() => okf.reset()}
          className="lift inline-flex items-center rounded-xl border border-white/15 bg-slate-800 px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-slate-700"
        >
          Upload More
        </button>
      </div>
    </div>
  );
}
