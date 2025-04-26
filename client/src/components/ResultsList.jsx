import ResultItem from "./ResultItem";

function ResultsList({ results, onClick }) {
  return (
    <div className="mt-8 mb-4 mx-48 flex flex-col min-h-[100vh] max-w-[55%] bg-white text-gray-900">
      {results.map((result) => (
        <ResultItem
          key={result.id}
          result={result}
          onClick={() => onClick(result)}
        />
      ))}
    </div>
  );
}

export default ResultsList;
