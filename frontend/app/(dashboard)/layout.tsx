import { Sidebar } from "@/components/sidebar/Sidebar";
import { MobileBottomNav } from "@/components/sidebar/MobileBottomNav";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative h-screen w-full overflow-hidden bg-[#0a0f16] font-sans">
      {/* Fixed Background Image */}
      <div 
        className="absolute inset-0 z-0 bg-cover bg-center bg-no-repeat opacity-90"
        style={{ backgroundImage: "url('/placeholder-mountains-bg.jpg')" }} 
      />
      
      {/* Main Layout Grid */}
      <div className="relative z-10 flex h-full px-4 py-4 gap-4 md:p-6 md:gap-8">
        {/* Sidebar: hidden on mobile, visible on md+ */}
        <div className="hidden md:flex">
          <Sidebar className="h-full flex-shrink-0" />
        </div>
        <main className="flex-1 flex flex-col min-w-0 h-full overflow-y-auto pb-20 md:pb-0 custom-scrollbar">
          {children}
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      <MobileBottomNav />
    </div>
  );
}