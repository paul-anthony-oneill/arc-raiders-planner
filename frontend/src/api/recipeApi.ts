import type { Recipe } from "../types";

const API_URL = "http://localhost:8080/api/recipes";

export const recipeApi = {
  getAllRecipes: async (): Promise<Recipe[]> => {
    const response = await fetch(API_URL);
    if (!response.ok) {
      throw new Error("Failed to fetch recipes");
    }
    return response.json();
  },

  createRecipe: async (recipe: Recipe): Promise<Recipe> => {
    const response = await fetch(API_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(recipe),
    });
    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "Failed to create recipe");
    }
    return response.json();
  },

  deleteRecipe: async (id: number): Promise<void> => {
    const response = await fetch(`${API_URL}/${id}`, {
      method: "DELETE",
    });
    if (!response.ok) {
      throw new Error("Failed to delete recipe");
    }
  },
};
