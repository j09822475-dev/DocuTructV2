import 'lexicon.dart';
import 'ukr_forms.dart';

/// Результат разбора слова из текста.
class WordAnalysis {
  final Lexeme lex;
  final String formName; // название формы (RU)
  final String rule; // грамматическое правило (RU)
  const WordAnalysis(this.lex, this.formName, this.rule);

  /// Три основные формы через дефис.
  String get formsLine {
    if (lex.kind == 'v') return '${lex.f1} — ${lex.f2} — ${lex.f3}n';
    if (lex.kind == 'n' || lex.kind == 'a') {
      return '${lex.f1} — ${lex.f2} — ${lex.f3}';
    }
    return lex.f1;
  }

  String get kindName => switch (lex.kind) {
        'n' => 'существительное',
        'a' => 'прилагательное / числительное',
        'v' => 'глагол',
        'p' => 'послелог места',
        _ => 'служебное слово',
      };
}

/// Падежные окончания, добавляемые к основе omastav (родительного падежа).
const _caseEndings = <String, (String, String)>{
  'sse': ('Sisseütlev (kuhu?)', 'куда? во что? Образуется: omastav + -sse'),
  's': ('Seesütlev (kus?)', 'где? в чём? Образуется: omastav + -s'),
  'st': ('Seestütlev (kust?)', 'откуда? из чего? Образуется: omastav + -st'),
  'le': ('Alaleütlev (kellele? kuhu peale?)',
      'кому? на что? Образуется: omastav + -le'),
  'l': ('Alalütlev (kus? kellel?)',
      'на чём? у кого? Образуется: omastav + -l'),
  'lt': ('Alaltütlev (kellelt? millelt?)',
      'от кого? с чего? Образуется: omastav + -lt'),
  'ga': ('Kaasaütlev (kellega? millega?)',
      'с кем? с чем? Образуется: omastav + -ga'),
  'ta': ('Ilmaütlev (ilma milleta?)', 'без чего? Образуется: omastav + -ta'),
  'ks': ('Saav (kelleks? milleks?)', 'кем? чем (становится)? omastav + -ks'),
  'ni': ('Rajav (milleni?)', 'до чего? Образуется: omastav + -ni'),
  'na': ('Olev (kellena?)',
      'кем? в роли кого? Образуется: omastav + -na (Ma töötan õpetajaNA)'),
};

/// Личные окончания глагола в настоящем времени.
const _verbEndings = <String, (String, String)>{
  'n': ('Olevik, mina-vorm', 'настоящее время, 1 л. ед.ч. (я): основа + -n'),
  'd': ('Olevik, sina-vorm', 'настоящее время, 2 л. ед.ч. (ты): основа + -d'),
  'b': ('Olevik, tema-vorm',
      'настоящее время, 3 л. ед.ч. (он/она): основа + -b'),
  'me': ('Olevik, meie-vorm', 'настоящее время, 1 л. мн.ч. (мы): основа + -me'),
  'te': ('Olevik, teie-vorm', 'настоящее время, 2 л. мн.ч. (вы): основа + -te'),
  'vad': ('Olevik, nemad-vorm',
      'настоящее время, 3 л. мн.ч. (они): основа + -vad'),
};

/// Окончания условного наклонения (tingiv kõneviis), добавляются к основе.
const _condEndings = <String, (String, String)>{
  'ksin': ('Tingiv kõneviis, mina-vorm',
      'условное наклонение (я бы …): основа + -ksin'),
  'ksid': ('Tingiv kõneviis, sina/nemad-vorm',
      'условное наклонение (ты бы / они бы): основа + -ksid'),
  'ksime': ('Tingiv kõneviis, meie-vorm',
      'условное наклонение (мы бы): основа + -ksime'),
  'ksite': ('Tingiv kõneviis, teie-vorm',
      'условное наклонение (вы бы): основа + -ksite'),
  'ks': ('Tingiv kõneviis (…бы)',
      'условное наклонение: основа + -ks (ta oleks — «він би був»)'),
};

/// Окончания прошедшего времени (lihtminevik), добавляются к основе.
const _pastEndings = <String, (String, String)>{
  'sin': ('Lihtminevik, mina-vorm',
      'прошедшее время, 1 л. ед.ч. (я): основа + -sin'),
  'sid': ('Lihtminevik, sina/nemad-vorm',
      'прошедшее время: основа + -sid (ты / они)'),
  'sime': ('Lihtminevik, meie-vorm',
      'прошедшее время, 1 л. мн.ч. (мы): основа + -sime'),
  'site': ('Lihtminevik, teie-vorm',
      'прошедшее время, 2 л. мн.ч. (вы): основа + -site'),
  's': ('Lihtminevik, tema-vorm',
      'прошедшее время, 3 л. ед.ч. (он/она): основа + -s'),
};

