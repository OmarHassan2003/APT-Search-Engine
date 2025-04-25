import React from "react";
import { Link } from "react-router-dom";
import SearchBar from "./SearchBar";

function Header({ minimal = false, query = "" }) {
  return (
    <header className="w-full py-4">
      <div className="container mx-auto px-4">
        <div className="flex items-center">
          {minimal && (
            <>
              <Link to="/" className="mr-4">
                <h1 className="text-3xl font-bold text-blue-500">Apt Apt</h1>
              </Link>
              <div className="flex-grow max-w-xl">
                <SearchBar initialValue={query} compact={true} />
              </div>
            </>
          )}

          {/* <div className="ml-auto">
            <nav>
              <ul className="flex space-x-4">
                <li>
                  <a href="#" className="text-gray-600 hover:text-gray-900">
                    About
                  </a>
                </li>
                <li>
                  <a href="#" className="text-gray-600 hover:text-gray-900">
                    Settings
                  </a>
                </li>
              </ul>
            </nav>
          </div> */}
        </div>
      </div>
    </header>
  );
}

export default Header;
