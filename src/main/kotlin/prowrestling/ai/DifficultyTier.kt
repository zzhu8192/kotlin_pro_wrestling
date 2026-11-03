package prowrestling.ai

data class DifficultyTier(
    val tier: Int,
    val name: String,
    val matchRange: String,
    val reactionOffsetMin: Int,
    val reactionOffsetMax: Int,
    val reversalRateMultiplier: Float,
    val mashFrequency: Float,     // Probability of mashing kickout per frame when pinned
    val strikeDodgeRate: Float
) {
    companion object {
        val Tier0 = DifficultyTier(
            tier = 0,
            name = "Novice (Matches 1-4)",
            matchRange = "1-4",
            reactionOffsetMin = 6,
            reactionOffsetMax = 9,
            reversalRateMultiplier = 0.50f,
            mashFrequency = 0.35f,
            strikeDodgeRate = 0.25f
        )

        val Tier1 = DifficultyTier(
            tier = 1,
            name = "Contender (Matches 5-8)",
            matchRange = "5-8",
            reactionOffsetMin = 4,
            reactionOffsetMax = 7,
            reversalRateMultiplier = 0.70f,
            mashFrequency = 0.50f,
            strikeDodgeRate = 0.38f
        )

        val Tier2 = DifficultyTier(
            tier = 2,
            name = "Champion Defense (Matches 9-12)",
            matchRange = "9-12",
            reactionOffsetMin = 2,
            reactionOffsetMax = 5,
            reversalRateMultiplier = 0.85f,
            mashFrequency = 0.65f,
            strikeDodgeRate = 0.50f
        )

        val Tier3 = DifficultyTier(
            tier = 3,
            name = "Master / Great Puma (Matches 13-16)",
            matchRange = "13-16",
            reactionOffsetMin = 0,
            reactionOffsetMax = 2,
            reversalRateMultiplier = 1.00f,
            mashFrequency = 0.85f,
            strikeDodgeRate = 0.65f
        )

        val Novice = Tier0
        val Contender = Tier1
        val ChampionDefense = Tier2
        val GreatPumaMaster = Tier3

        val AllTiers = arrayOf(Tier0, Tier1, Tier2, Tier3)

        fun forMatchNumber(matchNum: Int): DifficultyTier {
            val tierIdx = Math.max(0, Math.min(3, (matchNum - 1) / 4))
            return AllTiers[tierIdx]
        }
    }
}