/// Неправильные и особые формы: слово → (лемма-f1, название формы, правило).
const _irregular = <String, (String, String, String)>{
  'on': ('olema', 'Olevik, tema/nemad-vorm',
      'глагол olema: ta on / nad on — «він є / вони є» (наст. время)'),
  'oli': ('olema', 'Lihtminevik, tema-vorm',
      'глагол olema в прошедшем времени: ta oli — «він був»'),
  'olid': ('olema', 'Lihtminevik, sina/nemad-vorm',
      'глагол olema в прошедшем: sa olid / nad olid'),
  'olin': ('olema', 'Lihtminevik, mina-vorm',
      'глагол olema в прошедшем: ma olin — «я був»'),
  'olnud': ('olema', 'nud-partitsiip',
      'причастие прошедшего: ma olen olnud — «я був/бував»'),
  'pole': ('olema', 'Eitus (краткая форма)',
      'pole = ei ole — «нема, не є». После pole — osastav!'),
  'sain': ('saama', 'Lihtminevik, mina-vorm',
      'неправильная форма: saama → ma sain — «я отримав»'),
  'sai': ('saama', 'Lihtminevik, tema-vorm',
      'неправильная форма: saama → ta sai — «він отримав»'),
  'läks': ('minema', 'Lihtminevik, tema-vorm',
      'неправильная форма: minema → ta läks — «він пішов»'),
  'läksin': ('minema', 'Lihtminevik, mina-vorm',
      'неправильная форма: minema → ma läksin — «я пішов»'),
  'lähme': ('minema', 'Olevik, meie-vorm',
      'разговорная форма: läheme/lähme — «ходімо!»'),
  'tegin': ('tegema', 'Lihtminevik, mina-vorm',
      'неправильная форма: tegema → ma tegin — «я зробив»'),
  'tegi': ('tegema', 'Lihtminevik, tema-vorm',
      'неправильная форма: tegema → ta tegi — «він зробив»'),
  'tuppa': ('tuba', 'Lühike sisseütlev (kuhu?)',
      'короткий иллатив: tuba → tuppa — «в комнату» (вместо toasse)'),
  'vannituppa': ('vannituba', 'Lühike sisseütlev (kuhu?)',
      'короткий иллатив: vannituba → vannituppa — «в ванную»'),
  'koju': ('kodu', 'Lühike sisseütlev (kuhu?)',
      'короткий иллатив: kodu → koju — «домой» (вместо kodusse)'),
  'kodus': ('kodu', 'Seesütlev (kus?)', 'kodu + -s — «дома»'),
  'poodi': ('pood', 'Lühike sisseütlev (kuhu?)',
      'короткий иллатив: pood → poodi — «в магазин»'),
  'linna': ('linn', 'Lühike sisseütlev / osastav',
      'linna — «в город» (короткий иллатив) или винительный (osastav)'),
  'kooli': ('kool', 'Lühike sisseütlev / omastav',
      'kooli — «в школу» (короткий иллатив): Ma lähen kooli'),
  'vette': ('vesi', 'Lühike sisseütlev (kuhu?)',
      'короткий иллатив: vesi → vette — «в воду»'),
  'mehe': ('mees', 'Omastav', 'mees → mehe → meest'),
  'aastane': ('aasta', 'Liide -ne (…-летний)',
      'aasta + -ne: kolmekümneaastane — «тридцятирічний»'),
  'sööb': ('sööma', 'Olevik, tema-vorm', 'sööma → ta sööb — «він їсть»'),
  'sõi': ('sööma', 'Lihtminevik, tema-vorm', 'sööma → ta sõi — «він з’їв»'),
  'jõin': ('jooma', 'Lihtminevik, mina-vorm', 'jooma → ma jõin — «я випив»'),
  'jõi': ('jooma', 'Lihtminevik, tema-vorm', 'jooma → ta jõi — «він випив»'),
  'käin': ('käima', 'Olevik, mina-vorm', 'käima → ma käin — «я ходжу»'),
  'käib': ('käima', 'Olevik, tema-vorm', 'käima → ta käib — «він ходить»'),
  'viin': ('viima', 'Olevik, mina-vorm', 'viima → ma viin — «я несу/веду»'),
  'toon': ('tooma', 'Olevik, mina-vorm', 'tooma → ma toon — «я приношу»'),
  'toob': ('tooma', 'Olevik, tema-vorm', 'tooma → ta toob — «він приносить»'),
  'tehakse': ('tegema', 'Umbisikuline (пассив)',
      'безличная форма: tehakse — «роблять» (röntgenit tehakse — рентген роблять)'),
  'süüa': ('sööma', 'da-infinitiiv',
      'da-инфинитив: teen süüa — «готую їжу»; tahan süüa — «хочу їсти»'),
  'juua': ('jooma', 'da-infinitiiv', 'da-инфинитив: tahan juua — «хочу пити»'),
  'teha': ('tegema', 'da-infinitiiv', 'da-инфинитив: oskan süüa teha'),
  'näha': ('nägema', 'da-infinitiiv', 'da-инфинитив: tahan näha — «хочу бачити»'),
  'ust': ('uks', 'Osastav', 'uks → ukse → ust (частичный падеж)'),
  'vett': ('vesi', 'Osastav', 'vesi → vee → vett: joon vett — «п’ю воду»'),
  'putru': ('puder', 'Osastav', 'puder → pudru → putru: söön putru'),
  'merd': ('meri', 'Osastav', 'meri → mere → merd'),
  'kätt': ('käsi', 'Osastav', 'käsi → käe → kätt'),
  'käsi': ('käsi', 'Nimetav / mitmuse osastav',
      'käsi — рука (ед.ч.) или «руки» (мн. osastav): pesen käsi'),
  'last': ('laps', 'Osastav', 'laps → lapse → last'),
  'meest': ('mees', 'Osastav', 'mees → mehe → meest'),
  'üht': ('üks', 'Osastav', 'üks → ühe → üht'),
  'kaht': ('kaks', 'Osastav', 'kaks → kahe → kaht'),
  'viit': ('viis', 'Osastav', 'viis → viie → viit'),
  'kuut': ('kuus', 'Osastav', 'kuus → kuue → kuut'),
  'peal': ('peal', 'Послелог', 'laua peal — «на столі» (kus?)'),
  'mööda': ('mööda', 'Предлог/послелог', 'mööda koridori — «уздовж коридору»'),
  'head': ('hea', 'Osastav', 'hea → hea → head: Head isu! Head aega!'),
  'uut': ('uus', 'Osastav', 'uus → uue → uut: otsin uut tööd'),
  'suurt': ('suur', 'Osastav', 'suur → suure → suurt'),
  'kõike': ('kõik', 'Osastav', 'kõik → kõige → kõike — «все» (вин.)'),
  'kõigile': ('kõik', 'Alaleütlev', 'kõik → kõigile — «всім»'),
  'kõiki': ('kõik', 'Mitmuse osastav', 'kõik → kõiki — «всіх»'),
  'pikem': ('pikk', 'Keskvõrre (сравнительная)',
      'сравнительная степень (нерегулярная): pikk → pikem — «довший»'),
  'vanem': ('vana', 'Keskvõrre (сравнительная)',
      'сравнительная степень: vana → vanem — «старший»'),
  'noorem': ('noor', 'Keskvõrre (сравнительная)',
      'сравнительная степень: noor → noorem — «молодший»'),
  'parem': ('hea', 'Keskvõrre (сравнительная)',
      'сравнительная степень (нерегулярная): hea → parem — «кращий»'),
  'paremaks': ('hea', 'Keskvõrre + saav',
      'parem (кращий) + -ks: teeb päeva paremaks — «робить день кращим»'),
  'raskemat': ('raske', 'Keskvõrre + osastav',
      'raske → raskem (складніший) + -t: tahad raskemat? — «хочеш складніше?»'),
  'suurem': ('suur', 'Keskvõrre (сравнительная)',
      'сравнительная степень: suur → suurem — «більший»'),
  'ilusam': ('ilus', 'Keskvõrre (сравнительная)',
      'сравнительная степень: ilus → ilusam — «гарніший»'),
  'esimesse': ('esimene', 'Lühike sisseütlev (kuhu?)',
      'короткий иллатив: esimene → esimesse (klassi) — «у перший (клас)»'),
  'inimeste': ('inimene', 'Mitmuse omastav',
      'родительный мн.ч. (нерегулярный): inimene → inimeste'),
  'sõbrad': ('sõber', 'Mitmuse nimetav',
      'множественное число: sõber → sõbrad — «друзі»'),
  'sõpradele': ('sõber', 'Alaleütlev (мн.ч.)',
      'sõprade (мн. omastav) + -le: kirjutan sõpradele — «пишу друзям»'),
  'sõpradega': ('sõber', 'Kaasaütlev (мн.ч.)',
      'sõprade + -ga: mängin sõpradega — «граю з друзями»'),
  'toad': ('tuba', 'Mitmuse nimetav',
      'множественное число: tuba → toad — «кімнати»'),
  'oma': ('oma', 'Omadussõna', 'oma — «свій», не изменяется перед словом'),
  // порядковые числительные: без этих записей nelja+s разобралось бы
  // как падеж количественного neli («в четырёх»)
  'neljas': ('neljas', 'Nimetav (järgarv — порядковое)',
      'порядковое числительное: neli → neljas — «четвертий». '
          'Склоняется от основы neljanda-: neljandal korrusel.'),
  'viies': ('viies', 'Nimetav (järgarv — порядковое)',
      'порядковое числительное: viis → viies — «п’ятий». '
          'Основа: viienda- (viiendal korrusel).'),
  'kuues': ('kuues', 'Nimetav (järgarv — порядковое)',
      'порядковое числительное: kuus → kuues — «шостий». '
          'Основа: kuuenda- (kuuendal korrusel).'),
  'seitsmes': ('seitsmes', 'Nimetav (järgarv — порядковое)',
      'порядковое числительное: seitse → seitsmes — «сьомий». '
          'Основа: seitsmenda-.'),
  'kaheksas': ('kaheksas', 'Nimetav (järgarv — порядковое)',
      'порядковое числительное: kaheksa → kaheksas — «восьмий». '
          'Основа: kaheksanda-.'),
  'üheksas': ('üheksas', 'Nimetav (järgarv — порядковое)',
      'порядковое числительное: üheksa → üheksas — «дев’ятий». '
          'Основа: üheksanda-.'),
  'kümnes': ('kümnes', 'Nimetav (järgarv — порядковое)',
      'порядковое числительное: kümme → kümnes — «десятий». '
          'Основа: kümnenda-.'),
  'sajas': ('sajas', 'Nimetav (järgarv — порядковое)',
      'порядковое числительное: sada → sajas — «сотий». '
          'Основа: sajanda- (sajandal korrusel).'),
  // послелоги-омонимы: без записей их перехватили бы vastama и koht
  'vastas': ('vastas', 'Послелог места',
      'voodi vastas — «навпроти ліжка» (kus?). Не путать с глаголом: '
          'ta vastas — «він відповів».'),
  'kohal': ('kohal', 'Послелог места',
      'diivani kohal — «над диваном» (kus?). Ставится после слова '
          'в omastav.'),
  // повелительное наклонение — неправильные формы
  'mine': ('minema', 'Käskiv kõneviis (sina)',
      'повелительное: minema → mine! — «іди!» (мн.ч.: minge!)'),
  'minge': ('minema', 'Käskiv kõneviis (mitmus)',
      'повелительное мн.ч.: minema → minge! — «ідіть!»'),
  'tulge': ('tulema', 'Käskiv kõneviis (mitmus)',
      'повелительное мн.ч.: tulema → tulge! — «ідіть/приходьте!»'),
  'tehke': ('tegema', 'Käskiv kõneviis (mitmus)',
      'повелительное мн.ч.: tegema → tehke! — «робіть!»'),
  'andke': ('andma', 'Käskiv kõneviis (mitmus)',
      'повелительное мн.ч.: andma → andke! — «дайте!»'),
  'sõitke': ('sõitma', 'Käskiv kõneviis (mitmus)',
      'повелительное мн.ч.: sõitma → sõitke! — «їдьте!»'),
  'sööngi': ('sööma', 'Olevik + -gi',
      'söön + -gi (усиление): Sööngi! — «я таки їм!»'),
  // прошедшее время — неправильные формы
  'nägin': ('nägema', 'Lihtminevik, mina-vorm',
      'неправильная форма: nägema → ma nägin — «я побачив»'),
  'nägi': ('nägema', 'Lihtminevik, tema-vorm',
      'неправильная форма: nägema → ta nägi — «він побачив»'),
  'nägid': ('nägema', 'Lihtminevik, sina/nemad-vorm',
      'неправильная форма: nägema → nad nägid — «вони бачили»'),
  'tegid': ('tegema', 'Lihtminevik, sina/nemad-vorm',
      'неправильная форма: tegema → nad tegid — «вони зробили»'),
  'panin': ('panema', 'Lihtminevik, mina-vorm',
      'неправильная форма: panema → ma panin — «я поклав»'),
  // mas/des/mist-формы
  'jalutamas': ('jalutama', 'mas-vorm',
      'käime jalutamas — «ходимо гуляти» (процесс)'),
  'matkamas': ('matkama', 'mas-vorm',
      'käin matkamas — «ходжу в походи»'),
  'jalutades': ('jalutama', 'des-vorm',
      'jalutades — «гуляючи»: Edasi jalutades nägid nad põõsast.'),
  'minnes': ('minema', 'des-vorm',
      'minnes — «йдучи»: edasi minnes — «йдучи далі»'),
  'söömist': ('sööma', 'mine-vorm (osastav)',
      'söömine → pärast söömist — «після їжі»'),
  'tegemist': ('tegema', 'mine-vorm (osastav)',
      'tegemine → meil on palju tegemist — «у нас багато справ»'),
  // множественное число — особые формы
  'lastel': ('laps', 'Alalütlev (мн.ч.)',
      'laps → lastel: Ta aitab lastel õppida — «допомагає дітям вчитися»'),
  'lapsi': ('laps', 'Mitmuse osastav',
      'laps → lapsi: Ta armastab lapsi — «любить дітей»'),
  'töid': ('töö', 'Mitmuse osastav',
      'töö → töid: teeme erinevaid töid — «різні роботи»'),
  'aiatöid': ('töö', 'Mitmuse osastav',
      'aiatöö → aiatöid — «садові роботи» (mulle meeldib aiatöid teha)'),
  'maju': ('maja', 'Mitmuse osastav',
      'maja → maju: Ta ehitab maju — «будує будинки»'),
  'puid': ('puu', 'Mitmuse osastav', 'puu → puid: palju puid — «багато дерев»'),
  'teisi': ('teine', 'Mitmuse osastav',
      'teine → teisi: näeb teisi linde — «бачить інших птахів»'),
  'sõnu': ('sõna', 'Mitmuse osastav',
      'sõna → sõnu: kasutada sõnu — «вживати слова»'),
  'kohtades': ('koht', 'Seesütlev (мн.ч.)',
      'koht → kohtades: erinevates kohtades — «у різних місцях»'),
  'dokumentidega': ('dokument', 'Kaasaütlev (мн.ч.)',
      'dokument → dokumentidega: riiul dokumentidega — «полиця з документами»'),
  'ühte': ('üks', 'Osastav / lühike sisseütlev',
      'üks → ühte: veel ühte kohta — «ще одне місце»'),
  // сравнительная степень — неправильные формы
  'targem': ('tark', 'Keskvõrre (сравнительная)',
      'сравнительная степень: tark → targem — «розумніший»'),
  'kurvem': ('kurb', 'Keskvõrre (сравнительная)',
      'сравнительная степень: kurb → kurvem — «сумніший»'),
  'halvem': ('halb', 'Keskvõrre (сравнительная)',
      'сравнительная степень: halb → halvem — «гірший»'),
  'külmem': ('külm', 'Keskvõrre (сравнительная)',
      'сравнительная степень: külm → külmem — «холодніший»'),
  'väiksem': ('väike', 'Keskvõrre (сравнительная)',
      'сравнительная степень: väike → väiksem — «менший»'),
  'lühem': ('lühike', 'Keskvõrre (сравнительная)',
      'сравнительная степень: lühike → lühem — «коротший»'),
  'abivalmid': ('abivalmis', 'Mitmuse nimetav',
      'abivalmis → abivalmid — «готові допомогти» (мн.ч.)'),
  // rõõmus — прилагательное, а не rõõm + -s
  'rõõmus': ('rõõmus', 'Nimetav (kes? mis?)',
      'прилагательное: rõõmus — «веселий, радісний». '
          'Формы: rõõmus — rõõmsa — rõõmsat. Мн.ч.: rõõmsad.'),
  'teksad': ('teksased', 'Разговорная форма',
      'teksad = teksased — «джинси»'),
  'olge': ('olema', 'Käskiv kõneviis (mitmus)',
      'повелительное мн.ч.: olema → olge! — «будьте!» (Olge viisakad!)'),
  'tõi': ('tooma', 'Lihtminevik, tema-vorm',
      'неправильная форма: tooma → ta tõi — «він приніс»'),
  'tegime': ('tegema', 'Lihtminevik, meie-vorm',
      'неправильная форма: tegema → me tegime — «ми зробили»'),
  'parim': ('hea', 'Ülivõrre (превосходная)',
      'превосходная степень: hea → parem → parim — «найкращий»'),
  'tulemast': ('tulema', 'mast-vorm',
      'Tere tulemast! — «Ласкаво просимо!» (дословно: с приходом)'),
  'tuli': ('tulema', 'Lihtminevik, tema-vorm',
      'неправильная форма: tulema → ta tuli — «він прийшов»'),
  'tulid': ('tulema', 'Lihtminevik, sina/nemad-vorm',
      'неправильная форма: tulema → nad tulid — «вони прийшли»'),
  'tõid': ('tooma', 'Lihtminevik, sina/nemad-vorm',
      'неправильная форма: tooma → nad tõid — «вони принесли»'),
  'sõid': ('sööma', 'Lihtminevik, sina/nemad-vorm',
      'неправильная форма: sööma → nad sõid — «вони з’їли»'),
  'sõime': ('sööma', 'Lihtminevik, meie-vorm',
      'неправильная форма: sööma → me sõime — «ми їли»'),
  'kingitakse': ('kinkima', 'Umbisikuline (пассив)',
      'безличная форма: kingitakse — «дарують» (lilli kingitakse)'),
  'tänatakse': ('tänama', 'Umbisikuline (пассив)',
      'безличная форма: tänatakse — «дякують»'),
  'kutsumast': ('kutsuma', 'mast-vorm',
      'Aitäh kutsumast! — «Дякую за запрошення!»'),
  'maksis': ('maksma', 'Lihtminevik, tema-vorm',
      'неправильная форма: maksma → see maksis — «це коштувало»'),
  'häid': ('hea', 'Mitmuse osastav',
      'hea → häid: Häid pühi! — «Гарних свят!»'),
  'sel': ('see', 'Alalütlev (краткая форма)',
      'see → sel: sel päeval — «того дня»'),
  'sünnipäevi': ('sünnipäev', 'Mitmuse osastav',
      'sünnipäev → sünnipäevi: palju sünnipäevi — «багато днів народження»'),
  'väikseid': ('väike', 'Mitmuse osastav',
      'väike → väikseid: teevad väikseid kingitusi'),
  'uutest': ('uus', 'Seestütlev (мн.ч.)',
      'uus → uutest: räägib uutest lugudest — «про нові історії»'),
  'lugudest': ('lugu', 'Seestütlev (мн.ч.)',
      'lugu → lugudest: põnevatest lugudest — «про захопливі історії»'),
  'mänge': ('mäng', 'Mitmuse osastav',
      'mäng → mänge: mängivad mänge — «грають в ігри»'),
  'pidusid': ('pidu', 'Mitmuse osastav',
      'pidu → pidusid: armastab pidusid — «любить свята»'),
  'sõprade': ('sõber', 'Mitmuse omastav',
      'sõber → sõprade: sõprade seas — «серед друзів»'),
  'parima': ('hea', 'Ülivõrre, omastav',
      'hea → parim → parima: minu parima sõbranna — «моєї найкращої подруги»'),
  'naistele': ('naine', 'Alaleütlev (мн.ч.)',
      'naine → naistele: mehed annavad naistele lilli — «жінкам»'),
  'inimestel': ('inimene', 'Alalütlev (мн.ч.)',
      'inimene → inimestel: aitab inimestel leida — «допомагає людям»'),
  'juhtumistest': ('juhtum', 'Seestütlev (мн.ч.)',
      'juhtum → juhtumistest: naljakatest juhtumistest — «про смішні випадки»'),
  'tehtud': ('tegema', 'tud-partitsiip (пассивное причастие)',
      'tegema → tehtud: Pilt on tehtud köögis — «фото зроблено на кухні»'),
  'siniseid': ('sinine', 'Mitmuse osastav',
      'sinine → siniseid: kannab siniseid teksapükse — «носить сині джинси»'),
  'lastele': ('laps', 'Alaleütlev (мн.ч.)',
      'laps → lastele: Isa naeratab lastele — «тато усміхається дітям»'),
};

