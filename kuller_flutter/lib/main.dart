import 'package:flutter/material.dart';

import 'game_state.dart';
import 'game_vm.dart';
import 'screens/home_screen.dart';
import 'screens/order_screen.dart';
import 'screens/phrasebook_screen.dart';
import 'screens/profile_screen.dart';
import 'screens/vocab_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final repo = await GameRepository.create();
  runApp(KullerApp(vm: GameViewModel(repo)));
}

const _brandGreen = Color(0xFF0B7A4B);
const _brandGreenDark = Color(0xFF075C39);
const _accentCyan = Color(0xFF00B4D8);
const _sun = Color(0xFFFFB703);
const _coral = Color(0xFFE5484D);
const _ink = Color(0xFF1A1C1A);
const _cloud = Color(0xFFF3F5F4);

class KullerApp extends StatelessWidget {
  final GameViewModel vm;
  const KullerApp({super.key, required this.vm});

  @override
  Widget build(BuildContext context) {
    const lightScheme = ColorScheme.light(
      primary: _brandGreen,
      onPrimary: Colors.white,
      primaryContainer: Color(0xFFCDEFDC),
      onPrimaryContainer: _brandGreenDark,
      secondary: _accentCyan,
      onSecondary: Colors.white,
      tertiary: _sun,
      error: _coral,
      surface: Colors.white,
      onSurface: _ink,
      surfaceContainerHighest: Color(0xFFE4EAE6),
      surfaceContainerLowest: _cloud,
    );
    const darkScheme = ColorScheme.dark(
      primary: Color(0xFF4FD89A),
      onPrimary: Color(0xFF00351E),
      primaryContainer: _brandGreenDark,
      onPrimaryContainer: Color(0xFFCDEFDC),
      secondary: _accentCyan,
      tertiary: _sun,
      error: _coral,
      surface: Color(0xFF1A201C),
      onSurface: _cloud,
      surfaceContainerHighest: Color(0xFF2A322D),
      surfaceContainerLowest: Color(0xFF101411),
    );
    return MaterialApp(
      title: 'Kuller',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(colorScheme: lightScheme, useMaterial3: true),
      darkTheme: ThemeData(colorScheme: darkScheme, useMaterial3: true),
      home: RootShell(vm: vm),
    );
  }
}

/// Верхняя панель с брендом и переключателем перевода — общая для всех экранов.
PreferredSizeWidget kullerAppBar(GameViewModel vm, BuildContext context) {
  final scheme = Theme.of(context).colorScheme;
  return AppBar(
    backgroundColor: scheme.primary,
    foregroundColor: scheme.onPrimary,
    title: const Text('🛵 Kuller',
        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
    actions: [
      Icon(Icons.translate, size: 18, color: scheme.onPrimary.withOpacity(0.85)),
      Padding(
        padding: const EdgeInsets.only(left: 4),
        child: Center(
          child: Text(vm.hideRu ? 'перевод скрыт' : 'перевод виден',
              style: TextStyle(
                  fontSize: 12, color: scheme.onPrimary.withOpacity(0.85))),
        ),
      ),
      IconButton(
        tooltip: 'Скрыть/показать русский перевод',
        onPressed: vm.toggleHideRu,
        icon: Icon(vm.hideRu ? Icons.visibility_off : Icons.visibility,
            color: scheme.onPrimary),
      ),
    ],
  );
}

class RootShell extends StatefulWidget {
  final GameViewModel vm;
  const RootShell({super.key, required this.vm});

  @override
  State<RootShell> createState() => _RootShellState();
}

class _RootShellState extends State<RootShell> {
  int tab = 0;

  GameViewModel get vm => widget.vm;

  void _openOrder() {
    Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => OrderScreen(vm: vm)),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: vm,
      builder: (context, _) {
        final scheme = Theme.of(context).colorScheme;
        return Scaffold(
          backgroundColor: scheme.surfaceContainerLowest,
          appBar: kullerAppBar(vm, context),
          body: IndexedStack(
            index: tab,
            children: [
              HomeScreen(
                vm: vm,
                onAccept: (order) {
                  vm.startOrder(order);
                  _openOrder();
                },
              ),
              VocabularyScreen(vm: vm),
              PhrasebookScreen(vm: vm),
              ProfileScreen(vm: vm),
            ],
          ),
          bottomNavigationBar: NavigationBar(
            selectedIndex: tab,
            onDestinationSelected: (i) => setState(() => tab = i),
            destinations: const [
              NavigationDestination(
                  icon: Icon(Icons.two_wheeler), label: 'Смена'),
              NavigationDestination(
                  icon: Icon(Icons.storefront), label: 'Слова'),
              NavigationDestination(
                  icon: Icon(Icons.menu_book), label: 'Разговорник'),
              NavigationDestination(icon: Icon(Icons.person), label: 'Профиль'),
            ],
          ),
        );
      },
    );
  }
}
