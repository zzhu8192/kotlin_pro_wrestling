package prowrestling.core

enum class Direction {
    Up, Down, Left, Right, UpLeft, UpRight, DownLeft, DownRight;

    val opposite: Direction
        get() = when (this) {
            Up -> Down
            Down -> Up
            Left -> Right
            Right -> Left
            UpLeft -> DownRight
            UpRight -> DownLeft
            DownLeft -> UpRight
            DownRight -> UpLeft
        }
}

sealed interface WrestlerState

data object Idle : WrestlerState
data class Walking(val direction: Direction) : WrestlerState
data class Running(val direction: Direction, val hasBounced: Boolean = false) : WrestlerState
data class RopeRebound(val direction: Direction, val framesLeft: Int, val isIrishWhipped: Boolean = false) : WrestlerState
data class Stunned(val framesLeft: Int) : WrestlerState
data class Flinching(val framesLeft: Int, val totalFrames: Int, val willKnockDown: Boolean, val knockDownFrames: Int = 0) : WrestlerState
data class Downed(val framesLeft: Int) : WrestlerState
data object Grappling : WrestlerState
data class Executing(val moveId: String, val currentFrame: Int, val totalFrames: Int) : WrestlerState
data class BeingThrown(val moveId: String, val currentFrame: Int, val totalFrames: Int) : WrestlerState
data object Pinning : WrestlerState
data class BeingPinned(val kickOutProgress: Float) : WrestlerState
data class IrishWhipped(val direction: Direction, val hasBounced: Boolean = false) : WrestlerState
data class ClimbingTurnbuckle(val corner: Turnbuckle, val framesLeft: Int) : WrestlerState
data class OnTurnbuckle(val corner: Turnbuckle) : WrestlerState
data class Diving(val corner: Turnbuckle, val startX: Float, val startY: Float, val targetX: Float, val targetY: Float, val progress: Float) : WrestlerState
data class FlyingCrossChop(val direction: Direction, val framesLeft: Int) : WrestlerState
data class ExitingRing(val progress: Float) : WrestlerState
data object OutsideRing : WrestlerState

class Wrestler(
    val characterId: String = "hayabusa",
    val isPlayer1: Boolean = true,
    var x: Float = 240f,
    var y: Float = 160f
) {
    var hp: Int = 240
    val maxHp: Int = 240
    var stamina: Int = 100
    var consecutivePunchHits: Int = 0

    // Visual appearance per character
    val trunksColorHex: Int = when (characterId.lowercase()) {
        "starman" -> 0x992299 // Magenta / Purple trunks
        "kincornkarn" -> 0xB8860B // Gold / Ochre
        "giant_panther" -> 0x2244AA // Blue
        "amazon" -> 0x2E8B57 // Green
        "king_slender" -> 0xD2691E // Rust / Orange
        "great_puma" -> 0xDA70D6 // Orchid
        else -> 0xCC1111 // Crimson Red (Hayabusa default)
    }

    val isMasked: Boolean = when (characterId.lowercase()) {
        "starman", "great_puma" -> true
        else -> false
    }

    var facingDirection: Direction = if (isPlayer1) Direction.Right else Direction.Left
    var state: WrestlerState = Idle

    // Movement speed constants (calibrated to 60 FPS in 480x360 virtual space, slowed by 25%)
    val walkSpeedX: Float = 2.0f
    val walkSpeedY: Float = 1.5f
    val runSpeed: Float = 3.6f
    val reboundSpeed: Float = 4.6f

    // Wrestler body & foot dimensions (1.5x scale)
    val width: Float = 42f
    val height: Float = 81f

    val footBounds: Rect get() = Rect(x - width / 2f, y - 9f, width, 18f)
    val bodyBounds: Rect get() = Rect(x - width / 2f, y, width, height)

    val isRunning: Boolean
        get() = when (state) {
            is Running, is RopeRebound, is FlyingCrossChop, is IrishWhipped -> true
            else -> false
        }

    val isGrounded: Boolean
        get() = when (state) {
            is Downed, is BeingPinned -> true
            else -> false
        }

    val canAct: Boolean
        get() = when (state) {
            Idle, is Walking -> true
            else -> false
        }

    /**
     * Calculates the duration (in 60 FPS frames) the wrestler stays downed on the mat.
     * Scales with damage taken:
     * - Full HP (0% damage): baseFrames (~1.5s, 90 frames minimum).
     * - Zero HP (100% damage / no energy): up to 600 frames (10.0 seconds).
     */
    fun calculateDownedFrames(baseFrames: Int = 90): Int {
        val damageRatio = (1.0f - (hp.toFloat() / maxHp.toFloat())).coerceIn(0f, 1f)
        val minFrames = Math.max(90, baseFrames)
        val maxFrames = 600 // 10.0 seconds at 60 FPS
        return (minFrames + (maxFrames - minFrames) * damageRatio).toInt()
    }
}
