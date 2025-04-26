import { useState } from "react";

function ResultItem({ result, onClick }) {
  const [isHovered, setIsHovered] = useState(false);
  const [isVisited, setIsVisited] = useState(false);

  const linkStyle = {
    color: isVisited ? "rgb(104, 29, 168)" : "rgb(38, 19, 177)",
    textDecoration: isHovered ? "underline" : "none",
    cursor: "pointer",
    transition: "all 0.3s ease",
  };

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
          target="_blank"
          rel="noopener noreferrer"
          style={linkStyle}
          // className="no-underline text-red-500 hover:text-blue-500 hover:underline transition-colors duration-300"
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
