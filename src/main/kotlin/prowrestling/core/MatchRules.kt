package prowrestling.core

sealed interface MatchResult
data object InProgress : MatchResult
data class MatchWonByPin(val winner: Wrestler, val loser: Wrestler) : MatchResult
data class MatchWonByCountOut(val winner: Wrestler, val loser: Wrestler) : MatchResult
data object MatchDraw : MatchResult

data class ActivePin(
    val attacker: Wrestler,
    val defender: Wrestler,
    var currentCount: Int = 0,
    var framesInCount: Int = 0,
    var pressCount: Int = 0,
    var isBroken: Boolean = false
) {
    /**
     * Required button presses to kick out:
     * - As long as there's energy (>= 25% maxHp): a single press (1 press) kicks out.
     * - Less than 25% energy: 2-3 presses kick out.
     * - 0 energy (no energy): cannot kick out.
     */
    val requiredPresses: Int
        get() {
            val quarterHp = (defender.maxHp * 0.25f).toInt() // 60 HP for maxHp 240
            return when {
                defender.hp <= 0 -> 999
                defender.hp >= quarterHp -> 1
                defender.hp >= (quarterHp / 2) -> 2 // 30..59 HP
                else -> 3 // 1..29 HP
            }
        }

    val requiredThreshold: Float
        get() = requiredPresses.toFloat()

    var kickOutMeter: Float
        get() = pressCount.toFloat()
        set(value) { pressCount = value.toInt() }

    fun registerPress() {
        pressCount += 1
    }

    fun registerMash() {
        registerPress()
    }

    fun canKickOut(): Boolean = pressCount >= requiredPresses
}

class MatchRules(val ring: Ring) {
    var activePin: ActivePin? = null
    var ringOutCount: Int = 0
    var ringOutTimerFrames: Int = 0
    var matchResult: MatchResult = InProgress
    private var defenderWasMashing: Boolean = false

    fun initiatePin(attacker: Wrestler, defender: Wrestler): ActivePin {
        attacker.state = Pinning
        defender.state = BeingPinned(0f)

        // Position attacker draped across defender
        attacker.x = defender.x
        attacker.y = defender.y + 6f
        attacker.facingDirection = defender.facingDirection

        defenderWasMashing = false
        val pin = ActivePin(attacker, defender)
        activePin = pin
        return pin
    }

    fun breakPin() {
        activePin?.let { pin ->
            pin.isBroken = true
            val totalFrames = 20
            // Initiate multi-frame kickout motion: defender bridges and launches attacker up and off
            pin.defender.state = Executing("kick_out", 0, totalFrames)
            pin.attacker.state = BeingThrown("kick_out", 0, totalFrames)
        }
        activePin = null
        defenderWasMashing = false
    }

