import 'package:flutter/material.dart';

import '../game_vm.dart';
import '../models.dart';
import '../widgets.dart';

class OrderScreen extends StatefulWidget {
  final GameViewModel vm;
  const OrderScreen({super.key, required this.vm});

  @override
  State<OrderScreen> createState() => _OrderScreenState();
}

class _OrderScreenState extends State<OrderScreen> {
  final _listCtrl = ScrollController();
  int _lastAutoScrollKey = -1;

  GameViewModel get vm => widget.vm;

  void _autoScroll(int key) {
    if (key == _lastAutoScrollKey) return;
    _lastAutoScrollKey = key;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!_listCtrl.hasClients) return;
      _listCtrl.animateTo(
        _listCtrl.position.maxScrollExtent,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    });
  }

  @override
  void dispose() {
    _listCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: vm,
      builder: (context, _) {
        final session = vm.session;
        if (session == null) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted && Navigator.of(context).canPop()) {
              Navigator.of(context).pop();
            }
          });
          return const Scaffold(body: SizedBox.shrink());
        }
        if (session.finished && vm.lastResult != null) {
          return _ResultView(vm: vm);
        }

        final scheme = Theme.of(context).colorScheme;
        final messages = session.messages(session.activeThread);
        final showTyping = session.typing == session.activeThread;
        _autoScroll(Object.hash(
          messages.length,
          showTyping,
          session.awaiting,
          session.atEnd,
          session.activeThread,
          session.nav != null,
        ));

        return Scaffold(
          backgroundColor: scheme.surfaceContainerLowest,
          body: SafeArea(
            child: Column(
              children: [
                Padding(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  child: Row(
                    children: [
                      TextButton.icon(
                        onPressed: () {
                          vm.cancelOrder();
                        },
                        icon: const Icon(Icons.close, size: 18),
                        label: const Text('Отмена'),
                      ),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                                '${session.order.restaurant} → ${session.order.customer}',
                                style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 14,
                                    color: scheme.onSurface)),
                            Text(session.order.address,
                                style: TextStyle(
                                    fontSize: 11,
                                    color: scheme.onSurface.withOpacity(0.6))),
                          ],
                        ),
                      ),
                      IconButton(
                        tooltip: 'Скрыть/показать русский перевод',
                        onPressed: vm.toggleHideRu,
                        icon: Icon(
                            vm.hideRu
                                ? Icons.visibility_off
                                : Icons.visibility,
                            size: 20),
                      ),
                    ],
                  ),
                ),
                LinearProgressIndicator(value: session.progress, minHeight: 5),

                // Вкладки-чаты
                if (session.tabs.length > 1)
                  SingleChildScrollView(
                    scrollDirection: Axis.horizontal,
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
                    child: Row(
                      children: [
                        for (final t in session.tabs)
                          Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: FilterChip(
                              selected: t == session.activeThread,
                              onSelected: (_) => vm.setActiveThread(t),
                              label: Text(threadLabel(t) +
                                  (session.unread.contains(t) ? '  🔴' : '')),
                            ),
                          ),
                      ],
                    ),
                  ),

                // Лента активного чата
                Expanded(
                  child: ListView(
                    controller: _listCtrl,
                    padding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 8),
                    children: [
                      for (final msg in messages)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: _ChatBubble(
                            msg: msg,
                            displayRu: vm.hideRu ? '' : vm.maskRu(msg.ru),
                            onSpeak: () =>
                                vm.speak(msg.et, msg.fromCourier, msg.thread),
                          ),
                        ),
                      if (showTyping)
                        _TypingBubble(
                            label: threadLabel(session.activeThread)),
                    ],
                  ),
                ),

                // Нижняя панель
                Material(
                  color: scheme.surface,
                  elevation: 8,
                  child: Padding(
                    padding: const EdgeInsets.all(12),
                    child: _bottomPanel(context, session),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _bottomPanel(BuildContext context, OrderSession session) {
    final scheme = Theme.of(context).colorScheme;
    final ask = session.currentAsk;
    final nav = session.nav;

    if (session.atEnd) {
      return SizedBox(
        width: double.infinity,
        child: FilledButton(
          onPressed: vm.finishDelivery,
          style: FilledButton.styleFrom(
              shape:
                  RoundedRectangleBorder(borderRadius: BorderRadius.circular(14))),
          child: const Text('Завершить доставку 🏁',
              style: TextStyle(fontWeight: FontWeight.bold)),
        ),
      );
    }
    if (nav != null) {
      return Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Text(nav.promptRu,
                style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                    color: scheme.onSurface)),
          ),
          for (final opt in nav.options)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 4),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton(
                  onPressed: () => vm.navChoose(opt),
                  style: FilledButton.styleFrom(
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14))),
                  child: Text(opt.labelRu,
                      style: const TextStyle(fontWeight: FontWeight.bold)),
                ),
              ),
            ),
        ],
      );
    }
    if (session.awaiting &&
        ask != null &&
        ask.thread == session.activeThread) {
      // Прокручиваемые варианты + ЗАКРЕПЛЁННАЯ кнопка «Отправить»
      return Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          ConstrainedBox(
            constraints: const BoxConstraints(maxHeight: 270),
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(ask.promptRu,
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                          color: scheme.onSurface)),
                  Padding(
                    padding: const EdgeInsets.only(top: 2, bottom: 6),
                    child: Text(
                        ask.courier
                            ? '🛵 Выберите, что сказать:'
                            : '🛵 Выберите ответ:',
                        style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: scheme.secondary)),
                  ),
                  for (var i = 0; i < ask.choices.length; i++)
                    _AnswerOption(
                      et: ask.choices[i].et,
                      ru: vm.hideRu ? '' : ask.choices[i].ru,
                      selected: session.selected == i,
                      onTap: () => vm.select(i),
                      onSpeak: () =>
                          vm.speak(ask.choices[i].et, true, ask.thread),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          FilledButton.icon(
            onPressed: session.selected != null ? vm.confirm : null,
            icon: const Icon(Icons.send, size: 18),
            label: const Text('Отправить',
                style: TextStyle(fontWeight: FontWeight.bold)),
            style: FilledButton.styleFrom(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14))),
          ),
        ],
      );
    }
    if (session.awaiting && ask != null) {
      return Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Text('Ответьте в чате «${threadLabel(ask.thread)}»',
                style: TextStyle(fontSize: 13, color: scheme.onSurface)),
          ),
          OutlinedButton(
            onPressed: () => vm.setActiveThread(ask.thread),
            style: OutlinedButton.styleFrom(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14))),
            child: Text('Открыть «${threadLabel(ask.thread)}»'),
          ),
        ],
      );
    }
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Text('💬 …',
          style:
              TextStyle(fontSize: 16, color: scheme.onSurface.withOpacity(0.5))),
    );
  }
}

