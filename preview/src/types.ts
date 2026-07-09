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
    model: string | null;
    modelFbx: string | null;
  };
}

// <model-viewer> is a web component from @google/model-viewer
import type React from "react";

declare module "react" {
  namespace JSX {
    interface IntrinsicElements {
      "model-viewer": React.DetailedHTMLProps<React.HTMLAttributes<HTMLElement>, HTMLElement> & {
        src?: string;
        alt?: string;
        "camera-controls"?: boolean;
        "auto-rotate"?: boolean;
        "shadow-intensity"?: string;
        exposure?: string;
      };
    }
  }
}
