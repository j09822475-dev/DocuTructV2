# Dota 2 Skin Generator

CLI-приложение, которое с помощью LLM генерирует **скин-киты для Dota 2 Workshop**:
концепт предмета (имя, лор, палитра, материалы, эффекты), концепт-арт и diffuse-текстуру,
**3D-меш (GLB/FBX/OBJ)**, производные текстурные карты (normal, mask1, mask2 по спекам Valve),
метаданные для формы Workshop и инструкцию по импорту. Плюс локальная React-галерея
с интерактивным 3D-просмотром.

- **Текст/концепт:** Claude API (`claude-opus-4-8`) со structured outputs (валидированный JSON)
- **Изображения:** OpenAI `gpt-image-1`
- **3D-меш:** Meshy AI image-to-3D — меш строится из сгенерированного концепт-арта
- **Карты:** normal/mask1/mask2 выводятся процедурно из diffuse (sharp)

## Честные ограничения

Сгенерированный меш — это **концепт-меш**: правильные формы и текстуры, но топология не
подходит для ригов Dota 2. Перед загрузкой в Workshop его нужно ретопологизировать в
Blender/Maya (меш отлично работает как база для ретопо и как источник запекания normal map).
`IMPORT.md` в каждом ките описывает шаги.

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
| `MESHY_API_KEY` | генерация 3D-меша (Meshy image-to-3D) | нет — без него меш-шаг пропускается |

## Команды

```bash
# Сгенерировать скин
npm run cli -- generate --hero "Pudge" --slot weapon --theme "ледяной древний ужас"

# Опции:
#   --style "..."     доп. стилистическое направление
#   --no-images       пропустить генерацию картинок (сохранить только промпты)
#   --no-mesh         пропустить генерацию 3D-меша
#   --polycount <n>   целевой поликаунт меша (по умолчанию 30000)
#   --mock            оффлайн-прогон без API-ключей (фиксированный концепт + плейсхолдеры)

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
├── model/
│   ├── model.glb       # AI-меш (Meshy) — смотрится в галерее и Blender
│   ├── model.fbx       # тот же меш для Blender/Maya (ретопология)
│   └── model.obj
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
