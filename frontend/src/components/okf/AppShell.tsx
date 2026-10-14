
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

function Logo({ dark }: { dark: boolean }) {
  return (
    <div className="okf-brand flex items-center gap-3">
      <img
        src="/okf-logo.png"
        alt="OKF"
        className="h-12 w-12 shrink-0 object-contain"
      />

      <div className="flex flex-col leading-none">
        <span
          className={cn(
            "font-display text-xl font-bold tracking-[0.12em]",
            dark ? "text-white" : "text-foreground"
          )}
        >
          OKF
        </span>

        <span
          className={cn(
            "mt-1.5 text-[8px] font-semibold tracking-[0.18em]",
            dark ? "text-slate-300" : "text-muted-foreground"
          )}
        >
          OPEN KNOWLEDGE FORMAT
        </span>
      </div>
    </div>
  );
}

export function AppShell({ children }: { children: ReactNode }) {
  const [intro, setIntro] = useState(true);

  useEffect(() => {
    if (sessionStorage.getItem("okf-intro")) {
      setIntro(false);
    }
  }, []);

  const path = useRouterState({
    select: (s) => s.location.pathname,
  });

  return (
    <div
      className="okf-app-background relative min-h-screen font-sans text-foreground"
    >
      {intro && (
        <IntroLoader
          onDone={() => {
            sessionStorage.setItem("okf-intro", "1");
            setIntro(false);
          }}
        />
      )}

      <PageBackdrop path={path} />

      <header
        className="sticky top-0 z-30 border-b border-white/10 bg-[#080d1b]/75 backdrop-blur-xl"
      >
        <div className="mx-auto flex h-[76px] max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <Logo dark />

          <nav className="flex items-center gap-1 rounded-full border border-white/60 bg-white/95 p-1 shadow-soft">
            {NAV.map(({ to, label, icon: Icon }) => (
              <Link
                key={to}
                to={to}
                activeOptions={{ exact: true }}
                className={cn(
                  "flex items-center gap-2 rounded-full px-3 py-2 text-sm font-medium transition-all duration-200 hover:-translate-y-px sm:px-4",
                  "data-[status=active]:bg-accent data-[status=active]:text-accent-foreground",
                  "text-slate-600 hover:bg-slate-100 hover:text-slate-950"
                )}
              >
                <Icon className="h-4 w-4" />
                <span className="hidden sm:inline">{label}</span>
              </Link>
            ))}
          </nav>
        </div>
      </header>

      <main
        key={path}
        className="relative z-10 mx-auto min-h-[calc(100vh-76px)] max-w-6xl px-4 py-8 animate-fade-up sm:px-6 sm:py-12"
      >
        {children}
      </main>
    </div>
  );
}

function PageBackdrop({ path }: { path: string }) {
  /*if (path === "/") {
    return (
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 z-0 overflow-hidden"
      >
        <div className="absolute -top-40 left-1/2 h-[520px] w-[520px] -translate-x-1/2 rounded-full bg-accent opacity-50 blur-3xl" />
      </div>
    );
  }*/

  return (
    <div
      aria-hidden="true"
      className="okf-background-art pointer-events-none fixed inset-0 z-0 overflow-hidden"
    >
      {/* Main circle behind the page heading */}
      <div className="okf-orbit okf-orbit-main">
        <div className="okf-orbit-ring okf-orbit-ring-one" />
        <div className="okf-orbit-ring okf-orbit-ring-two" />
        <div className="okf-orbit-core" />
      </div>

      {/* Large layered circles in the upper-right */}
      <div className="okf-orbit okf-orbit-top-right">
        <div className="okf-orbit-ring okf-orbit-ring-one" />
        <div className="okf-orbit-ring okf-orbit-ring-two" />
        <div className="okf-orbit-core" />
      </div>

      {/* Lower-left layered circle */}
      <div className="okf-orbit okf-orbit-bottom-left">
        <div className="okf-orbit-ring okf-orbit-ring-one" />
        <div className="okf-orbit-ring okf-orbit-ring-two" />
        <div className="okf-orbit-core" />
      </div>

      {/* Lower-right blue accent */}
      <div className="okf-orbit okf-orbit-bottom-right">
        <div className="okf-orbit-ring okf-orbit-ring-one" />
        <div className="okf-orbit-ring okf-orbit-ring-two" />
        <div className="okf-orbit-core" />
      </div>

      <svg
        className="absolute inset-0 h-full w-full opacity-40"
        viewBox="0 0 1440 900"
        preserveAspectRatio="xMidYMid slice"
      >
        <circle
          cx="400"
          cy="150"
          r="205"
          fill="none"
          stroke="#087dff"
          strokeWidth="1.3"
        />
        <circle
          cx="400"
          cy="150"
          r="150"
          fill="none"
          stroke="#087dff"
          strokeWidth="1"
          strokeDasharray="3 7"
        />
        <circle
          cx="1440"
          cy="650"
          r="245"
          fill="none"
          stroke="#176bff"
          strokeWidth="1.2"
        />
        <circle
          cx="1440"
          cy="650"
          r="195"
          fill="none"
          stroke="#176bff"
          strokeWidth="1"
          strokeDasharray="3 8"
        />
      </svg>
    </div>
  );
}