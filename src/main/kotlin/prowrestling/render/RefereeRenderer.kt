package prowrestling.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import prowrestling.core.*

class RefereeRenderer {
    private val skinColor = Color(0.92f, 0.72f, 0.68f, 1.0f)
    private val pantsColor = Color(0.10f, 0.10f, 0.12f, 1.0f)
    private val shoesColor = Color(0.08f, 0.08f, 0.10f, 1.0f)
    private val shadowColor = Color(0f, 0.02f, 0.05f, 0.13f)

    private var animTimer = 0f
    private var cachedSheet: SpriteSheet? = null

    fun update(delta: Float) {
        animTimer += delta
    }

    /**
     * Render referee drop shadow on ring mat.
     * Uses a subtle, realistic horizontal sliver directly under the referee's feet/body.
     */
    fun renderShadow(shape: ShapeRenderer, referee: Referee) {
        val x = referee.x
        val y = referee.y
        shape.color = shadowColor
        when (referee.state) {
            is RefereeDropping, is RefereeCounting -> {
                val w = 34f
                val h = 2.8f
                shape.ellipse(x - w / 2f, y + 1.5f - h / 2f, w, h)
            }
            else -> {
                val w = 24f
                val h = 2.3f
                shape.ellipse(x - w / 2f, y - h / 2f, w, h)
            }
        }
    }

    /**
     * Render referee with sprite sheet if available, otherwise procedural fallback.
     */
    fun render(batch: SpriteBatch, shapeFallback: ShapeRenderer, referee: Referee) {
        val sheet: SpriteSheet? = cachedSheet ?: SpriteSheet.load("referee").also { cachedSheet = it }
        if (sheet != null) {
            val x = referee.x
            val y = referee.y
            val isFacingLeft = referee.facingDirection == Direction.Left
            val drawW = 66f
            val drawH = 96f
            val originX = drawW / 2f
            val originY = 0f
            val scaleX = if (isFacingLeft) -1f else 1f

            // Map referee states to frame indices:
            // Frame 0: idle standing
            // Frame 4, 5: walking
            // Frame 10: slap / counting
            // Frame 49: prone on mat
            // Frame 16: signaling / raising hand
            val frameIdx = when (val state = referee.state) {
                is RefereeDropping -> 49
                is RefereeCounting -> if (state.framesInCount < 16) 10 else 49
                is RefereeRaisingHand, is RefereeSignaling -> 16
                is RefereeMoving -> if (((animTimer * 8).toInt() % 2) == 0) 4 else 5
                else -> 0
            }

            val region = sheet.getFrame(frameIdx)
            batch.color = Color.WHITE
            // Atlas cell is 96x192 with shoe soles ending at row 162 (30px bottom padding).
            // Shifting drawY by 15f (96 * 30 / 192) aligns referee shoe soles directly at y.
            val refereeFootOffset = 15f
            val drawX = x - originX
            val drawY = y - refereeFootOffset
            batch.draw(region, drawX, drawY, originX, originY, drawW, drawH, scaleX, 1f, 0f)
        } else {
            val wasDrawing = batch.isDrawing
            if (wasDrawing) batch.end()
            shapeFallback.begin(ShapeRenderer.ShapeType.Filled)
            render(shapeFallback, referee)
            shapeFallback.end()
            if (wasDrawing) batch.begin()
        }
    }

    /**
     * Procedural shape fallback rendering.
     */
    fun render(shape: ShapeRenderer, referee: Referee) {
        val x = referee.x
        val y = referee.y
        val sign = if (referee.facingDirection == Direction.Left) -1f else 1f

        when (val state = referee.state) {
            is RefereeDropping -> {
                renderProneSlap(shape, referee, x, y, isSlapping = false, count = 0)
            }

            is RefereeCounting -> {
                val isSlapping = state.framesInCount < 16
                renderProneSlap(shape, referee, x, y, isSlapping, state.count)
            }

            is RefereeRaisingHand -> {
                renderRaisingHandPose(shape, x, y, sign)
            }

            is RefereeSignaling -> {
                renderSignalingPose(shape, x, y, sign)
            }

            else -> {
                renderStandingReferee(shape, referee, x, y, sign)
            }
        }
    }

