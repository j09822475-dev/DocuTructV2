import 'dart:math';

import 'models.dart';

/// Процедурная генерация заказов.
///
/// Заведение, клиент, адрес, состав заказа и сценарий выбираются случайно, но
/// СЦЕНАРИЙ и БЛЮДА привязаны к ТИПУ заведения (пиццерия чаще «оставить под
/// дверью» и отдаёт пиццу; кофейня — доставка в офис и т.д.).
class OrderFactory {
  OrderFactory._();

  static final _rnd = Random();

  static const _pizza = 'PIZZA', _sushi = 'SUSHI', _asian = 'ASIAN',
      _burger = 'BURGER', _cafe = 'CAFE', _soup = 'SOUP', _bakery = 'BAKERY',
      _generic = 'GENERIC';

  static const List<(String, String)> _restaurants = [
    ('Pizza Grande', _pizza),
    ('Pitsabaar Napoli', _pizza),
    ('Pizzakuller', _pizza),
    ('Sushi Aasia', _sushi),
    ('Sushi Tokyo', _sushi),
    ('Wasabi Bar', _sushi),
    ('Nuudlibaar Wok', _asian),
    ('Aasia Express', _asian),
    ('Indica Curry', _asian),
    ('Burger Maja', _burger),
    ('Grillinurk', _burger),
    ('Kebabimaja', _burger),
    ('Kohvik Sõbralik', _cafe),
    ('Tere Kohvik', _cafe),
    ('Päikese Kohvik', _cafe),
    ('Hommiku Kohvik', _cafe),
    ('Tervisekohvik', _cafe),
    ('Supiköök', _soup),
    ('Roheline Taldrik', _soup),
    ('Toidukoda', _soup),
    ('Magus Pagar', _bakery),
    ('Pelmeenikohvik', _bakery),
    ('Kala & Salat', _generic),
    ('Vana Linn Bistroo', _generic),
    ('Mere Restoran', _generic),
    ('Maailma Köök', _generic),
    ('Kuldne Lusikas', _generic),
  ];

  /// (et, ru, teach, venue) — venue == null: напитки/дополнения к любому заведению.
  static const List<(String, String, String, String?)> _dishes = [
    ('pepperoni pitsa', 'пицца пепперони', 'f_pitsa', _pizza),
    ('suur juustupitsa', 'большая пицца с сыром', 'f_pitsa', _pizza),
    ('kanaburger', 'куриный бургер', 'f_burger', _burger),
    ('juustuburger', 'чизбургер', 'f_burger', _burger),
    ('friikartulid', 'картофель фри', 'f_friikad', _burger),
    ('kanasupp', 'куриный суп', 'f_supp', _soup),
    ('seenesupp', 'грибной суп', 'f_supp', _soup),
    ('kreeka salat', 'греческий салат', 'f_salat', _cafe),
    ('caesari salat', 'салат цезарь', 'f_salat', _cafe),
    ('sai juustuga', 'булка с сыром', 'f_sai', _cafe),
    ('must leib', 'чёрный хлеб', 'f_leib', _cafe),
    ('praetud kala', 'жареная рыба', 'f_kala', _generic),
    ('praetud kana', 'жареная курица', 'f_kana', _generic),
    ('pelmeenid', 'пельмени', 'f_pelmeenid', _generic),
    ('sushikomplekt', 'суши-сет', 'f_sushi', _sushi),
    ('lõhesushi', 'суши с лососем', 'f_sushi', _sushi),
    ('kana karri', 'куриное карри', 'f_karri', _asian),
    ('aasia nuudlid', 'азиатская лапша', 'f_nuudlid', _asian),
    ('kanawrap', 'куриный ролл', 'f_wrap', _asian),
    ('šokolaadikook', 'шоколадный торт', 'f_kook', _bakery),
    ('vaniljejäätis', 'ванильное мороженое', 'f_jaatis', _bakery),
    ('pannkook moosiga', 'блин с вареньем', 'f_pannkook', _bakery),
    // Напитки и дополнения (подходят к любому заведению)
    ('kohv piimaga', 'кофе с молоком', 'd_kohv', null),
    ('roheline tee', 'зелёный чай', 'd_tee', null),
    ('õunamahl', 'яблочный сок', 'd_mahl', null),
    ('klaas vett', 'стакан воды', 'd_vesi', null),
    ('limonaad', 'лимонад', 'd_limonaad', null),
    ('marjasmuuti', 'ягодный смузи', 'd_smuuti', null),
    ('jõhvikamorss', 'клюквенный морс', 'd_morss', null),
    ('kakao', 'какао', 'd_kakao', null),
  ];

