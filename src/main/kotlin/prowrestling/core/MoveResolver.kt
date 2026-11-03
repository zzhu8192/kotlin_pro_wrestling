package prowrestling.core

import prowrestling.input.*

object MoveResolver {

    /**
     * Resolves a standing or running strike/ground attack when a button is pressed.
     */
    fun resolveStrike(
        attacker: Wrestler,
        defender: Wrestler,
        action: InputAction,
        heldDir: Direction? = null
    ): MoveInfo? {
        return when (action) {
            AttackB -> {
                if (attacker.isRunning) {
                    if (attacker.characterId == "starman") DamageTable.FlyingCrossChop
                    else DamageTable.RunningKneeLift
                } else if (defender.isGrounded && isWithinDistance(attacker, defender, DamageTable.GroundStomp.range)) {
                    DamageTable.GroundStomp
                } else if (attacker.canAct) {
                    if (attacker.characterId == "kincornkarn") DamageTable.MongolianChop
                    else DamageTable.Punch
                } else null
            }

            AttackA -> {
                val wantsClothesline = attacker.isRunning ||
                    defender.state is IrishWhipped || defender.state is RopeRebound

                if (wantsClothesline) {
                    DamageTable.RunningClothesline
                } else if (attacker.canAct) {
                    if (attacker.characterId == "kincornkarn") DamageTable.KarateKick
                    else if (attacker.characterId == "hayabusa") DamageTable.Enzuigiri
                    else DamageTable.Kick
                } else null
            }

            else -> null
        }
    }

    /**
     * Resolves which move executes upon winning a grapple lockup based on winner input and directional stick.
     */
    fun resolveGrappleMove(
        winner: Wrestler,
        input: InputAction,
        heldDir: Direction?
    ): MoveInfo {
        return when (input) {
            AttackB -> {
                when (heldDir) {
                    Direction.Left, Direction.Right, Direction.UpLeft, Direction.DownLeft, Direction.UpRight, Direction.DownRight -> DamageTable.IrishWhip
                    else -> DamageTable.BodySlam
                }
            }

            AttackA -> {
                // Character signatures
                when (winner.characterId) {
                    "starman" -> if (heldDir == Direction.Down || heldDir == Direction.DownLeft || heldDir == Direction.DownRight) return DamageTable.SomersaultKick
                    "giant_panther" -> {
                        if (heldDir == Direction.Left || heldDir == Direction.Right) return DamageTable.IronClaw
                        if (heldDir == Direction.Down || heldDir == Direction.DownLeft || heldDir == Direction.DownRight) return DamageTable.Headbutt
                    }
                    "amazon" -> {
                        if (heldDir == Direction.Left || heldDir == Direction.Right) return DamageTable.PiranhaBite
                        if (heldDir == Direction.Down || heldDir == Direction.DownLeft || heldDir == Direction.DownRight) return DamageTable.OutlawChoke
                    }
                    "king_slender" -> return DamageTable.Backbreaker
                    else -> {}
                }

                when (heldDir) {
                    Direction.Left, Direction.Right -> DamageTable.BackSuplex
                    Direction.Up, Direction.UpLeft, Direction.UpRight -> DamageTable.VerticalSuplex
                    Direction.Down, Direction.DownLeft, Direction.DownRight -> DamageTable.Piledriver
                    else -> DamageTable.VerticalSuplex
                }
            }

            else -> DamageTable.BodySlam // Default fallback
        }
    }

    /**
     * Checks if a strike connects in pseudo-3D/2.5D ring space:
     * - Requires tight Y-alignment (depth on canvas) so wrestlers must be on the same horizontal plane.
     * - Requires forward reach in the attacker's facing direction.
     */
    fun canStrikeHit(attacker: Wrestler, defender: Wrestler, move: MoveInfo): Boolean {
        val maxDy = when (move.id) {
            "ground_stomp" -> 16f
            "clothesline" -> 22f
            "dropkick", "flying_cross_chop", "knee_lift" -> 16f
            else -> 10f // Standing strikes require close horizontal lane alignment
        }
        val dy = Math.abs(attacker.y - defender.y)
        if (dy > maxDy) return false

        val forwardX = if (attacker.facingDirection == Direction.Right) {
            defender.x - attacker.x
        } else {
            attacker.x - defender.x
        }

        // Allow close-body contact up to move reach
        val minX = if (move.id == "clothesline") -10f else -4f
        return forwardX in minX..move.range
    }

    fun isWithinDistance(w1: Wrestler, w2: Wrestler, maxDist: Float): Boolean {
        val dx = w1.x - w2.x
        val dy = w1.y - w2.y
        return Math.sqrt((dx * dx + dy * dy).toDouble()) <= maxDist
    }

    fun isFacingOpponent(attacker: Wrestler, defender: Wrestler): Boolean {
        return when (attacker.facingDirection) {
            Direction.Right -> defender.x >= attacker.x - 4f
            Direction.Left -> defender.x <= attacker.x + 4f
            else -> true
        }
    }
}
