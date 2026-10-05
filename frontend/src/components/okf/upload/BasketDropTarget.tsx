import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

/** Visual "knowledge basket" — reacts to drag state. Not a delete action. */
export function BasketDropTarget({ state }: { state: "idle" | "armed" | "over" | "success" }) {
  const open = state === "armed" || state === "over";
  return (
    <div className="relative h-36 w-36 shrink-0 sm:h-44 sm:w-44" aria-hidden>
      <svg viewBox="0 0 120 120" className="h-full w-full text-primary">
        {/* lid */}
        <g
          className="transition-transform duration-300 ease-out"
          style={{
            transformOrigin: "22px 40px",
            transform: open ? `rotate(${state === "over" ? -32 : -18}deg) translateY(-4px)` : "none",
          }}
        >
          <rect x="18" y="34" width="84" height="8" rx="4" fill="currentColor" opacity=".85" />
          <rect x="50" y="27" width="20" height="8" rx="3" fill="currentColor" opacity=".6" />
        </g>
        {/* body */}
        <path
          d="M26 46h68l-7 56a8 8 0 0 1-8 7H41a8 8 0 0 1-8-7z"
          fill="currentColor"
          className={cn("transition-opacity duration-300", state === "idle" ? "opacity-15" : "opacity-25")}
        />
        <path d="M26 46h68l-7 56a8 8 0 0 1-8 7H41a8 8 0 0 1-8-7z" fill="none" stroke="currentColor" strokeWidth="2.5" />
        {[46, 60, 74].map((x) => (
          <line key={x} x1={x} y1="58" x2={x - 1} y2="96" stroke="currentColor" strokeWidth="2" strokeLinecap="round" opacity=".5" />
        ))}
        {/* incoming doc */}
        <g
          className="transition-all duration-300"
          style={{ opacity: state === "over" ? 1 : 0, transform: state === "over" ? "translateY(0)" : "translateY(-14px)" }}
        >
          <rect x="48" y="6" width="24" height="30" rx="3" fill="var(--card)" stroke="currentColor" strokeWidth="2" />
          <line x1="53" y1="15" x2="67" y2="15" stroke="currentColor" strokeWidth="1.5" />
          <line x1="53" y1="21" x2="64" y2="21" stroke="currentColor" strokeWidth="1.5" />
        </g>
      </svg>
      {state === "success" && (
        <div className="absolute inset-0 grid place-items-center">
          <div className="grid h-12 w-12 place-items-center rounded-full bg-primary text-primary-foreground shadow-lift animate-pop">
            <Check className="h-6 w-6" />
          </div>
        </div>
      )}
    </div>
  );
}
