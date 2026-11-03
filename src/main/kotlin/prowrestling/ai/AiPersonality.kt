package prowrestling.ai

data class AiPersonality(
    val characterId: String,
    val aggression: Float,          // 0.0 to 1.0 - how aggressively the AI advances and strikes
    val reversalRate: Float,        // 0.0 to 1.0 - accuracy timing the grapple stomp cue
    val divePropensity: Float,      // 0.0 to 1.0 - likelihood of climbing turnbuckle for diving splashes
    val signatureThreshold: Float,  // 0.0 to 1.0 - opponent HP percentage below which AI prioritizes signature
    val reactionDelayFrames: Int    // baseline reaction delay frames
) {
    companion object {
        // Profiles calibrated from wrestlers.json & NES ROM disassembly
        val Profiles: Map<String, AiPersonality> = mapOf(
            "hayabusa" to AiPersonality(
                characterId = "hayabusa",
                aggression = 0.60f,
                reversalRate = 0.50f,
                divePropensity = 0.30f,
                signatureThreshold = 0.40f,
                reactionDelayFrames = 8
            ),
            "starman" to AiPersonality(
                characterId = "starman",
                aggression = 0.70f,
                reversalRate = 0.45f,
                divePropensity = 0.65f, // Star Man loves high-flying corner dives
                signatureThreshold = 0.35f,
                reactionDelayFrames = 6
            ),
            "kincornkarn" to AiPersonality(
                characterId = "kincornkarn",
                aggression = 0.65f,
                reversalRate = 0.55f,
                divePropensity = 0.15f,
                signatureThreshold = 0.45f,
                reactionDelayFrames = 9
            ),
            "giant_panther" to AiPersonality(
                characterId = "giant_panther",
                aggression = 0.55f,
                reversalRate = 0.65f, // Heavy grapple specialist
                divePropensity = 0.10f,
                signatureThreshold = 0.50f,
                reactionDelayFrames = 10
            ),
            "amazon" to AiPersonality(
                characterId = "amazon",
                aggression = 0.85f, // Relentless wild brawler
                reversalRate = 0.50f,
                divePropensity = 0.20f,
                signatureThreshold = 0.30f,
                reactionDelayFrames = 7
            ),
            "king_slender" to AiPersonality(
                characterId = "king_slender",
                aggression = 0.60f,
                reversalRate = 0.60f,
                divePropensity = 0.15f,
                signatureThreshold = 0.45f,
                reactionDelayFrames = 8
            ),
            "great_puma" to AiPersonality(
                characterId = "great_puma",
                aggression = 0.80f,
                reversalRate = 0.85f, // Supreme grapple timing near frame 14
                divePropensity = 0.40f,
                signatureThreshold = 0.25f,
                reactionDelayFrames = 4
            )
        )

        fun forCharacter(characterId: String): AiPersonality {
            return Profiles[characterId.lowercase()] ?: Profiles["hayabusa"]!!
        }
    }
}
