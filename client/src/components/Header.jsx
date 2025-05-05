import React from "react";
import { Link } from "react-router-dom";
import SearchBar from "./SearchBar";
import ThemeToggle from "./ThemeToggle";

function Header({ minimal = false, query = "" }) {
  return minimal ? (
    <div className="bg-white dark:bg-gray-900 relative z-10 pb-15">
      <header className="w-full py-4 fixed top-0 left-0 bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-800">
        <div className="container mx-auto px-4">
          <div className="flex items-center">
            <Link to="/" className="mr-10">
              <h2
                style={{ fontFamily: "Baumans" }}
                className="text-3xl font-bold text-blue-500 dark:text-blue-400"
              >
                Search Engine
              </h2>
            </Link>
            <div className="flex-grow max-w-xl">
              <SearchBar initialValue={query} compact={true} />
            </div>

            <div className="ml-auto">
              <ThemeToggle />
            </div>
          </div>
        </div>
      </header>
    </div>
  ) : (
    <></>
  );
}

export default Header;