String _norm(String raw) {
  var t = raw.toLowerCase().trim();
  t = t.replaceAll(RegExp(r'''[.,!?:;„“"«»()\[\]…—–]'''), '');
  return t.trim();
}

WordAnalysis? _matchNominal(Lexeme lx, String t) {
  if (t == lx.f1) {
    return WordAnalysis(lx, 'Nimetav (kes? mis?)',
        'Начальная форма — именительный падеж (algvorm). Три формы: ${lx.f1} — ${lx.f2} — ${lx.f3}.');
  }
  if (t == lx.f2 && lx.f2 != lx.f1) {
    return WordAnalysis(lx, 'Omastav (kelle? mille?)',
        'Родительный падеж — базовая форма, от которой образуются остальные падежи (${lx.f2} + окончание).');
  }
  if (t == lx.f3 && lx.f3 != lx.f1) {
    return WordAnalysis(lx, 'Osastav (keda? mida?)',
        'Частичный падеж (3-я форма). Употребляется после чисел и при отрицании: ei ole ${lx.f3}.');
  }
  if (lx.pl.isNotEmpty && t == lx.pl) {
    return WordAnalysis(lx, 'Mitmuse osastav (keda? mida? мн.ч.)',
        'Частичный падеж множественного числа: palju ${lx.pl}. Три формы: ${lx.f1} — ${lx.f2} — ${lx.f3}.');
  }
  // множественное число: omastav + d
  if (t == '${lx.f2}d') {
    return WordAnalysis(lx, 'Mitmuse nimetav (мн.ч.)',
        'Множественное число: ${lx.f2} (omastav) + -d = ${lx.f2}d.');
  }
  if (t == '${lx.f2}de' ||
      t == '${lx.f2}te' ||
      t == '${lx.f1}de' ||
      t == '${lx.f1}te') {
    return WordAnalysis(lx, 'Mitmuse omastav (кого? чего? мн.ч.)',
        'Родительный мн.ч.: основа + -de/-te (akende, unistuste).');
  }
  // наречие на -sti от прилагательного: kiire + -sti = kiiresti
  if (lx.kind == 'a' && t == '${lx.f2}sti') {
    return WordAnalysis(lx, 'Määrsõna (kuidas? — наречие)',
        'Наречие: ${lx.f2} (omastav) + -sti = ${lx.f2}sti («как? — ${lx.tr}»).');
  }
  // omastav + падежное окончание (сначала длинные окончания)
  final endings = _caseEndings.keys.toList()
    ..sort((a, b) => b.length.compareTo(a.length));
  for (final e in endings) {
    if (t == lx.f2 + e) {
      final info = _caseEndings[e]!;
      var name = info.$1;
      var extra = '';
      if (lx.kind == 'a' && e == 'lt') {
        name = 'Määrsõna (kuidas? — наречие)';
        extra = ' Часто это наречие: ${lx.f2}lt — «как? каким образом».';
      }
      return WordAnalysis(lx, name,
          '${lx.f2} (omastav) + -$e. ${info.$2}.$extra Три формы: ${lx.f1} — ${lx.f2} — ${lx.f3}.');
    }
    // падеж во множественном числе: omastav + de/te + окончание
    if (t == '${lx.f2}de$e' || t == '${lx.f2}te$e') {
      final info = _caseEndings[e]!;
      return WordAnalysis(lx, '${info.$1} (мн.ч.)',
          'Множественное число: ${lx.f2} + -de/-te + -$e. ${info.$2}.');
    }
  }
  // сравнительная степень прилагательных: omastav + m
  if (lx.kind == 'a' && (t == '${lx.f2}m' || t == '${lx.f2}mad')) {
    return WordAnalysis(lx, 'Keskvõrre (сравнительная степень)',
        'Сравнительная степень: ${lx.f2} (omastav) + -m = ${lx.f2}m («более ${lx.tr}»).');
  }
  return null;
}

