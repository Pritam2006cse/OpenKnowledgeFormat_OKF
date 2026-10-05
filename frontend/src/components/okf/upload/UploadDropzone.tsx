import { useEffect, useRef, useState } from "react";
import { UploadCloud } from "lucide-react";
import { SUPPORTED_EXTENSIONS } from "@/lib/okf/api";
import { BasketDropTarget } from "./BasketDropTarget";
import { cn } from "@/lib/utils";

export function UploadDropzone({ onFiles }: { onFiles: (f: File[]) => void }) {
  const input = useRef<HTMLInputElement>(null);
  const [pageDrag, setPageDrag] = useState(false); // file dragged anywhere on page
  const [over, setOver] = useState(false); // file over the zone
  const [justDropped, setJustDropped] = useState(false);

  useEffect(() => {
    let depth = 0;
    const enter = (e: DragEvent) => {
      if (e.dataTransfer?.types.includes("Files")) (depth++, setPageDrag(true));
    };
    const leave = () => {
      depth = Math.max(0, depth - 1);
      if (!depth) setPageDrag(false);
    };
    const end = () => ((depth = 0), setPageDrag(false));
    const prevent = (e: DragEvent) => e.preventDefault();
    window.addEventListener("dragenter", enter);
    window.addEventListener("dragleave", leave);
    window.addEventListener("drop", end);
    window.addEventListener("dragover", prevent);
    window.addEventListener("drop", prevent);
    return () => {
      window.removeEventListener("dragenter", enter);
      window.removeEventListener("dragleave", leave);
      window.removeEventListener("drop", end);
      window.removeEventListener("dragover", prevent);
      window.removeEventListener("drop", prevent);
    };
  }, []);

  const accept = (files: File[]) => {
    if (!files.length) return;
    onFiles(files);
    setJustDropped(true);
    setTimeout(() => setJustDropped(false), 1200);
  };

  const state = justDropped ? "success" : over ? "over" : pageDrag ? "armed" : "idle";

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={() => input.current?.click()}
      onKeyDown={(e) => (e.key === "Enter" || e.key === " ") && input.current?.click()}
      onDragOver={(e) => (e.preventDefault(), setOver(true))}
      onDragLeave={() => setOver(false)}
      onDrop={(e) => {
        e.preventDefault();
        setOver(false);
        setPageDrag(false);
        accept(Array.from(e.dataTransfer.files));
      }}
      className={cn(
        "group relative flex cursor-pointer flex-col items-center gap-6 rounded-3xl border-2 border-dashed bg-card px-6 py-10 text-center shadow-soft outline-none transition-all duration-300 sm:flex-row sm:justify-between sm:px-12 sm:py-14 sm:text-left focus-visible:ring-2 focus-visible:ring-ring",
        state === "idle" && "border-border hover:border-primary/50 hover:bg-accent/30",
        state === "armed" && "border-primary/60 bg-accent/30",
        state === "over" && "scale-[1.01] border-primary bg-accent/60",
        state === "success" && "border-primary bg-success-soft",
      )}
    >
      <input
        ref={input}
        type="file"
        multiple
        hidden
        accept={SUPPORTED_EXTENSIONS.map((e) => "." + e).join(",")}
        onChange={(e) => {
          accept(Array.from(e.target.files ?? []));
          e.target.value = "";
        }}
      />
      <div className="flex flex-col items-center gap-4 sm:items-start">
        <div className="grid h-12 w-12 place-items-center rounded-xl bg-accent text-accent-foreground transition-transform duration-300 group-hover:-translate-y-1">
          <UploadCloud className="h-6 w-6" />
        </div>
        <div>
          <p className="font-display text-2xl font-semibold tracking-tight sm:text-3xl">
            {state === "success" ? "Received" : state === "over" ? "Release to add" : "Drop files here"}
          </p>
          <p className="mt-1 text-muted-foreground">
            or <span className="font-semibold text-primary underline-offset-4 group-hover:underline">choose files</span>
          </p>
        </div>
        <p className="text-xs font-medium uppercase tracking-widest text-muted-foreground">
          Supported: {SUPPORTED_EXTENSIONS.map((e) => e.toUpperCase()).join(" • ")}
        </p>
      </div>
      <BasketDropTarget state={state} />
    </div>
  );
}
