import { Sidebar } from "@/components/sidebar/Sidebar";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="relative h-screen w-full overflow-hidden bg-[#0a0f16] font-sans">
      {/* Fixed Background Image */}
      <div 
        className="absolute inset-0 z-0 bg-cover bg-center bg-no-repeat opacity-90"
        style={{ backgroundImage: "url('/placeholder-mountains-bg.jpg')" }} 
      />
      
      {/* Main Layout Grid */}
      <div className="relative z-10 flex h-full p-6 gap-8">
        <Sidebar className="h-full flex-shrink-0" />
        <main className="flex-1 flex flex-col min-w-0 h-full">
          {children}
        </main>
      </div>
    </div>
  );
}