import { useEffect, useRef, useState } from "react";

/** One-time 3.5s intro with animated 3D knowledge grid. */
export function IntroLoader({ onDone }: { onDone: () => void }) {
  const [leaving, setLeaving] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const progressRef = useRef<HTMLDivElement>(null);

  // --------------------------------------------------
  // EXISTING INTRO BUSINESS LOGIC
  // --------------------------------------------------

  useEffect(() => {
    const t1 = setTimeout(() => setLeaving(true), 3100);
    const t2 = setTimeout(onDone, 3500);

    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
    };
  }, [onDone]);

  // --------------------------------------------------
  // GRID ANIMATION ONLY
  // --------------------------------------------------

  useEffect(() => {
    const canvas = canvasRef.current;

    if (!canvas) return;

    const ctx = canvas.getContext("2d");

    if (!ctx) return;

    let animationFrame = 0;
    let startTime = performance.now();

    const resize = () => {
      const dpr = Math.min(
        window.devicePixelRatio || 1,
        2
      );

      canvas.width = window.innerWidth * dpr;
      canvas.height = window.innerHeight * dpr;

      canvas.style.width = `${window.innerWidth}px`;
      canvas.style.height = `${window.innerHeight}px`;

      ctx.setTransform(
        dpr,
        0,
        0,
        dpr,
        0,
        0
      );
    };

    resize();

    window.addEventListener(
      "resize",
      resize
    );

    const project = (
      x: number,
      y: number,
      z: number,
      width: number,
      height: number
    ) => {
      const perspective = 650;

      const scale =
        perspective /
        (perspective + z);

      return {
        x: width / 2 + x * scale,
        y: height * 0.56 + y * scale,
      };
    };

    const draw = (time: number) => {
      const width = window.innerWidth;
      const height = window.innerHeight;

      const elapsed =
        time - startTime;
      const animationDuration = 3100;
      const progress = Math.min(elapsed / animationDuration, 1);

      if (progressRef.current) {
        progressRef.current.style.transform = `scaleX(${progress})`;
      }
      ctx.clearRect(
        0,
        0,
        width,
        height
      );

      // Black background
      ctx.fillStyle = "#000";
      ctx.fillRect(
        0,
        0,
        width,
        height
      );

      const rows = 30;
      const columns = 70;

      const gridWidth =
        Math.min(
          width * 1.7,
          1500
        );

      const gridDepth = 900;

      const rowSpacing =
        gridDepth / rows;

      const columnSpacing =
        gridWidth / columns;

      const timeOffset =
        elapsed * 0.00022;

      // --------------------------------------------
      // HORIZONTAL GRID LINES
      // --------------------------------------------

      for (
        let row = 0;
        row < rows;
        row++
      ) {
        const points: {
          x: number;
          y: number;
          z: number;
        }[] = [];

        const z =
          row * rowSpacing - 100;

        for (
          let col = 0;
          col <= columns;
          col++
        ) {
          const x =
            col * columnSpacing -
            gridWidth / 2;

          const wave1 =
            Math.sin(
              x * 0.012 +
                timeOffset * 8 +
                row * 0.18
            ) * 75;

          const wave2 =
            Math.sin(
              x * 0.004 -
                timeOffset * 5
            ) * 35;

          const centerInfluence =
            Math.exp(
              -Math.pow(
                x /
                  (gridWidth * 0.34),
                2
              )
            );

          const wave3 =
            Math.sin(
              x * 0.018 +
                timeOffset * 4
            ) *
            70 *
            centerInfluence;

          points.push({
            x,
            y:
              wave1 +
              wave2 +
              wave3,
            z,
          });
        }

        for (
          let col = 0;
          col < points.length - 1;
          col++
        ) {
          const a =
            points[col];

          const b =
            points[col + 1];

          const pa =
            project(
              a.x,
              a.y,
              a.z,
              width,
              height
            );

          const pb =
            project(
              b.x,
              b.y,
              b.z,
              width,
              height
            );

          const t =
            a.x /
              gridWidth +
            0.5;

          const r =
            Math.round(
              255 * (1 - t)
            );

          const g =
            Math.round(
              210 * t
            );

          ctx.beginPath();

          ctx.moveTo(
            pa.x,
            pa.y
          );

          ctx.lineTo(
            pb.x,
            pb.y
          );

          ctx.strokeStyle =
            `rgba(${r}, ${g}, 255, 0.82)`;

          ctx.lineWidth = 0.75;

          ctx.stroke();
        }
      }

      // --------------------------------------------
      // VERTICAL GRID LINES
      // --------------------------------------------

      for (
        let col = 0;
        col <= columns;
        col++
      ) {
        const x =
          col * columnSpacing -
          gridWidth / 2;

        ctx.beginPath();

        for (
          let row = 0;
          row <= rows;
          row++
        ) {
          const z =
            row * rowSpacing -
            100;

          const wave1 =
            Math.sin(
              x * 0.012 +
                timeOffset * 8 +
                row * 0.18
            ) * 75;

          const wave2 =
            Math.sin(
              x * 0.004 -
                timeOffset * 5
            ) * 35;

          const centerInfluence =
            Math.exp(
              -Math.pow(
                x /
                  (gridWidth * 0.34),
                2
              )
            );

          const wave3 =
            Math.sin(
              x * 0.018 +
                timeOffset * 4
            ) *
            70 *
            centerInfluence;

          const y =
            wave1 +
            wave2 +
            wave3;

          const p =
            project(
              x,
              y,
              z,
              width,
              height
            );

          if (row === 0) {
            ctx.moveTo(
              p.x,
              p.y
            );
          } else {
            ctx.lineTo(
              p.x,
              p.y
            );
          }
        }

        const t =
          x /
            gridWidth +
          0.5;

        const r =
          Math.round(
            255 * (1 - t)
          );

        const g =
          Math.round(
            210 * t
          );

        ctx.strokeStyle =
          `rgba(${r}, ${g}, 255, 0.72)`;

        ctx.lineWidth = 0.7;

        ctx.stroke();
      }

      // --------------------------------------------
      // BOTTOM FADE
      // --------------------------------------------

      const gradient =
        ctx.createLinearGradient(
          0,
          height * 0.35,
          0,
          height
        );

      gradient.addColorStop(
        0,
        "rgba(0,0,0,0)"
      );

      gradient.addColorStop(
        0.82,
        "rgba(0,0,0,0.05)"
      );

      gradient.addColorStop(
        1,
        "rgba(0,0,0,0.75)"
      );

      ctx.fillStyle = gradient;

      ctx.fillRect(
        0,
        0,
        width,
        height
      );

      animationFrame =
        requestAnimationFrame(draw);
    };

    animationFrame =
      requestAnimationFrame(draw);

    return () => {
      cancelAnimationFrame(
        animationFrame
      );

      window.removeEventListener(
        "resize",
        resize
      );
    };
  }, []);

  // --------------------------------------------------
  // UI
  // --------------------------------------------------

  return (
    <div
      onClick={onDone}
      className="fixed inset-0 z-50 overflow-hidden bg-black transition-opacity duration-400"
      style={{
        opacity: leaving ? 0 : 1,
      }}
    >
      {/* Animated grid */}
      <canvas
        ref={canvasRef}
        className="absolute inset-0 h-full w-full"
      />

      {/* Existing OKF intro text */}
      <div className="relative z-10 flex h-full flex-col items-center justify-start pt-[10vh]">
        <div className="text-center">
          <div
            className="font-display text-4xl font-bold tracking-[0.18em]"
            style={{
              color: "white",
              textShadow:
                "0 0 24px rgba(0, 210, 255, 0.7)",
            }}
          >
            OKF
          </div>

          <p
            className="mt-3 font-display text-lg tracking-wide"
            style={{
              color:
                "rgba(255,255,255,0.92)",
            }}
          >
            Initializing OKF
          </p>

          <p
            className="mt-1 font-mono text-xs"
            style={{
              color:
                "rgba(255,255,255,0.48)",
            }}
          >
            preparing your knowledge workspace…
          </p>

          <div
            className="mx-auto mt-6 h-0.5 w-56 overflow-hidden rounded"
            style={{
              background: "rgba(255,255,255,0.12)",
            }}
          >
            <div
              ref={progressRef}
              className="h-full origin-left"
              style={{
                transform: "scaleX(0)",
                background:
                  "linear-gradient(90deg, #ff00ff, #7b5cff, #00d9ff)",
                boxShadow: "0 0 12px #00d9ff",
              }}
            />
          </div>
        </div>
      </div>
    </div>
  );
}