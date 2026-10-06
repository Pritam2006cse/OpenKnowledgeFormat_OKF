import { useEffect, useMemo, useState } from "react";
import { searchKnowledge, type BackendSearchResult } from "@/lib/okf/api";
import { Link } from "@tanstack/react-router";
import { AlertTriangle, ArrowRight, FileText, Search as SearchIcon, X } from "lucide-react";
import { useOkf } from "@/lib/okf/store";
import type { KnowledgeItem, SourceDocument } from "@/lib/okf/types";
import { StatusBadge } from "../StatusBadge";
import { Drawer, Field } from "../knowledge/KnowledgeLibrary";

/*// Client-side inverted index mirroring the Java InvertedIndex (TF-IDF ranking).
// Swap for GET /search when the backend is connected.
const STOP = new Set("the and for are with this that from was were has have not but you your into its our can".split(" "));
const tokenize = (t: string) => (t.toLowerCase().match(/[a-z0-9]{2,}/g) ?? []).filter((w) => !STOP.has(w));

function buildIndex(items: KnowledgeItem[]) {
  const t0 = performance.now();
  const index = new Map<string, Map<string, number>>();
  for (const it of items) {
    for (const w of tokenize(`${it.title} ${it.title} ${it.description}`)) {
      const post = index.get(w) ?? new Map<string, number>();
      post.set(it.id, (post.get(it.id) ?? 0) + 1);
      index.set(w, post);
    }
  }
  return { index, buildMs: performance.now() - t0 };
}*/

const selectCls = "h-10 rounded-xl border bg-card px-3 text-sm shadow-soft outline-none hover:border-primary/40 focus:ring-2 focus:ring-ring cursor-pointer";

/*function Highlight({ text, terms }: { text: string; terms: string[] }) {
  if (!terms.length) return <>{text}</>;
  const re = new RegExp(`(${terms.map((t) => t.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("|")})`, "gi");
  return <>{text.split(re).map((p, i) => (i % 2 ? <mark key={i} className="rounded bg-accent px-0.5 text-accent-foreground">{p}</mark> : p))}</>;
}*/

