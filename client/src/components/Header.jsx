import React from "react";
import { Link } from "react-router-dom";
import SearchBar from "./SearchBar";

function Header({ minimal = false, query = "" }) {
  return (
    (minimal? (
    <div className="bg-white relative z-10 pb-15">
    <header className="w-full py-4 fixed top-0 left-0 bg-white">
      <div className="container mx-auto px-4">
        <div className="flex items-center">
          
              <Link to="/" className="mr-10">
                <h2 style={{fontFamily:"Baumans"}}  className="text-3xl font-bold text-blue-500">Search Engine</h2>
              </Link>
              <div className="flex-grow max-w-xl">
                <SearchBar initialValue={query} compact={true} />
              </div>
            
          

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
    </div>
    ) : <></>)
  );
}

export default Header;
