import 'dart:math';

import 'models.dart';

/// Сборщик диалога-доставки как ГРАФА ФАЗ.
///
/// Каждая фаза — разговор в ОДНОМ чате; внутри фазы вопросы ветвятся (любой
/// вариант рабочий и ведёт к логичному завершению фазы). После фазы курьер САМ
/// выбирает, что делать дальше ([NavChoose]) — написать клиенту или в поддержку.
class DialogueFactory {
  DialogueFactory._();

  static final _rnd = Random();
  static T _pick<T>(List<T> list) => list[_rnd.nextInt(list.length)];

  static Say _rest(String et, String ru) => Say(ChatThread.restoran, et, ru);
  static Say _client(String et, String ru) => Say(ChatThread.klient, et, ru);
  static Say _support(String et, String ru) => Say(ChatThread.tugi, et, ru);

  static Choice _c(String et, String ru,
          {bool correct = true,
          double rating = 0.0,
          List<Turn> followUp = const []}) =>
      Choice(et, ru, correct: correct, followUp: followUp, ratingDelta: rating);

  static Ask _ask(ChatThread thread, String prompt, bool courier,
          List<Choice> choices,
          [List<String> teach = const []]) =>
      Ask(thread, prompt, courier, choices, teach);

  /// Вариант объяснения дороги клиентом + подтверждения курьера:
  /// (say-et, say-ru, ok-et, ok-ru, bad-et, bad-ru, teach)
  static const List<(String, String, String, String, String, String, List<String>)>
      _dirVariants = [
    ('Minge otse, siis paremale. Kollane maja.', 'Идите прямо, потом направо. Жёлтый дом.',
     'Selge: otse ja siis paremale.', 'Понятно: прямо, потом направо.',
     'Selge: tagasi ja vasakule.', 'Понятно: назад и налево.',
     ['s_otse', 's_paremale', 's_maja']),
    ('Pöörake vasakule, sinine maja hoovis.', 'Поверните налево, синий дом во дворе.',
     'Selge: vasakule, maja hoovis.', 'Понятно: налево, дом во дворе.',
     'Selge: paremale, esimene maja.', 'Понятно: направо, первый дом.',
     ['s_vasakule', 's_maja']),
    ('Sissepääs on tagant, hoovi poolt.', 'Вход сзади, со стороны двора.',
     'Selge: sissepääs hoovi poolt.', 'Понятно: вход со стороны двора.',
     'Selge: sissepääs tänava poolt.', 'Понятно: вход со стороны улицы.',
     ['s_uks', 's_maja']),
    ('Minge keldrist sisse, kõige alumine uks.', 'Заходите через подвал, самая нижняя дверь.',
     'Selge: kelder, alumine uks.', 'Понятно: подвал, нижняя дверь.',
     'Selge: ülemine korrus, lift.', 'Понятно: верхний этаж, лифт.',
     ['s_uks', 's_korrus']),
    ('Roheline maja, teine sissepääs.', 'Зелёный дом, второй вход.',
     'Selge: roheline maja, teine uks.', 'Понятно: зелёный дом, вторая дверь.',
     'Selge: punane maja, esimene uks.', 'Понятно: красный дом, первая дверь.',
     ['s_maja', 's_uks']),
    ('Sõitke otse lõpuni, siis paremale. Punane maja.', 'Прямо до конца, потом направо. Красный дом.',
     'Selge: otse lõpuni ja paremale.', 'Понятно: прямо до конца и направо.',
     'Selge: kohe vasakule.', 'Понятно: сразу налево.',
     ['s_otse', 's_paremale', 's_maja']),
    ('Maja on tagahoovis, väravast sisse.', 'Дом в заднем дворе, входить через ворота.',
     'Selge: tagahoovi, väravast sisse.', 'Понятно: в задний двор, через ворота.',
     'Selge: peauksest, esiküljelt.', 'Понятно: через парадную, спереди.',
     ['s_maja', 's_uks']),
    ('Esimene maja vasakul, valge uks.', 'Первый дом слева, белая дверь.',
     'Selge: esimene maja vasakul, valge uks.', 'Понятно: первый дом слева, белая дверь.',
     'Selge: viimane maja paremal.', 'Понятно: последний дом справа.',
     ['s_vasakule', 's_maja', 's_uks']),
    ('Minge läbi värava, siis trepist üles.', 'Идите через ворота, потом вверх по лестнице.',
     'Selge: värava kaudu, trepist üles.', 'Понятно: через ворота, вверх по лестнице.',
     'Selge: liftiga alla keldrisse.', 'Понятно: на лифте вниз в подвал.',
     ['s_uks', 's_lift']),
    ('Kollane maja, sissepääs küljelt.', 'Жёлтый дом, вход сбоку.',
     'Selge: kollane maja, küljelt sisse.', 'Понятно: жёлтый дом, заходить сбоку.',
     'Selge: otse peauksest.', 'Понятно: прямо через парадную.',
     ['s_maja', 's_uks']),
  ];

  /// 14 вариантов, где именно в здании находится клиент (офис).
  static const List<(String, String)> _officeLocations = [
    ('Kabinet kakskümmend üks, vastuvõtu juures.', 'Кабинет 21, у ресепшена.'),
    ('Kolmas korrus, kabinet seitse, koridori lõpus.', 'Третий этаж, кабинет 7, в конце коридора.'),
    ('Teine korrus, esimene uks vasakul.', 'Второй этаж, первая дверь слева.'),
    ('Neljas korrus, klaasuksega kontor.', 'Четвёртый этаж, офис со стеклянной дверью.'),
    ('Kabinet kümme, köögi kõrval.', 'Кабинет 10, рядом с кухней.'),
    ('Viies korrus, otse liftist välja.', 'Пятый этаж, прямо из лифта.'),
    ('Kabinet kolmkümmend kaks, paremal koridoris.', 'Кабинет 32, справа по коридору.'),
    ('Teine korrus, suur avatud kontor, akna juures.', 'Второй этаж, опенспейс, у окна.'),
    ('Kuues korrus, juhi kabinet, lõpus paremal.', 'Шестой этаж, кабинет директора, в конце справа.'),
    ('Kabinet viis, kohe trepi kõrval.', 'Кабинет 5, сразу у лестницы.'),
    ('Kolmas korrus, IT-osakond, sinine uks.', 'Третий этаж, IT-отдел, синяя дверь.'),
    ('Teine korrus, raamatupidamine, viimane uks.', 'Второй этаж, бухгалтерия, последняя дверь.'),
    ('Seitsmes korrus, terrassi poolt.', 'Седьмой этаж, со стороны террасы.'),
    ('Kabinet üheksa, valvelaua taga.', 'Кабинет 9, за стойкой охраны.'),
  ];

  static const _hub = NavChoose('Заказ у вас. Что дальше?', [
    NavOption('📲 Написать клиенту', 'client'),
    NavOption('🎧 Связаться с поддержкой', 'support_opt'),
  ]);

  // ---------- общие фазы ----------
  static Turn _courierOpen() {
    final greet = _pick(const [
      ('Tere! Tulin tellimusele järele.', 'Здравствуйте! Я за заказом.'),
      ('Tervist! Olen kuller, tulin tellimusele järele.', 'Здравствуйте! Я курьер, я за заказом.'),
      ('Tere päevast! Tulin tellimusele järele.', 'Добрый день! Я за заказом.'),
      ('Tere! Kuller siin, tulin tellimusele järele.', 'Здравствуйте! Курьер, я за заказом.'),
      ('Tere! Toidukuller, tulin tellimusele järele.', 'Здравствуйте! Курьер еды, я за заказом.'),
      ('Tervist! Mul on üks tellimus välja viia.', 'Здравствуйте! У меня заказ на доставку.'),
      ('Tere päevast! Kas saan tellimuse kätte?', 'Добрый день! Можно забрать заказ?'),
      ('Tere! Olen kuller, tulin tellimust võtma.', 'Здравствуйте! Я курьер, пришёл забрать заказ.'),
      ('Tervist! Kuller siin, üks tellimus väljaviimiseks.', 'Здравствуйте! Курьер, заказ на доставку.'),
    ]);
    return _ask(
      ChatThread.restoran,
      'Зайдите в ресторан, поздоровайтесь и скажите, что вы за заказом:',
      true,
      [
        _c(greet.$1, greet.$2),
        _c('Head aega, nägemist!', 'До свидания!', correct: false, rating: -0.05,
            followUp: [_rest('Oih, te ikka tulite tellimusele järele? Üks hetk.', 'Ой, вы всё-таки за заказом? Минутку.')]),
        _c('Võtan selle koti siit, see on vist minu.', 'Возьму вот этот пакет, он вроде мой.', correct: false, rating: -0.05,
            followUp: [_rest('Oodake! See on teise kulleri tellimus. Küsige alati enne.', 'Подождите! Это заказ другого курьера. Всегда сначала спрашивайте.')]),
      ],
      ['g_tere', 'p_jargi'],
    );
  }