    fun update(
        w1: Wrestler,
        w2: Wrestler,
        referee: Referee,
        p1Mashed: Boolean,
        p2Mashed: Boolean
    ): MatchResult {
        if (matchResult != InProgress) return matchResult

        val currentPin = activePin
        if (currentPin != null) {
            // Register button presses from defender (edge-detected so holding doesn't rapid-fire)
            val defenderMashed = if (currentPin.defender.isPlayer1) p1Mashed else p2Mashed
            if (defenderMashed && !defenderWasMashing) {
                currentPin.registerPress()
            }
            defenderWasMashing = defenderMashed

            // Check kick-out
            if (currentPin.canKickOut()) {
                breakPin()
                referee.state = RefereeSignaling("kick_out", framesLeft = 30)
                return InProgress
            }

            // Referee countdown handling
            when (val refState = referee.state) {
                is RefereeCounting -> {
                    currentPin.framesInCount = refState.framesInCount
                    if (refState.framesInCount >= referee.countDurationFrames) {
                        val nextCount = refState.count + 1
                        currentPin.currentCount = nextCount

                        if (nextCount >= 3) {
                            // Pinfall victory!
                            matchResult = MatchWonByPin(currentPin.attacker, currentPin.defender)
                            referee.state = RefereeRaisingHand(currentPin.attacker)
                            return matchResult
                        } else {
                            referee.state = RefereeCounting(nextCount, 0)
                        }
                    } else {
                        referee.state = RefereeCounting(refState.count, refState.framesInCount + 1)
                    }
                }

                is RefereeDropping -> {
                    if (refState.framesLeft <= 1) {
                        referee.state = RefereeCounting(0, 0)
                    } else {
                        referee.state = RefereeDropping(refState.framesLeft - 1)
                    }
                }

                is RefereeMoving -> {
                    // Run to pin position beside the bodies
                    val dx = refState.targetX - referee.x
                    val dy = refState.targetY - referee.y
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    if (dist <= referee.runSpeed) {
                        referee.x = refState.targetX
                        referee.y = refState.targetY
                        referee.state = RefereeDropping(referee.dropDurationFrames)
                    } else {
                        referee.x += (dx / dist) * referee.runSpeed
                        referee.y += (dy / dist) * referee.runSpeed
                    }
                }

                else -> {
                    // Start moving to pin position
                    val targetX = currentPin.defender.x
                    val targetY = currentPin.defender.y + 24f
                    referee.state = RefereeMoving(targetX, targetY, isRunning = true)
                }
            }
        } else {
            // 2. Process Ring-Out Count if a wrestler is on the floor
            val w1Out = ring.isOutsideRing(w1.x, w1.y)
            val w2Out = ring.isOutsideRing(w2.x, w2.y)

            if (w1Out || w2Out) {
                ringOutTimerFrames += 1
                if (ringOutTimerFrames >= 60) {
                    ringOutTimerFrames = 0
                    ringOutCount += 1

                    referee.state = RefereeSignaling("ringout_$ringOutCount", framesLeft = 40)

                    if (ringOutCount >= 20) {
                        // Count out!
                        matchResult = if (w1Out && !w2Out) MatchWonByCountOut(w2, w1)
                        else if (w2Out && !w1Out) MatchWonByCountOut(w1, w2)
                        else MatchDraw
                        referee.state = RefereeSignaling("count_out_end", framesLeft = 120)
                        return matchResult
                    }
                }
            } else {
                // Both in ring
                if (ringOutCount > 0) {
                    ringOutCount = 0
                    ringOutTimerFrames = 0
                    referee.state = RefereeIdle
                }
            }

            // 3. Referee ambient movement to stay near the action
            updateRefereeAmbient(referee, w1, w2)
        }

        return matchResult
    }

    private fun updateRefereeAmbient(referee: Referee, w1: Wrestler, w2: Wrestler) {
        when (val refState = referee.state) {
            RefereeIdle -> {
                val midX = (w1.x + w2.x) / 2f
                val midY = (w1.y + w2.y) / 2f

                // Position referee slightly above the action
                val idealX = midX
                val idealY = Math.min(ring.innerBounds.top - 15f, midY + 45f)

                val dx = idealX - referee.x
                val dy = idealY - referee.y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist > 35f) {
                    referee.state = RefereeMoving(idealX, idealY, isRunning = false)
                }
            }

            is RefereeMoving -> {
                val speed = if (refState.isRunning) referee.runSpeed else referee.walkSpeed
                val dx = refState.targetX - referee.x
                val dy = refState.targetY - referee.y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                if (dist <= speed) {
                    referee.x = refState.targetX
                    referee.y = refState.targetY
                    referee.state = RefereeIdle
                } else {
                    referee.x += (dx / dist) * speed
                    referee.y += (dy / dist) * speed
                    referee.facingDirection = if (dx > 0) Direction.Right else Direction.Left
                }
            }

            is RefereeSignaling -> {
                if (refState.framesLeft <= 1) referee.state = RefereeIdle
                else referee.state = RefereeSignaling("signaling", refState.framesLeft - 1)
            }

            else -> {}
        }
    }
}
