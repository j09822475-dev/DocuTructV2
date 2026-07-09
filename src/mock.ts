import sharp from "sharp";
import type { Hero, ItemSlot } from "./data/heroes.js";
import type { SkinConcept } from "./llm/schema.js";
import { slugify } from "./output/package.js";

/** Offline stand-in for the Claude call — used by `generate --mock` and tests. */
export function mockConcept(hero: Hero, slot: ItemSlot, theme: string): SkinConcept {
  const name = `Mock ${theme} ${slot} of ${hero.name}`;
  return {
    name,
    slug: slugify(name),
    hero: hero.name,
    slot,
    rarity: "mythical",
    lore: `A placeholder relic conjured for testing. They say ${hero.name} once wielded it in the land of Unit Tests, where no API keys are required.\n\nIts power is entirely deterministic.`,
    visualDescription: `A ${theme}-themed ${slot} for ${hero.name}: bold silhouette, three-tone palette, weathered edges. (mock)`,
    colorPalette: ["#2b3a4a", "#7fd1e0", "#d9a441", "#5a2f2f"],
    materials: ["weathered steel", "carved bone", "woven leather"],
    particleEffects: ["faint frost mist along the edge", "slow ember drift"],
    texturePrompts: {
      conceptArt: `Dota 2 style concept art of a ${theme} ${slot} for ${hero.name}, painterly, dark neutral background`,
      diffuse: `Flat albedo texture sheet of ${theme} materials: steel, bone, leather; even lighting, no shadows`,
    },
    workshopMetadata: {
      title: name,
      description: `A ${theme}-themed ${slot} for ${hero.name}. (mock)`,
      tags: [hero.name, slot, theme, "mock"],
    },
  };
}

/** Deterministic placeholder image so texture-map derivation can run offline. */
export async function mockImage(outPath: string, palette: string[]): Promise<void> {
  const size = 1024;
  const bands = palette
    .map(
      (hex, i) =>
        `<rect x="0" y="${(size / palette.length) * i}" width="${size}" height="${size / palette.length}" fill="${hex}"/>`,
    )
    .join("");
  // Noise circles give the Sobel-based normal map something to react to.
  const circles = Array.from({ length: 48 }, (_, i) => {
    const x = (i * 337) % size;
    const y = (i * 199) % size;
    const r = 20 + ((i * 53) % 60);
    return `<circle cx="${x}" cy="${y}" r="${r}" fill="rgba(255,255,255,${0.05 + (i % 5) * 0.05})"/>`;
  }).join("");
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${size}" height="${size}">${bands}${circles}</svg>`;
  await sharp(Buffer.from(svg)).png().toFile(outPath);
}
