import { useState } from "react";

function Pagination({ totalPages = 10, initialPage = 1, onPageChange }) {
  const [page, setPage] = useState(initialPage);

  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setPage(newPage);
      if (onPageChange) {
        onPageChange(newPage);
      }
    }
  };

  return (
    <div className="flex items-center justify-center space-x-4 text-white">
      <button
        onClick={() => handlePageChange(page - 1)}
        disabled={page === 1}
        className="flex items-center justify-center px-4 py-2 rounded-md bg-gray-800 border border-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-700 transition-colors"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-4 w-4 mr-1"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 19l-7-7 7-7"
          />
        </svg>
        Previous
      </button>

      <div className="px-4 py-2 text-black">Page {page}</div>

      <button
        onClick={() => handlePageChange(page + 1)}
        disabled={page === totalPages}
        className="flex items-center justify-center px-4 py-2 rounded-md bg-gray-800 border border-gray-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-700 transition-colors"
      >
        Next
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-4 w-4 ml-1"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 5l7 7-7 7"
          />
        </svg>
      </button>
    </div>
  );
}

export default Pagination;
