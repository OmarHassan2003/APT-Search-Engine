/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  darkMode: false, // Set to false to disable dark mode
  theme: {
    extend: {
      colors: {
        primary: "#1DA1F2",
        secondary: "#14171A",
        accent: "#657786",
        background: "#F5F8FA",
        text: "#14171A",
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"],
        baumans: ["Baumans", "cursive"],
      },
    },
  },

  plugins: [],
};
