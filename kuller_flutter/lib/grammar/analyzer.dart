import 'lexicon.dart';

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
  'd': ('Mitmuse nimetav (кто? что? мн.ч.)',
      'множественное число: omastav + -d'),
  'de': ('Mitmuse omastav (кого? чего? мн.ч.)',
      'родительный мн.ч.: omastav + -de'),
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

String _norm(String raw) {
  var t = raw.toLowerCase().trim();
  t = t.replaceAll(RegExp(r'''[.,!?:;„“"«»()\[\]…—–]'''), '');
  return t.trim();
}

WordAnalysis? _match(Lexeme lx, String t) {
  switch (lx.kind) {
    case 'n':
    case 'a':
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
      // omastav + падежное окончание (сначала длинные окончания)
      final endings = _caseEndings.keys.toList()
        ..sort((a, b) => b.length.compareTo(a.length));
      for (final e in endings) {
        if (t == lx.f2 + e) {
          final info = _caseEndings[e]!;
          return WordAnalysis(lx, info.$1,
              '${lx.f2} (omastav) + -$e. ${info.$2}. Три формы: ${lx.f1} — ${lx.f2} — ${lx.f3}.');
        }
        // мн. число: omastav + de + окончание (напр. riiulitele упрощённо не разбираем)
      }
      // короткий illatiiv (tuppa, koju и т.п.) не разбираем — не найдено
      return null;
    case 'v':
      if (t == lx.f1) {
        return WordAnalysis(lx, 'ma-infinitiiv',
            'ma-инфинитив. Употребляется после pean, hakkan, lähen: Ma lähen ${lx.f1}.');
      }
      if (t == lx.f2) {
        return WordAnalysis(lx, 'da-infinitiiv',
            'da-инфинитив. Употребляется после tahan, armastan, oskan, meeldib: Ma tahan ${lx.f2}.');
      }
      if (t == lx.f3) {
        return WordAnalysis(lx, 'Käskiv kõneviis (sina!)',
            'Повелительное наклонение ед.ч. — чистая основа настоящего времени: ${lx.f3}!');
      }
      final vEndings = _verbEndings.keys.toList()
        ..sort((a, b) => b.length.compareTo(a.length));
      for (final e in vEndings) {
        if (t == lx.f3 + e) {
          final info = _verbEndings[e]!;
          return WordAnalysis(lx, info.$1,
              '${lx.f3}- + -$e. ${info.$2}. Формы глагола: ${lx.f1} / ${lx.f2} / ${lx.f3}n.');
        }
      }
      return null;
    case 'p':
      if (t == lx.f1) {
        return WordAnalysis(lx, 'Послелог места',
            'Ставится ПОСЛЕ слова в omastav: maja ${lx.f1} («${lx.tr}»).');
      }
      return null;
    default: // 'x'
      if (t == lx.f1 || (lx.f2.isNotEmpty && t == lx.f2) ||
          (lx.f3.isNotEmpty && t == lx.f3)) {
        return WordAnalysis(lx, 'Служебное слово',
            lx.f2.isNotEmpty && lx.f2 != lx.f1
                ? 'Полная форма — ${lx.f1}, краткая — ${lx.f2}.'
                : 'Не изменяется по падежам.');
      }
      return null;
  }
}

/// Разбор слова: ищем лемму, форму которой представляет собой [raw].
WordAnalysis? analyze(String raw) {
  final t = _norm(raw);
  if (t.isEmpty) return null;
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
    if (lx.kind != 'n' && lx.kind != 'a') continue;
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
