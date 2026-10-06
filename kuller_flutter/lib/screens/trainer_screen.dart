import 'dart:math';

import 'package:flutter/material.dart';

import '../grammar/analyzer.dart';
import '../grammar/ukr_forms.dart';
import '../learn_vm.dart';
import '../models.dart';

/// Одна карточка тренировки: форма слова или фраза с этим словом.
class _Drill {
  final WordCard word; // родительская карточка урока (ключ повторений)
  final String et; // что показываем: форма или предложение
  final String formLabel; // подпись формы ('' — без подписи)
  final String tr; // перевод, показывается после «Показать»
  final String highlight; // токен, выделяемый в фразе ('' — не выделять)
  final bool isPhrase;
  final String exEt; // пример употребления формы (эстонский)
  final String exTr; // перевод примера
  const _Drill(this.word, this.et, this.formLabel, this.tr,
      {this.highlight = '', this.isPhrase = false,
      this.exEt = '', this.exTr = ''});
}

/// Тренажёр слов урока: этап 1 — три основные формы, этап 2 — те же
/// слова в фразах из урока. Каждое открытие перевода = +1 повторение;
/// после 150 повторений слово выучено и перевод больше не показывается.
class TrainerScreen extends StatefulWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const TrainerScreen({super.key, required this.vm, required this.lesson});

  @override
  State<TrainerScreen> createState() => _TrainerScreenState();
}

class _TrainerScreenState extends State<TrainerScreen> {
  late List<_Drill> _deck;
  late int _phraseStart; // индекс первой карточки этапа 2
  int _index = 0;
  bool _revealed = false;
  int _sessionReps = 0;

  @override
  void initState() {
    super.initState();
    _deck = _buildDeck();
    _phraseStart = _deck.indexWhere((d) => d.isPhrase);
    if (_phraseStart < 0) _phraseStart = _deck.length;
    // озвучиваем первую карточку после построения экрана
    WidgetsBinding.instance.addPostFrameCallback((_) => _speakCurrent());
  }

  static final _punct = RegExp(r'''[.,!?:;„“"«»()\[\]…—–]''');
  String _clean(String s) => s.replaceAll(_punct, '').trim();

  /// Есть ли у переводов общий корень (первые 4-5 букв слова)?
  /// Нужно, чтобы у омонимов выбрать лемму по смыслу карточки:
  /// maal «у селі» -> лемма maa (село), а не maal (картина).
  static bool _trOverlap(String a, String b) {
    final split = RegExp(r'[^а-щьюяіїєґ’]+');
    final wa = a.toLowerCase().split(split);
    final wb = b.toLowerCase().split(split);
    for (final x in wa) {
      if (x.length < 4) continue;
      for (final y in wb) {
        if (y.length < 4) continue;
        final k = (x.length >= 6 && y.length >= 6) ? 5 : 4;
        if (x.substring(0, k) == y.substring(0, k)) return true;
      }
    }
    return false;
  }

  /// Разбор слова карточки с учётом её перевода (выбор среди омонимов).
  WordAnalysis? _analyzeForCard(String token, String cardTr) {
    final all = analyzeAll(token);
    if (all.isEmpty) return null;
    for (final cand in all) {
      if (_trOverlap(cand.lex.tr, cardTr)) return cand;
    }
    return all.first;
  }

  bool _isLetter(String ch) =>
      RegExp(r'[a-zõäöüšž]', caseSensitive: false).hasMatch(ch);

  /// Ищет форму слова в предложении как отдельный токен.
  /// Возвращает найденный кусок оригинала или ''.
  String _findToken(String sentence, String form) {
    final low = sentence.toLowerCase();
    final f = form.toLowerCase();
    var from = 0;
    while (true) {
      final i = low.indexOf(f, from);
      if (i < 0) return '';
      final okBefore = i == 0 || !_isLetter(low[i - 1]);
      final after = i + f.length;
      final okAfter = after >= low.length || !_isLetter(low[after]);
      if (okBefore && okAfter) return sentence.substring(i, after);
      from = i + 1;
    }
  }

