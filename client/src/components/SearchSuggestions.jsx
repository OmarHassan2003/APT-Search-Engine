import React from "react";
import { Search } from "lucide-react";

function SearchSuggestions({ suggestions, query, onSuggestionClick }) {
  const highlightMatch = (suggestion) => {
    if (!query.trim()) return suggestion;

    const index = suggestion.toLowerCase().indexOf(query.toLowerCase());
    if (index === -1) return suggestion;

    return (
      <>
        {suggestion.substring(0, index)}
        <strong className="font-semibold text-black dark:text-white">
          {suggestion.substring(index, index + query.length)}
        </strong>
        {suggestion.substring(index + query.length)}
      </>
    );
  };

  return (
    <div className="absolute z-10 w-full bg-white dark:bg-gray-800 border-x border-b border-gray-300 dark:border-gray-700 rounded-b-3xl shadow-md -mt-1 pb-1 overflow-hidden">
      <ul className="divide-y divide-gray-200 dark:divide-gray-700">
        {suggestions.map((suggestion, index) => (
          <li
            key={index}
            className="px-4 py-2 cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center gap-2"
            onClick={() => onSuggestionClick(suggestion)}
          >
            <Search className="h-4 w-4 text-gray-500 dark:text-gray-400" />
            <span className="text-sm text-gray-800 dark:text-gray-200">
              {highlightMatch(suggestion)}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default SearchSuggestions;
