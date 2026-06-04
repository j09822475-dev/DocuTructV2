package ee.kuller.app.data

import ee.kuller.app.model.Category
import ee.kuller.app.model.Choice
import ee.kuller.app.model.Order
import ee.kuller.app.model.Speaker
import ee.kuller.app.model.Turn
import ee.kuller.app.model.WordEntry

/**
 * Весь учебный контент приложения «Kuller».
 * Эстонский язык для русскоговорящих в формате симулятора курьера.
 */
object Content {

    // ----------------------------------------------------------------------
    // СЛОВАРЬ ПО ТЕМАМ
    // ----------------------------------------------------------------------
    val categories: List<Category> = listOf(
        Category(
            id = "greet", titleEt = "Tervitused", titleRu = "Приветствия", emoji = "👋",
            words = listOf(
                WordEntry("g_tere", "Tere", "Здравствуйте / Привет", "Tere, kuidas läheb?"),
                WordEntry("g_hommik", "Tere hommikust", "Доброе утро", "Tere hommikust!"),
                WordEntry("g_ohtu", "Tere õhtust", "Добрый вечер", "Tere õhtust!"),
                WordEntry("g_head_aega", "Head aega", "До свидания", "Head aega, nägemist!"),
                WordEntry("g_nagemist", "Nägemist", "Увидимся / Пока", "Nägemist homme!"),
                WordEntry("g_aitah", "Aitäh", "Спасибо", "Aitäh, väga lahke!"),
                WordEntry("g_palun", "Palun", "Пожалуйста", "Palun, siin on teie tellimus."),
                WordEntry("g_vabandust", "Vabandust", "Извините", "Vabandust, ma hilinen."),
                WordEntry("g_jah", "Jah", "Да", "Jah, muidugi."),
                WordEntry("g_ei", "Ei", "Нет", "Ei, aitäh."),
            )
        ),
        Category(
            id = "food", titleEt = "Toit", titleRu = "Еда", emoji = "🍔",
            words = listOf(
                WordEntry("f_pitsa", "Pitsa", "Пицца", "Üks pepperoni pitsa."),
                WordEntry("f_burger", "Burger", "Бургер", "Kanaburger friikartulitega."),
                WordEntry("f_supp", "Supp", "Суп", "Kuum kanasupp."),
                WordEntry("f_salat", "Salat", "Салат", "Kreeka salat."),
                WordEntry("f_kana", "Kana", "Курица", "Praetud kana."),
                WordEntry("f_kala", "Kala", "Рыба", "Värske kala."),
                WordEntry("f_leib", "Leib", "Чёрный хлеб", "Must leib."),
                WordEntry("f_sai", "Sai", "Белый хлеб / булка", "Pehme sai."),
                WordEntry("f_juust", "Juust", "Сыр", "Lisa juustu."),
                WordEntry("f_friikad", "Friikartulid", "Картофель фри", "Suured friikartulid."),
            )
        ),
        Category(
            id = "drink", titleEt = "Joogid", titleRu = "Напитки", emoji = "☕",
            words = listOf(
                WordEntry("d_vesi", "Vesi", "Вода", "Klaas vett, palun."),
                WordEntry("d_kohv", "Kohv", "Кофе", "Üks kohv piimaga."),
                WordEntry("d_tee", "Tee", "Чай", "Roheline tee."),
                WordEntry("d_mahl", "Mahl", "Сок", "Õunamahl."),
                WordEntry("d_piim", "Piim", "Молоко", "Kohv piimaga."),
                WordEntry("d_limonaad", "Limonaad", "Лимонад", "Külm limonaad."),
                WordEntry("d_olu", "Õlu", "Пиво", "Üks õlu, palun."),
                WordEntry("d_kakao", "Kakao", "Какао", "Soe kakao."),
            )
        ),
        Category(
            id = "num", titleEt = "Numbrid", titleRu = "Числа", emoji = "🔢",
            words = listOf(
                WordEntry("n_1", "Üks", "Один", "Üks pitsa."),
                WordEntry("n_2", "Kaks", "Два", "Kaks kohvi."),
                WordEntry("n_3", "Kolm", "Три", "Kolm burgerit."),
                WordEntry("n_4", "Neli", "Четыре", "Neli korterit."),
                WordEntry("n_5", "Viis", "Пять", "Korter number viis."),
                WordEntry("n_6", "Kuus", "Шесть", "Kuus eurot."),
                WordEntry("n_7", "Seitse", "Семь", "Kell seitse."),
                WordEntry("n_8", "Kaheksa", "Восемь", "Kaheksa minutit."),
                WordEntry("n_9", "Üheksa", "Девять", "Üheksa eurot."),
                WordEntry("n_10", "Kümme", "Десять", "Kümme eurot."),
            )
        ),
        Category(
            id = "dir", titleEt = "Suunad", titleRu = "Направления", emoji = "🧭",
            words = listOf(
                WordEntry("s_paremale", "Paremale", "Направо", "Pööra paremale."),
                WordEntry("s_vasakule", "Vasakule", "Налево", "Pööra vasakule."),
                WordEntry("s_otse", "Otse", "Прямо", "Sõida otse."),
                WordEntry("s_tagasi", "Tagasi", "Назад", "Keera tagasi."),
                WordEntry("s_korrus", "Korrus", "Этаж", "Kolmas korrus."),
                WordEntry("s_uks", "Uks", "Дверь", "Esimene uks."),
                WordEntry("s_ristmik", "Ristmik", "Перекрёсток", "Ristmikul pööra."),
                WordEntry("s_maja", "Maja", "Дом", "Kollane maja."),
                WordEntry("s_korter", "Korter", "Квартира", "Korter number viis."),
                WordEntry("s_lift", "Lift", "Лифт", "Lift on katki."),
            )
        ),
        Category(
            id = "phrase", titleEt = "Kulleri fraasid", titleRu = "Фразы курьера", emoji = "🛵",
            words = listOf(
                WordEntry("p_tellimus", "Teil on tellimus", "У вас заказ", "Tere, teil on tellimus Boltist."),
                WordEntry("p_kohal", "Olen kohal", "Я на месте", "Olen kohal, ootan teid all."),
                WordEntry("p_alla", "Kas saate alla tulla?", "Можете спуститься?", "Olen ukse ees, kas saate alla tulla?"),
                WordEntry("p_korrus", "Millisel korrusel te olete?", "На каком вы этаже?", "Vabandust, millisel korrusel te olete?"),
                WordEntry("p_ukse_taha", "Jätan ukse taha", "Оставлю под дверью", "Selge, jätan toidu ukse taha."),
                WordEntry("p_head_isu", "Head isu!", "Приятного аппетита!", "Palun, siin on tellimus. Head isu!"),
                WordEntry("p_valmis", "Tellimus on valmis", "Заказ готов", "Teie tellimus on valmis."),
                WordEntry("p_jargi", "Tulin tellimusele järele", "Я пришёл за заказом", "Tere, tulin tellimusele järele."),
                // Вопросы, которые задаёт сам курьер
                WordEntry("p_valmis_q", "Kas tellimus on valmis?", "Заказ готов?", "Tere! Kas tellimus on juba valmis?"),
                WordEntry("p_nimi_q", "Mis nimi on tellimusel?", "На какое имя заказ?", "Mis nimi on tellimusel?"),
                WordEntry("p_oige_q", "Kas see on õige tellimus?", "Это верный заказ?", "Vabandust, kas see on õige tellimus?"),
                WordEntry("p_veel_q", "Kas tuleb veel midagi?", "Будет ещё что-то к заказу?", "Kas tuleb veel midagi kaasa?"),
                WordEntry("p_korter_q", "Mis on teie korterinumber?", "Какой у вас номер квартиры?", "Mis on teie korterinumber?"),
                WordEntry("p_kus_q", "Kus ma teid leian?", "Где мне вас найти?", "Olen kohal. Kus ma teid leian?"),
                WordEntry("p_kood_q", "Mis on ukse kood?", "Какой код двери?", "Värav on lukus. Mis on ukse kood?"),
                WordEntry("p_alla_voi_q", "Kas tulete alla või tulen üles?", "Вы спуститесь или мне подняться?", "Lift on katki. Kas tulete alla või tulen üles?"),
                // Оплата, проблемы с адресом, клиента нет дома
                WordEntry("p_maksate_q", "Kas maksate sularahas või kaardiga?", "Платите наличными или картой?", "Tere! Kas maksate sularahas või kaardiga?"),
                WordEntry("p_aadress_q", "Kas see aadress on õige?", "Этот адрес верный?", "Vabandust, kas see aadress on õige?"),
                WordEntry("p_kus_maja_q", "Kus maja täpselt asub?", "Где именно находится дом?", "Kus maja täpselt asub?"),
                WordEntry("p_helistan", "Ma helistan teile", "Я вам позвоню", "Ma helistan teile kohe."),
                WordEntry("p_ootan", "Ma ootan paar minutit", "Я подожду пару минут", "Hästi, ma ootan paar minutit."),
                WordEntry("p_foto", "Teen tellimusest foto", "Сделаю фото заказа", "Jätan ukse taha ja teen foto."),
            )
        ),
        Category(
            id = "money", titleEt = "Raha", titleRu = "Деньги", emoji = "💶",
            words = listOf(
                WordEntry("m_sularaha", "Sularaha", "Наличные", "Kas maksate sularahas?"),
                WordEntry("m_kaart", "Kaart", "Карта", "Maksan kaardiga."),
                WordEntry("m_kviitung", "Kviitung", "Чек", "Siin on teie kviitung."),
                WordEntry("m_vahetusraha", "Vahetusraha", "Сдача", "Siin on vahetusraha."),
                WordEntry("m_hind", "Hind", "Цена", "Mis on hind?"),
                WordEntry("m_maksab", "See maksab", "Это стоит", "See maksab kümme eurot."),
                WordEntry("m_eurot", "Eurot", "Евро", "Kümme eurot."),
            )
        ),
    )

