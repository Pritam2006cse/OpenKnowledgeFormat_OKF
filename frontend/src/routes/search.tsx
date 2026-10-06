import { createFileRoute } from "@tanstack/react-router";
import { KnowledgeSearch } from "@/components/okf/search/KnowledgeSearch";

export const Route = createFileRoute("/search")({
  head: () => ({
    meta: [
      { title: "Search Knowledge — OKF" },
      { name: "description", content: "Search across all knowledge stored in OKF." },
      { property: "og:title", content: "Search Knowledge — OKF" },
      { property: "og:description", content: "Search across all knowledge stored in OKF." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary" },
    ],
  }),
  component: KnowledgeSearch,
});
