import axios from "axios";
const backendUrl = "http://localhost:8080";

export const searchQuery = async (query) => {
  try {
    // Get existing search history from localStorage
    const searchHistory = JSON.parse(
      localStorage.getItem("searchHistory") || "[]"
    );

    // Check if current query exists anywhere in the history
    const queryIndex = searchHistory.indexOf(query);

    // Only add if it's not already in the history
    if (queryIndex === -1) {
      // Add new query to the beginning
      searchHistory.unshift(query);
      // Keep only the last 10 searches
      if (searchHistory.length > 150) {
        searchHistory.pop();
      }
    } else if (queryIndex > 0) {
      // If query exists but not at position 0, remove it from current position
      searchHistory.splice(queryIndex, 1);
      // Add it to the beginning
      searchHistory.unshift(query);
    }

    // Save back to localStorage
    localStorage.setItem("searchHistory", JSON.stringify(searchHistory));

    console.log("Query", query);
    const response = await axios.get(`${backendUrl}/search`, {
      params: {
        query: query,
      },
    });

    console.log("Filtered results", response.data);
    return response.data;
  } catch (error) {
    console.error("Error fetching search results:", error);
    throw error; // Re-throw the error to handle it in the calling code
  }
};

export const getSuggestions = (query) => {
  try {
    const searchHistory = JSON.parse(
      localStorage.getItem("searchHistory") || "[]"
    );
    const suggestions = searchHistory.filter((item) =>
      item.toLowerCase().includes(query.toLowerCase())
    );
    console.log("Suggestions", suggestions);
    // Limit the suggestions to a maximum of 5 items
    return suggestions.slice(0, 5);
  } catch (error) {
    console.error("Error fetching search results:", error);
    throw error; // Re-throw the error to handle it in the calling code
  }
};