class _ChatBubble extends StatelessWidget {
  final ChatMsg msg;
  final String displayRu;
  final VoidCallback onSpeak;
  const _ChatBubble(
      {required this.msg, required this.displayRu, required this.onSpeak});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final courier = msg.fromCourier;
    final bubbleColor = courier ? scheme.primary : scheme.surfaceContainerHighest;
    final onBubble = courier ? scheme.onPrimary : scheme.onSurface;
    final radius = BorderRadius.only(
      topLeft: const Radius.circular(18),
      topRight: const Radius.circular(18),
      bottomLeft: Radius.circular(courier ? 18 : 4),
      bottomRight: Radius.circular(courier ? 4 : 18),
    );
    return Row(
      mainAxisAlignment:
          courier ? MainAxisAlignment.end : MainAxisAlignment.start,
      children: [
        ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 300),
          child: Column(
            crossAxisAlignment:
                courier ? CrossAxisAlignment.end : CrossAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(left: 6, right: 6, bottom: 2),
                child: Text(msg.label,
                    style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        color: scheme.onSurface.withOpacity(0.6))),
              ),
              Material(
                color: bubbleColor,
                borderRadius: radius,
                child: Padding(
                  padding: const EdgeInsets.only(
                      left: 12, right: 4, top: 8, bottom: 8),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Flexible(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(msg.et,
                                style: TextStyle(
                                    fontWeight: FontWeight.bold,
                                    fontSize: 16,
                                    color: onBubble)),
                            if (displayRu.trim().isNotEmpty)
                              Text(displayRu,
                                  style: TextStyle(
                                      fontSize: 13,
                                      color: onBubble.withOpacity(0.8))),
                          ],
                        ),
                      ),
                      const SizedBox(width: 4),
                      SpeakButton(
                          onPressed: onSpeak,
                          tint: courier ? scheme.onPrimary : scheme.primary),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _TypingBubble extends StatelessWidget {
  final String label;
  const _TypingBubble({required this.label});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 6, right: 6, bottom: 2),
          child: Text(label,
              style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                  color: scheme.onSurface.withOpacity(0.6))),
        ),
        Material(
          color: scheme.surfaceContainerHighest,
          borderRadius: const BorderRadius.only(
            topLeft: Radius.circular(18),
            topRight: Radius.circular(18),
            bottomLeft: Radius.circular(4),
            bottomRight: Radius.circular(18),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
            child: Text('печатает •••',
                style: TextStyle(
                    fontSize: 14, color: scheme.onSurface.withOpacity(0.6))),
          ),
        ),
      ],
    );
  }
}

