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
import prowrestling.core.Wrestler
import prowrestling.core.WrestlerData
import prowrestling.render.SpriteSheet

class VictoryScreen(
    val game: ProWrestlingGame,
    val winner: Wrestler,
    val loser: Wrestler,
    val matchTimeSeconds: Float,
    val isCountOut: Boolean = false,
    val tournament: TournamentState? = null
) : ScreenAdapter() {

    private val virtualWidth = 480f
    private val virtualHeight = 360f

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(virtualWidth, virtualHeight, camera)
    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()

    private var animTimer: Float = 0f
    private val isPlayer1Winner = winner.isPlayer1
    private val winnerData = WrestlerData.forId(winner.characterId)

    override fun show() {
        MusicPlayer.play("victory", loop = true)
        SfxPlayer.play("crowd_roar", 1.0f)
    }

    override fun render(delta: Float) {
        animTimer += delta

        handleInput()

        // Pure black background matching original NES game
        Gdx.gl.glClearColor(0f, 0f, 0f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        val combined = camera.combined

        // Sprites & Text
        game.batch.projectionMatrix = combined
        game.batch.begin()

        renderWinnerPose()
        renderVictoryMessage()
        renderMatchStats()

        game.batch.end()
    }

    private fun handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.screen = TitleScreen(game)
            return
        }

        // 0.3s cooldown so the winning input doesn't instantly dismiss the screen
        if (animTimer < 0.3f) return

        val advance = Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.Z) ||
            Gdx.input.isKeyJustPressed(Input.Keys.X) ||
            Gdx.input.isKeyJustPressed(Input.Keys.C) ||
            Gdx.input.isKeyJustPressed(Input.Keys.A) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S) ||
            Gdx.input.isKeyJustPressed(Input.Keys.D) ||
            Gdx.input.isKeyJustPressed(Input.Keys.J) ||
            Gdx.input.isKeyJustPressed(Input.Keys.K) ||
            Gdx.input.isKeyJustPressed(Input.Keys.N) ||
            Gdx.input.isKeyJustPressed(Input.Keys.M) ||
            Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)

        if (advance) {
            SfxPlayer.play("bell_ring")

            if (tournament != null) {
                if (isPlayer1Winner) {
                    tournament.recordWin()
                    if (tournament.isCompleted) {
                        game.screen = TitleScreen(game)
                    } else {
                        game.screen = TournamentScreen(game, tournament)
                    }
                } else {
                    tournament.recordLoss()
                    // Retry same match
                    game.screen = TournamentScreen(game, tournament)
                }
            } else {
                // 2-Player rematch
                game.screen = MatchScreen(
                    game,
                    p1Id = if (isPlayer1Winner) winner.characterId else loser.characterId,
                    p2Id = if (isPlayer1Winner) loser.characterId else winner.characterId,
                    isP2Cpu = false
                )
            }
        }
    }

    private fun renderWinnerPose() {
        // Winner victory pose centered at the top of the black screen
        SpriteSheet.load(winner.characterId)?.let { sheet ->
            val region = sheet.getFrame(16) // Victory raised-arm pose
            game.batch.color = Color.WHITE
            val charW = 75f
            val charH = 120f
            val charX = (virtualWidth - charW) / 2f
            val charY = 210f
            game.batch.draw(region, charX, charY, charW, charH)
        }
    }

    private fun renderVictoryMessage() {
        val pulse = (Math.sin(animTimer * 6.0).toFloat() * 0.15f) + 0.85f
        game.font.data.setScale(1.8f)

        // Headline: "A WINNER IS YOU !" only if Player 1 won; "YOU LOSE !" if Player 1 lost
        val headlineText = if (isPlayer1Winner) {
            game.font.color = Color(1.0f * pulse, 0.85f * pulse, 0.2f, 1.0f)
            "A WINNER IS YOU !"
        } else {
            game.font.color = Color(1.0f * pulse, 0.35f * pulse, 0.35f * pulse, 1.0f)
            "YOU LOSE !"
        }
        layout.setText(game.font, headlineText)
        game.font.draw(game.batch, headlineText, (virtualWidth - layout.width) / 2f, 192f)

        // Winner announcement line
        game.font.color = Color(0.95f, 0.95f, 0.95f, 1.0f)
        game.font.data.setScale(1.2f)
        val winnerLine = "${winnerData.name.uppercase()} WINS BY ${if (isCountOut) "COUNT OUT" else "PINFALL"}!"
        layout.setText(game.font, winnerLine)
        game.font.draw(game.batch, winnerLine, (virtualWidth - layout.width) / 2f, 156f)
    }

    private fun renderMatchStats() {
        val totalSec = matchTimeSeconds.toInt()
        val mins = totalSec / 60
        val secs = totalSec % 60
        val timeStr = String.format("%02d:%02d", mins, secs)

        game.font.color = Color(0.75f, 0.85f, 0.95f, 1.0f)
        game.font.data.setScale(1.0f)

        val timeLine = "MATCH TIME: $timeStr"
        layout.setText(game.font, timeLine)
        game.font.draw(game.batch, timeLine, (virtualWidth - layout.width) / 2f, 122f)

        val hpLine = "WINNER REMAINING HP: ${winner.hp}/240"
        layout.setText(game.font, hpLine)
        game.font.draw(game.batch, hpLine, (virtualWidth - layout.width) / 2f, 100f)

        if (tournament != null) {
            val titleStr = if (tournament.hasVwfTitle) "VWF WORLD UNIFIED CHAMPION"
            else if (tournament.hasVwaTitle) "VWA WORLD CHAMPION"
            else "VWA TITLE CONTENDER"
            game.font.color = Color(1.0f, 0.85f, 0.2f, 1.0f)
            val statusLine = "STATUS: $titleStr (WINS: ${tournament.wins})"
            layout.setText(game.font, statusLine)
            game.font.draw(game.batch, statusLine, (virtualWidth - layout.width) / 2f, 78f)
        }

        // Blinking Prompt to continue or retry
        val blink = Math.sin(animTimer * 8.0).toFloat() > 0
        if (blink) {
            game.font.color = Color(0.3f, 1.0f, 0.5f, 1.0f)
            game.font.data.setScale(1.1f)
            val prompt = if (tournament != null) {
                if (isPlayer1Winner) "PRESS ENTER FOR NEXT MATCH"
                else "PRESS ENTER TO RETRY MATCH"
            } else {
                "PRESS ENTER FOR REMATCH"
            }
            layout.setText(game.font, prompt)
            game.font.draw(game.batch, prompt, (virtualWidth - layout.width) / 2f, 38f)
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
