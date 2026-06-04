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
                // Вопросы, которые задаёт сам курьер
                WordEntry("p_valmis_q", "Kas tellimus on valmis?", "Заказ готов?", "Tere, kas tellimus on valmis?"),
                WordEntry("p_nimi_q", "Mis nimi on tellimusel?", "На какое имя заказ?", "Mis nimi on tellimusel?"),
                WordEntry("p_oige_q", "Kas see on õige tellimus?", "Это верный заказ?", "Kas see on õige tellimus?"),
                WordEntry("p_veel_q", "Kas siia tuleb veel midagi?", "Сюда ещё что-то будет?", "Kas siia tuleb veel midagi?"),
                WordEntry("p_korter_q", "Mis on teie korterinumber?", "Какой у вас номер квартиры?", "Mis on teie korterinumber?"),
                WordEntry("p_kus_q", "Kus ma teid leian?", "Где мне вас найти?", "Kus ma teid leian?"),
                WordEntry("p_kood_q", "Mis on ukse kood?", "Какой код двери?", "Mis on ukse kood?"),
                WordEntry("p_alla_voi_q", "Kas tulete alla või tulen üles?", "Спуститесь или мне подняться?", "Kas tulete alla või tulen üles?"),
            )
        ),
    )

    val allWords: List<WordEntry> = categories.flatMap { it.words }
    fun word(id: String): WordEntry? = allWords.firstOrNull { it.id == id }

    // ----------------------------------------------------------------------
    // ЗАКАЗЫ (мини-уроки в виде доставок)
    //
    // Диалоги двусторонние: курьер не только отвечает, но и сам
    // задаёт вопросы в ресторане и клиенту, а персонажи отвечают.
    // ----------------------------------------------------------------------

    /** Курьер сам заходит в ресторан и спрашивает, готов ли заказ. */
    private fun askReadyStep(replyEt: String, replyRu: String) = DialogueStep(
        speaker = Speaker.RESTORAN,
        npcEt = "", npcRu = "",
        questionRu = "Вы зашли в ресторан. Поздоровайтесь и спросите, готов ли заказ:",
        courierAsks = true,
        choices = listOf(
            Choice("Tere! Kas tellimus on valmis?", "Здравствуйте! Заказ готов?", true),
            Choice("Head aega, nägemist!", "До свидания, увидимся!", false),
            Choice("Ei, aitäh, ma ei taha.", "Нет, спасибо, я не хочу.", false),
        ),
        npcReplyEt = replyEt, npcReplyRu = replyRu,
        teachWordIds = listOf("g_tere", "p_valmis_q")
    )

    val orders: List<Order> = listOf(
        Order(
            id = "o1", restaurant = "Pizza Grande", customer = "Maarja",
            address = "Pärnu maantee 12, korter 5", distanceKm = 2.3, payout = 4.20,
            itemsEt = "Üks pepperoni pitsa ja Coca-Cola",
            itemsRu = "Одна пицца пепперони и кока-кола",
            steps = listOf(
                askReadyStep("Jah, kohe valmis! Üks hetk.", "Да, сейчас будет готов! Минутку."),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Palun, siin on kott.",
                    npcRu = "Пожалуйста, вот пакет.",
                    questionRu = "Уточните у сотрудника, на какое имя заказ:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Mis nimi on tellimusel?", "На какое имя заказ?", true),
                        Choice("Kus on tualett?", "Где туалет?", false),
                        Choice("Palju kell on?", "Который час?", false),
                    ),
                    npcReplyEt = "Tellimus on Maarjale: pitsa ja kola.",
                    npcReplyRu = "Заказ для Маарьи: пицца и кола.",
                    teachWordIds = listOf("p_nimi_q", "f_pitsa")
                ),
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
                    questionRu = "Вы у дома. Сообщите клиенту, что вы на месте:",
                    choices = listOf(
                        Choice("Tere, olen kohal. Teil on tellimus.", "Здравствуйте, я на месте. У вас заказ.", true),
                        Choice("Vabandust, ma eksisin ära.", "Извините, я заблудился.", false),
                        Choice("Ei, mul ei ole midagi.", "Нет, у меня ничего нет.", false),
                    ),
                    npcReplyEt = "Super, tulen kohe alla!",
                    npcReplyRu = "Супер, сейчас спущусь!",
                    teachWordIds = listOf("p_kohal", "p_tellimus")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "", npcRu = "",
                    questionRu = "Клиента пока не видно. Спросите, где вам его найти:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Kus ma teid leian?", "Где мне вас найти?", true),
                        Choice("Head isu!", "Приятного аппетита!", false),
                        Choice("Üks pitsa, palun.", "Одну пиццу, пожалуйста.", false),
                    ),
                    npcReplyEt = "Olen maja ees, punase jopega. Aitäh, head isu!",
                    npcReplyRu = "Я перед домом, в красной куртке. Спасибо, приятного аппетита!",
                    teachWordIds = listOf("p_kus_q", "s_maja", "p_head_isu")
                ),
            )
        ),
        Order(
            id = "o2", restaurant = "Sushi Aasia", customer = "Andres",
            address = "Narva maantee 5, 3. korrus", distanceKm = 3.1, payout = 5.10,
            itemsEt = "Kaks sushi komplekti ja roheline tee",
            itemsRu = "Два суши-сета и зелёный чай",
            steps = listOf(
                askReadyStep("Peaaegu! Kaks komplekti, eks?", "Почти! Два сета, верно?"),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Kaks komplekti ja roheline tee. Õige?",
                    npcRu = "Два сета и зелёный чай. Верно?",
                    questionRu = "Сколько комплектов в заказе? Подтвердите по-эстонски:",
                    choices = listOf(
                        Choice("Jah, kaks komplekti. Aitäh!", "Да, два сета. Спасибо!", true),
                        Choice("Ei, kümme komplekti.", "Нет, десять сетов.", false),
                        Choice("Üks õlu, palun.", "Одно пиво, пожалуйста.", false),
                    ),
                    npcReplyEt = "Tubli, kõik on kotis.",
                    npcReplyRu = "Отлично, всё в пакете.",
                    teachWordIds = listOf("n_2", "g_aitah", "p_valmis")
                ),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "", npcRu = "",
                    questionRu = "Перед уходом спросите, не нужно ли добавить что-то ещё к заказу:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Kas siia tuleb veel midagi?", "Сюда ещё что-то будет?", true),
                        Choice("Kas tohib magada?", "Можно поспать?", false),
                        Choice("Kus on parkla?", "Где парковка?", false),
                    ),
                    npcReplyEt = "Ei, see on kõik. Head teed!",
                    npcReplyRu = "Нет, это всё. Счастливого пути!",
                    teachWordIds = listOf("p_veel_q", "g_ei")
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
                    npcEt = "", npcRu = "",
                    questionRu = "Лифт не работает. Спросите, спустится ли клиент или вам подняться:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Kas tulete alla või tulen üles?", "Спуститесь или мне подняться?", true),
                        Choice("Kas pitsa on kuum?", "Пицца горячая?", false),
                        Choice("Pööra vasakule.", "Поверни налево.", false),
                    ),
                    npcReplyEt = "Oi, ma tulen ise alla. Üks minut! Aitäh, head isu!",
                    npcReplyRu = "Ой, я сам спущусь. Одну минуту! Спасибо, приятного аппетита!",
                    teachWordIds = listOf("p_alla_voi_q", "p_head_isu")
                ),
            )
        ),
        Order(
            id = "o3", restaurant = "Burger Maja", customer = "Kati",
            address = "Tartu maantee 40, korter 9", distanceKm = 1.8, payout = 3.90,
            itemsEt = "Kolm kanaburgerit ja suured friikartulid",
            itemsRu = "Три куриных бургера и большая картошка фри",
            steps = listOf(
                askReadyStep("Tere! Jah, valmis. Kontrolli üle, palun.", "Здравствуйте! Да, готов. Проверьте, пожалуйста."),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Kolm kanaburgerit ja friikartulid.",
                    npcRu = "Три куриных бургера и картошка фри.",
                    questionRu = "Проверьте сумку и спросите, верный ли это заказ:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Kas see on õige tellimus?", "Это верный заказ?", true),
                        Choice("Kas ma võin koju minna?", "Можно я пойду домой?", false),
                        Choice("Kus on bussipeatus?", "Где автобусная остановка?", false),
                    ),
                    npcReplyEt = "Jah, kõik õige: kana ja friikartulid.",
                    npcReplyRu = "Да, всё верно: курица и картофель фри.",
                    teachWordIds = listOf("p_oige_q", "f_burger", "f_kana", "f_friikad")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Tere! Ma ootan kodus.",
                    npcRu = "Здравствуйте! Я жду дома.",
                    questionRu = "Спросите у клиента, на каком он этаже:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Millisel korrusel te olete?", "На каком вы этаже?", true),
                        Choice("Mis su lemmikvärv on?", "Какой твой любимый цвет?", false),
                        Choice("Kas sajab vihma?", "Идёт дождь?", false),
                    ),
                    npcReplyEt = "Neljandal korrusel.",
                    npcReplyRu = "На четвёртом этаже.",
                    teachWordIds = listOf("p_korrus", "n_4", "s_korrus")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "", npcRu = "",
                    questionRu = "Теперь уточните номер квартиры:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Mis on teie korterinumber?", "Какой у вас номер квартиры?", true),
                        Choice("Kas teil on koer?", "У вас есть собака?", false),
                        Choice("Sõida otse.", "Езжай прямо.", false),
                    ),
                    npcReplyEt = "Korter number üheksa. Aitäh, head isu!",
                    npcReplyRu = "Квартира номер девять. Спасибо, приятного аппетита!",
                    teachWordIds = listOf("p_korter_q", "n_9", "s_korter")
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
                    npcEt = "Tere hommikust!",
                    npcRu = "Доброе утро!",
                    questionRu = "Утро. Поздоровайтесь по-утреннему и спросите про заказ:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Tere hommikust! Kas tellimus on valmis?", "Доброе утро! Заказ готов?", true),
                        Choice("Tere õhtust! Head aega.", "Добрый вечер! До свидания.", false),
                        Choice("Head ööd!", "Спокойной ночи!", false),
                    ),
                    npcReplyEt = "Jah! Kaks kohvi piimaga ja sai juustuga.",
                    npcReplyRu = "Да! Два кофе с молоком и булка с сыром.",
                    teachWordIds = listOf("g_hommik", "p_valmis_q", "d_kohv", "d_piim")
                ),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Siin on kaks topsi.",
                    npcRu = "Вот два стакана.",
                    questionRu = "Уточните, на какое имя заказ:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Mis nimi on tellimusel?", "На какое имя заказ?", true),
                        Choice("Kas kohv on magus?", "Кофе сладкий?", false),
                        Choice("Kas tohin istuda?", "Можно сесть?", false),
                    ),
                    npcReplyEt = "Pjotr. Ilusat päeva!",
                    npcReplyRu = "Пётр. Хорошего дня!",
                    teachWordIds = listOf("p_nimi_q", "n_2")
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
                    npcEt = "Tere! Ma olen õues.",
                    npcRu = "Здравствуйте! Я на улице.",
                    questionRu = "Клиент где-то снаружи. Спросите, где его найти:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Kus ma teid leian?", "Где мне вас найти?", true),
                        Choice("Kas teile meeldib kohv?", "Вам нравится кофе?", false),
                        Choice("Kas lift töötab?", "Лифт работает?", false),
                    ),
                    npcReplyEt = "Olen pargi juures, sinise mütsiga.",
                    npcReplyRu = "Я у парка, в синей шапке.",
                    teachWordIds = listOf("p_kus_q")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Ah, näen sind! Aitäh.",
                    npcRu = "А, вижу тебя! Спасибо.",
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
                askReadyStep("Jah, valmis: supp, leib ja vesi.", "Да, готов: суп, хлеб и вода."),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Supp on kuum, ole ettevaatlik.",
                    npcRu = "Суп горячий, будь осторожен.",
                    questionRu = "Поблагодарите за готовый заказ:",
                    choices = listOf(
                        Choice("Aitäh! Ilusat päeva.", "Спасибо! Хорошего дня.", true),
                        Choice("Ei, ma ei taha suppi.", "Нет, я не хочу суп.", false),
                        Choice("Vabandust, kus on uks?", "Извините, где дверь?", false),
                    ),
                    npcReplyEt = "Sulle ka! Head teed.",
                    npcReplyRu = "И тебе! Счастливого пути.",
                    teachWordIds = listOf("f_supp", "f_leib", "d_vesi", "g_aitah")
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
                    npcEt = "", npcRu = "",
                    questionRu = "На входной двери домофон. Спросите код двери:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Mis on ukse kood?", "Какой код двери?", true),
                        Choice("Kas teil on kass?", "У вас есть кошка?", false),
                        Choice("Kui vana te olete?", "Сколько вам лет?", false),
                    ),
                    npcReplyEt = "Kood on üks-kaks-kolm-neli.",
                    npcReplyRu = "Код — один-два-три-четыре.",
                    teachWordIds = listOf("p_kood_q", "n_1", "n_2", "n_3")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "Suur aitäh, et nii kiiresti!",
                    npcRu = "Большое спасибо, что так быстро!",
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
                askReadyStep("Üks hetk, panen kotti. Kala, salat ja mahl.", "Минутку, складываю в пакет. Рыба, салат и сок."),
                DialogueStep(
                    speaker = Speaker.RESTORAN,
                    npcEt = "Palun, kõik on koos.",
                    npcRu = "Пожалуйста, всё вместе.",
                    questionRu = "Проверьте сумку и спросите, верный ли это заказ:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Kas see on õige tellimus?", "Это верный заказ?", true),
                        Choice("Kas te tantsite?", "Вы танцуете?", false),
                        Choice("Mis film see on?", "Что это за фильм?", false),
                    ),
                    npcReplyEt = "Jah: kala, salat ja õunamahl.",
                    npcReplyRu = "Да: рыба, салат и яблочный сок.",
                    teachWordIds = listOf("p_oige_q", "f_kala", "f_salat", "d_mahl")
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
                    npcReplyEt = "Suurepärane, aitäh!",
                    npcReplyRu = "Прекрасно, спасибо!",
                    teachWordIds = listOf("p_ukse_taha", "n_2", "s_uks")
                ),
                DialogueStep(
                    speaker = Speaker.KLIENT,
                    npcEt = "", npcRu = "",
                    questionRu = "Двор закрыт. Спросите у клиента код двери:",
                    courierAsks = true,
                    choices = listOf(
                        Choice("Mis on ukse kood?", "Какой код двери?", true),
                        Choice("Kas meri on soe?", "Море тёплое?", false),
                        Choice("Head ööd!", "Спокойной ночи!", false),
                    ),
                    npcReplyEt = "Kood on viis-kuus-seitse-kaheksa. Aitäh!",
                    npcReplyRu = "Код — пять-шесть-семь-восемь. Спасибо!",
                    teachWordIds = listOf("p_kood_q", "n_5", "n_6", "n_7", "n_8")
                ),
            )
        ),
    )
}
