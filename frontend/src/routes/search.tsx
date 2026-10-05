import { createFileRoute } from "@tanstack/react-router";
import { Search } from "lucide-react";

export const Route = createFileRoute("/search")({
  head: () => ({
    meta: [
      { title: "Search Knowledge — OKF" },
      { name: "description", content: "Search across all knowledge stored in OKF." },
      { property: "og:title", content: "Search Knowledge — OKF" },
      { property: "og:description", content: "Search across all knowledge stored in OKF." },
    ],
  }),
  component: SearchPage,
});

function SearchPage() {
  return (
    <div className="space-y-8">
      <div>
        <h1 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">Search</h1>
        <p className="mt-2 text-muted-foreground">Find information across everything OKF knows.</p>
      </div>
      <div className="grid place-items-center rounded-3xl border bg-card px-6 py-20 text-center shadow-soft">
        <div className="grid h-14 w-14 place-items-center rounded-2xl bg-accent text-accent-foreground"><Search className="h-7 w-7" /></div>
        <h2 className="mt-4 font-display text-xl font-semibold">Coming next</h2>
        <p className="mt-1 text-muted-foreground">The search screen will be built in the next step.</p>
      </div>
    </div>
  );
}