WordAnalysis? _matchVerb(Lexeme lx, String t) {
  if (t == lx.f1) {
    return WordAnalysis(lx, 'ma-infinitiiv',
        'ma-инфинитив. Употребляется после pean, hakkan, lähen: Ma lähen ${lx.f1}.');
  }
  if (t == lx.f2) {
    return WordAnalysis(lx, 'da-infinitiiv',
        'da-инфинитив. Употребляется после tahan, armastan, oskan, meeldib: Ma tahan ${lx.f2}.');
  }
  if (t == lx.f3) {
    return WordAnalysis(lx, 'Tüvi: käskiv kõneviis / eitus',
        'Чистая основа наст. времени: повелительное «${lx.f3}!» и отрицание «ma ei ${lx.f3}».');
  }
  final maStem = lx.f1.endsWith('ma') ? lx.f1.substring(0, lx.f1.length - 2) : lx.f1;
  // настоящее время: основа + личное окончание
  final vEndings = _verbEndings.keys.toList()
    ..sort((a, b) => b.length.compareTo(a.length));
  for (final e in vEndings) {
    if (t == lx.f3 + e) {
      final info = _verbEndings[e]!;
      return WordAnalysis(lx, info.$1,
          '${lx.f3}- + -$e. ${info.$2}. Формы глагола: ${lx.f1} / ${lx.f2} / ${lx.f3}n.');
    }
  }
  // условное наклонение: основа + -ks-окончание
  final cEndings = _condEndings.keys.toList()
    ..sort((a, b) => b.length.compareTo(a.length));
  for (final e in cEndings) {
    if (t == lx.f3 + e) {
      final info = _condEndings[e]!;
      return WordAnalysis(lx, info.$1,
          '${lx.f3}- + -$e. ${info.$2}. Формы глагола: ${lx.f1} / ${lx.f2} / ${lx.f3}n.');
    }
  }
  // прошедшее время: основа (наст. или ma-основа) + -si-окончание
  final pEndings = _pastEndings.keys.toList()
    ..sort((a, b) => b.length.compareTo(a.length));
  for (final e in pEndings) {
    if (t == lx.f3 + e || t == maStem + e) {
      final info = _pastEndings[e]!;
      return WordAnalysis(lx, info.$1,
          '${info.$2}: ${lx.f1} → $t. Формы глагола: ${lx.f1} / ${lx.f2} / ${lx.f3}n.');
    }
  }
  // nud-причастие: ma-основа + nud
  if (t == '${maStem}nud') {
    return WordAnalysis(lx, 'nud-partitsiip (прошедшее причастие)',
        'ma-основа + -nud: ma olen ${maStem}nud — «я (уже) …». Формы: ${lx.f1} / ${lx.f2} / ${lx.f3}n.');
  }
  // повелительное наклонение мн.ч.: основа + -ge / da-основа + -ke
  final daStem =
      lx.f2.length > 2 && (lx.f2.endsWith('da') || lx.f2.endsWith('ta'))
          ? lx.f2.substring(0, lx.f2.length - 2)
          : '';
  if (t == '${lx.f3}ge' ||
      t == '${lx.f3}ke' ||
      (daStem.isNotEmpty && (t == '${daStem}ge' || t == '${daStem}ke'))) {
    return WordAnalysis(lx, 'Käskiv kõneviis (mitmus — вы)',
        'Повелительное наклонение мн.ч.: основа + -ge/-ke ($t!). '
            'Ед.ч.: ${lx.f3}! Формы глагола: ${lx.f1} / ${lx.f2} / ${lx.f3}n.');
  }
  return null;
}

WordAnalysis? _match(Lexeme lx, String t) {
  switch (lx.kind) {
    case 'n':
    case 'a':
      return _matchNominal(lx, t);
    case 'v':
      return _matchVerb(lx, t);
    case 'p':
      if (t == lx.f1) {
        return WordAnalysis(lx, 'Послелог места',
            'Ставится ПОСЛЕ слова в omastav: maja ${lx.f1} («${lx.tr}»).');
      }
      return null;
    default: // 'x'
      if (t == lx.f1 ||
          (lx.f2.isNotEmpty && t == lx.f2) ||
          (lx.f3.isNotEmpty && t == lx.f3)) {
        return WordAnalysis(
            lx,
            'Служебное слово',
            lx.f2.isNotEmpty && lx.f2 != lx.f1
                ? 'Полная форма — ${lx.f1}, краткая — ${lx.f2}.'
                : 'Не изменяется по падежам.');
      }
      return null;
  }
}

Lexeme? _byLemma(String f1) {
  for (final lx in lexicon) {
    if (lx.f1 == f1) return lx;
  }
  return null;
}

/// Разбор слова: ищем лемму, форму которой представляет собой [raw].
WordAnalysis? analyze(String raw) {
  final t = _norm(raw);
  if (t.isEmpty) return null;
  // 0) неправильные/особые формы
  final irr = _irregular[t];
  if (irr != null) {
    final lx = _byLemma(irr.$1);
    if (lx != null) return WordAnalysis(lx, irr.$2, irr.$3);
  }
  // 1) точное совпадение с одной из форм
  for (final lx in lexicon) {
    final r = _match(lx, t);
    if (r != null) return r;
  }
  // 2) составные слова: пробуем сматчить по концу слова
  //    (raamaturiiulil -> riiul + il), берём самое длинное совпадение
  WordAnalysis? best;
  var bestLen = 0;
  for (final lx in lexicon) {
    if (lx.kind != 'n' && lx.kind != 'a' && lx.kind != 'v') continue;
    for (var cut = 1; cut <= t.length - 3; cut++) {
      final tail = t.substring(cut);
      if (tail.length <= bestLen) break; // дальше хвосты только короче
      final r = _match(lx, tail);
      if (r != null) {
        best = WordAnalysis(r.lex, r.formName,
            '${r.rule} (составное слово: разобрана часть «$tail»)');
        bestLen = tail.length;
        break;
      }
    }
  }
  return best;
}

