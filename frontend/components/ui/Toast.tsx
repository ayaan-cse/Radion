"use client";

import { motion, AnimatePresence } from "framer-motion";
import { CheckCircle2, AlertCircle, Info } from "lucide-react";
import { useToast } from "@/hooks/useToast";

export function ToastContainer() {
  const { toasts } = useToast();

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-3 pointer-events-none">
      <AnimatePresence>
        {toasts.map((toast) => {
          const Icon = 
            toast.type === "success" ? CheckCircle2 : 
            toast.type === "error" ? AlertCircle : Info;
            
          const colorClass = 
            toast.type === "success" ? "text-semantic-green" : 
            toast.type === "error" ? "text-semantic-amber" : "text-semantic-blue";

          return (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: 20, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, scale: 0.95, transition: { duration: 0.2 } }}
              className="pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-2xl bg-glass backdrop-blur-glass border border-glass-border shadow-glass min-w-[250px]"
            >
              <Icon className={`w-5 h-5 ${colorClass}`} />
              <span className="text-sm font-medium text-white">{toast.message}</span>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
}