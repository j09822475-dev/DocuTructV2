import '../models.dart';
import 'lessons_1.dart';
import 'lessons_2.dart';
import 'lessons_3.dart';

/// Все уроки курса в хронологическом порядке (как заполнялся конспект).
final List<Lesson> allLessons = [
  ...lessonsPart1,
  ...lessonsPart2,
  ...lessonsPart3,
];
