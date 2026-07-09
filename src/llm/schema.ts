import { z } from "zod";

export const SkinConceptSchema = z.object({
  name: z.string().describe("Evocative item set name in Dota 2 style, e.g. 'Hook of the Sunken Maw'"),
  slug: z.string().describe("URL-safe kebab-case slug derived from the name, ascii only"),
  hero: z.string().describe("Hero name exactly as provided"),
  slot: z.string().describe("Item slot exactly as provided"),
  rarity: z
    .enum(["common", "uncommon", "rare", "mythical", "legendary", "immortal", "arcana"])
    .describe("Workshop rarity tier appropriate for the concept's ambition"),
  lore: z.string().describe("2-3 paragraphs of item lore in Dota 2 voice, tied to the hero's story"),
  visualDescription: z
    .string()
    .describe("Detailed visual description: silhouette, shapes, materials, wear, glow — written for a 3D artist"),
  colorPalette: z
    .array(z.string())
    .describe("4-6 hex colors (#rrggbb) forming the palette; must keep the hero readable at game camera distance"),
  materials: z
    .array(z.string())
    .describe("Physical materials used, e.g. 'corroded bronze', 'frost-etched steel', 'cursed bone'"),
  particleEffects: z
    .array(z.string())
    .describe("Suggested particle/ambient effects, each one short phrase"),
  texturePrompts: z
    .object({
      conceptArt: z
        .string()
        .describe("Image-generation prompt for a full concept-art render of the item on/with the hero"),
      diffuse: z
        .string()
        .describe("Image-generation prompt for a flat, evenly lit diffuse/albedo texture sheet of the item surface materials, no scene, no lighting drama"),
    })
    .describe("Prompts to feed an image generation model"),
  workshopMetadata: z
    .object({
      title: z.string().describe("Title for the Steam Workshop submission form"),
      description: z.string().describe("Short Workshop description (1-2 sentences)"),
      tags: z.array(z.string()).describe("Workshop tags, e.g. hero name, slot, style keywords"),
    })
    .describe("Ready-to-paste Steam Workshop form fields"),
});

export type SkinConcept = z.infer<typeof SkinConceptSchema>;

export interface SkinPackageIndexEntry {
  dir: string;
  concept: SkinConcept;
  generatedAt: string;
  images: {
    conceptArt: boolean;
    diffuse: boolean;
    normal: boolean;
    masks: boolean;
  };
}
