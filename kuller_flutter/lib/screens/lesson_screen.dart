import 'dart:math';

import 'package:flutter/material.dart';

import '../learn_vm.dart';
import '../models.dart';
import '../widgets.dart';

class LessonScreen extends StatelessWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const LessonScreen({super.key, required this.vm, required this.lesson});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final tabs = <Tab>[
      const Tab(text: 'Слова'),
      if (lesson.texts.isNotEmpty) const Tab(text: 'Тексты'),
      if (lesson.dialogues.isNotEmpty) const Tab(text: 'Диалоги'),
      const Tab(text: 'Тест'),
    ];
    return DefaultTabController(
      length: tabs.length,
      child: Scaffold(
        backgroundColor: scheme.surfaceContainerLowest,
        appBar: AppBar(
          backgroundColor: scheme.primary,
          foregroundColor: scheme.onPrimary,
          title: Text('${lesson.emoji} ${lesson.title}',
              style:
                  const TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
          actions: [
            ListenableBuilder(
              listenable: vm,
              builder: (context, _) => IconButton(
                tooltip: 'Скрыть/показать переводы',
                onPressed: vm.toggleHideTr,
                icon: Icon(
                    vm.hideTr ? Icons.visibility_off : Icons.visibility,
                    color: scheme.onPrimary),
              ),
            ),
          ],
          bottom: TabBar(
            tabs: tabs,
            labelColor: scheme.onPrimary,
            unselectedLabelColor: scheme.onPrimary.withOpacity(0.6),
            indicatorColor: scheme.onPrimary,
          ),
        ),
        body: ListenableBuilder(
          listenable: vm,
          builder: (context, _) => TabBarView(
            children: [
              _WordsTab(vm: vm, lesson: lesson),
              if (lesson.texts.isNotEmpty) _TextsTab(vm: vm, lesson: lesson),
              if (lesson.dialogues.isNotEmpty)
                _DialoguesTab(vm: vm, lesson: lesson),
              TestTab(vm: vm, lesson: lesson),
            ],
          ),
        ),
      ),
    );
  }
}

// ============================ СЛОВА ============================

class _WordsTab extends StatefulWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const _WordsTab({required this.vm, required this.lesson});

  @override
  State<_WordsTab> createState() => _WordsTabState();
}

class _WordsTabState extends State<_WordsTab> {
  final Set<int> revealed = {};

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final vm = widget.vm;
    return ListView.builder(
      padding: const EdgeInsets.all(12),
      itemCount: widget.lesson.words.length,
      itemBuilder: (context, i) {
        final w = widget.lesson.words[i];
        final learned = vm.state.learnedWords.contains(w.et);
        final showTr = !vm.hideTr || revealed.contains(i);
        return Card(
          elevation: 1,
          margin: const EdgeInsets.symmetric(vertical: 4),
          shape:
              RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          color: scheme.surface,
          child: InkWell(
            borderRadius: BorderRadius.circular(16),
            onTap: vm.hideTr
                ? () => setState(() =>
                    revealed.contains(i) ? revealed.remove(i) : revealed.add(i))
                : null,
            child: Padding(
              padding: const EdgeInsets.all(12).copyWith(left: 14),
              child: Row(
                children: [
                  if (w.emoji.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.only(right: 10),
                      child: Text(w.emoji, style: const TextStyle(fontSize: 24)),
                    ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Flexible(
                              child: Text(w.et,
                                  style: TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 16,
                                      color: scheme.onSurface)),
                            ),
                            if (learned)
                              const Padding(
                                padding: EdgeInsets.only(left: 6),
                                child: Text('✅', style: TextStyle(fontSize: 12)),
                              ),
                          ],
                        ),
                        Text(showTr ? w.tr : '••• (нажмите, чтобы открыть)',
                            style: TextStyle(
                                fontSize: 13,
                                color: scheme.onSurface
                                    .withOpacity(showTr ? 0.7 : 0.4))),
                        if (w.example.isNotEmpty)
                          Text('„${w.example}“',
                              style: TextStyle(
                                  fontSize: 12, color: scheme.primary)),
                      ],
                    ),
                  ),
                  SpeakButton(onPressed: () => vm.speakWord(w.et)),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

// ============================ ТЕКСТЫ ============================

class _TextsTab extends StatefulWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const _TextsTab({required this.vm, required this.lesson});

  @override
  State<_TextsTab> createState() => _TextsTabState();
}

class _TextsTabState extends State<_TextsTab> {
  final Set<String> openAnswers = {};

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final vm = widget.vm;
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        for (final text in widget.lesson.texts) ...[
          Text(text.title,
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 18,
                  color: scheme.onSurface)),
          const SizedBox(height: 8),
          for (final p in text.paras)
            Card(
              elevation: 0,
              margin: const EdgeInsets.symmetric(vertical: 4),
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14)),
              color: scheme.surface,
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(p.et,
                              style: TextStyle(
                                  fontSize: 15,
                                  height: 1.35,
                                  color: scheme.onSurface)),
                          if (p.tr.isNotEmpty && !vm.hideTr)
                            Padding(
                              padding: const EdgeInsets.only(top: 4),
                              child: Text(p.tr,
                                  style: TextStyle(
                                      fontSize: 13,
                                      color:
                                          scheme.onSurface.withOpacity(0.6))),
                            ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 6),
                    SpeakButton(onPressed: () => vm.speakWord(p.et)),
                  ],
                ),
              ),
            ),
          if (text.questions.isNotEmpty) ...[
            const SizedBox(height: 10),
            Text('❓ Вопросы к тексту',
                style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                    color: scheme.onSurface)),
            const SizedBox(height: 4),
            for (var qi = 0; qi < text.questions.length; qi++)
              Builder(builder: (context) {
                final key = '${text.title}#$qi';
                final qa = text.questions[qi];
                final open = openAnswers.contains(key);
                return Card(
                  elevation: 0,
                  margin: const EdgeInsets.symmetric(vertical: 3),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12)),
                  color: open
                      ? scheme.primaryContainer.withOpacity(0.5)
                      : scheme.surface,
                  child: InkWell(
                    borderRadius: BorderRadius.circular(12),
                    onTap: () => setState(() =>
                        open ? openAnswers.remove(key) : openAnswers.add(key)),
                    child: Padding(
                      padding: const EdgeInsets.all(10),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(qa.q,
                              style: TextStyle(
                                  fontWeight: FontWeight.w600,
                                  fontSize: 14,
                                  color: scheme.onSurface)),
                          Padding(
                            padding: const EdgeInsets.only(top: 3),
                            child: Text(open ? qa.a : 'Показать ответ…',
                                style: TextStyle(
                                    fontSize: 13,
                                    color: open
                                        ? scheme.primary
                                        : scheme.onSurface.withOpacity(0.5))),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }),
          ],
          const SizedBox(height: 20),
        ],
      ],
    );
  }
}

