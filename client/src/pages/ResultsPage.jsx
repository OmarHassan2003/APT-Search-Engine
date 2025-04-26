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
  const [isLoading, setIsLoading] = useState(true);

  const fetchResults = async (query) => {
    setIsLoading(true);
    try {
      const response = await searchQuery(query);
      setResults(response.results);
      console.log("Fetched results:", response.results);
    } catch (error) {
      console.error("Error fetching results:", error);
    } finally {
      setIsLoading(false);
    }
  };
  useEffect(() => {
    fetchResults(query);
  }, [query]);

  return (
    (isLoading && <LoadingSpinner />) || (
      <div className="flex flex-col min-h-screen bg-white text-gray-900">
        <Header minimal={true} query={query} />

        <ResultsList
          results={results}
          onClick={(result) => {
            console.log("Result clicked:", result);
            // Handle result click (e.g., navigate to result details)
          }}
        />

        <Pagination
          totalPages={10}
          currentPage={1}
          onPageChange={(page) => console.log(`Page changed to: ${page}`)}
        />

        <div className="mt-auto">
          <Footer />
        </div>
      </div>
    )
  );
}

export default ResultsPage;
