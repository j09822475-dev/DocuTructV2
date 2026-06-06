package ee.kuller.app.data

import ee.kuller.app.model.Choice
import ee.kuller.app.model.Delivery
import ee.kuller.app.model.Nav
import ee.kuller.app.model.NavOption
import ee.kuller.app.model.Order
import ee.kuller.app.model.Phase
import ee.kuller.app.model.Scenario
import ee.kuller.app.model.Thread
import ee.kuller.app.model.Turn

/**
 * Сборщик диалога-доставки как ГРАФА ФАЗ.
 *
 * Каждая фаза — разговор в ОДНОМ чате; внутри фазы вопросы ветвятся (любой
 * вариант рабочий и ведёт к логичному завершению фазы). После фазы курьер САМ
 * выбирает, что делать дальше ([Nav.Choose]) — написать клиенту или в поддержку.
 */
object DialogueFactory {

    private fun rest(et: String, ru: String) = Turn.Say(Thread.RESTORAN, et, ru)
    private fun client(et: String, ru: String) = Turn.Say(Thread.KLIENT, et, ru)
    private fun support(et: String, ru: String) = Turn.Say(Thread.TUGI, et, ru)

    private fun c(
        et: String, ru: String, correct: Boolean = true,
        rating: Double = 0.0, followUp: List<Turn> = emptyList()
    ) = Choice(et, ru, correct, followUp, rating)

    private fun ask(
        thread: Thread, prompt: String, courier: Boolean,
        choices: List<Choice>, teach: List<String> = emptyList()
    ): Turn = Turn.Ask(thread, prompt, courier, choices, teach)

    private val hub = Nav.Choose(
        "Заказ у вас. Что дальше?",
        listOf(NavOption("📲 Написать клиенту", "client"), NavOption("🎧 Связаться с поддержкой", "support_opt"))
    )

    // ---------- общие фазы ----------
    private fun greetReadyAsk(): Turn = ask(
        Thread.RESTORAN, "Зайдите в ресторан, поздоровайтесь и спросите, готов ли заказ:", true,
        listOf(
            c("Tere! Kas tellimus on valmis?", "Здравствуйте! Заказ готов?"),
            c("Head aega, nägemist!", "До свидания!", correct = false, rating = -0.05,
                followUp = listOf(rest("Oih, te ikka tulite tellimusele järele? Üks hetk.", "Ой, вы всё-таки за заказом? Минутку."))),
            c("Üks õlu, palun.", "Одно пиво, пожалуйста.", correct = false, rating = -0.03,
                followUp = listOf(rest("Me ei müü õlut. Aga teie tellimus on kohe valmis.", "Мы не продаём пиво. Но ваш заказ сейчас будет."))),
        ),
        listOf("g_tere", "p_valmis_q")
    )

    private fun confirmAsk(o: Order): Turn = ask(
        Thread.RESTORAN, "Сверьте заказ и подтвердите, что всё верно:", false,
        listOf(
            c(o.confirmEt, o.confirmRu),
            c("Ei, see on vale tellimus.", "Нет, это неверный заказ.", correct = false, rating = -0.05,
                followUp = listOf(rest("Kontrollin... ei, kõik on õige.", "Проверяю... нет, всё верно."))),
            c("Üks õlu, palun.", "Одно пиво.", correct = false, rating = -0.03,
                followUp = listOf(rest("Naljakas! Aga siin on teie toit.", "Смешно! Но вот ваша еда."))),
        ),
        o.itemTeach
    )

    private fun readyTurn(): Turn = rest("Nüüd on valmis. Palun, head teed!", "Теперь готово. Пожалуйста, счастливого пути!")

