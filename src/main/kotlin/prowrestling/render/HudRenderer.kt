package prowrestling.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import prowrestling.core.*

class HudRenderer(val virtualWidth: Float = 480f, val virtualHeight: Float = 360f) {
    private val layout = GlyphLayout()

    private var p1LerpHp: Float = 240f
    private var p2LerpHp: Float = 240f
    private var animTimer: Float = 0f

    fun update(delta: Float, w1Hp: Int, w2Hp: Int) {
        animTimer += delta
        // Smooth lerp toward target HP (approx 10 units per second or 8x delta)
        p1LerpHp += (w1Hp.toFloat() - p1LerpHp) * Math.min(1.0f, delta * 8.0f)
        p2LerpHp += (w2Hp.toFloat() - p2LerpHp) * Math.min(1.0f, delta * 8.0f)
    }

    fun render(
        batch: SpriteBatch,
        shape: ShapeRenderer,
        font: BitmapFont,
        camera: OrthographicCamera,
        w1: Wrestler,
        w2: Wrestler,
        rules: MatchRules,
        matchTime: Float,
        isP2Cpu: Boolean
    ) {
        // 1. Render Health Bars & Geometric Backdrops with ShapeRenderer
        shape.projectionMatrix = camera.combined
        shape.begin(ShapeRenderer.ShapeType.Filled)

        // Top HUD background bar
        shape.color = Color(0.06f, 0.08f, 0.12f, 0.85f)
        shape.rect(0f, virtualHeight - 38f, virtualWidth, 38f)
        shape.color = Color(0.2f, 0.25f, 0.35f, 1.0f)
        shape.rect(0f, virtualHeight - 39f, virtualWidth, 1.5f)

        // Health Bar 1 (Left - P1)
        val isP1Crit = w1.hp <= 48
        val p1Color = if (isP1Crit && ((animTimer * 8).toInt() % 2 == 0)) Color.YELLOW else Color(0.9f, 0.2f, 0.2f, 1.0f)
        drawBar(shape, 14f, virtualHeight - 28f, 150f, 8f, w1.hp, p1LerpHp, w1.maxHp, p1Color)

        // Health Bar 2 (Right - P2)
        val isP2Crit = w2.hp <= 48
        val p2Color = if (isP2Crit && ((animTimer * 8).toInt() % 2 == 0)) Color.YELLOW else Color(0.3f, 0.6f, 0.95f, 1.0f)
        drawBar(shape, virtualWidth - 164f, virtualHeight - 28f, 150f, 8f, w2.hp, p2LerpHp, w2.maxHp, p2Color)

        // Match Won Backdrop Banner
        if (rules.matchResult != InProgress) {
            shape.color = Color(0f, 0f, 0f, 0.75f)
            shape.rect(0f, virtualHeight / 2f - 45f, virtualWidth, 90f)
            shape.color = Color(1.0f, 0.85f, 0.2f, 1.0f)
            shape.rect(0f, virtualHeight / 2f + 45f, virtualWidth, 2f)
            shape.rect(0f, virtualHeight / 2f - 45f, virtualWidth, 2f)
        }

        shape.end()

        // 2. Render Text Typography with SpriteBatch
        batch.projectionMatrix = camera.combined
        batch.begin()

        // P1 Name & HP
        font.data.setScale(0.85f)
        font.color = Color.WHITE
        font.draw(batch, "1P: ${w1.characterId.uppercase()}", 14f, virtualHeight - 6f)
        font.color = if (isP1Crit) Color.RED else Color.YELLOW
        font.draw(batch, "${w1.hp}", 138f, virtualHeight - 6f)

        // P2 Name & HP
        val p2Prefix = if (isP2Cpu) "CPU" else "2P"
        font.color = Color.WHITE
        font.draw(batch, "$p2Prefix: ${w2.characterId.uppercase()}", virtualWidth - 164f, virtualHeight - 6f)
        font.color = if (isP2Crit) Color.RED else Color.YELLOW
        font.draw(batch, "${w2.hp}", virtualWidth - 40f, virtualHeight - 6f)

        // Match Timer in Center
        val minutes = (matchTime / 60f).toInt()
        val seconds = (matchTime % 60f).toInt()
        val timeStr = String.format("%02d:%02d", minutes, seconds)
        font.data.setScale(1.0f)
        font.color = Color.CYAN
        drawCenteredText(batch, font, timeStr, virtualHeight - 12f)

        // Pinfall Counter
        val currentPin = rules.activePin
        if (currentPin != null) {
            font.data.setScale(1.4f)
            font.color = Color.GOLD
            val countStr = if (currentPin.currentCount > 0) "PIN! COUNT: ${currentPin.currentCount}" else "PINFALL ATTEMPT!"
            drawCenteredText(batch, font, countStr, virtualHeight - 55f)
        } else {
            // Ring-out warning if active
            if (rules.ringOutCount > 0) {
                font.data.setScale(1.1f)
                font.color = if ((rules.ringOutTimerFrames / 8) % 2 == 0) Color.RED else Color.YELLOW
                drawCenteredText(batch, font, "RING-OUT COUNT: ${rules.ringOutCount} / 20", virtualHeight - 55f)
            }
        }

        // Match Finish Banner
        when (val result = rules.matchResult) {
            is MatchWonByPin -> {
                val isP1Winner = result.winner.isPlayer1
                font.data.setScale(1.6f)
                font.color = if (isP1Winner) Color.YELLOW else Color(1.0f, 0.35f, 0.35f, 1.0f)
                val headline = if (isP1Winner) "A WINNER IS YOU!" else "YOU LOSE!"
                drawCenteredText(batch, font, headline, virtualHeight / 2f + 25f)
                font.data.setScale(1.0f)
                font.color = Color.WHITE
                drawCenteredText(batch, font, "${result.winner.characterId.uppercase()} WINS BY PINFALL 3-COUNT!", virtualHeight / 2f - 2f)
            }

            is MatchWonByCountOut -> {
                val isP1Winner = result.winner.isPlayer1
                font.data.setScale(1.6f)
                font.color = if (isP1Winner) Color.YELLOW else Color(1.0f, 0.35f, 0.35f, 1.0f)
                val headline = if (isP1Winner) "A WINNER IS YOU!" else "YOU LOSE!"
                drawCenteredText(batch, font, headline, virtualHeight / 2f + 25f)
                font.data.setScale(1.0f)
                font.color = Color.WHITE
                drawCenteredText(batch, font, "${result.winner.characterId.uppercase()} WINS BY 20-COUNT RING-OUT!", virtualHeight / 2f - 2f)
            }

            else -> {}
        }

        batch.end()
    }

