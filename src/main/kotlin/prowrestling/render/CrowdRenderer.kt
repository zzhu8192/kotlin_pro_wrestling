package prowrestling.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import java.util.Random

data class Flashbulb(
    val x: Float,
    val y: Float,
    var life: Float,
    val maxLife: Float,
    val radius: Float
)

class CrowdRenderer(val virtualWidth: Float = 480f, val virtualHeight: Float = 360f) {
    private val crowdTop = virtualHeight
    private val crowdBottom = 262f
    private val crowdHeight = crowdTop - crowdBottom

    private val rng = Random(42) // Fixed seed for spectator row layout
    private val activeFlashes = mutableListOf<Flashbulb>()
    private var flashTimer = 0f

    // Pre-generate crowd seating rows for consistent retro look
    private data class Spectator(val x: Float, val y: Float, val color: Color, val width: Float, val height: Float)

    private val spectators: List<Spectator> = run {
        val crowdColors = arrayOf(
            Color(0.20f, 0.25f, 0.40f, 1f), // Navy
            Color(0.40f, 0.20f, 0.25f, 1f), // Maroon
            Color(0.25f, 0.40f, 0.30f, 1f), // Muted green
            Color(0.45f, 0.45f, 0.45f, 1f), // Light gray
            Color(0.15f, 0.15f, 0.20f, 1f), // Dark gray
            Color(0.60f, 0.55f, 0.40f, 1f), // Beige
            Color(0.35f, 0.25f, 0.50f, 1f)  // Purple
        )
        val rows = mutableListOf<Spectator>()
        val rowCount = 6
        val rowStep = crowdHeight / rowCount

        for (r in 0 until rowCount) {
            val y = crowdBottom + r * rowStep
            var x = 8f
            while (x < virtualWidth - 8f) {
                val w = 8f + rng.nextInt(6)
                val h = 10f + (r * 1.2f)
                val col = crowdColors[rng.nextInt(crowdColors.size)]
                rows.add(Spectator(x, y, col, w, h))
                x += w + 2f + rng.nextInt(3)
            }
        }
        rows
    }

    fun update(delta: Float) {
        // Ambient crowd flashbulbs
        flashTimer += delta
        if (flashTimer > 0.12f) {
            flashTimer = 0f
            if (rng.nextFloat() < 0.65f) {
                spawnRandomFlash()
            }
        }

        // Update active flashes
        var i = 0
        while (i < activeFlashes.size) {
            val f = activeFlashes[i]
            f.life -= delta
            if (f.life <= 0f) {
                activeFlashes.removeAt(i)
            } else {
                i++
            }
        }
    }

    fun triggerFlashburst(count: Int = 8) {
        for (i in 0 until count) {
            spawnRandomFlash()
        }
    }

    private fun spawnRandomFlash() {
        val fx = 15f + rng.nextFloat() * (virtualWidth - 30f)
        val fy = crowdBottom + 4f + rng.nextFloat() * (crowdHeight - 10f)
        val duration = 0.08f + rng.nextFloat() * 0.14f
        val rad = 4f + rng.nextFloat() * 6f
        activeFlashes.add(Flashbulb(fx, fy, duration, duration, rad))
    }

    fun render(shape: ShapeRenderer) {
        // 1. Arena upper background / darkness
        shape.set(ShapeRenderer.ShapeType.Filled)
        shape.color = Color(0.04f, 0.05f, 0.10f, 1.0f)
        shape.rect(0f, crowdBottom, virtualWidth, crowdHeight)

        // 2. Crowd balcony railing & dividing tier lines
        shape.color = Color(0.08f, 0.10f, 0.16f, 1.0f)
        for (r in 0 until 6) {
            val lineY = crowdBottom + r * (crowdHeight / 6f)
            shape.rect(0f, lineY, virtualWidth, 2f)
        }

        // 3. Render spectators
        for (s in spectators) {
            // Body / torso
            shape.color = s.color
            shape.rect(s.x, s.y, s.width, s.height * 0.65f)
            // Head
            val headColor = Color(s.color.r * 1.2f, s.color.g * 1.2f, s.color.b * 1.2f, 1f)
            shape.color = headColor
            shape.circle(s.x + s.width / 2f, s.y + s.height * 0.75f, s.width * 0.35f, 6)
        }

        // 4. Ringside barrier / barricade separating crowd from arena floor
        shape.color = Color(0.12f, 0.14f, 0.18f, 1.0f)
        shape.rect(0f, crowdBottom - 6f, virtualWidth, 6f)
        shape.color = Color(0.25f, 0.28f, 0.35f, 1.0f)
        shape.rect(0f, crowdBottom - 2f, virtualWidth, 2f) // Highlight rail

        // 5. Active flashbulbs
        for (f in activeFlashes) {
            val progress = f.life / f.maxLife
            val alpha = Math.min(1.0f, progress * 1.3f)

            // Outer glow
            shape.color = Color(1f, 1f, 1f, alpha * 0.7f)
            shape.circle(f.x, f.y, f.radius * (1.2f - progress * 0.2f), 8)

            // Bright white core
            shape.color = Color(1f, 1f, 0.9f, 1.0f)
            shape.circle(f.x, f.y, f.radius * 0.5f, 6)

            // Cross star flare
            val flareLen = f.radius * 2.0f
            shape.color = Color(1f, 1f, 1f, alpha)
            shape.rectLine(f.x - flareLen, f.y, f.x + flareLen, f.y, 1.5f)
            shape.rectLine(f.x, f.y - flareLen, f.x, f.y + flareLen, 1.5f)
        }
    }

    fun dispose() {}
}