/// Короткий перевод для подстановки во фразы: «лев; Лев (знак)» → «лев».
String _shortTr(String tr) {
  var t = tr;
  for (final sep in [';', '(', ' / ', ' —']) {
    final i = t.indexOf(sep);
    if (i > 0) t = t.substring(0, i);
  }
  final c = t.indexOf(',');
  if (c > 0) t = t.substring(0, c);
  return t.trim();
}

/// Падежная форма перевода: сначала полный перевод, потом короткий,
/// в конце — сам короткий перевод как запасной вариант.
String _ukrForm(String tr, String Function(String) f) {
  final full = f(tr);
  if (full.isNotEmpty) return full;
  final s = _shortTr(tr);
  final short = f(s);
  return short.isEmpty ? s : short;
}

/// Фразы-шаблоны: каждая основная форма слова в предложении + перевод.
/// Возвращает (форма, эстонская фраза, украинский перевод).
List<(String, String, String)> formPhrases(Lexeme lx) {
  final tr = _shortTr(lx.tr);
  final gen = _ukrForm(lx.tr, ukrGenPlain);
  final acc = _ukrForm(lx.tr, ukrAccPlain);
  switch (lx.kind) {
    case 'n':
      return [
        (lx.f1, 'See on ${lx.f1}.', 'Це $tr.'),
        (lx.f2, 'Ma olen ${lx.f2} juures.', 'Я біля $gen.'),
        (lx.f3, 'Ma näen ${lx.f3}.', 'Я бачу $acc.'),
      ];
    case 'a':
      return [
        (lx.f1, 'See maja on ${lx.f1}.', 'Цей будинок $tr.'),
        (lx.f2, 'See on ${lx.f2} maja uks.', 'Це двері $gen будинку.'),
        (lx.f3, 'Ma näen ${lx.f3} maja.', 'Я бачу $acc будинок.'),
      ];
    case 'v':
      final pres = ukrPres1Plain(lx.tr);
      return [
        (lx.f1, 'Ma pean ${lx.f1}.', 'Я мушу $tr.'),
        (lx.f2, 'Ma tahan ${lx.f2}.', 'Я хочу $tr.'),
        (
          '${lx.f3}n',
          'Ma ${lx.f3}n iga päev.',
          pres.isEmpty ? 'Я щодня це роблю ($tr).' : '${pres[0].toUpperCase()}${pres.substring(1)} щодня.'
        ),
      ];
    case 'p':
      return [
        (lx.f1, 'Kass on laua ${lx.f1}.', 'Кіт — $tr стола.'),
      ];
    default:
      return const [];
  }
}

/// Слова-переводы, означающие человека или животное (точное слово).
const _personWords = [
  'вчитель', 'кухар', 'майстер', 'таксист', 'програміст', 'клієнт',
  'лікар', 'пацієнт', 'педіатр', 'дерматолог', 'окуліст', 'стоматолог',
  'фізіотерапевт', 'психолог', 'художник', 'студент', 'пенсіонер',
  'пенсіонерка', 'мешканець', 'ріелтор', 'рятувальник', 'бібліотекар',
  'тренер', 'спеціаліст', 'брат', 'сестра', 'дитина', 'мама', 'тато',
  'син', 'донька', 'дочка', 'бабуся', 'дідусь', 'дядько', 'тітка',
  'онук', 'онука', 'немовля', 'друг', 'подруга', 'сусід', 'гість',
  'господиня', 'господар', 'будівельник', 'електрик', 'інженер',
  'працівник', 'фін', 'медсестра', 'бухгалтер', 'фармацевт', 'водій',
  'школяр', 'школярка', 'учениця', 'учень', 'кіт', 'собака', 'папуга',
  'хом’як', 'ведмідь', 'лисиця', 'миша', 'вовк', 'рись', 'заєць',
  'їжак', 'сова', 'черепаха', 'кобра', 'бджола', 'лев', 'доктор',
  'чоловік', 'дружина', 'хлопець', 'дівчина', 'велетень', 'спортсмен',
  'танцюрист', 'фотограф', 'гід', 'логопед', 'людина', 'жінка',
  'малюк', 'дід', 'санта', 'мороз', 'кішка', 'пес', 'родина',
  'сім’я', 'птах', 'тварина', 'цуценя', 'соловей', 'команда',
  'група', 'хор', 'улюбленець', 'близнюки',
];

/// Маркеры переводов: место (фразы «иду в / я в / выхожу из»).
const _placeMarkers = [
  'кімнат', 'кухн', 'будинок', 'школ', 'магазин', 'аптек', 'парк',
  'ліс', 'озеро', 'гора', 'ринок', 'місто', 'село', 'квартир', 'балкон',
  'передпокій', 'ванн', 'спальн', 'вітальн', 'підвал', 'гараж', 'сауна',
  'кафе', 'ресторан', 'лікарн', 'поліклінік', 'садок', 'офіс', 'басейн',
  'терас', 'сад', 'парковка', 'зупинк', 'кіоск', 'салон', 'зал',
  'коридор', 'поверх', 'вулиц', 'майданчик', 'хутір', 'ферм', 'комор',
  'душов', 'центр', 'передмістя', 'Фінлянд', 'Естон', 'Україн',
  'галявин', 'клас', 'море', 'пляж', 'двір', 'реєстратур', 'сцен',
  'бібліотек', 'готел', 'стадіон', 'річка', 'природ', 'фірм',
  'міста', 'дім', 'домівка', 'будівл', 'приміщенн', 'одеса',
  'бердянськ', 'київ', 'таллінн', 'нарва', 'маарду', 'пярну',
  'кейла', 'йихві', 'нимме', 'алея',
];

/// Еда и напитки: фразы про вкус и приготовление.
const _foodMarkers = [
  'сік', 'чай', 'кава', 'суп', 'каша', 'каші', 'печиво',
  'салат', 'мед', 'молок', 'вода', 'морозиво', 'їжа', 'страв',
  'тістечко', 'пиріг', 'фрукт', 'яблук', 'виноград', 'ананас',
  'апельсин', 'помідор', 'рис', 'макарон', 'круасан', 'ласощі',
  'цукор', 'бутерброд', 'хліб', 'сніданок', 'обід', 'вечеря',
  'лимонад', 'сіль', 'банан', 'картопл', 'м’ясо', 'трава',
  'піца', 'вино', 'риба', 'яйце', 'консерв', 'моркв', 'ягод',
  'виноградин', 'зелень',
];

/// Одежда и обувь: фразы про ношение.
const _clothesMarkers = [
  'сукня', 'сорочка', 'футболка', 'блуз', 'джинси', 'штани', 'светр',
  'шарф', 'шапка', 'куртка', 'капц', 'капец', 'туфл', 'шкарпетк',
  'кофтин', 'пальто', 'кепка', 'плаття',
];

/// Части здания: стены, окна, двери — фразы про «на стене, из окна».
const _fixtureMarkers = [
  'стін', 'вікн', 'двері', 'підлог', 'стел', 'сходи', 'дах',
  'ворота',
];

/// Транспорт: фразы «сажусь в, еду на».
const _vehicleMarkers = [
  'автомобіл', 'автобус', 'ліфт',
];

/// Транспорт, на который садятся сверху.
const _rideMarkers = [
  'велосипед', 'скейтборд',
];

/// Волосы, перья и прочее «пушистое».
const _hairyMarkers = [
  'волосся', 'борода', 'вуса', 'перо', 'крило', 'хвіст', 'дзьоб',
  'грива',
];

/// Ёмкости: можно класть внутрь и доставать.
const _containerMarkers = [
  'шафа', 'ящик', 'коробка', 'сумка', 'рюкзак', 'каструля', 'миска',
  'ваза', 'холодильник', 'морозильник', 'пенал', 'глечик', 'склянка',
  'чашка', 'відро', 'каністра', 'акваріум', 'клітка', 'пляшка',
  'кошик', 'комод', 'сковорода', 'духовка', 'кишеня', 'гаманець',
  'тарілка', 'сільничк', 'цукорниц', 'посилка', 'пакунок', 'упаковка',
  'піч',
];

/// Части тела: фразы про крем, боль и царапины.
const _bodyMarkers = [
  'рука', 'обличчя', 'око', 'очі', 'зуб', 'нога', 'голова', 'шия',
  'спина', 'живіт', 'вухо', 'палець', 'коліно',
];

