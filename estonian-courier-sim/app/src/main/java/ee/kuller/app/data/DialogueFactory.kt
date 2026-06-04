package ee.kuller.app.data

import ee.kuller.app.model.Choice
import ee.kuller.app.model.Order
import ee.kuller.app.model.Scenario
import ee.kuller.app.model.Speaker
import ee.kuller.app.model.Turn

/**
 * Генератор диалогов доставки.
 *
 * Диалог собирается на лету из блоков-заготовок, поэтому одно и то же заведение
 * каждый раз звучит немного иначе, но структура сохраняется:
 *   1) РЕСТОРАН  — курьер спрашивает «готов?», ресторан отвечает несколькими
 *      репликами с логическими паузами, курьер подтверждает заказ;
 *   2) ПОИСК КЛИЕНТА — зависит от сценария (лифт, код, «как пройти», нет дома…);
 *   3) ПЕРЕДАЧА — курьер отдаёт заказ / оставляет под дверью, клиент благодарит.
 *
 * Никакого «навигатора»: направления курьер узнаёт у самого клиента.
 */
object DialogueFactory {

    private fun say(speaker: Speaker, et: String, ru: String) = Turn.Say(speaker, et, ru)
    private fun rest(et: String, ru: String) = say(Speaker.RESTORAN, et, ru)
    private fun client(et: String, ru: String) = say(Speaker.KLIENT, et, ru)

    /** Ask с одним верным и двумя случайными неверными вариантами. */
    private fun ask(
        promptRu: String,
        courierAsks: Boolean,
        correct: Pair<String, String>,
        wrong: List<Pair<String, String>>,
        teach: List<String>
    ): Turn {
        val choices = buildList {
            add(Choice(correct.first, correct.second, true))
            wrong.shuffled().take(2).forEach { add(Choice(it.first, it.second, false)) }
        }
        return Turn.Ask(promptRu, courierAsks, choices, teach)
    }

    // --- Общие пулы фраз (для разнообразия) ---
    private val moment = listOf(
        "Üks hetk, vaatan." to "Минутку, смотрю.",
        "Kohe vaatan järele." to "Сейчас проверю.",
        "Oota natuke, palun." to "Подожди немного, пожалуйста.",
    )
    private val almostReady = listOf(
        "Peaaegu valmis, oota üks minut." to "Почти готово, подожди минутку.",
        "Kohe-kohe, veel üks hetk." to "Сейчас-сейчас, ещё момент.",
        "Pakin kohe kotti." to "Сейчас сложу в пакет.",
    )
    private val ready = listOf(
        "Nüüd on valmis. Palun, head teed!" to "Теперь готово. Пожалуйста, счастливого пути!",
        "Valmis! Võta, head teed!" to "Готово! Держи, счастливого пути!",
        "Siin on su tellimus. Head teed!" to "Вот твой заказ. Счастливого пути!",
    )
    private val readyWrong = listOf(
        "Head aega, nägemist!" to "До свидания!",
        "Ei, aitäh, ma ei taha." to "Нет, спасибо, я не хочу.",
        "Üks õlu, palun." to "Одно пиво, пожалуйста.",
        "Kus on tualett?" to "Где туалет?",
    )
    private val confirmWrong = listOf(
        "Ei, see on vale tellimus." to "Нет, это неверный заказ.",
        "Üks õlu, palun." to "Одно пиво, пожалуйста.",
        "Ma ei tea." to "Я не знаю.",
        "Söön selle ise ära." to "Съем это сам.",
    )
    private val enRouteWrong = listOf(
        "Ma ei leidnud restorani." to "Я не нашёл ресторан.",
        "Söön teie toidu ära." to "Съем вашу еду.",
        "Head ööd!" to "Спокойной ночи!",
        "Mis kell on?" to "Который час?",
    )

    private fun greetCourier() = listOf(
        "Tere! Kas tellimus on valmis?" to "Здравствуйте! Заказ готов?",
        "Tervist! Kas saan tellimuse kätte?" to "Здравствуйте! Можно забрать заказ?",
    ).random()

