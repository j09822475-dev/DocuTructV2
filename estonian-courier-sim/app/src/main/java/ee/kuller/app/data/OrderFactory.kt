package ee.kuller.app.data

import ee.kuller.app.model.Order
import ee.kuller.app.model.Scenario
import kotlin.random.Random

/**
 * Процедурная генерация заказов: ресторан, клиент, адрес, состав заказа и
 * сценарий выбираются случайно из больших пулов, поэтому заказы каждый раз
 * разные. Учебные слова (teach) всегда ссылаются на существующий словарь.
 */
object OrderFactory {

    private data class Dish(val et: String, val ru: String, val teach: String)

    private val dishes = listOf(
        Dish("pepperoni pitsa", "пицца пепперони", "f_pitsa"),
        Dish("suur juustupitsa", "большая пицца с сыром", "f_pitsa"),
        Dish("kanaburger", "куриный бургер", "f_burger"),
        Dish("juustuburger", "чизбургер", "f_burger"),
        Dish("friikartulid", "картофель фри", "f_friikad"),
        Dish("kanasupp", "куриный суп", "f_supp"),
        Dish("seenesupp", "грибной суп", "f_supp"),
        Dish("kreeka salat", "греческий салат", "f_salat"),
        Dish("caesari salat", "салат цезарь", "f_salat"),
        Dish("praetud kala", "жареная рыба", "f_kala"),
        Dish("praetud kana", "жареная курица", "f_kana"),
        Dish("must leib", "чёрный хлеб", "f_leib"),
        Dish("sai juustuga", "булка с сыром", "f_sai"),
        Dish("sushikomplekt", "суши-сет", "f_sushi"),
        Dish("lõhesushi", "суши с лососем", "f_sushi"),
        Dish("kana karri", "куриное карри", "f_karri"),
        Dish("aasia nuudlid", "азиатская лапша", "f_nuudlid"),
        Dish("pelmeenid", "пельмени", "f_pelmeenid"),
        Dish("kanawrap", "куриный ролл", "f_wrap"),
        Dish("šokolaadikook", "шоколадный торт", "f_kook"),
        Dish("vaniljejäätis", "ванильное мороженое", "f_jaatis"),
        Dish("pannkook moosiga", "блин с вареньем", "f_pannkook"),
        Dish("kohv piimaga", "кофе с молоком", "d_kohv"),
        Dish("roheline tee", "зелёный чай", "d_tee"),
        Dish("õunamahl", "яблочный сок", "d_mahl"),
        Dish("klaas vett", "стакан воды", "d_vesi"),
        Dish("limonaad", "лимонад", "d_limonaad"),
        Dish("marjasmuuti", "ягодный смузи", "d_smuuti"),
        Dish("jõhvikamorss", "клюквенный морс", "d_morss"),
        Dish("kakao", "какао", "d_kakao"),
    )

    private val restaurants = listOf(
        "Pizza Grande", "Sushi Aasia", "Burger Maja", "Kohvik Sõbralik", "Supiköök",
        "Kala & Salat", "Pitsabaar Napoli", "Indica Curry", "Sushi Tokyo", "Nuudlibaar Wok",
        "Pelmeenikohvik", "Maailma Köök", "Tere Kohvik", "Roheline Taldrik", "Kebabimaja",
        "Vana Linn Bistroo", "Mere Restoran", "Päikese Kohvik", "Grillinurk", "Magus Pagar",
        "Tervisekohvik", "Kuldne Lusikas", "Toidukoda", "Aasia Express", "Hommiku Kohvik",
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

    private fun randomOrder(id: String): Order {
        val n = if (Random.nextBoolean()) 2 else 1
        val chosen = dishes.shuffled().take(n)
        val itemsEt = chosen.joinToString(" ja ") { it.et }
        val itemsRu = chosen.joinToString(" и ") { it.ru }
        val korter = Random.nextInt(1, 40)
        val payout = (300 + Random.nextInt(350)) / 100.0
        val distance = (8 + Random.nextInt(45)) / 10.0

        return Order(
            id = id,
            restaurant = restaurants.random(),
            customer = customers.random(),
            address = "${streets.random()}, korter $korter",
            distanceKm = distance,
            payout = payout,
            itemsEt = itemsEt,
            itemsRu = itemsRu,
            confirmEt = "Jah, $itemsEt. Aitäh!",
            confirmRu = "Да, $itemsRu. Спасибо!",
            itemTeach = (chosen.map { it.teach }.distinct() + listOf("g_jah", "g_aitah")),
            scenario = Scenario.entries.random()
        )
    }

    /** Свежая пачка из [count] заказов с уникальными id и разными заведениями. */
    fun batch(count: Int = 5): List<Order> {
        val base = Random.nextInt(1_000_000)
        val rests = restaurants.shuffled()
        val custs = customers.shuffled()
        return (0 until count).map { i ->
            randomOrder("ord_${base}_$i").copy(
                restaurant = rests[i % rests.size],
                customer = custs[i % custs.size]
            )
        }
    }
}