export function KnowledgeSearch() {
  const documents = useOkf((s) => s.documents);
  const items = useOkf((s) => s.items);
  const [q, setQ] = useState("");
  const [results, setResults] = useState<BackendSearchResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [searchError, setSearchError] = useState("");
  const [type, setType] = useState("all");
  const [status, setStatus] = useState("all");
  const [source, setSource] = useState("all");
  const [open, setOpen] = useState<KnowledgeItem | null>(null);
  const [mdDoc, setMdDoc] = useState<SourceDocument | null>(null);

  const docById = useMemo(() => Object.fromEntries(documents.map((d) => [d.id, d])), [documents]);
  const types = useMemo(() => Array.from(new Set(items.map((i) => i.type))), [items]);
  const failed = documents.filter((d) => d.status === "invalid");
  const reviews = items.filter((i) => i.status === "review");
  useEffect(() => {
  const query = q.trim();

  if (!query) {
    setResults([]);
    setSearchError("");
    return;
  }

  const timer = setTimeout(async () => {

    try {
      setSearching(true);
      setSearchError("");

      const data = await searchKnowledge(query);

      setResults(data);

    } catch (error) {

      console.error("Search error:", error);

      setSearchError(
        error instanceof Error
          ? error.message
          : "Search failed"
      );

      setResults([]);

    } finally {
      setSearching(false);
    }

  }, 250);

  return () => clearTimeout(timer);

}, [q]);

  /*const max = results[0]?.score || 1;
  const metrics: [string, ReactNode][] = [
    ["Indexed terms", index.size.toLocaleString()],
    ["Knowledge items", items.length],
    ["Index build", `${buildMs.toFixed(2)} ms`],
    ["Query latency", q ? `${queryMs.toFixed(2)} ms` : "—"],
    ["Results", q ? results.length : "—"],
    ["Coverage", items.length ? `${Math.round(((items.length - reviews.length) / items.length) * 100)}% valid` : "—"],
  ];*/

  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">Search</h1>
        <p className="mt-2 text-muted-foreground">Find information across everything OKF knows.</p>
      </div>

      <label className="relative block">
        <SearchIcon className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground" />
        <input
          autoFocus
          value={q}
          onChange={(e) => setQ(e.target.value)}
          placeholder={items.length ? "Search knowledge, e.g. revenue, definition, overview…" : "Upload documents to start searching"}
          className="h-14 w-full rounded-2xl border bg-card pl-12 pr-12 text-base shadow-soft outline-none focus:ring-2 focus:ring-ring"
        />
        {q && <button onClick={() => setQ("")} aria-label="Clear" className="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-1.5 hover:bg-muted"><X className="h-4 w-4" /></button>}
      </label>

      

      {(failed.length > 0 || reviews.length > 0) && (
        <section className="rounded-2xl border border-warning/40 bg-warning-soft p-4">
          <h2 className="flex items-center gap-2 text-sm font-semibold"><AlertTriangle className="h-4 w-4 text-warning" /> Processing issues</h2>
          <ul className="mt-2 space-y-1 text-sm">
            {failed.map((d) => <li key={d.id}><span className="font-medium">{d.originalName}</span> — {d.error ?? "Processing failed"}</li>)}
            {reviews.slice(0, 5).map((i) => (
              <li key={i.id}>
                <button onClick={() => setOpen(i)} className="text-left hover:underline"><span className="font-medium">{i.title}</span> — {i.issue ?? "Needs review"}</button>
              </li>
            ))}
            {reviews.length > 5 && <li className="text-muted-foreground">+{reviews.length - 5} more needing review</li>}
          </ul>
        </section>
      )}

      {!items.length ? (
        <div className="grid place-items-center rounded-3xl border bg-card px-6 py-16 text-center shadow-soft">
          <h2 className="font-display text-xl font-semibold">Nothing to search yet</h2>
          <p className="mt-1 text-muted-foreground">Upload and process documents to build the index.</p>
          <Link to="/" className="lift mt-6 inline-flex rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground">Upload Knowledge</Link>
        </div>
      ) : (
        <section className="space-y-4">
          <div className="grid grid-cols-3 gap-2 sm:max-w-xl">
            <select value={type} onChange={(e) => setType(e.target.value)} className={selectCls} aria-label="Type">
              <option value="all">All Types</option>
              {types.map((t) => <option key={t}>{t}</option>)}
            </select>
            <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectCls} aria-label="Status">
              <option value="all">All Status</option>
              <option value="valid">Valid</option>
              <option value="review">Needs Review</option>
            </select>
            <select value={source} onChange={(e) => setSource(e.target.value)} className={selectCls} aria-label="Source">
              <option value="all">All Sources</option>
              {documents.filter((d) => d.status !== "processing").map((d) => <option key={d.id} value={d.id}>{d.originalName}</option>)}
            </select>
          </div>

          {!q ? (

  <p className="rounded-2xl border bg-card p-8 text-center text-sm text-muted-foreground">
    Type a keyword to search {items.length} knowledge items.
  </p>

) : searching ? (

  <p className="rounded-2xl border bg-card p-8 text-center text-sm text-muted-foreground">
    Searching OKF...
  </p>

) : searchError ? (

  <p className="rounded-2xl border border-destructive/30 bg-card p-8 text-center text-sm text-destructive">
    {searchError}
  </p>

) : results.length === 0 ? (

  <p className="rounded-2xl border bg-card p-8 text-center text-sm text-muted-foreground">
    No results for “{q}”.
  </p>

) : (
            <ul className="space-y-3">

              {results.map((result, n) => {
                  const d = documents.find(
                    (doc) =>
                      doc.id === result.id ||
                      doc.markdownName === result.id ||
                      doc.originalName === result.id
                  );

                  const item = d
                    ? items.find((i) => i.documentId === d.id)
                    : undefined;

                  return (
                    <li
                      key={result.id}
                      className="lift animate-fade-up rounded-2xl border bg-card p-5 shadow-soft"
                      style={{
                        animationDelay: `${Math.min(n, 8) * 50}ms`,
                      }}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <h3 className="font-display text-lg font-semibold">
                          {item?.title ?? result.title}
                        </h3>

                        {item?.type && (
                          <span className="shrink-0 rounded-md bg-secondary px-2 py-0.5 text-xs font-medium text-secondary-foreground">
                            {item.type}
                          </span>
                        )}
                      </div>

                      <p className="mt-2 line-clamp-3 text-sm text-muted-foreground">
                        {result.snippet}
                      </p>

                      <p className="mt-3 flex flex-wrap items-center gap-1.5 font-mono text-xs text-muted-foreground">
                        {d?.originalName ?? result.id}

                        {item?.sourcePage
                          ? ` (p. ${item.sourcePage})`
                          : ""}

                        <ArrowRight className="h-3 w-3" />

                        {d?.markdownName ?? result.id}
                      </p>

                      <div className="mt-4 flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3">
                          {item?.status && (
                            <StatusBadge status={item.status} />
                          )}

                          <span className="font-mono text-xs text-muted-foreground">
                            Score: {result.score}
                          </span>
                        </div>

                        <div className="flex gap-2">
                          {item && (
                            <button
                              onClick={() => setOpen(item)}
                              className="rounded-lg border px-3 py-1.5 text-xs font-semibold hover:bg-accent"
                            >
                              View Knowledge
                            </button>
                          )}

                          {d && (
                            <button
                              onClick={() => setMdDoc(d)}
                              className="rounded-lg border px-3 py-1.5 text-xs font-semibold hover:bg-accent"
                            >
                              Preview Source
                            </button>
                          )}
                        </div>
                      </div>
                    </li>
                  );
                })}

            </ul>
          )}
        </section>
      )}

      {open && (
        <Drawer title={open.title} onClose={() => setOpen(null)}>
          <Field label="Type">{open.type}</Field>
          <Field label="Description">{open.description}</Field>
          <Field label="Original document">{docById[open.documentId]?.originalName}</Field>
          <Field label="Validation"><StatusBadge status={open.status} />{open.issue && <p className="mt-2 text-sm text-muted-foreground">{open.issue}</p>}</Field>
          {open.sourcePage && <Field label="Source page">Page {open.sourcePage}</Field>}
          <button onClick={() => setMdDoc(docById[open.documentId] ?? null)} className="lift inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground">
            <FileText className="h-4 w-4" /> Preview Source
          </button>
        </Drawer>
      )}
      {mdDoc && (
        <Drawer title={mdDoc.originalName} onClose={() => setMdDoc(null)} wide>
          <p className="text-sm text-muted-foreground">Converted to <span className="font-mono">{mdDoc.markdownName}</span></p>
          <pre className="whitespace-pre-wrap break-words rounded-2xl border bg-muted p-5 font-mono text-[13px] leading-relaxed">
            {mdDoc.markdown}
          </pre>
        </Drawer>
      )}
    </div>
  );
}