/// Абстрактные понятия, время, погода: «говорим о, думаю о».
const _abstractMarkers = [
  'радість', 'допомог', 'консультац', 'оптимізм', 'песимізм', 'щаст',
  'честь', 'сміливість', 'ввічливість', 'мудрість', 'доброт', 'спокій',
  'мир', 'краса', 'любов', 'розмов', 'свято', 'мова', 'характер',
  'час', 'різниц', 'відстан', 'паркуванн', 'прибиранн', 'в’язанн',
  'біг', 'подорож', 'тренуванн', 'обстеженн', 'прийом', 'майбутн',
  'початок', 'кінець', 'мить', 'ранок', 'вечір', 'день', 'тиждень',
  'рік', 'вихідн', 'дощ', 'сніг', 'вітер', 'сонце', 'хмар', 'небо',
  'погод', 'робота', 'відпочинок', 'спів', 'число', 'урок', 'заняття',
  'музик', 'фільм', 'новин', 'знижк', 'оренд', 'рахунок', 'ніч',
  'ночівл', 'життя', 'вік', 'гроші', 'ціна', 'зарплат', 'здоров',
  'настрій', 'сон', 'весна', 'літо', 'зима', 'осінь', 'січень',
  'лютий', 'березень', 'квітень', 'травень', 'червень', 'липень',
  'серпень', 'вересень', 'жовтень', 'листопад', 'грудень',
  'понеділок', 'вівторок', 'середа', 'четвер', 'п’ятниц', 'субот',
  'неділ', 'відмінок', 'відсутн', 'запитанн', 'відповідь', 'слово',
  'речення', 'текст', 'правда', 'думка', 'ідея', 'хобі', 'спорт',
  'теніс', 'баскетбол', 'футбол', 'гра', 'візит', 'похід', 'зустріч',
  'загадка', 'тест', 'екзамен', 'весілля', 'Різдво', 'уваг',
  'порядкове', 'зберіганн', 'рентген', 'узд', 'товар', 'поїздк',
  'математик', 'фізик', 'історі', 'інформаці', 'мисленн', 'мрія',
  'завданн', 'сума', 'гривн', 'хвилин', 'пауз', 'раз', 'лист',
  'доставк', 'розрахунок', 'повідомленн', 'адрес', 'контакт',
  'зображенн', 'фото', 'голос', 'гороскоп', 'кілограм', 'край',
  'гроз', 'дзюрчанн', 'ліки', 'план', 'мультик', 'пікнік',
  'документ', 'власніст', 'метр', 'євро', 'банкнот', 'платеж',
  'оголошенн', 'нагляд', 'світло', 'дата', 'форма', 'номер',
  'ім’я', 'прізвищ', 'місц', 'черга', 'запереченн', 'відсутніст',
  'читанн', 'фотографі', 'вайфай', 'вхід', 'слух',
];

bool _matchesAny(String tr, List<String> markers) {
  final low = tr.toLowerCase();
  for (final m in markers) {
    if (low.contains(m.toLowerCase())) return true;
  }
  return false;
}

/// Человек/животное: точное совпадение отдельного слова перевода.
bool _isPerson(String tr) {
  final words = tr.toLowerCase().split(RegExp(r'[^а-щьюяіїєґ’]+'));
  for (final w in words) {
    if (w.isNotEmpty && _personWords.contains(w)) return true;
  }
  return false;
}

/// Совпадение по началу слова (не более двух лишних букв в конце),
/// чтобы «рис» не находился внутри «туристична».
bool _wordPrefix(String tr, List<String> markers) {
  final words = tr.toLowerCase().split(RegExp(r'[^а-щьюяіїєґ’]+'));
  for (final w in words) {
    if (w.isEmpty) continue;
    for (final m in markers) {
      if (w == m) return true;
      // короткие маркеры — только точное слово («зуб» ≠ «зубна»)
      if (m.length >= 4 && w.startsWith(m) && w.length - m.length <= 2) {
        return true;
      }
    }
  }
  return false;
}

/// Фразы со всеми падежами (для существительных). Шаблон подбирается
/// по смыслу слова: человек / место / предмет — чтобы не выходило
/// «я иду в сок». (падеж-вопрос, фраза, перевод).
/// Местоимения — падежные фразы-шаблоны для них не строим.
const _noPhraseLemmas = {'see', 'need', 'kes', 'mis'};

