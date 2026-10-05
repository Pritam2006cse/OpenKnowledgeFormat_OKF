import { createFileRoute } from "@tanstack/react-router";
import { KnowledgeLibrary } from "@/components/okf/knowledge/KnowledgeLibrary";

export const Route = createFileRoute("/knowledge")({
  head: () => ({
    meta: [
      { title: "Knowledge Library — OKF" },
      { name: "description", content: "Explore and verify the knowledge created from your documents." },
      { property: "og:title", content: "Knowledge Library — OKF" },
      { property: "og:description", content: "Explore and verify the knowledge created from your documents." },
    ],
  }),
  component: KnowledgeLibrary,
});
