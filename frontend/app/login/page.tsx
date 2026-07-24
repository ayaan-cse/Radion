"use client";

import { signIn } from "next-auth/react";
import { GlassCard } from "@/components/ui/GlassCard";
import { motion } from "framer-motion";

export default function LoginPage() {
  return (
    <div className="relative h-screen w-full flex items-center justify-center overflow-hidden bg-[#0a0f16] font-sans">
      <div 
        className="absolute inset-0 z-0 bg-cover bg-center bg-no-repeat opacity-60"
        style={{ backgroundImage: "url('/placeholder-mountains-bg.jpg')" }} 
      />
      
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: "easeOut" }}
        className="z-10 w-full max-w-md"
      >
        <GlassCard className="p-10 flex flex-col items-center text-center">
          <div className="w-16 h-16 rounded-2xl bg-white/10 border border-white/20 flex items-center justify-center mb-6 shadow-glass-sm">
            <span className="text-2xl font-bold text-white">R</span>
          </div>
          <h1 className="text-2xl font-bold text-white tracking-tight mb-2">Welcome to Radion</h1>
          <p className="text-sm text-white/60 mb-8">Eliminate notification overload with AI.</p>
          
          <button 
            onClick={() => signIn("google", { callbackUrl: "/" })}
            className="w-full flex items-center justify-center gap-3 px-6 py-3 rounded-full bg-white text-black text-sm font-semibold hover:bg-white/90 transition-colors focus-visible:ring-2 focus-visible:ring-white/50 outline-none"
          >
            <img src="https://www.svgrepo.com/show/475656/google-color.svg" alt="Google" className="w-5 h-5" />
            Continue with Google
          </button>
        </GlassCard>
      </motion.div>
    </div>
  );
}