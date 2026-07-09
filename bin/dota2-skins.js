#!/usr/bin/env node
// Thin launcher so the CLI works both via `npm run cli` and as an installed bin.
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { spawnSync } from "node:child_process";

const here = dirname(fileURLToPath(import.meta.url));
const cliEntry = join(here, "..", "src", "cli.ts");
const tsx = join(here, "..", "node_modules", ".bin", "tsx");

const result = spawnSync(tsx, [cliEntry, ...process.argv.slice(2)], {
  stdio: "inherit",
});
process.exit(result.status ?? 1);