    val allWords: List<WordEntry> = categories.flatMap { it.words }
    fun word(id: String): WordEntry? = allWords.firstOrNull { it.id == id }

    // ----------------------------------------------------------------------
    // ЗАКАЗЫ — чат со СТРОГИМ чередованием: реплика NPC ↔ ответ курьера.
    // После каждого ответа появляется ровно ОДНА реплика собеседника.
    // Служебные подсказки (навигатор и т.п.) вынесены в текст задания.
    // ----------------------------------------------------------------------

    private fun say(speaker: Speaker, et: String, ru: String): Turn = Turn.Say(speaker, et, ru)

    private fun ask(
        promptRu: String,
        courierAsks: Boolean,
        choices: List<Choice>,
        teach: List<String> = emptyList()
    ): Turn = Turn.Ask(promptRu, courierAsks, choices, teach)

    /**
     * Получение заказа в ресторане (логичный порядок):
     * [Ask] курьер «готов?» → [Say] ресторан называет заказ → [Ask] курьер подтверждает
     * → [Say] ресторан «почти готово, минутку… держите». Заканчивается репликой ресторана.
     */
    private fun pickup(
        orderEt: String, orderRu: String,
        confirmEt: String, confirmRu: String,
        teach: List<String>
    ): List<Turn> = listOf(
        ask(
            "Зайдите в ресторан, поздоровайтесь и спросите, готов ли заказ:", true,
            listOf(
                Choice("Tere! Kas tellimus on valmis?", "Здравствуйте! Заказ готов?", true),
                Choice("Head aega, nägemist!", "До свидания, увидимся!", false),
                Choice("Ei, aitäh, ma ei taha.", "Нет, спасибо, я не хочу.", false),
            ),
            listOf("g_tere", "p_valmis_q")
        ),
        say(Speaker.RESTORAN,
            "Tere! Teil on tellimus: $orderEt, eks?",
            "Здравствуйте! У вас заказ: $orderRu, верно?"),
        ask(
            "Сверьте заказ и подтвердите, что всё верно:", false,
            listOf(
                Choice(confirmEt, confirmRu, true),
                Choice("Ei, see on vale tellimus.", "Нет, это неверный заказ.", false),
                Choice("Üks õlu, palun.", "Одно пиво, пожалуйста.", false),
            ),
            teach
        ),
        say(Speaker.RESTORAN,
            "Tubli! Peaaegu valmis, oota üks minut… Nüüd valmis. Palun, head teed!",
            "Отлично! Почти готово, подожди минутку… Готово. Пожалуйста, счастливого пути!"),
    )