// ============================ ДИАЛОГИ ============================

class _DialoguesTab extends StatelessWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const _DialoguesTab({required this.vm, required this.lesson});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        for (final d in lesson.dialogues) ...[
          Text('💬 ${d.title}',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 16,
                  color: scheme.onSurface)),
          const SizedBox(height: 8),
          for (final line in d.lines) _bubble(context, line),
          const SizedBox(height: 20),
        ],
      ],
    );
  }

  Widget _bubble(BuildContext context, DialogueLine line) {
    final scheme = Theme.of(context).colorScheme;
    final isA = line.speaker == 'A';
    final bubbleColor = isA ? scheme.surfaceContainerHighest : scheme.primary;
    final onBubble = isA ? scheme.onSurface : scheme.onPrimary;
    return Row(
      mainAxisAlignment: isA ? MainAxisAlignment.start : MainAxisAlignment.end,
      children: [
        ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 320),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 3),
            child: Material(
              color: bubbleColor,
              borderRadius: BorderRadius.only(
                topLeft: const Radius.circular(16),
                topRight: const Radius.circular(16),
                bottomLeft: Radius.circular(isA ? 4 : 16),
                bottomRight: Radius.circular(isA ? 16 : 4),
              ),
              child: Padding(
                padding:
                    const EdgeInsets.only(left: 12, right: 4, top: 8, bottom: 8),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Flexible(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(line.et,
                              style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  fontSize: 15,
                                  color: onBubble)),
                          if (line.tr.isNotEmpty && !vm.hideTr)
                            Text(line.tr,
                                style: TextStyle(
                                    fontSize: 12.5,
                                    color: onBubble.withOpacity(0.75))),
                        ],
                      ),
                    ),
                    const SizedBox(width: 4),
                    SpeakButton(
                      onPressed: () => vm.speak(line.et, speaker: line.speaker),
                      tint: isA ? scheme.primary : scheme.onPrimary,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

// ============================ ТЕСТ ============================

class _Question {
  final String prompt; // что показываем
  final String correct; // правильный вариант
  final List<String> options;
  final String wordEt; // эстонское слово (для отметки «выучено»)
  final bool etToTr;
  _Question(this.prompt, this.correct, this.options, this.wordEt, this.etToTr);
}

class TestTab extends StatefulWidget {
  final LearnViewModel vm;
  final Lesson lesson;
  const TestTab({super.key, required this.vm, required this.lesson});

  @override
  State<TestTab> createState() => _TestTabState();
}

class _TestTabState extends State<TestTab> {
  List<_Question> questions = [];
  int current = 0;
  int correct = 0;
  int? picked;
  bool finished = false;
  final Set<String> correctWords = {};
  final rnd = Random();

  @override
  void initState() {
    super.initState();
    _generate();
  }

  void _generate() {
    final words = widget.lesson.words;
    final pool = [...words]..shuffle(rnd);
    final count = min(10, pool.length);
    questions = [];
    for (var i = 0; i < count; i++) {
      final w = pool[i];
      final etToTr = rnd.nextBool();
      final wrong = ([...words]..remove(w)..shuffle(rnd)).take(3);
      final options = etToTr
          ? [w.tr, ...wrong.map((x) => x.tr)]
          : [w.et, ...wrong.map((x) => x.et)];
      options.shuffle(rnd);
      questions.add(_Question(
        etToTr ? w.et : w.tr,
        etToTr ? w.tr : w.et,
        options,
        w.et,
        etToTr,
      ));
    }
    current = 0;
    correct = 0;
    picked = null;
    finished = false;
    correctWords.clear();
  }

  void _pick(int i) {
    if (picked != null) return;
    setState(() {
      picked = i;
      final q = questions[current];
      if (q.options[i] == q.correct) {
        correct++;
        correctWords.add(q.wordEt);
      }
    });
  }

  void _next() {
    if (current + 1 >= questions.length) {
      widget.vm.recordTest(
          widget.lesson.id, correct, questions.length, correctWords);
      setState(() => finished = true);
    } else {
      setState(() {
        current++;
        picked = null;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    if (questions.length < 4) {
      return const Center(child: Text('В этом уроке мало слов для теста.'));
    }
    if (finished) {
      final pct = (correct * 100) ~/ questions.length;
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(pct >= 80 ? '🏆' : (pct >= 50 ? '👍' : '💪'),
                  style: const TextStyle(fontSize: 64)),
              Text('Результат: $correct из ${questions.length} ($pct%)',
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 20,
                      color: scheme.onSurface)),
              const SizedBox(height: 6),
              Text(
                  pct >= 80
                      ? 'Отлично! Урок пройден.'
                      : 'Повторите слова и попробуйте ещё раз.',
                  style: TextStyle(
                      fontSize: 14, color: scheme.onSurface.withOpacity(0.7))),
              const SizedBox(height: 20),
              FilledButton(
                onPressed: () => setState(_generate),
                child: const Text('Пройти ещё раз'),
              ),
            ],
          ),
        ),
      );
    }
    final q = questions[current];
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          LinearProgressIndicator(value: current / questions.length),
          const SizedBox(height: 14),
          Text('Вопрос ${current + 1} из ${questions.length}',
              style: TextStyle(
                  fontSize: 13, color: scheme.onSurface.withOpacity(0.6))),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(
                child: Text(
                  q.etToTr
                      ? 'Как переводится: «${q.prompt}»?'
                      : 'Как по-эстонски: «${q.prompt}»?',
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 18,
                      color: scheme.onSurface),
                ),
              ),
              if (q.etToTr)
                SpeakButton(onPressed: () => widget.vm.speakWord(q.prompt)),
            ],
          ),
          const SizedBox(height: 14),
          for (var i = 0; i < q.options.length; i++)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: Builder(builder: (context) {
                Color bg = scheme.surface;
                Color border = scheme.onSurface.withOpacity(0.15);
                if (picked != null) {
                  if (q.options[i] == q.correct) {
                    bg = scheme.primaryContainer;
                    border = scheme.primary;
                  } else if (picked == i) {
                    bg = scheme.errorContainer;
                    border = scheme.error;
                  }
                }
                return Material(
                  color: bg,
                  borderRadius: BorderRadius.circular(14),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(14),
                    onTap: () => _pick(i),
                    child: Container(
                      decoration: BoxDecoration(
                        border: Border.all(color: border, width: 2),
                        borderRadius: BorderRadius.circular(14),
                      ),
                      padding: const EdgeInsets.all(14),
                      child: Text(q.options[i],
                          style: TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w600,
                              color: scheme.onSurface)),
                    ),
                  ),
                );
              }),
            ),
          const Spacer(),
          FilledButton(
            onPressed: picked != null ? _next : null,
            style: FilledButton.styleFrom(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14))),
            child: Text(
                current + 1 >= questions.length ? 'Завершить' : 'Дальше',
                style: const TextStyle(fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
