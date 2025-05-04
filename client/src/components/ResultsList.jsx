import ResultItem from "./ResultItem";

function ResultsList({ results,totalTime }) {
  console.log("ResultsList component rendered with results:", results);
  return (
    <div className="mt-8 mb-4 mx-48 flex flex-col min-h-[100vh] max-w-[55%] bg-white text-gray-900">
      <div>
        <p className="text-sm text-gray-600 mb-4">
          About {results.totalCount} results ({totalTime} ms)
        </p>
      </div>
      {results?.map((result, index) => (
        <ResultItem key={index} result={result} />
      ))}
    </div>
  );
}

export default ResultsList;
