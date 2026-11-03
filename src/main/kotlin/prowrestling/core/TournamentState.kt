package prowrestling.core

import prowrestling.ai.DifficultyTier

/**
 * Tracks 1-Player Tournament progression through the classic 16-match championship campaign.
 *
 * Structure:
 * - Matches 1 to 5: VWA Title quest against the 5 other standard wrestlers.
 * - Match 6: VWA Championship Match (awarded VWA Championship Belt upon victory).
 * - Matches 6 to 15: 10 VWA Title Defenses against advancing difficulty contenders.
 * - Match 16: VWF Unification Title Match against the undefeated boss, The Great Puma!
 *
 * Fully decoupled from libGDX for pure headless testability.
 */
class TournamentState(val playerCharacterId: String) {

    // All 6 selectable wrestlers
    val allSelectable: List<String> = listOf(
        "hayabusa",
        "starman",
        "kincornkarn",
        "giant_panther",
        "amazon",
        "king_slender"
    )

    // Opponents for matches 1 to 5: the 5 other wrestlers excluding player
    val baseOpponents: List<String> = allSelectable.filterNot { it == playerCharacterId }

    var currentMatchIndex: Int = 1 // 1 to 16
    var wins: Int = 0
    var losses: Int = 0
    var hasVwaTitle: Boolean = false
    var hasVwfTitle: Boolean = false

    val currentOpponentId: String
        get() {
            return if (currentMatchIndex <= 5) {
                baseOpponents[(currentMatchIndex - 1) % baseOpponents.size]
            } else if (currentMatchIndex < 16) {
                // Title defense cycle
                baseOpponents[(currentMatchIndex - 6) % baseOpponents.size]
            } else {
                // Final Boss Unification match
                "great_puma"
            }
        }

    val currentDifficultyTier: DifficultyTier
        get() {
            return if (currentMatchIndex <= 2) DifficultyTier.Novice
            else if (currentMatchIndex <= 5) DifficultyTier.Contender
            else if (currentMatchIndex < 16) DifficultyTier.ChampionDefense
            else DifficultyTier.GreatPumaMaster
        }

    val matchTitle: String
        get() {
            return if (currentMatchIndex < 5) "MATCH $currentMatchIndex/16 - VWA TITLE ELIMINATION"
            else if (currentMatchIndex == 5) "MATCH 5/16 - VWA TITLE BOUT"
            else if (currentMatchIndex < 16) "DEFENSE ${currentMatchIndex - 5}/10 - VWA TITLE DEFENSE"
            else "MATCH 16/16 - VWF UNIFICATION CHAMPIONSHIP"
        }

    fun recordWin() {
        wins += 1
        if (currentMatchIndex == 5) hasVwaTitle = true
        if (currentMatchIndex == 16) hasVwfTitle = true
        currentMatchIndex += 1
    }

    fun recordLoss() {
        losses += 1
    }

    val isCompleted: Boolean get() = currentMatchIndex > 16
}