class _AnswerOption extends StatelessWidget {
  final String et;
  final String ru;
  final bool selected;
  final VoidCallback onTap;
  final VoidCallback onSpeak;
  const _AnswerOption({
    required this.et,
    required this.ru,
    required this.selected,
    required this.onTap,
    required this.onSpeak,
  });

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final border =
        selected ? scheme.primary : scheme.onSurface.withOpacity(0.12);
    final bg = selected ? scheme.primary.withOpacity(0.08) : scheme.surface;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Material(
        color: bg,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          borderRadius: BorderRadius.circular(14),
          onTap: onTap,
          child: Container(
            decoration: BoxDecoration(
              border: Border.all(color: border, width: 2),
              borderRadius: BorderRadius.circular(14),
            ),
            padding: const EdgeInsets.all(12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(et,
                          style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 15,
                              color: scheme.onSurface)),
                      if (ru.trim().isNotEmpty)
                        Text(ru,
                            style: TextStyle(
                                fontSize: 12,
                                color: scheme.onSurface.withOpacity(0.65))),
                    ],
                  ),
                ),
                SpeakButton(onPressed: onSpeak),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ResultView extends StatelessWidget {
  final GameViewModel vm;
  const _ResultView({required this.vm});

  @override
  Widget build(BuildContext context) {
    final r = vm.lastResult;
    if (r == null) return const Scaffold(body: SizedBox.shrink());
    final scheme = Theme.of(context).colorScheme;
    return Scaffold(
      backgroundColor: scheme.surfaceContainerLowest,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 24),
              Text(r.perfect ? '🏆' : '🏁',
                  textAlign: TextAlign.center,
                  style: const TextStyle(fontSize: 64)),
              Text(r.perfect ? 'Доставка без ошибок!' : 'Доставка выполнена',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 24,
                      color: scheme.onSurface)),
              Text('${r.order.restaurant} → ${r.order.customer}',
                  textAlign: TextAlign.center,
                  style: TextStyle(
                      fontSize: 14, color: scheme.onSurface.withOpacity(0.7))),
              const SizedBox(height: 20),
              Card(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(20)),
                color: scheme.surface,
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Column(
                    children: [
                      _resultRow(context, 'Верных ответов', '${r.steps}'),
                      _resultRow(context, 'Ошибок',
                          r.mistakes == 0 ? 'нет 🎯' : '${r.mistakes}'),
                      _resultRow(context, 'Оплата доставки',
                          '${r.order.payout.toStringAsFixed(2)} €'),
                      _resultRow(context, 'Чаевые за качество',
                          r.tip > 0 ? '+${r.tipStr}' : '—'),
                      _resultRow(context, 'Опыт', '+${r.xp} XP'),
                      Divider(color: scheme.onSurface.withOpacity(0.1)),
                      _resultRow(context, 'Итого заработано', r.earnedStr,
                          highlight: true),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 24),
              FilledButton(
                onPressed: () {
                  vm.closeResult();
                  Navigator.of(context).pop();
                },
                style: FilledButton.styleFrom(
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14))),
                child: const Text('Дальше работать 🛵',
                    style: TextStyle(fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _resultRow(BuildContext context, String label, String value,
      {bool highlight = false}) {
    final scheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label,
              style: TextStyle(
                  fontSize: highlight ? 16 : 14,
                  fontWeight: highlight ? FontWeight.bold : FontWeight.normal,
                  color: scheme.onSurface)),
          Text(value,
              style: TextStyle(
                  fontSize: highlight ? 18 : 14,
                  fontWeight: FontWeight.bold,
                  color: highlight ? scheme.primary : scheme.onSurface)),
        ],
      ),
    );
  }
}
