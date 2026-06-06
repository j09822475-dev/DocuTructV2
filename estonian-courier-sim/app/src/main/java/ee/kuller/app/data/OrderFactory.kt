package ee.kuller.app.data

import ee.kuller.app.model.Order
import ee.kuller.app.model.Scenario
import kotlin.random.Random

/**
 * Процедурная генерация заказов.
 *
 * Заведение, клиент, адрес, состав заказа и сценарий выбираются случайно, но
 * СЦЕНАРИЙ и БЛЮДА привязаны к ТИПУ заведения (пиццерия чаще «оставить под
 * дверью» и отдаёт пиццу; кофейня — доставка в офис и т.д.).
 */
object OrderFactory {

    private enum class Venue { PIZZA, SUSHI, ASIAN, BURGER, CAFE, SOUP, BAKERY, GENERIC }

    private data class Restaurant(val name: String, val venue: Venue)
    private data class Dish(val et: String, val ru: String, val teach: String, val venue: Venue?)

    private val restaurants = listOf(
        Restaurant("Pizza Grande", Venue.PIZZA),
        Restaurant("Pitsabaar Napoli", Venue.PIZZA),
        Restaurant("Pizzakuller", Venue.PIZZA),
        Restaurant("Sushi Aasia", Venue.SUSHI),
        Restaurant("Sushi Tokyo", Venue.SUSHI),
        Restaurant("Wasabi Bar", Venue.SUSHI),
        Restaurant("Nuudlibaar Wok", Venue.ASIAN),
        Restaurant("Aasia Express", Venue.ASIAN),
        Restaurant("Indica Curry", Venue.ASIAN),
        Restaurant("Burger Maja", Venue.BURGER),
        Restaurant("Grillinurk", Venue.BURGER),
        Restaurant("Kebabimaja", Venue.BURGER),
        Restaurant("Kohvik Sõbralik", Venue.CAFE),
        Restaurant("Tere Kohvik", Venue.CAFE),
        Restaurant("Päikese Kohvik", Venue.CAFE),
        Restaurant("Hommiku Kohvik", Venue.CAFE),
        Restaurant("Tervisekohvik", Venue.CAFE),
        Restaurant("Supiköök", Venue.SOUP),
        Restaurant("Roheline Taldrik", Venue.SOUP),
        Restaurant("Toidukoda", Venue.SOUP),
        Restaurant("Magus Pagar", Venue.BAKERY),
        Restaurant("Pelmeenikohvik", Venue.BAKERY),
        Restaurant("Kala & Salat", Venue.GENERIC),
        Restaurant("Vana Linn Bistroo", Venue.GENERIC),
        Restaurant("Mere Restoran", Venue.GENERIC),
        Restaurant("Maailma Köök", Venue.GENERIC),
        Restaurant("Kuldne Lusikas", Venue.GENERIC),
    )

    private val dishes = listOf(
        Dish("pepperoni pitsa", "пицца пепперони", "f_pitsa", Venue.PIZZA),
        Dish("suur juustupitsa", "большая пицца с сыром", "f_pitsa", Venue.PIZZA),
        Dish("kanaburger", "куриный бургер", "f_burger", Venue.BURGER),
        Dish("juustuburger", "чизбургер", "f_burger", Venue.BURGER),
        Dish("friikartulid", "картофель фри", "f_friikad", Venue.BURGER),
        Dish("kanasupp", "куриный суп", "f_supp", Venue.SOUP),
        Dish("seenesupp", "грибной суп", "f_supp", Venue.SOUP),
        Dish("kreeka salat", "греческий салат", "f_salat", Venue.CAFE),
        Dish("caesari salat", "салат цезарь", "f_salat", Venue.CAFE),
        Dish("sai juustuga", "булка с сыром", "f_sai", Venue.CAFE),
        Dish("must leib", "чёрный хлеб", "f_leib", Venue.CAFE),
        Dish("praetud kala", "жареная рыба", "f_kala", Venue.GENERIC),
        Dish("praetud kana", "жареная курица", "f_kana", Venue.GENERIC),
        Dish("pelmeenid", "пельмени", "f_pelmeenid", Venue.GENERIC),
        Dish("sushikomplekt", "суши-сет", "f_sushi", Venue.SUSHI),
        Dish("lõhesushi", "суши с лососем", "f_sushi", Venue.SUSHI),
        Dish("kana karri", "куриное карри", "f_karri", Venue.ASIAN),
        Dish("aasia nuudlid", "азиатская лапша", "f_nuudlid", Venue.ASIAN),
        Dish("kanawrap", "куриный ролл", "f_wrap", Venue.ASIAN),
        Dish("šokolaadikook", "шоколадный торт", "f_kook", Venue.BAKERY),
        Dish("vaniljejäätis", "ванильное мороженое", "f_jaatis", Venue.BAKERY),
        Dish("pannkook moosiga", "блин с вареньем", "f_pannkook", Venue.BAKERY),
        // Напитки и дополнения (подходят к любому заведению)
        Dish("kohv piimaga", "кофе с молоком", "d_kohv", null),
        Dish("roheline tee", "зелёный чай", "d_tee", null),
        Dish("õunamahl", "яблочный сок", "d_mahl", null),
        Dish("klaas vett", "стакан воды", "d_vesi", null),
        Dish("limonaad", "лимонад", "d_limonaad", null),
        Dish("marjasmuuti", "ягодный смузи", "d_smuuti", null),
        Dish("jõhvikamorss", "клюквенный морс", "d_morss", null),
        Dish("kakao", "какао", "d_kakao", null),
    )

