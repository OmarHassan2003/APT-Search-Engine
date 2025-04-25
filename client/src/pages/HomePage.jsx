import React from "react";
import Header from "../components/Header.jsx";
import SearchBar from "../components/SearchBar";
import Footer from "../components/Footer";

function HomePage() {
  return (
    <div className="flex flex-col min-h-screen bg-white text-gray-800">
      <Header minimal={false} />

      <main className="flex-grow flex flex-col items-center justify-center px-4">
        <div className="w-full max-w-2xl">
          <div className="flex justify-center mb-8">
            <h1 className="text-5xl font-bold bg-gradient-to-r from-blue-400 to-green-400 bg-clip-text text-transparent">
              Apt Apt
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
