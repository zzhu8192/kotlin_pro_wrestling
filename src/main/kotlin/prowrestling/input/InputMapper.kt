package prowrestling.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import prowrestling.core.Direction

class InputMapper(val isPlayer1: Boolean = true) {
    val doubleTapDetector = DoubleTapDetector()

    // Key configurations
    val keyUp: Int = if (isPlayer1) Input.Keys.UP else Input.Keys.I
    val keyDown: Int = if (isPlayer1) Input.Keys.DOWN else Input.Keys.K
    val keyLeft: Int = if (isPlayer1) Input.Keys.LEFT else Input.Keys.J
    val keyRight: Int = if (isPlayer1) Input.Keys.RIGHT else Input.Keys.L
    val keyB: Int = if (isPlayer1) Input.Keys.Z else Input.Keys.N
    val keyA: Int = if (isPlayer1) Input.Keys.X else Input.Keys.M
    val keySelect: Int = if (isPlayer1) Input.Keys.A else Input.Keys.O
    val keyStart: Int = if (isPlayer1) Input.Keys.ENTER else Input.Keys.P

    fun isAnyJustPressed(): Boolean {
        if (Gdx.input == null) return false
        val basePressed = Gdx.input.isKeyJustPressed(keyA) ||
            Gdx.input.isKeyJustPressed(keyB) ||
            Gdx.input.isKeyJustPressed(keyUp) ||
            Gdx.input.isKeyJustPressed(keyDown) ||
            Gdx.input.isKeyJustPressed(keyLeft) ||
            Gdx.input.isKeyJustPressed(keyRight) ||
            Gdx.input.isKeyJustPressed(keyStart) ||
            Gdx.input.isKeyJustPressed(keySelect)

        return if (isPlayer1) {
            basePressed ||
                Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
                Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
                Gdx.input.isKeyJustPressed(Input.Keys.C) ||
                Gdx.input.isKeyJustPressed(Input.Keys.S) ||
                Gdx.input.isKeyJustPressed(Input.Keys.D)
        } else {
            basePressed ||
                Gdx.input.isKeyJustPressed(Input.Keys.U) ||
                Gdx.input.isKeyJustPressed(Input.Keys.H)
        }
    }

    fun poll(delta: Float): InputAction {
        doubleTapDetector.update(delta)

        // Check button attacks
        if (Gdx.input.isKeyJustPressed(keyA)) return AttackA
        if (Gdx.input.isKeyJustPressed(keyB)) return AttackB
        if (Gdx.input.isKeyJustPressed(keyStart)) return StartAction
        if (Gdx.input.isKeyJustPressed(keySelect)) return SelectAction

        // Detect double-tap triggers (horizontal running only: Left and Right)
        val horizontalDirs = listOf(
            Pair(keyLeft, Direction.Left),
            Pair(keyRight, Direction.Right)
        )

        // Pressing Up or Down cancels any pending horizontal double-tap
        if (Gdx.input.isKeyJustPressed(keyUp) || Gdx.input.isKeyJustPressed(keyDown)) {
            doubleTapDetector.reset()
        }

        for ((key, dir) in horizontalDirs) {
            if (Gdx.input.isKeyJustPressed(key)) {
                val isDouble = doubleTapDetector.onDirectionJustPressed(dir)
                if (isDouble) {
                    return RunAction(dir)
                }
            }
            if (!Gdx.input.isKeyPressed(key)) {
                doubleTapDetector.onDirectionReleased(dir)
            }
        }

        // If currently running from double-tap and holding the run key
        for ((key, dir) in horizontalDirs) {
            if (Gdx.input.isKeyPressed(key) && doubleTapDetector.currentlyRunning) {
                return RunAction(dir)
            }
        }

        // Standard walk holding
        val left = Gdx.input.isKeyPressed(keyLeft)
        val right = Gdx.input.isKeyPressed(keyRight)
        val up = Gdx.input.isKeyPressed(keyUp)
        val down = Gdx.input.isKeyPressed(keyDown)

        if (up && left) return MoveAction(Direction.UpLeft)
        if (up && right) return MoveAction(Direction.UpRight)
        if (down && left) return MoveAction(Direction.DownLeft)
        if (down && right) return MoveAction(Direction.DownRight)

        if (left) return MoveAction(Direction.Left)
        if (right) return MoveAction(Direction.Right)
        if (up) return MoveAction(Direction.Up)
        if (down) return MoveAction(Direction.Down)

        return IdleAction
    }
}
