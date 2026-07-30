"use client";

import { useSession, signOut } from "next-auth/react";
import { GlassCard } from "@/components/ui/GlassCard";
import { LogOut, User, Mail, ShieldCheck } from "lucide-react";

export default function ProfilePage() {
  const { data: session, status } = useSession();

  if (status === "loading") {
    return <div className="p-8 text-white/60">Loading profile...</div>;
  }

  return (
    <div className="flex-1 flex flex-col pt-4 overflow-y-auto custom-scrollbar pr-4 max-w-3xl">
      <h1 className="text-2xl font-bold text-white tracking-tight mb-2">Website Identity</h1>
      <p className="text-sm text-white/60 mb-8">
        This identity is used solely for logging into Radion and managing account authorization and data ownership. It is never used as your connected service accounts.
      </p>

      <GlassCard className="p-8 flex flex-col gap-6">
        <div className="flex items-center gap-6 pb-6 border-b border-white/10">
          <div className="relative w-20 h-20 rounded-full overflow-hidden border-2 border-white/20 bg-white/10 shrink-0 flex items-center justify-center">
            {session?.user?.image ? (
              <img src={session.user.image} alt={session.user.name || "User Avatar"} className="w-full h-full object-cover" />
            ) : (
              <span className="text-2xl font-bold text-white">{session?.user?.name?.[0] || "U"}</span>
            )}
          </div>
          <div className="flex flex-col">
            <div className="flex items-center gap-2">
              <h2 className="text-xl font-bold text-white">{session?.user?.name || "Radion User"}</h2>
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-xs font-bold uppercase tracking-wider">
                <ShieldCheck className="w-3 h-3" />
                Authenticated
              </span>
            </div>
            <p className="text-sm text-zinc-400 mt-1">{session?.user?.email || "No email available"}</p>
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="flex items-center justify-between py-2 border-b border-white/5">
            <div className="flex items-center gap-3 text-zinc-300">
              <User className="w-4 h-4 text-zinc-400" />
              <span className="text-sm font-medium">Full Name</span>
            </div>
            <span className="text-sm text-white font-semibold">{session?.user?.name || "N/A"}</span>
          </div>

          <div className="flex items-center justify-between py-2 border-b border-white/5">
            <div className="flex items-center gap-3 text-zinc-300">
              <Mail className="w-4 h-4 text-zinc-400" />
              <span className="text-sm font-medium">Website Email</span>
            </div>
            <span className="text-sm text-white font-semibold">{session?.user?.email || "N/A"}</span>
          </div>
        </div>

        <div className="pt-4 flex justify-end">
          <button
            onClick={() => signOut({ callbackUrl: "/login" })}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 text-sm font-bold transition-all shadow-sm hover:scale-[1.02] active:scale-[0.98]"
          >
            <LogOut className="w-4 h-4" />
            <span>Logout of Radion</span>
          </button>
        </div>
      </GlassCard>
    </div>
  );
}