    private fun enRoute() = ask(
        "Напишите клиенту, что забрали заказ и уже едете:", true,
        correct = listOf(
            "Tere! Võtsin tellimuse, olen kohe kohal." to "Здравствуйте! Забрал заказ, скоро буду.",
            "Tere! Olen teel, kohe kohal." to "Здравствуйте! Я в пути, скоро буду.",
        ).random(),
        wrong = enRouteWrong,
        teach = listOf("p_kohal", "g_tere")
    )

    // ======================================================================
    // 1. ПОЛУЧЕНИЕ ЗАКАЗА В РЕСТОРАНЕ
    // ======================================================================
    private fun pickup(o: Order): List<Turn> = buildList {
        add(
            ask(
                "Зайдите в ресторан, поздоровайтесь и спросите, готов ли заказ:", true,
                correct = greetCourier(),
                wrong = readyWrong,
                teach = listOf("g_tere", "p_valmis_q")
            )
        )
        val m = moment.random()
        add(rest("Tere! ${m.first}", "Здравствуйте! ${m.second}"))
        add(rest("Teil on tellimus: ${o.itemsEt}, eks?", "У вас заказ: ${o.itemsRu}, верно?"))
        add(
            ask(
                "Сверьте заказ и подтвердите, что всё верно:", false,
                correct = o.confirmEt to o.confirmRu,
                wrong = confirmWrong,
                teach = o.itemTeach
            )
        )
        // Две реплики с логической паузой между ними:
        val a = almostReady.random()
        add(rest(a.first, a.second))
        val r = ready.random()
        add(rest(r.first, r.second))
    }

