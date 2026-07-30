"use client";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <html>
      <body>
        <div style={{ padding: "2rem", backgroundColor: "white", color: "black", minHeight: "100vh" }}>
          <h2>CRITICAL ROOT RUNTIME ERROR</h2>
          <pre style={{ color: "red", whiteSpace: "pre-wrap" }}>{error.message}</pre>
          <pre style={{ fontSize: "12px", marginTop: "1rem" }}>{error.stack}</pre>
        </div>
      </body>
    </html>
  );
}