    /** 10 разных сюжетов получения заказа в ресторане — выбирается случайно. */
    private fun restScript(o: Order): List<Turn> = listOf(
        // 1. Классический
        buildList {
            add(greetReadyAsk())
            add(rest("Jah! Teil on tellimus: ${o.itemsEt}, eks?", "Да! У вас заказ: ${o.itemsRu}, верно?"))
            add(confirmAsk(o))
            add(rest("Peaaegu valmis, oota üks minut.", "Почти готово, подожди минутку."))
            add(readyTurn())
        },
        // 2. По имени на заказе
        buildList {
            add(rest("Tere! Tere tulemast!", "Здравствуйте! Добро пожаловать!"))
            add(ask(Thread.RESTORAN, "Поздоровайтесь и спросите, на какое имя заказ:", true, listOf(
                c("Tere! Mis nimi on tellimusel?", "Здравствуйте! На какое имя заказ?"),
                c("Andke mulle suvaline tellimus.", "Дайте любой заказ.", correct = false, rating = -0.05,
                    followUp = listOf(rest("Ee, ma vajan nime.", "Эм, мне нужно имя."))),
                c("Head aega!", "До свидания!", correct = false, rating = -0.05,
                    followUp = listOf(rest("Oodake, te ei võtnud tellimust!", "Подождите, вы не забрали заказ!"))),
            ), listOf("g_tere", "p_nimi_q")))
            add(rest("Tellimus on ${o.customer} nimele: ${o.itemsEt}.", "Заказ на имя ${o.customer}: ${o.itemsRu}."))
            add(confirmAsk(o))
            add(readyTurn())
        },
        // 3. По номеру заказа
        buildList {
            add(greetReadyAsk())
            add(rest("Jah! Teie tellimuse number on seitse.", "Да! Номер вашего заказа — семь."))
            add(ask(Thread.RESTORAN, "Назовите номер заказа, чтобы забрать:", false, listOf(
                c("Number seitse, palun.", "Номер семь, пожалуйста."),
                c("Number sada.", "Номер сто.", correct = false, rating = -0.05,
                    followUp = listOf(rest("Ei, number seitse.", "Нет, номер семь."))),
                c("Ma ei tea numbrit.", "Я не знаю номер.", correct = false, rating = -0.03,
                    followUp = listOf(rest("See on seitse. Siin on teie toit.", "Это семь. Вот ваша еда."))),
            ), listOf("n_7")))
            add(rest("Siin on number seitse: ${o.itemsEt}. Head teed!", "Вот номер семь: ${o.itemsRu}. Счастливого пути!"))
        },
        // 4. Запара в ресторане
        buildList {
            add(rest("Tere! Meil on praegu väga kiire. Üks hetk!", "Здравствуйте! У нас сейчас очень много дел. Минутку!"))
            add(ask(Thread.RESTORAN, "В ресторане запара. Ответьте вежливо:", true, listOf(
                c("Pole muret, ma ootan.", "Не беспокойтесь, я подожду."),
                c("Kiirustage, mul pole aega!", "Поторопитесь, у меня нет времени!", correct = false, rating = -0.1,
                    followUp = listOf(rest("Me teeme nii kiiresti kui saame.", "Мы делаем так быстро, как можем."))),
                c("Ma lähen ära.", "Я ухожу.", correct = false, rating = -0.1,
                    followUp = listOf(rest("Oodake, kohe valmis!", "Подождите, сейчас готово!"))),
            ), listOf("g_tere")))
            add(rest("Aitäh kannatlikkuse eest! Teil on: ${o.itemsEt}.", "Спасибо за терпение! У вас: ${o.itemsRu}."))
            add(confirmAsk(o))
            add(readyTurn())
        },
        // 5. Ещё не готов
        buildList {
            add(greetReadyAsk())
            add(rest("Vabandust, see pole veel valmis. Umbes viis minutit.", "Извините, ещё не готово. Около пяти минут."))
            add(ask(Thread.RESTORAN, "Заказ ещё готовится. Ответьте:", true, listOf(
                c("Selge, ootan viis minutit.", "Понятно, подожду пять минут."),
                c("See on liiga kaua!", "Это слишком долго!", correct = false, rating = -0.05,
                    followUp = listOf(rest("Anname endast parima.", "Постараемся побыстрее."))),
                c("Tühistage tellimus.", "Отмените заказ.", correct = false, rating = -0.1,
                    followUp = listOf(rest("Ei-ei, kohe saab valmis!", "Нет-нет, сейчас будет готово!"))),
            ), listOf("n_5", "g_aitah")))
            add(rest("Nüüd on valmis: ${o.itemsEt}. Palun!", "Теперь готово: ${o.itemsRu}. Пожалуйста!"))
            add(confirmAsk(o))
        },
        // 6. Сначала выдали не тот
        buildList {
            add(greetReadyAsk())
            add(rest("Jah, siin on teie tellimus.", "Да, вот ваш заказ."))
            add(ask(Thread.RESTORAN, "Проверьте — точно ваш заказ? Спросите:", true, listOf(
                c("Kas see on õige tellimus?", "Это верный заказ?"),
                c("Aitäh, ma lähen!", "Спасибо, я пошёл!", correct = false, rating = -0.1,
                    followUp = listOf(rest("Oodake! See oli vale tellimus!", "Подождите! Это был неверный заказ!"))),
                c("Pole vahet.", "Без разницы.", correct = false, rating = -0.05,
                    followUp = listOf(rest("Vahet on! See pole teie oma.", "Разница есть! Это не ваш."))),
            ), listOf("p_oige_q")))
            add(rest("Oih, vabandust! Siin on õige: ${o.itemsEt}.", "Ой, извините! Вот верный: ${o.itemsRu}."))
            add(confirmAsk(o))
            add(readyTurn())
        },
        // 7. С собой или на месте
        buildList {
            add(greetReadyAsk())
            add(rest("Teil on: ${o.itemsEt}. Kas kaasa või sööte kohapeal?", "У вас: ${o.itemsRu}. С собой или здесь?"))
            add(ask(Thread.RESTORAN, "Вы курьер — заказ навынос. Ответьте:", false, listOf(
                c("Kaasa, palun. Olen kuller.", "С собой, пожалуйста. Я курьер."),
                c("Söön kohapeal.", "Поем здесь.", correct = false, rating = -0.05,
                    followUp = listOf(rest("Aga see on kulleritellimus, eks?", "Но это же курьерский заказ, верно?"))),
                c("Mõlemat.", "И то, и другое.", correct = false, rating = -0.03,
                    followUp = listOf(rest("Hmm, pakin kaasa siis.", "Хм, тогда упакую с собой."))),
            ), listOf("fq_kaasa", "p_jargi")))
            add(rest("Selge, pakin kaasa. Head teed!", "Понятно, упакую с собой. Счастливого пути!"))
        },
        // 8. Проверка комплектности
        buildList {
            add(greetReadyAsk())
            add(rest("Teie tellimus: ${o.itemsEt}. Kontrollige, kas kõik on olemas.", "Ваш заказ: ${o.itemsRu}. Проверьте, всё ли на месте."))
            add(ask(Thread.RESTORAN, "Проверьте сумку и ответьте, всё ли есть:", false, listOf(
                c("Jah, kõik on olemas. Aitäh!", "Да, всё на месте. Спасибо!"),
                c("Midagi on puudu.", "Кое-чего не хватает.", correct = false, rating = 0.02,
                    followUp = listOf(rest("Oih, tõesti! Lisan kohe. Nüüd kõik.", "Ой, и правда! Сейчас добавлю. Теперь всё."))),
                c("Ma ei vaata.", "Я не буду проверять.", correct = false, rating = -0.05,
                    followUp = listOf(rest("Palun kontrollige, et vältida probleeme.", "Пожалуйста, проверьте, чтобы избежать проблем."))),
            ), listOf("p_veel_q", "g_aitah")))
            add(rest("Suurepärane! Head teed!", "Отлично! Счастливого пути!"))
        },
        // 9. «Вы из Bolt?»
        buildList {
            add(rest("Tere! Kas teie olete Boltist?", "Здравствуйте! Вы из Bolt?"))
            add(ask(Thread.RESTORAN, "Подтвердите, что вы курьер за заказом:", true, listOf(
                c("Jah, tulin tellimusele järele.", "Да, я пришёл за заказом."),
                c("Ei, ma lihtsalt vaatan.", "Нет, я просто смотрю.", correct = false, rating = -0.05,
                    followUp = listOf(rest("Ah, vabandust. Siis oodake.", "А, извините. Тогда подождите."))),
                c("Kes küsib?", "Кто спрашивает?", correct = false, rating = -0.05,
                    followUp = listOf(rest("Meie, restoran. Kas tellimuse järele?", "Мы, ресторан. За заказом?"))),
            ), listOf("p_jargi", "g_jah")))
            add(rest("Suurepärane! Teie tellimus: ${o.itemsEt}.", "Отлично! Ваш заказ: ${o.itemsRu}."))
            add(confirmAsk(o))
            add(readyTurn())
        },
        // 10. Утреннее приветствие
        buildList {
            add(ask(Thread.RESTORAN, "Поздоровайтесь по-утреннему и спросите про заказ:", true, listOf(
                c("Tere hommikust! Kas tellimus on valmis?", "Доброе утро! Заказ готов?"),
                c("Head ööd! Tellimus?", "Спокойной ночи! Заказ?", correct = false, rating = -0.03,
                    followUp = listOf(rest("Ee... praegu on hommik. Aga jah.", "Эм... сейчас утро. Но да."))),
                c("Üks õlu hommikuks!", "Одно пиво на утро!", correct = false, rating = -0.05,
                    followUp = listOf(rest("Ei, me ei müü õlut.", "Нет, мы не продаём пиво."))),
            ), listOf("g_hommik", "p_valmis_q")))
            add(rest("Tere hommikust! Teil on: ${o.itemsEt}.", "Доброе утро! У вас: ${o.itemsRu}."))
            add(confirmAsk(o))
            add(readyTurn())
        },
    ).random()

