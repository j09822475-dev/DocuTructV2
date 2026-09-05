import 'package:flutter/material.dart';

import '../game_vm.dart';
import '../models.dart';
import '../widgets.dart';

class HomeScreen extends StatelessWidget {
  final GameViewModel vm;
  final void Function(Order) onAccept;
  const HomeScreen({super.key, required this.vm, required this.onAccept});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 16),
          child: Text('Tere, kuller! 🛵',
              style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 26,
                  color: scheme.onSurface)),
        ),
        Text('Учи эстонский, развозя заказы',
            style: TextStyle(
                fontSize: 14, color: scheme.onSurface.withOpacity(0.7))),
        const SizedBox(height: 10),
        Row(
          children: [
            Expanded(child: StatPill('💶', vm.moneyStr(), 'Заработок')),
            const SizedBox(width: 10),
            Expanded(child: StatPill('⭐', vm.ratingStr(), 'Рейтинг')),
            const SizedBox(width: 10),
            Expanded(child: StatPill('📦', '${vm.state.deliveries}', 'Доставки')),
            const SizedBox(width: 10),
            Expanded(child: StatPill('🎚', 'Lvl ${vm.state.level}', 'Уровень')),
          ],
        ),
        const SizedBox(height: 10),
        _OnlineCard(vm: vm),
        if (vm.online) ...[
          Padding(
            padding: const EdgeInsets.only(top: 18),
            child: Row(
              children: [
                Expanded(
                  child: Text('Доступные заказы',
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                          color: scheme.onSurface)),
                ),
                OutlinedButton.icon(
                  onPressed: vm.refreshOrders,
                  icon: const Icon(Icons.refresh, size: 16),
                  label: const Text('Обновить'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 6),
          for (final order in vm.availableOrders)
            _OrderCard(order: order, onAccept: () => onAccept(order)),
          const SizedBox(height: 24),
        ],
      ],
    );
  }
}

class _OnlineCard extends StatelessWidget {
  final GameViewModel vm;
  const _OnlineCard({required this.vm});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      color: vm.online ? scheme.primaryContainer : scheme.surface,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(vm.online ? 'Вы на линии' : 'Вы офлайн',
                      style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 18,
                          color: scheme.onSurface)),
                  Text(
                      vm.online
                          ? 'Принимайте заказы и учите слова'
                          : 'Включите смену, чтобы получать заказы',
                      style: TextStyle(
                          fontSize: 13,
                          color: scheme.onSurface.withOpacity(0.7))),
                ],
              ),
            ),
            Switch(value: vm.online, onChanged: (_) => vm.toggleOnline()),
          ],
        ),
      ),
    );
  }
}

class _OrderCard extends StatelessWidget {
  final Order order;
  final VoidCallback onAccept;
  const _OrderCard({required this.order, required this.onAccept});

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 5),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      color: scheme.surface,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Text('🍽', style: TextStyle(fontSize: 22)),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(order.restaurant,
                          style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 18,
                              color: scheme.onSurface)),
                      Text(order.itemsRu,
                          style: TextStyle(
                              fontSize: 13,
                              color: scheme.onSurface.withOpacity(0.7))),
                    ],
                  ),
                ),
                Text('+${order.payout.toStringAsFixed(2)} €',
                    style: TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 18,
                        color: scheme.primary)),
              ],
            ),
            Padding(
              padding: const EdgeInsets.only(top: 10),
              child: Row(
                children: [
                  Icon(Icons.place, size: 16, color: scheme.secondary),
                  const SizedBox(width: 4),
                  Flexible(
                    child: Text(order.address,
                        style: TextStyle(
                            fontSize: 13,
                            color: scheme.onSurface.withOpacity(0.8))),
                  ),
                  Text(' · ${order.distanceKm.toStringAsFixed(1)} km',
                      style: TextStyle(
                          fontSize: 13,
                          color: scheme.onSurface.withOpacity(0.6))),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.only(top: 12),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: onAccept,
                  icon: const Icon(Icons.location_on, size: 18),
                  label: const Text('Принять заказ',
                      style: TextStyle(fontWeight: FontWeight.bold)),
                  style: FilledButton.styleFrom(
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14)),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
