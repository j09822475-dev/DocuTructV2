import "dotenv/config";
import { join } from "node:path";

export const OUTPUT_DIR = process.env.DOTA2_SKINS_OUTPUT ?? join(process.cwd(), "output");

export const ANTHROPIC_API_KEY = process.env.ANTHROPIC_API_KEY;
export const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
export const MESHY_API_KEY = process.env.MESHY_API_KEY;

export const CLAUDE_MODEL = "claude-opus-4-8";
export const IMAGE_MODEL = "gpt-image-1";

export const MESHY_BASE_URL = "https://api.meshy.ai/openapi/v1";
export const MESHY_POLL_INTERVAL_MS = 8_000;
export const MESHY_TIMEOUT_MS = 12 * 60_000;

export function requireAnthropicKey(): string {
  if (!ANTHROPIC_API_KEY) {
    throw new Error(
      "ANTHROPIC_API_KEY is not set. Copy .env.example to .env and add your key, " +
        "or run with --mock for an offline dry run.",
    );
  }
  return ANTHROPIC_API_KEY;
}
