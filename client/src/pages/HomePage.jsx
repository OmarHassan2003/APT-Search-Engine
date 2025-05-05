import React from "react";
import Header from "../components/Header.jsx";
import SearchBar from "../components/SearchBar";
import Footer from "../components/Footer";
import ThemeToggle from "../components/ThemeToggle";

function HomePage() {
  return (
    <div className="flex flex-col min-h-screen bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>

      <Header minimal={false} />

      <main className="flex-grow flex flex-col items-center justify-center px-4">
        <div className="w-full max-w-2xl">
          <div className="flex justify-center mb-8">
            <h1
              style={{ fontFamily: "Baumans" }}
              className="text-5xl font-bold bg-gradient-to-r from-blue-400 to-green-400 bg-clip-text text-transparent"
            >
              Search Engine
            </h1>
          </div>

          <SearchBar />
        </div>
      </main>

      <Footer />
    </div>
  );
}

export default HomePage;
