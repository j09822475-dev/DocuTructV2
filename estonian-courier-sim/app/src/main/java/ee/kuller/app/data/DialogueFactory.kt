package ee.kuller.app.data

import ee.kuller.app.model.Choice
import ee.kuller.app.model.Order
import ee.kuller.app.model.Scenario
import ee.kuller.app.model.Thread
import ee.kuller.app.model.Turn

/**
 * Генератор диалогов доставки.
 *
 * Реплики распределены по ОТДЕЛЬНЫМ ЧАТАМ ([Thread]): ресторан, клиент, поддержка.
 * Часть вопросов — обучающие (один верный вариант), часть — РАЗВИЛКИ ([branching]),
 * где каждый ответ ведёт в свою ветку с последствиями (рейтинг, жалобы, поддержка).
 */
object DialogueFactory {

    private fun rest(et: String, ru: String) = Turn.Say(Thread.RESTORAN, et, ru)
    private fun client(et: String, ru: String) = Turn.Say(Thread.KLIENT, et, ru)
    private fun support(et: String, ru: String) = Turn.Say(Thread.TUGI, et, ru)

    /** Обучающий вопрос: один верный вариант, остальные — «попробуй ещё». */
    private fun learnAsk(
        thread: Thread, promptRu: String, courier: Boolean,
        correct: Pair<String, String>, wrong: List<Pair<String, String>>, teach: List<String>
    ): Turn {
        val choices = buildList {
            add(Choice(correct.first, correct.second, correct = true))
            wrong.shuffled().take(2).forEach { add(Choice(it.first, it.second, correct = false)) }
        }
        return Turn.Ask(thread, promptRu, courier, branching = false, choices = choices, teachWordIds = teach)
    }

    /** Развилка: каждый вариант ведёт в свою ветку (followUp) с последствиями. */
    private fun decideAsk(
        thread: Thread, promptRu: String, courier: Boolean,
        choices: List<Choice>, teach: List<String> = emptyList()
    ): Turn = Turn.Ask(thread, promptRu, courier, branching = true, choices = choices, teachWordIds = teach)

    private fun enRoute() = learnAsk(
        Thread.KLIENT, "Напишите клиенту, что забрали заказ и уже едете:", true,
        correct = listOf(
            "Tere! Võtsin tellimuse, olen kohe kohal." to "Здравствуйте! Забрал заказ, скоро буду.",
            "Tere! Olen teel, kohe kohal." to "Здравствуйте! Я в пути, скоро буду.",
        ).random(),
        wrong = listOf(
            "Ma ei leidnud restorani." to "Я не нашёл ресторан.",
            "Söön teie toidu ära." to "Съем вашу еду.",
            "Head ööd!" to "Спокойной ночи!",
        ),
        teach = listOf("p_kohal", "g_tere")
    )

