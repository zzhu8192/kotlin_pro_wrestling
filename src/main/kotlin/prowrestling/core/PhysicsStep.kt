package prowrestling.core

import prowrestling.input.*

object PhysicsStep {

    /**
     * Updates wrestler movement, state transitions, rope rebounds, turnbuckle climb/dive, and boundaries.
     */
    fun step(wrestler: Wrestler, action: InputAction, ring: Ring, delta: Float) {
        // 1. Process current active states that run on timers
        when (val st = wrestler.state) {
            is ClimbingTurnbuckle -> {
                wrestler.x = st.corner.x
                val progress = (16 - st.framesLeft).toFloat() / 16f
                wrestler.y = st.corner.y - 6f + progress * 22f
                if (st.framesLeft <= 1) {
                    wrestler.state = OnTurnbuckle(st.corner)
                    wrestler.x = st.corner.x
                    wrestler.y = st.corner.y + 16f
                } else {
                    wrestler.state = ClimbingTurnbuckle(st.corner, st.framesLeft - 1)
                }
                return
            }

            is OnTurnbuckle -> {
                when (action) {
                    AttackA, AttackB -> {
                        // Launch dive towards ring center
                        val targetX = ring.innerBounds.centerX
                        val targetY = ring.innerBounds.centerY
                        wrestler.state = Diving(st.corner, startX = wrestler.x, startY = wrestler.y, targetX = targetX, targetY = targetY, progress = 0f)
                    }
                    is MoveAction -> {
                        if (action.dir == Direction.Down) {
                            // Step down from turnbuckle into the ring
                            wrestler.y = st.corner.y - 14f
                            wrestler.state = Idle
                        }
                    }
                    else -> {}
                }
                return
            }

            is Diving -> {
                val nextProg = st.progress + 0.045f
                wrestler.x = st.startX + (st.targetX - st.startX) * nextProg
                wrestler.y = st.startY + (st.targetY - st.startY) * nextProg
                if (nextProg >= 1.0f) {
                    wrestler.x = st.targetX
                    wrestler.y = st.targetY
                    wrestler.state = Diving(st.corner, st.startX, st.startY, st.targetX, st.targetY, 1.0f)
                } else {
                    wrestler.state = Diving(st.corner, st.startX, st.startY, st.targetX, st.targetY, nextProg)
                }
                return
            }

            is RopeRebound -> {
                // Move with rebound momentum away from the ropes
                val speed = wrestler.reboundSpeed
                when (st.direction) {
                    Direction.Right -> wrestler.x += speed
                    Direction.Left -> wrestler.x -= speed
                    Direction.Up -> wrestler.y += speed * 0.75f
                    Direction.Down -> wrestler.y -= speed * 0.75f
                    else -> {}
                }
                wrestler.facingDirection = st.direction

                if (st.framesLeft <= 1) {
                    if (st.isIrishWhipped) {
                        // After guaranteed 1st rope bounce, forced run continues across ring until near other rope!
                        wrestler.state = IrishWhipped(st.direction, hasBounced = true)
                    } else if (wrestler.characterId == "starman") {
                        // Starman signature move: dive tackle with crossed arms!
                        // Travel limited to 1/5th ring width (~61px at 3.4f speed for 18 frames)
                        wrestler.state = FlyingCrossChop(st.direction, framesLeft = 18)
                    } else {
                        // Rebound completes into full running sprint with 1 bounce used
                        wrestler.state = Running(st.direction, hasBounced = true)
                    }
                    wrestler.facingDirection = st.direction
                } else {
                    wrestler.state = RopeRebound(st.direction, st.framesLeft - 1, st.isIrishWhipped)
                }
                clampPosition(wrestler, ring)
                return
            }

            is FlyingCrossChop -> {
                val speed = 3.4f
                when (st.direction) {
                    Direction.Right -> wrestler.x += speed
                    Direction.Left -> wrestler.x -= speed
                    Direction.Up -> wrestler.y += speed * 0.8f
                    Direction.Down -> wrestler.y -= speed * 0.8f
                    else -> {}
                }
                wrestler.facingDirection = st.direction

                val b = ring.innerBounds
                val hitEdge = (st.direction == Direction.Left && wrestler.x - wrestler.width / 2f <= b.left) ||
                              (st.direction == Direction.Right && wrestler.x + wrestler.width / 2f >= b.right) ||
                              (st.direction == Direction.Up && wrestler.y >= b.top) ||
                              (st.direction == Direction.Down && wrestler.y <= b.bottom)

                if (st.framesLeft <= 1 || hitEdge) {
                    wrestler.state = Idle
                } else {
                    wrestler.state = FlyingCrossChop(st.direction, st.framesLeft - 1)
                }
                clampPosition(wrestler, ring)
                return
            }

            is Flinching -> {
                if (st.framesLeft > st.totalFrames - 8) {
                    val pushDir = if (wrestler.facingDirection == Direction.Right) -1.6f else 1.6f
                    wrestler.x += pushDir
                }
                if (st.framesLeft <= 1) {
                    if (st.willKnockDown) {
                        wrestler.state = Downed(st.knockDownFrames)
                    } else {
                        wrestler.state = Idle
                    }
                } else {
                    wrestler.state = Flinching(st.framesLeft - 1, st.totalFrames, st.willKnockDown, st.knockDownFrames)
                }
                clampPosition(wrestler, ring)
                return
            }

            is Stunned -> {
                if (st.framesLeft <= 1) wrestler.state = Idle
                else wrestler.state = Stunned(st.framesLeft - 1)
                return
            }

            is Downed -> {
                if (st.framesLeft <= 1) wrestler.state = Idle
                else wrestler.state = Downed(st.framesLeft - 1)
                return
            }

            is IrishWhipped -> {
                moveIrishWhipped(wrestler, st.direction, st.hasBounced, ring)
                clampPosition(wrestler, ring)
                return
            }

            is Executing, is BeingThrown, Grappling, Pinning, is BeingPinned -> {
                // Wrestler is in a locked animation/state; do not allow movement inputs to cancel
                return
            }

            else -> {
                // Continue to standard action processing
            }
        }

        // 2. Process Input Actions
        when (action) {
            is RunAction -> {
                if (action.dir == Direction.Left || action.dir == Direction.Right) {
                    val prevBounced = (wrestler.state as? Running)?.hasBounced ?: false
                    wrestler.state = Running(action.dir, hasBounced = prevBounced)
                    wrestler.facingDirection = action.dir
                    moveRunning(wrestler, action.dir, prevBounced, ring)
                } else {
                    wrestler.state = Walking(action.dir)
                    moveWalking(wrestler, action.dir, ring)
                }
            }

            is MoveAction -> {
                // Check if player is pressing UP near a corner turnbuckle to climb
                if (action.dir == Direction.Up) {
                    val tb = ring.nearestTurnbuckle(wrestler.x, wrestler.y, maxDist = 26f)
                    if (tb != null && (tb.corner == CornerPosition.TopLeft || tb.corner == CornerPosition.TopRight)) {
                        wrestler.state = ClimbingTurnbuckle(tb, framesLeft = 16)
                        return
                    }
                }

                wrestler.state = Walking(action.dir)
                when (action.dir) {
                    Direction.Left, Direction.UpLeft, Direction.DownLeft -> wrestler.facingDirection = Direction.Left
                    Direction.Right, Direction.UpRight, Direction.DownRight -> wrestler.facingDirection = Direction.Right
                    else -> {}
                }
                moveWalking(wrestler, action.dir, ring)
            }

            IdleAction -> {
                when (val st = wrestler.state) {
                    is Running -> moveRunning(wrestler, st.direction, st.hasBounced, ring)
                    is Walking -> wrestler.state = Idle
                    else -> {}
                }
            }

            else -> {}
        }

        clampPosition(wrestler, ring)
    }

