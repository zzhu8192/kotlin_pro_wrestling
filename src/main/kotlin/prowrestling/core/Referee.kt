package prowrestling.core

sealed interface RefereeState

data object RefereeIdle : RefereeState
data class RefereeMoving(val targetX: Float, val targetY: Float, val isRunning: Boolean) : RefereeState
data class RefereeDropping(val framesLeft: Int) : RefereeState
data class RefereeCounting(val count: Int, val framesInCount: Int) : RefereeState
data class RefereeSignaling(val signal: String, val framesLeft: Int) : RefereeState
data class RefereeRaisingHand(val winner: Wrestler) : RefereeState

class Referee(
    var x: Float = 240f,
    var y: Float = 190f
) {
    var facingDirection: Direction = Direction.Down
    var state: RefereeState = RefereeIdle

    val walkSpeed: Float = 1.6f
    val runSpeed: Float = 3.6f

    val width: Float = 22f
    val height: Float = 48f

    // 60 frames = 1.0 second per pin count / ring-out count
    val countDurationFrames: Int = 60
    val dropDurationFrames: Int = 16

    val isCountingPin: Boolean
        get() = when (state) {
            is RefereeCounting, is RefereeDropping -> true
            else -> false
        }
}
