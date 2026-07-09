# Dota 2 Skin Generator

CLI-приложение, которое с помощью LLM генерирует **скин-киты для Dota 2 Workshop**:
концепт предмета (имя, лор, палитра, материалы, эффекты), концепт-арт и diffuse-текстуру,
производные текстурные карты (normal, mask1, mask2 по спекам Valve), метаданные для формы
Workshop и инструкцию по импорту. Плюс локальная React-галерея для просмотра результатов.

- **Текст/концепт:** Claude API (`claude-opus-4-8`) со structured outputs (валидированный JSON)
- **Изображения:** OpenAI `gpt-image-1`
- **Карты:** normal/mask1/mask2 выводятся процедурно из diffuse (sharp)

## Честные ограничения

AI не создаёт 3D-меши. Готовый для загрузки в Workshop предмет требует модели с UV-развёрткой
(Blender/Maya). Этот инструмент генерирует всё *вокруг* модели: концепт, референсы, текстуры-заготовки,
метаданные и пошаговый план (`IMPORT.md` в каждом ките) — чтобы художник начинал не с нуля.

## Установка

```bash
npm install
cp .env.example .env   # вписать ключи
```

Ключи в `.env`:

| Переменная | Зачем | Обязательно |
|---|---|---|
| `ANTHROPIC_API_KEY` | генерация концепта (Claude) | да (кроме `--mock`) |
| `OPENAI_API_KEY` | генерация картинок (gpt-image-1) | нет — без него `--no-images` |

## Команды

```bash
# Сгенерировать скин
npm run cli -- generate --hero "Pudge" --slot weapon --theme "ледяной древний ужас"

# Опции:
#   --style "..."   доп. стилистическое направление
#   --no-images     пропустить генерацию картинок (сохранить только промпты)
#   --mock          оффлайн-прогон без API-ключей (фиксированный концепт + плейсхолдеры)

# Список героев и их слотов
npm run cli -- heroes

# Список сгенерированных скинов
npm run cli -- list

# Галерея в браузере (http://localhost:4173)
npm run cli -- preview
```

## Что получается

```
output/pudge-hook-of-the-sunken-maw/
├── concept.md          # лор, визуал, палитра, материалы, эффекты
├── item.json           # машиночитаемые метаданные + поля формы Workshop
├── prompts.txt         # использованные image-промпты (можно перегенерировать)
├── IMPORT.md           # шаги импорта в Dota 2 Workshop Tools
└── textures/
    ├── concept-art.png # AI-концепт-арт (референс для моделирования)
    ├── diffuse.png     # AI-albedo-референс
    ├── normal.png      # normal map (Sobel-аппроксимация из diffuse)
    ├── mask1.png       # R detail / G fresnel / B metalness / A self-illum
    └── mask2.png       # R spec / G rim / B tint-spec / A spec exponent
```

## Разработка

```bash
npm run typecheck        # проверка типов (CLI + preview)
npm run build:preview    # сборка React-галереи (preview/dist)

# e2e без ключей:
npm run cli -- generate --hero Pudge --slot weapon --theme test --mock
```

Структура: `src/` — CLI, Claude/OpenAI-модули, генерация карт, сборка пакета;
`preview/` — Vite + React галерея; сервер (`src/server/preview.ts`) раздаёт
собранную галерею и `/api/skins`.
