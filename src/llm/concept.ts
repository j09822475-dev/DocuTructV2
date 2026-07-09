import Anthropic from "@anthropic-ai/sdk";
import { zodOutputFormat } from "@anthropic-ai/sdk/helpers/zod";
import { CLAUDE_MODEL, requireAnthropicKey } from "../config.js";
import type { Hero, ItemSlot } from "../data/heroes.js";
import { SkinConceptSchema, type SkinConcept } from "./schema.js";

const SYSTEM_PROMPT = `You are a senior Dota 2 Workshop item designer. You know Valve's character art
guidelines by heart: items must preserve the hero's silhouette readability at game camera distance,
respect the hero's color identity (accents may deviate, the core palette may not), avoid visual noise,
and use gradients that darken toward the ground. Rarity should match ambition: subtle retextures are
uncommon/rare, new silhouettes with custom particles are mythical/legendary.

You produce complete, production-oriented skin concepts. Lore is written in Valve's terse, wry Dota 2
voice. Visual descriptions are written for a 3D artist who will sculpt and texture the item.
Texture prompts are written for an image-generation model:
- conceptArt prompt: a dramatic full render, painterly, Dota 2 art style, dark neutral background.
- diffuse prompt: a flat albedo material sheet, even studio lighting, no shadows, no scene, top-down
  view of the surface materials, suitable as a texture painting reference.`;

export interface ConceptRequest {
  hero: Hero;
  slot: ItemSlot;
  theme: string;
  style?: string;
}

export async function generateConcept(req: ConceptRequest): Promise<SkinConcept> {
  const client = new Anthropic({ apiKey: requireAnthropicKey() });

  const userPrompt = [
    `Design a Dota 2 Workshop item concept.`,
    ``,
    `Hero: ${req.hero.name} (${req.hero.attribute})`,
    `Hero visual identity: ${req.hero.identity}`,
    `Item slot: ${req.slot}`,
    `Theme requested by the user: ${req.theme}`,
    req.style ? `Additional style direction: ${req.style}` : ``,
    ``,
    `Set the "hero" field to exactly "${req.hero.name}" and the "slot" field to exactly "${req.slot}".`,
  ]
    .filter(Boolean)
    .join("\n");

  const response = await client.messages.parse({
    model: CLAUDE_MODEL,
    max_tokens: 16000,
    system: SYSTEM_PROMPT,
    messages: [{ role: "user", content: userPrompt }],
    output_config: { format: zodOutputFormat(SkinConceptSchema) },
  });

  const concept = response.parsed_output;
  if (!concept) {
    throw new Error(
      `Claude did not return a parseable concept (stop_reason: ${response.stop_reason}).`,
    );
  }
  return concept;
}
