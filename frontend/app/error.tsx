"use client";

import { GlassCard } from "@/components/ui/GlassCard";
import { AlertCircle } from "lucide-react";

export default function GlobalError({ error, reset }: { error: Error; reset: () => void }) {
  return (
    <div className="h-screen w-full flex items-center justify-center bg-[#0a0f16]">
      <GlassCard className="p-8 max-w-md text-center flex flex-col items-center">
        <AlertCircle className="w-12 h-12 text-semantic-amber mb-4" />
        <h2 className="text-white text-xl font-bold mb-2">Something went wrong!</h2>
        <p className="text-white/60 text-sm mb-6">{error.message}</p>
        <button 
          onClick={() => reset()}
          className="px-6 py-2 rounded-full bg-white/10 hover:bg-white/20 text-white text-sm font-medium transition-colors border border-white/10"
        >
          Try again
        </button>
      </GlassCard>
    </div>
  );
}