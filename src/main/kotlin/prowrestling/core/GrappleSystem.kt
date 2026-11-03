package prowrestling.core

import prowrestling.input.*

data class GrappleInput(
    val action: InputAction,
    val direction: Direction?,
    val frame: Int
)

sealed interface GrappleResult
data class GrappleWin(
    val winner: Wrestler,
    val loser: Wrestler,
    val move: MoveInfo,
    val whipDir: Direction? = null
) : GrappleResult
data object GrappleStalemate : GrappleResult

class GrappleSession(
    val w1: Wrestler,
    val w2: Wrestler,
    val initialCountdown: Int = 60,
    var lockupWaitFrames: Int = 0
) {
    var countdown: Int = initialCountdown
}

object GrappleSystem {

    fun canInitiateGrapple(w: Wrestler): Boolean {
        return when (w.state) {
            Idle, is Walking -> true
            else -> false
        }
    }

    fun checkCollision(w1: Wrestler, w2: Wrestler): Boolean {
        return canInitiateGrapple(w1) && canInitiateGrapple(w2) &&
            Math.abs(w1.x - w2.x) < 35f && Math.abs(w1.y - w2.y) <= 15f
    }

    fun startSession(w1: Wrestler, w2: Wrestler, lockupWaitFrames: Int = 0): GrappleSession {
        val midX = (w1.x + w2.x) / 2f
        val midY = (w1.y + w2.y) / 2f

        if (w1.x <= w2.x) {
            w1.x = midX - 12f
            w1.facingDirection = Direction.Right
            w2.x = midX + 12f
            w2.facingDirection = Direction.Left
        } else {
            w1.x = midX + 12f
            w1.facingDirection = Direction.Left
            w2.x = midX - 12f
            w2.facingDirection = Direction.Right
        }

        w1.y = midY
        w1.state = Grappling
        w2.y = midY
        w2.state = Grappling

        return GrappleSession(w1, w2, lockupWaitFrames = lockupWaitFrames)
    }

    fun updateSession(
        session: GrappleSession,
        p1Action: InputAction,
        p1Dir: Direction?,
        p2Action: InputAction,
        p2Dir: Direction?
    ): GrappleResult? {
        if (session.lockupWaitFrames > 0) {
            session.lockupWaitFrames -= 1
            return null
        }

        session.countdown -= 1

        val p1Pressed = p1Action == AttackA || p1Action == AttackB
        val p2Pressed = p2Action == AttackA || p2Action == AttackB

        if (p1Pressed && !p2Pressed) {
            val move = MoveResolver.resolveGrappleMove(session.w1, p1Action, p1Dir)
            val whipDir = if (move.id == "irish_whip") (if (p1Dir == Direction.Left || p1Dir == Direction.Right) p1Dir else null) else null
            return GrappleWin(session.w1, session.w2, move, whipDir)
        } else if (p2Pressed && !p1Pressed) {
            val move = MoveResolver.resolveGrappleMove(session.w2, p2Action, p2Dir)
            val whipDir = if (move.id == "irish_whip") (if (p2Dir == Direction.Left || p2Dir == Direction.Right) p2Dir else null) else null
            return GrappleWin(session.w2, session.w1, move, whipDir)
        } else if (p1Pressed && p2Pressed) {
            // Simultaneous press tiebreak: higher HP ratio wins, or P1 on tie
            val p1Ratio = session.w1.hp.toFloat() / session.w1.maxHp.toFloat()
            val p2Ratio = session.w2.hp.toFloat() / session.w2.maxHp.toFloat()
            return if (p1Ratio >= p2Ratio) {
                val move = MoveResolver.resolveGrappleMove(session.w1, p1Action, p1Dir)
                val whipDir = if (move.id == "irish_whip") (if (p1Dir == Direction.Left || p1Dir == Direction.Right) p1Dir else null) else null
                GrappleWin(session.w1, session.w2, move, whipDir)
            } else {
                val move = MoveResolver.resolveGrappleMove(session.w2, p2Action, p2Dir)
                val whipDir = if (move.id == "irish_whip") (if (p2Dir == Direction.Left || p2Dir == Direction.Right) p2Dir else null) else null
                GrappleWin(session.w2, session.w1, move, whipDir)
            }
        }

        if (session.countdown <= 0) {
            return GrappleStalemate
        }

        return null
    }
}