  List<_Drill> _buildDeck() {
    final lesson = widget.lesson;
    final words = <_Drill>[];
    final phrases = <_Drill>[];

    // Пул предложений урока (тексты + диалоги) — только материал из курса.
    final pool = <Sent>[
      for (final t in lesson.texts) ...t.paras,
      for (final t in lesson.grammar) ...t.paras,
      for (final d in lesson.dialogues)
        for (final turn in d.turns) ...[
          if (turn is DSay) Sent(turn.et, turn.tr),
          if (turn is DAsk)
            for (final c in turn.options) Sent(c.et, c.tr),
        ],
    ];

    final order = List.of(lesson.words)..shuffle(Random());
    for (final w in order) {
      final clean = _clean(w.et);
      final single = !clean.contains(' ');
      final a = single ? _analyzeForCard(clean.toLowerCase(), w.tr) : null;
      final lx = a?.lex;
      // Перевод форм — словарный перевод ЛЕММЫ (карточка может быть
      // производной формой: suhkruta «без цукру» -> лемма suhkur «цукор»).
      final baseTr = lx?.tr ?? w.tr;
      final isBaseForm = lx == null ||
          clean.toLowerCase() == lx.f1 ||
          clean.toLowerCase() == lx.f2 ||
          clean.toLowerCase() == lx.f3;

      // Пример употребления формы: сначала настоящее предложение урока,
      // иначе — сгенерированная фраза с переводом (formPhrases).
      (String, String) exampleFor(String form) {
        for (final s in pool) {
          if (_findToken(s.et, form).isNotEmpty) return (s.et, s.tr);
        }
        if (lx != null) {
          for (final p in formPhrases(lx)) {
            if (p.$1 == form) return (p.$2, p.$3);
          }
        }
        return ('', '');
      }

      // ---- Этап 1: формы слова ----
      if (lx != null && (lx.kind == 'n' || lx.kind == 'a')) {
        final (e1, t1) = exampleFor(lx.f1);
        words.add(_Drill(w, lx.f1, '1-я форма · Nimetav — kes? mis?', baseTr,
            exEt: e1, exTr: t1));
        if (lx.f2.isNotEmpty && lx.f2 != lx.f1) {
          final (e2, t2) = exampleFor(lx.f2);
          words.add(_Drill(
              w, lx.f2, '2-я форма · Omastav — kelle? mille?',
              ukrGen(baseTr), exEt: e2, exTr: t2));
        }
        if (lx.f3.isNotEmpty && lx.f3 != lx.f2) {
          final (e3, t3) = exampleFor(lx.f3);
          words.add(_Drill(
              w, lx.f3, '3-я форма · Osastav — keda? mida?',
              ukrAcc(baseTr), exEt: e3, exTr: t3));
        }
        if (!isBaseForm) {
          // сама карточка — производная форма со своим переводом
          var (eD, tD) = exampleFor(clean);
          if (eD.isEmpty && w.example.isNotEmpty) {
            eD = w.example;
            tD = '';
          }
          words.add(_Drill(w, clean, a!.formName, w.tr, exEt: eD, exTr: tD));
        }
      } else if (lx != null && lx.kind == 'v') {
        final (e1, t1) = exampleFor(lx.f1);
        words.add(_Drill(
            w, lx.f1, 'ma-инфинитив — после pean, hakkan, lähen', baseTr,
            exEt: e1, exTr: t1));
        if (lx.f2.isNotEmpty && lx.f2 != lx.f1) {
          final (e2, t2) = exampleFor(lx.f2);
          words.add(_Drill(
              w, lx.f2, 'da-инфинитив — после tahan, oskan, meeldib',
              baseTr, exEt: e2, exTr: t2));
        }
        if (lx.f3.isNotEmpty) {
          final (e3, t3) = exampleFor('${lx.f3}n');
          words.add(_Drill(
              w, '${lx.f3}n', 'настоящее время — ma …n', ukrPres1(baseTr),
              exEt: e3, exTr: t3));
        }
        if (!isBaseForm) {
          var (eD, tD) = exampleFor(clean);
          if (eD.isEmpty && w.example.isNotEmpty) {
            eD = w.example;
            tD = '';
          }
          words.add(_Drill(w, clean, a!.formName, w.tr, exEt: eD, exTr: tD));
        }
      } else {
        var eX = '', tX = '';
        if (single) {
          (eX, tX) = exampleFor(clean);
        } else {
          final lowEt = clean.toLowerCase();
          for (final s in pool) {
            if (s.et.toLowerCase().contains(lowEt)) {
              eX = s.et;
              tX = s.tr;
              break;
            }
          }
        }
        if (eX.isEmpty && w.example.isNotEmpty) eX = w.example;
        // пример, совпадающий с самой карточкой, не показываем
        if (_clean(eX).toLowerCase() == clean.toLowerCase()) eX = '';
        words.add(_Drill(w, w.et, single ? '' : 'выражение', w.tr,
            exEt: eX, exTr: eX.isEmpty ? '' : tX));
      }

      // ---- Этап 2: слово в контексте ----
      final forms = <String>{};
      if (lx != null) {
        forms.addAll([lx.f1, lx.f2, lx.f3, lx.pl]
            .where((f) => f.isNotEmpty && f.length >= 3));
      }
      forms.add(_clean(w.et).toLowerCase());
      var added = 0;
      for (final s in pool) {
        if (added >= 2) break;
        for (final f in forms) {
          final hit = _findToken(s.et, f);
          if (hit.isNotEmpty) {
            phrases.add(_Drill(w, s.et, '', s.tr,
                highlight: hit, isPhrase: true));
            added++;
            break;
          }
        }
      }
      if (added == 0 && w.example.isNotEmpty) {
        phrases.add(_Drill(w, w.example, '', '',
            highlight:
                lx == null ? '' : _findToken(w.example, lx.f1), isPhrase: true));
      }
    }
    return [...words, ...phrases];
  }

