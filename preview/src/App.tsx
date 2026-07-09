import { useEffect, useState } from "react";
import type { SkinEntry } from "./types";
import { SkinCard } from "./SkinCard";

export function App() {
  const [skins, setSkins] = useState<SkinEntry[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch("/api/skins")
      .then((r) => {
        if (!r.ok) throw new Error(`API error ${r.status}`);
        return r.json();
      })
      .then(setSkins)
      .catch((e) => setError(String(e)));
  }, []);

  return (
    <div className="app">
      <header>
        <h1>
          Dota 2 <span className="accent">Skin Generator</span>
        </h1>
        <p className="sub">Концепты и текстур-киты, сгенерированные Claude + gpt-image</p>
      </header>

      {error && <div className="error">Не удалось загрузить скины: {error}</div>}
      {!error && skins === null && <div className="loading">Загрузка…</div>}
      {skins !== null && skins.length === 0 && (
        <div className="empty">
          Пока пусто. Сгенерируйте первый скин:
          <pre>npm run cli -- generate --hero Pudge --slot weapon --theme "ancient frost"</pre>
        </div>
      )}

      <div className="grid">
        {skins?.map((s) => (
          <SkinCard key={s.id} skin={s} />
        ))}
      </div>
    </div>
  );
}
