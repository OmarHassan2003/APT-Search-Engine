import { useLocation } from "react-router-dom";
import Header from "../components/Header";

import Footer from "../components/Footer";

function ResultsPage() {
  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const query = queryParams.get("q") || "";

  return (
    <div className="flex flex-col min-h-screen bg-white text-gray-900">
      <Header minimal={true} query={query} />

      <div className="mt-auto">
        <Footer />
      </div>
    </div>
  );
}

export default ResultsPage;