    private val customers = listOf(
        "Maarja", "Andres", "Kati", "Pjotr", "Olga", "Mikk", "Tiina", "Rein", "Jaan", "Liis",
        "Mart", "Kristi", "Toomas", "Eve", "Marko", "Anna", "Sergei", "Jelena", "Nikolai",
        "Kristjan", "Heli", "Priit", "Karl", "Maria", "Dmitri", "Triin", "Urmas", "Helena",
        "Aleksei", "Kalev",
    )

    private val streets = listOf(
        "Pärnu maantee 12", "Narva maantee 5", "Tartu maantee 40", "Liivalaia 8",
        "Sõpruse puiestee 21", "Vana-Posti 7", "Roosikrantsi 9", "Tehnika 18", "Mustamäe tee 16",
        "Endla 45", "Tornimäe 3", "Sadama 6", "Estonia puiestee 9", "Gonsiori 14", "Pikk 24",
        "Lai 7", "Viru 11", "Kreutzwaldi 25", "Toompuiestee 30", "Akadeemia tee 21",
        "Paldiski maantee 52", "Kadaka tee 4", "Õismäe tee 88", "Lasnamäe 15", "Kalevi 6",
    )

    /** Сценарии, типичные для каждого типа заведения. */
    private val scenarioByVenue: Map<Venue, List<Scenario>> = mapOf(
        Venue.PIZZA to listOf(Scenario.LEAVE_DOOR, Scenario.FACE_DOOR, Scenario.CASH, Scenario.LATE, Scenario.SPOILED),
        Venue.SUSHI to listOf(Scenario.LIFT_BROKEN, Scenario.GATE_CODE, Scenario.FACE_DOOR, Scenario.OFFICE, Scenario.SPOILED, Scenario.COMPLAINT, Scenario.INTERCOM),
        Venue.ASIAN to listOf(Scenario.FACE_DOOR, Scenario.OFFICE, Scenario.LATE, Scenario.WRONG_ADDRESS, Scenario.COMPLAINT, Scenario.INTERCOM),
        Venue.BURGER to listOf(Scenario.FACE_DOOR, Scenario.LEAVE_DOOR, Scenario.CASH, Scenario.BREAKDOWN),
        Venue.CAFE to listOf(Scenario.OFFICE, Scenario.FACE_DOOR, Scenario.DIRECTIONS, Scenario.LATE, Scenario.COMPLAINT, Scenario.INTERCOM),
        Venue.SOUP to listOf(Scenario.GATE_CODE, Scenario.FACE_DOOR, Scenario.DIRECTIONS, Scenario.SPOILED, Scenario.INTERCOM),
        Venue.BAKERY to listOf(Scenario.OFFICE, Scenario.FACE_DOOR, Scenario.CANCELLED, Scenario.LATE),
        Venue.GENERIC to Scenario.entries.toList(),
    )

    private fun buildOrder(id: String, r: Restaurant, customer: String): Order {
        val mains = (if (r.venue == Venue.GENERIC) dishes.filter { it.venue != null }
        else dishes.filter { it.venue == r.venue }).ifEmpty { dishes.filter { it.venue != null } }
        val main = mains.random()
        val chosen = buildList {
            add(main)
            if (Random.nextBoolean()) add(dishes.filter { it.venue == null }.random())
        }
        val itemsEt = chosen.joinToString(" ja ") { it.et }
        val itemsRu = chosen.joinToString(" и ") { it.ru }
        val korter = Random.nextInt(1, 40)
        val payout = (300 + Random.nextInt(350)) / 100.0
        val distance = (8 + Random.nextInt(45)) / 10.0
        val scenario = (scenarioByVenue[r.venue] ?: Scenario.entries.toList()).random()

        return Order(
            id = id,
            restaurant = r.name,
            customer = customer,
            address = "${streets.random()}, korter $korter",
            distanceKm = distance,
            payout = payout,
            itemsEt = itemsEt,
            itemsRu = itemsRu,
            confirmEt = "Jah, $itemsEt. Aitäh!",
            confirmRu = "Да, $itemsRu. Спасибо!",
            itemTeach = (chosen.map { it.teach }.distinct() + listOf("g_jah", "g_aitah")),
            scenario = scenario
        )
    }

    /** Свежая пачка из [count] заказов с уникальными id и разными заведениями. */
    fun batch(count: Int = 5): List<Order> {
        val base = Random.nextInt(1_000_000)
        val rs = restaurants.shuffled().take(count)
        val custs = customers.shuffled()
        return rs.mapIndexed { i, r -> buildOrder("ord_${base}_$i", r, custs[i % custs.size]) }
    }
}
