import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
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
            className={`w-full border border-gray-300 dark:border-gray-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-400 focus:border-transparent rounded-full bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-200 ${
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
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className={`${
                compact ? "h-4 w-4" : "h-5 w-5"
              } dark:stroke-gray-300`}
              fill="none"
              viewBox="0 0 24 24"
              stroke={isHovering ? "#000000" : "#808080"}
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
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
