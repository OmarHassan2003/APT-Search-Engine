import { useLocation } from "react-router-dom";
import Header from "../components/Header";
import Footer from "../components/Footer";
import { searchQuery } from "../services/api";
import Pagination from "../components/Pagination";
import ResultsList from "../components/ResultsList";
import LoadingSpinner from "../components/LoadingSpinner";
import { useState, useEffect } from "react";

function ResultsPage() {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const query = queryParams.get("q") || "";
  const [results, setResults] = useState([]);
  const [pageData, setPageData] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchResults = async (query) => {
    setIsLoading(true);
    try {
      const response = await searchQuery(query);
      setResults(response);
      setTotalPages(Math.ceil(response.totalCount / 10)); // Assuming 10 results per page
      const limit = response.results
        ? Math.min(10, response.results.length)
        : 0;
      setPageData(response.results ? response.results.slice(0, limit) : []);
      setPage(0); // Reset to the first page

      console.log("Total pages:", Math.ceil(response.totalCount / 10));
      console.log("Fetched results:", response);
    } catch (error) {
      console.error("Error fetching results:", error);
    } finally {
      setIsLoading(false);
    }
  };
  useEffect(() => {
    fetchResults(query);
  }, [query]);
  useEffect(() => {
    if (page <= totalPages && results.results) {
      const startIndex = page * 10;
      const endIndex = Math.min(startIndex + 10, results.results.length);
      setPageData(results.results.slice(startIndex, endIndex) || []);
      console.log("Page data:", pageData);
    }
  }, [page]);

  return (
    (isLoading && <LoadingSpinner />) || (
      <div className="flex flex-col min-h-screen bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100">
        <Header minimal={true} query={query} />

        <ResultsList
          totalCount={results.totalCount}
          results={pageData}
          totalTime={results.totalTime}
        />

        <Pagination
          totalPages={totalPages}
          initialPage={page}
          onPageChange={setPage}
        />

        <div className="mt-auto">
          <Footer />
        </div>
      </div>
    )
  );
}

export default ResultsPage;
