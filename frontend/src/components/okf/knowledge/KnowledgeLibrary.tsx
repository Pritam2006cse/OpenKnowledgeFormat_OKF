import { useMemo, useState, type ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import { createPortal } from "react-dom";

import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  FileText,
  Filter,
  Library,
  Plus,
  Search,
  X,
} from "lucide-react";

import { useOkf } from "@/lib/okf/store";
import type {
  KnowledgeItem,
  SourceDocument,
  ValidationStatus,
} from "@/lib/okf/types";

import { StatusBadge, STATUS_LABEL } from "../StatusBadge";

const selectCls =
  "h-11 rounded-xl border border-primary/20 bg-black/20 px-3 text-sm text-foreground outline-none backdrop-blur-md transition focus:border-primary focus:ring-2 focus:ring-primary/20";

export function KnowledgeLibrary() {
  const documents = useOkf((s) => s.documents);
  const items = useOkf((s) => s.items);

  const [q, setQ] = useState("");
  const [type, setType] = useState("all");
  const [status, setStatus] = useState("all");
  const [source, setSource] = useState("all");

  const [open, setOpen] = useState<KnowledgeItem | null>(null);
  const [mdDoc, setMdDoc] = useState<SourceDocument | null>(null);

  const types = useMemo(
    () => Array.from(new Set(items.map((i) => i.type))),
    [items]
  );

  const docById = useMemo(
    () => Object.fromEntries(documents.map((d) => [d.id, d])),
    [documents]
  );

  const count = (s: ValidationStatus) =>
    items.filter((i) => i.status === s).length;

  const filtered = items.filter((i) => {
    const matchesType =
      type === "all" || i.type === type;

    const matchesStatus =
      status === "all" || i.status === status;

    const matchesSource =
      source === "all" || i.documentId === source;

    const matchesSearch =
      !q ||
      `${i.title} ${i.description}`
        .toLowerCase()
        .includes(q.toLowerCase());

    return (
      matchesType &&
      matchesStatus &&
      matchesSource &&
      matchesSearch
    );
  });

  const clearFilters = () => {
    setQ("");
    setType("all");
    setStatus("all");
    setSource("all");
  };

  const hasFilters =
    q || type !== "all" || status !== "all" || source !== "all";

  return (
    <div className="space-y-10">

      {/* ───────────────── HEADER ───────────────── */}

      <header className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">

        <div>
          <div className="mb-2 flex items-center gap-2 text-sm font-medium text-primary">
            <Library className="h-4 w-4" />
            Knowledge Base
          </div>

          <h1 className="font-display text-4xl font-semibold tracking-tight">
            Knowledge Library
          </h1>

          <p className="mt-2 max-w-2xl text-muted-foreground">
            Explore, search and verify the knowledge extracted from
            your documents.
          </p>
        </div>

        <Link
          to="/"
          className="lift inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-primary px-5 text-sm font-semibold text-primary-foreground"
        >
          <Plus className="h-4 w-4" />
          Upload Knowledge
        </Link>

      </header>


      {/* ───────────────── STATS ───────────────── */}

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">

        <StatCard
          label="Documents"
          value={documents.length}
          icon={<FileText className="h-4 w-4" />}
          active={status === "all" && type === "all"}
          onClick={() => {
            setStatus("all");
            setType("all");
          }}
        />

        <StatCard
          label="Knowledge Items"
          value={items.length}
          icon={<Library className="h-4 w-4" />}
          active={status === "all"}
          onClick={() => setStatus("all")}
        />

        <StatCard
          label="Valid"
          value={count("valid")}
          icon={<CheckCircle2 className="h-4 w-4" />}
          active={status === "valid"}
          onClick={() => setStatus("valid")}
        />

        <StatCard
          label="Needs Review"
          value={count("review")}
          icon={<AlertTriangle className="h-4 w-4" />}
          active={status === "review"}
          onClick={() => setStatus("review")}
        />

      </section>


      {/* ───────────────── SEARCH / FILTER ───────────────── */}

      <section className="glass rounded-2xl p-4">

        <div className="mb-3 flex items-center justify-between">

          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4 text-primary" />

            <span className="text-sm font-semibold">
              Browse knowledge
            </span>
          </div>

          {hasFilters && (
            <button
              onClick={clearFilters}
              className="text-xs font-medium text-muted-foreground hover:text-foreground"
            >
              Clear filters
            </button>
          )}

        </div>

        <div className="grid gap-3 lg:grid-cols-[1fr_160px_160px_200px]">

          {/* Search */}

          <label className="relative block">

            <Search
              className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
            />

            <input
              value={q}
              onChange={(e) => setQ(e.target.value)}
              placeholder="Search knowledge..."
              className="h-11 w-full rounded-xl border border-primary/20 bg-black/20 pl-10 pr-3 text-sm text-foreground outline-none backdrop-blur-md placeholder:text-muted-foreground transition focus:border-primary focus:ring-2 focus:ring-primary/20"
            />

          </label>

          {/* Type */}

          <select
            value={type}
            onChange={(e) => setType(e.target.value)}
            className={selectCls}
            aria-label="Filter by type"
          >
            <option value="all">All Types</option>

            {types.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>

          {/* Status */}

          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className={selectCls}
            aria-label="Filter by status"
          >
            <option value="all">All Status</option>

            {(["valid", "review", "invalid"] as const).map((s) => (
              <option key={s} value={s}>
                {STATUS_LABEL[s]}
              </option>
            ))}
          </select>

          {/* Source */}

          <select
            value={source}
            onChange={(e) => setSource(e.target.value)}
            className={selectCls}
            aria-label="Filter by source"
          >
            <option value="all">All Sources</option>

            {documents
              .filter((d) => d.status !== "processing")
              .map((d) => (
                <option key={d.id} value={d.id}>
                  {d.originalName}
                </option>
              ))}
          </select>

        </div>

      </section>


      {/* ───────────────── RESULTS HEADER ───────────────── */}

      <section>

        <div className="mb-4 flex items-end justify-between">

          <div>
            <h2 className="font-display text-xl font-semibold">
              Knowledge
            </h2>

            <p className="mt-1 text-sm text-muted-foreground">
              {filtered.length}{" "}
              {filtered.length === 1 ? "item" : "items"} found
            </p>
          </div>

        </div>


        {/* ───────────────── KNOWLEDGE CARDS ───────────────── */}

        {filtered.length === 0 ? (

          <div className="rounded-2xl border bg-card px-6 py-16 text-center shadow-soft">

            <div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-muted">
              <Search className="h-5 w-5 text-muted-foreground" />
            </div>

            <h3 className="mt-4 font-display text-lg font-semibold">
              No matching knowledge
            </h3>

            <p className="mt-1 text-sm text-muted-foreground">
              Try changing your search or filters.
            </p>

            {hasFilters && (
              <button
                onClick={clearFilters}
                className="mt-5 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
              >
                Clear filters
              </button>
            )}

          </div>

        ) : (

          <div className="grid gap-4 md:grid-cols-2">

            {filtered.map((item) => {

              const doc = docById[item.documentId];

              return (
                <KnowledgeCard
                  key={item.id}
                  item={item}
                  document={doc}
                  onView={() => setOpen(item)}
                />
              );

            })}

          </div>

        )}

      </section>


      {/* ───────────────── SOURCES ───────────────── */}

      {documents.length > 0 && (

        <section>

          <div className="mb-4">
            <h2 className="font-display text-xl font-semibold">
              Sources
            </h2>

            <p className="mt-1 text-sm text-muted-foreground">
              Documents currently contributing to the knowledge base.
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">

            {documents.map((doc) => {

              const itemCount =
                items.filter(
                  (i) => i.documentId === doc.id
                ).length;

              return (
                <div
                  key={doc.id}
                  className="glass rounded-2xl p-4"
                >

                  <div className="flex items-start justify-between gap-3">

                    <div className="min-w-0">

                      <div className="flex items-center gap-2">

                        <FileText className="h-4 w-4 shrink-0 text-primary" />

                        <p className="truncate text-sm font-semibold">
                          {doc.originalName}
                        </p>

                      </div>

                      <p className="mt-2 truncate font-mono text-xs text-muted-foreground">
                        {doc.markdownName}
                      </p>

                    </div>

                    <StatusBadge status={doc.status} />

                  </div>

                  <div className="mt-4 flex items-center justify-between border-t pt-3">

                    <span className="text-xs text-muted-foreground">
                      {itemCount}{" "}
                      {itemCount === 1
                        ? "knowledge item"
                        : "knowledge items"}
                    </span>

                    {doc.status !== "processing" && (
                      <button
                        onClick={() => setMdDoc(doc)}
                        className="text-xs font-semibold text-primary hover:underline"
                      >
                        View Markdown
                      </button>
                    )}

                  </div>

                </div>
              );
            })}

          </div>

        </section>

      )}


      {/* ───────────────── KNOWLEDGE DRAWER ───────────────── */}

      {open && (
        <Drawer
          title={open.title}
          onClose={() => setOpen(null)}
        >

          <Field label="Type">
            {open.type}
          </Field>

          <Field label="Description">
            {cleanMarkdownPreview(open.description)}
          </Field>

          <Field label="Original document">
            {docById[open.documentId]?.originalName}
          </Field>

          <Field label="Generated Markdown">
            <span className="font-mono">
              {docById[open.documentId]?.markdownName}
            </span>
          </Field>

          <Field label="Validation">
            <StatusBadge status={open.status} />

            {open.issue && (
              <p className="mt-2 text-sm text-muted-foreground">
                {open.issue}
              </p>
            )}
          </Field>

          {open.sourcePage && (
            <Field label="Source page">
              Page {open.sourcePage}
            </Field>
          )}

          <button
            onClick={() => {
              setOpen(null);
              setMdDoc(docById[open.documentId]);
            }}
            className="lift mt-2 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground"
          >
            <FileText className="h-4 w-4" />
            View Markdown
          </button>

        </Drawer>
      )}


      {/* ───────────────── MARKDOWN DRAWER ───────────────── */}

      {mdDoc && (
        <Drawer
          title="Generated Markdown"
          onClose={() => setMdDoc(null)}
          wide
        >

          <p className="text-sm text-muted-foreground">
            Converted from{" "}
            <span className="font-medium text-foreground">
              {mdDoc.originalName}
            </span>{" "}
            →{" "}
            <span className="font-mono">
              {mdDoc.markdownName}
            </span>
          </p>

          <pre className="mt-2 whitespace-pre-wrap break-words rounded-2xl border bg-muted p-5 font-mono text-[13px] leading-relaxed">
            {mdDoc.markdown}
          </pre>

        </Drawer>
      )}

    </div>
  );
}


/* ───────────────── STAT CARD ───────────────── */

function StatCard({
  label,
  value,
  icon,
  active,
  onClick,
}: {
  label: string;
  value: number;
  icon: ReactNode;
  active?: boolean;
  onClick?: () => void;
}) {
  return (
    <button
      onClick={onClick}
      className={`glass group rounded-2xl p-4 text-left transition-all hover:-translate-y-0.5 ${
        active ? "border-primary/40 ring-1 ring-primary/20" : ""
      }`}
    >

      <div className="flex items-center justify-between">

        <span className="text-xs font-medium uppercase tracking-widest text-muted-foreground">
          {label}
        </span>

        <span className="rounded-lg bg-muted p-2 text-primary transition group-hover:bg-primary/10">
          {icon}
        </span>

      </div>

      <p className="mt-3 font-display text-3xl font-semibold">
        {value}
      </p>

    </button>
  );
}


/* ───────────────── KNOWLEDGE CARD ───────────────── */

function KnowledgeCard({
  item,
  document,
  onView,
}: {
  item: KnowledgeItem;
  document?: SourceDocument;
  onView: () => void;
}) {
  return (
    <article className="glass group flex min-h-[220px] flex-col rounded-2xl p-5 transition-all hover:-translate-y-0.5">

      {/* Top */}

      <div className="flex items-start justify-between gap-4">

        <div className="min-w-0">

          <p className="mb-2 text-[11px] font-semibold uppercase tracking-widest text-muted-foreground">
            Knowledge Item
          </p>

          <h3 className="font-display text-xl font-semibold leading-tight">
            {cleanMarkdownPreview(item.title)}
          </h3>

        </div>

        <span className="shrink-0 rounded-full bg-accent px-2.5 py-1 text-xs font-medium text-accent-foreground">
          {item.type}
        </span>

      </div>


      {/* Description */}

      <p className="mt-4 line-clamp-3 text-sm leading-6 text-muted-foreground">
        {cleanMarkdownPreview(item.description)}
      </p>


      {/* Spacer */}

      <div className="flex-1" />


      {/* Metadata */}

      <div className="mt-5 border-t pt-4">

        <div className="flex items-center justify-between gap-3">

          <div className="min-w-0">

            <p className="truncate text-xs font-medium text-foreground">
              {document?.originalName}
            </p>

            <p className="mt-1 truncate font-mono text-[11px] text-muted-foreground">
              {document?.markdownName}
              {item.sourcePage
                ? ` · Page ${item.sourcePage}`
                : ""}
            </p>

          </div>

          <StatusBadge status={item.status} />

        </div>


        <button
          onClick={onView}
          className="mt-4 flex w-full items-center justify-between rounded-xl bg-muted px-3 py-2.5 text-xs font-semibold transition hover:bg-accent"
        >

          <span>View knowledge</span>

          <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />

        </button>

      </div>

    </article>
  );
}


/* ───────────────── MARKDOWN CLEANUP ───────────────── */

function cleanMarkdownPreview(text: string): string {
  return text
    .replace(/^#{1,6}\s*/gm, "")
    .replace(/^#{1,6}\d+\s*/gm, "")
    .replace(/(\*\*|__|\*|_|~~)/g, "")
    .replace(/`([^`]+)`/g, "$1")
    .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
    .replace(/\s+/g, " ")
    .trim();
}


/* ───────────────── FIELD ───────────────── */

function Field({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div>
      <p className="text-xs uppercase tracking-widest text-muted-foreground">
        {label}
      </p>

      <div className="mt-1">
        {children}
      </div>
    </div>
  );
}


/* ───────────────── DRAWER ───────────────── */

function Drawer({
  title,
  onClose,
  children,
  wide,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
  wide?: boolean;
}) {
  return createPortal(
    <div
      className="fixed inset-0 z-40 flex justify-end font-sans text-foreground"
      role="dialog"
      aria-modal
    >

      <div
        className="absolute inset-0 bg-foreground/20 animate-fade-up"
        onClick={onClose}
      />

      <aside
        className={`relative flex h-full w-full flex-col bg-card shadow-lift animate-slide-in ${
          wide ? "max-w-3xl" : "max-w-md"
        }`}
      >

        <div className="flex items-center justify-between border-b px-6 py-4">

          <h2 className="truncate font-display text-lg font-semibold">
            {title}
          </h2>

          <button
            onClick={onClose}
            aria-label="Close"
            className="rounded-lg p-1.5 hover:bg-muted"
          >
            <X className="h-4 w-4" />
          </button>

        </div>

        <div className="flex-1 space-y-5 overflow-y-auto px-6 py-5">
          {children}
        </div>

      </aside>

    </div>,
    document.body
  );
}