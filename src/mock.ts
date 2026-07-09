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

/**
 * Minimal valid GLB (a shaded cube) assembled by hand — lets the mesh pipeline
 * and the <model-viewer> gallery be tested offline without a Meshy API key.
 */
export async function mockMesh(outPath: string, colorHex = "#7fd1e0"): Promise<void> {
  // 24 vertices (4 per face, so each face gets flat normals) + 36 indices.
  const faces: Array<{ n: [number, number, number]; v: Array<[number, number, number]> }> = [
    { n: [0, 0, 1], v: [[-1, -1, 1], [1, -1, 1], [1, 1, 1], [-1, 1, 1]] },
    { n: [0, 0, -1], v: [[1, -1, -1], [-1, -1, -1], [-1, 1, -1], [1, 1, -1]] },
    { n: [1, 0, 0], v: [[1, -1, 1], [1, -1, -1], [1, 1, -1], [1, 1, 1]] },
    { n: [-1, 0, 0], v: [[-1, -1, -1], [-1, -1, 1], [-1, 1, 1], [-1, 1, -1]] },
    { n: [0, 1, 0], v: [[-1, 1, 1], [1, 1, 1], [1, 1, -1], [-1, 1, -1]] },
    { n: [0, -1, 0], v: [[-1, -1, -1], [1, -1, -1], [1, -1, 1], [-1, -1, 1]] },
  ];
  const positions: number[] = [];
  const normals: number[] = [];
  const indices: number[] = [];
  faces.forEach((f, fi) => {
    f.v.forEach((p) => positions.push(...p));
    for (let i = 0; i < 4; i++) normals.push(...f.n);
    const b = fi * 4;
    indices.push(b, b + 1, b + 2, b, b + 2, b + 3);
  });

  const idxBuf = Buffer.from(new Uint16Array(indices).buffer);
  const posBuf = Buffer.from(new Float32Array(positions).buffer);
  const normBuf = Buffer.from(new Float32Array(normals).buffer);
  const bin = Buffer.concat([idxBuf, posBuf, normBuf]);

  const rgb = [1, 3, 5].map((i) => parseInt(colorHex.slice(i, i + 2), 16) / 255);
  const gltf = {
    asset: { version: "2.0", generator: "dota2-skin-generator mock" },
    scene: 0,
    scenes: [{ nodes: [0] }],
    nodes: [{ mesh: 0 }],
    meshes: [{ primitives: [{ attributes: { POSITION: 1, NORMAL: 2 }, indices: 0, material: 0 }] }],
    materials: [{ pbrMetallicRoughness: { baseColorFactor: [...rgb, 1], metallicFactor: 0.3, roughnessFactor: 0.6 } }],
    buffers: [{ byteLength: bin.length }],
    bufferViews: [
      { buffer: 0, byteOffset: 0, byteLength: idxBuf.length, target: 34963 },
      { buffer: 0, byteOffset: idxBuf.length, byteLength: posBuf.length, target: 34962 },
      { buffer: 0, byteOffset: idxBuf.length + posBuf.length, byteLength: normBuf.length, target: 34962 },
    ],
    accessors: [
      { bufferView: 0, componentType: 5123, count: indices.length, type: "SCALAR" },
      { bufferView: 1, componentType: 5126, count: 24, type: "VEC3", min: [-1, -1, -1], max: [1, 1, 1] },
      { bufferView: 2, componentType: 5126, count: 24, type: "VEC3" },
    ],
  };

  let jsonBuf = Buffer.from(JSON.stringify(gltf));
  const jsonPad = (4 - (jsonBuf.length % 4)) % 4;
  jsonBuf = Buffer.concat([jsonBuf, Buffer.alloc(jsonPad, 0x20)]);
  const binPad = (4 - (bin.length % 4)) % 4;
  const binPadded = Buffer.concat([bin, Buffer.alloc(binPad)]);

  const chunk = (type: number, data: Buffer): Buffer => {
    const h = Buffer.alloc(8);
    h.writeUInt32LE(data.length, 0);
    h.writeUInt32LE(type, 4);
    return Buffer.concat([h, data]);
  };
  const jsonChunk = chunk(0x4e4f534a, jsonBuf); // "JSON"
  const binChunk = chunk(0x004e4942, binPadded); // "BIN\0"

  const header = Buffer.alloc(12);
  header.write("glTF", 0, "ascii");
  header.writeUInt32LE(2, 4);
  header.writeUInt32LE(12 + jsonChunk.length + binChunk.length, 8);

  const { writeFile } = await import("node:fs/promises");
  await writeFile(outPath, Buffer.concat([header, jsonChunk, binChunk]));
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