    private fun restPhase(o: Order): Phase = Phase("rest", Thread.RESTORAN, restScript(o), hub)

    private fun supportOptPhase(): Phase = Phase(
        "support_opt", Thread.TUGI,
        listOf(
            support("Tere! Klienditugi kuuleb. Kuidas saan aidata?", "Здравствуйте! Поддержка слушает. Чем помочь?"),
            ask(
                Thread.TUGI, "Что напишете в поддержку?", true,
                listOf(
                    c("Kõik on hästi, tahtsin vaid kontrollida.", "Всё хорошо, хотел только уточнить.",
                        followUp = listOf(support("Suurepärane! Head tööd.", "Отлично! Хорошей работы."))),
                    c("Kas aadress on kindlasti õige?", "Адрес точно верный?",
                        followUp = listOf(support("Jah, aadress on õige. Edu!", "Да, адрес верный. Удачи!"))),
                    c("Ma tahan koju minna.", "Я хочу домой.", correct = false, rating = -0.05,
                        followUp = listOf(support("Palun lõpetage tellimus enne. Aitäh.", "Пожалуйста, сначала завершите заказ. Спасибо."))),
                ),
                listOf("p_tugi_q", "g_tere")
            )
        ),
        Nav.Choose("Что дальше?", listOf(NavOption("📲 Написать клиенту", "client")))
    )