  static Turn _confirmAsk(Order o) => _ask(
        ChatThread.restoran,
        'Сверьте заказ и подтвердите, что всё верно:',
        false,
        [
          _c(o.confirmEt, o.confirmRu),
          _c('Ei, see on vale tellimus.', 'Нет, это неверный заказ.', correct: false, rating: -0.05,
              followUp: [_rest('Kontrollin... ei, kõik on õige.', 'Проверяю... нет, всё верно.')]),
          _c('Jah, kindlasti õige. Ma ei vaata, mul on kiire.', 'Да, точно верный. Я не смотрю, тороплюсь.', correct: false, rating: -0.05,
              followUp: [
                _rest('Palun ikka kontrollige. Kui midagi on puudu, on probleem teie oma.', 'Пожалуйста, всё-таки проверьте. Если чего-то не хватает — проблема будет ваша.'),
                _rest('Vaatame koos: ${o.itemsEt}. Kõik on olemas.', 'Смотрим вместе: ${o.itemsRu}. Всё на месте.'),
              ]),
        ],
        o.itemTeach,
      );

  static Turn _readyTurn() =>
      _rest('Nii, valmis ongi. Palun, head teed!', 'Так, готово. Пожалуйста, счастливого пути!');

  /// Финальный ход курьера в ресторане: благодарит ПОСЛЕ получения заказа.
  static Turn _courierThanks() => _ask(
        ChatThread.restoran,
        'Заберите заказ, поблагодарите и попрощайтесь:',
        false,
        [
          _c('Aitäh! Head päeva!', 'Спасибо! Хорошего дня!'),
          _c('Lõpuks ometi!', 'Наконец-то!', correct: false, rating: -0.03,
              followUp: [_rest('Vabandust ootamise eest.', 'Извините за ожидание.')]),
          _c('Aitäh! (võtab ainult toidukoti)', 'Спасибо! (берёт только пакет с едой)', correct: false, rating: -0.03,
              followUp: [
                _rest('Oodake! Jook jäi maha, see käib ka tellimusega.', 'Подождите! Напиток остался, он тоже к заказу.'),
                _rest('Nüüd on kõik. Head teed!', 'Теперь всё. Счастливого пути!'),
              ]),
        ],
        ['g_aitah', 'g_head_aega'],
      );

  /// Получение заказа: курьер ВСЕГДА инициирует, ресторан НЕ знает заказ и
  /// уточняет (имя получателя / номер / показать в приложении). 5 вариантов.
  static List<Turn> _restScript(Order o) {
    final no = 100000 + _rnd.nextInt(900000);
    final variants = <List<Turn>>[
      // 1. Ресторан спрашивает имя получателя
      [
        _courierOpen(),
        _rest('Tere! Mis nimi on tellimusel?', 'Здравствуйте! На какое имя заказ?'),
        _ask(ChatThread.restoran, 'Назовите получателя заказа:', false, [
          _c('Tellimus on ${o.customer} nimel.', 'Заказ на имя ${o.customer}.'),
          _c('Ma ei tea nime.', 'Я не знаю имя.', correct: false, rating: -0.05,
              followUp: [_rest('Ilma nimeta ma ei leia tellimust.', 'Без имени я не найду заказ.')]),
          _c('Pole vahet, andke midagi.', 'Без разницы, дайте что-нибудь.', correct: false, rating: -0.05,
              followUp: [_rest('Ma vajan õiget nime.', 'Мне нужно правильное имя.')]),
        ], ['p_nimi_q']),
        _rest('Üks hetk... Leidsin! ${o.itemsEt}.', 'Минутку... Нашёл! ${o.itemsRu}.'),
        _confirmAsk(o),
        _readyTurn(),
        _courierThanks(),
      ],
      // 2. Ресторан спрашивает номер заказа
      [
        _courierOpen(),
        _rest('Tere! Mis on tellimuse number?', 'Здравствуйте! Какой номер заказа?'),
        _ask(ChatThread.restoran, 'Назовите номер заказа из приложения:', false, [
          _c('Tellimuse number on $no.', 'Номер заказа $no.'),
          _c('Ma ei vaadanud numbrit.', 'Я не смотрел номер.', correct: false, rating: -0.05,
              followUp: [_rest('Vaadake palun äpist.', 'Посмотрите, пожалуйста, в приложении.')]),
          _c('Number üks.', 'Номер один.', correct: false, rating: -0.05,
              followUp: [_rest('Hmm, sellist pole. Kontrollige.', 'Хм, такого нет. Проверьте.')]),
        ]),
        _rest('Number $no... Jah, siin: ${o.itemsEt}.', 'Номер $no... Да, вот: ${o.itemsRu}.'),
        _confirmAsk(o),
        _readyTurn(),
        _courierThanks(),
      ],
      // 3. Ресторан просит показать заказ в приложении
      [
        _courierOpen(),
        _rest('Tere! Näidake palun tellimust äpis.', 'Здравствуйте! Покажите, пожалуйста, заказ в приложении.'),
        _ask(ChatThread.restoran, 'Покажите заказ — назовите имя и состав:', false, [
          _c('Palun, siin: ${o.customer}, ${o.itemsEt}.', 'Пожалуйста, вот: ${o.customer}, ${o.itemsRu}.'),
          _c('Mul pole äppi.', 'У меня нет приложения.', correct: false, rating: -0.05,
              followUp: [_rest('Kuidas te siis tellimuse saite?', 'А как вы тогда получили заказ?')]),
          _c('Vaadake ise.', 'Сами смотрите.', correct: false, rating: -0.05,
              followUp: [_rest('See on teie telefonis, palun.', 'Это в вашем телефоне, пожалуйста.')]),
        ], ['p_nimi_q']),
        _rest('Aitäh! Üks hetk, toon: ${o.itemsEt}.', 'Спасибо! Минутку, несу: ${o.itemsRu}.'),
        _confirmAsk(o),
        _readyTurn(),
        _courierThanks(),
      ],
      // 4. Запара: ресторан быстро спрашивает имя
      [
        _courierOpen(),
        _rest('Tere! Meil on kiire. Kelle nimele tellimus?', 'Здравствуйте! У нас запара. На чьё имя заказ?'),
        _ask(ChatThread.restoran, 'Быстро назовите имя получателя:', false, [
          _c('${o.customer} nimele, palun.', 'На имя ${o.customer}, пожалуйста.'),
          _c('Kiirustage ise!', 'Сами поторопитесь!', correct: false, rating: -0.1,
              followUp: [_rest('Me proovime. Aga kelle nimele?', 'Мы стараемся. Но на чьё имя?')]),
          _c('Ükskõik kelle.', 'Чьё угодно.', correct: false, rating: -0.05,
              followUp: [_rest('Ei, ma vajan nime.', 'Нет, мне нужно имя.')]),
        ], ['p_nimi_q']),
        _rest('Selge! Üks hetk... Valmis: ${o.itemsEt}.', 'Понятно! Минутку... Готово: ${o.itemsRu}.'),
        _confirmAsk(o),
        _readyTurn(),
        _courierThanks(),
      ],
      // 5. Ресторан переспрашивает имя и сверяет состав
      [
        _courierOpen(),
        _rest('Tere! Kelle tellimus?', 'Здравствуйте! Чей заказ?'),
        _ask(ChatThread.restoran, 'Назовите получателя:', false, [
          _c('${o.customer}.', '${o.customer}.'),
          _c('Ma unustasin nime.', 'Я забыл имя.', correct: false, rating: -0.05,
              followUp: [_rest('Vaadake äpist nime, palun.', 'Посмотрите имя в приложении, пожалуйста.')]),
          _c('Teie peate teadma.', 'Вы должны знать.', correct: false, rating: -0.05,
              followUp: [_rest('Meil on palju tellimusi. Mis nimi?', 'У нас много заказов. Какое имя?')]),
        ], ['p_nimi_q']),
        _rest('Aa, ${o.customer}! Kas tellimus on ${o.itemsEt}?', 'А, ${o.customer}! Заказ — ${o.itemsRu}?'),
        _confirmAsk(o),
        _readyTurn(),
        _courierThanks(),
      ],
    ];
    return _pick(variants);
  }

  static Phase _restPhase(Order o) =>
      Phase('rest', ChatThread.restoran, _restScript(o), _hub);

  static Phase _supportOptPhase() => Phase(
        'support_opt',
        ChatThread.tugi,
        [
          _support('Tere! Siin klienditugi. Kuidas saan aidata?', 'Здравствуйте! Это служба поддержки. Чем могу помочь?'),
          _ask(ChatThread.tugi, 'Что напишете в поддержку?', true, [
            _c('Kõik on hästi, tahtsin lihtsalt üle küsida.', 'Всё хорошо, хотел просто уточнить.',
                followUp: [_support('Suurepärane! Head tööd.', 'Отлично! Хорошей работы.')]),
            _c('Kas aadress on kindlasti õige?', 'Адрес точно верный?',
                followUp: [_support('Jah, aadress on õige. Edu!', 'Да, адрес верный. Удачи!')]),
            _c('Ma tahan koju minna.', 'Я хочу домой.', correct: false, rating: -0.05,
                followUp: [_support('Palun lõpetage tellimus enne. Aitäh.', 'Пожалуйста, сначала завершите заказ. Спасибо.')]),
          ], ['p_tugi_q', 'g_tere']),
        ],
        const NavChoose('Что дальше?', [NavOption('📲 Написать клиенту', 'client')]),
      );

