function ResultItem({ result, onClick }) {
  return (
    <div className="result-item" onClick={() => onClick(result)}>
      <h3>{result.title}</h3>
      <p>{result.description}</p>
    </div>
  );
}

export default ResultItem;