    /** Курьер пишет клиенту, что забрал заказ и уже едет (мостик после ресторана). */
    private fun enRoute(): Turn = ask(
        "Напишите клиенту, что забрали заказ и уже едете:", true,
        listOf(
            Choice("Tere! Võtsin tellimuse, olen kohe kohal.", "Здравствуйте! Забрал заказ, скоро буду.", true),
            Choice("Ma ei leidnud restorani.", "Я не нашёл ресторан.", false),
            Choice("Söön teie toidu ära.", "Съем вашу еду.", false),
        ),
        listOf("p_kohal", "g_tere")
    )

    /** Передача заказа клиенту лицом к лицу: [Ask] курьер отдаёт → [Say] клиент благодарит. */
    private fun handover(): List<Turn> = listOf(
        ask(
            "Вы встретились с клиентом. Передайте заказ и пожелайте приятного аппетита:", false,
            listOf(
                Choice("Palun, siin on teie tellimus. Head isu!", "Пожалуйста, вот ваш заказ. Приятного аппетита!", true),
                Choice("Vabandust, ma sõin selle ära.", "Извините, я это съел.", false),
                Choice("Ei, see on minu tellimus.", "Нет, это мой заказ.", false),
            ),
            listOf("g_palun", "p_head_isu")
        ),
        say(Speaker.KLIENT,
            "Suur aitäh! Ilusat päeva, nägemist!",
            "Большое спасибо! Хорошего дня, до свидания!"),
    )