    // ---------------- 1. РЕСТОРАН ----------------
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
    )

    private fun pickup(o: Order): List<Turn> = buildList {
        add(
            learnAsk(
                Thread.RESTORAN, "Зайдите в ресторан, поздоровайтесь и спросите, готов ли заказ:", true,
                correct = listOf(
                    "Tere! Kas tellimus on valmis?" to "Здравствуйте! Заказ готов?",
                    "Tervist! Kas saan tellimuse kätte?" to "Здравствуйте! Можно забрать заказ?",
                ).random(),
                wrong = listOf(
                    "Head aega, nägemist!" to "До свидания!",
                    "Ei, aitäh, ma ei taha." to "Нет, спасибо, я не хочу.",
                    "Kus on tualett?" to "Где туалет?",
                ),
                teach = listOf("g_tere", "p_valmis_q")
            )
        )
        val m = moment.random()
        add(rest("Tere! ${m.first}", "Здравствуйте! ${m.second}"))
        add(rest("Teil on tellimus: ${o.itemsEt}, eks?", "У вас заказ: ${o.itemsRu}, верно?"))
        add(
            learnAsk(
                Thread.RESTORAN, "Сверьте заказ и подтвердите, что всё верно:", false,
                correct = o.confirmEt to o.confirmRu,
                wrong = listOf(
                    "Ei, see on vale tellimus." to "Нет, это неверный заказ.",
                    "Üks õlu, palun." to "Одно пиво, пожалуйста.",
                    "Ma ei tea." to "Я не знаю.",
                ),
                teach = o.itemTeach
            )
        )
        val a = almostReady.random()
        add(rest(a.first, a.second))
        val r = ready.random()
        add(rest(r.first, r.second))
    }

    // ---------------- 2. ПОИСК КЛИЕНТА / СОБЫТИЕ ----------------
    private fun findClient(o: Order): List<Turn> = when (o.scenario) {
        Scenario.FACE_DOOR -> buildList {
            add(enRoute())
            add(client("Tere! Kus te olete?", "Здравствуйте! Где вы?"))
            add(
                learnAsk(
                    Thread.KLIENT, "Ответьте, что вы на месте, и спросите, где его найти:", false,
                    correct = "Olen kohal, maja ees. Kus ma teid leian?" to "Я на месте, перед домом. Где мне вас найти?",
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
                learnAsk(
                    Thread.KLIENT, "Лифт сломан. Спросите, спустится клиент или вам подняться:", true,
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
                learnAsk(
                    Thread.KLIENT, "Ворота заперты — спросите код двери:", true,
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
                learnAsk(
                    Thread.KLIENT, "Спросите у клиента, как к нему пройти:", true,
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
                learnAsk(
                    Thread.KLIENT, "Клиент объяснил дорогу. Подтвердите, что поняли:", false,
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
                learnAsk(
                    Thread.KLIENT, "Уточните номер квартиры:", true,
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
                learnAsk(
                    Thread.RESTORAN, "🏪 Ресторан: клиент платит наличными, 10 €. Сколько взять с клиента?", false,
                    correct = "Kümme eurot." to "Десять евро.",
                    wrong = listOf("Kaks eurot." to "Два евро.", "Sada eurot." to "Сто евро."),
                    teach = listOf("m_maksab", "m_eurot", "n_10")
                )
            )
            add(client("Tere! Te tõite minu tellimuse?", "Здравствуйте! Вы привезли мой заказ?"))
            add(
                learnAsk(
                    Thread.KLIENT, "Уточните способ оплаты:", true,
                    correct = "Kas maksate sularahas või kaardiga?" to "Платите наличными или картой?",
                    wrong = listOf("Kas teil on koer?" to "У вас есть собака?", "Mis kell on?" to "Который час?"),
                    teach = listOf("p_maksate_q", "m_sularaha", "m_kaart")
                )
            )
            add(client("Sularahas. Siin on kümme eurot.", "Наличными. Вот десять евро."))
        }

        Scenario.WRONG_ADDRESS -> buildList {
            add(
                learnAsk(
                    Thread.KLIENT, "Вы приехали, но такого дома нет. Что случилось?", false,
                    correct = "Aadress on vale." to "Адрес неверный.",
                    wrong = listOf("Toit on valmis." to "Еда готова.", "Lift on katki." to "Лифт сломан."),
                    teach = listOf("p_aadress_q")
                )
            )
            add(client("Halloo?", "Алло?"))
            add(
                learnAsk(
                    Thread.KLIENT, "Позвоните клиенту и уточните, верный ли адрес:", true,
                    correct = "Tere! Kas see aadress on õige?" to "Здравствуйте! Этот адрес верный?",
                    wrong = listOf("Kas teile maitseb karri?" to "Вам нравится карри?", "Head ööd!" to "Спокойной ночи!"),
                    teach = listOf("p_aadress_q")
                )
            )
            add(client("Oi, ei! Õige maja on kollane, teisel pool tänavat.", "Ой, нет! Нужный дом жёлтый, на другой стороне улицы."))
            add(
                learnAsk(
                    Thread.KLIENT, "Уточните, где именно дом:", true,
                    correct = "Kus maja täpselt asub?" to "Где именно находится дом?",
                    wrong = listOf("Mis su lemmiktoit on?" to "Какая твоя любимая еда?", "Kas sajab lund?" to "Идёт снег?"),
                    teach = listOf("p_kus_maja_q", "s_maja")
                )
            )
            add(client("Roheline uks, teine korrus, korter neli.", "Зелёная дверь, второй этаж, квартира четыре."))
        }

        Scenario.NOT_HOME -> buildList {
            add(
                learnAsk(
                    Thread.KLIENT, "Вы на месте, но клиент не отвечает на телефон. Что сделать?", false,
                    correct = "Ma helistan teile veel kord." to "Я позвоню вам ещё раз.",
                    wrong = listOf("Söön toidu ise ära." to "Съем еду сам.", "Sõidan kohe koju." to "Сразу поеду домой."),
                    teach = listOf("p_helistan")
                )
            )
            add(client("Vabandust, ma ei kuulnud telefoni!", "Извините, я не слышал телефон!"))
            add(
                learnAsk(
                    Thread.KLIENT, "Спросите, оставить ли заказ под дверью:", true,
                    correct = "Kas jätan toidu ukse taha?" to "Оставить еду под дверью?",
                    wrong = listOf("Kas maksate kaardiga?" to "Платите картой?", "Kus on lift?" to "Где лифт?"),
                    teach = listOf("p_ukse_taha", "s_uks")
                )
            )
            add(client("Jah, jätke ukse taha, palun.", "Да, оставьте под дверью, пожалуйста."))
        }

        Scenario.LATE -> buildList {
            add(enRoute())
            add(client("Tere! Kus mu toit on? Ma ootan juba ammu.", "Здравствуйте! Где моя еда? Я уже давно жду."))
            add(
                learnAsk(
                    Thread.KLIENT, "Вы немного опоздали (была пробка). Извинитесь:", true,
                    correct = "Vabandust hilinemise pärast, olin ummikus." to "Извините за опоздание, я был в пробке.",
                    wrong = listOf("Teie toit on otsas." to "Ваша еда закончилась.", "Ma ei tule kohale." to "Я не приеду."),
                    teach = listOf("p_hilinen", "g_vabandust")
                )
            )
            add(client("Pole hullu! Peaasi, et olete kohal.", "Ничего страшного! Главное, что вы здесь."))
        }

        Scenario.OFFICE -> buildList {
            add(enRoute())
            add(client("Tere! Tooge palun kontorisse, teine korrus.", "Здравствуйте! Принесите, пожалуйста, в офис, второй этаж."))
            add(
                learnAsk(
                    Thread.KLIENT, "Уточните номер кабинета:", true,
                    correct = "Selge! Mis kabinetis te olete?" to "Понятно! В каком вы кабинете?",
                    wrong = listOf("Kas teil on kass?" to "У вас есть кошка?", "Pööra paremale." to "Поверни направо."),
                    teach = listOf("p_kabinet_q", "s_korrus")
                )
            )
            add(client("Kabinet kakskümmend üks, vastuvõtu juures.", "Кабинет двадцать один, у ресепшена."))
        }

        Scenario.CANCELLED -> buildList {
            add(enRoute())
            add(client("Vabandust! Ma pean tellimuse tühistama.", "Извините! Мне нужно отменить заказ."))
        }

        // ----- ПРОБЛЕМНЫЕ СЮЖЕТЫ С РАЗВИЛКАМИ -----
        Scenario.COMPLAINT -> buildList {
            add(enRoute())
            add(client("Tere! Aga toit on külm! Ma ootasin liiga kaua.", "Здравствуйте! Но еда холодная! Я слишком долго ждал."))
            add(
                decideAsk(
                    Thread.KLIENT, "Клиент жалуется на холодную еду. Как ответите?", false,
                    choices = listOf(
                        Choice(
                            "Vabandust! Pöördun kohe klienditoe poole.", "Извините! Сразу обращусь в поддержку.",
                            correct = true, ratingDelta = 0.05,
                            followUp = listOf(
                                support("Tere, klienditugi. Kuidas saan aidata?", "Здравствуйте, поддержка. Чем помочь?"),
                                decideAsk(
                                    Thread.TUGI, "Опишите проблему поддержке:", true,
                                    choices = listOf(
                                        Choice(
                                            "Klient sai külma toidu, palun hüvitist.", "Клиент получил холодную еду, прошу компенсацию.",
                                            correct = true, ratingDelta = 0.1,
                                            followUp = listOf(
                                                support("Selge, lisame kliendile kupongi. Aitäh!", "Понятно, добавим клиенту купон. Спасибо!"),
                                                client("Oo, sain kupongi! Tänan abi eest.", "О, я получил купон! Спасибо за помощь.")
                                            )
                                        ),
                                        Choice(
                                            "Pole midagi, unustage.", "Ничего, забудьте.",
                                            correct = false, ratingDelta = -0.05,
                                            followUp = listOf(support("Olgu, head aega.", "Хорошо, до свидания."))
                                        ),
                                    ),
                                    teach = listOf("p_tugi_q")
                                )
                            )
                        ),
                        Choice(
                            "See on restorani süü, mitte minu.", "Это вина ресторана, не моя.",
                            correct = false, ratingDelta = -0.1,
                            followUp = listOf(client("Hmm, ikkagi pettumus...", "Хм, всё равно обидно..."))
                        ),
                        Choice(
                            "Pole minu probleem.", "Не моя проблема.",
                            correct = false, ratingDelta = -0.2,
                            followUp = listOf(client("See on kohutav teenindus! Ma kirjutan halva arvustuse.", "Это ужасный сервис! Я напишу плохой отзыв."))
                        ),
                    ),
                    teach = listOf("g_vabandust", "p_tugi_q")
                )
            )
        }

        Scenario.BREAKDOWN -> buildList {
            add(enRoute())
            add(client("Tere! Ootan tellimust.", "Здравствуйте! Жду заказ."))
            add(
                decideAsk(
                    Thread.KLIENT, "В пути сломался велосипед. Что написать клиенту?", true,
                    choices = listOf(
                        Choice(
                            "Vabandust, mu ratas läks katki. Hilinen veidi.", "Извините, мой велосипед сломался. Немного опоздаю.",
                            correct = true, ratingDelta = 0.05,
                            followUp = listOf(client("Selge, ma ootan. Aitäh, et teatasite!", "Понятно, я подожду. Спасибо, что предупредили!"))
                        ),
                        Choice(
                            "Ma ei saa kohale tulla.", "Я не смогу приехать.",
                            correct = false, ratingDelta = -0.15,
                            followUp = listOf(client("Mis?! Aga ma olen näljane!", "Что?! Но я голодный!"))
                        ),
                    ),
                    teach = listOf("p_hilinen", "g_vabandust")
                )
            )
            add(support("Klienditugi: nägime, et teil on probleem. Mis juhtus?", "Поддержка: мы видим у вас проблему. Что случилось?"))
            add(
                decideAsk(
                    Thread.TUGI, "Сообщите в поддержку о поломке:", true,
                    choices = listOf(
                        Choice(
                            "Mu sõiduk on katki, vajan abi.", "Мой транспорт сломан, нужна помощь.",
                            correct = true, ratingDelta = 0.05,
                            followUp = listOf(support("Saadame appi teise kulleri. Tänan!", "Отправим на помощь другого курьера. Спасибо!"))
                        ),
                        Choice(
                            "Kõik on korras.", "Всё в порядке.",
                            correct = false, ratingDelta = -0.05,
                            followUp = listOf(support("Kindel? Olgu, hoidke ühendust.", "Уверены? Хорошо, оставайтесь на связи."))
                        ),
                    ),
                    teach = listOf("p_katki")
                )
            )
        }

        Scenario.SPOILED -> buildList {
            add(rest("Pane tähele: pakend on veidi lahti.", "Обрати внимание: упаковка немного открыта."))
            add(
                decideAsk(
                    Thread.RESTORAN, "Упаковка повреждена, еда может вытечь. Что сделать?", true,
                    choices = listOf(
                        Choice(
                            "Kas saate ümber pakkida?", "Можете переупаковать?",
                            correct = true, ratingDelta = 0.1,
                            followUp = listOf(
                                rest("Muidugi! Pakime uuesti, korralikult.", "Конечно! Переупакуем как следует."),
                                enRoute(),
                                client("Tere! Tellimus on terve ja korralik. Aitäh!", "Здравствуйте! Заказ целый и аккуратный. Спасибо!")
                            )
                        ),
                        Choice(
                            "Pole midagi, võtan nii.", "Ничего, возьму так.",
                            correct = false, ratingDelta = -0.15,
                            followUp = listOf(
                                enRoute(),
                                client("Toit on laiali! Pakend lekkis kotis.", "Еда растеклась! Упаковка протекла в сумке."),
                                decideAsk(
                                    Thread.KLIENT, "Еда вытекла. Как ответить клиенту?", false,
                                    choices = listOf(
                                        Choice(
                                            "Vabandust! Pöördun klienditoe poole hüvitise pärast.", "Извините! Обращусь в поддержку за компенсацией.",
                                            correct = true, ratingDelta = 0.1,
                                            followUp = listOf(
                                                support("Klienditugi: vormistame uue tellimuse tasuta.", "Поддержка: оформим новый заказ бесплатно."),
                                                client("Aitäh, et lahendasite!", "Спасибо, что решили!")
                                            )
                                        ),
                                        Choice(
                                            "See pole minu mure.", "Это не моя забота.",
                                            correct = false, ratingDelta = -0.2,
                                            followUp = listOf(client("Ma annan teile ühe tärni!", "Я поставлю вам одну звезду!"))
                                        ),
                                    ),
                                    teach = listOf("p_tugi_q", "g_vabandust")
                                )
                            )
                        ),
                    ),
                    teach = listOf("p_umber_q")
                )
            )
        }
    }

    // ---------------- 3. ПЕРЕДАЧА / ФИНАЛ ----------------
    private fun closing(o: Order): List<Turn> = when (o.scenario) {
        Scenario.LEAVE_DOOR, Scenario.NOT_HOME -> buildList {
            add(
                learnAsk(
                    Thread.KLIENT, "Подтвердите: оставите под дверью и сделаете фото:", false,
                    correct = "Selge, jätan ukse taha ja teen foto." to "Понятно, оставлю под дверью и сделаю фото.",
                    wrong = listOf("Ei, ma võtan toidu endale." to "Нет, я заберу еду себе.", "Pööra paremale." to "Поверни направо."),
                    teach = listOf("p_foto", "p_ukse_taha")
                )
            )
            add(client("Suur aitäh! Vabandust ja head isu!", "Большое спасибо! Извините и приятного аппетита!"))
        }

        Scenario.CASH -> buildList {
            add(
                learnAsk(
                    Thread.KLIENT, "Деньги ровные. Передайте чек, заказ и пожелайте приятного аппетита:", false,
                    correct = "Aitäh! Siin on kviitung. Head isu!" to "Спасибо! Вот чек. Приятного аппетита!",
                    wrong = listOf("Vabandust, mul ei ole toitu." to "Извините, у меня нет еды.", "Ei, see on liiga vähe." to "Нет, этого мало."),
                    teach = listOf("m_kviitung", "p_head_isu")
                )
            )
            add(client("Suur aitäh! Nägemist.", "Большое спасибо! До свидания."))
        }

        Scenario.CANCELLED -> buildList {
            add(
                learnAsk(
                    Thread.KLIENT, "Заказ отменён. Ответьте вежливо и попрощайтесь:", false,
                    correct = "Selge, pole probleemi. Head aega!" to "Понятно, без проблем. До свидания!",
                    wrong = listOf("Ei, te peate maksma!" to "Нет, вы должны заплатить!", "Söön teie toidu ära." to "Съем вашу еду."),
                    teach = listOf("g_head_aega", "g_vabandust")
                )
            )
            add(client("Aitäh mõistmise eest! Vabandust veel kord.", "Спасибо за понимание! Ещё раз извините."))
        }

        Scenario.BREAKDOWN -> buildList {
            add(support("Teine kuller võttis tellimuse üle. Tubli töö!", "Другой курьер принял заказ. Хорошая работа!"))
            add(client("Sain toidu kätte, aitäh abi eest!", "Я получил еду, спасибо за помощь!"))
        }

        else -> buildList {
            add(
                learnAsk(
                    Thread.KLIENT, "Вы встретились с клиентом. Передайте заказ и пожелайте приятного аппетита:", false,
                    correct = "Palun, siin on teie tellimus. Head isu!" to "Пожалуйста, вот ваш заказ. Приятного аппетита!",
                    wrong = listOf("Vabandust, ma sõin selle ära." to "Извините, я это съел.", "Ei, see on minu tellimus." to "Нет, это мой заказ."),
                    teach = listOf("g_palun", "p_head_isu")
                )
            )
            add(client("Suur aitäh! Ilusat päeva, nägemist!", "Большое спасибо! Хорошего дня, до свидания!"))
        }
    }

    fun build(o: Order): List<Turn> = buildList {
        addAll(pickup(o))
        addAll(findClient(o))
        addAll(closing(o))
    }
}
