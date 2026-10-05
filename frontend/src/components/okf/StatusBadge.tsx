import { AlertTriangle, CheckCircle2, Loader2, XCircle } from "lucide-react";
import type { ValidationStatus } from "@/lib/okf/types";
import { cn } from "@/lib/utils";

const MAP = {
  valid: { label: "Valid", icon: CheckCircle2, cls: "bg-success-soft text-accent-foreground" },
  review: { label: "Needs Review", icon: AlertTriangle, cls: "bg-warning-soft text-foreground" },
  invalid: { label: "Invalid", icon: XCircle, cls: "bg-destructive-soft text-destructive" },
  processing: { label: "Processing", icon: Loader2, cls: "bg-muted text-muted-foreground" },
} as const;

export const STATUS_LABEL = Object.fromEntries(Object.entries(MAP).map(([k, v]) => [k, v.label])) as Record<ValidationStatus, string>;

export function StatusBadge({ status }: { status: ValidationStatus }) {
  const { label, icon: Icon, cls } = MAP[status];
  return (
    <span className={cn("inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium", cls)}>
      <Icon className={cn("h-3.5 w-3.5", status === "processing" && "animate-spin", status === "review" && "text-warning")} />
      {label}
    </span>
  );
}