    val orders: List<Order> = listOf(
        // ---- 1. Пицца, навигация, встреча у дома ----
        Order(
            id = "o1", restaurant = "Pizza Grande", customer = "Maarja",
            address = "Pärnu maantee 12, korter 5", distanceKm = 2.3, payout = 4.20,
            itemsEt = "Üks pepperoni pitsa ja kola",
            itemsRu = "Одна пицца пепперони и кола",
            turns = buildList {
                addAll(pickup(
                    "üks pepperoni pitsa ja kola", "одна пицца пепперони и кола",
                    "Jah, üks pitsa ja kola. Aitäh!", "Да, одна пицца и кола. Спасибо!",
                    listOf("f_pitsa", "g_jah", "g_aitah")
                ))
                add(ask("🧭 Навигатор: на перекрёстке поверни направо. Куда повернуть?", false, listOf(
                    Choice("Paremale.", "Направо.", true),
                    Choice("Vasakule.", "Налево.", false),
                    Choice("Tagasi.", "Назад.", false),
                ), listOf("s_paremale", "s_ristmik")))
                add(say(Speaker.KLIENT, "Halloo! Kus mu toit on?", "Алло! Где моя еда?"))
                add(ask("Ответьте клиенту, что вы на месте:", false, listOf(
                    Choice("Tere, olen kohal, maja ees.", "Здравствуйте, я на месте, перед домом.", true),
                    Choice("Vabandust, ma eksisin ära.", "Извините, я заблудился.", false),
                    Choice("Ei, mul ei ole midagi.", "Нет, у меня ничего нет.", false),
                ), listOf("p_kohal", "s_maja")))
                add(say(Speaker.KLIENT, "Tulen kohe alla!", "Сейчас спущусь!"))
                addAll(handover())
            }
        ),
        // ---- 2. Суши, лифт сломан ----
        Order(
            id = "o2", restaurant = "Sushi Aasia", customer = "Andres",
            address = "Narva maantee 5, 3. korrus", distanceKm = 3.1, payout = 5.10,
            itemsEt = "Kaks sushikomplekti ja roheline tee",
            itemsRu = "Два суши-сета и зелёный чай",
            turns = buildList {
                addAll(pickup(
                    "kaks sushikomplekti ja roheline tee", "два суши-сета и зелёный чай",
                    "Jah, kaks komplekti ja tee. Aitäh!", "Да, два сета и чай. Спасибо!",
                    listOf("n_2", "d_tee", "g_jah")
                ))
                add(enRoute())
                add(say(Speaker.KLIENT,
                    "Tere! Ma olen kolmandal korrusel. Lift on katki.",
                    "Здравствуйте! Я на третьем этаже. Лифт сломан."))
                add(ask("Лифт сломан. Спросите, спустится клиент или вам подняться:", true, listOf(
                    Choice("Kas tulete alla või tulen üles?", "Вы спуститесь или мне подняться?", true),
                    Choice("Kas pitsa on kuum?", "Пицца горячая?", false),
                    Choice("Pööra vasakule.", "Поверни налево.", false),
                ), listOf("p_alla_voi_q", "s_korrus", "s_lift")))
                add(say(Speaker.KLIENT, "Oi, ma tulen ise alla. Üks minut!", "Ой, я сам спущусь. Одну минуту!"))
                addAll(handover())
            }
        ),
        // ---- 3. Бургеры, клиент дома ----
        Order(
            id = "o3", restaurant = "Burger Maja", customer = "Kati",
            address = "Tartu maantee 40, korter 9", distanceKm = 1.8, payout = 3.90,
            itemsEt = "Kolm kanaburgerit ja friikartulid",
            itemsRu = "Три куриных бургера и картошка фри",
            turns = buildList {
                addAll(pickup(
                    "kolm kanaburgerit ja friikartulid", "три куриных бургера и картошка фри",
                    "Jah, kolm burgerit ja friikartulid.", "Да, три бургера и картошка фри.",
                    listOf("f_burger", "f_kana", "f_friikad", "n_3")
                ))
                add(enRoute())
                add(say(Speaker.KLIENT, "Tere! Ma ootan kodus, neljandal korrusel.", "Здравствуйте! Я жду дома, на четвёртом этаже."))
                add(ask("Уточните номер квартиры:", true, listOf(
                    Choice("Mis on teie korterinumber?", "Какой у вас номер квартиры?", true),
                    Choice("Mis su lemmikvärv on?", "Какой твой любимый цвет?", false),
                    Choice("Kas sajab vihma?", "Идёт дождь?", false),
                ), listOf("p_korter_q", "s_korrus", "n_4")))
                add(say(Speaker.KLIENT, "Korter number üheksa.", "Квартира номер девять."))
                addAll(handover())
            }
        ),
        // ---- 4. Утро, кофе, встреча у парка ----
        Order(
            id = "o4", restaurant = "Kohvik Sõbralik", customer = "Pjotr",
            address = "Liivalaia 8, korter 4", distanceKm = 2.7, payout = 4.60,
            itemsEt = "Kaks kohvi piimaga ja üks sai juustuga",
            itemsRu = "Два кофе с молоком и одна булка с сыром",
            turns = buildList {
                add(ask("Утро. Зайдите, поздоровайтесь по-утреннему и спросите про заказ:", true, listOf(
                    Choice("Tere hommikust! Kas tellimus on valmis?", "Доброе утро! Заказ готов?", true),
                    Choice("Tere õhtust! Head aega.", "Добрый вечер! До свидания.", false),
                    Choice("Head ööd!", "Спокойной ночи!", false),
                ), listOf("g_hommik", "p_valmis_q")))
                add(say(Speaker.RESTORAN,
                    "Tere hommikust! Teil on tellimus: kaks kohvi piimaga ja sai juustuga, eks?",
                    "Доброе утро! У вас заказ: два кофе с молоком и булка с сыром, верно?"))
                add(ask("Подтвердите заказ:", false, listOf(
                    Choice("Jah, kaks kohvi ja sai. Aitäh!", "Да, два кофе и булка. Спасибо!", true),
                    Choice("Ei, see on vale.", "Нет, это неверно.", false),
                    Choice("Üks õlu, palun.", "Одно пиво, пожалуйста.", false),
                ), listOf("d_kohv", "d_piim", "f_sai", "f_juust", "n_2")))
                add(say(Speaker.RESTORAN, "Suurepärane! Üks minut ja valmis. Palun, head teed!", "Прекрасно! Минута — и готово. Пожалуйста, счастливого пути!"))
                add(ask("🧭 Навигатор: езжай прямо, дом слева. Где дом?", false, listOf(
                    Choice("Vasakul.", "Слева.", true),
                    Choice("Paremal.", "Справа.", false),
                    Choice("Tagasi.", "Сзади.", false),
                ), listOf("s_otse", "s_vasakule", "s_maja")))
                add(say(Speaker.KLIENT, "Tere! Ma olen õues, pargi juures.", "Здравствуйте! Я на улице, у парка."))
                add(ask("Спросите, где именно найти клиента:", true, listOf(
                    Choice("Kus ma teid leian?", "Где мне вас найти?", true),
                    Choice("Kas teile meeldib kohv?", "Вам нравится кофе?", false),
                    Choice("Kas lift töötab?", "Лифт работает?", false),
                ), listOf("p_kus_q")))
                add(say(Speaker.KLIENT, "Sinise mütsiga, näen teid juba!", "В синей шапке, уже вас вижу!"))
                addAll(handover())
            }
        ),
        // ---- 5. Суп, домофон с кодом ----
        Order(
            id = "o5", restaurant = "Supiköök", customer = "Olga",
            address = "Sõpruse puiestee 21, 7. korrus", distanceKm = 4.0, payout = 5.80,
            itemsEt = "Kanasupp, must leib ja klaas vett",
            itemsRu = "Куриный суп, чёрный хлеб и стакан воды",
            turns = buildList {
                addAll(pickup(
                    "kanasupp, must leib ja klaas vett", "куриный суп, чёрный хлеб и стакан воды",
                    "Jah, supp, leib ja vesi.", "Да, суп, хлеб и вода.",
                    listOf("f_supp", "f_leib", "d_vesi")
                ))
                add(enRoute())
                add(say(Speaker.KLIENT, "Tere! Olen seitsmendal korrusel.", "Здравствуйте! Я на седьмом этаже."))
                add(ask("Уточните номер квартиры:", true, listOf(
                    Choice("Mis on teie korterinumber?", "Какой у вас номер квартиры?", true),
                    Choice("Kas teil on kass?", "У вас есть кошка?", false),
                    Choice("Kui vana te olete?", "Сколько вам лет?", false),
                ), listOf("p_korter_q", "n_7", "s_korrus")))
                add(say(Speaker.KLIENT, "Korter number üheksa. Värav on lukus.", "Квартира номер девять. Ворота заперты."))
                add(ask("Ворота заперты — спросите код двери:", true, listOf(
                    Choice("Mis on ukse kood?", "Какой код двери?", true),
                    Choice("Kas supp on kuum?", "Суп горячий?", false),
                    Choice("Head ööd!", "Спокойной ночи!", false),
                ), listOf("p_kood_q", "n_9", "s_korter")))
                add(say(Speaker.KLIENT, "Kood on üks-kaks-kolm-neli.", "Код — один-два-три-четыре."))
                addAll(handover())
            }
        ),
        // ---- 6. Рыба, оставить под дверью + код ----
        Order(
            id = "o6", restaurant = "Kala & Salat", customer = "Mikk",
            address = "Vana-Posti 7, korter 2", distanceKm = 1.5, payout = 3.50,
            itemsEt = "Kala, kreeka salat ja õunamahl",
            itemsRu = "Рыба, греческий салат и яблочный сок",
            turns = buildList {
                addAll(pickup(
                    "kala, kreeka salat ja õunamahl", "рыба, греческий салат и яблочный сок",
                    "Jah, kala, salat ja mahl.", "Да, рыба, салат и сок.",
                    listOf("f_kala", "f_salat", "d_mahl")
                ))
                add(enRoute())
                add(say(Speaker.KLIENT,
                    "Tere! Jätke palun toit ukse taha, korter kaks. Värava kood on viis-kuus-seitse-kaheksa.",
                    "Здравствуйте! Оставьте, пожалуйста, еду под дверью, квартира два. Код ворот пять-шесть-семь-восемь."))
                add(ask("Подтвердите: оставите под дверью и сделаете фото:", false, listOf(
                    Choice("Selge! Jätan ukse taha ja teen foto.", "Понятно! Оставлю под дверью и сделаю фото.", true),
                    Choice("Ei, ma võtan toidu endale.", "Нет, я заберу еду себе.", false),
                    Choice("Pööra paremale.", "Поверни направо.", false),
                ), listOf("p_ukse_taha", "p_foto", "n_5", "n_6", "n_7", "n_8")))
                add(say(Speaker.KLIENT, "Suur aitäh! Olete väga kiire.", "Большое спасибо! Вы очень быстрый."))
            }
        ),
        // ---- 7. Оплата наличными ----
        Order(
            id = "o7", restaurant = "Pitsabaar Napoli", customer = "Tiina",
            address = "Roosikrantsi 9, korter 3", distanceKm = 2.0, payout = 4.40,
            itemsEt = "Üks suur pitsa (sularahamakse)",
            itemsRu = "Одна большая пицца (оплата наличными)",
            turns = buildList {
                addAll(pickup(
                    "üks suur pitsa", "одна большая пицца",
                    "Jah, üks suur pitsa.", "Да, одна большая пицца.",
                    listOf("f_pitsa", "n_1")
                ))
                add(ask("🏪 Ресторан: клиент платит наличными, 10 €. Сколько взять с клиента?", false, listOf(
                    Choice("Kümme eurot.", "Десять евро.", true),
                    Choice("Kaks eurot.", "Два евро.", false),
                    Choice("Sada eurot.", "Сто евро.", false),
                ), listOf("m_maksab", "m_eurot", "n_10")))
                add(say(Speaker.KLIENT, "Tere! Te tõite pitsa?", "Здравствуйте! Вы привезли пиццу?"))
                add(ask("Уточните способ оплаты:", true, listOf(
                    Choice("Kas maksate sularahas või kaardiga?", "Платите наличными или картой?", true),
                    Choice("Kas teil on koer?", "У вас есть собака?", false),
                    Choice("Mis kell on?", "Который час?", false),
                ), listOf("p_maksate_q", "m_sularaha", "m_kaart")))
                add(say(Speaker.KLIENT, "Sularahas. Siin on kümme eurot.", "Наличными. Вот десять евро."))
                add(ask("Деньги ровные. Передайте чек, заказ и пожелайте приятного аппетита:", false, listOf(
                    Choice("Aitäh! Siin on kviitung. Head isu!", "Спасибо! Вот чек. Приятного аппетита!", true),
                    Choice("Vabandust, mul ei ole pitsat.", "Извините, у меня нет пиццы.", false),
                    Choice("Ei, see on liiga vähe.", "Нет, этого мало.", false),
                ), listOf("m_kviitung", "p_head_isu")))
                add(say(Speaker.KLIENT, "Suur aitäh! Nägemist.", "Большое спасибо! До свидания."))
            }
        ),
        // ---- 8. Проблема с адресом ----
        Order(
            id = "o8", restaurant = "Indica Curry", customer = "Rein",
            address = "Tehnika 18 (aadress segane)", distanceKm = 3.4, payout = 5.40,
            itemsEt = "Kaks karrit ja nan-leib",
            itemsRu = "Два карри и лепёшка нан",
            turns = buildList {
                addAll(pickup(
                    "kaks karrit ja nan-leib", "два карри и лепёшка нан",
                    "Jah, kaks karrit ja nan.", "Да, два карри и нан.",
                    listOf("n_2", "f_leib")
                ))
                add(ask("🧭 Навигатор привёл не туда — такого номера дома нет. Что случилось?", false, listOf(
                    Choice("Aadress on vale.", "Адрес неверный.", true),
                    Choice("Toit on valmis.", "Еда готова.", false),
                    Choice("Lift on katki.", "Лифт сломан.", false),
                ), listOf("p_aadress_q")))
                add(say(Speaker.KLIENT, "Halloo?", "Алло?"))
                add(ask("Позвоните клиенту и уточните, верный ли адрес:", true, listOf(
                    Choice("Tere! Kas see aadress on õige?", "Здравствуйте! Этот адрес верный?", true),
                    Choice("Kas teile maitseb karri?", "Вам нравится карри?", false),
                    Choice("Head ööd!", "Спокойной ночи!", false),
                ), listOf("p_aadress_q")))
                add(say(Speaker.KLIENT,
                    "Oi, ei! Õige maja on kollane, teisel pool tänavat.",
                    "Ой, нет! Нужный дом жёлтый, на другой стороне улицы."))
                add(ask("Уточните, где именно дом:", true, listOf(
                    Choice("Kus maja täpselt asub?", "Где именно находится дом?", true),
                    Choice("Mis su lemmiktoit on?", "Какая твоя любимая еда?", false),
                    Choice("Kas sajab lund?", "Идёт снег?", false),
                ), listOf("p_kus_maja_q", "s_maja")))
                add(say(Speaker.KLIENT,
                    "Roheline uks, teine korrus, korter neli.",
                    "Зелёная дверь, второй этаж, квартира четыре."))
                addAll(handover())
            }
        ),
        // ---- 9. Клиента нет дома ----
        Order(
            id = "o9", restaurant = "Sushi Tokyo", customer = "Jaan",
            address = "Mustamäe tee 16, korter 21", distanceKm = 2.9, payout = 4.90,
            itemsEt = "Üks sushikomplekt ja miso supp",
            itemsRu = "Один суши-сет и суп мисо",
            turns = buildList {
                addAll(pickup(
                    "üks sushikomplekt ja miso supp", "один суши-сет и суп мисо",
                    "Jah, komplekt ja supp.", "Да, сет и суп.",
                    listOf("f_supp", "n_1")
                ))
                add(ask("Вы на месте, но клиент не отвечает на телефон. Что сделать?", false, listOf(
                    Choice("Ma helistan teile veel kord.", "Я позвоню вам ещё раз.", true),
                    Choice("Söön toidu ise ära.", "Съем еду сам.", false),
                    Choice("Sõidan kohe koju.", "Сразу поеду домой.", false),
                ), listOf("p_helistan")))
                add(say(Speaker.KLIENT, "Vabandust, ma ei kuulnud telefoni!", "Извините, я не слышал телефон!"))
                add(ask("Спросите, оставить ли заказ под дверью:", true, listOf(
                    Choice("Kas jätan toidu ukse taha?", "Оставить еду под дверью?", true),
                    Choice("Kas maksate kaardiga?", "Платите картой?", false),
                    Choice("Kus on lift?", "Где лифт?", false),
                ), listOf("p_ukse_taha", "s_uks")))
                add(say(Speaker.KLIENT, "Jah, jätke ukse taha, palun.", "Да, оставьте под дверью, пожалуйста."))
                add(ask("Подтвердите: оставите под дверью и сделаете фото:", false, listOf(
                    Choice("Selge, jätan ukse taha ja teen foto.", "Понятно, оставлю под дверью и сделаю фото.", true),
                    Choice("Ei, ma võtan toidu endale.", "Нет, я заберу еду себе.", false),
                    Choice("Pööra paremale.", "Поверни направо.", false),
                ), listOf("p_foto", "p_ukse_taha")))
                add(say(Speaker.KLIENT, "Suur aitäh! Vabandust veel kord.", "Большое спасибо! Ещё раз извините."))
            }
        ),
    )
}
