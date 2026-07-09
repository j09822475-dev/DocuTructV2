import OpenAI from "openai";
import { writeFile } from "node:fs/promises";
import { IMAGE_MODEL, OPENAI_API_KEY } from "../config.js";

export interface ImageResult {
  conceptArtPath?: string;
  diffusePath?: string;
  skippedReason?: string;
}

async function generateOne(client: OpenAI, prompt: string, outPath: string): Promise<void> {
  const result = await client.images.generate({
    model: IMAGE_MODEL,
    prompt,
    size: "1024x1024",
    n: 1,
  });
  const b64 = result.data?.[0]?.b64_json;
  if (!b64) {
    throw new Error("Image API returned no image data");
  }
  await writeFile(outPath, Buffer.from(b64, "base64"));
}

export async function generateImages(
  prompts: { conceptArt: string; diffuse: string },
  paths: { conceptArt: string; diffuse: string },
): Promise<ImageResult> {
  if (!OPENAI_API_KEY) {
    return { skippedReason: "OPENAI_API_KEY is not set — image generation skipped (prompts saved)" };
  }
  const client = new OpenAI({ apiKey: OPENAI_API_KEY });

  await generateOne(client, prompts.conceptArt, paths.conceptArt);
  await generateOne(client, prompts.diffuse, paths.diffuse);

  return { conceptArtPath: paths.conceptArt, diffusePath: paths.diffuse };
}
