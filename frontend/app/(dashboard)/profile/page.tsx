"use client";

import { useSession, signOut } from "next-auth/react";
import { GlassCard } from "@/components/ui/GlassCard";
import { LogOut, User, Mail, ShieldCheck } from "lucide-react";

export default function ProfilePage() {
  const { data: session, status } = useSession();

  if (status === "loading") {
    return <div className="p-4 text-white/60 text-sm">Loading profile...</div>;
  }

  const name = session?.user?.name || "Radion User";
  const email = session?.user?.email || "No email available";

  return (
    <div className="flex-1 flex flex-col pt-2 md:pt-4 overflow-y-auto custom-scrollbar pr-2 md:pr-4 max-w-3xl">
      <h1 className="text-xl md:text-2xl font-bold text-white tracking-tight mb-1">
        Website Identity
      </h1>
      <p className="text-[11px] md:text-sm text-white/60 mb-4 md:mb-8 line-clamp-2 md:line-clamp-none">
        This identity is used solely for logging into Radion. It is never used as your connected service accounts.
      </p>

      <GlassCard className="p-4 md:p-8 flex flex-col gap-4 md:gap-6">
        {/* Profile header row */}
        <div className="flex items-center gap-3 md:gap-6 pb-4 md:pb-6 border-b border-white/10">
          {/* Avatar — compact on mobile */}
          <div className="relative w-14 h-14 md:w-20 md:h-20 rounded-full overflow-hidden border-2 border-white/20 bg-white/10 shrink-0 flex items-center justify-center">
            {session?.user?.image ? (
              <img
                src={session.user.image}
                alt={name}
                className="w-full h-full object-cover"
              />
            ) : (
              <span className="text-xl md:text-2xl font-bold text-white">{name[0]}</span>
            )}
          </div>

          {/* Name + badge + email */}
          <div className="flex flex-col min-w-0 flex-1">
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-base md:text-xl font-bold text-white leading-tight">{name}</h2>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-[10px] md:text-xs font-bold uppercase tracking-wider shrink-0">
                <ShieldCheck className="w-3 h-3 shrink-0" />
                Auth
              </span>
            </div>
            <p className="text-[11px] md:text-sm text-zinc-400 mt-0.5 truncate max-w-full">{email}</p>
          </div>
        </div>

        {/* Info rows */}
        <div className="flex flex-col gap-1 md:gap-4">
          <div className="flex items-center justify-between py-2 md:py-2 border-b border-white/5">
            <div className="flex items-center gap-2 text-zinc-300">
              <User className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
              <span className="text-xs md:text-sm font-medium">Full Name</span>
            </div>
            <span className="text-xs md:text-sm text-white font-semibold ml-4">{name}</span>
          </div>

          <div className="flex items-center justify-between py-2 md:py-2 border-b border-white/5 gap-2">
            <div className="flex items-center gap-2 text-zinc-300 shrink-0">
              <Mail className="w-3.5 h-3.5 text-zinc-400 shrink-0" />
              <span className="text-xs md:text-sm font-medium">Email</span>
            </div>
            <span className="text-xs md:text-sm text-white font-semibold truncate max-w-[55%] ml-4 text-right">
              {email}
            </span>
          </div>
        </div>

        {/* Logout button — full width on mobile */}
        <div className="pt-1 md:pt-4 flex md:justify-end">
          <button
            onClick={() => signOut({ callbackUrl: "/login" })}
            className="w-full md:w-auto flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 text-sm font-bold transition-all shadow-sm active:scale-[0.98]"
          >
            <LogOut className="w-4 h-4" />
            <span>Logout of Radion</span>
          </button>
        </div>
      </GlassCard>
    </div>
  );
}
