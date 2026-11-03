package prowrestling.ai

import prowrestling.core.*
import prowrestling.input.*
import java.util.Random

class CpuController(
    val personality: AiPersonality,
    val tier: DifficultyTier = DifficultyTier.Tier1
) {
    private val rng = Random()
    private var committedAction: Pair<InputAction, Direction?> = Pair(IdleAction, null)
    private var committedFrames: Int = 0

    private var plannedGrappleFrame: Int? = null
    private var strikeCooldown: Int = 0

    fun decide(self: Wrestler, opponent: Wrestler, gameState: GameState): Pair<InputAction, Direction?> {
        // 1. If currently pinned by opponent -> Kick-out logic
        val pin = gameState.rules.activePin
        if (pin != null && pin.defender == self) {
            if (self.hp <= 0) {
                // Exhausted / KO - cannot kick out!
                return Pair(IdleAction, null)
            }

            val quarterHp = (self.maxHp * 0.25f).toInt()
            val refCounting = gameState.referee.state is RefereeCounting
            val refCount = (gameState.referee.state as? RefereeCounting)?.count ?: 0

            return if (self.hp >= quarterHp) {
                // Healthy (>= 25% energy): single press kicks out after referee counts 1
                if ((refCounting && refCount >= 1) || pin.framesInCount >= 90) {
                    Pair(AttackA, null)
                } else {
                    Pair(IdleAction, null)
                }
            } else {
                // Low energy (< 25% energy): 2-3 presses kick out at count 2
                if ((refCounting && refCount >= 2) || pin.framesInCount >= 180) {
                    // Alternate frames to trigger edge detector presses
                    if ((gameState.matchTime * 60).toInt() % 2 == 0) {
                        Pair(AttackA, null)
                    } else {
                        Pair(IdleAction, null)
                    }
                } else {
                    Pair(IdleAction, null)
                }
            }
        }

        // 2. If outside the ring, highest priority is to return inside before count-out
        if (gameState.ring.isOutsideRing(self.x, self.y)) {
            val targetX = gameState.ring.innerBounds.centerX
            val targetY = gameState.ring.innerBounds.centerY
            val dir = getNavDirection(self.x, self.y, targetX, targetY)
            return Pair(MoveAction(dir), dir)
        }

        // 3. If currently perched on a turnbuckle
        when (self.state) {
            is OnTurnbuckle -> {
                // Dive if opponent is near center or downed
                return if (opponent.isGrounded || MoveResolver.isWithinDistance(self, opponent, 140f)) {
                    Pair(AttackA, null) // Launch diving body splash!
                } else if (rng.nextFloat() < 0.08f) {
                    Pair(MoveAction(Direction.Down), Direction.Down) // Climb down
                } else {
                    Pair(IdleAction, null)
                }
            }
            is ClimbingTurnbuckle, is Diving -> {
                return Pair(IdleAction, null)
            }
            else -> {}
        }

        // 4. In Grapple Lockup -> Time the frame-14 stomp cue based on tier & reversal rate
        // 4. In Grapple Lockup -> Wait for reaction window based on difficulty tier before acting
        val session = gameState.activeGrapple
        if (session != null) {
            if (plannedGrappleFrame == null) {
                // Generous reaction delay giving human player first-strike opportunity
                val delay = when (tier.tier) {
                    0 -> 28 + rng.nextInt(12) // Tier 0 (Novice): 28-40 frames (~0.47 - 0.67s)
                    1 -> 22 + rng.nextInt(10) // Tier 1 (Contender): 22-32 frames (~0.37 - 0.53s)
                    2 -> 16 + rng.nextInt(8)  // Tier 2 (Champion Defense): 16-24 frames (~0.27 - 0.4s)
                    3 -> 10 + rng.nextInt(6)  // Tier 3 (Master Puma): 10-16 frames (~0.17 - 0.27s)
                    else -> 18
                }
                plannedGrappleFrame = session.initialCountdown - delay
            }

            val targetFrame = plannedGrappleFrame ?: 15
            return if (session.countdown <= targetFrame) {
                plannedGrappleFrame = null // Reset for next grapple

                // Move selection:
                val oppHpRatio = opponent.hp.toFloat() / opponent.maxHp.toFloat()
                if (oppHpRatio <= personality.signatureThreshold) {
                    // Prioritize Signature Move (Down + A)
                    Pair(AttackA, Direction.Down)
                } else {
                    when (rng.nextInt(4)) {
                        0 -> Pair(AttackB, if (self.facingDirection == Direction.Right) Direction.Right else Direction.Left) // Irish Whip
                        1 -> Pair(AttackA, Direction.Up)                                                 // Brainbuster / Vertical Suplex
                        2 -> Pair(AttackA, if (self.facingDirection == Direction.Right) Direction.Left else Direction.Right) // Back Suplex
                        else -> Pair(AttackB, null)                                                     // Body Slam
                    }
                }
            } else {
                Pair(IdleAction, null)
            }
        }

        plannedGrappleFrame = null // Clear when not grappling

        // If Irish whipped into ropes or rebounding, CPU cannot act or break out of run
        if (self.state is IrishWhipped || self.state is RopeRebound) {
            return Pair(IdleAction, null)
        }

        // If currently running, strike when in range or maintain run without breaking into walk
        if (self.state is Running) {
            val dx = opponent.x - self.x
            val dy = opponent.y - self.y
            val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            return if (dist in 20f..75f && Math.abs(dy) <= 20f) {
                Pair(AttackA, null)
            } else {
                Pair(IdleAction, null)
            }
        }

        // Cannot act if downed, executing, stunned, etc.
        if (!self.canAct) {
            return Pair(IdleAction, null)
        }

        // 5. If opponent is Downed on the canvas
        if (opponent.isGrounded) {
            val dist = getDistance(self.x, self.y, opponent.x, opponent.y)

            // Chance to climb top turnbuckle if character has high dive propensity
            if (personality.divePropensity > 0.40f && dist > 70f && self.y > gameState.ring.matBounds.centerY) {
                val tb = gameState.ring.nearestTurnbuckle(self.x, self.y, 35f)
                if (tb != null && (tb.corner == CornerPosition.TopLeft || tb.corner == CornerPosition.TopRight)) {
                    return Pair(MoveAction(Direction.Up), Direction.Up) // Scale corner
                }
            }

            return if (dist <= 32f) {
                // In range: pin or stomp
                if (opponent.hp < 62) {
                    Pair(AttackA, null) // Pin attempt!
                } else {
                    Pair(AttackB, null) // Ground stomp to wear them down
                }
            } else {
                // Move toward downed opponent
                val dir = getNavDirection(self.x, self.y, opponent.x, opponent.y)
                Pair(MoveAction(dir), dir)
            }
        }

        // 5b. When opponent is Irish whipped or rebounding from ropes -> Charge with running clothesline!
        if ((opponent.state is IrishWhipped || opponent.state is RopeRebound) && self.canAct) {
            val chargeDir = if (opponent.x > self.x) Direction.Right else Direction.Left
            return Pair(AttackA, chargeDir)
        }

        // 6. Neutral Play & Spacing
        if (committedFrames > 0) {
            committedFrames -= 1
            return committedAction
        }

        val dx = opponent.x - self.x
        val dy = opponent.y - self.y
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

        if (self.isRunning && dist in 26f..75f) {
            return Pair(AttackA, null)
        }

        if (dist > 120f) {
            // Far range: sprint / run horizontally toward opponent (Left or Right only)
            val runDir = if (opponent.x > self.x) Direction.Right else Direction.Left
            committedAction = Pair(RunAction(runDir), runDir)
            committedFrames = 18
            return committedAction
        } else if (dist > 36f) {
            // Mid range: approach with walk
            val dir = getNavDirection(self.x, self.y, opponent.x, opponent.y)
            return Pair(MoveAction(dir), dir)
        } else if (dist > 22f) {
            // Strike range: give player space, do not spam strikes
            if (strikeCooldown > 0) strikeCooldown -= 1

            val strikeRoll = rng.nextFloat()
            if (strikeCooldown == 0 && strikeRoll < 0.02f) {
                committedAction = Pair(AttackB, null) // Occasional punch
                committedFrames = 12
                strikeCooldown = 75 // 1.25 seconds cooldown before next strike
                return committedAction
            } else if (strikeCooldown == 0 && strikeRoll < 0.035f) {
                committedAction = Pair(AttackA, null) // Occasional kick
                committedFrames = 14
                strikeCooldown = 90 // 1.5 seconds cooldown
                return committedAction
            } else {
                // Actively step in to engage in grapple lockup
                val dir = if (dx > 0) Direction.Right else Direction.Left
                return Pair(MoveAction(dir), dir)
            }
        } else {
            // Overlapping: move directly into opponent to trigger grapple lockup
            val dir = if (dx > 0) Direction.Right else Direction.Left
            return Pair(MoveAction(dir), dir)
        }
    }

    private fun getNavDirection(fromX: Float, fromY: Float, toX: Float, toY: Float): Direction {
        val dx = toX - fromX
        val dy = toY - fromY
        return if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) Direction.Right else Direction.Left
        } else {
            if (dy > 0) Direction.Up else Direction.Down
        }
    }

    private fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
