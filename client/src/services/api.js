// This is a mock API service for the search engine
// In a real implementation, this would connect to your Java backend

// Mock data for development purposes
const mockResults = [
  {
    id: 1,
    title: "Introduction to Computer Science - Cairo University",
    url: "https://www.cu.edu.eg/cs/intro",
    snippet:
      "This course is an introduction to computer science and programming. Learn about algorithms, data structures, and software engineering principles.",
  },
  {
    id: 3,
    title: "Introduction to Computer Science - Cairo University",
    url: "https://www.cu.edu.eg/cs/intro",
    snippet:
      "This course is an introduction to computer science and programming. Learn about algorithms, data structures, and software engineering principles.",
  },
  {
    id: 2,
    title: "Advanced Programming Techniques - Course Overview",
    url: "https://www.cu.edu.eg/cs/apt",
    snippet:
      "The Advanced Programming Techniques course focuses on developing robust and efficient software using Java and other programming languages.",
  },
  // Add more mock results as needed
];

// Mock suggestions for development purposes
const mockSuggestions = [
  "computer science cairo university",
  "computer engineering department",
  "cairo university search engine",
  "advanced programming techniques",
  "java programming tutorials",
  "web crawler implementation",
  "search engine algorithms",
  "indexing in search engines",
  "page ranking algorithms",
];

/**
 * Simulates a search query to the backend
 * @param {string} query - The search query
 * @param {number} page - Current page number
 * @param {number} resultsPerPage - Number of results per page
 * @returns {Promise} - Promise resolving to search results
 */
export const searchQuery = async (query, page = 1, resultsPerPage = 10) => {
  // In a real implementation, this would be an API call to your Java backend
  // For now, we'll simulate a network request with a timeout
  return new Promise((resolve) => {
    setTimeout(() => {
      // Filter mock results based on query (case insensitive)
      const filteredResults = mockResults.filter((result) => {
        // Check if this is a phrase search (enclosed in quotes)
        if (query.startsWith('"') && query.endsWith('"')) {
          const phrase = query.slice(1, -1).toLowerCase();
          return (
            result.title.toLowerCase().includes(phrase) ||
            result.snippet.toLowerCase().includes(phrase)
          );
        }
        console.log("Regular search", query);
        // Regular search - check if any of the terms are in the title or snippet
        const terms = query.toLowerCase().split(" ");
        return terms.some(
          (term) =>
            result.title.toLowerCase().includes(term) ||
            result.snippet.toLowerCase().includes(term)
        );
      });

      console.log("Filtered results", filteredResults);
      // Calculate pagination
      const startIndex = (page - 1) * resultsPerPage;
      const endIndex = startIndex + resultsPerPage;
      const paginatedResults = filteredResults.slice(startIndex, endIndex);

      resolve({
        results: paginatedResults,
        totalResults: filteredResults.length,
      });
    }, 800); // Simulate network delay
  });
};

/**
 * Gets search suggestions based on the current query
 * @param {string} query - The current search query
 * @returns {Promise} - Promise resolving to an array of suggestions
 */
export const getSuggestions = async (query) => {
  // In a real implementation, this would fetch suggestions from your Java backend
  return new Promise((resolve) => {
    setTimeout(() => {
      if (!query.trim()) {
        resolve([]);
        return;
      }

      const filteredSuggestions = mockSuggestions
        .filter((suggestion) =>
          suggestion.toLowerCase().includes(query.toLowerCase())
        )
        .slice(0, 5); // Limit to 5 suggestions

      resolve(filteredSuggestions);
    }, 300); // Simulate network delay
  });
};