  /// Стандартная передача заказа лицом к лицу.
  static List<Turn> _handover() => [
        _ask(ChatThread.klient, 'Передайте заказ и пожелайте приятного аппетита:', false, [
          _c('Palun, siin on teie tellimus. Head isu!', 'Пожалуйста, вот ваш заказ. Приятного аппетита!'),
          _c('Võtke kiiresti, mul on järgmine tellimus ootel.', 'Берите быстрее, у меня следующий заказ ждёт.', correct: false, rating: -0.05,
              followUp: [_client('Üks hetk, palun. Pole vaja mind kiirustada.', 'Минутку, пожалуйста. Не надо меня торопить.')]),
          _c('Ei, see on minu tellimus.', 'Нет, это мой заказ.', correct: false, rating: -0.1,
              followUp: [_client('Kuidas palun?', 'Простите, что?')]),
        ], ['g_palun', 'p_head_isu']),
        _client('Suur aitäh! Ilusat päeva, nägemist!', 'Большое спасибо! Хорошего дня, до свидания!'),
      ];

  /// Опенер курьера: он первым пишет клиенту, что прибыл с заказом.
  static Turn _clientArrived() {
    final greet = _pick(const [
      ('Tere! Kuller siin, olen teie tellimusega kohal.', 'Здравствуйте! Курьер, я на месте с вашим заказом.'),
      ('Tere! Toon teie tellimuse, olen kohal.', 'Здравствуйте! Привёз ваш заказ, я на месте.'),
      ('Tervist! Olen teie ukse all teie tellimusega.', 'Здравствуйте! Я у вашей двери с заказом.'),
      ('Tere päevast! Teie toit on kohal.', 'Добрый день! Ваша еда здесь.'),
      ('Tere! Olen kohal. Kuhu tellimuse toon?', 'Здравствуйте! Я на месте. Куда принести заказ?'),
      ('Tere! Kuller siin, teie tellimus on käes.', 'Здравствуйте! Курьер, ваш заказ у меня.'),
      ('Tervist! Jõudsin kohale teie tellimusega.', 'Здравствуйте! Я доехал с вашим заказом.'),
      ('Tere! Olen maja juures, toon teie toidu.', 'Здравствуйте! Я у дома, несу вашу еду.'),
      ('Tere! Teie tellimus saabus. Olen all.', 'Здравствуйте! Ваш заказ прибыл. Я внизу.'),
      ('Tere! Kuller kohal. Kust ma teid leian?', 'Здравствуйте! Курьер на месте. Где мне вас найти?'),
    ]);
    return _ask(
      ChatThread.klient,
      'Напишите клиенту, что вы прибыли с заказом:',
      true,
      [
        _c(greet.$1, greet.$2),
        _c('Olen all. Teil on kaks minutit, siis ma lähen.', 'Я внизу. У вас две минуты, потом я уеду.', correct: false, rating: -0.05,
            followUp: [_client('Palun ärge survestage mind, ma tulen kohe.', 'Пожалуйста, не давите на меня, я сейчас выйду.')]),
        _c('Avage uks kohe!', 'Откройте дверь немедленно!', correct: false, rating: -0.05,
            followUp: [_client('Palun olge viisakas.', 'Пожалуйста, будьте вежливы.')]),
      ],
      ['g_tere', 'p_kohal', 'p_tellimus'],
    );
  }

  /// Общая фаза «извиниться и завершить» (для жалобы/протечки).
  static Phase _apologyPhase() => Phase(
        'client_apo',
        ChatThread.klient,
        [
          _ask(ChatThread.klient, 'Извинитесь напрямую и завершите:', false, [
            _c('Veel kord vabandust. Palun, teie tellimus. Head isu!', 'Ещё раз извините. Пожалуйста, ваш заказ. Приятного аппетита!'),
            _c('Pole midagi teha.', 'Ничего не поделать.', correct: false, rating: -0.05,
                followUp: [_client('Hmm.', 'Хм.')]),
          ], ['g_vabandust', 'p_head_isu']),
          _client('Olgu... aitäh.', 'Ладно... спасибо.'),
        ],
        const NavEnd(),
      );