    private fun drawBar(
        shape: ShapeRenderer,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        actualHp: Int,
        lerpHp: Float,
        maxHp: Int,
        fillColor: Color
    ) {
        val actualW = Math.max(0f, Math.min(w, (actualHp.toFloat() / maxHp.toFloat()) * w))
        val trailingW = Math.max(actualW, Math.min(w, (lerpHp / maxHp.toFloat()) * w))

        // Background bar
        shape.color = Color(0.12f, 0.12f, 0.15f, 1.0f)
        shape.rect(x, y, w, h)

        // Trailing damage chunk (white-yellow)
        shape.color = Color(1.0f, 0.9f, 0.7f, 0.8f)
        shape.rect(x, y, trailingW, h)

        // Current solid health
        shape.color = fillColor
        shape.rect(x, y, actualW, h)

        // Perimeter border
        shape.color = Color(0.5f, 0.55f, 0.65f, 1.0f)
        shape.rectLine(x, y + h, x + w, y + h, 1.0f)
        shape.rectLine(x, y, x + w, y, 1.0f)
    }

    private fun drawCenteredText(batch: SpriteBatch, font: BitmapFont, text: String, y: Float) {
        layout.setText(font, text)
        val x = (virtualWidth - layout.width) / 2f
        font.draw(batch, text, x, y)
    }
}
