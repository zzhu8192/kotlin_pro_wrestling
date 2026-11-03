package prowrestling.input

import prowrestling.core.Direction

/**
 * Detects double-tap inputs strictly for horizontal running (Left and Right).
 * Running can only be initiated by physically tapping a horizontal direction (held <= 160ms),
 * physically releasing it for a human gap (35ms <= gap <= 200ms),
 * and pressing it again.
 * Holding a direction key will NEVER initiate running.
 * Up and Down will NEVER initiate running.
 */
class DoubleTapDetector(
    val maxTapDurationSeconds: Float = 0.16f,
    val minReleaseGapSeconds: Float = 0.035f,
    val maxReleaseGapSeconds: Float = 0.20f
) {
    constructor(windowSeconds: Float) : this(
        maxTapDurationSeconds = 0.16f,
        minReleaseGapSeconds = 0.035f,
        maxReleaseGapSeconds = windowSeconds
    )

    private enum class State {
        IDLE,
        FIRST_TAP_DOWN,
        FIRST_TAP_UP,
        RUNNING
    }

    private var state: State = State.IDLE
    private var trackedDir: Direction? = null
    private var tapDownTimer: Float = 0f
    private var releaseTimer: Float = 0f
    private var isDisqualifiedHold: Boolean = false

    fun update(delta: Float) {
        when (state) {
            State.FIRST_TAP_DOWN -> {
                tapDownTimer += delta
                if (tapDownTimer > maxTapDurationSeconds) {
                    // Held down longer than quick tap duration -> this is a sustained WALK, not a tap!
                    isDisqualifiedHold = true
                }
            }

            State.FIRST_TAP_UP -> {
                releaseTimer += delta
                if (releaseTimer > maxReleaseGapSeconds) {
                    // Too long gap -> double tap timed out
                    reset()
                }
            }

            State.RUNNING -> {
                // Maintained until released
            }

            State.IDLE -> {}
        }
    }

    /**
     * Called when direction key is pressed down.
     */
    fun onDirectionJustPressed(dir: Direction): Boolean {
        if (dir != Direction.Left && dir != Direction.Right) {
            reset()
            return false
        }

        return when (state) {
            State.FIRST_TAP_UP -> {
                if (trackedDir == dir && releaseTimer >= minReleaseGapSeconds && releaseTimer <= maxReleaseGapSeconds) {
                    // Legitimate double-tap!
                    state = State.RUNNING
                    true
                } else {
                    // Different direction or gap too short (OS auto-repeat): restart tap tracking
                    trackedDir = dir
                    tapDownTimer = 0f
                    releaseTimer = 0f
                    isDisqualifiedHold = false
                    state = State.FIRST_TAP_DOWN
                    false
                }
            }

            State.FIRST_TAP_DOWN -> {
                // Key pressed again while already considered down (e.g. repeat): do nothing
                false
            }

            State.RUNNING -> {
                trackedDir == dir
            }

            State.IDLE -> {
                trackedDir = dir
                tapDownTimer = 0f
                releaseTimer = 0f
                isDisqualifiedHold = false
                state = State.FIRST_TAP_DOWN
                false
            }
        }
    }

    /**
     * Called when direction key is released.
     */
    fun onDirectionReleased(dir: Direction) {
        if (dir != Direction.Left && dir != Direction.Right) return

        if (trackedDir == dir) {
            when (state) {
                State.FIRST_TAP_DOWN -> {
                    if (isDisqualifiedHold || tapDownTimer > maxTapDurationSeconds) {
                        // Was a sustained hold (walk), not a quick tap -> reset to IDLE
                        reset()
                    } else {
                        // Quick tap completed! Arm for second tap
                        state = State.FIRST_TAP_UP
                        releaseTimer = 0f
                    }
                }

                State.RUNNING -> {
                    // Releasing key while running immediately stops running!
                    reset()
                }

                else -> {}
            }
        }
    }

    val currentlyRunning: Boolean
        get() = state == State.RUNNING

    fun reset() {
        trackedDir = null
        tapDownTimer = 0f
        releaseTimer = 0f
        isDisqualifiedHold = false
        state = State.IDLE
    }
}