List<(String, String, String)> casePhrases(Lexeme lx) {
  if (lx.kind != 'n' || lx.f2.isEmpty) return const [];
  if (_noPhraseLemmas.contains(lx.f1)) return const [];
  final s = lx.f2;
  final gen = _ukrForm(lx.tr, ukrGenPlain);
  final acc = _ukrForm(lx.tr, ukrAccPlain);
  final loc = _ukrForm(lx.tr, ukrLocPlain);
  final ins = _ukrForm(lx.tr, ukrInstrPlain);
  // класс определяем по короткому переводу («колесо; велосипед» → колесо)
  final st = _shortTr(lx.tr);
  final zodiac = lx.tr.contains('(знак');
  final cityNote = lx.tr.contains('(міст') || lx.tr.contains('(район');
  String cls;
  if (zodiac) {
    cls = 'abstract';
  } else if (_isPerson(st)) {
    cls = 'person';
  } else if (cityNote || _matchesAny(st, _placeMarkers)) {
    cls = 'place';
  } else if (_wordPrefix(st, _clothesMarkers)) {
    cls = 'clothes';
  } else if (_wordPrefix(st, _containerMarkers)) {
    cls = 'container';
  } else if (_wordPrefix(st, _foodMarkers)) {
    cls = 'food';
  } else if (_wordPrefix(st, _vehicleMarkers)) {
    cls = 'vehicle';
  } else if (_wordPrefix(st, _rideMarkers)) {
    cls = 'ride';
  } else if (_wordPrefix(st, _hairyMarkers)) {
    cls = 'hairy';
  } else if (_wordPrefix(st, _bodyMarkers)) {
    cls = 'body';
  } else if (_wordPrefix(st, _fixtureMarkers)) {
    cls = 'fixture';
  } else if (_wordPrefix(st, _abstractMarkers)) {
    cls = 'abstract';
  } else {
    cls = 'thing';
  }

  switch (cls) {
    case 'person':
      return [
        ('Kuhu? — kelle juurde (до кого?)', 'Ma lähen $s juurde.',
            'Я йду до $gen.'),
        ('Kus? — kelle juures (у кого?)', 'Ma olen $s juures.',
            'Я у $gen.'),
        ('Kust? — kelle juurest (від кого?)', 'Ma tulen $s juurest.',
            'Я йду від $gen.'),
        ('Alaleütlev — kellele? (кому?)', 'Ma helistan ${s}le.',
            'Я телефоную до $gen.'),
        ('Alalütlev — kellel? (у кого є?)', 'Kas ${s}l on aega?',
            'Чи є у $gen час?'),
        ('Alaltütlev — kellelt? (від кого?)', 'Ma sain kirja ${s}lt.',
            'Я отримав листа від $gen.'),
        ('Kaasaütlev — kellega? (з ким?)', 'Ma räägin ${s}ga.',
            'Я розмовляю з $ins.'),
        ('Ilmaütlev — ilma kelleta? (без кого?)', 'Me läheme ilma ${s}ta.',
            'Ми йдемо без $gen.'),
        ('Saav — kelleks? (ким вважає?)', 'Ta peab mind ${s}ks.',
            'Він вважає мене $ins.'),
        ('Rajav — kelleni? (до кого?)', 'Järjekord jõuab ${s}ni.',
            'Черга доходить до $gen.'),
        ('Olev — kellena? (ким почувається?)', 'Ma tunnen end ${s}na.',
            'Я почуваюся $ins.'),
      ];
    case 'place':
      return [
        ('Sisseütlev — kuhu? (куди?)', 'Ma lähen ${s}sse.', 'Я йду в $acc.'),
        ('Seesütlev — kus? (де?)', 'Ma olen ${s}s.', 'Я в $loc.'),
        ('Seestütlev — kust? (звідки?)', 'Ma tulen ${s}st.',
            'Я виходжу з $gen.'),
        ('Alaleütlev — millele? (до чого?)', 'Ma elan ${s}le väga lähedal.',
            'Я живу дуже близько до $gen.'),
        ('Alalütlev — millel? (у чого?)', '${s}l on oma ajalugu.',
            'У $gen своя історія.'),
        ('Alaltütlev — millelt? (від чого?)', 'Mida sa ${s}lt ootad?',
            'Чого ти чекаєш від $gen?'),
        ('Kaasaütlev — millega? (чим задоволений?)', 'Ma olen ${s}ga rahul.',
            'Я задоволений $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Ilma ${s}ta oleks raske.',
            'Без $gen було б важко.'),
        ('Saav — milleks? (на що перетворюється?)', 'See koht muutub ${s}ks.',
            'Це місце перетворюється на $acc.'),
        ('Rajav — milleni? (до чого?)', 'Ma jalutan ${s}ni.',
            'Я гуляю до $gen.'),
        ('Olev — millena? (як що?)', 'Me kasutame seda kohta ${s}na.',
            'Ми використовуємо це місце як $acc.'),
      ];
    case 'clothes':
      return [
        ('Sisseütlev — millesse? (у що?)', 'Plekk läks ${s}sse sisse.',
            'Пляма в’їлася в $acc.'),
        ('Seesütlev — milles? (у чому?)', 'Ta on täna ${s}s.',
            'Він/вона сьогодні в $loc.'),
        ('Seestütlev — millest? (з чого?)', 'Plekk läks ${s}st välja.',
            'Пляма зійшла з $gen.'),
        ('Alaleütlev — mille peale? (на що?)', 'Tilk vett kukkus ${s}le.',
            'Крапля води впала на $acc.'),
        ('Alalütlev — millel? (у чого?)', '${s}l on ilus värv.',
            'У $gen гарний колір.'),
        ('Alaltütlev — millelt? (з чого?)', 'Ma harjan tolmu ${s}lt.',
            'Я зчищаю пил з $gen.'),
        ('Kaasaütlev — millega? (з чим пасує?)', 'See sobib ${s}ga kokku.',
            'Це пасує до $gen.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Ma tulen täna ${s}ta.',
            'Я прийду сьогодні без $gen.'),
        ('Saav — milleks? (чим стане?)', 'Materjal sai ${s}ks.',
            'Матеріал став $ins.'),
        ('Rajav — milleni? (до чого?)', 'Ma ulatun ${s}ni.',
            'Я дотягуюся до $gen.'),
        ('Olev — millena? (як що?)', 'Me kasutame seda ${s}na.',
            'Ми використовуємо це як $acc.'),
      ];
    case 'food':
      return [
        ('Sisseütlev — millesse? (до чого?)', 'Ta suhtub ${s}sse hästi.',
            'Він добре ставиться до $gen.'),
        ('Seesütlev — milles? (у чому?)', 'Mis ${s}s on?', 'Що є в $loc?'),
        ('Seestütlev — millest? (про що?)', 'Me räägime ${s}st.',
            'Ми говоримо про $acc.'),
        ('Alaleütlev — millele? (про що?)', 'Ma mõtlen ${s}le.',
            'Я думаю про $acc.'),
        ('Alalütlev — millel? (у чого?)', '${s}l on hea maitse.',
            'У $gen гарний смак.'),
        ('Alaltütlev — millelt? (від чого?)', 'See lõhn tuleb ${s}lt.',
            'Цей запах іде від $gen.'),
        ('Kaasaütlev — millega? (чим задоволений?)', 'Ma olen ${s}ga rahul.',
            'Я задоволений $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Ma saan ${s}ta hakkama.',
            'Я обходжуся без $gen.'),
        ('Saav — milleks? (чим вважає?)', 'Ma pean seda ${s}ks.',
            'Я вважаю це $ins.'),
        ('Rajav — milleni? (до чого?)', 'Jutt jõudis ${s}ni.',
            'Розмова дійшла до $gen.'),
        ('Olev — millena? (як що?)', 'See tundub ${s}na.',
            'Це здається $ins.'),
      ];
    case 'vehicle':
      return [
        ('Sisseütlev — kuhu? (куди?)', 'Ma istun ${s}sse.',
            'Я сідаю в $acc.'),
        ('Seesütlev — kus? (де?)', 'Ma olen ${s}s.', 'Я в $loc.'),
        ('Seestütlev — kust? (звідки?)', 'Ma tulen ${s}st välja.',
            'Я виходжу з $gen.'),
        ('Alaleütlev — millele? (на що?)', 'Ma jään ${s}le hiljaks.',
            'Я запізнююся на $acc.'),
        ('Alalütlev — millel? (у чого?)', '${s}l on oma graafik.',
            'У $gen свій розклад.'),
        ('Alaltütlev — millelt? (від чого?)', 'Mida sa ${s}lt ootad?',
            'Чого ти чекаєш від $gen?'),
        ('Kaasaütlev — millega? (чим їду?)', 'Ma sõidan ${s}ga.',
            'Я їду $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Ma saan ${s}ta hakkama.',
            'Я обходжуся без $gen.'),
        ('Saav — milleks? (чим вважає?)', 'Ma pean seda ${s}ks.',
            'Я вважаю це $ins.'),
        ('Rajav — milleni? (до чого?)', 'Ma jalutan ${s}ni.',
            'Я йду пішки до $gen.'),
        ('Olev — millena? (як що?)', 'Me kasutame seda ${s}na.',
            'Ми використовуємо це як $acc.'),
      ];
    case 'ride':
      return [
        ('Sisseütlev — millesse? (у що?)', 'Ta on ${s}sse armunud.',
            'Він закоханий у $acc.'),
        ('Seesütlev — milles? (у чому?)', 'Asi on ${s}s.',
            'Справа в $loc.'),
        ('Seestütlev — millest? (про що?)', 'Me räägime ${s}st.',
            'Ми говоримо про $acc.'),
        ('Alaleütlev — mille peale? (на що?)', 'Ma istun ${s}le.',
            'Я сідаю на $acc.'),
        ('Alalütlev — millel? (на чому?)', 'Ma sõidan ${s}l.',
            'Я їду на $loc.'),
        ('Alaltütlev — millelt? (з чого?)', 'Ma tulen ${s}lt maha.',
            'Я злажу з $gen.'),
        ('Kaasaütlev — millega? (чим їду?)', 'Ma sõidan ${s}ga.',
            'Я їду $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Ma saan ${s}ta hakkama.',
            'Я обходжуся без $gen.'),
        ('Saav — milleks? (чим вважає?)', 'Ma pean seda ${s}ks.',
            'Я вважаю це $ins.'),
        ('Rajav — milleni? (до чого?)', 'Ma jalutan ${s}ni.',
            'Я йду пішки до $gen.'),
        ('Olev — millena? (як що?)', 'Me kasutame seda ${s}na.',
            'Ми використовуємо це як $acc.'),
      ];
    case 'body':
      return [
        ('Sisseütlev — kuhu? (у що?)', 'Valu kiirgub ${s}sse.',
            'Біль віддає в $acc.'),
        ('Seesütlev — kus? (у чому?)', '${s}s on valu.', 'У $loc біль.'),
        ('Seestütlev — millest? (з чого?)', 'Valu kadus ${s}st.',
            'Біль зник з $gen.'),
        ('Alaleütlev — mille peale? (на що?)', 'Ma panen kreemi ${s}le.',
            'Я наношу крем на $acc.'),
        ('Alalütlev — millel? (на чому?)', '${s}l on väike kriips.',
            'На $loc маленька подряпина.'),
        ('Alaltütlev — millelt? (з чого?)', 'Ma pühin vee ${s}lt ära.',
            'Я витираю воду з $gen.'),
        ('Kaasaütlev — millega? (з чим?)', 'Tal on ${s}ga probleeme.',
            'У нього проблеми з $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Ilma ${s}ta on raske.',
            'Без $gen важко.'),
        ('Rajav — milleni? (до чого?)', 'Vesi ulatub ${s}ni.',
            'Вода сягає до $gen.'),
      ];
    case 'hairy':
      return [
        ('Sisseütlev — kuhu? (у що?)', 'Tuul puhub ${s}sse.',
            'Вітер дме у $acc.'),
        ('Seesütlev — kus? (у чому?)', '${s}s on midagi.',
            'У $loc щось є.'),
        ('Seestütlev — millest? (з чого?)', 'Vesi tilgub ${s}st.',
            'Вода капає з $gen.'),
        ('Alaleütlev — mille peale? (на що?)', 'Lumi langeb ${s}le.',
            'Сніг падає на $acc.'),
        ('Alalütlev — millel? (у чого?)', '${s}l on ilus värv.',
            'У $gen гарний колір.'),
        ('Alaltütlev — millelt? (з чого?)', 'Ma raputan vee ${s}lt.',
            'Я струшую воду з $gen.'),
        ('Kaasaütlev — millega? (чим задоволений?)', 'Ta on ${s}ga rahul.',
            'Він задоволений $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)',
            'Ilma ${s}ta näeks ta teistsugune välja.',
            'Без $gen він виглядав би інакше.'),
        ('Rajav — milleni? (до чого?)', 'Vesi ulatub ${s}ni.',
            'Вода сягає до $gen.'),
      ];
    case 'fixture':
      return [
        ('Sisseütlev — kuhu? (у що?)', 'Nael läks ${s}sse.',
            'Цвях увійшов у $acc.'),
        ('Seesütlev — kus? (у чому?)', '${s}s on väike auk.',
            'У $loc маленька дірка.'),
        ('Seestütlev — kust? (звідки?)', 'Kummaline heli tuli ${s}st.',
            'Дивний звук ішов з $gen.'),
        ('Alaleütlev — mille peale? (на що?)', 'Vesi voolas ${s}le.',
            'Вода налилася на $acc.'),
        ('Alalütlev — millel? (на чому?)', '${s}l on kriimustus.',
            'На $loc подряпина.'),
        ('Alaltütlev — millelt? (з чого?)', 'Ma pühin ${s}lt tolmu.',
            'Я витираю пил з $gen.'),
        ('Kaasaütlev — millega? (чим задоволений?)', 'Ma olen ${s}ga rahul.',
            'Я задоволений $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Tuba on ${s}ta.',
            'Кімната без $gen.'),
        ('Saav — milleks? (чим стає?)', 'See muutub ${s}ks.',
            'Це перетворюється на $acc.'),
        ('Rajav — milleni? (до чого?)', 'Kapp ulatub ${s}ni.',
            'Шафа сягає $gen.'),
        ('Olev — millena? (як що?)', 'Me kasutame seda ${s}na.',
            'Ми використовуємо це як $acc.'),
      ];
    case 'abstract':
      return [
        ('Sisseütlev — millesse? (у що?)', 'Ma usun ${s}sse.',
            'Я вірю в $acc.'),
        ('Seesütlev — milles? (у чому?)', 'Ma olen ${s}s kindel.',
            'Я впевнений у $loc.'),
        ('Seestütlev — millest? (про що?)', 'Me räägime ${s}st.',
            'Ми говоримо про $acc.'),
        ('Alaleütlev — millele? (про що?)', 'Ma mõtlen ${s}le.',
            'Я думаю про $acc.'),
        ('Alalütlev — millel? (у чого?)', '${s}l on tähtis roll.',
            'У $gen важлива роль.'),
        ('Alaltütlev — millelt? (від чого?)', 'Ma ootan ${s}lt palju.',
            'Я чекаю від $gen багато.'),
        ('Kaasaütlev — millega? (чим задоволений?)', 'Ma olen ${s}ga rahul.',
            'Я задоволений $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Elu ${s}ta on raske.',
            'Життя без $gen важке.'),
        ('Saav — milleks? (на що перетворюється?)', 'See muutub ${s}ks.',
            'Це перетворюється на $acc.'),
        ('Rajav — milleni? (до чого?)', 'Me jõuame ${s}ni.',
            'Ми доходимо до $gen.'),
        ('Olev — millena? (як що?)', 'See tundub ${s}na.',
            'Це здається $ins.'),
      ];
    case 'container':
      return [
        ('Sisseütlev — kuhu? (куди? у що?)', 'Ma panen selle ${s}sse.',
            'Я кладу це в $acc.'),
        ('Seesütlev — kus? (у чому?)', 'Mis on ${s}s?', 'Що в $loc?'),
        ('Seestütlev — millest? (з чого?)', 'Ma võtan selle ${s}st.',
            'Я беру це з $gen.'),
        ('Alaleütlev — mille peale? (на що?)', 'Ma panen selle ${s}le.',
            'Я кладу це на $acc.'),
        ('Alalütlev — millel? (у чого?)', '${s}l on oma koht.',
            'У $gen є своє місце.'),
        ('Alaltütlev — millelt? (з чого?)', 'Ma võtan selle ${s}lt.',
            'Я беру це з $gen.'),
        ('Kaasaütlev — millega? (чим задоволений?)', 'Ma olen ${s}ga rahul.',
            'Я задоволений $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Ma saan ${s}ta hakkama.',
            'Я обходжуся без $gen.'),
        ('Saav — milleks? (чим вважає?)', 'Ma pean seda ${s}ks.',
            'Я вважаю це $ins.'),
        ('Rajav — milleni? (до чого?)', 'Ma ulatun ${s}ni.',
            'Я дотягуюся до $gen.'),
        ('Olev — millena? (як що?)', 'Me kasutame seda ${s}na.',
            'Ми використовуємо це як $acc.'),
      ];
    default:
      return [
        ('Sisseütlev — millesse? (до чого?)', 'Ma suhtun ${s}sse hoolikalt.',
            'Я ставлюся до $gen дбайливо.'),
        ('Seesütlev — milles? (у чому?)', 'Asi on ${s}s.', 'Справа в $loc.'),
        ('Seestütlev — millest? (про що?)', 'Me räägime ${s}st.',
            'Ми говоримо про $acc.'),
        ('Alaleütlev — millele? (про що?)', 'Ma mõtlen ${s}le.',
            'Я думаю про $acc.'),
        ('Alalütlev — millel? (у чого?)', '${s}l on oma koht.',
            'У $gen є своє місце.'),
        ('Alaltütlev — millelt? (з чого?)', 'Ma pühin ${s}lt tolmu.',
            'Я витираю пил з $gen.'),
        ('Kaasaütlev — millega? (чим задоволений?)', 'Ma olen ${s}ga rahul.',
            'Я задоволений $ins.'),
        ('Ilmaütlev — ilma milleta? (без чого?)', 'Ma saan ${s}ta hakkama.',
            'Я обходжуся без $gen.'),
        ('Saav — milleks? (чим вважає?)', 'Ma pean seda ${s}ks.',
            'Я вважаю це $ins.'),
        ('Rajav — milleni? (до чого?)', 'Ma ulatun ${s}ni.',
            'Я дотягуюся до $gen.'),
        ('Olev — millena? (як що?)', 'Me kasutame seda ${s}na.',
            'Ми використовуємо це як $acc.'),
      ];
  }
}

