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
  const _Drill(this.word, this.et, this.formLabel, this.tr,
      {this.highlight = '', this.isPhrase = false});
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
      final a = single ? analyze(clean.toLowerCase()) : null;
      final lx = a?.lex;
      // Перевод форм — словарный перевод ЛЕММЫ (карточка может быть
      // производной формой: suhkruta «без цукру» -> лемма suhkur «цукор»).
      final baseTr = lx?.tr ?? w.tr;
      final isBaseForm = lx == null ||
          clean.toLowerCase() == lx.f1 ||
          clean.toLowerCase() == lx.f2 ||
          clean.toLowerCase() == lx.f3;

      // ---- Этап 1: формы слова ----
      if (lx != null && (lx.kind == 'n' || lx.kind == 'a')) {
        words.add(
            _Drill(w, lx.f1, '1-я форма · Nimetav — kes? mis?', baseTr));
        if (lx.f2.isNotEmpty && lx.f2 != lx.f1) {
          words.add(_Drill(
              w, lx.f2, '2-я форма · Omastav — kelle? mille?',
              ukrGen(baseTr)));
        }
        if (lx.f3.isNotEmpty && lx.f3 != lx.f2) {
          words.add(_Drill(
              w, lx.f3, '3-я форма · Osastav — keda? mida?',
              ukrAcc(baseTr)));
        }
        if (!isBaseForm) {
          // сама карточка — производная форма со своим переводом
          words.add(_Drill(w, clean, a!.formName, w.tr));
        }
      } else if (lx != null && lx.kind == 'v') {
        words.add(_Drill(
            w, lx.f1, 'ma-инфинитив — после pean, hakkan, lähen', baseTr));
        if (lx.f2.isNotEmpty && lx.f2 != lx.f1) {
          words.add(_Drill(
              w, lx.f2, 'da-инфинитив — после tahan, oskan, meeldib',
              baseTr));
        }
        if (lx.f3.isNotEmpty) {
          words.add(_Drill(
              w, '${lx.f3}n', 'настоящее время — ma …n', ukrPres1(baseTr)));
        }
        if (!isBaseForm) {
          words.add(_Drill(w, clean, a!.formName, w.tr));
        }
      } else {
        words.add(_Drill(w, w.et, single ? '' : 'выражение', w.tr));
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
