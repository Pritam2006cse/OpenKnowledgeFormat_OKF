import { createFileRoute } from "@tanstack/react-router";
import { KnowledgeUpload } from "@/components/okf/upload/KnowledgeUpload";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Upload Knowledge — OKF" },
      { name: "description", content: "Upload documents and turn them into structured, searchable OKF knowledge." },
      { property: "og:title", content: "Upload Knowledge — OKF" },
      { property: "og:description", content: "Upload documents and turn them into structured, searchable OKF knowledge." },
    ],
  }),
  component: KnowledgeUpload,
});
