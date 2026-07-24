import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        glass: {
          DEFAULT: "rgba(255, 255, 255, 0.06)",
          hover: "rgba(255, 255, 255, 0.10)",
          border: "rgba(255, 255, 255, 0.15)",
        },
        semantic: {
          blue: "#3b82f6",
          green: "#34d399",
          amber: "#fbbf24",
          purple: "#a78bfa",
        },
      },
      boxShadow: {
        glass: "0 8px 32px 0 rgba(0, 0, 0, 0.25), inset 0 1px 0 0 rgba(255, 255, 255, 0.15)",
        'glass-sm': "0 4px 16px 0 rgba(0, 0, 0, 0.15), inset 0 1px 0 0 rgba(255, 255, 255, 0.15)",
      },
      backdropBlur: {
        glass: "32px",
      },
      borderRadius: {
        glass: "32px",
      },
    },
  },
  plugins: [],
};
export default config;