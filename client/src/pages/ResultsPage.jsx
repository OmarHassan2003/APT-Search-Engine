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
      setPageData(response.results ? response.results.slice(0, 10) : []);
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
    if (page <= totalPages) {
      setPageData(response.results ? response.results.slice(page, 10) : []);
    }
  }, [page]);

  return (
    (isLoading && <LoadingSpinner />) || (
      <div className="flex flex-col min-h-screen bg-white text-gray-900">
        <Header minimal={true} query={query} />

        <ResultsList results={pageData} />

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
