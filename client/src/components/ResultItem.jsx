import { useState } from "react";

function ResultItem({ result, onClick }) {
  const [isHovered, setIsHovered] = useState(false);
  const [isVisited, setIsVisited] = useState(false);

  return (
    <div
      className="my-3"
      onClick={(e) => {
        e.stopPropagation();
        onClick(result);
      }}
    >
      <h3 className="text-lg font-semibold">
        <a
          href={result.url}
          className={`cursor-pointer transition-all duration-300 ${
            isVisited ? "text-purple-800" : "text-blue-800"
          } ${isHovered ? "underline" : "no-underline"}`}
          onMouseEnter={() => setIsHovered(true)}
          onMouseLeave={() => setIsHovered(false)}
          onClick={() => setIsVisited(true)}
        >
          {result.title}
        </a>
      </h3>
      <p className="pt-1">{result.snippet}</p>
    </div>
  );
}

export default ResultItem;
