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

    /** Вариант объяснения дороги клиентом + подходящее/неподходящее подтверждение курьера. */
    private data class Dir(
        val say: Pair<String, String>,
        val ok: Pair<String, String>,
        val bad: Pair<String, String>,
        val teach: List<String>,
    )

    private val dirVariants = listOf(
        Dir("Minge otse, siis paremale. Kollane maja." to "Идите прямо, потом направо. Жёлтый дом.",
            "Selge: otse ja siis paremale." to "Понятно: прямо, потом направо.",
            "Selge: tagasi ja vasakule." to "Понятно: назад и налево.",
            listOf("s_otse", "s_paremale", "s_maja")),
        Dir("Pöörake vasakule, sinine maja hoovis." to "Поверните налево, синий дом во дворе.",
            "Selge: vasakule, maja hoovis." to "Понятно: налево, дом во дворе.",
            "Selge: paremale, esimene maja." to "Понятно: направо, первый дом.",
            listOf("s_vasakule", "s_maja")),
        Dir("Sissepääs on tagant, hoovi poolt." to "Вход сзади, со стороны двора.",
            "Selge: sissepääs hoovi poolt." to "Понятно: вход со стороны двора.",
            "Selge: sissepääs tänava poolt." to "Понятно: вход со стороны улицы.",
            listOf("s_uks", "s_maja")),
        Dir("Minge keldrist sisse, kõige alumine uks." to "Заходите через подвал, самая нижняя дверь.",
            "Selge: kelder, alumine uks." to "Понятно: подвал, нижняя дверь.",
            "Selge: ülemine korrus, lift." to "Понятно: верхний этаж, лифт.",
            listOf("s_uks", "s_korrus")),
        Dir("Roheline maja, teine sissepääs." to "Зелёный дом, второй вход.",
            "Selge: roheline maja, teine uks." to "Понятно: зелёный дом, вторая дверь.",
            "Selge: punane maja, esimene uks." to "Понятно: красный дом, первая дверь.",
            listOf("s_maja", "s_uks")),
        Dir("Sõitke otse lõpuni, siis paremale. Punane maja." to "Прямо до конца, потом направо. Красный дом.",
            "Selge: otse lõpuni ja paremale." to "Понятно: прямо до конца и направо.",
            "Selge: kohe vasakule." to "Понятно: сразу налево.",
            listOf("s_otse", "s_paremale", "s_maja")),
        Dir("Maja on tagahoovis, väravast sisse." to "Дом в заднем дворе, входить через ворота.",
            "Selge: tagahoovi, väravast sisse." to "Понятно: в задний двор, через ворота.",
            "Selge: peauksest, esiküljelt." to "Понятно: через парадную, спереди.",
            listOf("s_maja", "s_uks")),
        Dir("Esimene maja vasakul, valge uks." to "Первый дом слева, белая дверь.",
            "Selge: esimene maja vasakul, valge uks." to "Понятно: первый дом слева, белая дверь.",
            "Selge: viimane maja paremal." to "Понятно: последний дом справа.",
            listOf("s_vasakule", "s_maja", "s_uks")),
        Dir("Minge läbi värava, siis trepist üles." to "Идите через ворота, потом вверх по лестнице.",
            "Selge: värava kaudu, trepist üles." to "Понятно: через ворота, вверх по лестнице.",
            "Selge: liftiga alla keldrisse." to "Понятно: на лифте вниз в подвал.",
            listOf("s_uks", "s_lift")),
        Dir("Kollane maja, sissepääs küljelt." to "Жёлтый дом, вход сбоку.",
            "Selge: kollane maja, küljelt sisse." to "Понятно: жёлтый дом, заходить сбоку.",
            "Selge: otse peauksest." to "Понятно: прямо через парадную.",
            listOf("s_maja", "s_uks")),
    )

    /** 14 вариантов, где именно в здании находится клиент (офис). */
    private val officeLocations = listOf(
        "Kabinet kakskümmend üks, vastuvõtu juures." to "Кабинет 21, у ресепшена.",
        "Kolmas korrus, kabinet seitse, koridori lõpus." to "Третий этаж, кабинет 7, в конце коридора.",
        "Teine korrus, esimene uks vasakul." to "Второй этаж, первая дверь слева.",
        "Neljas korrus, klaasuksega kontor." to "Четвёртый этаж, офис со стеклянной дверью.",
        "Kabinet kümme, köögi kõrval." to "Кабинет 10, рядом с кухней.",
        "Viies korrus, otse liftist välja." to "Пятый этаж, прямо из лифта.",
        "Kabinet kolmkümmend kaks, paremal koridoris." to "Кабинет 32, справа по коридору.",
        "Teine korrus, suur avatud kontor, akna juures." to "Второй этаж, опенспейс, у окна.",
        "Kuues korrus, juhi kabinet, lõpus paremal." to "Шестой этаж, кабинет директора, в конце справа.",
        "Kabinet viis, kohe trepi kõrval." to "Кабинет 5, сразу у лестницы.",
        "Kolmas korrus, IT-osakond, sinine uks." to "Третий этаж, IT-отдел, синяя дверь.",
        "Teine korrus, raamatupidamine, viimane uks." to "Второй этаж, бухгалтерия, последняя дверь.",
        "Seitsmes korrus, terrassi poolt." to "Седьмой этаж, со стороны террасы.",
        "Kabinet üheksa, valvelaua taga." to "Кабинет 9, за стойкой охраны.",
    )

    private val hub = Nav.Choose(
        "Заказ у вас. Что дальше?",
        listOf(NavOption("📲 Написать клиенту", "client"), NavOption("🎧 Связаться с поддержкой", "support_opt"))
    )

    // ---------- общие фазы ----------
    private fun courierOpen(): Turn {
        val greet = listOf(
            "Tere! Tulin tellimusele järele." to "Здравствуйте! Я за заказом.",
            "Tervist! Olen kuller, tulin tellimusele järele." to "Здравствуйте! Я курьер, я за заказом.",
            "Tere päevast! Tulin tellimusele järele." to "Добрый день! Я за заказом.",
            "Tere! Kuller siin, tulin tellimusele järele." to "Здравствуйте! Курьер, я за заказом.",
        ).random()
        return ask(
            Thread.RESTORAN, "Зайдите в ресторан, поздоровайтесь и скажите, что вы за заказом:", true,
            listOf(
                c(greet.first, greet.second),
                c("Head aega, nägemist!", "До свидания!", correct = false, rating = -0.05,
                    followUp = listOf(rest("Oih, te ikka tulite tellimusele järele? Üks hetk.", "Ой, вы всё-таки за заказом? Минутку."))),
                c("Võtan selle koti siit, see on vist minu.", "Возьму вот этот пакет, он вроде мой.", correct = false, rating = -0.05,
                    followUp = listOf(rest("Oodake! See on teise kulleri tellimus. Küsige alati enne.", "Подождите! Это заказ другого курьера. Всегда сначала спрашивайте."))),
            ),
            listOf("g_tere", "p_jargi")
        )
    }

    private fun confirmAsk(o: Order): Turn = ask(
        Thread.RESTORAN, "Сверьте заказ и подтвердите, что всё верно:", false,
        listOf(
            c(o.confirmEt, o.confirmRu),
            c("Ei, see on vale tellimus.", "Нет, это неверный заказ.", correct = false, rating = -0.05,
                followUp = listOf(rest("Kontrollin... ei, kõik on õige.", "Проверяю... нет, всё верно."))),
            c("Jah, kindlasti õige. Ma ei vaata, mul on kiire.", "Да, точно верный. Я не смотрю, тороплюсь.", correct = false, rating = -0.05,
                followUp = listOf(
                    rest("Palun ikka kontrollige. Kui midagi on puudu, on probleem teie oma.", "Пожалуйста, всё-таки проверьте. Если чего-то не хватает — проблема будет ваша."),
                    rest("Vaatame koos: ${o.itemsEt}. Kõik on olemas.", "Смотрим вместе: ${o.itemsRu}. Всё на месте."))),
        ),
        o.itemTeach
    )

    private fun readyTurn(): Turn = rest("Nii, valmis ongi. Palun, head teed!", "Так, готово. Пожалуйста, счастливого пути!")

    /** Финальный ход курьера в ресторане: благодарит ПОСЛЕ получения заказа. */
    private fun courierThanks(): Turn = ask(
        Thread.RESTORAN, "Заберите заказ, поблагодарите и попрощайтесь:", false,
        listOf(
            c("Aitäh! Head päeva!", "Спасибо! Хорошего дня!"),
            c("Lõpuks ometi!", "Наконец-то!", correct = false, rating = -0.03,
                followUp = listOf(rest("Vabandust ootamise eest.", "Извините за ожидание."))),
            c("Aitäh! (võtab ainult toidukoti)", "Спасибо! (берёт только пакет с едой)", correct = false, rating = -0.03,
                followUp = listOf(
                    rest("Oodake! Jook jäi maha, see käib ka tellimusega.", "Подождите! Напиток остался, он тоже к заказу."),
                    rest("Nüüd on kõik. Head teed!", "Теперь всё. Счастливого пути!"))),
        ),
        listOf("g_aitah", "g_head_aega")
    )

    /** Получение заказа: курьер ВСЕГДА инициирует, ресторан НЕ знает заказ и
     *  уточняет (имя получателя / номер / показать в приложении). 5 вариантов. */
    private fun restScript(o: Order): List<Turn> {
        val no = (100000..999999).random()
        return listOf(
            // 1. Ресторан спрашивает имя получателя
            buildList {
                add(courierOpen())
                add(rest("Tere! Mis nimi on tellimusel?", "Здравствуйте! На какое имя заказ?"))
                add(ask(Thread.RESTORAN, "Назовите получателя заказа:", false, listOf(
                    c("Tellimus on ${o.customer} nimel.", "Заказ на имя ${o.customer}."),
                    c("Ma ei tea nime.", "Я не знаю имя.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Ilma nimeta ma ei leia tellimust.", "Без имени я не найду заказ."))),
                    c("Pole vahet, andke midagi.", "Без разницы, дайте что-нибудь.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Ma vajan õiget nime.", "Мне нужно правильное имя."))),
                ), listOf("p_nimi_q")))
                add(rest("Üks hetk... Leidsin! ${o.itemsEt}.", "Минутку... Нашёл! ${o.itemsRu}."))
                add(confirmAsk(o))
                add(readyTurn())
                add(courierThanks())
            },
            // 2. Ресторан спрашивает номер заказа
            buildList {
                add(courierOpen())
                add(rest("Tere! Mis on tellimuse number?", "Здравствуйте! Какой номер заказа?"))
                add(ask(Thread.RESTORAN, "Назовите номер заказа из приложения:", false, listOf(
                    c("Tellimuse number on $no.", "Номер заказа $no."),
                    c("Ma ei vaadanud numbrit.", "Я не смотрел номер.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Vaadake palun äpist.", "Посмотрите, пожалуйста, в приложении."))),
                    c("Number üks.", "Номер один.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Hmm, sellist pole. Kontrollige.", "Хм, такого нет. Проверьте."))),
                ), emptyList()))
                add(rest("Number $no... Jah, siin: ${o.itemsEt}.", "Номер $no... Да, вот: ${o.itemsRu}."))
                add(confirmAsk(o))
                add(readyTurn())
                add(courierThanks())
            },
            // 3. Ресторан просит показать заказ в приложении
            buildList {
                add(courierOpen())
                add(rest("Tere! Näidake palun tellimust äpis.", "Здравствуйте! Покажите, пожалуйста, заказ в приложении."))
                add(ask(Thread.RESTORAN, "Покажите заказ — назовите имя и состав:", false, listOf(
                    c("Palun, siin: ${o.customer}, ${o.itemsEt}.", "Пожалуйста, вот: ${o.customer}, ${o.itemsRu}."),
                    c("Mul pole äppi.", "У меня нет приложения.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Kuidas te siis tellimuse saite?", "А как вы тогда получили заказ?"))),
                    c("Vaadake ise.", "Сами смотрите.", correct = false, rating = -0.05,
                        followUp = listOf(rest("See on teie telefonis, palun.", "Это в вашем телефоне, пожалуйста."))),
                ), listOf("p_nimi_q")))
                add(rest("Aitäh! Üks hetk, toon: ${o.itemsEt}.", "Спасибо! Минутку, несу: ${o.itemsRu}."))
                add(confirmAsk(o))
                add(readyTurn())
                add(courierThanks())
            },
            // 4. Запара: ресторан быстро спрашивает имя
            buildList {
                add(courierOpen())
                add(rest("Tere! Meil on kiire. Kelle nimele tellimus?", "Здравствуйте! У нас запара. На чьё имя заказ?"))
                add(ask(Thread.RESTORAN, "Быстро назовите имя получателя:", false, listOf(
                    c("${o.customer} nimele, palun.", "На имя ${o.customer}, пожалуйста."),
                    c("Kiirustage ise!", "Сами поторопитесь!", correct = false, rating = -0.1,
                        followUp = listOf(rest("Me proovime. Aga kelle nimele?", "Мы стараемся. Но на чьё имя?"))),
                    c("Ükskõik kelle.", "Чьё угодно.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Ei, ma vajan nime.", "Нет, мне нужно имя."))),
                ), listOf("p_nimi_q")))
                add(rest("Selge! Üks hetk... Valmis: ${o.itemsEt}.", "Понятно! Минутку... Готово: ${o.itemsRu}."))
                add(confirmAsk(o))
                add(readyTurn())
                add(courierThanks())
            },
            // 5. Ресторан переспрашивает имя и сверяет состав
            buildList {
                add(courierOpen())
                add(rest("Tere! Kelle tellimus?", "Здравствуйте! Чей заказ?"))
                add(ask(Thread.RESTORAN, "Назовите получателя:", false, listOf(
                    c("${o.customer}.", "${o.customer}."),
                    c("Ma unustasin nime.", "Я забыл имя.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Vaadake äpist nime, palun.", "Посмотрите имя в приложении, пожалуйста."))),
                    c("Teie peate teadma.", "Вы должны знать.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Meil on palju tellimusi. Mis nimi?", "У нас много заказов. Какое имя?"))),
                ), listOf("p_nimi_q")))
                add(rest("Aa, ${o.customer}! Kas tellimus on ${o.itemsEt}?", "А, ${o.customer}! Заказ — ${o.itemsRu}?"))
                add(confirmAsk(o))
                add(readyTurn())
                add(courierThanks())
            },
        ).random()
    }

    private fun restPhase(o: Order): Phase = Phase("rest", Thread.RESTORAN, restScript(o), hub)

    private fun supportOptPhase(): Phase = Phase(
        "support_opt", Thread.TUGI,
        listOf(
            support("Tere! Siin klienditugi. Kuidas saan aidata?", "Здравствуйте! Это служба поддержки. Чем могу помочь?"),
            ask(
                Thread.TUGI, "Что напишете в поддержку?", true,
                listOf(
                    c("Kõik on hästi, tahtsin lihtsalt üle küsida.", "Всё хорошо, хотел просто уточнить.",
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
                c("Võtke kiiresti, mul on järgmine tellimus ootel.", "Берите быстрее, у меня следующий заказ ждёт.", correct = false, rating = -0.05,
                    followUp = listOf(client("Üks hetk, palun. Pole vaja mind kiirustada.", "Минутку, пожалуйста. Не надо меня торопить."))),
                c("Ei, see on minu tellimus.", "Нет, это мой заказ.", correct = false, rating = -0.1,
                    followUp = listOf(client("Kuidas palun?", "Простите, что?"))),
            ),
            listOf("g_palun", "p_head_isu")
        ),
        client("Suur aitäh! Ilusat päeva, nägemist!", "Большое спасибо! Хорошего дня, до свидания!")
    )

    /** Опенер курьера: он первым пишет клиенту, что прибыл с заказом. */
    private fun clientArrived(): Turn = ask(
        Thread.KLIENT, "Напишите клиенту, что вы прибыли с заказом:", true,
        listOf(
            c("Tere! Kuller siin, olen teie tellimusega kohal.", "Здравствуйте! Это курьер, я на месте с вашим заказом."),
            c("Olen all. Teil on kaks minutit, siis ma lähen.", "Я внизу. У вас две минуты, потом я уеду.", correct = false, rating = -0.05,
                followUp = listOf(client("Palun ärge survestage mind, ma tulen kohe.", "Пожалуйста, не давите на меня, я сейчас выйду."))),
            c("Avage uks kohe!", "Откройте дверь немедленно!", correct = false, rating = -0.05,
                followUp = listOf(client("Palun olge viisakas.", "Пожалуйста, будьте вежливы."))),
        ),
        listOf("g_tere", "p_kohal", "p_tellimus")
    )

    // ---------- фазы клиента/поддержки по сценариям ----------
    private fun scenarioPhases(o: Order): List<Phase> = when (o.scenario) {

        Scenario.FACE_DOOR -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
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
                add(clientArrived())
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
                add(clientArrived())
                add(client("Tere! Värav on lukus, korter üheksa.", "Здравствуйте! Ворота заперты, квартира девять."))
                add(ask(Thread.KLIENT, "Ворота заперты — спросите код двери:", true, listOf(
                    c("Mis on uksekood?", "Какой код двери?"),
                    c("Värav on lukus. Tulge ise alla, ma ei otsi koodi.", "Ворота заперты. Спуститесь сами, я не буду искать код.", correct = false, rating = -0.05,
                        followUp = listOf(client("Mul on väike laps kodus, ma ei saa tulla. Kood on olemas, küsige.", "У меня дома маленький ребёнок, я не могу выйти. Код есть, спросите."))),
                    c("Ma jätan toidu lihtsalt siia.", "Я просто оставлю еду здесь.", correct = false, rating = -0.05,
                        followUp = listOf(client("Ei-ei, palun tooge ukse juurde!", "Нет-нет, принесите к двери!"))),
                ), listOf("p_kood_q", "n_9", "s_korter")))
                add(client("Kood on üks-kaks-kolm-neli.", "Код — один-два-три-четыре."))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.DIRECTIONS -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                val dir = dirVariants.random()
                add(clientArrived())
                add(client("Tere! Meie maja on raske leida.", "Здравствуйте! Наш дом трудно найти."))
                add(ask(Thread.KLIENT, "Спросите у клиента, как к нему пройти:", true, listOf(
                    c("Kuidas ma teie juurde saan?", "Как мне к вам пройти?"),
                    c("Pole vaja seletada, ma leian GPS-iga.", "Не надо объяснять, найду по GPS.", correct = false, rating = -0.03,
                        followUp = listOf(client("GPS näitab siin valesti. Kuulake parem mind.", "GPS здесь показывает неправильно. Лучше послушайте меня."))),
                    c("Leidke ise mind.", "Найдите меня сами.", correct = false, rating = -0.05,
                        followUp = listOf(client("See on teie töö, mitte minu.", "Это ваша работа, не моя."))),
                ), listOf("p_kuidas_q")))
                add(client(dir.say.first, dir.say.second))
                add(ask(Thread.KLIENT, "Подтвердите, что поняли дорогу:", false, listOf(
                    c(dir.ok.first, dir.ok.second),
                    c(dir.bad.first, dir.bad.second, correct = false, rating = -0.05,
                        followUp = listOf(client("Ei-ei! Kuulake uuesti.", "Нет-нет! Послушайте ещё раз."))),
                    c("Ma ei saa aru.", "Я не понимаю.", correct = false, rating = -0.03,
                        followUp = listOf(client(dir.say.first, dir.say.second))),
                ), dir.teach))
                add(client("Just nii! Ootan teid.", "Именно! Жду вас."))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.LEAVE_DOOR -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
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
                add(client("Suur aitäh! Ilusat päeva, nägemist!", "Большое спасибо! Хорошего дня, до свидания!"))
            }, Nav.End)
        )

        Scenario.CASH -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
                add(client("Tere! Kas te tõite mu tellimuse? Kui palju ma maksma pean?", "Здравствуйте! Вы привезли мой заказ? Сколько мне платить?"))
                add(ask(Thread.KLIENT, "С клиента 10 €. Назовите сумму:", false, listOf(
                    c("Kümme eurot, palun.", "Десять евро, пожалуйста."),
                    c("Kaksteist eurot, palun.", "Двенадцать евро, пожалуйста.", correct = false, rating = -0.05,
                        followUp = listOf(
                            client("Äpis on kümme eurot. Palun kontrollige oma äpist.", "В приложении десять евро. Проверьте, пожалуйста, в своём приложении."),
                            client("Näete? Kümme.", "Видите? Десять."))),
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
                add(ask(Thread.KLIENT, "Напишите клиенту, что вы прибыли по адресу:", true, listOf(
                    c("Tere! Kuller siin, olen teie aadressil.", "Здравствуйте! Курьер, я по вашему адресу."),
                    c("Olen kohal, aga teid pole.", "Я на месте, но вас нет.", correct = false, rating = -0.05,
                        followUp = listOf(client("Imelik... kus te olete?", "Странно... где вы?"))),
                    c("Te andsite vale aadressi.", "Вы дали неверный адрес.", correct = false, rating = -0.05,
                        followUp = listOf(client("Kontrollime koos, palun.", "Давайте проверим вместе."))),
                ), listOf("g_tere", "p_jargi", "p_kohal")))
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
                add(ask(Thread.KLIENT, "Напишите клиенту, что вы прибыли, и позвоните:", true, listOf(
                    c("Tere! Olen kohal teie tellimusega, helistan teile.", "Здравствуйте! Я на месте с заказом, звоню вам."),
                    c("Kus te olete?! Ma lahkun.", "Где вы?! Я ухожу.", correct = false, rating = -0.05,
                        followUp = listOf(client("(Telefon ei vasta...)", "(Телефон не отвечает...)"))),
                    c("Ma ei viitsi oodata.", "Мне неохота ждать.", correct = false, rating = -0.05,
                        followUp = listOf(client("(Telefon ei vasta...)", "(Телефон не отвечает...)"))),
                ), listOf("g_tere", "p_kohal", "p_tellimus")))
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
                    c("Jätan ukse taha ja lähen kohe ära.", "Оставлю под дверью и сразу уеду.", correct = false, rating = -0.05,
                        followUp = listOf(client("Palun tehke enne foto! Muidu ma ei leia tellimust üles.", "Пожалуйста, сначала сделайте фото! Иначе я не найду заказ."))),
                    c("Ma ootan tund aega.", "Я подожду час.", correct = false, rating = -0.03,
                        followUp = listOf(client("Pole vaja! Jätke ukse taha.", "Не надо! Оставьте под дверью."))),
                ), listOf("p_ukse_taha", "s_uks")))
                add(client("Jah, jätke ukse taha, palun. Aitäh!", "Да, оставьте под дверью, пожалуйста. Спасибо!"))
            }, Nav.End)
        )

        Scenario.LATE -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
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
                val loc = officeLocations.random()
                add(clientArrived())
                add(client("Tere! Tooge palun kontorisse.", "Здравствуйте! Принесите, пожалуйста, в офис."))
                add(ask(Thread.KLIENT, "Уточните номер кабинета / где вас найти:", true, listOf(
                    c("Selge! Mis kabinetis te olete?", "Понятно! В каком вы кабинете?"),
                    c("Pööra paremale.", "Поверни направо.", correct = false, rating = -0.05,
                        followUp = listOf(client("Ää? Ma räägin kontorist.", "Эм? Я про офис."))),
                    c("Tulge ise alla.", "Спуститесь сами.", correct = false, rating = -0.05,
                        followUp = listOf(client("Mul on koosolek, palun tooge üles.", "У меня встреча, принесите наверх."))),
                ), listOf("p_kabinet_q", "s_korrus")))
                add(client(loc.first, loc.second))
                addAll(handover())
            }, Nav.End)
        )

        Scenario.CANCELLED -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(ask(Thread.KLIENT, "Напишите клиенту, что вы уже едете с заказом:", true, listOf(
                    c("Tere! Olen teel teie tellimusega.", "Здравствуйте! Я уже еду с вашим заказом."),
                    c("Kus mu jootraha on?", "Где мои чаевые?", correct = false, rating = -0.05,
                        followUp = listOf(client("Ää... ma pean tellimuse tühistama.", "Эм... мне нужно отменить заказ."))),
                    c("Ärge tülitage mind.", "Не беспокойте меня.", correct = false, rating = -0.05,
                        followUp = listOf(client("Vabandust... ma tühistan tellimuse.", "Извините... я отменяю заказ."))),
                ), listOf("g_tere", "p_tellimus", "p_kohal")))
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
                add(ask(Thread.KLIENT, "Напишите клиенту, что вы прибыли и передаёте заказ:", true, listOf(
                    c("Tere! Olen kohal, siin on teie tellimus.", "Здравствуйте! Я на месте, вот ваш заказ."),
                    c("Kus te nii kaua olite?", "Где вы так долго были?", correct = false, rating = -0.05,
                        followUp = listOf(client("Mina küsin sama!", "Это я хочу спросить!"))),
                    c("Sööge kiiresti, muidu jahtub ära.", "Ешьте быстрее, а то остынет.", correct = false, rating = -0.05,
                        followUp = listOf(client("Just nimelt — see ongi külm!", "Вот именно — она холодная!"))),
                ), listOf("g_tere", "p_kohal", "p_tellimus")))
                add(client("Tere! Aga toit on külm! Ma ootasin liiga kaua.", "Здравствуйте! Но еда холодная! Я слишком долго ждал."))
                add(ask(Thread.KLIENT, "Клиент жалуется на холодную еду. Ваш ответ:", false, listOf(
                    c("Vabandust! Saan aru, see on ebameeldiv. Vaatan, mida saab teha.", "Извините! Понимаю, это неприятно. Посмотрю, что можно сделать.",
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
                add(support("Klienditugi, tere. Mis mure on?", "Поддержка, здравствуйте. Что случилось?"))
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
                add(ask(Thread.KLIENT, "Напишите клиенту, что вы в пути с заказом:", true, listOf(
                    c("Tere! Olen teel teie tellimusega.", "Здравствуйте! Я в пути с вашим заказом."),
                    c("Kus mu toit on?", "Где моя еда?", correct = false, rating = -0.05,
                        followUp = listOf(client("Seda küsin mina!", "Это я спрашиваю!"))),
                    c("Ärge kiirustage mind.", "Не торопите меня.", correct = false, rating = -0.05,
                        followUp = listOf(client("Ma lihtsalt ootan.", "Я просто жду."))),
                ), listOf("g_tere", "p_tellimus", "p_kohal")))
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
                add(support("Klienditugi siin. Näeme, et midagi on valesti. Mis juhtus?", "Это поддержка. Видим, что что-то не так. Что случилось?"))
                add(ask(Thread.TUGI, "Сообщите в поддержку о поломке:", true, listOf(
                    c("Mu sõiduk on katki, vajan abi.", "Мой транспорт сломан, нужна помощь.",
                        followUp = listOf(support("Saadame teise kulleri appi. Tänan!", "Отправим другого курьера на помощь. Спасибо!"))),
                    c("Kõik on korras.", "Всё в порядке.", correct = false, rating = -0.05,
                        followUp = listOf(support("Kindel? Hoidke ühendust.", "Уверены? Оставайтесь на связи."))),
                ), listOf("p_katki")))
                add(support("Teine kuller on teel kliendi juurde. Tubli töö, aitäh!", "Другой курьер уже едет к клиенту. Хорошая работа, спасибо!"))
            }, Nav.End),
        )

        Scenario.SPOILED -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
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
                    c("Pakend lekkis, toit on rikutud. Palun uut tellimust.", "Упаковка протекла, еда испорчена. Прошу новый заказ.",
                        followUp = listOf(support("Vormistame uue tellimuse tasuta. Vabandust!", "Оформим новый заказ бесплатно. Извините!"))),
                    c("Unustage, pole midagi.", "Забудьте, ничего.", correct = false, rating = -0.05,
                        followUp = listOf(support("Kindel? Olgu.", "Уверены? Хорошо."))),
                ), listOf("p_tugi_q", "g_vabandust")))
                add(support("Uus tellimus on vormistatud. Tänan abi eest!", "Новый заказ оформлен. Спасибо за помощь!"))
            }, Nav.End),
            apologyPhase(),
        )

        Scenario.INTERCOM -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
                add(client("Tere! Maja uksel on fonolukk. Vajutage palun uksekella.", "Здравствуйте! На двери домофон. Нажмите, пожалуйста, на звонок."))
                add(ask(Thread.KLIENT, "Узнайте номер квартиры, чтобы набрать на домофоне:", true, listOf(
                    c("Mis on teie korterinumber?", "Какой у вас номер квартиры?"),
                    c("Vajutan suvalist korterit, keegi ikka avab.", "Нажму любую квартиру, кто-нибудь да откроет.", correct = false, rating = -0.05,
                        followUp = listOf(client("Palun ärge tehke nii! Naabrid pahandavad. Küsige minu korterinumbrit.", "Пожалуйста, не делайте так! Соседи будут недовольны. Спросите мой номер квартиры."))),
                    c("Ma jätan toidu õue.", "Оставлю еду на улице.", correct = false, rating = -0.05,
                        followUp = listOf(client("Ei, palun vajutage uksekella.", "Нет, пожалуйста, нажмите на звонок."))),
                ), listOf("p_korter_q", "s_uksekell", "s_fonolukk")))
                add(client("Korter kakskümmend kolm.", "Квартира двадцать три."))
                add(ask(Thread.KLIENT, "Наберите номер квартиры на домофоне и позвоните:", false, listOf(
                    c("Valin korteri kakskümmend kolm ja helistan uksekella.", "Наберу квартиру двадцать три и позвоню в звонок."),
                    c("Koputan lihtsalt uksele.", "Просто постучу в дверь.", correct = false, rating = -0.03,
                        followUp = listOf(client("Te ei jõua ukseni. Kasutage uksekella.", "Вы не дойдёте до двери. Воспользуйтесь домофоном."))),
                    c("Karjun teie nime.", "Крикну ваше имя.", correct = false, rating = -0.05,
                        followUp = listOf(client("Palun ärge! Lihtsalt helistage.", "Пожалуйста, не надо! Просто позвоните."))),
                ), listOf("p_uksekell", "s_uksekell")))
                val whoThere = listOf(
                    "Kes seal?" to "Кто там?",
                    "Halloo, kes räägib?" to "Алло, кто говорит?",
                    "Jah, kes see on?" to "Да, кто это?",
                    "Kuulen, kes seal?" to "Слушаю, кто там?",
                ).random()
                add(client(whoThere.first, whoThere.second))
                add(ask(Thread.KLIENT, "Клиент спрашивает по домофону, кто пришёл. Ответьте:", false, listOf(
                    c("Tere, kuller Boltist. Toon teie tellimuse.", "Здравствуйте, курьер из Bolt. Привёз ваш заказ."),
                    c("Ma ei tea.", "Я не знаю.", correct = false, rating = -0.05,
                        followUp = listOf(client("Mida? Kes te olete?", "Что? Кто вы?"))),
                    c("Avage lihtsalt uks!", "Просто откройте дверь!", correct = false, rating = -0.05,
                        followUp = listOf(client("Öelge enne, kes te olete.", "Сначала скажите, кто вы."))),
                ), listOf("p_jargi", "p_tellimus", "g_tere")))
                add(client("Selge, kuller! Avan ukse. Tulge teisele korrusele.", "Понятно, курьер! Открываю дверь. Поднимитесь на второй этаж."))
                addAll(handover())
            }, Nav.End)
        )

        // ----- Грубый клиент: угрожает одной звездой, нужна деэскалация -----
        Scenario.ANGRY_CLIENT -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
                add(client("Lõpuks ometi! Te olete liiga aeglane. Ma annan nagunii ühe tärni!", "Наконец-то! Вы слишком медленный. Я всё равно поставлю одну звезду!"))
                add(ask(Thread.KLIENT, "Клиент злится и грозит одной звездой. Ответьте спокойно:", false, listOf(
                    c("Vabandust, et pidite ootama. Siin on teie tellimus, toit on veel kuum.", "Извините, что пришлось ждать. Вот ваш заказ, еда ещё горячая."),
                    c("Mina ei ole süüdi, restoran oli aeglane.", "Я не виноват, ресторан был медленный.", correct = false, rating = -0.1,
                        followUp = listOf(client("Mind ei huvita, kes on süüdi! Toidu tõite teie.", "Мне всё равно, кто виноват! Еду привезли вы."))),
                    c("Kui annate ühe tärni, ei too ma teile enam kunagi toitu.", "Если поставите одну звезду, я вам больше никогда ничего не привезу.", correct = false, rating = -0.2,
                        followUp = listOf(client("Kas te ähvardate mind?! Nüüd kirjutan ka kaebuse.", "Вы мне угрожаете?! Теперь ещё и жалобу напишу."))),
                ), listOf("g_vabandust", "p_tellimus")))
                add(client("Hmm... olgu. Aga ma olen ikka pahane.", "Хм... ладно. Но я всё ещё сердит."))
                add(ask(Thread.KLIENT, "Постарайтесь сгладить и завершить передачу:", false, listOf(
                    c("Ma saan aru. Klienditugi saab teid aidata. Palun, head isu!", "Я понимаю. Поддержка сможет вам помочь. Пожалуйста, приятного аппетита!"),
                    c("Tärnid ei ole tähtsad.", "Звёзды не важны.", correct = false, rating = -0.05,
                        followUp = listOf(client("Minu arvamus on tähtis!", "Моё мнение важно!"))),
                    c("Palun pange viis tärni, see on minu töö.", "Пожалуйста, поставьте пять звёзд, это моя работа.", correct = false, rating = -0.05,
                        followUp = listOf(client("Hinde panen mina ise. Ärge küsige tärne.", "Оценку ставлю я сам. Не выпрашивайте звёзды."))),
                ), listOf("p_tugi_q", "p_head_isu")))
                add(client("Olgu. Aitäh toidu eest.", "Ладно. Спасибо за еду."))
            }, Nav.Choose("Клиент остался недоволен. Что дальше?", listOf(
                NavOption("🎧 Сообщить в поддержку", "support_rude"),
                NavOption("🙇 Ещё раз извиниться и завершить", "client_apo"),
            ))),
            Phase("support_rude", Thread.TUGI, buildList {
                add(support("Tere! Siin klienditugi. Kuidas saan aidata?", "Здравствуйте! Это служба поддержки. Чем могу помочь?"))
                add(ask(Thread.TUGI, "Сообщите об инциденте:", true, listOf(
                    c("Klient on väga vihane ja ähvardab ühe tärniga. Tellimuse andsin üle.", "Клиент очень зол и грозит одной звездой. Заказ я передал.",
                        followUp = listOf(support("Aitäh, et teatasite. Märgime selle üles, teie reiting on kaitstud.", "Спасибо, что сообщили. Зафиксируем это, ваш рейтинг защищён."))),
                    c("Klient on loll, tehke midagi.", "Клиент дурак, сделайте что-нибудь.", correct = false, rating = -0.1,
                        followUp = listOf(support("Palun rääkige viisakalt. Kirjeldage probleemi rahulikult.", "Пожалуйста, говорите вежливо. Опишите проблему спокойно."))),
                    c("Pole midagi, unustage.", "Ничего, забудьте.", correct = false, rating = -0.03,
                        followUp = listOf(support("Parem teatage alati. Siis saame teid kaitsta.", "Лучше всегда сообщайте. Тогда мы сможем вас защитить."))),
                ), listOf("p_tugi_q")))
                add(support("Head tööd! Te käitusite õigesti.", "Хорошей работы! Вы поступили правильно."))
            }, Nav.End),
            apologyPhase(),
        )

        // ----- Отказ платить (наличные) — эскалация в поддержку -----
        Scenario.REFUSE_PAY -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
                add(client("Tere! Aa, see on sularahas? Mul ei ole raha. Ma ei maksa.", "Здравствуйте! А, это наличными? У меня нет денег. Я не буду платить."))
                add(ask(Thread.KLIENT, "Заказ наличными, 10 €. Клиент отказывается платить:", false, listOf(
                    c("Tellimus maksab kümme eurot. Kas saate maksta kaardiga?", "Заказ стоит десять евро. Можете заплатить картой?",
                        followUp = listOf(client("Ei. Ma ei maksa üldse.", "Нет. Я вообще не буду платить."))),
                    c("Olgu, võtke toit niisama.", "Ладно, возьмите еду просто так.", correct = false, rating = -0.15,
                        followUp = listOf(
                            client("Tõesti? Suur aitäh!", "Правда? Большое спасибо!"),
                            client("(Raha jääb saamata — see on teie probleem.)", "(Деньги не получены — это ваша проблема.)"))),
                    c("Makske kohe või toitu ei tule!", "Платите немедленно, или еды не будет!", correct = false, rating = -0.1,
                        followUp = listOf(client("Ärge karjuge mu peale!", "Не кричите на меня!"))),
                ), listOf("m_maksab", "n_10", "m_eurot", "m_kaart")))
                add(client("Ma ei maksa. Tehke, mis tahate.", "Я не заплачу. Делайте что хотите."))
                add(ask(Thread.KLIENT, "Клиент твёрдо отказывается. Что сделать?", true, listOf(
                    c("Selge. Ma pöördun klienditoe poole.", "Понятно. Я обращусь в поддержку.",
                        followUp = listOf(client("Nagu soovite.", "Как хотите."))),
                    c("Ma kutsun politsei!", "Я вызову полицию!", correct = false, rating = -0.1,
                        followUp = listOf(client("Politsei? Kümne euro pärast? Olge tõsine.", "Полицию? Из-за десяти евро? Будьте серьёзнее."))),
                    c("Ma ootan siin, kuni te maksate.", "Я подожду здесь, пока вы заплатите.", correct = false, rating = -0.05,
                        followUp = listOf(client("Siis ootate väga kaua.", "Тогда вы будете ждать очень долго."))),
                ), listOf("p_tugi_q", "m_sularaha")))
            }, Nav.Choose("Клиент не платит. Что дальше?", listOf(
                NavOption("🎧 Связаться с поддержкой", "support_pay"),
            ))),
            Phase("support_pay", Thread.TUGI, buildList {
                add(support("Klienditugi, tere. Mis mure on?", "Поддержка, здравствуйте. Что случилось?"))
                add(ask(Thread.TUGI, "Опишите ситуацию поддержке:", true, listOf(
                    c("Klient ei taha maksta. Tellimus on sularahas, kümme eurot.", "Клиент не хочет платить. Заказ наличными, десять евро.",
                        followUp = listOf(support("Selge. Ärge andke toitu üle. Tühistame tellimuse, teie tasu jääb alles.", "Понятно. Не отдавайте еду. Отменим заказ, ваша оплата сохранится."))),
                    c("Ma andsin toidu juba ära.", "Я уже отдал еду.", correct = false, rating = -0.15,
                        followUp = listOf(support("Sularahatellimusel küsige raha alati enne. Seekord katame kahju, aga pidage meeles.", "При заказе наличными всегда берите деньги до передачи. В этот раз покроем убыток, но запомните."))),
                    c("Kõik on korras, ei midagi.", "Всё в порядке, ничего.", correct = false, rating = -0.05,
                        followUp = listOf(support("Kindel? Süsteemis makse puudub. Rääkige ausalt.", "Уверены? В системе нет оплаты. Говорите честно."))),
                ), listOf("p_tugi_q", "m_sularaha")))
                add(support("Võtke toit kaasa ja jätkake tööd. Aitäh, et teatasite!", "Заберите еду с собой и продолжайте работу. Спасибо, что сообщили!"))
            }, Nav.End),
        )

        // ----- Пьяный клиент у двери -----
        Scenario.DRUNK_CLIENT -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
                add(client("Ooo, sõber! Tule sisse, joome koos!", "Ооо, друг! Заходи, выпьем вместе!"))
                add(ask(Thread.KLIENT, "Клиент нетрезв и зовёт внутрь. Откажитесь вежливо, останьтесь снаружи:", false, listOf(
                    c("Aitäh, aga ma olen tööl. Palun, siin on teie tellimus.", "Спасибо, но я на работе. Пожалуйста, вот ваш заказ."),
                    c("Olgu, aga ainult üks klaas.", "Ладно, но только один стакан.", correct = false, rating = -0.2,
                        followUp = listOf(
                            client("Super, tule edasi!", "Супер, проходи!"),
                            client("(Järgmised tellimused hilinevad. Töö ajal külla minna ei tohi.)", "(Следующие заказы опоздают. Во время работы заходить в гости нельзя.)"))),
                    c("Te olete purjus. Häbi!", "Вы пьяны. Стыдно!", correct = false, rating = -0.1,
                        followUp = listOf(client("Mida sa ütlesid?! Mine minema!", "Что ты сказал?! Уходи!"))),
                ), listOf("g_aitah", "p_tellimus")))
                add(client("Oota... kas ma üldse tellisin midagi? Ma ei mäleta.", "Погоди... я вообще что-то заказывал? Не помню."))
                add(ask(Thread.KLIENT, "Клиент не помнит заказ. Спокойно подтвердите по приложению:", false, listOf(
                    c("Jah, see on teie tellimus. Äpis on teie nimi ja see aadress.", "Да, это ваш заказ. В приложении ваше имя и этот адрес.",
                        followUp = listOf(client("Ahaa... jah, see olen mina!", "Ага... да, это я!"))),
                    c("Pole vahet, lihtsalt võtke.", "Без разницы, просто берите.", correct = false, rating = -0.1,
                        followUp = listOf(client("Aga äkki see ei ole minu oma?", "А вдруг это не моё?"))),
                    c("Kui te ei mäleta, viin toidu tagasi.", "Если не помните, увезу еду обратно.", correct = false, rating = -0.1,
                        followUp = listOf(client("Ei-ei, oota! Las ma mõtlen...", "Нет-нет, погоди! Дай подумаю..."))),
                ), listOf("p_nimi_q", "p_oige_q", "g_jah")))
                add(client("Jah, õige, minu tellimus! Aitäh, sõber!", "Да, точно, мой заказ! Спасибо, друг!"))
                addAll(handover())
            }, Nav.End)
        )

        // ----- Собака во дворе -----
        Scenario.DOG_YARD -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
                add(client("Tere! Värav on lahti, tulge hoovi. Uks on maja taga.", "Здравствуйте! Ворота открыты, заходите во двор. Дверь за домом."))
                add(ask(Thread.KLIENT, "Во дворе большая собака без привязи. Напишите клиенту:", true, listOf(
                    c("Teie hoovis on suur koer. Kas saate koera kinni hoida, palun?", "У вас во дворе большая собака. Можете подержать собаку, пожалуйста?"),
                    c("Pole hullu, ma lähen ikka sisse.", "Ничего, я всё равно зайду.", correct = false, rating = -0.15,
                        followUp = listOf(
                            client("Ärge minge! Ta võib hammustada!", "Не заходите! Она может укусить!"),
                            client("Oodake värava juures, ma tulen.", "Подождите у ворот, я подойду."))),
                    c("Ma viskan koti üle aia.", "Я переброшу пакет через забор.", correct = false, rating = -0.1,
                        followUp = listOf(client("Palun ärge! Kott läheb katki ja koer sööb toidu ära.", "Пожалуйста, не надо! Пакет порвётся, и собака съест еду."))),
                ), listOf("g_palun", "s_maja", "s_uks")))
                add(client("Oi, vabandust! Ma panen koera ketti ja tulen ise värava juurde.", "Ой, извините! Посажу собаку на цепь и сам подойду к воротам."))
                add(ask(Thread.KLIENT, "Согласуйте передачу у ворот:", true, listOf(
                    c("Aitäh! Ma ootan teid värava juures.", "Спасибо! Я подожду вас у ворот."),
                    c("Kiiremini, palun, mul on kiire.", "Побыстрее, пожалуйста, я тороплюсь.", correct = false, rating = -0.05,
                        followUp = listOf(client("Üks hetk! Koer on suur, see võtab aega.", "Минутку! Собака большая, это занимает время."))),
                    c("Jätan toidu värava taha ja lähen.", "Оставлю еду за воротами и уеду.", correct = false, rating = -0.1,
                        followUp = listOf(client("Ei, palun oodake! Muidu sööb koer selle ära.", "Нет, пожалуйста, подождите! Иначе собака её съест."))),
                ), listOf("g_aitah", "p_ootan")))
                add(client("Olen kohal, koer on toas. Aitäh, et ootasite!", "Я на месте, собака дома. Спасибо, что подождали!"))
                addAll(handover())
            }, Nav.End)
        )

        // ----- Ресторан собрал не то и отказывается менять — эскалация -----
        Scenario.WRONG_ITEMS -> listOf(
            Phase("client", Thread.KLIENT, buildList {
                add(clientArrived())
                add(client("Tere! ... Oot, see on vale toit! Mina tellisin ${o.itemsEt}, aga kotis on midagi muud.", "Здравствуйте! ...Стоп, это не та еда! Я заказывал ${o.itemsRu}, а в пакете что-то другое."))
                add(ask(Thread.KLIENT, "Клиенту привезли не те блюда. Ответьте:", false, listOf(
                    c("Vabandust! Kontrollin kohe äpist. Kott oli kinni, ma ei näinud sisse.", "Извините! Сейчас проверю в приложении. Пакет был закрыт, я не видел внутрь."),
                    c("Te ise tellisite valesti.", "Вы сами заказали неправильно.", correct = false, rating = -0.15,
                        followUp = listOf(client("Ei tellinud! Vaadake ise äppi!", "Не заказывал! Сами посмотрите в приложение!"))),
                    c("Toit on toit. Sööge seda.", "Еда есть еда. Ешьте это.", correct = false, rating = -0.2,
                        followUp = listOf(client("Mis?! Ma annan ühe tärni ja kirjutan kaebuse!", "Что?! Поставлю одну звезду и напишу жалобу!"))),
                ), listOf("g_vabandust", "p_oige_q")))
                add(client("Palun lahendage see ära. Ma ootan.", "Пожалуйста, решите это. Я жду."))
            }, Nav.Choose("Не те блюда. Что дальше?", listOf(
                NavOption("🏪 Написать ресторану", "rest_wrong"),
                NavOption("🎧 Связаться с поддержкой", "support_wrong"),
            ))),
            Phase("rest_wrong", Thread.RESTORAN, buildList {
                add(ask(Thread.RESTORAN, "Напишите ресторану о неверном заказе:", true, listOf(
                    c("Tere! Klient sai vale tellimuse. Kas saate õige toidu valmis teha?", "Здравствуйте! Клиент получил не тот заказ. Можете приготовить правильную еду?"),
                    c("Te tegite kõik valesti! See on häbi!", "Вы всё сделали неправильно! Это позор!", correct = false, rating = -0.1,
                        followUp = listOf(rest("Palun rääkige viisakalt, siis saame aidata.", "Пожалуйста, говорите вежливо, тогда сможем помочь."))),
                    c("Ma toon toidu tagasi ja võtan ise uue.", "Я привезу еду назад и сам возьму новую.", correct = false, rating = -0.05,
                        followUp = listOf(rest("Oodake! Ilma klienditoe loata me ei vaheta midagi.", "Подождите! Без разрешения поддержки мы ничего не меняем."))),
                ), listOf("p_oige_q", "p_valmis_q")))
                add(rest("Meie andsime õige koti. Me ei saa midagi teha. Pöörduge klienditoe poole.", "Мы выдали правильный пакет. Мы ничего не можем сделать. Обратитесь в поддержку."))
            }, Nav.Choose("Ресторан отказывается помочь. Что дальше?", listOf(
                NavOption("🎧 Связаться с поддержкой", "support_wrong"),
            ))),
            Phase("support_wrong", Thread.TUGI, buildList {
                add(support("Tere! Siin klienditugi. Kuidas saan aidata?", "Здравствуйте! Это служба поддержки. Чем могу помочь?"))
                add(ask(Thread.TUGI, "Опишите проблему поддержке:", true, listOf(
                    c("Klient sai vale toidu. Restoran ei aita. Palun lahendust.", "Клиент получил не ту еду. Ресторан не помогает. Прошу решения.",
                        followUp = listOf(support("Selge. Teeme kliendile tagasimakse ja vormistame uue tellimuse.", "Понятно. Сделаем клиенту возврат и оформим новый заказ."))),
                    c("See on restorani süü, mitte minu!", "Это вина ресторана, не моя!", correct = false, rating = -0.05,
                        followUp = listOf(support("Me ei otsi süüdlast. Kirjeldage lihtsalt probleemi.", "Мы не ищем виноватых. Просто опишите проблему."))),
                    c("Klient vist valetab.", "Клиент, наверное, врёт.", correct = false, rating = -0.1,
                        followUp = listOf(support("Meil on tellimusest foto. Klient ei valeta.", "У нас есть фото заказа. Клиент не врёт."))),
                ), listOf("p_tugi_q", "p_oige_q")))
                add(support("Kliendile tuleb tagasimakse. Öelge talle ka. Aitäh abi eest!", "Клиенту будет возврат. Сообщите ему тоже. Спасибо за помощь!"))
            }, Nav.Choose("Сообщите клиенту решение", listOf(
                NavOption("📲 Вернуться к клиенту", "client_wrong_end"),
            ))),
            Phase("client_wrong_end", Thread.KLIENT, buildList {
                add(ask(Thread.KLIENT, "Передайте клиенту решение поддержки:", true, listOf(
                    c("Klienditugi teeb tagasimakse ja uue tellimuse. Vabandust veel kord!", "Поддержка сделает возврат и новый заказ. Ещё раз извините!"),
                    c("Kõik on korras, ärge muretsege.", "Всё в порядке, не волнуйтесь.", correct = false, rating = -0.05,
                        followUp = listOf(client("Mis täpselt on korras? Öelge konkreetselt.", "Что именно в порядке? Скажите конкретно."))),
                    c("Ma ei tea, mis edasi saab.", "Я не знаю, что будет дальше.", correct = false, rating = -0.05,
                        followUp = listOf(client("Kuidas ei tea? Te ju rääkisite klienditoega!", "Как не знаете? Вы же говорили с поддержкой!"))),
                ), listOf("g_vabandust", "g_aitah")))
                add(client("Aitäh abi eest! Te olete tubli kuller.", "Спасибо за помощь! Вы молодец-курьер."))
            }, Nav.End),
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
