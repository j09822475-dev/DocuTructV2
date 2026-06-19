# Публикация Kuller в Google Play — пошаговая инструкция

Этот файл проведёт вас от пустого аккаунта до приложения в Google Play.
Все артефакты для магазина уже лежат рядом:

```
play/
├── PUBLISHING.md            ← вы здесь
├── privacy-policy.md        ← политика конфиденциальности (нужен публичный URL)
├── listing/STORE_LISTING.md ← тексты для карточки (RU/ET/EN)
└── graphics/
    ├── icon-512.png             (иконка 512×512)
    ├── feature-1024x500.png     (feature graphic)
    ├── screen-1-restaurant.png  (скриншот 1080×2280)
    ├── screen-2-client.png
    └── screen-3-home.png
```

> ⚠️ Графика в `graphics/` — это рабочие заготовки-плейсхолдеры (сгенерированы
> программно). Для красивой карточки замените их на реальные скриншоты
> приложения. Требования размеров уже выдержаны, можно публиковаться и с ними.

---

## Шаг 0. Что понадобится

- Аккаунт Google.
- **25 USD** — единоразовый взнос за аккаунт разработчика Google Play.
- Карта для оплаты.
- ~1–2 часа времени (плюс ожидание проверки Google: от пары часов до нескольких дней).

---

## Шаг 1. Создать аккаунт разработчика

1. Откройте https://play.google.com/console и войдите в Google-аккаунт.
2. Выберите тип **Personal** (личный) — для физлица.
3. Заполните имя разработчика, адрес, телефон. Подтвердите личность (Google
   может попросить документ).
4. Оплатите единоразовый взнос **25 USD**.
5. Дождитесь подтверждения аккаунта (обычно быстро, иногда до 48 ч).

> Для **личных** аккаунтов, созданных после ноября 2023, Google требует
> **закрытое тестирование с 12 тестировщиками в течение 14 дней** перед выпуском
> в продакшн. Это нормально — см. Шаг 8.

---

## Шаг 2. Сгенерировать ключ загрузки (upload keystore)

Подпись нужна, чтобы собрать релизный `.aab`. Ключ создаётся **один раз** и
хранится у вас (в Git его коммитить НЕЛЬЗЯ). Выполните на своём компьютере
(нужен установленный JDK — команда `keytool` входит в него):

```bash
keytool -genkeypair -v \
  -keystore upload.jks \
  -alias kuller \
  -keyalg RSA -keysize 2048 -validity 9125 \
  -storepass "ПРИДУМАЙТЕ_ПАРОЛЬ" \
  -keypass "ПРИДУМАЙТЕ_ПАРОЛЬ" \
  -dname "CN=Kuller, OU=Mobile, O=Kuller, L=Tallinn, S=Harju, C=EE"
```

- `validity 9125` = 25 лет (Google требует срок действия минимум до 2033 г.).
- Запомните **пароль** и **alias** (`kuller`) — без них вы не сможете обновлять
  приложение. Сделайте резервную копию файла `upload.jks` в надёжном месте.

> 💡 Рекомендуется включить **Play App Signing** (по умолчанию включён): тогда
> Google хранит «боевой» ключ подписи, а ваш `upload.jks` — только ключ
> загрузки. Если потеряете upload-ключ, Google поможет сбросить его.

---

## Шаг 3. Передать ключ в GitHub Actions (сборка `.aab` в CI)

CI (`.github/workflows/android.yml`) уже умеет подписывать бандл, если в
секретах репозитория заданы 4 значения. Без них бандл соберётся с debug-ключом
(годится только для проверки, не для Play).

1. Закодируйте keystore в base64:

   ```bash
   base64 -w0 upload.jks > upload.jks.base64   # Linux
   # macOS: base64 -i upload.jks -o upload.jks.base64
   ```

2. В GitHub: **Settings → Secrets and variables → Actions → New repository secret**
   создайте 4 секрета:

   | Имя секрета         | Значение                                  |
   |---------------------|-------------------------------------------|
   | `KEYSTORE_BASE64`   | содержимое файла `upload.jks.base64`      |
   | `KEYSTORE_PASSWORD` | пароль хранилища (из Шага 2)              |
   | `KEY_ALIAS`         | `kuller`                                  |
   | `KEY_PASSWORD`      | пароль ключа (из Шага 2)                  |

3. Запустите сборку: запушьте любой коммит в ветку
   `claude/estonian-learning-simulator-eXkAJ` или вручную через
   **Actions → Build Kuller APK → Run workflow**.

4. Когда CI завершится, скачайте `app-release.aab`:
   - со страницы **Releases** репозитория (тег `kuller-latest`), либо
   - из артефактов прогона (**Actions → нужный run → Artifacts → kuller-release-aab**).

> Альтернатива без CI: собрать локально командой
> `KEYSTORE_FILE=$PWD/upload.jks KEYSTORE_PASSWORD=… KEY_ALIAS=kuller KEY_PASSWORD=… ./gradlew bundleRelease`
> — файл появится в `app/build/outputs/bundle/release/app-release.aab`.

---

## Шаг 4. Опубликовать политику конфиденциальности (публичный URL)

Play требует **публичную ссылку** на политику. Самый простой способ — GitHub:

- Включите **GitHub Pages** (Settings → Pages) или просто используйте «raw»-ссылку
  на `play/privacy-policy.md`, например:
  `https://raw.githubusercontent.com/<owner>/<repo>/claude/estonian-learning-simulator-eXkAJ/estonian-courier-sim/play/privacy-policy.md`
- Либо вставьте текст из `privacy-policy.md` на любой бесплатный хостинг страниц.

Сохраните полученный URL — пригодится в Шаге 7.

---

## Шаг 5. Создать приложение в Play Console

1. **Play Console → All apps → Create app**.
2. App name: `Kuller` (можно `Kuller: эстонский курьеру`).
3. Default language: **Russian (ru-RU)**.
4. App or game: **Game**. Free or paid: **Free**.
5. Примите декларации (правила разработчика, экспортное законодательство США).

---

## Шаг 6. Загрузить бандл (внутреннее тестирование)

1. **Testing → Internal testing → Create new release**.
2. Загрузите `app-release.aab` (из Шага 3).
3. При первой загрузке согласитесь на **Play App Signing**.
4. Release name заполнится автоматически (versionCode 1). Release notes —
   например: «Первая версия».
5. **Save → Review release → Start rollout to Internal testing**.
6. Добавьте себя как тестировщика (вкладка **Testers**), скопируйте
   opt-in ссылку и установите приложение на телефон для проверки.

---

## Шаг 7. Заполнить карточку и обязательные разделы

В разделе **Grow → Store presence → Main store listing** возьмите тексты из
`listing/STORE_LISTING.md`:

- **App name**, **Short description**, **Full description** — язык RU (затем
  добавьте переводы ET/EN через «Manage translations»).
- **App icon**: `graphics/icon-512.png`.
- **Feature graphic**: `graphics/feature-1024x500.png`.
- **Phone screenshots**: три файла `graphics/screen-*.png` (минимум 2).

Заполните разделы из левого меню (значок ✔ должен загореться у каждого):

- **Privacy policy** → вставьте URL из Шага 4.
- **App access** → «All functionality is available without restrictions».
- **Ads** → **No ads** (рекламы нет).
- **Content rating** → пройдите анкету (категория: всё «нет» → рейтинг
  Everyone / 3+).
- **Target audience and content** → выберите возрастные группы (можно 13+/18+,
  чтобы не попадать под строгие правила для детей). Приложение не нацелено на детей.
- **Data safety** → заполните по `privacy-policy.md`: **данные не собираются и
  не передаются** (No data collected, No data shared). См. ниже.
- **Government apps / Financial features / Health** → No.
- **Category**: **Education** (или Educational game).

### Data safety — что отвечать

- Does your app collect or share any required user data types? → **No**.
- Is all of the user data encrypted in transit? → не применимо (данных нет).
- Do you provide a way for users to request data deletion? → данные хранятся
  только локально; пользователь удаляет их кнопкой «Сбросить прогресс» и при
  удалении приложения.

---

## Шаг 8. Закрытое тестирование (требование для личных аккаунтов)

Если аккаунт личный и создан после ноября 2023:

1. **Testing → Closed testing → Create track** (например, «alpha»).
2. Загрузите тот же `.aab`, добавьте список email тестировщиков (нужно **12+**).
3. Тестирование должно идти **минимум 14 дней** подряд с активными тестерами.
4. После этого Play Console разрешит подать заявку на **Production**.

(Если у вас организационный аккаунт — этот шаг можно пропустить.)

---

## Шаг 9. Выпуск в продакшн

1. **Production → Create new release** → загрузите `.aab` (или продвиньте сборку
   из тестового трека кнопкой «Promote release»).
2. Заполните release notes.
3. **Review release** → исправьте оставшиеся предупреждения → **Start rollout to
   Production**.
4. Дождитесь проверки Google (от нескольких часов до нескольких дней). После
   одобрения приложение появится в магазине.

---

## Чек-лист перед отправкой

- [ ] Аккаунт разработчика создан и оплачен (25 USD).
- [ ] `upload.jks` сгенерирован и сохранён в бэкап.
- [ ] 4 секрета добавлены в GitHub, CI собрал подписанный `app-release.aab`.
- [ ] Политика конфиденциальности доступна по публичному URL.
- [ ] Карточка заполнена (RU + переводы), загружены иконка/feature/скриншоты.
- [ ] Content rating, Data safety, Target audience, Ads — заполнены.
- [ ] Бандл загружен во внутренний трек и проверен на телефоне.
- [ ] (Личный аккаунт) пройдено 14-дневное закрытое тестирование с 12 тестерами.
- [ ] Выпуск в Production отправлен на проверку.

---

## ⚠️ Важно про товарные знаки (Bolt / Wolt)

«Bolt» и «Wolt» — зарегистрированные торговые марки. Чтобы карточку не
отклонили:

- **Название и иконка** не должны содержать «Bolt» или «Wolt» (сейчас — `Kuller`,
  всё ок).
- В описании уже есть дисклеймер «не связано с Bolt/Wolt».
- В диалогах внутри приложения слово «Bolt» встречается как пример курьерского
  приложения. Это обычно допустимо, но для максимально чистой подачи в Play их
  можно заменить на нейтральное «kulleri äpp» / «приложение». Скажите — заменю.
