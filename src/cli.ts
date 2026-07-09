import { Command } from "commander";
import { relative } from "node:path";
import { HEROES, ITEM_SLOTS, findHero, type Hero, type ItemSlot } from "./data/heroes.js";
import { generateConcept } from "./llm/concept.js";
import { generateImages } from "./images/generate.js";
import { deriveAllMaps } from "./textures/maps.js";
import { preparePackageDir, writePackage, listPackages } from "./output/package.js";
import { mockConcept, mockImage } from "./mock.js";

const program = new Command();

program
  .name("dota2-skins")
  .description("LLM-powered Dota 2 skin concept & texture kit generator (Claude + gpt-image)")
  .version("0.1.0");

program
  .command("generate")
  .description("Generate a skin concept + texture kit for a hero")
  .requiredOption("--hero <name>", `hero name, e.g. "Pudge" (see \`heroes\` command)`)
  .requiredOption("--slot <slot>", `item slot: ${ITEM_SLOTS.join(", ")}`)
  .requiredOption("--theme <theme>", `theme/mood, e.g. "ancient frost horror"`)
  .option("--style <style>", "extra style direction passed to the concept model")
  .option("--no-images", "skip image generation, save prompts only")
  .option("--mock", "offline dry run: fixed concept + placeholder images, no API calls")
  .action(async (opts: { hero: string; slot: string; theme: string; style?: string; images: boolean; mock?: boolean }) => {
    const hero: Hero | undefined = findHero(opts.hero);
    if (!hero) {
      console.error(`Unknown hero "${opts.hero}". Known heroes:\n  ${HEROES.map((h) => h.name).join(", ")}`);
      process.exit(1);
    }
    const slot = opts.slot as ItemSlot;
    if (!ITEM_SLOTS.includes(slot)) {
      console.error(`Unknown slot "${opts.slot}". Valid slots: ${ITEM_SLOTS.join(", ")}`);
      process.exit(1);
    }
    if (!hero.slots.includes(slot)) {
      console.warn(`⚠ ${hero.name} does not normally have a "${slot}" slot (has: ${hero.slots.join(", ")}). Continuing anyway.`);
    }

    console.log(`⚙ Generating concept for ${hero.name} / ${slot} / "${opts.theme}"${opts.mock ? " (mock)" : ""}...`);
    const concept = opts.mock
      ? mockConcept(hero, slot, opts.theme)
      : await generateConcept({ hero, slot, theme: opts.theme, style: opts.style });

    console.log(`✓ Concept: "${concept.name}" [${concept.rarity}]`);

    const paths = await preparePackageDir(concept);
    let imagesSkippedReason: string | undefined;

    if (opts.mock) {
      console.log("⚙ Writing placeholder images (mock)...");
      await mockImage(paths.conceptArt, concept.colorPalette);
      await mockImage(paths.diffuse, concept.colorPalette);
    } else if (!opts.images) {
      imagesSkippedReason = "--no-images flag";
      console.log("↷ Image generation skipped (--no-images); prompts saved to prompts.txt");
    } else {
      console.log("⚙ Generating concept art & diffuse texture (gpt-image-1)...");
      const result = await generateImages(concept.texturePrompts, {
        conceptArt: paths.conceptArt,
        diffuse: paths.diffuse,
      });
      if (result.skippedReason) {
        imagesSkippedReason = result.skippedReason;
        console.warn(`↷ ${result.skippedReason}`);
      }
    }

    if (!imagesSkippedReason) {
      console.log("⚙ Deriving normal & mask maps from diffuse...");
      await deriveAllMaps(paths.diffuse, {
        normal: paths.normal,
        mask1: paths.mask1,
        mask2: paths.mask2,
      });
    }

    await writePackage(concept, paths, { imagesSkippedReason });
    console.log(`\n✓ Skin kit written to ${relative(process.cwd(), paths.dir)}/`);
    console.log(`  Next: read IMPORT.md inside the kit, or run \`npm run cli -- preview\` to browse.`);
  });

program
  .command("list")
  .description("List generated skin kits")
  .action(async () => {
    const entries = await listPackages();
    if (entries.length === 0) {
      console.log("No skins generated yet. Try: npm run cli -- generate --hero Pudge --slot weapon --theme \"ancient frost\"");
      return;
    }
    for (const e of entries) {
      const imgs = e.images.conceptArt ? "🖼 " : "   ";
      console.log(`${imgs}${e.concept.name}  —  ${e.concept.hero} / ${e.concept.slot} / ${e.concept.rarity}  (${relative(process.cwd(), e.dir)})`);
    }
  });

program
  .command("heroes")
  .description("List known heroes and their item slots")
  .action(() => {
    for (const h of HEROES) {
      console.log(`${h.name.padEnd(18)} [${h.attribute}]  slots: ${h.slots.join(", ")}`);
    }
  });

program
  .command("preview")
  .description("Serve the React gallery of generated skins")
  .option("--port <port>", "port to listen on", "4173")
  .action(async (opts: { port: string }) => {
    const { startPreviewServer } = await import("./server/preview.js");
    await startPreviewServer(Number(opts.port));
  });

program.parseAsync(process.argv).catch((err: unknown) => {
  console.error(`✗ ${err instanceof Error ? err.message : String(err)}`);
  process.exit(1);
});
