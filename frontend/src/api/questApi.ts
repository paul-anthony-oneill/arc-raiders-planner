import type { Quest } from "../types";

export const fetchQuests = async (): Promise<Quest[]> => {
    const response = await fetch("/api/quests");
    if (!response.ok) {
        throw new Error("Failed to fetch quests");
    }
    return response.json();
};
