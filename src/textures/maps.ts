import sharp from "sharp";

/**
 * Derives auxiliary texture maps from a diffuse/albedo image, following the
 * channel layout Valve documents for Dota 2 items:
 *
 *   normal:  tangent-space normal map approximated from diffuse luminance
 *   mask1:   R detail mask, G diffuse fresnel warp, B metalness, A self-illumination
 *   mask2:   R specular intensity, G rim light, B base-tint spec, A spec exponent
 *
 * These are starting points for hand tuning, not final production maps.
 */

const SIZE = 1024;

async function loadGray(diffusePath: string): Promise<{ data: Buffer; w: number; h: number }> {
  const { data, info } = await sharp(diffusePath)
    .resize(SIZE, SIZE, { fit: "cover" })
    .grayscale()
    .raw()
    .toBuffer({ resolveWithObject: true });
  return { data, w: info.width, h: info.height };
}

/** Sobel-based normal map from luminance ("bump to normal"). */
export async function makeNormalMap(diffusePath: string, outPath: string, strength = 2.0): Promise<void> {
  const { data, w, h } = await loadGray(diffusePath);
  const out = Buffer.alloc(w * h * 3);

  const at = (x: number, y: number): number => {
    const cx = Math.min(w - 1, Math.max(0, x));
    const cy = Math.min(h - 1, Math.max(0, y));
    return data[cy * w + cx];
  };

  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const tl = at(x - 1, y - 1), t = at(x, y - 1), tr = at(x + 1, y - 1);
      const l = at(x - 1, y), r = at(x + 1, y);
      const bl = at(x - 1, y + 1), b = at(x, y + 1), br = at(x + 1, y + 1);

      const dx = ((tr + 2 * r + br) - (tl + 2 * l + bl)) / (4 * 255);
      const dy = ((bl + 2 * b + br) - (tl + 2 * t + tr)) / (4 * 255);

      let nx = -dx * strength;
      let ny = -dy * strength;
      let nz = 1.0;
      const len = Math.sqrt(nx * nx + ny * ny + nz * nz);
      nx /= len; ny /= len; nz /= len;

      const i = (y * w + x) * 3;
      out[i] = Math.round((nx * 0.5 + 0.5) * 255);
      out[i + 1] = Math.round((ny * 0.5 + 0.5) * 255);
      out[i + 2] = Math.round((nz * 0.5 + 0.5) * 255);
    }
  }

  await sharp(out, { raw: { width: w, height: h, channels: 3 } }).png().toFile(outPath);
}

function clamp255(v: number): number {
  return Math.min(255, Math.max(0, Math.round(v)));
}

/** mask1: R detail, G fresnel warp, B metalness, A self-illum. */
export async function makeMask1(diffusePath: string, outPath: string): Promise<void> {
  const { data, w, h } = await loadGray(diffusePath);
  const out = Buffer.alloc(w * h * 4);
  for (let i = 0; i < w * h; i++) {
    const lum = data[i];
    out[i * 4] = 0; // detail map off by default
    out[i * 4 + 1] = 128; // neutral fresnel warp
    out[i * 4 + 2] = clamp255((lum - 140) * 2); // bright surfaces read as metal
    out[i * 4 + 3] = 0; // no self-illumination by default
  }
  await sharp(out, { raw: { width: w, height: h, channels: 4 } }).png().toFile(outPath);
}

/** mask2: R spec intensity, G rim light, B tint-by-base spec, A spec exponent. */
export async function makeMask2(diffusePath: string, outPath: string): Promise<void> {
  const { data, w, h } = await loadGray(diffusePath);
  const out = Buffer.alloc(w * h * 4);
  for (let i = 0; i < w * h; i++) {
    const lum = data[i];
    out[i * 4] = clamp255(lum * 0.6 + 40); // spec follows luminance
    out[i * 4 + 1] = 60; // gentle rim light
    out[i * 4 + 2] = 255; // tint spec by base color
    out[i * 4 + 3] = clamp255(lum * 0.5 + 64); // spec exponent
  }
  await sharp(out, { raw: { width: w, height: h, channels: 4 } }).png().toFile(outPath);
}

export async function deriveAllMaps(
  diffusePath: string,
  outPaths: { normal: string; mask1: string; mask2: string },
): Promise<void> {
  await makeNormalMap(diffusePath, outPaths.normal);
  await makeMask1(diffusePath, outPaths.mask1);
  await makeMask2(diffusePath, outPaths.mask2);
}
