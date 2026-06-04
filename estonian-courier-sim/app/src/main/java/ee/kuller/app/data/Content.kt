package ee.kuller.app.data

import ee.kuller.app.model.Category
import ee.kuller.app.model.Choice
import ee.kuller.app.model.DialogueStep
import ee.kuller.app.model.Order
import ee.kuller.app.model.Speaker
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
                WordEntry("g_palun", "Palun", "Пожалуйста", "Palun, võtke heaks."),
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
                WordEntry("f_supp", "Supp", "Суп", "Kuum seljanka supp."),
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
                WordEntry("n_9", "Üheksa", "Девять", "Üheksas korrus."),
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
                WordEntry("p_kohal", "Olen kohal", "Я на месте", "Tere, olen kohal."),
                WordEntry("p_alla", "Kas saate alla tulla?", "Можете спуститься?", "Kas saate alla tulla?"),
                WordEntry("p_korrus", "Millisel korrusel te olete?", "На каком вы этаже?", "Millisel korrusel te olete?"),
                WordEntry("p_ukse_taha", "Jätan ukse taha", "Оставлю под дверью", "Jätan toidu ukse taha."),
                WordEntry("p_head_isu", "Head isu!", "Приятного аппетита!", "Palun, head isu!"),
                WordEntry("p_valmis", "Tellimus on valmis", "Заказ готов", "Teie tellimus on valmis."),
                WordEntry("p_jargi", "Tulin tellimusele järele", "Я за заказом", "Tere, tulin tellimusele järele."),
            )
        ),
    )

    val allWords: List<WordEntry> = categories.flatMap { it.words }
    fun word(id: String): WordEntry? = allWords.firstOrNull { it.id == id }

    // ----------------------------------------------------------------------
    // ЗАКАЗЫ (мини-уроки в виде доставок)
    // ----------------------------------------------------------------------

    private fun greetStep() = DialogueStep(
        speaker = Speaker.RESTORAN,
        npcEt = "Tere! Kas tulite tellimusele järele?",
        npcRu = "Здравствуйте! Вы пришли за заказом?",
        questionRu = "Поздоровайтесь и подтвердите, что вы за заказом:",
        choices = listOf(
            Choice("Tere! Jah, tulin tellimusele järele.", "Здравствуйте! Да, я за заказом.", true),
            Choice("Head aega, nägemist!", "До свидания, увидимся!", false),
            Choice("Ei, aitäh, ma ei taha.", "Нет, спасибо, я не хочу.", false),
        ),
        teachWordIds = listOf("g_tere", "g_jah", "p_jargi")
    )

    val orders: List<Order> = listOf(
        Order(
            id = "o1", restaurant = "Pizza Grande", customer = "Maarja",
            address = "Pärnu maantee 12, korter 5", distanceKm = 2.3, payout = 4.20,
            itemsEt = "Üks pepperoni pitsa ja Coca-Cola",
            itemsRu = "Одна пицца пепперони и кока-кола",
            steps = listOf(
                greetStep(),
                DialogueStep(
                    speaker = Speaker.NARRATOR,
                    npcEt = "Navigaator: ristmikul pööra paremale, siis sõida otse.",
                    npcRu = "Навигатор: на перекрёстке поверни направо, потом прямо.",
                    questionRu = "Куда нужно повернуть на перекрёстке?",
                    choices = listOf(
                        Choice("Paremale", "Направо", true),
                        Choice("Vasakule", "Налево", false),
                        Choice("Tagasi", "Назад", false),
                    ),
                    teachWordIds = listOf("s_paremale", "s_otse", "s_ristmik")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Halloo! Kus mu toit on?",
                    npcRu = "Алло! Где моя еда?",
                    questionRu = "Вы у двери. Сообщите клиенту, что вы на месте:",
                    choices = listOf(
                        Choice("Tere, olen kohal. Teil on tellimus.", "Здравствуйте, я на месте. У вас заказ.", true),
                        Choice("Vabandust, ma eksisin ära.", "Извините, я заблудился.", false),
                        Choice("Ei, mul ei ole midagi.", "Нет, у меня ничего нет.", false),
                    ),
                    teachWordIds = listOf("p_kohal", "p_tellimus")
                ),
            )
        ),
        Order(
            id = "o2", restaurant = "Sushi Aasia", customer = "Andres",
            address = "Narva maantee 5, 3. korrus", distanceKm = 3.1, payout = 5.10,
            itemsEt = "Kaks sushi komplekti ja roheline tee",
            itemsRu = "Два суши-сета и зелёный чай",
            steps = listOf(
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Tere! Teie tellimus on valmis. Kontrollige: kaks komplekti?",
                    npcRu = "Здравствуйте! Ваш заказ готов. Проверьте: два сета?",
                    questionRu = "Сколько комплектов в заказе? Подтвердите по-эстонски:",
                    choices = listOf(
                        Choice("Jah, kaks komplekti. Aitäh!", "Да, два сета. Спасибо!", true),
                        Choice("Ei, kümme komplekti.", "Нет, десять сетов.", false),
                        Choice("Üks õlu, palun.", "Одно пиво, пожалуйста.", false),
                    ),
                    teachWordIds = listOf("n_2", "g_aitah", "p_valmis")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Tere, ma olen kolmandal korrusel. Lift on katki.",
                    npcRu = "Здравствуйте, я на третьем этаже. Лифт сломан.",
                    questionRu = "Клиент на 3-м этаже. Что значит «kolmandal korrusel»?",
                    choices = listOf(
                        Choice("Kolmas korrus", "Третий этаж", true),
                        Choice("Kümnes maja", "Десятый дом", false),
                        Choice("Esimene uks", "Первая дверь", false),
                    ),
                    teachWordIds = listOf("n_3", "s_korrus", "s_lift")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Suur tänu! Kas saate toidu ukse taha jätta?",
                    npcRu = "Большое спасибо! Можете оставить еду под дверью?",
                    questionRu = "Согласитесь и пожелайте приятного аппетита:",
                    choices = listOf(
                        Choice("Jah, jätan ukse taha. Head isu!", "Да, оставлю под дверью. Приятного аппетита!", true),
                        Choice("Ei, ma võtan toidu tagasi.", "Нет, я заберу еду обратно.", false),
                        Choice("Vabandust, kus on lift?", "Извините, где лифт?", false),
                    ),
                    teachWordIds = listOf("p_ukse_taha", "p_head_isu")
                ),
            )
        ),
        Order(
            id = "o3", restaurant = "Burger Maja", customer = "Kati",
            address = "Tartu maantee 40, korter 9", distanceKm = 1.8, payout = 3.90,
            itemsEt = "Kolm kanaburgerit ja suured friikartulid",
            itemsRu = "Три куриных бургера и большая картошка фри",
            steps = listOf(
                greetStep(),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Kolm kanaburgerit ja friikartulid. Kõik õige?",
                    npcRu = "Три куриных бургера и картошка фри. Всё верно?",
                    questionRu = "Что внутри заказа? Выберите правильный перевод:",
                    choices = listOf(
                        Choice("Kana ja friikartulid", "Курица и картофель фри", true),
                        Choice("Kala ja juust", "Рыба и сыр", false),
                        Choice("Supp ja leib", "Суп и хлеб", false),
                    ),
                    teachWordIds = listOf("f_burger", "f_kana", "f_friikad", "n_3")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Tere! Millisel korrusel te olete? Ma ootan all.",
                    npcRu = "Здравствуйте! На каком вы этаже? Я жду внизу.",
                    questionRu = "Клиент ждёт внизу. Спросите, может ли он спуститься:",
                    choices = listOf(
                        Choice("Kas saate alla tulla? Olen maja ees.", "Можете спуститься? Я перед домом.", true),
                        Choice("Pööra vasakule ristmikul.", "Поверни налево на перекрёстке.", false),
                        Choice("Üks kohv piimaga, palun.", "Один кофе с молоком, пожалуйста.", false),
                    ),
                    teachWordIds = listOf("p_alla", "s_maja")
                ),
            )
        ),
        Order(
            id = "o4", restaurant = "Kohvik Sõbralik", customer = "Pjotr",
            address = "Liivalaia 8, korter 4", distanceKm = 2.7, payout = 4.60,
            itemsEt = "Kaks kohvi piimaga ja üks sai juustuga",
            itemsRu = "Два кофе с молоком и одна булка с сыром",
            steps = listOf(
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Tere hommikust! Kaks kohvi piimaga, eks?",
                    npcRu = "Доброе утро! Два кофе с молоком, верно?",
                    questionRu = "Утро. Поздоровайтесь и подтвердите напитки:",
                    choices = listOf(
                        Choice("Tere hommikust! Jah, kaks kohvi piimaga.", "Доброе утро! Да, два кофе с молоком.", true),
                        Choice("Tere õhtust! Üks õlu.", "Добрый вечер! Одно пиво.", false),
                        Choice("Head aega! Ei kohvi.", "До свидания! Без кофе.", false),
                    ),
                    teachWordIds = listOf("g_hommik", "d_kohv", "d_piim", "n_2")
                ),
                DialogueStep(
                    speaker = Speaker.NARRATOR,
                    npcEt = "Navigaator: sõida otse, maja on vasakul.",
                    npcRu = "Навигатор: езжай прямо, дом слева.",
                    questionRu = "Где находится дом?",
                    choices = listOf(
                        Choice("Vasakul", "Слева", true),
                        Choice("Paremal", "Справа", false),
                        Choice("Tagasi", "Сзади", false),
                    ),
                    teachWordIds = listOf("s_otse", "s_vasakule", "s_maja")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Aitäh! Ma võtan ise alt vastu.",
                    npcRu = "Спасибо! Я сам встречу внизу.",
                    questionRu = "Отдайте заказ и попрощайтесь вежливо:",
                    choices = listOf(
                        Choice("Palun, siin on teie tellimus. Head isu, nägemist!", "Пожалуйста, вот ваш заказ. Приятного аппетита, до встречи!", true),
                        Choice("Vabandust, toit on otsas.", "Извините, еда закончилась.", false),
                        Choice("Ei, see ei ole teie tellimus.", "Нет, это не ваш заказ.", false),
                    ),
                    teachWordIds = listOf("g_palun", "p_head_isu", "g_nagemist")
                ),
            )
        ),
        Order(
            id = "o5", restaurant = "Supiköök", customer = "Olga",
            address = "Sõpruse puiestee 21, 7. korrus", distanceKm = 4.0, payout = 5.80,
            itemsEt = "Üks kana supp, must leib ja klaas vett",
            itemsRu = "Один куриный суп, чёрный хлеб и стакан воды",
            steps = listOf(
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Tellimus on valmis: supp, leib ja vesi. Palun, võtke kaasa.",
                    npcRu = "Заказ готов: суп, хлеб и вода. Пожалуйста, забирайте.",
                    questionRu = "Поблагодарите за готовый заказ:",
                    choices = listOf(
                        Choice("Aitäh! Ilusat päeva.", "Спасибо! Хорошего дня.", true),
                        Choice("Ei, ma ei taha supi.", "Нет, я не хочу суп.", false),
                        Choice("Vabandust, kus on uks?", "Извините, где дверь?", false),
                    ),
                    teachWordIds = listOf("f_supp", "f_leib", "d_vesi", "p_valmis")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Tere! Olen seitsmendal korrusel, korter number üheksa.",
                    npcRu = "Здравствуйте! Я на седьмом этаже, квартира номер девять.",
                    questionRu = "Какой номер квартиры назвал клиент?",
                    choices = listOf(
                        Choice("Üheksa", "Девять", true),
                        Choice("Seitse", "Семь", false),
                        Choice("Üks", "Один", false),
                    ),
                    teachWordIds = listOf("n_7", "n_9", "s_korter")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Suur aitäh, et nii kiiresti! Head isu mulle, ahah.",
                    npcRu = "Большое спасибо, что так быстро! Приятного аппетита мне, ха-ха.",
                    questionRu = "Тепло попрощайтесь с клиентом:",
                    choices = listOf(
                        Choice("Palun! Head isu ja head aega!", "Пожалуйста! Приятного аппетита и до свидания!", true),
                        Choice("Vabandust, ma hilinen.", "Извините, я опаздываю.", false),
                        Choice("Ei, aitäh, nägemist.", "Нет, спасибо, пока.", false),
                    ),
                    teachWordIds = listOf("g_palun", "p_head_isu", "g_head_aega")
                ),
            )
        ),
        Order(
            id = "o6", restaurant = "Kala & Salat", customer = "Mikk",
            address = "Vana-Posti 7, korter 2", distanceKm = 1.5, payout = 3.50,
            itemsEt = "Üks kala, kreeka salat ja õunamahl",
            itemsRu = "Одна рыба, греческий салат и яблочный сок",
            steps = listOf(
                greetStep(),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Kala, salat ja mahl. Kontrollige kotti, palun.",
                    npcRu = "Рыба, салат и сок. Проверьте сумку, пожалуйста.",
                    questionRu = "Что в заказе? Выберите верный перевод:",
                    choices = listOf(
                        Choice("Kala, salat ja mahl", "Рыба, салат и сок", true),
                        Choice("Kana, supp ja kohv", "Курица, суп и кофе", false),
                        Choice("Pitsa, leib ja tee", "Пицца, хлеб и чай", false),
                    ),
                    teachWordIds = listOf("f_kala", "f_salat", "d_mahl")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Tere! Jätke palun toit ukse taha, korter kaks.",
                    npcRu = "Здравствуйте! Оставьте, пожалуйста, еду под дверью, квартира два.",
                    questionRu = "Подтвердите, что оставите заказ под дверью:",
                    choices = listOf(
                        Choice("Selge, jätan ukse taha. Head isu!", "Понятно, оставлю под дверью. Приятного аппетита!", true),
                        Choice("Ei, ma tulen sisse.", "Нет, я зайду внутрь.", false),
                        Choice("Pööra paremale.", "Поверни направо.", false),
                    ),
                    teachWordIds = listOf("p_ukse_taha", "n_2", "s_uks")
                ),
            )
        ),
    )
}
