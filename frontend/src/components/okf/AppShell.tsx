import { Link, useRouterState } from "@tanstack/react-router";
import { Library, Search, UploadCloud } from "lucide-react";
import { useEffect, useState, type ReactNode } from "react";
import { IntroLoader } from "./IntroLoader";
import { cn } from "@/lib/utils";

const NAV = [
  { to: "/", label: "Upload", icon: UploadCloud },
  { to: "/knowledge", label: "Knowledge", icon: Library },
  { to: "/search", label: "Search", icon: Search },
] as const;

function Logo() {
  return (
    <div className="flex items-center gap-2">
      <div className="grid h-8 w-8 place-items-center rounded-lg bg-primary font-display text-sm font-bold text-primary-foreground">
        O
      </div>
      <span className="font-display text-lg font-semibold tracking-tight">OKF</span>
    </div>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  const [intro, setIntro] = useState(true);
  useEffect(() => {
    if (sessionStorage.getItem("okf-intro")) setIntro(false);
  }, []);
  const path = useRouterState({ select: (s) => s.location.pathname });

  return (
    <div className="min-h-screen bg-background text-foreground font-sans">
      {intro && (
        <IntroLoader
          onDone={() => {
            sessionStorage.setItem("okf-intro", "1");
            setIntro(false);
          }}
        />
      )}
      <PageBackdrop path={path} />
      <header className="sticky top-0 z-30 border-b bg-background/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <Logo />
          <nav className="flex items-center gap-1 rounded-xl border bg-card p-1 shadow-soft">
            {NAV.map(({ to, label, icon: Icon }) => (
              <Link
                key={to}
                to={to}
                activeOptions={{ exact: true }}
                className="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm font-medium text-muted-foreground transition-all duration-200 hover:-translate-y-px hover:bg-muted hover:text-foreground data-[status=active]:bg-accent data-[status=active]:text-accent-foreground"
              >
                <Icon className="h-4 w-4" />
                <span className="hidden sm:inline">{label}</span>
              </Link>
            ))}
          </nav>
        </div>
      </header>
      <main key={path} className="relative mx-auto max-w-6xl px-4 py-8 sm:px-6 sm:py-12 animate-fade-up">
        {children}
      </main>
    </div>
  );
}

/** Very subtle, page-specific decoration; never competes with content. */
function PageBackdrop({ path }: { path: string }) {
  return (
    <div aria-hidden className="pointer-events-none fixed inset-0 -z-0 overflow-hidden">
      <div
        className={cn(
          "absolute h-[520px] w-[520px] rounded-full bg-accent opacity-50 blur-3xl transition-all duration-1000",
          path === "/" && "-top-40 left-1/2 -translate-x-1/2",
          path === "/knowledge" && "-top-60 -right-40",
          path === "/search" && "top-1/3 -left-60",
        )}
      />
      <svg className="absolute right-8 bottom-8 h-64 w-64 text-primary opacity-[0.06]" viewBox="0 0 100 100">
        {[[20, 30], [50, 15], [80, 35], [35, 65], [70, 75], [50, 45]].map(([x, y], i, a) => (
          <g key={i}>
            <circle cx={x} cy={y} r="2.5" fill="currentColor" />
            <line x1={x} y1={y} x2={a[5][0]} y2={a[5][1]} stroke="currentColor" strokeWidth=".6" />
          </g>
        ))}
      </svg>
    </div>
  );
}
