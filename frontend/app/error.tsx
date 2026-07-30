"use client";
import { useEffect } from "react";
export default function Error({ error, reset }: { error: Error; reset: () => void }) {
  useEffect(() => { console.error("NEXT.JS ROOT ERROR:", error); }, [error]);
  return (
    <div style={{ padding: "2rem", backgroundColor: "white", color: "black", minHeight: "100vh" }}>
      <h2>CRITICAL RUNTIME ERROR</h2>
      <pre style={{ color: "red", whiteSpace: "pre-wrap" }}>{error.message}</pre>
      <pre style={{ fontSize: "12px", marginTop: "1rem" }}>{error.stack}</pre>
    </div>
  );
}