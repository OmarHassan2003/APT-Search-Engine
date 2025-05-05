import ResultItem from "./ResultItem";

function ResultsList({ results, totalTime, totalCount }) {
  console.log("ResultsList component rendered with results:", results);
  return (
    <div className="mt-8 mb-4 mx-48 flex flex-col min-h-[100vh] max-w-[55%] bg-white dark:bg-gray-900 text-gray-900 dark:text-gray-100">
      <div>
        <p className="text-sm text-gray-600 dark:text-gray-400 mb-4">
          About {totalCount} results ({totalTime} ms)
        </p>
      </div>
      {results?.map((result, index) => (
        <ResultItem key={index} result={result} />
      ))}
    </div>
  );
}

export default ResultsList;
