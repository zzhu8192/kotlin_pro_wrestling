package prowrestling.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import prowrestling.core.Referee
import prowrestling.core.Ring
import prowrestling.core.Wrestler

class MatchRenderer(val ring: Ring) {
    val shapeRenderer = ShapeRenderer()
    val crowdRenderer = CrowdRenderer(ring.virtualWidth, ring.virtualHeight)
    val ringRenderer = RingRenderer(ring)
    val wrestlerRenderer = WrestlerRenderer()
    val refereeRenderer = RefereeRenderer()

    fun update(delta: Float) {
        crowdRenderer.update(delta)
        wrestlerRenderer.update(delta)
        refereeRenderer.update(delta)
    }

    fun update(delta: Float, wrestlers: Iterable<Wrestler>) {
        crowdRenderer.update(delta)
        wrestlerRenderer.update(delta, wrestlers)
        refereeRenderer.update(delta)
    }

    /**
     * Complete multi-pass rendering pipeline:
     * 1. Shapes (Crowd background, Ring mat/apron/back ropes, Drop shadows)
     * 2. Sprites (Characters & Referee depth-sorted Y-down)
     * 3. Shapes (Front ropes & bottom posts for correct depth occlusion)
     */
    fun render(
        camera: OrthographicCamera,
        batch: SpriteBatch,
        wrestlers: List<Wrestler>,
        referee: Referee?
    ) {
        // Clear back buffer
        Gdx.gl.glClearColor(0.04f, 0.05f, 0.10f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Pass 1: Background, Ring back, and character drop shadows
        if (Gdx.gl != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        crowdRenderer.render(shapeRenderer)
        ringRenderer.renderBack(shapeRenderer)

        for (w in wrestlers) {
            wrestlerRenderer.renderShadow(shapeRenderer, w)
        }
        if (referee != null) {
            refereeRenderer.renderShadow(shapeRenderer, referee)
        }

        shapeRenderer.end()

        // Pass 2: Sprites for characters & referee (depth-sorted Y-down)
        batch.projectionMatrix = camera.combined
        batch.begin()

        val renderables = mutableListOf<Pair<Float, () -> Unit>>()
        for (w in wrestlers) {
            renderables.add(Pair(w.y) { wrestlerRenderer.render(batch, shapeRenderer, w) })
        }
        if (referee != null) {
            renderables.add(Pair(referee.y) { refereeRenderer.render(batch, shapeRenderer, referee) })
        }

        renderables.sortByDescending { it.first }
        for ((_, draw) in renderables) {
            draw()
        }

        batch.end()

        // Pass 3: Front Ring Elements (front ropes & bottom posts occlude characters standing behind them)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        ringRenderer.renderFront(shapeRenderer)
        shapeRenderer.end()
    }

    /**
     * Fallback shape-only render method for headless or non-textured execution.
     */
    fun render(
        camera: OrthographicCamera,
        wrestlers: List<Wrestler>,
        referee: Referee?
    ) {
        Gdx.gl.glClearColor(0.04f, 0.05f, 0.10f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        if (Gdx.gl != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        crowdRenderer.render(shapeRenderer)
        ringRenderer.renderBack(shapeRenderer)

        val renderables = mutableListOf<Pair<Float, () -> Unit>>()
        for (w in wrestlers) {
            renderables.add(Pair(w.y) { wrestlerRenderer.render(shapeRenderer, w) })
        }
        if (referee != null) {
            renderables.add(Pair(referee.y) { refereeRenderer.render(shapeRenderer, referee) })
        }

        renderables.sortByDescending { it.first }
        for ((_, draw) in renderables) {
            draw()
        }

        ringRenderer.renderFront(shapeRenderer)
        shapeRenderer.end()
    }

    fun dispose() {
        shapeRenderer.dispose()
        crowdRenderer.dispose()
        SpriteSheet.disposeAll()
    }
}