  void _speakCurrent() {
    if (_index < _deck.length) widget.vm.speakWord(_deck[_index].et);
  }

  void _reveal() {
    final d = _deck[_index];
    widget.vm.bumpRep(d.word.et);
    setState(() {
      _revealed = true;
      _sessionReps++;
    });
    widget.vm.speakWord(d.et);
  }

  void _next() {
    setState(() {
      _index++;
      _revealed = false;
    });
    _speakCurrent();
  }

  void _restart() {
    setState(() {
      _deck = _buildDeck();
      _phraseStart = _deck.indexWhere((d) => d.isPhrase);
      if (_phraseStart < 0) _phraseStart = _deck.length;
      _index = 0;
      _revealed = false;
      _sessionReps = 0;
    });
    _speakCurrent();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final vm = widget.vm;
    final done = _index >= _deck.length;
    final stage2 = !done && _index >= _phraseStart;

    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      appBar: AppBar(
        backgroundColor: scheme.primary,
        foregroundColor: scheme.onPrimary,
        title: Text('🏋️ Тренировка · ${widget.lesson.title}',
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
      ),
      body: done ? _buildDone(scheme) : _buildCard(scheme, vm, stage2),
    );
  }

  Widget _buildCard(ColorScheme scheme, LearnViewModel vm, bool stage2) {
    final d = _deck[_index];
    final mastered = vm.isWordLearned(d.word.et);
    final reps = vm.repsOf(d.word.et);

    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            stage2
                ? 'Этап 2 · Слова в фразах'
                : 'Этап 1 · Основные формы слов',
            textAlign: TextAlign.center,
            style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 14,
                color: scheme.primary),
          ),
          const SizedBox(height: 6),
          LinearProgressIndicator(
            value: (_index + 1) / _deck.length,
            minHeight: 6,
            borderRadius: BorderRadius.circular(3),
          ),
          const SizedBox(height: 4),
          Text('${_index + 1} / ${_deck.length}',
              textAlign: TextAlign.center,
              style: TextStyle(
                  fontSize: 11, color: scheme.onSurface.withOpacity(0.5))),
          const Spacer(),
          Card(
            elevation: 2,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            color: scheme.surface,
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  if (d.formLabel.isNotEmpty)
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: scheme.primaryContainer.withOpacity(0.6),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(d.formLabel,
                          textAlign: TextAlign.center,
                          style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w600,
                              color: scheme.onSurface)),
                    ),
                  const SizedBox(height: 14),
                  _etText(d, scheme),
                  const SizedBox(height: 14),
                  if (_revealed) ...[
                    Divider(color: scheme.onSurface.withOpacity(0.1)),
                    const SizedBox(height: 8),
                    if (mastered)
                      Text('✓ выучено (150+ повторений) — перевод скрыт',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 14,
                              color: scheme.primary))
                    else ...[
                      if (d.tr.isNotEmpty)
                        Text(d.tr,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.w600,
                                color: scheme.onSurface.withOpacity(0.85))),
                      if (d.isPhrase)
                        Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text(
                              '${d.word.et} — ${d.word.tr}',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                  fontSize: 13, color: scheme.primary)),
                        ),
                    ],
                    if (!d.isPhrase && d.exEt.isNotEmpty) ...[
                      const SizedBox(height: 10),
                      _exampleBlock(d, scheme, vm, mastered),
                    ],
                    const SizedBox(height: 6),
                    Text(
                        mastered
                            ? '🏆'
                            : 'повторений: $reps/${LearnViewModel.learnThreshold}',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                            fontSize: 11,
                            color: scheme.onSurface.withOpacity(0.5))),
                  ] else
                    Text(
                        d.isPhrase
                            ? 'Прочитайте фразу и вспомните выделенное слово'
                            : 'Вспомните перевод, затем проверьте себя',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                            fontSize: 12.5,
                            color: scheme.onSurface.withOpacity(0.5))),
                ],
              ),
            ),
          ),
          const Spacer(),
          Row(
            children: [
              OutlinedButton(
                onPressed: _speakCurrent,
                child: const Text('🔊'),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: FilledButton(
                  onPressed: _revealed ? _next : _reveal,
                  child: Text(
                      _revealed
                          ? (_index + 1 < _deck.length ? 'Дальше ▶' : 'Готово ✔')
                          : 'Показать',
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Пример употребления формы: фраза с озвучкой и переводом.
  /// Нажатие на фразу — эстонская озвучка, на перевод — украинская.
  Widget _exampleBlock(
      _Drill d, ColorScheme scheme, LearnViewModel vm, bool mastered) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: scheme.secondaryContainer.withOpacity(0.35),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Пример:',
              style: TextStyle(
                  fontSize: 10.5,
                  fontWeight: FontWeight.w600,
                  color: scheme.onSurface.withOpacity(0.45))),
          const SizedBox(height: 3),
          InkWell(
            borderRadius: BorderRadius.circular(6),
            onTap: () => vm.speakWord(d.exEt),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(child: _exampleText(d, scheme)),
                const SizedBox(width: 6),
                Icon(Icons.volume_up, size: 18, color: scheme.primary),
              ],
            ),
          ),
          if (d.exTr.isNotEmpty && !mastered)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: InkWell(
                borderRadius: BorderRadius.circular(6),
                onTap: () => vm.speakTr(d.exTr),
                child: Text(d.exTr,
                    style: TextStyle(
                        fontSize: 12.5,
                        color: scheme.onSurface.withOpacity(0.65))),
              ),
            ),
        ],
      ),
    );
  }

  /// Текст примера: тренируемая форма выделяется цветом.
  Widget _exampleText(_Drill d, ColorScheme scheme) {
    final style = TextStyle(
        fontSize: 14, height: 1.3, color: scheme.onSurface.withOpacity(0.85));
    final hit = _findToken(d.exEt, _clean(d.et));
    if (hit.isEmpty) return Text(d.exEt, style: style);
    final i = d.exEt.indexOf(hit);
    return Text.rich(
      TextSpan(children: [
        TextSpan(text: d.exEt.substring(0, i)),
        TextSpan(
            text: hit,
            style: TextStyle(
                fontWeight: FontWeight.bold, color: scheme.primary)),
        TextSpan(text: d.exEt.substring(i + hit.length)),
      ]),
      style: style,
    );
  }

  /// Эстонский текст карточки; в фразе выделяем тренируемое слово.
  Widget _etText(_Drill d, ColorScheme scheme) {
    final style = TextStyle(
        fontSize: d.isPhrase ? 19 : 30,
        fontWeight: FontWeight.bold,
        height: 1.3,
        color: scheme.onSurface);
    if (!d.isPhrase || d.highlight.isEmpty) {
      return Text(d.et, textAlign: TextAlign.center, style: style);
    }
    final i = d.et.indexOf(d.highlight);
    if (i < 0) return Text(d.et, textAlign: TextAlign.center, style: style);
    return Text.rich(
      TextSpan(children: [
        TextSpan(text: d.et.substring(0, i)),
        TextSpan(
            text: d.highlight,
            style: TextStyle(
                color: scheme.primary,
                decoration: TextDecoration.underline)),
        TextSpan(text: d.et.substring(i + d.highlight.length)),
      ]),
      textAlign: TextAlign.center,
      style: style,
    );
  }

  Widget _buildDone(ColorScheme scheme) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text('🎉', style: TextStyle(fontSize: 56)),
            const SizedBox(height: 12),
            Text('Тренировка завершена!',
                style: TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: scheme.onSurface)),
            const SizedBox(height: 8),
            Text('+$_sessionReps повторений слов этого урока',
                style: TextStyle(
                    fontSize: 14, color: scheme.onSurface.withOpacity(0.7))),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: _restart,
              child: const Text('Ещё раз 🔁',
                  style: TextStyle(fontWeight: FontWeight.bold)),
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('К уроку'),
            ),
          ],
        ),
      ),
    );
  }
}