    /** Стандартная передача заказа лицом к лицу. */
    private fun handover(): List<Turn> = listOf(
        ask(
            Thread.KLIENT, "Передайте заказ и пожелайте приятного аппетита:", false,
            listOf(
                c("Palun, siin on teie tellimus. Head isu!", "Пожалуйста, вот ваш заказ. Приятного аппетита!"),
                c("Vabandust, ma sõin selle ära.", "Извините, я это съел.", correct = false, rating = -0.1,
                    followUp = listOf(client("Mida?! See pole naljakas.", "Что?! Это не смешно."))),
                c("Ei, see on minu tellimus.", "Нет, это мой заказ.", correct = false, rating = -0.1,
                    followUp = listOf(client("Kuidas palun?", "Простите, что?"))),
            ),
            listOf("g_palun", "p_head_isu")
        ),
        client("Suur aitäh! Head isu, nägemist!", "Большое спасибо! Приятного аппетита, до свидания!")
    )

    // ---------- фазы клиента/поддержки по сценариям ----------
    private fun scenarioPhases(o: Order): List<Phase> = when (o.scenario) {

        Scenario.FACE_DOOR -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Kus te olete?", "Здравствуйте! Где вы?"))
                add(ask(Thread.KLIENT, "Ответьте, что вы на месте, и спросите, где вас найти:", false, listOf(
                    c("Olen kohal, maja ees. Kus ma teid leian?", "Я на месте, перед домом. Где мне вас найти?"),
                    c("Vabandust, ma eksisin ära.", "Извините, я заблудился.", correct = false, rating = -0.05,
                        followUp = listOf(client("Oh ei... Ma tulen teile vastu.", "О нет... Я выйду навстречу."))),
                    c("Ei, mul ei ole midagi.", "Нет, у меня ничего нет.", correct = false, rating = -0.05,
                        followUp = listOf(client("Kuidas? Ma tellisin toidu!", "Как? Я заказал еду!"))),
                ), listOf("p_kohal", "p_kus_q", "s_maja")))
                add(client("Näen teid! Tulen kohe alla.", "Вижу вас! Сейчас спущусь."))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.LIFT_BROKEN -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Olen kolmandal korrusel. Lift on katki.", "Здравствуйте! Я на третьем этаже. Лифт сломан."))
                add(ask(Thread.KLIENT, "Лифт сломан. Спросите, спустится клиент или вам подняться:", true, listOf(
                    c("Kas tulete alla või tulen üles?", "Вы спуститесь или мне подняться?"),
                    c("Kas pitsa on kuum?", "Пицца горячая?", correct = false, rating = -0.05,
                        followUp = listOf(client("Ää... lihtsalt tooge palun üles.", "Эм... просто принесите наверх."))),
                    c("Ma ootan all igatahes.", "Я в любом случае жду внизу.", correct = false, rating = -0.03,
                        followUp = listOf(client("Olgu, ma tulen siis ise alla.", "Ладно, тогда я сам спущусь."))),
                ), listOf("p_alla_voi_q", "s_korrus", "s_lift")))
                add(client("Tulen ise alla, üks minut!", "Сейчас сам спущусь, минутку!"))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.GATE_CODE -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Värav on lukus, korter üheksa.", "Здравствуйте! Ворота заперты, квартира девять."))
                add(ask(Thread.KLIENT, "Ворота заперты — спросите код двери:", true, listOf(
                    c("Mis on ukse kood?", "Какой код двери?"),
                    c("Head ööd!", "Спокойной ночи!", correct = false, rating = -0.05,
                        followUp = listOf(client("Mis? Praegu on päev!", "Что? Сейчас день!"))),
                    c("Ma jätan toidu lihtsalt siia.", "Я просто оставлю еду здесь.", correct = false, rating = -0.05,
                        followUp = listOf(client("Ei-ei, palun tooge ukse juurde!", "Нет-нет, принесите к двери!"))),
                ), listOf("p_kood_q", "n_9", "s_korter")))
                add(client("Kood on üks-kaks-kolm-neli.", "Код — один-два-три-четыре."))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.DIRECTIONS -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Maja on hoovis, seda on raske leida.", "Здравствуйте! Дом во дворе, его трудно найти."))
                add(ask(Thread.KLIENT, "Спросите у клиента, как к нему пройти:", true, listOf(
                    c("Kuidas ma teie juurde saan?", "Как мне к вам пройти?"),
                    c("Mis kell on?", "Который час?", correct = false, rating = -0.05,
                        followUp = listOf(client("Hmm? Lihtsalt tulge hoovi.", "Хм? Просто зайдите во двор."))),
                    c("Leidke ise mind.", "Найдите меня сами.", correct = false, rating = -0.05,
                        followUp = listOf(client("See on teie töö, mitte minu.", "Это ваша работа, не моя."))),
                ), listOf("p_kuidas_q")))
                add(client("Minge otse, siis paremale. Kollane maja.", "Идите прямо, потом направо. Жёлтый дом."))
                add(ask(Thread.KLIENT, "Подтвердите, что поняли дорогу:", false, listOf(
                    c("Selge: otse ja siis paremale.", "Понятно: прямо, потом направо."),
                    c("Selge: tagasi ja vasakule.", "Понятно: назад и налево.", correct = false, rating = -0.05,
                        followUp = listOf(client("Ei-ei! Otse ja paremale!", "Нет-нет! Прямо и направо!"))),
                    c("Ma ei saa aru.", "Я не понимаю.", correct = false, rating = -0.03,
                        followUp = listOf(client("Otse. Siis. Paremale. Kollane maja!", "Прямо. Потом. Направо. Жёлтый дом!"))),
                ), listOf("s_otse", "s_paremale", "s_maja")))
                add(client("Just nii! Ootan ukse ees.", "Именно! Жду у двери."))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.LEAVE_DOOR -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Jätke palun toit ukse taha. Värava kood on viis-kuus-seitse-kaheksa.", "Здравствуйте! Оставьте еду под дверью. Код ворот пять-шесть-семь-восемь."))
                add(ask(Thread.KLIENT, "Уточните номер квартиры:", true, listOf(
                    c("Selge! Mis on korteri number?", "Понятно! Какой номер квартиры?"),
                    c("Pööra paremale.", "Поверни направо.", correct = false, rating = -0.05,
                        followUp = listOf(client("Mida? Ma räägin korterist.", "Что? Я про квартиру."))),
                    c("Ma ei jäta ust taha.", "Я не оставлю под дверью.", correct = false, rating = -0.05,
                        followUp = listOf(client("Palun jätke, mul pole aega.", "Пожалуйста, оставьте, у меня нет времени."))),
                ), listOf("p_korter_q", "n_5", "n_6", "n_7", "n_8")))
                add(client("Korter kaks. Aitäh!", "Квартира два. Спасибо!"))
                add(ask(Thread.KLIENT, "Подтвердите: оставите под дверью и сделаете фото:", false, listOf(
                    c("Selge, jätan ukse taha ja teen foto.", "Понятно, оставлю под дверью и сделаю фото."),
                    c("Ei, ma võtan toidu endale.", "Нет, я заберу еду себе.", correct = false, rating = -0.15,
                        followUp = listOf(client("See on vargus!", "Это воровство!"))),
                    c("Ma helistan teile.", "Я позвоню вам.", correct = false, rating = -0.02,
                        followUp = listOf(client("Pole vaja, lihtsalt jätke ukse taha.", "Не надо, просто оставьте под дверью."))),
                ), listOf("p_foto", "p_ukse_taha")))
                add(client("Suur aitäh! Head isu!", "Большое спасибо! Приятного аппетита!"))
            }, Nav.End)
        )

        Scenario.CASH -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Te tõite minu tellimuse? Palju ma võlgnen?", "Здравствуйте! Вы привезли мой заказ? Сколько я должен?"))
                add(ask(Thread.KLIENT, "С клиента 10 €. Назовите сумму:", false, listOf(
                    c("Kümme eurot, palun.", "Десять евро, пожалуйста."),
                    c("Sada eurot.", "Сто евро.", correct = false, rating = -0.1,
                        followUp = listOf(client("Mida?! See on liiga palju!", "Что?! Это слишком много!"))),
                    c("Ma ei tea.", "Я не знаю.", correct = false, rating = -0.05,
                        followUp = listOf(client("Vaadake äpist, palun.", "Посмотрите в приложении, пожалуйста."))),
                ), listOf("m_maksab", "m_eurot", "n_10")))
                add(client("Selge. Kas saan kaardiga maksta?", "Понятно. Можно картой?"))
                add(ask(Thread.KLIENT, "Ответьте про способ оплаты:", false, listOf(
                    c("Jah, või sularahas — kuidas soovite.", "Да, или наличными — как хотите."),
                    c("Ainult sularaha.", "Только наличные.", correct = false, rating = -0.03,
                        followUp = listOf(client("Hästi, mul on sularaha.", "Ладно, у меня есть наличные."))),
                    c("Ma ei võta raha.", "Я не беру деньги.", correct = false, rating = -0.05,
                        followUp = listOf(client("Aga tellimus on tasuline...", "Но заказ платный..."))),
                ), listOf("p_maksate_q", "m_sularaha", "m_kaart")))
                add(client("Maksan kaardiga. Aitäh!", "Плачу картой. Спасибо!"))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.WRONG_ADDRESS -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Halloo?", "Алло?"))
                add(ask(Thread.KLIENT, "Уточните, верный ли адрес:", true, listOf(
                    c("Tere! Kas see aadress on õige?", "Здравствуйте! Этот адрес верный?"),
                    c("Kas teile maitseb karri?", "Вам нравится карри?", correct = false, rating = -0.05,
                        followUp = listOf(client("Mis? Ei... kus mu toit on?", "Что? Нет... где моя еда?"))),
                    c("Te elate vales kohas.", "Вы живёте не там.", correct = false, rating = -0.1,
                        followUp = listOf(client("Vabandust?! See on minu kodu.", "Простите?! Это мой дом."))),
                ), listOf("p_aadress_q")))
                add(client("Oi, ei! Õige maja on kollane, teisel pool tänavat.", "Ой, нет! Нужный дом жёлтый, на другой стороне улицы."))
                add(ask(Thread.KLIENT, "Уточните, где именно дом:", true, listOf(
                    c("Kus maja täpselt asub?", "Где именно находится дом?"),
                    c("Kas sajab lund?", "Идёт снег?", correct = false, rating = -0.05,
                        followUp = listOf(client("Ää... roheline uks, teisel korrusel.", "Эм... зелёная дверь, второй этаж."))),
                    c("Tulge ise välja.", "Выйдите сами.", correct = false, rating = -0.05,
                        followUp = listOf(client("Olgu, ma tulen õue.", "Ладно, я выйду на улицу."))),
                ), listOf("p_kus_maja_q", "s_maja")))
                add(client("Roheline uks, teine korrus, korter neli.", "Зелёная дверь, второй этаж, квартира четыре."))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.NOT_HOME -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("(Telefon ei vasta...)", "(Телефон не отвечает...)"))
                add(ask(Thread.KLIENT, "Клиент не отвечает. Что сделать?", true, listOf(
                    c("Ma helistan teile veel kord.", "Я позвоню вам ещё раз.",
                        followUp = listOf(client("Vabandust, ma ei kuulnud! Olen kohe ukse juures.", "Извините, я не слышал! Сейчас буду у двери."))),
                    c("Söön toidu ise ära.", "Съем еду сам.", correct = false, rating = -0.15,
                        followUp = listOf(client("Halloo! Ärge sööge mu toitu!", "Алло! Не ешьте мою еду!"))),
                    c("Sõidan kohe koju.", "Сразу поеду домой.", correct = false, rating = -0.1,
                        followUp = listOf(client("Oodake! Ma olen kodus!", "Подождите! Я дома!"))),
                ), listOf("p_helistan")))
                add(ask(Thread.KLIENT, "Спросите, оставить ли под дверью:", true, listOf(
                    c("Kas jätan toidu ukse taha?", "Оставить еду под дверью?"),
                    c("Kas maksate kaardiga?", "Платите картой?", correct = false, rating = -0.03,
                        followUp = listOf(client("Ää, lihtsalt jätke ukse taha.", "Эм, просто оставьте под дверью."))),
                    c("Ma ootan tund aega.", "Я подожду час.", correct = false, rating = -0.03,
                        followUp = listOf(client("Pole vaja! Jätke ukse taha.", "Не надо! Оставьте под дверью."))),
                ), listOf("p_ukse_taha", "s_uks")))
                add(client("Jah, jätke ukse taha, palun. Aitäh!", "Да, оставьте под дверью, пожалуйста. Спасибо!"))
            }, Nav.End)
        )

        Scenario.LATE -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Kus mu toit on? Ma ootan juba ammu.", "Здравствуйте! Где моя еда? Я уже давно жду."))
                add(ask(Thread.KLIENT, "Вы опоздали (была пробка). Извинитесь:", true, listOf(
                    c("Vabandust hilinemise pärast, olin ummikus.", "Извините за опоздание, я был в пробке."),
                    c("See pole minu süü.", "Это не моя вина.", correct = false, rating = -0.1,
                        followUp = listOf(client("Ikkagi ebameeldiv.", "Всё равно неприятно."))),
                    c("Teie toit on otsas.", "Ваша еда закончилась.", correct = false, rating = -0.15,
                        followUp = listOf(client("Mida?! Ma olen näljane!", "Что?! Я голодный!"))),
                ), listOf("p_hilinen", "g_vabandust")))
                add(client("Pole hullu! Peaasi, et olete kohal.", "Ничего страшного! Главное, что вы здесь."))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.OFFICE -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Tooge palun kontorisse, teine korrus.", "Здравствуйте! Принесите в офис, второй этаж."))
                add(ask(Thread.KLIENT, "Уточните номер кабинета:", true, listOf(
                    c("Selge! Mis kabinetis te olete?", "Понятно! В каком вы кабинете?"),
                    c("Pööra paremale.", "Поверни направо.", correct = false, rating = -0.05,
                        followUp = listOf(client("Ää? Ma räägin kontorist.", "Эм? Я про офис."))),
                    c("Tulge ise alla.", "Спуститесь сами.", correct = false, rating = -0.05,
                        followUp = listOf(client("Mul on koosolek, palun tooge üles.", "У меня встреча, принесите наверх."))),
                ), listOf("p_kabinet_q", "s_korrus")))
                add(client("Kabinet kakskümmend üks, vastuvõtu juures.", "Кабинет двадцать один, у ресепшена."))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.CANCELLED -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Vabandust! Ma pean tellimuse tühistama.", "Извините! Мне нужно отменить заказ."))
                add(ask(Thread.KLIENT, "Клиент отменяет заказ. Ваш ответ:", false, listOf(
                    c("Selge, pole probleemi. Head aega!", "Понятно, без проблем. До свидания!"),
                    c("Ei, te peate maksma!", "Нет, вы должны заплатить!", correct = false, rating = -0.15,
                        followUp = listOf(client("Ma tühistasin reeglite järgi!", "Я отменил по правилам!"))),
                    c("Söön teie toidu ise ära.", "Съем вашу еду сам.", correct = false, rating = -0.05,
                        followUp = listOf(client("Tehke, mis tahate...", "Делайте что хотите..."))),
                ), listOf("g_head_aega", "g_vabandust")))
                add(client("Aitäh mõistmise eest!", "Спасибо за понимание!"))
            }, Nav.End)
        )

        Scenario.COMPLAINT -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Aga toit on külm! Ma ootasin liiga kaua.", "Здравствуйте! Но еда холодная! Я слишком долго ждал."))
                add(ask(Thread.KLIENT, "Клиент жалуется на холодную еду. Ваш ответ:", false, listOf(
                    c("Vabandust! See on ebameeldiv. Lahendame selle.", "Извините! Это неприятно. Решим это.",
                        followUp = listOf(client("Olen pettunud. Mida te teete?", "Я разочарован. Что вы сделаете?"))),
                    c("See on restorani süü, mitte minu.", "Это вина ресторана, не моя.", correct = false, rating = -0.1,
                        followUp = listOf(client("Mind ei huvita, kelle süü!", "Мне всё равно, чья вина!"))),
                    c("Pole minu probleem.", "Не моя проблема.", correct = false, rating = -0.2,
                        followUp = listOf(client("Kohutav! Ma annan ühe tärni.", "Ужасно! Поставлю одну звезду."))),
                ), listOf("g_vabandust")))
            }, Nav.Choose("Как поступить с жалобой?", listOf(
                NavOption("🎧 Связаться с поддержкой", "support_complaint"),
                NavOption("🙇 Извиниться и завершить", "client_apo"),
            ))),
            Phase("support_complaint", Thread.TUGI, buildList {
                add(support("Klienditugi. Kuulen teid.", "Поддержка. Слушаю вас."))
                add(ask(Thread.TUGI, "Опишите проблему поддержке:", true, listOf(
                    c("Klient sai külma toidu, palun hüvitist.", "Клиент получил холодную еду, прошу компенсацию.",
                        followUp = listOf(support("Selge, lisame kliendile kupongi. Tänan!", "Понятно, добавим клиенту купон. Спасибо!"))),
                    c("Klient on lihtsalt vihane.", "Клиент просто злой.", correct = false, rating = -0.05,
                        followUp = listOf(support("Palun olge professionaalne.", "Пожалуйста, будьте профессиональны."))),
                ), listOf("p_tugi_q")))
            }, Nav.Choose("Что дальше?", listOf(NavOption("📲 Вернуться к клиенту", "client_resolved")))),
            Phase("client_resolved", Thread.KLIENT, buildList {
                add(client("Sain kupongi! Aitäh, et aitasite.", "Я получил купон! Спасибо, что помогли."))
                addAll(handover())
            }, Nav.End),
            apologyPhase(),
        )

        Scenario.BREAKDOWN -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Ootan tellimust.", "Здравствуйте! Жду заказ."))
                add(ask(Thread.KLIENT, "В пути сломался велосипед. Что напишете клиенту?", true, listOf(
                    c("Vabandust, mu ratas läks katki. Hilinen veidi.", "Извините, мой велосипед сломался. Немного опоздаю.",
                        followUp = listOf(client("Selge, ma ootan. Aitäh, et teatasite!", "Понятно, я подожду. Спасибо, что предупредили!"))),
                    c("Ma ei saa kohale tulla.", "Я не смогу приехать.", correct = false, rating = -0.15,
                        followUp = listOf(client("Mis?! Aga ma olen näljane!", "Что?! Но я голодный!"))),
                    c("Teie probleem.", "Ваша проблема.", correct = false, rating = -0.2,
                        followUp = listOf(client("Milline ebaviisakus!", "Какая грубость!"))),
                ), listOf("p_hilinen", "g_vabandust")))
            }, Nav.Choose("Транспорт сломан. Что дальше?", listOf(
                NavOption("🎧 Связаться с поддержкой", "support_break"),
            ))),
            Phase("support_break", Thread.TUGI, buildList {
                add(support("Klienditugi: nägime probleemi. Mis juhtus?", "Поддержка: мы видим проблему. Что случилось?"))
                add(ask(Thread.TUGI, "Сообщите в поддержку о поломке:", true, listOf(
                    c("Mu sõiduk on katki, vajan abi.", "Мой транспорт сломан, нужна помощь.",
                        followUp = listOf(support("Saadame appi teise kulleri. Tänan!", "Отправим другого курьера. Спасибо!"))),
                    c("Kõik on korras.", "Всё в порядке.", correct = false, rating = -0.05,
                        followUp = listOf(support("Kindel? Hoidke ühendust.", "Уверены? Оставайтесь на связи."))),
                ), listOf("p_katki")))
                add(client("Sain toidu kätte, aitäh abi eest!", "Я получил еду, спасибо за помощь!"))
            }, Nav.End),
        )

        Scenario.SPOILED -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Oih... kott on märg. Kas toiduga on kõik korras?", "Здравствуйте! Ой... пакет мокрый. С едой всё в порядке?"))
                add(ask(Thread.KLIENT, "Упаковка протекла в пути. Ваш ответ:", false, listOf(
                    c("Kontrollin kohe... Vabandust, pakend lekkis.", "Сейчас проверю... Извините, упаковка протекла.",
                        followUp = listOf(client("Toit on tõesti laiali. Mida te teete?", "Еда и правда растеклась. Что вы сделаете?"))),
                    c("Kõik on korras, ärge muretsege.", "Всё хорошо, не волнуйтесь.", correct = false, rating = -0.1,
                        followUp = listOf(client("Aga toit on ju laiali!", "Но еда же растеклась!"))),
                    c("Pole minu mure.", "Не моя забота.", correct = false, rating = -0.2,
                        followUp = listOf(client("Ma annan ühe tärni!", "Я поставлю одну звезду!"))),
                ), listOf("p_umber_q", "g_vabandust")))
            }, Nav.Choose("Упаковка протекла. Что дальше?", listOf(
                NavOption("🎧 Связаться с поддержкой", "support_spoiled"),
                NavOption("🙇 Извиниться и завершить", "client_apo"),
            ))),
            Phase("support_spoiled", Thread.TUGI, buildList {
                add(support("Klienditugi. Kirjeldage probleemi.", "Поддержка. Опишите проблему."))
                add(ask(Thread.TUGI, "Опишите проблему поддержке:", true, listOf(
                    c("Pakend lekkis, toit on rikutud. Palun uus tellimus.", "Упаковка протекла, еда испорчена. Прошу новый заказ.",
                        followUp = listOf(support("Vormistame uue tellimuse tasuta. Vabandust!", "Оформим новый заказ бесплатно. Извините!"))),
                    c("Unustage, pole midagi.", "Забудьте, ничего.", correct = false, rating = -0.05,
                        followUp = listOf(support("Kindel? Olgu.", "Уверены? Хорошо."))),
                ), listOf("p_tugi_q", "g_vabandust")))
                add(client("Selge, ootan uut tellimust. Aitäh!", "Понятно, жду новый заказ. Спасибо!"))
            }, Nav.End),
            apologyPhase(),
        )

        Scenario.INTERCOM -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(client("Tere! Maja uksel on fonolukk. Helistage palun uksekellaga.", "Здравствуйте! На двери домофон. Позвоните, пожалуйста, в звонок."))
                add(ask(Thread.KLIENT, "Узнайте номер квартиры, чтобы набрать на домофоне:", true, listOf(
                    c("Mis on teie korterinumber?", "Какой у вас номер квартиры?"),
                    c("Kas teil on koer?", "У вас есть собака?", correct = false, rating = -0.05,
                        followUp = listOf(client("Ää? Korter on kakskümmend kolm.", "Эм? Квартира двадцать три."))),
                    c("Ma jätan toidu õue.", "Оставлю еду на улице.", correct = false, rating = -0.05,
                        followUp = listOf(client("Ei, palun helistage uksekellaga.", "Нет, пожалуйста, позвоните в домофон."))),
                ), listOf("p_korter_q", "s_uksekell", "s_fonolukk")))
                add(client("Korter kakskümmend kolm.", "Квартира двадцать три."))
                add(ask(Thread.KLIENT, "Наберите номер квартиры на домофоне и позвоните:", false, listOf(
                    c("Vajutan kakskümmend kolm ja helistan uksekellaga.", "Наберу двадцать три и позвоню в домофон."),
                    c("Koputan lihtsalt uksele.", "Просто постучу в дверь.", correct = false, rating = -0.03,
                        followUp = listOf(client("Te ei jõua ukseni. Kasutage uksekella.", "Вы не дойдёте до двери. Воспользуйтесь домофоном."))),
                    c("Karjun teie nime.", "Крикну ваше имя.", correct = false, rating = -0.05,
                        followUp = listOf(client("Palun ärge! Lihtsalt helistage.", "Пожалуйста, не надо! Просто позвоните."))),
                ), listOf("p_uksekell", "s_uksekell")))
                add(client("Kuulen teid! Avan ukse. Tulge teisele korrusele.", "Слышу вас! Открываю дверь. Поднимитесь на второй этаж."))
                addAll(handover())
            }, Nav.End)
        )
    }

    /** Общая фаза «извиниться и завершить» (для жалобы/протечки). */
    private fun apologyPhase(): Phase = Phase(
        "client_apo", Thread.KLIENT, buildList {
            add(ask(Thread.KLIENT, "Извинитесь напрямую и завершите:", false, listOf(
                c("Veel kord vabandust. Palun, teie tellimus. Head isu!", "Ещё раз извините. Пожалуйста, ваш заказ. Приятного аппетита!"),
                c("Pole midagi teha.", "Ничего не поделать.", correct = false, rating = -0.05,
                    followUp = listOf(client("Hmm.", "Хм."))),
            ), listOf("g_vabandust", "p_head_isu")))
            add(client("Olgu... aitäh.", "Ладно... спасибо."))
        },
        Nav.End
    )

    fun build(o: Order): Delivery {
        val phases = buildMap {
            put("rest", restPhase(o))
            put("support_opt", supportOptPhase())
            scenarioPhases(o).forEach { put(it.id, it) }
        }
        return Delivery("rest", phases)
    }
}
