package prowrestling.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

/**
 * Handles smooth fade-to-black and fade-in transitions between screens.
 */
class ScreenTransition(val duration: Float = 0.35f) {
    private var timer: Float = duration
    private var isFadingIn: Boolean = true
    private var isFadingOut: Boolean = false
    private var onComplete: () -> Unit = {}

    fun startFadeIn() {
        isFadingIn = true
        isFadingOut = false
        timer = duration
    }

    fun startFadeOut(callback: () -> Unit) {
        isFadingOut = true
        isFadingIn = false
        timer = 0f
        onComplete = callback
    }

    val isTransitioning: Boolean get() = isFadingIn || isFadingOut

    fun update(delta: Float) {
        if (isFadingIn) {
            timer -= delta
            if (timer <= 0f) {
                timer = 0f
                isFadingIn = false
            }
        } else if (isFadingOut) {
            timer += delta
            if (timer >= duration) {
                timer = duration
                isFadingOut = false
                onComplete()
            }
        }
    }

    fun render(shape: ShapeRenderer, width: Float, height: Float) {
        val alpha = if (isFadingIn || isFadingOut) {
            Math.max(0f, Math.min(1.0f, timer / duration))
        } else {
            0f
        }

        if (alpha > 0.005f) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
            shape.begin(ShapeRenderer.ShapeType.Filled)
            shape.color = Color(0f, 0f, 0f, alpha)
            shape.rect(0f, 0f, width, height)
            shape.end()
        }
    }
}
