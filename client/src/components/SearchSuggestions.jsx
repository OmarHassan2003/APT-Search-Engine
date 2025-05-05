import React from "react";

function SearchSuggestions({ suggestions, query, onSuggestionClick }) {
  const highlightMatch = (suggestion) => {
    if (!query.trim()) return suggestion;

    const index = suggestion.toLowerCase().indexOf(query.toLowerCase());
    if (index === -1) return suggestion;

    return (
      <>
        {suggestion.substring(0, index)}
        <strong className="font-bold text-blue-600 dark:text-blue-400">
          {suggestion.substring(index, index + query.length)}
        </strong>
        {suggestion.substring(index + query.length)}
      </>
    );
  };

  return (
    <div className="absolute z-10 w-full mt-1 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-700 rounded-lg shadow-md">
      <ul className="py-1">
        {suggestions.map((suggestion, index) => (
          <li
            key={index}
            className="px-4 py-2 cursor-pointer hover:bg-gray-200 dark:hover:bg-gray-700 flex items-center"
            onClick={() => onSuggestionClick(suggestion)}
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-4 w-4 text-gray-400 dark:text-gray-500 mr-2"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
            <span className="text-gray-700 dark:text-gray-300">
              {highlightMatch(suggestion)}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default SearchSuggestions;
