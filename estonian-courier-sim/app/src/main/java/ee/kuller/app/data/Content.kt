package ee.kuller.app.data

import ee.kuller.app.model.Category
import ee.kuller.app.model.Order
import ee.kuller.app.model.Scenario
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
                WordEntry("p_kuidas_q", "Kuidas ma teie juurde saan?", "Как мне к вам пройти?", "Maja on hoovis. Kuidas ma teie juurde saan?"),
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
    // ЗАКАЗЫ — только ФАКТЫ. Реплики собирает DialogueFactory на лету,
    // поэтому каждый заход в одно и то же заведение звучит немного иначе.
    // ----------------------------------------------------------------------
    val orders: List<Order> = listOf(
        Order(
            id = "o1", restaurant = "Pizza Grande", customer = "Maarja",
            address = "Pärnu maantee 12, korter 5", distanceKm = 2.3, payout = 4.20,
            itemsEt = "üks pepperoni pitsa ja kola", itemsRu = "одна пицца пепперони и кола",
            confirmEt = "Jah, üks pitsa ja kola. Aitäh!", confirmRu = "Да, одна пицца и кола. Спасибо!",
            itemTeach = listOf("f_pitsa", "g_jah", "g_aitah"), scenario = Scenario.FACE_DOOR
        ),
        Order(
            id = "o2", restaurant = "Sushi Aasia", customer = "Andres",
            address = "Narva maantee 5, 3. korrus", distanceKm = 3.1, payout = 5.10,
            itemsEt = "kaks sushikomplekti ja roheline tee", itemsRu = "два суши-сета и зелёный чай",
            confirmEt = "Jah, kaks komplekti ja tee. Aitäh!", confirmRu = "Да, два сета и чай. Спасибо!",
            itemTeach = listOf("n_2", "d_tee", "g_jah"), scenario = Scenario.LIFT_BROKEN
        ),
        Order(
            id = "o3", restaurant = "Burger Maja", customer = "Kati",
            address = "Tartu maantee 40, korter 9", distanceKm = 1.8, payout = 3.90,
            itemsEt = "kolm kanaburgerit ja friikartulid", itemsRu = "три куриных бургера и картошка фри",
            confirmEt = "Jah, kolm burgerit ja friikartulid.", confirmRu = "Да, три бургера и картошка фри.",
            itemTeach = listOf("f_burger", "f_kana", "f_friikad", "n_3"), scenario = Scenario.DIRECTIONS
        ),
        Order(
            id = "o4", restaurant = "Kohvik Sõbralik", customer = "Pjotr",
            address = "Liivalaia 8, korter 4", distanceKm = 2.7, payout = 4.60,
            itemsEt = "kaks kohvi piimaga ja sai juustuga", itemsRu = "два кофе с молоком и булка с сыром",
            confirmEt = "Jah, kaks kohvi ja sai. Aitäh!", confirmRu = "Да, два кофе и булка. Спасибо!",
            itemTeach = listOf("d_kohv", "d_piim", "f_sai", "f_juust", "n_2"), scenario = Scenario.FACE_DOOR
        ),
        Order(
            id = "o5", restaurant = "Supiköök", customer = "Olga",
            address = "Sõpruse puiestee 21, 7. korrus", distanceKm = 4.0, payout = 5.80,
            itemsEt = "kanasupp, must leib ja klaas vett", itemsRu = "куриный суп, чёрный хлеб и стакан воды",
            confirmEt = "Jah, supp, leib ja vesi.", confirmRu = "Да, суп, хлеб и вода.",
            itemTeach = listOf("f_supp", "f_leib", "d_vesi"), scenario = Scenario.GATE_CODE
        ),
        Order(
            id = "o6", restaurant = "Kala & Salat", customer = "Mikk",
            address = "Vana-Posti 7, korter 2", distanceKm = 1.5, payout = 3.50,
            itemsEt = "kala, kreeka salat ja õunamahl", itemsRu = "рыба, греческий салат и яблочный сок",
            confirmEt = "Jah, kala, salat ja mahl.", confirmRu = "Да, рыба, салат и сок.",
            itemTeach = listOf("f_kala", "f_salat", "d_mahl"), scenario = Scenario.LEAVE_DOOR
        ),
        Order(
            id = "o7", restaurant = "Pitsabaar Napoli", customer = "Tiina",
            address = "Roosikrantsi 9, korter 3", distanceKm = 2.0, payout = 4.40,
            itemsEt = "üks suur pitsa", itemsRu = "одна большая пицца",
            confirmEt = "Jah, üks suur pitsa.", confirmRu = "Да, одна большая пицца.",
            itemTeach = listOf("f_pitsa", "n_1"), scenario = Scenario.CASH
        ),
        Order(
            id = "o8", restaurant = "Indica Curry", customer = "Rein",
            address = "Tehnika 18 (aadress segane)", distanceKm = 3.4, payout = 5.40,
            itemsEt = "kaks karrit ja nan-leib", itemsRu = "два карри и лепёшка нан",
            confirmEt = "Jah, kaks karrit ja nan.", confirmRu = "Да, два карри и нан.",
            itemTeach = listOf("n_2", "f_leib"), scenario = Scenario.WRONG_ADDRESS
        ),
        Order(
            id = "o9", restaurant = "Sushi Tokyo", customer = "Jaan",
            address = "Mustamäe tee 16, korter 21", distanceKm = 2.9, payout = 4.90,
            itemsEt = "üks sushikomplekt ja miso supp", itemsRu = "один суши-сет и суп мисо",
            confirmEt = "Jah, komplekt ja supp.", confirmRu = "Да, сет и суп.",
            itemTeach = listOf("f_supp", "n_1"), scenario = Scenario.NOT_HOME
        ),
    )
}