    // ======================================================================
    // 2. ПОИСК КЛИЕНТА (зависит от сценария) — без навигатора
    // ======================================================================
    private fun findClient(o: Order): List<Turn> = when (o.scenario) {
        Scenario.FACE_DOOR -> buildList {
            add(enRoute())
            add(client("Tere! Kus te olete?", "Здравствуйте! Где вы?"))
            add(
                ask(
                    "Ответьте клиенту, что вы на месте, и спросите, где его найти:", false,
                    correct = "Olen kohal, maja ees. Kus ma teid leian?"
                        to "Я на месте, перед домом. Где мне вас найти?",
                    wrong = listOf(
                        "Vabandust, ma eksisin ära." to "Извините, я заблудился.",
                        "Ei, mul ei ole midagi." to "Нет, у меня ничего нет.",
                    ),
                    teach = listOf("p_kohal", "p_kus_q", "s_maja")
                )
            )
            add(client("Näen teid! Tulen kohe alla.", "Вижу вас! Сейчас спущусь."))
        }

        Scenario.LIFT_BROKEN -> buildList {
            add(enRoute())
            add(client("Tere! Olen kolmandal korrusel. Lift on katki.", "Здравствуйте! Я на третьем этаже. Лифт сломан."))
            add(
                ask(
                    "Лифт сломан. Спросите, спустится клиент или вам подняться:", true,
                    correct = "Kas tulete alla või tulen üles?" to "Вы спуститесь или мне подняться?",
                    wrong = listOf(
                        "Kas pitsa on kuum?" to "Пицца горячая?",
                        "Pööra vasakule." to "Поверни налево.",
                    ),
                    teach = listOf("p_alla_voi_q", "s_korrus", "s_lift")
                )
            )
            add(client("Oi, ma tulen ise alla. Üks minut!", "Ой, я сам спущусь. Одну минуту!"))
        }

        Scenario.GATE_CODE -> buildList {
            add(enRoute())
            add(client("Tere! Olen üheksandas korteris, aga värav on lukus.", "Здравствуйте! Я в квартире девять, но ворота заперты."))
            add(
                ask(
                    "Ворота заперты — спросите код двери:", true,
                    correct = "Mis on ukse kood?" to "Какой код двери?",
                    wrong = listOf(
                        "Kas teil on kass?" to "У вас есть кошка?",
                        "Head ööd!" to "Спокойной ночи!",
                    ),
                    teach = listOf("p_kood_q", "n_9", "s_korter")
                )
            )
            add(client("Kood on üks-kaks-kolm-neli.", "Код — один-два-три-четыре."))
        }

        Scenario.DIRECTIONS -> buildList {
            add(enRoute())
            add(client("Tere! Maja on hoovis, seda on raske leida.", "Здравствуйте! Дом во дворе, его трудно найти."))
            add(
                ask(
                    "Спросите у клиента, как к нему пройти:", true,
                    correct = "Kuidas ma teie juurde saan?" to "Как мне к вам пройти?",
                    wrong = listOf(
                        "Kas teile meeldib kohv?" to "Вам нравится кофе?",
                        "Mis kell on?" to "Который час?",
                    ),
                    teach = listOf("p_kuidas_q")
                )
            )
            add(client("Minge otse, siis paremale. Kollane maja.", "Идите прямо, потом направо. Жёлтый дом."))
            add(
                ask(
                    "Клиент объяснил дорогу. Подтвердите, что поняли:", false,
                    correct = "Selge: otse ja siis paremale." to "Понятно: прямо, потом направо.",
                    wrong = listOf(
                        "Selge: tagasi ja vasakule." to "Понятно: назад и налево.",
                        "Ma ei saa aru." to "Я не понимаю.",
                    ),
                    teach = listOf("s_otse", "s_paremale", "s_maja")
                )
            )
            add(client("Just nii! Ootan ukse ees.", "Именно! Жду у двери."))
        }

        Scenario.LEAVE_DOOR -> buildList {
            add(enRoute())
            add(client("Tere! Jätke palun toit ukse taha. Värava kood on viis-kuus-seitse-kaheksa.", "Здравствуйте! Оставьте, пожалуйста, еду под дверью. Код ворот пять-шесть-семь-восемь."))
            add(
                ask(
                    "Уточните номер квартиры:", true,
                    correct = "Selge! Mis on korteri number?" to "Понятно! Какой номер квартиры?",
                    wrong = listOf(
                        "Kas te tantsite?" to "Вы танцуете?",
                        "Pööra paremale." to "Поверни направо.",
                    ),
                    teach = listOf("p_korter_q", "n_5", "n_6", "n_7", "n_8")
                )
            )
            add(client("Korter kaks. Aitäh!", "Квартира два. Спасибо!"))
        }

        Scenario.CASH -> buildList {
            add(
                ask(
                    "🏪 Ресторан: клиент платит наличными, 10 €. Сколько взять с клиента?", false,
                    correct = "Kümme eurot." to "Десять евро.",
                    wrong = listOf(
                        "Kaks eurot." to "Два евро.",
                        "Sada eurot." to "Сто евро.",
                    ),
                    teach = listOf("m_maksab", "m_eurot", "n_10")
                )
            )
            add(client("Tere! Te tõite minu tellimuse?", "Здравствуйте! Вы привезли мой заказ?"))
            add(
                ask(
                    "Уточните способ оплаты:", true,
                    correct = "Kas maksate sularahas või kaardiga?" to "Платите наличными или картой?",
                    wrong = listOf(
                        "Kas teil on koer?" to "У вас есть собака?",
                        "Mis kell on?" to "Который час?",
                    ),
                    teach = listOf("p_maksate_q", "m_sularaha", "m_kaart")
                )
            )
            add(client("Sularahas. Siin on kümme eurot.", "Наличными. Вот десять евро."))
        }

        Scenario.WRONG_ADDRESS -> buildList {
            add(
                ask(
                    "Вы приехали, но такого дома нет. Что случилось?", false,
                    correct = "Aadress on vale." to "Адрес неверный.",
                    wrong = listOf(
                        "Toit on valmis." to "Еда готова.",
                        "Lift on katki." to "Лифт сломан.",
                    ),
                    teach = listOf("p_aadress_q")
                )
            )
            add(client("Halloo?", "Алло?"))
            add(
                ask(
                    "Позвоните клиенту и уточните, верный ли адрес:", true,
                    correct = "Tere! Kas see aadress on õige?" to "Здравствуйте! Этот адрес верный?",
                    wrong = listOf(
                        "Kas teile maitseb karri?" to "Вам нравится карри?",
                        "Head ööd!" to "Спокойной ночи!",
                    ),
                    teach = listOf("p_aadress_q")
                )
            )
            add(client("Oi, ei! Õige maja on kollane, teisel pool tänavat.", "Ой, нет! Нужный дом жёлтый, на другой стороне улицы."))
            add(
                ask(
                    "Уточните, где именно дом:", true,
                    correct = "Kus maja täpselt asub?" to "Где именно находится дом?",
                    wrong = listOf(
                        "Mis su lemmiktoit on?" to "Какая твоя любимая еда?",
                        "Kas sajab lund?" to "Идёт снег?",
                    ),
                    teach = listOf("p_kus_maja_q", "s_maja")
                )
            )
            add(client("Roheline uks, teine korrus, korter neli.", "Зелёная дверь, второй этаж, квартира четыре."))
            add(
                ask(
                    "Подтвердите, что нашли дом и сейчас будете:", false,
                    correct = "Selge, leian üles. Tulen kohe!" to "Понятно, найду. Сейчас буду!",
                    wrong = listOf(
                        "Ma ei tule kohale." to "Я не приеду.",
                        "Sõidan tagasi restorani." to "Поеду обратно в ресторан.",
                    ),
                    teach = listOf("s_maja")
                )
            )
            add(client("Tore, ootan ukse ees!", "Отлично, жду у двери!"))
        }

        Scenario.NOT_HOME -> buildList {
            add(
                ask(
                    "Вы на месте, но клиент не отвечает на телефон. Что сделать?", false,
                    correct = "Ma helistan teile veel kord." to "Я позвоню вам ещё раз.",
                    wrong = listOf(
                        "Söön toidu ise ära." to "Съем еду сам.",
                        "Sõidan kohe koju." to "Сразу поеду домой.",
                    ),
                    teach = listOf("p_helistan")
                )
            )
            add(client("Vabandust, ma ei kuulnud telefoni!", "Извините, я не слышал телефон!"))
            add(
                ask(
                    "Спросите, оставить ли заказ под дверью:", true,
                    correct = "Kas jätan toidu ukse taha?" to "Оставить еду под дверью?",
                    wrong = listOf(
                        "Kas maksate kaardiga?" to "Платите картой?",
                        "Kus on lift?" to "Где лифт?",
                    ),
                    teach = listOf("p_ukse_taha", "s_uks")
                )
            )
            add(client("Jah, jätke ukse taha, palun.", "Да, оставьте под дверью, пожалуйста."))
        }

        Scenario.LATE -> buildList {
            add(enRoute())
            add(client("Tere! Kus mu toit on? Ma ootan juba ammu.", "Здравствуйте! Где моя еда? Я уже давно жду."))
            add(
                ask(
                    "Вы немного опоздали (была пробка). Извинитесь:", true,
                    correct = "Vabandust hilinemise pärast, olin ummikus." to "Извините за опоздание, я был в пробке.",
                    wrong = listOf(
                        "Teie toit on otsas." to "Ваша еда закончилась.",
                        "Ma ei tule kohale." to "Я не приеду.",
                    ),
                    teach = listOf("p_hilinen", "g_vabandust")
                )
            )
            add(client("Pole hullu! Peaasi, et olete kohal.", "Ничего страшного! Главное, что вы здесь."))
        }

        Scenario.OFFICE -> buildList {
            add(enRoute())
            add(client("Tere! Tooge palun kontorisse, teine korrus.", "Здравствуйте! Принесите, пожалуйста, в офис, второй этаж."))
            add(
                ask(
                    "Уточните номер кабинета:", true,
                    correct = "Selge! Mis kabinetis te olete?" to "Понятно! В каком вы кабинете?",
                    wrong = listOf(
                        "Kas teil on kass?" to "У вас есть кошка?",
                        "Pööra paremale." to "Поверни направо.",
                    ),
                    teach = listOf("p_kabinet_q", "s_korrus")
                )
            )
            add(client("Kabinet kakskümmend üks, otse vastuvõtu juures.", "Кабинет двадцать один, прямо у ресепшена."))
        }

        Scenario.CANCELLED -> buildList {
            add(enRoute())
            add(client("Vabandust! Ma pean tellimuse tühistama.", "Извините! Мне нужно отменить заказ."))
        }
    }

