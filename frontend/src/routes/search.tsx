import { createFileRoute } from "@tanstack/react-router";
import { Search } from "lucide-react";
import { useState } from "react";

import {
  searchKnowledge,
  type SearchResult,
} from "@/lib/okf/api";

export const Route = createFileRoute("/search")({
  head: () => ({
    meta: [
      {
        title: "Search Knowledge — OKF",
      },
      {
        name: "description",
        content: "Search across all knowledge stored in OKF.",
      },
      {
        property: "og:title",
        content: "Search Knowledge — OKF",
      },
      {
        property: "og:description",
        content: "Search across all knowledge stored in OKF.",
      },
    ],
  }),
  component: SearchPage,
});

function SearchPage() {

  const [query, setQuery] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleSearch(
    event: React.FormEvent<HTMLFormElement>
  ) {
    event.preventDefault();

    if (!query.trim()) {
      return;
    }

    setLoading(true);
    setError("");
    setResults([]);

    try {

      const data = await searchKnowledge(query.trim());

      setResults(data);

    } catch (err) {

      setError(
        err instanceof Error
          ? err.message
          : "Something went wrong"
      );

    } finally {

      setLoading(false);

    }
  }

  return (
    <div className="space-y-8">

      {/* Header */}

      <div>
        <h1 className="font-display text-3xl font-semibold tracking-tight sm:text-4xl">
          Search
        </h1>

        <p className="mt-2 text-muted-foreground">
          Find information across everything OKF knows.
        </p>
      </div>


      {/* Search box */}

      <form
        onSubmit={handleSearch}
        className="flex gap-3"
      >

        <div className="relative flex-1">

          <Search
            className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-muted-foreground"
          />

          <input
            type="text"
            value={query}
            onChange={(event) =>
              setQuery(event.target.value)
            }
            placeholder="Search knowledge..."
            className="w-full rounded-2xl border bg-card px-12 py-4 outline-none transition focus:ring-2 focus:ring-primary"
          />

        </div>

        <button
          type="submit"
          disabled={loading || !query.trim()}
          className="rounded-2xl bg-primary px-6 py-4 font-medium text-primary-foreground transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {loading ? "Searching..." : "Search"}
        </button>

      </form>


      {/* Error */}

      {error && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-destructive">
          {error}
        </div>
      )}


      {/* No results */}

      {!loading &&
        !error &&
        query &&
        results.length === 0 && (
          <div className="rounded-3xl border bg-card px-6 py-16 text-center shadow-soft">

            <Search className="mx-auto h-10 w-10 text-muted-foreground" />

            <h2 className="mt-4 font-display text-xl font-semibold">
              No results found
            </h2>

            <p className="mt-2 text-muted-foreground">
              Try searching for another keyword.
            </p>

          </div>
        )}


      {/* Results */}

      {results.length > 0 && (

        <div className="space-y-4">

          <div className="flex items-center justify-between">

            <h2 className="font-display text-xl font-semibold">
              Search Results
            </h2>

            <span className="text-sm text-muted-foreground">
              {results.length} result
              {results.length !== 1 ? "s" : ""}
            </span>

          </div>


          {results.map((result, index) => (

            <div
              key={result.id}
              className="rounded-2xl border bg-card p-5 shadow-soft transition hover:shadow-md"
            >

              <div className="flex items-start justify-between gap-4">

                <div>

                  <div className="flex items-center gap-3">

                    <span className="text-sm font-medium text-muted-foreground">
                      #{index + 1}
                    </span>

                    <h3 className="font-display text-lg font-semibold">
                      {result.title}
                    </h3>

                  </div>

                  <p className="mt-2 text-sm text-muted-foreground">
                    Document ID: {result.id}
                  </p>

                </div>


                <div className="rounded-xl bg-accent px-3 py-2 text-sm font-medium">
                  Score: {result.score}
                </div>

              </div>

            </div>

          ))}

        </div>

      )}

    </div>
  );
}