import ResultItem from "./ResultItem";

function ResultsList({ results, onClick }) {
  return (
    <div className="results-list">
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