/// Все варианты разбора слова — для омонимов (maal = «картина»
/// или maa + -l «у селі»). Первый элемент совпадает с [analyze].
List<WordAnalysis> analyzeAll(String raw) {
  final t = _norm(raw);
  if (t.isEmpty) return const [];
  final out = <WordAnalysis>[];
  final irr = _irregular[t];
  if (irr != null) {
    final lx = _byLemma(irr.$1);
    if (lx != null) out.add(WordAnalysis(lx, irr.$2, irr.$3));
  }
  for (final lx in lexicon) {
    final r = _match(lx, t);
    if (r != null && !out.any((w) => identical(w.lex, r.lex))) out.add(r);
  }
  if (out.isEmpty) {
    final a = analyze(raw);
    if (a != null) out.add(a);
  }
  return out;
}

/// Основные формы леммы и правила, КОГДА каждая используется (для панели слова).
/// Формы слова с правилами употребления и переводом каждой формы
/// (перевод — в соответствующем украинском падеже, '' если формы нет
/// в словаре склонений).
List<(String, String, String, String)> formUsage(Lexeme lx) {
  switch (lx.kind) {
    case 'n':
    case 'a':
      final u = <(String, String, String, String)>[
        (
          lx.f1,
          '1. Nimetav — kes? mis? (хто? що?)',
          'Словарная форма — называет предмет. Подлежащее в предложении: '
              '«${lx.f1} on siin» — «${lx.tr} тут».',
          lx.tr
        ),
        (
          lx.f2,
          '2. Omastav — kelle? mille? (чий? чого?)',
          'Принадлежность («${lx.f2} + что-то» = «чей?») и ОСНОВА для всех '
              'падежей: ${lx.f2}+s «в», ${lx.f2}+st «из», ${lx.f2}+sse «в (куда)», '
              '${lx.f2}+l «на/у», ${lx.f2}+le «на/к», ${lx.f2}+lt «с/от», '
              '${lx.f2}+ga «с», ${lx.f2}+ta «без», ${lx.f2}+na «в роли». '
              'Мн. число: ${lx.f2}+d.',
          ukrGenPlain(lx.tr)
        ),
        (
          lx.f3,
          '3. Osastav — keda? mida? (кого? що?)',
          'Частичный падеж: после чисел (kaks ${lx.f3}, viis ${lx.f3}), '
              'при отрицании (ei ole ${lx.f3} — «нет …») и как частичный '
              'объект действия.',
          ukrAccPlain(lx.tr)
        ),
      ];
      if (lx.kind == 'a') {
        u.add((
          '${lx.f2}m',
          'Keskvõrre — сравнительная степень',
          'omastav + -m: ${lx.f2}m — «более ${lx.tr}». '
              'Например: see on ${lx.f2}m.',
          ''
        ));
      }
      return u;
    case 'v':
      return [
        (
          lx.f1,
          '1. ma-infinitiiv',
          'Употребляется после pean (должен), hakkan (начну), lähen (иду): '
              '«Ma pean ${lx.f1}», «Ma lähen ${lx.f1}».',
          lx.tr
        ),
        (
          lx.f2,
          '2. da-infinitiiv',
          'Употребляется после tahan (хочу), oskan (умею), armastan (люблю), '
              'mulle meeldib (мне нравится): «Ma tahan ${lx.f2}».',
          lx.tr
        ),
        (
          '${lx.f3}n',
          '3. Olevik — настоящее время (основа ${lx.f3}-)',
          'Основа + личные окончания: ma ${lx.f3}n, sa ${lx.f3}d, '
              'ta ${lx.f3}b, me ${lx.f3}me, te ${lx.f3}te, nad ${lx.f3}vad. '
              'Отрицание: ma ei ${lx.f3}. Повеление: ${lx.f3}! '
              'Прошедшее: ${lx.f3}sin (я), ${lx.f3}s (он).',
          ukrPres1Plain(lx.tr)
        ),
      ];
    case 'p':
      return [
        (
          lx.f1,
          'Послелог места',
          'Ставится ПОСЛЕ слова в omastav: maja ${lx.f1} — «${lx.tr}». '
              'Например: laua ${lx.f1}, kapi ${lx.f1}.',
          ''
        ),
      ];
    default:
      return [
        (
          lx.f1,
          'Служебное слово',
          'Не изменяется по падежам. Значение: ${lx.tr}.',
          ''
        ),
      ];
  }
}
