import { useState } from "react";
import type { SkinEntry } from "./types";

const RARITY_COLORS: Record<string, string> = {
  common: "#b0c3d9",
  uncommon: "#5e98d9",
  rare: "#4b69ff",
  mythical: "#8847ff",
  legendary: "#d32ce6",
  immortal: "#e4ae39",
  arcana: "#ade55c",
};

export function SkinCard({ skin }: { skin: SkinEntry }) {
  const [expanded, setExpanded] = useState(false);
  const c = skin.concept;
  const rarityColor = RARITY_COLORS[c.rarity] ?? "#b0c3d9";

  const textures = [
    // when the 3D viewer occupies the card header, surface the concept art here
    ...(skin.urls.model ? [{ label: "Concept", url: skin.urls.conceptArt }] : []),
    { label: "Diffuse", url: skin.urls.diffuse },
    { label: "Normal", url: skin.urls.normal },
    { label: "Mask 1", url: skin.urls.mask1 },
    { label: "Mask 2", url: skin.urls.mask2 },
  ].filter((t): t is { label: string; url: string } => t.url !== null);

  const meshDownloads = [
    { label: "GLB", url: skin.urls.model },
    { label: "FBX", url: skin.urls.modelFbx },
  ].filter((m): m is { label: string; url: string } => m.url !== null);

  return (
    <article className="card" style={{ borderColor: rarityColor }}>
      {skin.urls.model ? (
        <model-viewer
          className="hero-3d"
          src={skin.urls.model}
          alt={`3D модель: ${c.name}`}
          camera-controls
          auto-rotate
          shadow-intensity="1"
        />
      ) : skin.urls.conceptArt ? (
        <img className="hero-img" src={skin.urls.conceptArt} alt={c.name} loading="lazy" />
      ) : (
        <div className="hero-img placeholder">нет изображения — сохранены только промпты</div>
      )}

      <div className="card-body">
        <h2>{c.name}</h2>
        <div className="meta">
          <span className="hero">{c.hero}</span>
          <span className="slot">{c.slot}</span>
          <span className="rarity" style={{ color: rarityColor }}>
            {c.rarity}
          </span>
        </div>

        <div className="palette">
          {c.colorPalette.map((hex) => (
            <span key={hex} className="swatch" style={{ background: hex }} title={hex} />
          ))}
        </div>

        <p className="lore">{expanded ? c.lore : `${c.lore.slice(0, 140)}…`}</p>

        {expanded && (
          <>
            <h3>Визуал</h3>
            <p>{c.visualDescription}</p>
            <h3>Материалы</h3>
            <p>{c.materials.join(" · ")}</p>
            <h3>Эффекты</h3>
            <p>{c.particleEffects.join(" · ")}</p>
            {meshDownloads.length > 0 && (
              <>
                <h3>3D-модель</h3>
                <p className="mesh-links">
                  {meshDownloads.map((m) => (
                    <a key={m.label} href={m.url} download>
                      скачать {m.label}
                    </a>
                  ))}
                </p>
              </>
            )}
            {textures.length > 0 && (
              <>
                <h3>Текстуры</h3>
                <div className="textures">
                  {textures.map((t) => (
                    <a key={t.label} href={t.url} target="_blank" rel="noreferrer">
                      <img src={t.url} alt={t.label} loading="lazy" />
                      <span>{t.label}</span>
                    </a>
                  ))}
                </div>
              </>
            )}
          </>
        )}

        <button onClick={() => setExpanded(!expanded)}>
          {expanded ? "Свернуть" : "Подробнее"}
        </button>
      </div>
    </article>
  );
}
