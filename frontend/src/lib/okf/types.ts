// OKF domain types. Kept open-ended (string unions widened) so the backend schema can evolve.

export type KnowledgeType = "Topic" | "Concept" | "Fact" | "Entity" | "Metric" | "Definition" | (string & {});
export type ValidationStatus = "valid" | "review" | "invalid" | "processing";

export type QueueStage =
  | "uploading"
  | "upload_failed"
  | "converting"
  | "converted"
  | "processing"
  | "processed"
  | "process_failed";

export interface QueuedFile {
  id: string;
  file: File;
  name: string;
  size: number;
  ext: string;
  stage: QueueStage;
  progress: number; // 0-100 for the current stage
  markdownName?: string | undefined;
  markdown?: string | undefined;
  error?: string | undefined;
}

export interface SourceDocument {
  id: string;
  originalName: string;
  markdownName: string;
  markdown: string;
  status: ValidationStatus;
  error?: string | undefined;
  createdAt: string;
}

export interface KnowledgeItem {
  id: string;
  documentId: string;
  title: string;
  type: KnowledgeType;
  description: string;
  status: ValidationStatus;
  sourcePage?: number | undefined;
  issue?: string | undefined;
  createdAt: string;
}

export interface ProcessResult {
  document: SourceDocument;
  items: KnowledgeItem[];
  termsIndexed: number;
}

export const PIPELINE_STEPS = [
  "File uploaded",
  "Text extracted",
  "Markdown generated",
  "Structuring knowledge",
  "Validating OKF",
  "Indexing knowledge",
] as const;
