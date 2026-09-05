import 'package:flutter/material.dart';

import 'learn_state.dart';
import 'learn_vm.dart';
import 'screens/lessons_screen.dart';
import 'screens/profile_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final repo = await LearnRepository.create();
  runApp(EestiKeelApp(vm: LearnViewModel(repo)));
}

const _brandGreen = Color(0xFF0B7A4B);
const _brandGreenDark = Color(0xFF075C39);
const _accentCyan = Color(0xFF00B4D8);
const _sun = Color(0xFFFFB703);
const _coral = Color(0xFFE5484D);
const _ink = Color(0xFF1A1C1A);
const _cloud = Color(0xFFF3F5F4);

class EestiKeelApp extends StatelessWidget {
  final LearnViewModel vm;
  const EestiKeelApp({super.key, required this.vm});

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
      title: 'Eesti keel A2',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(colorScheme: lightScheme, useMaterial3: true),
      darkTheme: ThemeData(colorScheme: darkScheme, useMaterial3: true),
      home: RootShell(vm: vm),
    );
  }
}

class RootShell extends StatefulWidget {
  final LearnViewModel vm;
  const RootShell({super.key, required this.vm});

  @override
  State<RootShell> createState() => _RootShellState();
}

class _RootShellState extends State<RootShell> {
  int tab = 0;

  LearnViewModel get vm => widget.vm;

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: vm,
      builder: (context, _) {
        final scheme = Theme.of(context).colorScheme;
        return Scaffold(
          backgroundColor: scheme.surfaceContainerLowest,
          appBar: AppBar(
            backgroundColor: scheme.primary,
            foregroundColor: scheme.onPrimary,
            title: const Text('🇪🇪 Eesti keel',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
            actions: [
              Icon(Icons.translate,
                  size: 18, color: scheme.onPrimary.withOpacity(0.85)),
              Padding(
                padding: const EdgeInsets.only(left: 4),
                child: Center(
                  child: Text(vm.hideTr ? 'перевод скрыт' : 'перевод виден',
                      style: TextStyle(
                          fontSize: 12,
                          color: scheme.onPrimary.withOpacity(0.85))),
                ),
              ),
              IconButton(
                tooltip: 'Скрыть/показать переводы',
                onPressed: vm.toggleHideTr,
                icon: Icon(
                    vm.hideTr ? Icons.visibility_off : Icons.visibility,
                    color: scheme.onPrimary),
              ),
            ],
          ),
          body: IndexedStack(
            index: tab,
            children: [
              LessonsScreen(vm: vm),
              ProfileScreen(vm: vm),
            ],
          ),
          bottomNavigationBar: NavigationBar(
            selectedIndex: tab,
            onDestinationSelected: (i) => setState(() => tab = i),
            destinations: const [
              NavigationDestination(
                  icon: Icon(Icons.menu_book), label: 'Уроки'),
              NavigationDestination(
                  icon: Icon(Icons.person), label: 'Прогресс'),
            ],
          ),
        );
      },
    );
  }
}
