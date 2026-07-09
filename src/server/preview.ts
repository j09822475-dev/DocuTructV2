import express from "express";
import { existsSync } from "node:fs";
import { spawnSync } from "node:child_process";
import { basename, dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { OUTPUT_DIR } from "../config.js";
import { listPackages } from "../output/package.js";

const here = dirname(fileURLToPath(import.meta.url));
const projectRoot = join(here, "..", "..");
const previewDist = join(projectRoot, "preview", "dist");

function ensurePreviewBuilt(): void {
  if (existsSync(join(previewDist, "index.html"))) return;
  console.log("⚙ Building preview UI (first run)...");
  const result = spawnSync("npx", ["vite", "build", "preview"], {
    cwd: projectRoot,
    stdio: "inherit",
  });
  if (result.status !== 0) {
    throw new Error("Failed to build the preview UI (vite build preview)");
  }
}

export async function startPreviewServer(port: number): Promise<void> {
  ensurePreviewBuilt();

  const app = express();

  app.get("/api/skins", async (_req, res) => {
    const entries = await listPackages();
    res.json(
      entries.map((e) => {
        const dirName = basename(e.dir);
        const tex = (file: string, present: boolean) =>
          present ? `/files/${dirName}/textures/${file}` : null;
        return {
          id: dirName,
          concept: e.concept,
          generatedAt: e.generatedAt,
          urls: {
            conceptArt: tex("concept-art.png", e.images.conceptArt),
            diffuse: tex("diffuse.png", e.images.diffuse),
            normal: tex("normal.png", e.images.normal),
            mask1: tex("mask1.png", e.images.masks),
            mask2: tex("mask2.png", e.images.masks),
          },
        };
      }),
    );
  });

  app.use("/files", express.static(OUTPUT_DIR));
  app.use(express.static(previewDist));
  // SPA fallback
  app.get("*", (_req, res) => res.sendFile(join(previewDist, "index.html")));

  await new Promise<void>((resolve) => app.listen(port, resolve));
  console.log(`✓ Preview running at http://localhost:${port}`);
}