    private fun renderStandingReferee(shape: ShapeRenderer, referee: Referee, x: Float, y: Float, sign: Float) {
        val isMoving = referee.state is RefereeMoving
        val bob = if (isMoving) Math.sin(animTimer * 14.0).toFloat() * 2.0f else 0f
        val baseY = y + Math.max(0f, bob)

        // Black trousers
        shape.color = pantsColor
        shape.rect(x - 7f, baseY, 6f, 18f)
        shape.rect(x + 1f, baseY, 6f, 18f)
        shape.color = shoesColor
        shape.rect(x - 8f, baseY, 7f, 6f)
        shape.rect(x + 1f, baseY, 7f, 6f)

        // Striped Shirt Torso
        val torsoY = baseY + 18f
        val torsoW = 20f
        val torsoH = 18f

        renderStripedShirt(shape, x - torsoW / 2f, torsoY, torsoW, torsoH)

        // Arms
        shape.color = skinColor
        shape.rect(x - (torsoW / 2f + 5f) * sign, torsoY + 2f, 5f, 14f)
        shape.rect(x + (torsoW / 2f - 1f) * sign, torsoY + 2f, 5f, 14f)

        // Head
        val headY = torsoY + torsoH
        shape.color = skinColor
        shape.circle(x + (1.5f * sign), headY + 7f, 7.5f, 14)

        // Hair
        shape.color = Color(0.2f, 0.15f, 0.1f, 1.0f)
        shape.rect(x - 6f, headY + 8f, 12f, 6f)
    }

    private fun renderProneSlap(shape: ShapeRenderer, referee: Referee, x: Float, y: Float, isSlapping: Boolean, count: Int) {
        // Legs stretched back
        shape.color = pantsColor
        shape.rect(x + 4f, y, 18f, 8f)
        shape.color = shoesColor
        shape.rect(x + 20f, y, 6f, 8f)

        // Striped Torso on mat
        renderStripedShirt(shape, x - 14f, y + 2f, 20f, 12f)

        // Head near canvas
        shape.color = skinColor
        shape.circle(x - 18f, y + 8f, 7f, 12)

        if (isSlapping) {
            // Slapping arm striking down on mat
            shape.color = skinColor
            shape.rectLine(x - 6f, y + 10f, x - 6f, y - 1f, 4.5f)
            shape.circle(x - 6f, y, 4f, 8)

            // Mat slap shockwave ring
            val waveRadius = 14f
            shape.color = Color(1.0f, 1.0f, 1.0f, 0.6f)
            shape.circle(x - 6f, y, waveRadius, 12)
        } else {
            // Arm raised high anticipating slap
            shape.color = skinColor
            shape.rectLine(x - 8f, y + 8f, x - 14f, y + 24f, 4.5f)
            shape.circle(x - 14f, y + 24f, 4f, 8)
        }
    }

    private fun renderRaisingHandPose(shape: ShapeRenderer, x: Float, y: Float, sign: Float) {
        renderStandingReferee(shape, Referee(x, y), x, y, sign)

        // Right arm raised high signaling victory
        shape.color = skinColor
        shape.rectLine(x + (10f * sign), y + 36f, x + (22f * sign), y + 54f, 5f)
        shape.circle(x + (22f * sign), y + 54f, 4f, 8)
    }

    private fun renderSignalingPose(shape: ShapeRenderer, x: Float, y: Float, sign: Float) {
        renderStandingReferee(shape, Referee(x, y), x, y, sign)

        // Arms waving outward
        shape.color = skinColor
        shape.rectLine(x - 10f, y + 36f, x - 22f, y + 42f, 4.5f)
        shape.rectLine(x + 10f, y + 36f, x + 22f, y + 42f, 4.5f)
    }

    private fun renderStripedShirt(shape: ShapeRenderer, startX: Float, startY: Float, width: Float, height: Float) {
        // White base shirt
        shape.color = Color(1.0f, 1.0f, 1.0f, 1.0f)
        shape.rect(startX, startY, width, height)

        // Vertical black referee stripes
        shape.color = Color(0.1f, 0.1f, 0.12f, 1.0f)
        val stripeWidth = 3f
        val gap = 3f
        var sx = startX + 1.5f
        while (sx < startX + width) {
            val w = Math.min(stripeWidth, (startX + width) - sx)
            shape.rect(sx, startY, w, height)
            sx += stripeWidth + gap
        }
    }
}
