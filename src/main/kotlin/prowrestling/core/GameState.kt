package prowrestling.core

import prowrestling.input.*

class GameState(
    val wrestler1: Wrestler = Wrestler("hayabusa", isPlayer1 = true, x = 180f, y = 160f),
    val wrestler2: Wrestler = Wrestler("starman", isPlayer1 = false, x = 300f, y = 160f),
    val referee: Referee = Referee(x = 240f, y = 195f),
    val ring: Ring = Ring()
) {
    val rules: MatchRules = MatchRules(ring)
    var activeGrapple: GrappleSession? = null
    var matchTime: Float = 0f

    val sfxEvents: MutableList<String> = mutableListOf()
    private var matchStartBellPlayed: Boolean = false
    private var matchEndBellPlayed: Boolean = false

    fun queueSfx(sfxId: String) {
        sfxEvents.add(sfxId)
    }

    fun pollSfxEvents(): List<String> {
        val res = sfxEvents.toList()
        sfxEvents.clear()
        return res
    }

    val isMatchOver: Boolean
        get() = rules.matchResult != InProgress

    fun update(
        p1Action: InputAction,
        p1Dir: Direction?,
        p2Action: InputAction,
        p2Dir: Direction?,
        delta: Float
    ) {
        matchTime += delta

        if (!matchStartBellPlayed) {
            matchStartBellPlayed = true
            queueSfx("bell_ring")
        }

        val p1Mashed = p1Action == AttackA || p1Action == AttackB || p1Action is MoveAction || p1Action is RunAction || p1Action == StartAction || p1Action == SelectAction
        val p2Mashed = p2Action == AttackA || p2Action == AttackB || p2Action is MoveAction || p2Action is RunAction || p2Action == StartAction || p2Action == SelectAction

        val wasPinActive = rules.activePin != null
        // Update Match Rules (Pinfall counting, kick-outs, ring-outs, referee actions)
        rules.update(wrestler1, wrestler2, referee, p1Mashed, p2Mashed)

        if (wasPinActive && rules.activePin == null && !isMatchOver) {
            queueSfx("slam_impact")
        }

        // Referee pin count slap sound
        val pin = rules.activePin
        if (pin != null && pin.framesInCount == 1) {
            queueSfx("referee_slap")
        }

        // If match is finished, freeze wrestlers & celebrate
        if (isMatchOver) {
            if (!matchEndBellPlayed) {
                matchEndBellPlayed = true
                queueSfx("bell_ring")
                queueSfx("crowd_roar")
            }
            return
        }

        // If pin is active, wrestlers are locked in pin
        if (rules.activePin != null) {
            return
        }

        // 1. Progress active grapple session if present
        val session = activeGrapple
        if (session != null) {
            when (val result = GrappleSystem.updateSession(session, p1Action, p1Dir, p2Action, p2Dir)) {
                is GrappleWin -> {
                    if (result.move.id == "irish_whip") {
                        // Direction resolution:
                        // 1. If player held Left or Right, throw in that direction!
                        // 2. Otherwise (neutral/diagonal), throw TOWARDS the center of the ring!
                        val ringCenterX = ring.matBounds.centerX
                        val resolvedWhipDir = result.whipDir ?: if (result.winner.x >= ringCenterX) Direction.Left else Direction.Right
                        result.winner.facingDirection = resolvedWhipDir
                        result.loser.facingDirection = resolvedWhipDir
                    }
                    result.winner.state = Executing(result.move.id, 0, result.move.animationFrames)
                    result.loser.state = BeingThrown(result.move.id, 0, result.move.animationFrames)
                    activeGrapple = null
                }

                GrappleStalemate -> {
                    session.w1.x -= 14f
                    session.w2.x += 14f
                    session.w1.state = Idle
                    session.w2.state = Idle
                    activeGrapple = null
                }

                null -> {
                    // Grapple ongoing
                }
            }
            return // When grappling, no standard physics
        }

        // 2. Advance ongoing execution / throw animations
        updateExecution(wrestler1, wrestler2)
        updateExecution(wrestler2, wrestler1)

        // 2b. Resolve Flying Cross Chop dive tackles (overrides other moves with top priority)
        resolveCrossChop(wrestler1, wrestler2)
        resolveCrossChop(wrestler2, wrestler1)

        // 3. Resolve airborne turnbuckle diving landings
        resolveDiving(wrestler1, wrestler2)
        resolveDiving(wrestler2, wrestler1)

        // 4. Process Pin Attempts on downed opponent
        if (checkPinAttempt(wrestler1, wrestler2, p1Action)) return
        if (checkPinAttempt(wrestler2, wrestler1, p2Action)) return

        // 4b. Process Ground Pickup into Grapple (B button on downed opponent)
        if (checkGroundPickup(wrestler1, wrestler2, p1Action)) return
        if (checkGroundPickup(wrestler2, wrestler1, p2Action)) return

        // 5. Process strikes if attackers can act
        processStrikes(wrestler1, wrestler2, p1Action, p1Dir)
        processStrikes(wrestler2, wrestler1, p2Action, p2Dir)

        // 6. Check for grapple lockup collision
        if (GrappleSystem.checkCollision(wrestler1, wrestler2)) {
            activeGrapple = GrappleSystem.startSession(wrestler1, wrestler2)
            return
        }

        // 7. Standard movement & physics
        val w1PrevState = wrestler1.state
        val w2PrevState = wrestler2.state

        PhysicsStep.step(wrestler1, p1Action, ring, delta)
        PhysicsStep.step(wrestler2, p2Action, ring, delta)

        checkStateSfx(w1PrevState, wrestler1.state)
        checkStateSfx(w2PrevState, wrestler2.state)
    }

    private fun checkStateSfx(prev: WrestlerState, curr: WrestlerState) {
        if (curr is RopeRebound && curr.framesLeft == 14) {
            queueSfx("rope_bounce")
        } else if (curr is FlyingCrossChop && prev !is FlyingCrossChop) {
            queueSfx("dive_crash")
        } else if (curr is ClimbingTurnbuckle && curr.framesLeft == 16) {
            queueSfx("turnbuckle_climb")
        }
    }

    private fun resolveCrossChop(attacker: Wrestler, defender: Wrestler) {
        val st = attacker.state
        if (st is FlyingCrossChop) {
            val dy = Math.abs(attacker.y - defender.y)
            val dx = Math.abs(attacker.x - defender.x)
            if (dy <= 20f && dx <= 50f && !defender.isGrounded) {
                val move = DamageTable.FlyingCrossChop
                defender.hp = Math.max(0, defender.hp - move.damage)
                defender.consecutivePunchHits = 0
                val knockbackDir = if (attacker.facingDirection == Direction.Right) 20f else -20f
                defender.x += knockbackDir
                val downedFrames = defender.calculateDownedFrames(move.stunFrames)
                defender.state = Flinching(framesLeft = 18, totalFrames = 18, willKnockDown = true, knockDownFrames = downedFrames)
                attacker.state = Idle
                queueSfx("slam_impact")
                queueSfx("crowd_roar")
                queueSfx("body_drop")
            }
        }
    }

    private fun resolveDiving(diver: Wrestler, target: Wrestler) {
        val st = diver.state
        if (st is Diving && st.progress >= 1.0f) {
            val dx = Math.abs(diver.x - target.x)
            val dy = Math.abs(diver.y - target.y)
            val hits = dx <= 32f && dy <= 20f
            if (hits) {
                target.hp = Math.max(0, target.hp - DamageTable.DivingSplash.damage)
                target.state = Downed(target.calculateDownedFrames(DamageTable.DivingSplash.stunFrames))
                diver.state = Executing("diving_splash", 0, DamageTable.DivingSplash.animationFrames)
                queueSfx("slam_impact")
                queueSfx("crowd_roar")
            } else {
                diver.hp = Math.max(0, diver.hp - DamageTable.DiveCrash.damage)
                diver.state = Downed(diver.calculateDownedFrames(DamageTable.DiveCrash.stunFrames))
                queueSfx("dive_crash")
            }
        }
    }

    private fun checkPinAttempt(attacker: Wrestler, defender: Wrestler, action: InputAction): Boolean {
        return if (action == AttackA && defender.isGrounded && attacker.canAct) {
            val dx = Math.abs(attacker.x - defender.x)
            val dy = Math.abs(attacker.y - defender.y)
            if (dx <= 36f && dy <= 18f) {
                rules.initiatePin(attacker, defender)
                true
            } else false
        } else false
    }

    private fun checkGroundPickup(attacker: Wrestler, defender: Wrestler, action: InputAction): Boolean {
        return if (action == AttackB && defender.isGrounded && attacker.canAct) {
            val dx = Math.abs(attacker.x - defender.x)
            val dy = Math.abs(attacker.y - defender.y)
            if (dx <= 50f && dy <= 24f) {
                if (attacker.x <= defender.x) {
                    attacker.facingDirection = Direction.Right
                    defender.facingDirection = Direction.Left
                } else {
                    attacker.facingDirection = Direction.Left
                    defender.facingDirection = Direction.Right
                }
                val pickupFrames = 52
                attacker.state = Executing("ground_pickup", 0, pickupFrames)
                defender.state = BeingThrown("ground_pickup", 0, pickupFrames)
                queueSfx("slam_impact")
                true
            } else false
        } else false
    }

    private fun processStrikes(
        attacker: Wrestler,
        defender: Wrestler,
        action: InputAction,
        heldDir: Direction? = null
    ) {
        if (!attacker.canAct && !attacker.isRunning) return
        if (action != AttackA && action != AttackB) return

        val move = MoveResolver.resolveStrike(attacker, defender, action, heldDir)
        if (move != null) {
            if (move.id == "clothesline") {
                // Determine running clothesline charge direction: prioritize held horizontal direction
                if (heldDir == Direction.Left || heldDir == Direction.Right) {
                    attacker.facingDirection = heldDir
                } else if (!attacker.isRunning) {
                    attacker.facingDirection = if (defender.x >= attacker.x) Direction.Right else Direction.Left
                }
                attacker.state = Executing("clothesline", 0, move.animationFrames)
                queueSfx("punch_hit")
                return
            }

            if (attacker.characterId == "starman" && attacker.isRunning && action == AttackB) {
                attacker.state = FlyingCrossChop(attacker.facingDirection, framesLeft = 18)
                queueSfx("dive_crash")
                return
            }

            if (MoveResolver.canStrikeHit(attacker, defender, move)) {
                attacker.state = Executing(move.id, 0, move.animationFrames)
                defender.hp = Math.max(0, defender.hp - move.damage)

                val knockbackDir = if (attacker.facingDirection == Direction.Right) 14f else -14f
                defender.x += knockbackDir

                if (move.knocksDown) {
                    defender.consecutivePunchHits = 0
                    val downedFrames = defender.calculateDownedFrames(move.stunFrames)
                    defender.state = Flinching(framesLeft = 18, totalFrames = 18, willKnockDown = true, knockDownFrames = downedFrames)
                    queueSfx("slam_impact")
                    queueSfx("body_drop")
                } else {
                    // Consecutive punches build up: 3 punches knock down!
                    defender.consecutivePunchHits += 1
                    if (defender.consecutivePunchHits >= 3) {
                        defender.consecutivePunchHits = 0
                        val downedFrames = defender.calculateDownedFrames(move.stunFrames + 50)
                        defender.state = Flinching(framesLeft = 18, totalFrames = 18, willKnockDown = true, knockDownFrames = downedFrames)
                        queueSfx("body_drop")
                    } else {
                        defender.state = Flinching(framesLeft = 14, totalFrames = 14, willKnockDown = false)
                    }
                }

                if (action == AttackB) queueSfx("punch_hit") else queueSfx("kick_hit")

            } else if (attacker.canAct) {
                attacker.state = Executing(move.id, 0, move.animationFrames)
            }
        }
    }

    private fun updateExecution(w: Wrestler, opponent: Wrestler) {
        val st = w.state
        if (st is Executing) {
            val moveId = st.moveId
            val curr = st.currentFrame
            val total = st.totalFrames

            if (moveId == "clothesline") {
                // Attacker charges forward across the canvas with running clothesline momentum
                val speed = 3.8f
                val sign = if (w.facingDirection == Direction.Right) 1f else -1f
                w.x += sign * speed
                val b = ring.innerBounds
                val halfW = w.width / 2f
                w.x = w.x.coerceIn(b.left + halfW, b.right - halfW)

                // Active hitbox check: downs any opponent in path who is not already falling or down
                if (opponent.state !is Flinching && opponent.state !is Downed && opponent.state !is BeingPinned && opponent.state != Pinning) {
                    val dy = Math.abs(w.y - opponent.y)
                    val forwardX = if (w.facingDirection == Direction.Right) opponent.x - w.x else w.x - opponent.x
                    if (dy <= 22f && forwardX in -12f..56f) {
                        val move = DamageTable.RunningClothesline
                        opponent.hp = Math.max(0, opponent.hp - move.damage)
                        opponent.consecutivePunchHits = 0
                        val downedFrames = opponent.calculateDownedFrames(move.stunFrames)
                        opponent.state = Flinching(framesLeft = 16, totalFrames = 16, willKnockDown = true, knockDownFrames = downedFrames)

                        val knockback = if (w.facingDirection == Direction.Right) 35f else -35f
                        val oppHalfW = opponent.width / 2f
                        opponent.x = (opponent.x + knockback).coerceIn(b.left + oppHalfW, b.right - oppHalfW)

                        queueSfx("slam_impact")
                        queueSfx("body_drop")
                        queueSfx("crowd_roar")
                    }
                }
            } else if (moveId == "dropkick") {
                val speed = 3.2f
                val sign = if (w.facingDirection == Direction.Right) 1f else -1f
                w.x += sign * speed
                val b = ring.innerBounds
                val halfW = w.width / 2f
                w.x = w.x.coerceIn(b.left + halfW, b.right - halfW)
            }

            if (opponent.state is BeingThrown) {
                // Synchronize opponent's position in attacker's hands during throw
                val sign = if (w.facingDirection == Direction.Right) 1f else -1f
                val progress = curr.toFloat() / Math.max(1, total)

                when (moveId) {
                    "body_slam" -> {
                        if (progress < 0.25f) {
                            opponent.x = w.x + 20f * sign
                            opponent.y = w.y
                        } else if (progress < 0.55f) {
                            opponent.x = w.x + 3f * sign
                            opponent.y = w.y + 35f
                        } else if (progress < 0.80f) {
                            opponent.x = w.x + 22f * sign
                            opponent.y = w.y + 15f
                        } else {
                            opponent.x = w.x + 30f * sign
                            opponent.y = w.y
                        }
                    }

                    "brainbuster", "vertical_suplex" -> {
                        if (progress < 0.25f) {
                            opponent.x = w.x + 15f * sign
                            opponent.y = w.y
                        } else if (progress < 0.55f) {
                            // Vertical stall straight upside down in air!
                            opponent.x = w.x + 3f * sign
                            opponent.y = w.y + 35f
                        } else if (progress < 0.80f) {
                            opponent.x = w.x - 20f * sign
                            opponent.y = w.y + 20f
                        } else {
                            opponent.x = w.x - 30f * sign
                            opponent.y = w.y
                        }
                    }

                    "piledriver" -> {
                        if (progress < 0.25f) {
                            opponent.x = w.x + 12f * sign
                            opponent.y = w.y
                        } else if (progress < 0.60f) {
                            // Hoisted straight inverted between thighs
                            opponent.x = w.x
                            opponent.y = w.y + 24f
                        } else {
                            // Spiked sitout mat impact
                            opponent.x = w.x
                            opponent.y = w.y
                        }
                    }

                    "back_suplex" -> {
                        if (progress < 0.25f) {
                            opponent.x = w.x + 12f * sign
                            opponent.y = w.y
                        } else if (progress < 0.55f) {
                            opponent.x = w.x - 6f * sign
                            opponent.y = w.y + 30f
                        } else if (progress < 0.80f) {
                            opponent.x = w.x - 20f * sign
                            opponent.y = w.y + 18f
                        } else {
                            opponent.x = w.x - 30f * sign
                            opponent.y = w.y
                        }
                    }

                    "irish_whip" -> {
                        val sign = if (w.facingDirection == Direction.Right) 1f else -1f
                        if (progress < 0.40f) {
                            opponent.x = w.x + 18f * sign
                            opponent.y = w.y
                        } else {
                            opponent.x = w.x + (18f + (progress - 0.40f) * 55f) * sign
                            opponent.y = w.y
                        }
                        // Clamp opponent within ring ropes during throw so they never exit the ring
                        val halfW = opponent.width / 2f
                        opponent.x = opponent.x.coerceIn(ring.innerBounds.left + halfW, ring.innerBounds.right - halfW)
                    }

                    "kick_out" -> {
                        // Multi-frame kickout: defender bridges chest upward; attacker is launched up and backward off the pin
                        if (progress < 0.35f) {
                            val p = progress / 0.35f
                            opponent.x = w.x - (p * 12f) * sign
                            opponent.y = w.y + 9f + (p * 22f)
                        } else if (progress < 0.75f) {
                            val p = (progress - 0.35f) / 0.40f
                            opponent.x = w.x - (12f + p * 28f) * sign
                            opponent.y = w.y + 31f - (p * 22f)
                        } else {
                            val p = (progress - 0.75f) / 0.25f
                            opponent.x = w.x - (40f + p * 9f) * sign
                            opponent.y = w.y + 9f - (p * 9f)
                        }
                    }

                    "ground_pickup" -> {
                        // Defender is smoothly pulled up from the mat to standing directly in front of attacker
                        val targetX = w.x + 22f * sign
                        opponent.x = opponent.x + (targetX - opponent.x) * 0.10f
                        opponent.y = w.y
                    }

                    else -> {
                        if (progress < 0.25f) {
                            opponent.x = w.x + 14f * sign
                            opponent.y = w.y
                        } else if (progress < 0.75f) {
                            opponent.x = w.x + 10f * sign
                            opponent.y = w.y + 8f
                        } else {
                            opponent.x = w.x + 18f * sign
                            opponent.y = w.y
                        }
                    }
                }
            }

            if (curr >= total) {
                if (moveId == "ground_pickup") {
                    // Full pickup animation complete! Now transition into fully grappled mode
                    activeGrapple = GrappleSystem.startSession(w, opponent)
                    return
                }
                if (moveId == "kick_out") {
                    opponent.state = Stunned(framesLeft = 20)
                    w.state = Downed(framesLeft = 20)
                    queueSfx("slam_impact")
                    queueSfx("body_drop")
                    return
                }
                if (opponent.state is BeingThrown) {
                    val move = findMoveInfo(moveId)
                    opponent.hp = Math.max(0, opponent.hp - move.damage)
                    opponent.consecutivePunchHits = 0

                    if (moveId == "irish_whip") {
                        val whipDir = w.facingDirection
                        opponent.facingDirection = whipDir
                        opponent.state = IrishWhipped(whipDir)
                    } else {
                        val downedFrames = opponent.calculateDownedFrames(move.stunFrames)
                        opponent.state = Downed(downedFrames)
                        queueSfx("slam_impact")
                        queueSfx("body_drop")
                    }
                }
                w.state = Idle
            } else {
                w.state = Executing(moveId, curr + 1, total)
            }
        } else if (w.state is BeingThrown) {
            val beingThrown = w.state as BeingThrown
            if (beingThrown.currentFrame < beingThrown.totalFrames) {
                w.state = BeingThrown(beingThrown.moveId, beingThrown.currentFrame + 1, beingThrown.totalFrames)
            }
        }
    }

    private fun findMoveInfo(id: String): MoveInfo = when (id) {
        "punch" -> DamageTable.Punch
        "kick" -> DamageTable.Kick
        "clothesline" -> DamageTable.RunningClothesline
        "dropkick" -> DamageTable.RunningDropkick
        "body_slam" -> DamageTable.BodySlam
        "irish_whip" -> DamageTable.IrishWhip
        "brainbuster" -> DamageTable.VerticalSuplex
        "back_suplex" -> DamageTable.BackSuplex
        "piledriver" -> DamageTable.Piledriver
        "enzuigiri" -> DamageTable.Enzuigiri
        "somersault_kick" -> DamageTable.SomersaultKick
        "mongolian_chop" -> DamageTable.MongolianChop
        "iron_claw" -> DamageTable.IronClaw
        "piranha_bite" -> DamageTable.PiranhaBite
        "backbreaker" -> DamageTable.Backbreaker
        "diving_splash" -> DamageTable.DivingSplash
        "dive_crash" -> DamageTable.DiveCrash
        "ground_stomp" -> DamageTable.GroundStomp
        else -> DamageTable.BodySlam
    }
}
