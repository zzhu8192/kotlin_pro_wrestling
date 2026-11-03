package prowrestling.core

data class WrestlerData(
    val id: String,
    val name: String,
    val country: String,
    val displayOrder: Int,
    val hp: Int,
    val walkSpeedX: Float,
    val walkSpeedY: Float,
    val runSpeed: Float,
    val weight: Int,
    val description: String,
    val signatureMoveName: String,
    val signatureDamage: Int,
    val trunksColorHex: Int,
    val isMasked: Boolean
) {
    val speed: Float get() = runSpeed

    companion object {
        val Roster: Map<String, WrestlerData> = mapOf(
            "hayabusa" to WrestlerData(
                id = "hayabusa",
                name = "Fighter Hayabusa",
                country = "Japan",
                displayOrder = 1,
                hp = 240,
                walkSpeedX = 1.0625f,
                walkSpeedY = 0.8125f,
                runSpeed = 2.5f,
                weight = 3,
                description = "Balanced Japanese martial artist with devastating Back Brain Kick.",
                signatureMoveName = "Back Brain Kick",
                signatureDamage = 28,
                trunksColorHex = 0xCC1111,
                isMasked = false
            ),
            "starman" to WrestlerData(
                id = "starman",
                name = "Star Man",
                country = "Mexico",
                displayOrder = 2,
                hp = 240,
                walkSpeedX = 1.0625f,
                walkSpeedY = 0.8125f,
                runSpeed = 2.5f,
                weight = 2,
                description = "High-flying acrobatic luchador with the Somersault Kick.",
                signatureMoveName = "Somersault Kick",
                signatureDamage = 26,
                trunksColorHex = 0x992299,
                isMasked = true
            ),
            "kincornkarn" to WrestlerData(
                id = "kincornkarn",
                name = "Kin Corn Karn",
                country = "Korea",
                displayOrder = 3,
                hp = 240,
                walkSpeedX = 1.0625f,
                walkSpeedY = 0.8125f,
                runSpeed = 2.5f,
                weight = 3,
                description = "Martial artist with lethal Mongolian Chops and Karate Kicks.",
                signatureMoveName = "Mongolian Chop",
                signatureDamage = 24,
                trunksColorHex = 0xB8860B,
                isMasked = false
            ),
            "giant_panther" to WrestlerData(
                id = "giant_panther",
                name = "Giant Panther",
                country = "USA",
                displayOrder = 4,
                hp = 240,
                walkSpeedX = 1.0625f,
                walkSpeedY = 0.8125f,
                runSpeed = 2.5f,
                weight = 4,
                description = "American heavyweight powerhouse with the crushing Iron Claw.",
                signatureMoveName = "Iron Claw",
                signatureDamage = 30,
                trunksColorHex = 0x2244AA,
                isMasked = false
            ),
            "amazon" to WrestlerData(
                id = "amazon",
                name = "The Amazon",
                country = "Unknown",
                displayOrder = 5,
                hp = 240,
                walkSpeedX = 1.0625f,
                walkSpeedY = 0.8125f,
                runSpeed = 2.5f,
                weight = 3,
                description = "Wild brawler who breaks the rules with the vicious Piranha Bite.",
                signatureMoveName = "Piranha Bite",
                signatureDamage = 26,
                trunksColorHex = 0x2E8B57,
                isMasked = false
            ),
            "king_slender" to WrestlerData(
                id = "king_slender",
                name = "King Slender",
                country = "USA",
                displayOrder = 6,
                hp = 240,
                walkSpeedX = 1.0625f,
                walkSpeedY = 0.8125f,
                runSpeed = 2.5f,
                weight = 3,
                description = "Technical American champion with the Slender Bender Backbreaker.",
                signatureMoveName = "Backbreaker",
                signatureDamage = 34,
                trunksColorHex = 0xD2691E,
                isMasked = false
            ),
            "great_puma" to WrestlerData(
                id = "great_puma",
                name = "The Great Puma",
                country = "Unknown",
                displayOrder = 7,
                hp = 240,
                walkSpeedX = 1.0625f,
                walkSpeedY = 0.8125f,
                runSpeed = 2.5f,
                weight = 4,
                description = "The final boss and undefeated VWF champion. Master of all techniques.",
                signatureMoveName = "All Techniques",
                signatureDamage = 38,
                trunksColorHex = 0xDA70D6,
                isMasked = true
            )
        )

        fun load(characterId: String): WrestlerData {
            return Roster[characterId.lowercase()] ?: Roster["hayabusa"]!!
        }

        fun forId(characterId: String): WrestlerData = load(characterId)

        val Hayabusa: WrestlerData by lazy { Roster["hayabusa"]!! }
        val StarMan: WrestlerData by lazy { Roster["starman"]!! }
        val KinCornKarn: WrestlerData by lazy { Roster["kincornkarn"]!! }
        val GiantPanther: WrestlerData by lazy { Roster["giant_panther"]!! }
        val TheAmazon: WrestlerData by lazy { Roster["amazon"]!! }
        val KingSlender: WrestlerData by lazy { Roster["king_slender"]!! }
        val GreatPuma: WrestlerData by lazy { Roster["great_puma"]!! }
    }
}
