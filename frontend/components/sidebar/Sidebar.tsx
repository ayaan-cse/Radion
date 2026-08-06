"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import { Home, Mail, Calendar, RefreshCw, Settings, User } from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { id: "/", icon: Home, label: "Dashboard" },
  { id: "/mail", icon: Mail, label: "Mail" },
  { id: "/calendar", icon: Calendar, label: "Calendar" },
  { id: "/integrations", icon: RefreshCw, label: "Integrations" },
  { id: "/profile", icon: User, label: "Profile" },
  { id: "/settings", icon: Settings, label: "Settings" },
];

export function Sidebar({ className }: { className?: string }) {
  const pathname = usePathname();

  return (
    <nav
      className={cn(
        "flex flex-col items-center py-8 w-[88px] rounded-full bg-glass backdrop-blur-glass shadow-glass border border-glass-border gap-6 relative",
        className
      )}
    >
      {/* Top glass reflection */}
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-white/20 to-transparent rounded-t-full" />

      {navItems.map((item) => {
        const isActive = pathname === item.id;
        const Icon = item.icon;

        return (
          <Link
            key={item.id}
            href={item.id}
            aria-label={item.label}
            className="relative p-3 rounded-full group outline-none focus-visible:ring-2 focus-visible:ring-white/30 transition-colors"
          >
            {isActive && (
              <motion.div
                layoutId="active-nav-pill"
                className="absolute inset-0 bg-white/15 rounded-full shadow-[inset_0_1px_1px_rgba(255,255,255,0.2)]"
                initial={false}
                transition={{
                  type: "spring",
                  stiffness: 350,
                  damping: 20,
                  mass: 0.6,
                }}
              />
            )}

            <motion.div
              animate={{
                scale: isActive ? 1.1 : 1,
                opacity: isActive ? 1 : 0.5,
              }}
              transition={{ duration: 0.15 }}
              className="relative z-10"
            >
              <Icon
                className={cn(
                  "w-6 h-6 transition-colors duration-200",
                  isActive
                    ? "text-white"
                    : "text-white/70 group-hover:text-white"
                )}
                strokeWidth={isActive ? 2.5 : 2}
              />
            </motion.div>
          </Link>
        );
      })}
    </nav>
  );
}