    private fun moveWalking(wrestler: Wrestler, dir: Direction, ring: Ring) {
        val speedX = wrestler.walkSpeedX
        val speedY = wrestler.walkSpeedY

        val wasInside = ring.isInsideRopes(wrestler.x, wrestler.y)

        when (dir) {
            Direction.Left -> wrestler.x -= speedX
            Direction.Right -> wrestler.x += speedX
            Direction.Up -> wrestler.y += speedY
            Direction.Down -> wrestler.y -= speedY
            Direction.UpLeft -> { wrestler.x -= speedX; wrestler.y += speedY }
            Direction.UpRight -> { wrestler.x += speedX; wrestler.y += speedY }
            Direction.DownLeft -> { wrestler.x -= speedX; wrestler.y -= speedY }
            Direction.DownRight -> { wrestler.x += speedX; wrestler.y -= speedY }
        }

        if (wasInside) {
            val b = ring.innerBounds
            val halfW = wrestler.width / 2f
            if (wrestler.x - halfW < b.left) wrestler.x = b.left + halfW
            if (wrestler.x + halfW > b.right) wrestler.x = b.right - halfW
            if (wrestler.y < b.bottom) wrestler.y = b.bottom
            if (wrestler.y > b.top) wrestler.y = b.top
        }
    }

