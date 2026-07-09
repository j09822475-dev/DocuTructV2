export interface SkinConcept {
  name: string;
  slug: string;
  hero: string;
  slot: string;
  rarity: string;
  lore: string;
  visualDescription: string;
  colorPalette: string[];
  materials: string[];
  particleEffects: string[];
  texturePrompts: { conceptArt: string; diffuse: string };
  workshopMetadata: { title: string; description: string; tags: string[] };
}

export interface SkinEntry {
  id: string;
  concept: SkinConcept;
  generatedAt: string;
  urls: {
    conceptArt: string | null;
    diffuse: string | null;
    normal: string | null;
    mask1: string | null;
    mask2: string | null;
  };
}