    // ======================================================================
    // 3. ПЕРЕДАЧА ЗАКАЗА
    // ======================================================================
    private fun closing(o: Order): List<Turn> = when (o.scenario) {
        Scenario.LEAVE_DOOR, Scenario.NOT_HOME -> buildList {
            add(
                ask(
                    "Подтвердите: оставите под дверью и сделаете фото:", false,
                    correct = "Selge, jätan ukse taha ja teen foto." to "Понятно, оставлю под дверью и сделаю фото.",
                    wrong = listOf(
                        "Ei, ma võtan toidu endale." to "Нет, я заберу еду себе.",
                        "Pööra paremale." to "Поверни направо.",
                    ),
                    teach = listOf("p_foto", "p_ukse_taha")
                )
            )
            add(client("Suur aitäh! Vabandust ja head isu!", "Большое спасибо! Извините и приятного аппетита!"))
        }

        Scenario.CASH -> buildList {
            add(
                ask(
                    "Деньги ровные. Передайте чек, заказ и пожелайте приятного аппетита:", false,
                    correct = "Aitäh! Siin on kviitung. Head isu!" to "Спасибо! Вот чек. Приятного аппетита!",
                    wrong = listOf(
                        "Vabandust, mul ei ole toitu." to "Извините, у меня нет еды.",
                        "Ei, see on liiga vähe." to "Нет, этого мало.",
                    ),
                    teach = listOf("m_kviitung", "p_head_isu")
                )
            )
            add(client("Suur aitäh! Nägemist.", "Большое спасибо! До свидания."))
        }

        Scenario.CANCELLED -> buildList {
            add(
                ask(
                    "Заказ отменён. Ответьте вежливо и попрощайтесь:", false,
                    correct = "Selge, pole probleemi. Head aega!" to "Понятно, без проблем. До свидания!",
                    wrong = listOf(
                        "Ei, te peate maksma!" to "Нет, вы должны заплатить!",
                        "Söön teie toidu ära." to "Съем вашу еду.",
                    ),
                    teach = listOf("g_head_aega", "g_vabandust")
                )
            )
            add(client("Aitäh mõistmise eest! Vabandust veel kord.", "Спасибо за понимание! Ещё раз извините."))
        }

        else -> buildList {
            add(
                ask(
                    "Вы встретились с клиентом. Передайте заказ и пожелайте приятного аппетита:", false,
                    correct = "Palun, siin on teie tellimus. Head isu!" to "Пожалуйста, вот ваш заказ. Приятного аппетита!",
                    wrong = listOf(
                        "Vabandust, ma sõin selle ära." to "Извините, я это съел.",
                        "Ei, see on minu tellimus." to "Нет, это мой заказ.",
                    ),
                    teach = listOf("g_palun", "p_head_isu")
                )
            )
            add(client("Suur aitäh! Ilusat päeva, nägemist!", "Большое спасибо! Хорошего дня, до свидания!"))
        }
    }

    /** Собрать полный диалог доставки. */
    fun build(o: Order): List<Turn> = buildList {
        addAll(pickup(o))
        addAll(findClient(o))
        addAll(closing(o))
    }
}
