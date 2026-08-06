"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import { Home, Mail, Calendar, RefreshCw, Settings, User } from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { id: "/", icon: Home, label: "Home" },
  { id: "/mail", icon: Mail, label: "Mail" },
  { id: "/calendar", icon: Calendar, label: "Calendar" },
  { id: "/integrations", icon: RefreshCw, label: "Sync" },
  { id: "/profile", icon: User, label: "Profile" },
  { id: "/settings", icon: Settings, label: "Settings" },
];

export function MobileBottomNav() {
  const pathname = usePathname();

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 flex items-center justify-around px-2 py-3 bg-black/40 backdrop-blur-xl border-t border-white/10 safe-area-pb">
      {navItems.map((item) => {
        const isActive = pathname === item.id;
        const Icon = item.icon;

        return (
          <Link
            key={item.id}
            href={item.id}
            aria-label={item.label}
            className="relative flex flex-col items-center gap-1 px-2 py-1 rounded-xl outline-none"
          >
            {isActive && (
              <motion.div
                layoutId="mobile-active-pill"
                className="absolute inset-0 bg-white/15 rounded-xl"
                initial={false}
                transition={{ type: "spring", stiffness: 350, damping: 25 }}
              />
            )}
            <motion.div
              animate={{ scale: isActive ? 1.1 : 1, opacity: isActive ? 1 : 0.5 }}
              transition={{ duration: 0.15 }}
              className="relative z-10 flex flex-col items-center gap-0.5"
            >
              <Icon
                className={cn("w-5 h-5 transition-colors", isActive ? "text-white" : "text-white/60")}
                strokeWidth={isActive ? 2.5 : 2}
              />
              <span className={cn("text-[10px] font-medium", isActive ? "text-white" : "text-white/50")}>
                {item.label}
              </span>
            </motion.div>
          </Link>
        );
      })}
    </nav>
  );
}
