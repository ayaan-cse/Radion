"use client";

import { GlassCard } from "@/components/ui/GlassCard";
import { Mail, BookOpen, MessageCircle } from "lucide-react";

export default function IntegrationsPage() {
  const handleConnect = (platform: string) => {
    // Redirects to the Spring Boot OAuth endpoint we built in Step 6A
    window.location.href = `http://localhost:8080/api/integrations/google/authorize?platform=${platform}`;
  };

  return (
    <div className="flex-1 flex flex-col pt-4">
      <h1 className="text-2xl font-bold text-white tracking-tight mb-2">Integrations</h1>
      <p className="text-sm text-white/60 mb-8">Connect your accounts to allow AI to monitor and extract tasks.</p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* Gmail Card */}
        <GlassCard className="p-6 flex flex-col items-start">
          <div className="w-12 h-12 rounded-xl bg-red-500/20 flex items-center justify-center mb-4 border border-red-500/30">
            <Mail className="w-6 h-6 text-red-400" />
          </div>
          <h3 className="text-lg font-bold text-white mb-1">Gmail</h3>
          <p className="text-sm text-white/60 mb-6 flex-1">Extract interviews, deadlines, and placement updates from your inbox.</p>
          <button onClick={() => handleConnect('GMAIL')} className="w-full py-2 rounded-lg bg-white/10 hover:bg-white/20 text-white text-sm font-medium transition-colors border border-white/10">
            Connect Gmail
          </button>
        </GlassCard>

        {/* Classroom Card */}
        <GlassCard className="p-6 flex flex-col items-start">
          <div className="w-12 h-12 rounded-xl bg-yellow-500/20 flex items-center justify-center mb-4 border border-yellow-500/30">
            <BookOpen className="w-6 h-6 text-yellow-400" />
          </div>
          <h3 className="text-lg font-bold text-white mb-1">Google Classroom</h3>
          <p className="text-sm text-white/60 mb-6 flex-1">Automatically sync assignments, due dates, and announcements.</p>
          <button onClick={() => handleConnect('CLASSROOM')} className="w-full py-2 rounded-lg bg-white/10 hover:bg-white/20 text-white text-sm font-medium transition-colors border border-white/10">
            Connect Classroom
          </button>
        </GlassCard>

        {/* WhatsApp Card */}
        <GlassCard className="p-6 flex flex-col items-start">
          <div className="w-12 h-12 rounded-xl bg-green-500/20 flex items-center justify-center mb-4 border border-green-500/30">
            <MessageCircle className="w-6 h-6 text-green-400" />
          </div>
          <h3 className="text-lg font-bold text-white mb-1">WhatsApp</h3>
          <p className="text-sm text-white/60 mb-6 flex-1">Monitor placement cell groups for urgent updates.</p>
          <button className="w-full py-2 rounded-lg bg-white/5 text-white/40 text-sm font-medium border border-white/5 cursor-not-allowed">
            Coming Soon
          </button>
        </GlassCard>
      </div>
    </div>
  );
}