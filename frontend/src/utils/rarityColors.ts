/**
 * Rarity color mapping for items.
 * WHY: Provides visual hierarchy and quick recognition of item value
 */

export interface RarityColors {
  background: string;
  text: string;
}

/**
 * Returns background and text colors for a given rarity tier.
 * Colors are chosen for the retro/sci-fi aesthetic and ensure good contrast.
 */
export function getRarityColors(rarity: string): RarityColors {
  const normalizedRarity = rarity.toLowerCase();

  switch (normalizedRarity) {
    case "common":
      return {
        background: "#666666",
        text: "#e6e6e6",
      };
    case "uncommon":
      return {
        background: "#00ff41", // retro-green
        text: "#0a0a0a",
      };
    case "rare":
      return {
        background: "#0099ff",
        text: "#ffffff",
      };
    case "epic":
      return {
        background: "#9933ff",
        text: "#ffffff",
      };
    case "legendary":
      return {
        background: "#ff6b00", // retro-orange
        text: "#0a0a0a",
      };
    case "mythic":
    case "exotic":
      return {
        background: "#ff003c", // retro-red
        text: "#ffffff",
      };
    default:
      // Default fallback for unknown rarities
      return {
        background: "#f0f0f0",
        text: "#333333",
      };
  }
}
