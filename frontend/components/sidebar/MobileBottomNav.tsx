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
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-50 flex items-center justify-around px-1 py-2 bg-black/50 backdrop-blur-xl border-t border-white/10">
      {navItems.map((item) => {
        const isActive = pathname === item.id;
        const Icon = item.icon;

        return (
          <Link
            key={item.id}
            href={item.id}
            aria-label={item.label}
            className="relative flex flex-col items-center gap-0.5 px-2 py-1 rounded-xl outline-none min-w-[44px]"
          >
            {isActive && (
              <motion.div
                layoutId="mobile-active-pill"
                className="absolute inset-0 bg-white/12 rounded-xl"
                initial={false}
                transition={{ type: "spring", stiffness: 400, damping: 28 }}
              />
            )}
            <motion.div
              animate={{ scale: isActive ? 1.05 : 1, opacity: isActive ? 1 : 0.5 }}
              transition={{ duration: 0.12 }}
              className="relative z-10 flex flex-col items-center gap-0.5"
            >
              <Icon
                className={cn(
                  "w-[18px] h-[18px] transition-colors",
                  isActive ? "text-white" : "text-white/55"
                )}
                strokeWidth={isActive ? 2.5 : 2}
              />
              <span
                className={cn(
                  "text-[9px] font-medium leading-none",
                  isActive ? "text-white" : "text-white/45"
                )}
              >
                {item.label}
              </span>
            </motion.div>
          </Link>
        );
      })}
    </nav>
  );
}
