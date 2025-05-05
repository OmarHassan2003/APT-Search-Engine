import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { Search } from "lucide-react";
import SearchSuggestions from "./SearchSuggestions";
import { getSuggestions } from "../services/api";

function SearchBar({ initialValue = "", compact = false }) {
  const [query, setQuery] = useState(initialValue);
  const [suggestions, setSuggestions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [isHovering, setIsHovering] = useState(false);
  const navigate = useNavigate();
  const searchInputRef = useRef(null);
  const suggestionsTimeoutRef = useRef(null);

  const fetchSuggestions = (query) => {
    try {
      const suggestionsData = getSuggestions(query);
      setSuggestions(suggestionsData);
      setShowSuggestions(true);
    } catch (error) {
      console.error("Error fetching suggestions:", error);
    }
  };

  useEffect(() => {
    if (isTyping) {
      clearTimeout(suggestionsTimeoutRef.current);
      suggestionsTimeoutRef.current = setTimeout(() => {
        fetchSuggestions(query);
      }, 100);
    } else {
      setSuggestions([]);
      setShowSuggestions(false);
    }

    return () => {
      clearTimeout(suggestionsTimeoutRef.current);
    };
  }, [query, isTyping]);

  const handleInputFocus = () => {
    fetchSuggestions(query);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (query.trim()) {
      navigate(`/search?q=${encodeURIComponent(query.trim())}`);
      setShowSuggestions(false);
    }
  };

  const handleInputChange = (e) => {
    setQuery(e.target.value);
    setIsTyping(true);
  };

  const handleSuggestionClick = (suggestion) => {
    setQuery(suggestion);
    navigate(`/search?q=${encodeURIComponent(suggestion)}`);
    setShowSuggestions(false);
  };

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (
        searchInputRef.current &&
        !searchInputRef.current.contains(event.target)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener("click", handleClickOutside);
    return () => {
      document.removeEventListener("click", handleClickOutside);
    };
  }, []);

  return (
    <div className="relative w-full" ref={searchInputRef}>
      <form onSubmit={handleSubmit}>
        <div className="relative">
          <input
            type="text"
            value={query}
            onChange={handleInputChange}
            onFocus={handleInputFocus}
            placeholder="Search..."
            className={`w-full border border-gray-300 dark:border-gray-700 shadow-sm focus:ring-0 focus:outline-none focus:border-gray-300 dark:focus:border-gray-700 ${
              showSuggestions && suggestions.length > 0
                ? "rounded-t-3xl"
                : "rounded-full"
            }     
            } bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-200 ${
              compact ? "py-2 px-4 pr-10" : "py-3 px-4 pr-12"
            }`}
            autoComplete="off"
          />
          <button
            type="submit"
            className="absolute right-4 top-1/2 transform -translate-y-1/2"
            style={{ border: "none", background: "transparent" }}
            onMouseEnter={() => setIsHovering(true)}
            onMouseLeave={() => setIsHovering(false)}
          >
            <Search
              className={`${compact ? "h-4 w-4" : "h-5 w-5"} ${
                isHovering
                  ? "text-black dark:text-white"
                  : "text-gray-500 dark:text-gray-400"
              }`}
            />
          </button>
        </div>
      </form>

      {showSuggestions && suggestions.length > 0 && (
        <SearchSuggestions
          suggestions={suggestions}
          query={query}
          onSuggestionClick={handleSuggestionClick}
        />
      )}
    </div>
  );
}

export default SearchBar;