    private fun moveIrishWhipped(wrestler: Wrestler, dir: Direction, hasBounced: Boolean, ring: Ring) {
        val speed = wrestler.runSpeed
        if (dir == Direction.Left || dir == Direction.Right) {
            wrestler.facingDirection = dir
        }

        val b = ring.innerBounds
        val halfW = wrestler.width / 2f

        when (dir) {
            Direction.Left -> {
                wrestler.x -= speed
                val distToRope = (wrestler.x - halfW) - b.left
                if (!hasBounced) {
                    if (distToRope <= 0f) {
                        wrestler.x = b.left + halfW
                        wrestler.facingDirection = Direction.Right
                        wrestler.state = RopeRebound(Direction.Right, framesLeft = 14, isIrishWhipped = true)
                    }
                } else {
                    if (distToRope <= 26f) {
                        // Natural stop near opposite ropes without bouncing!
                        wrestler.state = Idle
                    }
                }
            }

            Direction.Right -> {
                wrestler.x += speed
                val distToRope = b.right - (wrestler.x + halfW)
                if (!hasBounced) {
                    if (distToRope <= 0f) {
                        wrestler.x = b.right - halfW
                        wrestler.facingDirection = Direction.Left
                        wrestler.state = RopeRebound(Direction.Left, framesLeft = 14, isIrishWhipped = true)
                    }
                } else {
                    if (distToRope <= 26f) {
                        // Natural stop near opposite ropes without bouncing!
                        wrestler.state = Idle
                    }
                }
            }

            else -> {}
        }
    }

    private fun moveRunning(wrestler: Wrestler, dir: Direction, hasBounced: Boolean, ring: Ring) {
        val speed = wrestler.runSpeed
        if (dir == Direction.Left || dir == Direction.Right) {
            wrestler.facingDirection = dir
        }

        val b = ring.innerBounds
        val halfW = wrestler.width / 2f

        when (dir) {
            Direction.Left -> {
                wrestler.x -= speed
                val distToRope = (wrestler.x - halfW) - b.left
                if (!hasBounced) {
                    if (distToRope <= 0f) {
                        wrestler.x = b.left + halfW
                        wrestler.facingDirection = Direction.Right
                        wrestler.state = RopeRebound(Direction.Right, framesLeft = 14, isIrishWhipped = false)
                    }
                } else {
                    if (distToRope <= 26f) {
                        // Natural stop near opposite ropes without bouncing!
                        wrestler.state = Idle
                    }
                }
            }

            Direction.Right -> {
                wrestler.x += speed
                val distToRope = b.right - (wrestler.x + halfW)
                if (!hasBounced) {
                    if (distToRope <= 0f) {
                        wrestler.x = b.right - halfW
                        wrestler.facingDirection = Direction.Left
                        wrestler.state = RopeRebound(Direction.Left, framesLeft = 14, isIrishWhipped = false)
                    }
                } else {
                    if (distToRope <= 26f) {
                        // Natural stop near opposite ropes without bouncing!
                        wrestler.state = Idle
                    }
                }
            }

            else -> {}
        }
    }

    private fun clampPosition(wrestler: Wrestler, ring: Ring) {
        val b = ring.floorBounds
        val halfW = wrestler.width / 2f

        if (wrestler.x - halfW < b.left) wrestler.x = b.left + halfW
        if (wrestler.x + halfW > b.right) wrestler.x = b.right - halfW
        if (wrestler.y < b.bottom) wrestler.y = b.bottom
        if (wrestler.y > b.top) wrestler.y = b.top
    }
}
