import { useMemo, useState, type ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import { createPortal } from "react-dom";
import { AlertTriangle, ArrowRight, FileText, Library, Plus, Search, X } from "lucide-react";
import { useOkf } from "@/lib/okf/store";
import type { KnowledgeItem, SourceDocument, ValidationStatus } from "@/lib/okf/types";
import { StatusBadge, STATUS_LABEL } from "../StatusBadge";

const selectCls =
  "h-10 rounded-xl border bg-card px-3 text-sm shadow-soft outline-none transition-colors hover:border-primary/40 focus:ring-2 focus:ring-ring cursor-pointer";

export function KnowledgeLibrary() {
  const documents = useOkf((s) => s.documents);
  const items = useOkf((s) => s.items);
  const [q, setQ] = useState("");
  const [type, setType] = useState("all");
  const [status, setStatus] = useState("all");
  const [source, setSource] = useState("all");
  const [open, setOpen] = useState<KnowledgeItem | null>(null);
  const [mdDoc, setMdDoc] = useState<SourceDocument | null>(null);

  const types = useMemo(() => Array.from(new Set(items.map((i) => i.type))), [items]);
  const docById = useMemo(() => Object.fromEntries(documents.map((d) => [d.id, d])), [documents]);
  const filtered = items.filter(
    (i) =>
      (type === "all" || i.type === type) &&
      (status === "all" || i.status === status) &&
      (source === "all" || i.documentId === source) &&
      (!q || (i.title + " " + i.description).toLowerCase().includes(q.toLowerCase())),
  );

  const header = (
    <div className="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">Knowledge Library</h1>
        <p className="mt-2 text-muted-foreground">Explore and verify the knowledge created from your documents.</p>
      </div>
      <Link to="/" className="lift inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground">
        <Plus className="h-4 w-4" /> Upload Knowledge
      </Link>
    </div>
  );

  if (!documents.length)
    return (
      <div className="space-y-8">
        {header}
        <div className="grid place-items-center rounded-3xl border bg-card px-6 py-20 text-center shadow-soft">
          <div className="grid h-14 w-14 place-items-center rounded-2xl bg-accent text-accent-foreground">
            <Library className="h-7 w-7" />
          </div>
          <h2 className="mt-4 font-display text-xl font-semibold">No knowledge yet</h2>
          <p className="mt-1 text-muted-foreground">Upload documents to start building your knowledge base.</p>
          <Link to="/" className="lift mt-6 inline-flex rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground">
            Upload Knowledge
          </Link>
        </div>
      </div>
    );

  const count = (s: ValidationStatus) => items.filter((i) => i.status === s).length;

  return (
    <div className="space-y-8">
      {header}

      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          ["Documents", documents.length, ""],
          ["Knowledge Items", items.length, ""],
          ["Valid", count("valid"), "✓"],
          ["Needs Review", count("review"), "⚠"],
        ].map(([k, v, icon]) => (
          <div key={k} className="rounded-2xl border bg-card p-4 shadow-soft">
            <dt className="text-xs uppercase tracking-widest text-muted-foreground">{k}</dt>
            <dd className="mt-1 font-display text-2xl font-semibold">{v} <span className="text-base text-primary">{icon}</span></dd>
          </div>
        ))}
      </dl>

      <section className="space-y-3">
        <h2 className="text-sm font-semibold uppercase tracking-widest text-muted-foreground">Sources</h2>
        <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {documents.map((d) => {
            const n = items.filter((i) => i.documentId === d.id).length;
            return (
              <li key={d.id} className="lift rounded-2xl border bg-card p-4 shadow-soft">
                <div className="flex items-center justify-between gap-2">
                  <p className="truncate font-medium">{d.originalName}</p>
                  <StatusBadge status={d.status} />
                </div>
                <div className="mt-2 space-y-1 font-mono text-xs text-muted-foreground">
                  <p className="flex items-center gap-1.5"><ArrowRight className="h-3 w-3" /> {d.markdownName}</p>
                  <p className="flex items-center gap-1.5">
                    <ArrowRight className="h-3 w-3" />
                    {d.status === "processing" ? "Processing knowledge…" : d.status === "invalid" ? "Processing failed" : `${n} knowledge items`}
                  </p>
                </div>
                {d.error && <p className="mt-2 flex items-center gap-1 text-xs text-destructive"><AlertTriangle className="h-3 w-3" /> {d.error}</p>}
                {d.status !== "processing" && (
                  <button onClick={() => setMdDoc(d)} className="mt-3 text-xs font-semibold text-primary hover:underline">
                    View Markdown →
                  </button>
                )}
              </li>
            );
          })}
        </ul>
      </section>

      <section className="space-y-4">
        <div className="flex flex-col gap-3 lg:flex-row">
          <label className="relative flex-1">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Search knowledge…"
              className="h-10 w-full rounded-xl border bg-card pl-9 pr-3 text-sm shadow-soft outline-none focus:ring-2 focus:ring-ring"
            />
          </label>
          <div className="grid grid-cols-3 gap-2">
            <select value={type} onChange={(e) => setType(e.target.value)} className={selectCls} aria-label="Type">
              <option value="all">All Types</option>
              {types.map((t) => <option key={t}>{t}</option>)}
            </select>
            <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectCls} aria-label="Status">
              <option value="all">All Status</option>
              {(["valid", "review", "invalid"] as const).map((s) => <option key={s} value={s}>{STATUS_LABEL[s]}</option>)}
            </select>
            <select value={source} onChange={(e) => setSource(e.target.value)} className={selectCls} aria-label="Source">
              <option value="all">All Sources</option>
              {documents.filter((d) => d.status !== "processing").map((d) => <option key={d.id} value={d.id}>{d.originalName}</option>)}
            </select>
          </div>
        </div>

        {filtered.length === 0 ? (
          <p className="rounded-2xl border bg-card p-8 text-center text-sm text-muted-foreground">No knowledge matches these filters.</p>
        ) : (
          <ul className="grid gap-3 md:grid-cols-2">
            {filtered.map((i) => {
              const d = docById[i.documentId];
              return (
                <li key={i.id} className="lift group flex flex-col rounded-2xl border bg-card p-5 shadow-soft hover:border-primary/40">
                  <div className="flex items-start justify-between gap-3">
                    <h3 className="font-display text-lg font-semibold leading-tight">{i.title}</h3>
                    <span className="shrink-0 rounded-md bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">{i.type}</span>
                  </div>
                  <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">{i.description}</p>
                  <div className="mt-3 space-y-0.5 text-xs text-muted-foreground">
                    <p>Source: <span className="text-foreground">{d?.originalName}</span></p>
                    <p>Markdown: <span className="font-mono text-foreground">{d?.markdownName}</span></p>
                  </div>
                  <div className="mt-4 flex items-center justify-between">
                    <StatusBadge status={i.status} />
                    <button onClick={() => setOpen(i)} className="inline-flex items-center gap-1 rounded-lg border px-3 py-1.5 text-xs font-semibold transition-all hover:-translate-y-px hover:bg-accent">
                      View <ArrowRight className="h-3 w-3 transition-transform group-hover:translate-x-0.5" />
                    </button>
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>

      {open && (
        <Drawer title={open.title} onClose={() => setOpen(null)}>
          <Field label="Type">{open.type}</Field>
          <Field label="Description">{open.description}</Field>
          <Field label="Original document">{docById[open.documentId]?.originalName}</Field>
          <Field label="Generated Markdown"><span className="font-mono">{docById[open.documentId]?.markdownName}</span></Field>
          <Field label="Validation"><StatusBadge status={open.status} />{open.issue && <p className="mt-2 text-sm text-muted-foreground">{open.issue}</p>}</Field>
          {open.sourcePage && <Field label="Source page">Page {open.sourcePage}</Field>}
          <button
            onClick={() => setMdDoc(docById[open.documentId])}
            className="lift mt-2 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground"
          >
            <FileText className="h-4 w-4" /> View Markdown
          </button>
        </Drawer>
      )}
      {mdDoc && (
        <Drawer title="Generated Markdown" onClose={() => setMdDoc(null)} wide>
          <p className="text-sm text-muted-foreground">Converted from: <span className="font-medium text-foreground">{mdDoc.originalName}</span> → <span className="font-mono">{mdDoc.markdownName}</span></p>
          <pre className="mt-2 whitespace-pre-wrap break-words rounded-2xl border bg-muted p-5 font-mono text-[13px] leading-relaxed">{mdDoc.markdown}</pre>
        </Drawer>
      )}
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <p className="text-xs uppercase tracking-widest text-muted-foreground">{label}</p>
      <div className="mt-1">{children}</div>
    </div>
  );
}

function Drawer({ title, onClose, children, wide }: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  return createPortal(
    <div className="fixed inset-0 z-40 flex justify-end font-sans text-foreground" role="dialog" aria-modal>
      <div className="absolute inset-0 bg-foreground/20 animate-fade-up" onClick={onClose} />
      <aside className={`relative flex h-full w-full flex-col bg-card shadow-lift animate-slide-in ${wide ? "max-w-2xl" : "max-w-md"}`}>
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="truncate font-display text-lg font-semibold">{title}</h2>
          <button onClick={onClose} aria-label="Close" className="rounded-lg p-1.5 hover:bg-muted"><X className="h-4 w-4" /></button>
        </div>
        <div className="flex-1 space-y-5 overflow-y-auto px-6 py-5">{children}</div>
      </aside>
    </div>,
    document.body,
  );
}
