import axios from "axios";
const backendUrl = "http://localhost:8080";

export const searchQuery = async (query) => {
  try {
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

export const getSuggestions = async (query) => {
  try {
    const response = await axios.get(`${backendUrl}/suggestions`, {
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
