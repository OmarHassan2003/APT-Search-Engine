import { useState } from "react";

function Pagination({ totalPages = 10, initialPage = 0, onPageChange }) {
  const [page, setPage] = useState(initialPage);

  const handlePageChange = (newPage) => {
    if (newPage >= 0 && newPage <= totalPages - 1) {
      setPage(newPage);
      if (onPageChange) {
        onPageChange(newPage);
      }
    }
  };

  return totalPages >= 0 ? (
    <div className="flex items-center justify-left ml-10 pl-80 pb-16 space-x-4">
      {page > 0 && (
        <button
          onClick={() => handlePageChange(page - 1)}
          disabled={page === 0}
          className="flex items-center justify-center text-black dark:text-white px-4 py-2 rounded-[5px] border border-gray-400 dark:border-gray-600 disabled:opacity-50 disabled:cursor-not-allowed hover:border-gray-700 dark:hover:border-gray-400 transition-colors"
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
      )}
      {page > 0 && (
        <div className="px-2 py-2 text-black dark:text-white">
          {page + 1} of {totalPages}
        </div>
      )}
      {page < totalPages - 1 && (
        <button
          onClick={() => handlePageChange(page + 1)}
          disabled={page === totalPages - 1}
          className="flex items-center justify-center text-black dark:text-white px-4 py-2 rounded-[5px] border border-gray-400 dark:border-gray-600 disabled:opacity-50 disabled:cursor-not-allowed hover:border-gray-700 dark:hover:border-gray-400 transition-colors"
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
      )}
    </div>
  ) : (
    <></>
  );
}

export default Pagination;
