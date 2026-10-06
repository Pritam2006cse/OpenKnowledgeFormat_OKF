// Mock OKF API. Replace each function body with a real fetch to the Java backend:
//   uploadFile   -> POST /upload
//   convertFile  -> (part of POST /upload or POST /convert)
//   processFile  -> POST /process
// Signatures stay the same so the UI does not change.
import type { KnowledgeItem, ProcessResult } from "./types";
const API_BASE_URL = "http://localhost:8080";

export interface SearchResult {
  id: string;
  title: string;
  score: number;
}

export async function searchKnowledge(
  query: string
): Promise<SearchResult[]> {

  const response = await fetch(
    `${API_BASE_URL}/api/search?q=${encodeURIComponent(query)}`
  );

  if (!response.ok) {
    throw new Error("Failed to search OKF backend");
  }

  return response.json();
}

export const SUPPORTED_EXTENSIONS = ["pdf", "docx", "txt", "md"];

const wait = (ms: number) => new Promise((r) => setTimeout(r, ms));
const uid = () => Math.random().toString(36).slice(2, 10);

export async function uploadFile(file: File, onProgress: (p: number) => void): Promise<void> {
  const ext = file.name.split(".").pop()?.toLowerCase() ?? "";
  for (let p = 0; p <= 100; p += 20) {
    onProgress(p);
    await wait(90);
  }
  if (!SUPPORTED_EXTENSIONS.includes(ext)) throw new Error(`Unsupported file type .${ext}`);
  if (file.size === 0) throw new Error("File is empty");
}

export async function convertFile(
  file: File,
  onProgress: (p: number) => void
): Promise<{ markdownName: string; markdown: string }> {

  const ext =
    file.name.split(".").pop()?.toLowerCase() ?? "";

  // Markdown and text files don't need PDF conversion
  if (ext === "md" || ext === "txt") {

    onProgress(20);

    const text = await file.text();

    onProgress(100);

    const base =
      file.name.replace(/\.[^.]+$/, "");

    const markdown =
      ext === "md"
        ? text
        : `# ${base.replace(/[_-]+/g, " ")}\n\n${text}`;

    return {
      markdownName: `${base}.md`,
      markdown,
    };
  }

  // PDF → real backend conversion
  if (ext === "pdf") {

    onProgress(10);

    const formData = new FormData();

    formData.append("file", file);

    onProgress(30);

    const response = await fetch(
      "http://localhost:8080/api/convert",
      {
        method: "POST",
        body: formData,
      }
    );

    onProgress(80);

    if (!response.ok) {

      const message =
        await response.text();

      throw new Error(
        message || "PDF conversion failed"
      );
    }

    const data =
      await response.json();

    onProgress(100);

    return {
      markdownName: data.markdownName,
      markdown: data.markdown,
    };
  }

  throw new Error(
    `Conversion for .${ext} is not implemented yet`
  );
}

export async function processFile(
  originalName: string,
  markdownName: string,
  markdown: string,
  onStep: (stepIndex: number) => void,
): Promise<ProcessResult> {
  for (let i = 0; i < 6; i++) {
    onStep(i);
    await wait(i < 3 ? 180 : 450);
  }
  onStep(6);
  const now = new Date().toISOString();
  const documentId = uid();
  const items = extractItems(markdown, documentId, now);
  const terms = new Set(markdown.toLowerCase().match(/[a-z]{3,}/g) ?? []);
  const anyIssue = items.some((i) => i.status !== "valid");
  return {
    document: { id: documentId, originalName, markdownName, markdown, status: anyIssue ? "review" : "valid", createdAt: now },
    items,
    termsIndexed: terms.size,
  };
}

const TYPES = ["Topic", "Concept", "Definition", "Fact", "Entity", "Metric"];

function extractItems(md: string, documentId: string, now: string): KnowledgeItem[] {
  const lines = md.split("\n");
  const items: KnowledgeItem[] = [];
  let current: { title: string; level: number; body: string[] } | null = null;
  const flush = () => {
    if (!current) return;
    const description = current.body.join(" ").replace(/\s+/g, " ").trim();
    const idx = items.length;
    const missing = description.length < 12;
    items.push({
      id: uid(),
      documentId,
      title: current.title,
      type: current.level === 1 ? "Topic" : /\d/.test(description.slice(0, 60)) ? "Metric" : TYPES[1 + (idx % 4)],
      description: description || "No content found under this heading.",
      status: missing ? "review" : "valid",
      issue: missing ? "Heading has little or no supporting content." : undefined,
      sourcePage: 1 + Math.floor(idx / 2),
      createdAt: now,
    });
  };
  for (const line of lines) {
    const m = line.match(/^(#{1,3})\s+(.*)/);
    if (m) {
      flush();
      current = { title: m[2].trim(), level: m[1].length, body: [] };
    } else if (current) current.body.push(line);
    else if (line.trim()) current = { title: "Introduction", level: 2, body: [line] };
  }
  flush();
  return items;
}
