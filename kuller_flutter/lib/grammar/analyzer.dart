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

String _or(String form, String fallback) => form.isEmpty ? fallback : form;

/// Фразы-шаблоны: каждая основная форма слова в предложении + перевод.
/// Возвращает (форма, эстонская фраза, украинский перевод).
List<(String, String, String)> formPhrases(Lexeme lx) {
  final tr = _shortTr(lx.tr);
  final gen = _or(ukrGenPlain(lx.tr), tr);
  final acc = _or(ukrAccPlain(lx.tr), tr);
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

/// Фразы со всеми падежами (для существительных): основа omastav +
/// окончание, украинский перевод фразы. (падеж-вопрос, фраза, перевод).
List<(String, String, String)> casePhrases(Lexeme lx) {
  if (lx.kind != 'n' || lx.f2.isEmpty) return const [];
  final s = lx.f2;
  final tr = _shortTr(lx.tr);
  final gen = _or(ukrGenPlain(lx.tr), tr);
  final acc = _or(ukrAccPlain(lx.tr), tr);
  final loc = _or(ukrLocPlain(lx.tr), tr);
  final ins = _or(ukrInstrPlain(lx.tr), tr);
  return [
    ('Sisseütlev — kuhu? (куди?)', 'Ma lähen ${s}sse.', 'Я йду в $acc.'),
    ('Seesütlev — kus? (де?)', 'Ma olen ${s}s.', 'Я в $loc.'),
    ('Seestütlev — kust? (звідки?)', 'Ma tulen ${s}st.', 'Я йду з $gen.'),
    ('Alaleütlev — kuhu peale? (на що?)', 'Ma panen selle ${s}le.',
        'Я кладу це на $acc.'),
    ('Alalütlev — kus? (на чому?)', 'See on ${s}l.', 'Це на $loc.'),
    ('Alaltütlev — millelt? (з чого?)', 'Ma võtan selle ${s}lt.',
        'Я беру це з $gen.'),
    ('Kaasaütlev — kellega? (з ким? з чим?)', 'Ma olen koos ${s}ga.',
        'Я разом з $ins.'),
    ('Ilmaütlev — ilma milleta? (без чого?)', 'Ma olen ilma ${s}ta.',
        'Я без $gen.'),
    ('Saav — kelleks? (ким? чим стає?)', 'Ta saab ${s}ks.',
        'Він стає $ins.'),
    ('Rajav — milleni? (до чого?)', 'Ma jõuan ${s}ni.',
        'Я доходжу до $gen.'),
    ('Olev — kellena? (в ролі кого?)', 'Ta töötab ${s}na.',
        'Він працює як $tr.'),
  ];
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
