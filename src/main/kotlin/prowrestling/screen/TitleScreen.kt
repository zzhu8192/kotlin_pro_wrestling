package prowrestling.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.utils.viewport.FitViewport
import prowrestling.ProWrestlingGame
import prowrestling.audio.MusicPlayer
import prowrestling.audio.SfxPlayer

class TitleScreen(val game: ProWrestlingGame) : ScreenAdapter() {
    private val virtualWidth = 480f
    private val virtualHeight = 360f

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(virtualWidth, virtualHeight, camera)
    private val layout = GlyphLayout()

    private var blinkTimer = 0f
    private var selectedMode = 0 // 0 = 1 Player, 1 = 2 Players
    private var showPressStart = true

    override fun show() {
        MusicPlayer.play("title_theme", loop = true)
    }

    override fun render(delta: Float) {
        // 60 FPS delta handling & blink effect
        blinkTimer += delta
        if (blinkTimer >= 0.5f) {
            showPressStart = !showPressStart
            blinkTimer = 0f
        }

        // Handle key input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen) {
                Gdx.graphics.setWindowedMode(960, 720)
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
            }
        }

        // Toggle menu option with Up/Down or 'A' (NES Select)
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) ||
            Gdx.input.isKeyJustPressed(Input.Keys.DOWN) ||
            Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            selectedMode = 1 - selectedMode
            SfxPlayer.play("kick_hit", 0.6f)
        }

        // Confirm with Enter, Space, or 'S' (NES Start) -> Switch to CharacterSelectScreen
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            SfxPlayer.play("bell_ring")
            game.screen = CharacterSelectScreen(game, is2Player = selectedMode == 1)
            return
        }

        // Clear background to retro dark navy
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.22f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        game.batch.projectionMatrix = camera.combined

        game.batch.begin()

        // Title Banner
        game.font.data.setScale(2.5f)
        game.font.color = Color.GOLD
        drawCenteredText("PRO WRESTLING", virtualHeight - 80f)

        // Subtitle
        game.font.data.setScale(1.2f)
        game.font.color = Color.CYAN
        drawCenteredText("REIMAGINED (KOTLIN / LIBGDX)", virtualHeight - 125f)

        // Mode choices
        game.font.data.setScale(1.3f)
        game.font.color = Color.WHITE
        val mode1Prefix = if (selectedMode == 0) "> " else "  "
        val mode1Suffix = if (selectedMode == 0) " <" else "  "
        val mode2Prefix = if (selectedMode == 1) "> " else "  "
        val mode2Suffix = if (selectedMode == 1) " <" else "  "
        drawCenteredText("${mode1Prefix}1 PLAYER TOURNAMENT$mode1Suffix", virtualHeight - 190f)
        drawCenteredText("${mode2Prefix}2 PLAYERS VERSUS$mode2Suffix", virtualHeight - 215f)

        // Blinking prompt
        if (showPressStart) {
            game.font.data.setScale(1.1f)
            game.font.color = Color.YELLOW
            drawCenteredText("PRESS ENTER OR SPACE TO START", virtualHeight - 275f)
        }

        // Copyright / footer
        game.font.data.setScale(0.9f)
        game.font.color = Color.LIGHT_GRAY
        drawCenteredText("(C) 1987 NINTENDO / REIMAGINED 2026", 40f)

        game.batch.end()
    }

    private fun drawCenteredText(text: String, y: Float) {
        layout.setText(game.font, text)
        val x = (virtualWidth - layout.width) / 2f
        game.font.draw(game.batch, text, x, y)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        // Game font and batch are owned and disposed by ProWrestlingGame
    }
}
