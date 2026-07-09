import { readFile, writeFile } from "node:fs/promises";
import { setTimeout as sleep } from "node:timers/promises";
import {
  MESHY_API_KEY,
  MESHY_BASE_URL,
  MESHY_POLL_INTERVAL_MS,
  MESHY_TIMEOUT_MS,
} from "../config.js";

/**
 * Meshy AI image-to-3D client.
 * Flow: POST /image-to-3d with the concept art as a base64 data URI,
 * poll GET /image-to-3d/{id} until SUCCEEDED, download model files.
 */

interface MeshyTask {
  id: string;
  status: "PENDING" | "IN_PROGRESS" | "SUCCEEDED" | "FAILED" | "CANCELED";
  progress?: number;
  model_urls?: Partial<Record<"glb" | "fbx" | "obj" | "usdz", string>>;
  task_error?: { message?: string };
}

export interface MeshResult {
  glbPath?: string;
  fbxPath?: string;
  objPath?: string;
  skippedReason?: string;
}

export interface MeshPaths {
  glb: string;
  fbx: string;
  obj: string;
}

async function meshyFetch(path: string, init?: RequestInit): Promise<Response> {
  const res = await fetch(`${MESHY_BASE_URL}${path}`, {
    ...init,
    headers: {
      Authorization: `Bearer ${MESHY_API_KEY}`,
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`Meshy API ${init?.method ?? "GET"} ${path} failed (${res.status}): ${body.slice(0, 300)}`);
  }
  return res;
}

async function download(url: string, outPath: string): Promise<void> {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Failed to download model file (${res.status}) from Meshy`);
  await writeFile(outPath, Buffer.from(await res.arrayBuffer()));
}

export async function generateMesh(
  conceptArtPath: string,
  outPaths: MeshPaths,
  opts?: { polycount?: number; onProgress?: (pct: number) => void },
): Promise<MeshResult> {
  if (!MESHY_API_KEY) {
    return { skippedReason: "MESHY_API_KEY is not set — 3D mesh generation skipped" };
  }

  const imageB64 = (await readFile(conceptArtPath)).toString("base64");

  const createRes = await meshyFetch("/image-to-3d", {
    method: "POST",
    body: JSON.stringify({
      image_url: `data:image/png;base64,${imageB64}`,
      topology: "quad",
      target_polycount: opts?.polycount ?? 30_000,
      should_texture: true,
      enable_pbr: true,
      target_formats: ["glb", "fbx", "obj"],
    }),
  });
  const { result: taskId } = (await createRes.json()) as { result: string };

  const deadline = Date.now() + MESHY_TIMEOUT_MS;
  let task: MeshyTask;
  for (;;) {
    if (Date.now() > deadline) {
      throw new Error(`Meshy task ${taskId} timed out after ${MESHY_TIMEOUT_MS / 60000} minutes`);
    }
    await sleep(MESHY_POLL_INTERVAL_MS);
    const pollRes = await meshyFetch(`/image-to-3d/${taskId}`);
    task = (await pollRes.json()) as MeshyTask;
    if (task.status === "SUCCEEDED") break;
    if (task.status === "FAILED" || task.status === "CANCELED") {
      throw new Error(`Meshy task ${task.status.toLowerCase()}: ${task.task_error?.message ?? "no details"}`);
    }
    opts?.onProgress?.(task.progress ?? 0);
  }

  const urls = task.model_urls ?? {};
  const result: MeshResult = {};
  if (urls.glb) {
    await download(urls.glb, outPaths.glb);
    result.glbPath = outPaths.glb;
  }
  if (urls.fbx) {
    await download(urls.fbx, outPaths.fbx);
    result.fbxPath = outPaths.fbx;
  }
  if (urls.obj) {
    await download(urls.obj, outPaths.obj);
    result.objPath = outPaths.obj;
  }
  if (!result.glbPath && !result.fbxPath && !result.objPath) {
    throw new Error("Meshy task succeeded but returned no model files");
  }
  return result;
}
