package prowrestling.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import prowrestling.ProWrestlingGame
import prowrestling.audio.MusicPlayer
import prowrestling.audio.SfxPlayer
import prowrestling.core.TournamentState
import prowrestling.core.WrestlerData
import prowrestling.render.SpriteSheet

class CharacterSelectScreen(
    val game: ProWrestlingGame,
    val is2Player: Boolean = false
) : ScreenAdapter() {

    private val virtualWidth = 480f
    private val virtualHeight = 360f

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(virtualWidth, virtualHeight, camera)
    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()

    // 6 selectable wrestlers in NES order
    private val characters: List<WrestlerData> = listOf(
        WrestlerData.Hayabusa,
        WrestlerData.StarMan,
        WrestlerData.KinCornKarn,
        WrestlerData.GiantPanther,
        WrestlerData.TheAmazon,
        WrestlerData.KingSlender
    )

    private var p1SelectionIndex: Int = 0
    private var p2SelectionIndex: Int = 1
    private var selectingPlayer2: Boolean = false
    private var animTimer: Float = 0f

    override fun show() {
        MusicPlayer.play("select_theme", loop = true)
    }

    override fun render(delta: Float) {
        animTimer += delta

        handleInput()

        // Clear background to retro ring apron deep navy
        Gdx.gl.glClearColor(0.05f, 0.07f, 0.16f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        val combined = camera.combined

        // 1. Shapes background cards & stat bars
        shapeRenderer.projectionMatrix = combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        renderCardBackgrounds()
        renderStatBars()

        shapeRenderer.end()

        // 2. Sprite & Text Pass
        game.batch.projectionMatrix = combined
        game.batch.begin()

        renderHeader()
        renderWrestlerPreviews()
        renderWrestlerDetails()

        game.batch.end()
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.screen = TitleScreen(game)
            return
        }

        val activeIndex = if (selectingPlayer2) p2SelectionIndex else p1SelectionIndex

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            val next = if (activeIndex > 0) activeIndex - 1 else characters.size - 1
            if (selectingPlayer2) p2SelectionIndex = next else p1SelectionIndex = next
            SfxPlayer.play("kick_hit", 0.5f)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            val next = (activeIndex + 1) % characters.size
            if (selectingPlayer2) p2SelectionIndex = next else p1SelectionIndex = next
            SfxPlayer.play("kick_hit", 0.5f)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.Z)) {

            SfxPlayer.play("bell_ring")

            if (!is2Player) {
                // 1-Player Tournament Campaign
                val p1 = characters[p1SelectionIndex].id
                val tournament = TournamentState(p1)
                game.screen = TournamentScreen(game, tournament)
            } else if (!selectingPlayer2) {
                // Player 1 locked in, proceed to Player 2
                selectingPlayer2 = true
            } else {
                // Both players selected in 2P mode
                val p1 = characters[p1SelectionIndex].id
                val p2 = characters[p2SelectionIndex].id
                game.screen = MatchScreen(game, p1Id = p1, p2Id = p2, isP2Cpu = false)
            }
        }
    }

    private fun renderHeader() {
        game.font.color = Color(1.0f, 0.85f, 0.2f, 1.0f)
        game.font.data.setScale(1.8f)

        val title = if (!is2Player) {
            "SELECT YOUR WRESTLER"
        } else if (!selectingPlayer2) {
            "PLAYER 1: CHOOSE WRESTLER"
        } else {
            "PLAYER 2: CHOOSE WRESTLER"
        }

        layout.setText(game.font, title)
        game.font.draw(game.batch, title, (virtualWidth - layout.width) / 2f, virtualHeight - 24f)

        game.font.color = Color(0.7f, 0.8f, 1.0f, 1.0f)
        game.font.data.setScale(1.0f)
        val subtitle = "< LEFT / RIGHT > TO BROWSE   [ENTER / SPACE] TO SELECT"
        layout.setText(game.font, subtitle)
        game.font.draw(game.batch, subtitle, (virtualWidth - layout.width) / 2f, virtualHeight - 56f)
    }

    private fun renderCardBackgrounds() {
        // Top roster selector thumbnails
        val slotW = 64f
        val slotH = 40f
        val gap = 10f
        val startX = (virtualWidth - (6 * slotW + 5 * gap)) / 2f
        val startY = virtualHeight - 110f

        val activeIdx = if (selectingPlayer2) p2SelectionIndex else p1SelectionIndex

        for (i in characters.indices) {
            val x = startX + i * (slotW + gap)
            if (i == activeIdx) {
                // Pulsing highlight
                val pulse = (Math.sin(animTimer * 10.0).toFloat() * 0.2f) + 0.8f
                shapeRenderer.color = Color(1.0f * pulse, 0.85f * pulse, 0.2f, 1.0f)
                shapeRenderer.rect(x - 3f, startY - 3f, slotW + 6f, slotH + 6f)
            } else if (i == p1SelectionIndex && selectingPlayer2) {
                // P1 locked slot
                shapeRenderer.color = Color(0.2f, 0.6f, 1.0f, 1.0f)
                shapeRenderer.rect(x - 2f, startY - 2f, slotW + 4f, slotH + 4f)
            }

            shapeRenderer.color = Color(0.12f, 0.16f, 0.28f, 1.0f)
            shapeRenderer.rect(x, startY, slotW, slotH)
        }

        // Main detail preview panel
        shapeRenderer.color = Color(0.09f, 0.12f, 0.22f, 1.0f)
        shapeRenderer.rect(30f, 30f, virtualWidth - 60f, 200f)

        shapeRenderer.color = Color(0.22f, 0.28f, 0.44f, 1.0f)
        shapeRenderer.rect(34f, 34f, virtualWidth - 68f, 192f)

        shapeRenderer.color = Color(0.07f, 0.09f, 0.18f, 1.0f)
        shapeRenderer.rect(38f, 38f, virtualWidth - 76f, 184f)
    }

    private fun renderStatBars() {
        val activeIdx = if (selectingPlayer2) p2SelectionIndex else p1SelectionIndex
        val char = characters[activeIdx]

        val statX = 250f
        val startY = 120f
        val barW = 140f
        val barH = 10f
        val gapY = 22f

        val stats = listOf(
            Pair("SPEED", char.speed / 2.0f),
            Pair("STRIKE", if (char.id == "starman" || char.id == "king_slender") 0.9f else 0.7f),
            Pair("GRAPPLE", if (char.id == "giant_panther" || char.id == "kincornkarn") 0.95f else 0.65f),
            Pair("POWER", char.weight.toFloat() / 4f)
        )

        for ((i, entry) in stats.withIndex()) {
            val value = entry.second
            val y = startY - i * gapY
            // Background bar
            shapeRenderer.color = Color(0.15f, 0.18f, 0.28f, 1.0f)
            shapeRenderer.rect(statX, y, barW, barH)

            // Filled bar
            shapeRenderer.color = Color(0.2f, 0.85f, 0.4f, 1.0f)
            shapeRenderer.rect(statX, y, barW * Math.min(1.0f, value), barH)
        }
    }

    private fun renderWrestlerPreviews() {
        val activeIdx = if (selectingPlayer2) p2SelectionIndex else p1SelectionIndex
        val char = characters[activeIdx]

        // Main large character preview sprite
        val sheet: SpriteSheet? = SpriteSheet.load(char.id)
        if (sheet != null) {
            val region = sheet.getFrame(0) // Idle frame
            val drawW = 76f
            val drawH = 120f
            val bob = Math.sin(animTimer * 4.0).toFloat() * 3.0f
            game.batch.color = Color.WHITE
            game.batch.draw(region, 80f, 60f + bob, drawW, drawH)
        }

        // Small thumbnails in top slot selector
        val slotW = 64f
        val gap = 10f
        val startX = (virtualWidth - (6 * slotW + 5 * gap)) / 2f
        val startY = virtualHeight - 110f

        for (i in characters.indices) {
            val c = characters[i]
            val x = startX + i * (slotW + gap)
            val thumbSheet = SpriteSheet.load(c.id)
            if (thumbSheet != null) {
                val region = thumbSheet.getFrame(0)
                game.batch.draw(region, x + 16f, startY + 2f, 32f, 36f)
            }
        }
    }

    private fun renderWrestlerDetails() {
        val activeIdx = if (selectingPlayer2) p2SelectionIndex else p1SelectionIndex
        val char = characters[activeIdx]

        val infoX = 200f

        // Character Name
        game.font.color = Color(1.0f, 0.95f, 0.3f, 1.0f)
        game.font.data.setScale(1.5f)
        game.font.draw(game.batch, char.name.uppercase(), infoX, 204f)

        // Country & Profile
        game.font.color = Color(0.8f, 0.85f, 0.95f, 1.0f)
        game.font.data.setScale(1.0f)
        game.font.draw(game.batch, "ORIGIN: ${char.country.uppercase()}", infoX, 178f)
        game.font.draw(game.batch, "STYLE: ${char.signatureMoveName.uppercase()}", infoX, 158f)

        // Stat labels
        val statLabels = listOf("SPEED", "STRIKE", "GRAPPLE", "POWER")
        val startY = 130f
        val gapY = 22f

        game.font.color = Color(0.7f, 0.75f, 0.85f, 1.0f)
        game.font.data.setScale(0.9f)
        for ((i, label) in statLabels.withIndex()) {
            val y = startY - i * gapY
            game.font.draw(game.batch, label, infoX, y)
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