  // ---------- фазы клиента/поддержки по сценариям ----------
  static List<Phase> _scenarioPhases(Order o) {
    switch (o.scenario) {
      case Scenario.faceDoor:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Kus te olete?', 'Здравствуйте! Где вы?'),
            _ask(ChatThread.klient, 'Ответьте, что вы на месте, и спросите, где вас найти:', false, [
              _c('Olen kohal, maja ees. Kus ma teid leian?', 'Я на месте, перед домом. Где мне вас найти?'),
              _c('Vabandust, ma eksisin ära.', 'Извините, я заблудился.', correct: false, rating: -0.05,
                  followUp: [_client('Oh ei... Ma tulen teile vastu.', 'О нет... Я выйду навстречу.')]),
              _c('Ei, mul ei ole midagi.', 'Нет, у меня ничего нет.', correct: false, rating: -0.05,
                  followUp: [_client('Kuidas? Ma tellisin toidu!', 'Как? Я заказал еду!')]),
            ], ['p_kohal', 'p_kus_q', 's_maja']),
            _client('Näen teid! Tulen kohe alla.', 'Вижу вас! Сейчас спущусь.'),
            ..._handover(),
          ], const NavEnd()),
        ];

      case Scenario.liftBroken:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Olen kolmandal korrusel. Lift on katki.', 'Здравствуйте! Я на третьем этаже. Лифт сломан.'),
            _ask(ChatThread.klient, 'Лифт сломан. Спросите, спустится клиент или вам подняться:', true, [
              _c('Kas tulete alla või tulen üles?', 'Вы спуститесь или мне подняться?'),
              _c('Kas pitsa on kuum?', 'Пицца горячая?', correct: false, rating: -0.05,
                  followUp: [_client('Ää... lihtsalt tooge palun üles.', 'Эм... просто принесите наверх.')]),
              _c('Ma ootan all igatahes.', 'Я в любом случае жду внизу.', correct: false, rating: -0.03,
                  followUp: [_client('Olgu, ma tulen siis ise alla.', 'Ладно, тогда я сам спущусь.')]),
            ], ['p_alla_voi_q', 's_korrus', 's_lift']),
            _client('Tulen ise alla, üks minut!', 'Сейчас сам спущусь, минутку!'),
            ..._handover(),
          ], const NavEnd()),
        ];

      case Scenario.gateCode:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Värav on lukus, korter üheksa.', 'Здравствуйте! Ворота заперты, квартира девять.'),
            _ask(ChatThread.klient, 'Ворота заперты — спросите код двери:', true, [
              _c('Mis on uksekood?', 'Какой код двери?'),
              _c('Värav on lukus. Tulge ise alla, ma ei otsi koodi.', 'Ворота заперты. Спуститесь сами, я не буду искать код.', correct: false, rating: -0.05,
                  followUp: [_client('Mul on väike laps kodus, ma ei saa tulla. Kood on olemas, küsige.', 'У меня дома маленький ребёнок, я не могу выйти. Код есть, спросите.')]),
              _c('Ma jätan toidu lihtsalt siia.', 'Я просто оставлю еду здесь.', correct: false, rating: -0.05,
                  followUp: [_client('Ei-ei, palun tooge ukse juurde!', 'Нет-нет, принесите к двери!')]),
            ], ['p_kood_q', 'n_9', 's_korter']),
            _client('Kood on üks-kaks-kolm-neli.', 'Код — один-два-три-четыре.'),
            ..._handover(),
          ], const NavEnd()),
        ];

      case Scenario.directions:
        final dir = _pick(_dirVariants);
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Meie maja on raske leida.', 'Здравствуйте! Наш дом трудно найти.'),
            _ask(ChatThread.klient, 'Спросите у клиента, как к нему пройти:', true, [
              _c('Kuidas ma teie juurde saan?', 'Как мне к вам пройти?'),
              _c('Pole vaja seletada, ma leian GPS-iga.', 'Не надо объяснять, найду по GPS.', correct: false, rating: -0.03,
                  followUp: [_client('GPS näitab siin valesti. Kuulake parem mind.', 'GPS здесь показывает неправильно. Лучше послушайте меня.')]),
              _c('Leidke ise mind.', 'Найдите меня сами.', correct: false, rating: -0.05,
                  followUp: [_client('See on teie töö, mitte minu.', 'Это ваша работа, не моя.')]),
            ], ['p_kuidas_q']),
            _client(dir.$1, dir.$2),
            _ask(ChatThread.klient, 'Подтвердите, что поняли дорогу:', false, [
              _c(dir.$3, dir.$4),
              _c(dir.$5, dir.$6, correct: false, rating: -0.05,
                  followUp: [_client('Ei-ei! Kuulake uuesti.', 'Нет-нет! Послушайте ещё раз.')]),
              _c('Ma ei saa aru.', 'Я не понимаю.', correct: false, rating: -0.03,
                  followUp: [_client(dir.$1, dir.$2)]),
            ], dir.$7),
            _client('Just nii! Ootan teid.', 'Именно! Жду вас.'),
            ..._handover(),
          ], const NavEnd()),
        ];

      case Scenario.leaveDoor:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Jätke palun toit ukse taha. Värava kood on viis-kuus-seitse-kaheksa.', 'Здравствуйте! Оставьте еду под дверью. Код ворот пять-шесть-семь-восемь.'),
            _ask(ChatThread.klient, 'Уточните номер квартиры:', true, [
              _c('Selge! Mis on korteri number?', 'Понятно! Какой номер квартиры?'),
              _c('Pööra paremale.', 'Поверни направо.', correct: false, rating: -0.05,
                  followUp: [_client('Mida? Ma räägin korterist.', 'Что? Я про квартиру.')]),
              _c('Ma ei jäta ust taha.', 'Я не оставлю под дверью.', correct: false, rating: -0.05,
                  followUp: [_client('Palun jätke, mul pole aega.', 'Пожалуйста, оставьте, у меня нет времени.')]),
            ], ['p_korter_q', 'n_5', 'n_6', 'n_7', 'n_8']),
            _client('Korter kaks. Aitäh!', 'Квартира два. Спасибо!'),
            _ask(ChatThread.klient, 'Подтвердите: оставите под дверью и сделаете фото:', false, [
              _c('Selge, jätan ukse taha ja teen foto.', 'Понятно, оставлю под дверью и сделаю фото.'),
              _c('Ei, ma võtan toidu endale.', 'Нет, я заберу еду себе.', correct: false, rating: -0.15,
                  followUp: [_client('See on vargus!', 'Это воровство!')]),
              _c('Ma helistan teile.', 'Я позвоню вам.', correct: false, rating: -0.02,
                  followUp: [_client('Pole vaja, lihtsalt jätke ukse taha.', 'Не надо, просто оставьте под дверью.')]),
            ], ['p_foto', 'p_ukse_taha']),
            _client('Suur aitäh! Ilusat päeva, nägemist!', 'Большое спасибо! Хорошего дня, до свидания!'),
          ], const NavEnd()),
        ];

      case Scenario.cash:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Kas te tõite mu tellimuse? Kui palju ma maksma pean?', 'Здравствуйте! Вы привезли мой заказ? Сколько мне платить?'),
            _ask(ChatThread.klient, 'С клиента 10 €. Назовите сумму:', false, [
              _c('Kümme eurot, palun.', 'Десять евро, пожалуйста.'),
              _c('Kaksteist eurot, palun.', 'Двенадцать евро, пожалуйста.', correct: false, rating: -0.05,
                  followUp: [
                    _client('Äpis on kümme eurot. Palun kontrollige oma äpist.', 'В приложении десять евро. Проверьте, пожалуйста, в своём приложении.'),
                    _client('Näete? Kümme.', 'Видите? Десять.'),
                  ]),
              _c('Ma ei tea.', 'Я не знаю.', correct: false, rating: -0.05,
                  followUp: [_client('Vaadake äpist, palun.', 'Посмотрите в приложении, пожалуйста.')]),
            ], ['m_maksab', 'm_eurot', 'n_10']),
            _client('Selge. Kas saan kaardiga maksta?', 'Понятно. Можно картой?'),
            _ask(ChatThread.klient, 'Ответьте про способ оплаты:', false, [
              _c('Jah, või sularahas — kuidas soovite.', 'Да, или наличными — как хотите.'),
              _c('Ainult sularaha.', 'Только наличные.', correct: false, rating: -0.03,
                  followUp: [_client('Hästi, mul on sularaha.', 'Ладно, у меня есть наличные.')]),
              _c('Ma ei võta raha.', 'Я не беру деньги.', correct: false, rating: -0.05,
                  followUp: [_client('Aga tellimus on tasuline...', 'Но заказ платный...')]),
            ], ['p_maksate_q', 'm_sularaha', 'm_kaart']),
            _client('Maksan kaardiga. Aitäh!', 'Плачу картой. Спасибо!'),
            ..._handover(),
          ], const NavEnd()),
        ];

      case Scenario.wrongAddress:
        return [
          Phase('client', ChatThread.klient, [
            _ask(ChatThread.klient, 'Напишите клиенту, что вы прибыли по адресу:', true, [
              _c('Tere! Kuller siin, olen teie aadressil.', 'Здравствуйте! Курьер, я по вашему адресу.'),
              _c('Olen kohal, aga teid pole.', 'Я на месте, но вас нет.', correct: false, rating: -0.05,
                  followUp: [_client('Imelik... kus te olete?', 'Странно... где вы?')]),
              _c('Te andsite vale aadressi.', 'Вы дали неверный адрес.', correct: false, rating: -0.05,
                  followUp: [_client('Kontrollime koos, palun.', 'Давайте проверим вместе.')]),
            ], ['g_tere', 'p_jargi', 'p_kohal']),
            _client('Halloo?', 'Алло?'),
            _ask(ChatThread.klient, 'Уточните, верный ли адрес:', true, [
              _c('Tere! Kas see aadress on õige?', 'Здравствуйте! Этот адрес верный?'),
              _c('Kas teile maitseb karri?', 'Вам нравится карри?', correct: false, rating: -0.05,
                  followUp: [_client('Mis? Ei... kus mu toit on?', 'Что? Нет... где моя еда?')]),
              _c('Te elate vales kohas.', 'Вы живёте не там.', correct: false, rating: -0.1,
                  followUp: [_client('Vabandust?! See on minu kodu.', 'Простите?! Это мой дом.')]),
            ], ['p_aadress_q']),
            _client('Oi, ei! Õige maja on kollane, teisel pool tänavat.', 'Ой, нет! Нужный дом жёлтый, на другой стороне улицы.'),
            _ask(ChatThread.klient, 'Уточните, где именно дом:', true, [
              _c('Kus maja täpselt asub?', 'Где именно находится дом?'),
              _c('Kas sajab lund?', 'Идёт снег?', correct: false, rating: -0.05,
                  followUp: [_client('Ää... roheline uks, teisel korrusel.', 'Эм... зелёная дверь, второй этаж.')]),
              _c('Tulge ise välja.', 'Выйдите сами.', correct: false, rating: -0.05,
                  followUp: [_client('Olgu, ma tulen õue.', 'Ладно, я выйду на улицу.')]),
            ], ['p_kus_maja_q', 's_maja']),
            _client('Roheline uks, teine korrus, korter neli.', 'Зелёная дверь, второй этаж, квартира четыре.'),
            ..._handover(),
          ], const NavEnd()),
        ];

      case Scenario.notHome:
        return [
          Phase('client', ChatThread.klient, [
            _ask(ChatThread.klient, 'Напишите клиенту, что вы прибыли, и позвоните:', true, [
              _c('Tere! Olen kohal teie tellimusega, helistan teile.', 'Здравствуйте! Я на месте с заказом, звоню вам.'),
              _c('Kus te olete?! Ma lahkun.', 'Где вы?! Я ухожу.', correct: false, rating: -0.05,
                  followUp: [_client('(Telefon ei vasta...)', '(Телефон не отвечает...)')]),
              _c('Ma ei viitsi oodata.', 'Мне неохота ждать.', correct: false, rating: -0.05,
                  followUp: [_client('(Telefon ei vasta...)', '(Телефон не отвечает...)')]),
            ], ['g_tere', 'p_kohal', 'p_tellimus']),
            _client('(Telefon ei vasta...)', '(Телефон не отвечает...)'),
            _ask(ChatThread.klient, 'Клиент не отвечает. Что сделать?', true, [
              _c('Ma helistan teile veel kord.', 'Я позвоню вам ещё раз.',
                  followUp: [_client('Vabandust, ma ei kuulnud! Olen kohe ukse juures.', 'Извините, я не слышал! Сейчас буду у двери.')]),
              _c('Söön toidu ise ära.', 'Съем еду сам.', correct: false, rating: -0.15,
                  followUp: [_client('Halloo! Ärge sööge mu toitu!', 'Алло! Не ешьте мою еду!')]),
              _c('Sõidan kohe koju.', 'Сразу поеду домой.', correct: false, rating: -0.1,
                  followUp: [_client('Oodake! Ma olen kodus!', 'Подождите! Я дома!')]),
            ], ['p_helistan']),
            _ask(ChatThread.klient, 'Спросите, оставить ли под дверью:', true, [
              _c('Kas jätan toidu ukse taha?', 'Оставить еду под дверью?'),
              _c('Jätan ukse taha ja lähen kohe ära.', 'Оставлю под дверью и сразу уеду.', correct: false, rating: -0.05,
                  followUp: [_client('Palun tehke enne foto! Muidu ma ei leia tellimust üles.', 'Пожалуйста, сначала сделайте фото! Иначе я не найду заказ.')]),
              _c('Ma ootan tund aega.', 'Я подожду час.', correct: false, rating: -0.03,
                  followUp: [_client('Pole vaja! Jätke ukse taha.', 'Не надо! Оставьте под дверью.')]),
            ], ['p_ukse_taha', 's_uks']),
            _client('Jah, jätke ukse taha, palun. Aitäh!', 'Да, оставьте под дверью, пожалуйста. Спасибо!'),
          ], const NavEnd()),
        ];

      case Scenario.lateOrder:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Kus mu toit on? Ma ootan juba ammu.', 'Здравствуйте! Где моя еда? Я уже давно жду.'),
            _ask(ChatThread.klient, 'Вы опоздали (была пробка). Извинитесь:', true, [
              _c('Vabandust hilinemise pärast, olin ummikus.', 'Извините за опоздание, я был в пробке.'),
              _c('See pole minu süü.', 'Это не моя вина.', correct: false, rating: -0.1,
                  followUp: [_client('Ikkagi ebameeldiv.', 'Всё равно неприятно.')]),
              _c('Teie toit on otsas.', 'Ваша еда закончилась.', correct: false, rating: -0.15,
                  followUp: [_client('Mida?! Ma olen näljane!', 'Что?! Я голодный!')]),
            ], ['p_hilinen', 'g_vabandust']),
            _client('Pole hullu! Peaasi, et olete kohal.', 'Ничего страшного! Главное, что вы здесь.'),
            ..._handover(),
          ], const NavEnd()),
        ];

      case Scenario.office:
        final loc = _pick(_officeLocations);
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Tooge palun kontorisse.', 'Здравствуйте! Принесите, пожалуйста, в офис.'),
            _ask(ChatThread.klient, 'Уточните номер кабинета / где вас найти:', true, [
              _c('Selge! Mis kabinetis te olete?', 'Понятно! В каком вы кабинете?'),
              _c('Pööra paremale.', 'Поверни направо.', correct: false, rating: -0.05,
                  followUp: [_client('Ää? Ma räägin kontorist.', 'Эм? Я про офис.')]),
              _c('Tulge ise alla.', 'Спуститесь сами.', correct: false, rating: -0.05,
                  followUp: [_client('Mul on koosolek, palun tooge üles.', 'У меня встреча, принесите наверх.')]),
            ], ['p_kabinet_q', 's_korrus']),
            _client(loc.$1, loc.$2),
            ..._handover(),
          ], const NavEnd()),
        ];

      case Scenario.cancelled:
        return [
          Phase('client', ChatThread.klient, [
            _ask(ChatThread.klient, 'Напишите клиенту, что вы уже едете с заказом:', true, [
              _c('Tere! Olen teel teie tellimusega.', 'Здравствуйте! Я уже еду с вашим заказом.'),
              _c('Kus mu jootraha on?', 'Где мои чаевые?', correct: false, rating: -0.05,
                  followUp: [_client('Ää... ma pean tellimuse tühistama.', 'Эм... мне нужно отменить заказ.')]),
              _c('Ärge tülitage mind.', 'Не беспокойте меня.', correct: false, rating: -0.05,
                  followUp: [_client('Vabandust... ma tühistan tellimuse.', 'Извините... я отменяю заказ.')]),
            ], ['g_tere', 'p_tellimus', 'p_kohal']),
            _client('Vabandust! Ma pean tellimuse tühistama.', 'Извините! Мне нужно отменить заказ.'),
            _ask(ChatThread.klient, 'Клиент отменяет заказ. Ваш ответ:', false, [
              _c('Selge, pole probleemi. Head aega!', 'Понятно, без проблем. До свидания!'),
              _c('Ei, te peate maksma!', 'Нет, вы должны заплатить!', correct: false, rating: -0.15,
                  followUp: [_client('Ma tühistasin reeglite järgi!', 'Я отменил по правилам!')]),
              _c('Söön teie toidu ise ära.', 'Съем вашу еду сам.', correct: false, rating: -0.05,
                  followUp: [_client('Tehke, mis tahate...', 'Делайте что хотите...')]),
            ], ['g_head_aega', 'g_vabandust']),
            _client('Aitäh mõistmise eest!', 'Спасибо за понимание!'),
          ], const NavEnd()),
        ];

      case Scenario.complaint:
        return [
          Phase('client', ChatThread.klient, [
            _ask(ChatThread.klient, 'Напишите клиенту, что вы прибыли и передаёте заказ:', true, [
              _c('Tere! Olen kohal, siin on teie tellimus.', 'Здравствуйте! Я на месте, вот ваш заказ.'),
              _c('Kus te nii kaua olite?', 'Где вы так долго были?', correct: false, rating: -0.05,
                  followUp: [_client('Mina küsin sama!', 'Это я хочу спросить!')]),
              _c('Sööge kiiresti, muidu jahtub ära.', 'Ешьте быстрее, а то остынет.', correct: false, rating: -0.05,
                  followUp: [_client('Just nimelt — see ongi külm!', 'Вот именно — она холодная!')]),
            ], ['g_tere', 'p_kohal', 'p_tellimus']),
            _client('Tere! Aga toit on külm! Ma ootasin liiga kaua.', 'Здравствуйте! Но еда холодная! Я слишком долго ждал.'),
            _ask(ChatThread.klient, 'Клиент жалуется на холодную еду. Ваш ответ:', false, [
              _c('Vabandust! Saan aru, see on ebameeldiv. Vaatan, mida saab teha.', 'Извините! Понимаю, это неприятно. Посмотрю, что можно сделать.',
                  followUp: [_client('Olen pettunud. Mida te teete?', 'Я разочарован. Что вы сделаете?')]),
              _c('See on restorani süü, mitte minu.', 'Это вина ресторана, не моя.', correct: false, rating: -0.1,
                  followUp: [_client('Mind ei huvita, kelle süü!', 'Мне всё равно, чья вина!')]),
              _c('Pole minu probleem.', 'Не моя проблема.', correct: false, rating: -0.2,
                  followUp: [_client('Kohutav! Ma annan ühe tärni.', 'Ужасно! Поставлю одну звезду.')]),
            ], ['g_vabandust']),
          ], const NavChoose('Как поступить с жалобой?', [
            NavOption('🎧 Связаться с поддержкой', 'support_complaint'),
            NavOption('🙇 Извиниться и завершить', 'client_apo'),
          ])),
          Phase('support_complaint', ChatThread.tugi, [
            _support('Klienditugi, tere. Mis mure on?', 'Поддержка, здравствуйте. Что случилось?'),
            _ask(ChatThread.tugi, 'Опишите проблему поддержке:', true, [
              _c('Klient sai külma toidu, palun hüvitist.', 'Клиент получил холодную еду, прошу компенсацию.',
                  followUp: [_support('Selge, lisame kliendile kupongi. Tänan!', 'Понятно, добавим клиенту купон. Спасибо!')]),
              _c('Klient on lihtsalt vihane.', 'Клиент просто злой.', correct: false, rating: -0.05,
                  followUp: [_support('Palun olge professionaalne.', 'Пожалуйста, будьте профессиональны.')]),
            ], ['p_tugi_q']),
          ], const NavChoose('Что дальше?', [NavOption('📲 Вернуться к клиенту', 'client_resolved')])),
          Phase('client_resolved', ChatThread.klient, [
            _client('Sain kupongi! Aitäh, et aitasite.', 'Я получил купон! Спасибо, что помогли.'),
            ..._handover(),
          ], const NavEnd()),
          _apologyPhase(),
        ];

      case Scenario.breakdown:
        return [
          Phase('client', ChatThread.klient, [
            _ask(ChatThread.klient, 'Напишите клиенту, что вы в пути с заказом:', true, [
              _c('Tere! Olen teel teie tellimusega.', 'Здравствуйте! Я в пути с вашим заказом.'),
              _c('Kus mu toit on?', 'Где моя еда?', correct: false, rating: -0.05,
                  followUp: [_client('Seda küsin mina!', 'Это я спрашиваю!')]),
              _c('Ärge kiirustage mind.', 'Не торопите меня.', correct: false, rating: -0.05,
                  followUp: [_client('Ma lihtsalt ootan.', 'Я просто жду.')]),
            ], ['g_tere', 'p_tellimus', 'p_kohal']),
            _client('Tere! Ootan tellimust.', 'Здравствуйте! Жду заказ.'),
            _ask(ChatThread.klient, 'В пути сломался велосипед. Что напишете клиенту?', true, [
              _c('Vabandust, mu ratas läks katki. Hilinen veidi.', 'Извините, мой велосипед сломался. Немного опоздаю.',
                  followUp: [_client('Selge, ma ootan. Aitäh, et teatasite!', 'Понятно, я подожду. Спасибо, что предупредили!')]),
              _c('Ma ei saa kohale tulla.', 'Я не смогу приехать.', correct: false, rating: -0.15,
                  followUp: [_client('Mis?! Aga ma olen näljane!', 'Что?! Но я голодный!')]),
              _c('Teie probleem.', 'Ваша проблема.', correct: false, rating: -0.2,
                  followUp: [_client('Milline ebaviisakus!', 'Какая грубость!')]),
            ], ['p_hilinen', 'g_vabandust']),
          ], const NavChoose('Транспорт сломан. Что дальше?', [
            NavOption('🎧 Связаться с поддержкой', 'support_break'),
          ])),
          Phase('support_break', ChatThread.tugi, [
            _support('Klienditugi siin. Näeme, et midagi on valesti. Mis juhtus?', 'Это поддержка. Видим, что что-то не так. Что случилось?'),
            _ask(ChatThread.tugi, 'Сообщите в поддержку о поломке:', true, [
              _c('Mu sõiduk on katki, vajan abi.', 'Мой транспорт сломан, нужна помощь.',
                  followUp: [_support('Saadame teise kulleri appi. Tänan!', 'Отправим другого курьера на помощь. Спасибо!')]),
              _c('Kõik on korras.', 'Всё в порядке.', correct: false, rating: -0.05,
                  followUp: [_support('Kindel? Hoidke ühendust.', 'Уверены? Оставайтесь на связи.')]),
            ], ['p_katki']),
            _support('Teine kuller on teel kliendi juurde. Tubli töö, aitäh!', 'Другой курьер уже едет к клиенту. Хорошая работа, спасибо!'),
          ], const NavEnd()),
        ];

      case Scenario.spoiled:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Oih... kott on märg. Kas toiduga on kõik korras?', 'Здравствуйте! Ой... пакет мокрый. С едой всё в порядке?'),
            _ask(ChatThread.klient, 'Упаковка протекла в пути. Ваш ответ:', false, [
              _c('Kontrollin kohe... Vabandust, pakend lekkis.', 'Сейчас проверю... Извините, упаковка протекла.',
                  followUp: [_client('Toit on tõesti laiali. Mida te teete?', 'Еда и правда растеклась. Что вы сделаете?')]),
              _c('Kõik on korras, ärge muretsege.', 'Всё хорошо, не волнуйтесь.', correct: false, rating: -0.1,
                  followUp: [_client('Aga toit on ju laiali!', 'Но еда же растеклась!')]),
              _c('Pole minu mure.', 'Не моя забота.', correct: false, rating: -0.2,
                  followUp: [_client('Ma annan ühe tärni!', 'Я поставлю одну звезду!')]),
            ], ['p_umber_q', 'g_vabandust']),
          ], const NavChoose('Упаковка протекла. Что дальше?', [
            NavOption('🎧 Связаться с поддержкой', 'support_spoiled'),
            NavOption('🙇 Извиниться и завершить', 'client_apo'),
          ])),
          Phase('support_spoiled', ChatThread.tugi, [
            _support('Klienditugi. Kirjeldage probleemi.', 'Поддержка. Опишите проблему.'),
            _ask(ChatThread.tugi, 'Опишите проблему поддержке:', true, [
              _c('Pakend lekkis, toit on rikutud. Palun uut tellimust.', 'Упаковка протекла, еда испорчена. Прошу новый заказ.',
                  followUp: [_support('Vormistame uue tellimuse tasuta. Vabandust!', 'Оформим новый заказ бесплатно. Извините!')]),
              _c('Unustage, pole midagi.', 'Забудьте, ничего.', correct: false, rating: -0.05,
                  followUp: [_support('Kindel? Olgu.', 'Уверены? Хорошо.')]),
            ], ['p_tugi_q', 'g_vabandust']),
            _support('Uus tellimus on vormistatud. Tänan abi eest!', 'Новый заказ оформлен. Спасибо за помощь!'),
          ], const NavEnd()),
          _apologyPhase(),
        ];

      case Scenario.intercom:
        final whoThere = _pick(const [
          ('Kes seal?', 'Кто там?'),
          ('Halloo, kes räägib?', 'Алло, кто говорит?'),
          ('Jah, kes see on?', 'Да, кто это?'),
          ('Kuulen, kes seal?', 'Слушаю, кто там?'),
        ]);
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Maja uksel on fonolukk. Vajutage palun uksekella.', 'Здравствуйте! На двери домофон. Нажмите, пожалуйста, на звонок.'),
            _ask(ChatThread.klient, 'Узнайте номер квартиры, чтобы набрать на домофоне:', true, [
              _c('Mis on teie korterinumber?', 'Какой у вас номер квартиры?'),
              _c('Vajutan suvalist korterit, keegi ikka avab.', 'Нажму любую квартиру, кто-нибудь да откроет.', correct: false, rating: -0.05,
                  followUp: [_client('Palun ärge tehke nii! Naabrid pahandavad. Küsige minu korterinumbrit.', 'Пожалуйста, не делайте так! Соседи будут недовольны. Спросите мой номер квартиры.')]),
              _c('Ma jätan toidu õue.', 'Оставлю еду на улице.', correct: false, rating: -0.05,
                  followUp: [_client('Ei, palun vajutage uksekella.', 'Нет, пожалуйста, нажмите на звонок.')]),
            ], ['p_korter_q', 's_uksekell', 's_fonolukk']),
            _client('Korter kakskümmend kolm.', 'Квартира двадцать три.'),
            _ask(ChatThread.klient, 'Наберите номер квартиры на домофоне и позвоните:', false, [
              _c('Valin korteri kakskümmend kolm ja helistan uksekella.', 'Наберу квартиру двадцать три и позвоню в звонок.'),
              _c('Koputan lihtsalt uksele.', 'Просто постучу в дверь.', correct: false, rating: -0.03,
                  followUp: [_client('Te ei jõua ukseni. Kasutage uksekella.', 'Вы не дойдёте до двери. Воспользуйтесь домофоном.')]),
              _c('Karjun teie nime.', 'Крикну ваше имя.', correct: false, rating: -0.05,
                  followUp: [_client('Palun ärge! Lihtsalt helistage.', 'Пожалуйста, не надо! Просто позвоните.')]),
            ], ['p_uksekell', 's_uksekell']),
            _client(whoThere.$1, whoThere.$2),
            _ask(ChatThread.klient, 'Клиент спрашивает по домофону, кто пришёл. Ответьте:', false, [
              _c('Tere, kuller siin. Toon teie tellimuse.', 'Здравствуйте, курьер. Привёз ваш заказ.'),
              _c('Ma ei tea.', 'Я не знаю.', correct: false, rating: -0.05,
                  followUp: [_client('Mida? Kes te olete?', 'Что? Кто вы?')]),
              _c('Avage lihtsalt uks!', 'Просто откройте дверь!', correct: false, rating: -0.05,
                  followUp: [_client('Öelge enne, kes te olete.', 'Сначала скажите, кто вы.')]),
            ], ['p_jargi', 'p_tellimus', 'g_tere']),
            _client('Selge, kuller! Avan ukse. Tulge teisele korrusele.', 'Понятно, курьер! Открываю дверь. Поднимитесь на второй этаж.'),
            ..._handover(),
          ], const NavEnd()),
        ];

      // ----- Грубый клиент: угрожает одной звездой, нужна деэскалация -----
      case Scenario.angryClient:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Lõpuks ometi! Te olete liiga aeglane. Ma annan nagunii ühe tärni!', 'Наконец-то! Вы слишком медленный. Я всё равно поставлю одну звезду!'),
            _ask(ChatThread.klient, 'Клиент злится и грозит одной звездой. Ответьте спокойно:', false, [
              _c('Vabandust, et pidite ootama. Siin on teie tellimus, toit on veel kuum.', 'Извините, что пришлось ждать. Вот ваш заказ, еда ещё горячая.'),
              _c('Mina ei ole süüdi, restoran oli aeglane.', 'Я не виноват, ресторан был медленный.', correct: false, rating: -0.1,
                  followUp: [_client('Mind ei huvita, kes on süüdi! Toidu tõite teie.', 'Мне всё равно, кто виноват! Еду привезли вы.')]),
              _c('Kui annate ühe tärni, ei too ma teile enam kunagi toitu.', 'Если поставите одну звезду, я вам больше никогда ничего не привезу.', correct: false, rating: -0.2,
                  followUp: [_client('Kas te ähvardate mind?! Nüüd kirjutan ka kaebuse.', 'Вы мне угрожаете?! Теперь ещё и жалобу напишу.')]),
            ], ['g_vabandust', 'p_tellimus']),
            _client('Hmm... olgu. Aga ma olen ikka pahane.', 'Хм... ладно. Но я всё ещё сердит.'),
            _ask(ChatThread.klient, 'Постарайтесь сгладить и завершить передачу:', false, [
              _c('Ma saan aru. Klienditugi saab teid aidata. Palun, head isu!', 'Я понимаю. Поддержка сможет вам помочь. Пожалуйста, приятного аппетита!'),
              _c('Tärnid ei ole tähtsad.', 'Звёзды не важны.', correct: false, rating: -0.05,
                  followUp: [_client('Minu arvamus on tähtis!', 'Моё мнение важно!')]),
              _c('Palun pange viis tärni, see on minu töö.', 'Пожалуйста, поставьте пять звёзд, это моя работа.', correct: false, rating: -0.05,
                  followUp: [_client('Hinde panen mina ise. Ärge küsige tärne.', 'Оценку ставлю я сам. Не выпрашивайте звёзды.')]),
            ], ['p_tugi_q', 'p_head_isu']),
            _client('Olgu. Aitäh toidu eest.', 'Ладно. Спасибо за еду.'),
          ], const NavChoose('Клиент остался недоволен. Что дальше?', [
            NavOption('🎧 Сообщить в поддержку', 'support_rude'),
            NavOption('🙇 Ещё раз извиниться и завершить', 'client_apo'),
          ])),
          Phase('support_rude', ChatThread.tugi, [
            _support('Tere! Siin klienditugi. Kuidas saan aidata?', 'Здравствуйте! Это служба поддержки. Чем могу помочь?'),
            _ask(ChatThread.tugi, 'Сообщите об инциденте:', true, [
              _c('Klient on väga vihane ja ähvardab ühe tärniga. Tellimuse andsin üle.', 'Клиент очень зол и грозит одной звездой. Заказ я передал.',
                  followUp: [_support('Aitäh, et teatasite. Märgime selle üles, teie reiting on kaitstud.', 'Спасибо, что сообщили. Зафиксируем это, ваш рейтинг защищён.')]),
              _c('Klient on loll, tehke midagi.', 'Клиент дурак, сделайте что-нибудь.', correct: false, rating: -0.1,
                  followUp: [_support('Palun rääkige viisakalt. Kirjeldage probleemi rahulikult.', 'Пожалуйста, говорите вежливо. Опишите проблему спокойно.')]),
              _c('Pole midagi, unustage.', 'Ничего, забудьте.', correct: false, rating: -0.03,
                  followUp: [_support('Parem teatage alati. Siis saame teid kaitsta.', 'Лучше всегда сообщайте. Тогда мы сможем вас защитить.')]),
            ], ['p_tugi_q']),
            _support('Head tööd! Te käitusite õigesti.', 'Хорошей работы! Вы поступили правильно.'),
          ], const NavEnd()),
          _apologyPhase(),
        ];

      // ----- Отказ платить (наличные) — эскалация в поддержку -----
      case Scenario.refusePay:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Aa, see on sularahas? Mul ei ole raha. Ma ei maksa.', 'Здравствуйте! А, это наличными? У меня нет денег. Я не буду платить.'),
            _ask(ChatThread.klient, 'Заказ наличными, 10 €. Клиент отказывается платить:', false, [
              _c('Tellimus maksab kümme eurot. Kas saate maksta kaardiga?', 'Заказ стоит десять евро. Можете заплатить картой?',
                  followUp: [_client('Ei. Ma ei maksa üldse.', 'Нет. Я вообще не буду платить.')]),
              _c('Olgu, võtke toit niisama.', 'Ладно, возьмите еду просто так.', correct: false, rating: -0.15,
                  followUp: [
                    _client('Tõesti? Suur aitäh!', 'Правда? Большое спасибо!'),
                    _client('(Raha jääb saamata — see on teie probleem.)', '(Деньги не получены — это ваша проблема.)'),
                  ]),
              _c('Makske kohe või toitu ei tule!', 'Платите немедленно, или еды не будет!', correct: false, rating: -0.1,
                  followUp: [_client('Ärge karjuge mu peale!', 'Не кричите на меня!')]),
            ], ['m_maksab', 'n_10', 'm_eurot', 'm_kaart']),
            _client('Ma ei maksa. Tehke, mis tahate.', 'Я не заплачу. Делайте что хотите.'),
            _ask(ChatThread.klient, 'Клиент твёрдо отказывается. Что сделать?', true, [
              _c('Selge. Ma pöördun klienditoe poole.', 'Понятно. Я обращусь в поддержку.',
                  followUp: [_client('Nagu soovite.', 'Как хотите.')]),
              _c('Ma kutsun politsei!', 'Я вызову полицию!', correct: false, rating: -0.1,
                  followUp: [_client('Politsei? Kümne euro pärast? Olge tõsine.', 'Полицию? Из-за десяти евро? Будьте серьёзнее.')]),
              _c('Ma ootan siin, kuni te maksate.', 'Я подожду здесь, пока вы заплатите.', correct: false, rating: -0.05,
                  followUp: [_client('Siis ootate väga kaua.', 'Тогда вы будете ждать очень долго.')]),
            ], ['p_tugi_q', 'm_sularaha']),
          ], const NavChoose('Клиент не платит. Что дальше?', [
            NavOption('🎧 Связаться с поддержкой', 'support_pay'),
          ])),
          Phase('support_pay', ChatThread.tugi, [
            _support('Klienditugi, tere. Mis mure on?', 'Поддержка, здравствуйте. Что случилось?'),
            _ask(ChatThread.tugi, 'Опишите ситуацию поддержке:', true, [
              _c('Klient ei taha maksta. Tellimus on sularahas, kümme eurot.', 'Клиент не хочет платить. Заказ наличными, десять евро.',
                  followUp: [_support('Selge. Ärge andke toitu üle. Tühistame tellimuse, teie tasu jääb alles.', 'Понятно. Не отдавайте еду. Отменим заказ, ваша оплата сохранится.')]),
              _c('Ma andsin toidu juba ära.', 'Я уже отдал еду.', correct: false, rating: -0.15,
                  followUp: [_support('Sularahatellimusel küsige raha alati enne. Seekord katame kahju, aga pidage meeles.', 'При заказе наличными всегда берите деньги до передачи. В этот раз покроем убыток, но запомните.')]),
              _c('Kõik on korras, ei midagi.', 'Всё в порядке, ничего.', correct: false, rating: -0.05,
                  followUp: [_support('Kindel? Süsteemis makse puudub. Rääkige ausalt.', 'Уверены? В системе нет оплаты. Говорите честно.')]),
            ], ['p_tugi_q', 'm_sularaha']),
            _support('Võtke toit kaasa ja jätkake tööd. Aitäh, et teatasite!', 'Заберите еду с собой и продолжайте работу. Спасибо, что сообщили!'),
          ], const NavEnd()),
        ];

      // ----- Пьяный клиент у двери -----
      case Scenario.drunkClient:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Ooo, sõber! Tule sisse, joome koos!', 'Ооо, друг! Заходи, выпьем вместе!'),
            _ask(ChatThread.klient, 'Клиент нетрезв и зовёт внутрь. Откажитесь вежливо, останьтесь снаружи:', false, [
              _c('Aitäh, aga ma olen tööl. Palun, siin on teie tellimus.', 'Спасибо, но я на работе. Пожалуйста, вот ваш заказ.'),
              _c('Olgu, aga ainult üks klaas.', 'Ладно, но только один стакан.', correct: false, rating: -0.2,
                  followUp: [
                    _client('Super, tule edasi!', 'Супер, проходи!'),
                    _client('(Järgmised tellimused hilinevad. Töö ajal külla minna ei tohi.)', '(Следующие заказы опоздают. Во время работы заходить в гости нельзя.)'),
                  ]),
              _c('Te olete purjus. Häbi!', 'Вы пьяны. Стыдно!', correct: false, rating: -0.1,
                  followUp: [_client('Mida sa ütlesid?! Mine minema!', 'Что ты сказал?! Уходи!')]),
            ], ['g_aitah', 'p_tellimus']),
            _client('Oota... kas ma üldse tellisin midagi? Ma ei mäleta.', 'Погоди... я вообще что-то заказывал? Не помню.'),
            _ask(ChatThread.klient, 'Клиент не помнит заказ. Спокойно подтвердите по приложению:', false, [
              _c('Jah, see on teie tellimus. Äpis on teie nimi ja see aadress.', 'Да, это ваш заказ. В приложении ваше имя и этот адрес.',
                  followUp: [_client('Ahaa... jah, see olen mina!', 'Ага... да, это я!')]),
              _c('Pole vahet, lihtsalt võtke.', 'Без разницы, просто берите.', correct: false, rating: -0.1,
                  followUp: [_client('Aga äkki see ei ole minu oma?', 'А вдруг это не моё?')]),
              _c('Kui te ei mäleta, viin toidu tagasi.', 'Если не помните, увезу еду обратно.', correct: false, rating: -0.1,
                  followUp: [_client('Ei-ei, oota! Las ma mõtlen...', 'Нет-нет, погоди! Дай подумаю...')]),
            ], ['p_nimi_q', 'p_oige_q', 'g_jah']),
            _client('Jah, õige, minu tellimus! Aitäh, sõber!', 'Да, точно, мой заказ! Спасибо, друг!'),
            ..._handover(),
          ], const NavEnd()),
        ];

      // ----- Собака во дворе -----
      case Scenario.dogYard:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! Värav on lahti, tulge hoovi. Uks on maja taga.', 'Здравствуйте! Ворота открыты, заходите во двор. Дверь за домом.'),
            _ask(ChatThread.klient, 'Во дворе большая собака без привязи. Напишите клиенту:', true, [
              _c('Teie hoovis on suur koer. Kas saate koera kinni hoida, palun?', 'У вас во дворе большая собака. Можете подержать собаку, пожалуйста?'),
              _c('Pole hullu, ma lähen ikka sisse.', 'Ничего, я всё равно зайду.', correct: false, rating: -0.15,
                  followUp: [
                    _client('Ärge minge! Ta võib hammustada!', 'Не заходите! Она может укусить!'),
                    _client('Oodake värava juures, ma tulen.', 'Подождите у ворот, я подойду.'),
                  ]),
              _c('Ma viskan koti üle aia.', 'Я переброшу пакет через забор.', correct: false, rating: -0.1,
                  followUp: [_client('Palun ärge! Kott läheb katki ja koer sööb toidu ära.', 'Пожалуйста, не надо! Пакет порвётся, и собака съест еду.')]),
            ], ['g_palun', 's_maja', 's_uks']),
            _client('Oi, vabandust! Ma panen koera ketti ja tulen ise värava juurde.', 'Ой, извините! Посажу собаку на цепь и сам подойду к воротам.'),
            _ask(ChatThread.klient, 'Согласуйте передачу у ворот:', true, [
              _c('Aitäh! Ma ootan teid värava juures.', 'Спасибо! Я подожду вас у ворот.'),
              _c('Kiiremini, palun, mul on kiire.', 'Побыстрее, пожалуйста, я тороплюсь.', correct: false, rating: -0.05,
                  followUp: [_client('Üks hetk! Koer on suur, see võtab aega.', 'Минутку! Собака большая, это занимает время.')]),
              _c('Jätan toidu värava taha ja lähen.', 'Оставлю еду за воротами и уеду.', correct: false, rating: -0.1,
                  followUp: [_client('Ei, palun oodake! Muidu sööb koer selle ära.', 'Нет, пожалуйста, подождите! Иначе собака её съест.')]),
            ], ['g_aitah', 'p_ootan']),
            _client('Olen kohal, koer on toas. Aitäh, et ootasite!', 'Я на месте, собака дома. Спасибо, что подождали!'),
            ..._handover(),
          ], const NavEnd()),
        ];

      // ----- Ресторан собрал не то и отказывается менять — эскалация -----
      case Scenario.wrongItems:
        return [
          Phase('client', ChatThread.klient, [
            _clientArrived(),
            _client('Tere! ... Oot, see on vale toit! Mina tellisin ${o.itemsEt}, aga kotis on midagi muud.', 'Здравствуйте! ...Стоп, это не та еда! Я заказывал ${o.itemsRu}, а в пакете что-то другое.'),
            _ask(ChatThread.klient, 'Клиенту привезли не те блюда. Ответьте:', false, [
              _c('Vabandust! Kontrollin kohe äpist. Kott oli kinni, ma ei näinud sisse.', 'Извините! Сейчас проверю в приложении. Пакет был закрыт, я не видел внутрь.'),
              _c('Te ise tellisite valesti.', 'Вы сами заказали неправильно.', correct: false, rating: -0.15,
                  followUp: [_client('Ei tellinud! Vaadake ise äppi!', 'Не заказывал! Сами посмотрите в приложение!')]),
              _c('Toit on toit. Sööge seda.', 'Еда есть еда. Ешьте это.', correct: false, rating: -0.2,
                  followUp: [_client('Mis?! Ma annan ühe tärni ja kirjutan kaebuse!', 'Что?! Поставлю одну звезду и напишу жалобу!')]),
            ], ['g_vabandust', 'p_oige_q']),
            _client('Palun lahendage see ära. Ma ootan.', 'Пожалуйста, решите это. Я жду.'),
          ], const NavChoose('Не те блюда. Что дальше?', [
            NavOption('🏪 Написать ресторану', 'rest_wrong'),
            NavOption('🎧 Связаться с поддержкой', 'support_wrong'),
          ])),
          Phase('rest_wrong', ChatThread.restoran, [
            _ask(ChatThread.restoran, 'Напишите ресторану о неверном заказе:', true, [
              _c('Tere! Klient sai vale tellimuse. Kas saate õige toidu valmis teha?', 'Здравствуйте! Клиент получил не тот заказ. Можете приготовить правильную еду?'),
              _c('Te tegite kõik valesti! See on häbi!', 'Вы всё сделали неправильно! Это позор!', correct: false, rating: -0.1,
                  followUp: [_rest('Palun rääkige viisakalt, siis saame aidata.', 'Пожалуйста, говорите вежливо, тогда сможем помочь.')]),
              _c('Ma toon toidu tagasi ja võtan ise uue.', 'Я привезу еду назад и сам возьму новую.', correct: false, rating: -0.05,
                  followUp: [_rest('Oodake! Ilma klienditoe loata me ei vaheta midagi.', 'Подождите! Без разрешения поддержки мы ничего не меняем.')]),
            ], ['p_oige_q', 'p_valmis_q']),
            _rest('Meie andsime õige koti. Me ei saa midagi teha. Pöörduge klienditoe poole.', 'Мы выдали правильный пакет. Мы ничего не можем сделать. Обратитесь в поддержку.'),
          ], const NavChoose('Ресторан отказывается помочь. Что дальше?', [
            NavOption('🎧 Связаться с поддержкой', 'support_wrong'),
          ])),
          Phase('support_wrong', ChatThread.tugi, [
            _support('Tere! Siin klienditugi. Kuidas saan aidata?', 'Здравствуйте! Это служба поддержки. Чем могу помочь?'),
            _ask(ChatThread.tugi, 'Опишите проблему поддержке:', true, [
              _c('Klient sai vale toidu. Restoran ei aita. Palun lahendust.', 'Клиент получил не ту еду. Ресторан не помогает. Прошу решения.',
                  followUp: [_support('Selge. Teeme kliendile tagasimakse ja vormistame uue tellimuse.', 'Понятно. Сделаем клиенту возврат и оформим новый заказ.')]),
              _c('See on restorani süü, mitte minu!', 'Это вина ресторана, не моя!', correct: false, rating: -0.05,
                  followUp: [_support('Me ei otsi süüdlast. Kirjeldage lihtsalt probleemi.', 'Мы не ищем виноватых. Просто опишите проблему.')]),
              _c('Klient vist valetab.', 'Клиент, наверное, врёт.', correct: false, rating: -0.1,
                  followUp: [_support('Meil on tellimusest foto. Klient ei valeta.', 'У нас есть фото заказа. Клиент не врёт.')]),
            ], ['p_tugi_q', 'p_oige_q']),
            _support('Kliendile tuleb tagasimakse. Öelge talle ka. Aitäh abi eest!', 'Клиенту будет возврат. Сообщите ему тоже. Спасибо за помощь!'),
          ], const NavChoose('Сообщите клиенту решение', [
            NavOption('📲 Вернуться к клиенту', 'client_wrong_end'),
          ])),
          Phase('client_wrong_end', ChatThread.klient, [
            _ask(ChatThread.klient, 'Передайте клиенту решение поддержки:', true, [
              _c('Klienditugi teeb tagasimakse ja uue tellimuse. Vabandust veel kord!', 'Поддержка сделает возврат и новый заказ. Ещё раз извините!'),
              _c('Kõik on korras, ärge muretsege.', 'Всё в порядке, не волнуйтесь.', correct: false, rating: -0.05,
                  followUp: [_client('Mis täpselt on korras? Öelge konkreetselt.', 'Что именно в порядке? Скажите конкретно.')]),
              _c('Ma ei tea, mis edasi saab.', 'Я не знаю, что будет дальше.', correct: false, rating: -0.05,
                  followUp: [_client('Kuidas ei tea? Te ju rääkisite klienditoega!', 'Как не знаете? Вы же говорили с поддержкой!')]),
            ], ['g_vabandust', 'g_aitah']),
            _client('Aitäh abi eest! Te olete tubli kuller.', 'Спасибо за помощь! Вы молодец-курьер.'),
          ], const NavEnd()),
        ];
    }
  }

  static Delivery build(Order o) {
    final phases = <String, Phase>{
      'rest': _restPhase(o),
      'support_opt': _supportOptPhase(),
    };
    for (final p in _scenarioPhases(o)) {
      phases[p.id] = p;
    }
    return Delivery('rest', phases);
  }
}
