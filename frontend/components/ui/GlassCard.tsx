import { cn } from "@/lib/utils"; // standard clsx + tailwind-merge utility

export function GlassCard({ children, className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "relative overflow-hidden rounded-glass bg-glass backdrop-blur-glass shadow-glass border border-glass-border",
        className
      )}
      {...props}
    >
      {/* Extra inner highlight to simulate the top edge of thick glass */}
      <div className="absolute inset-x-0 top-0 h-px bg-gradient-to-r from-transparent via-white/20 to-transparent" />
      {children}
    </div>
  );
}