  static const List<String> _customers = [
    'Maarja', 'Andres', 'Kati', 'Pjotr', 'Olga', 'Mikk', 'Tiina', 'Rein',
    'Jaan', 'Liis', 'Mart', 'Kristi', 'Toomas', 'Eve', 'Marko', 'Anna',
    'Sergei', 'Jelena', 'Nikolai', 'Kristjan', 'Heli', 'Priit', 'Karl',
    'Maria', 'Dmitri', 'Triin', 'Urmas', 'Helena', 'Aleksei', 'Kalev',
  ];

  static const List<String> _streets = [
    'Pärnu maantee 12', 'Narva maantee 5', 'Tartu maantee 40', 'Liivalaia 8',
    'Sõpruse puiestee 21', 'Vana-Posti 7', 'Roosikrantsi 9', 'Tehnika 18',
    'Mustamäe tee 16', 'Endla 45', 'Tornimäe 3', 'Sadama 6',
    'Estonia puiestee 9', 'Gonsiori 14', 'Pikk 24', 'Lai 7', 'Viru 11',
    'Kreutzwaldi 25', 'Toompuiestee 30', 'Akadeemia tee 21',
    'Paldiski maantee 52', 'Kadaka tee 4', 'Õismäe tee 88', 'Lasnamäe 15',
    'Kalevi 6',
  ];

  /// Сценарии, типичные для каждого типа заведения.
  static const Map<String, List<Scenario>> _scenarioByVenue = {
    _pizza: [Scenario.leaveDoor, Scenario.faceDoor, Scenario.cash, Scenario.lateOrder, Scenario.spoiled, Scenario.angryClient, Scenario.refusePay, Scenario.dogYard],
    _sushi: [Scenario.liftBroken, Scenario.gateCode, Scenario.faceDoor, Scenario.office, Scenario.spoiled, Scenario.complaint, Scenario.intercom, Scenario.wrongItems],
    _asian: [Scenario.faceDoor, Scenario.office, Scenario.lateOrder, Scenario.wrongAddress, Scenario.complaint, Scenario.intercom, Scenario.wrongItems, Scenario.angryClient],
    _burger: [Scenario.faceDoor, Scenario.leaveDoor, Scenario.cash, Scenario.breakdown, Scenario.refusePay, Scenario.drunkClient, Scenario.angryClient],
    _cafe: [Scenario.office, Scenario.faceDoor, Scenario.directions, Scenario.lateOrder, Scenario.complaint, Scenario.intercom, Scenario.dogYard],
    _soup: [Scenario.gateCode, Scenario.faceDoor, Scenario.directions, Scenario.spoiled, Scenario.intercom, Scenario.dogYard],
    _bakery: [Scenario.office, Scenario.faceDoor, Scenario.cancelled, Scenario.lateOrder],
    _generic: Scenario.values,
  };

  static T _pick<T>(List<T> list) => list[_rnd.nextInt(list.length)];

  static Order _buildOrder(String id, (String, String) r, String customer) {
    final venue = r.$2;
    var mains = venue == _generic
        ? _dishes.where((d) => d.$4 != null).toList()
        : _dishes.where((d) => d.$4 == venue).toList();
    if (mains.isEmpty) mains = _dishes.where((d) => d.$4 != null).toList();
    final main = _pick(mains);
    final chosen = [
      main,
      if (_rnd.nextBool()) _pick(_dishes.where((d) => d.$4 == null).toList()),
    ];
    final itemsEt = chosen.map((d) => d.$1).join(' ja ');
    final itemsRu = chosen.map((d) => d.$2).join(' и ');
    final payout = (300 + _rnd.nextInt(350)) / 100.0;
    final distance = (8 + _rnd.nextInt(45)) / 10.0;
    final scenario = _pick(_scenarioByVenue[venue] ?? Scenario.values);

    return Order(
      id: id,
      restaurant: r.$1,
      customer: customer,
      // Без номера квартиры — её называет клиент в диалоге
      address: _pick(_streets),
      distanceKm: distance,
      payout: payout,
      itemsEt: itemsEt,
      itemsRu: itemsRu,
      confirmEt: 'Jah, $itemsEt.',
      confirmRu: 'Да, $itemsRu.',
      itemTeach: [
        ...{for (final d in chosen) d.$3},
        'g_jah',
        'g_aitah',
      ],
      scenario: scenario,
    );
  }

  /// Свежая пачка из [count] заказов с уникальными id и разными заведениями.
  static List<Order> batch([int count = 5]) {
    final base = _rnd.nextInt(1000000);
    final rs = [..._restaurants]..shuffle(_rnd);
    final custs = [..._customers]..shuffle(_rnd);
    return [
      for (var i = 0; i < count; i++)
        _buildOrder('ord_${base}_$i', rs[i], custs[i % custs.length]),
    ];
  }
}
