import { useEffect, useState } from "react";

/** One-time 2.4s intro: "OKF is preparing your knowledge workspace". Click to skip. */
export function IntroLoader({ onDone }: { onDone: () => void }) {
  const [leaving, setLeaving] = useState(false);
  useEffect(() => {
    const t1 = setTimeout(() => setLeaving(true), 2100);
    const t2 = setTimeout(onDone, 2500);
    return () => (clearTimeout(t1), clearTimeout(t2));
  }, [onDone]);

  return (
    <div
      onClick={onDone}
      className="fixed inset-0 z-50 grid place-items-center bg-intro-bg transition-opacity duration-400"
      style={{ opacity: leaving ? 0 : 1 }}
    >
      <div
        aria-hidden
        className="absolute inset-0 opacity-20"
        style={{
          backgroundImage:
            "linear-gradient(var(--intro-glow) 1px, transparent 1px), linear-gradient(90deg, var(--intro-glow) 1px, transparent 1px)",
          backgroundSize: "48px 48px",
          maskImage: "radial-gradient(circle at center, black, transparent 65%)",
        }}
      />
      <div className="relative flex flex-col items-center gap-8">
        <div className="relative grid h-24 w-24 place-items-center rounded-2xl border border-intro-glow/60 animate-pulse-ring">
          <span className="font-display text-3xl font-bold text-intro-glow" style={{ textShadow: "0 0 24px var(--intro-glow)" }}>
            OKF
          </span>
        </div>
        <div className="text-center">
          <p className="font-display text-lg tracking-wide text-intro-glow">Initializing OKF</p>
          <p className="mt-1 font-mono text-xs text-intro-glow/60">preparing your knowledge workspace…</p>
        </div>
        <div className="h-0.5 w-56 overflow-hidden rounded bg-intro-glow/15">
          <div className="h-full bg-intro-glow animate-grow" style={{ boxShadow: "0 0 12px var(--intro-glow)" }} />
        </div>
      </div>
    </div>
  );
}
