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

class TournamentScreen(
    val game: ProWrestlingGame,
    val tournament: TournamentState
) : ScreenAdapter() {

    private val virtualWidth = 480f
    private val virtualHeight = 360f

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(virtualWidth, virtualHeight, camera)
    private val shapeRenderer = ShapeRenderer()
    private val layout = GlyphLayout()

    private var animTimer: Float = 0f
    private val p1Data = WrestlerData.forId(tournament.playerCharacterId)
    private val oppData = WrestlerData.forId(tournament.currentOpponentId)

    override fun show() {
        MusicPlayer.play("select_theme", loop = true)
    }

    override fun render(delta: Float) {
        animTimer += delta

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.screen = TitleScreen(game)
            return
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) ||
            Gdx.input.isKeyJustPressed(Input.Keys.SPACE) ||
            Gdx.input.isKeyJustPressed(Input.Keys.S) ||
            Gdx.input.isKeyJustPressed(Input.Keys.Z)) {

            SfxPlayer.play("bell_ring")
            game.screen = MatchScreen(
                game,
                p1Id = tournament.playerCharacterId,
                p2Id = tournament.currentOpponentId,
                isP2Cpu = true,
                tournament = tournament
            )
            return
        }

        Gdx.gl.glClearColor(0.04f, 0.06f, 0.14f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        val combined = camera.combined

        // 1. Shapes (Versus card backgrounds & belt banner)
        shapeRenderer.projectionMatrix = combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        renderBackgroundCards()

        shapeRenderer.end()

        // 2. Sprites & Text
        game.batch.projectionMatrix = combined
        game.batch.begin()

        renderHeader()
        renderWrestlers()
        renderVersusText()

        game.batch.end()
    }

    private fun renderBackgroundCards() {
        // Header Banner
        shapeRenderer.color = Color(0.12f, 0.16f, 0.32f, 1.0f)
        shapeRenderer.rect(20f, virtualHeight - 64f, virtualWidth - 40f, 44f)

        // Player Card (Left)
        shapeRenderer.color = Color(0.08f, 0.12f, 0.22f, 1.0f)
        shapeRenderer.rect(30f, 70f, 180f, 200f)
        shapeRenderer.color = Color(0.18f, 0.35f, 0.65f, 1.0f)
        shapeRenderer.rect(34f, 74f, 172f, 192f)
        shapeRenderer.color = Color(0.06f, 0.08f, 0.18f, 1.0f)
        shapeRenderer.rect(38f, 78f, 164f, 184f)

        // Opponent Card (Right)
        val oppX = virtualWidth - 210f
        shapeRenderer.color = Color(0.08f, 0.12f, 0.22f, 1.0f)
        shapeRenderer.rect(oppX, 70f, 180f, 200f)
        shapeRenderer.color = Color(0.75f, 0.25f, 0.22f, 1.0f)
        shapeRenderer.rect(oppX + 4f, 74f, 172f, 192f)
        shapeRenderer.color = Color(0.06f, 0.08f, 0.18f, 1.0f)
        shapeRenderer.rect(oppX + 8f, 78f, 164f, 184f)

        // Belt banner if champion
        if (tournament.hasVwaTitle) {
            shapeRenderer.color = Color(1.0f, 0.82f, 0.15f, 1.0f)
            shapeRenderer.rect(40f, 240f, 160f, 16f)
        }
    }

    private fun renderHeader() {
        // Tournament Stage Title
        game.font.color = Color(1.0f, 0.85f, 0.2f, 1.0f)
        game.font.data.setScale(1.4f)
        val title = tournament.matchTitle
        layout.setText(game.font, title)
        game.font.draw(game.batch, title, (virtualWidth - layout.width) / 2f, virtualHeight - 34f)

        // Prompt
        val blink = Math.sin(animTimer * 8.0).toFloat() > 0
        if (blink) {
            game.font.color = Color(0.3f, 1.0f, 0.5f, 1.0f)
            game.font.data.setScale(1.2f)
            val prompt = "PRESS START [ENTER] TO WRESTLE"
            layout.setText(game.font, prompt)
            game.font.draw(game.batch, prompt, (virtualWidth - layout.width) / 2f, 44f)
        }
    }

    private fun renderWrestlers() {
        // Player 1 Sprite (Left)
        SpriteSheet.load(p1Data.id)?.let { sheet ->
            val region = sheet.getFrame(0)
            game.batch.color = Color.WHITE
            game.batch.draw(region, 85f, 130f, 70f, 110f)
        }

        // Opponent Sprite (Right - facing left)
        SpriteSheet.load(oppData.id)?.let { sheet ->
            val region = sheet.getFrame(0)
            game.batch.color = Color.WHITE
            game.batch.draw(region, 325f, 130f, 35f, 55f, 70f, 110f, -1f, 1f, 0f)
        }

        val cardW = 180f

        // P1 Info
        val p1CardX = 30f
        game.font.data.setScale(1.0f)
        val p1Name = p1Data.name.uppercase()
        layout.setText(game.font, p1Name)
        game.font.color = Color(0.9f, 0.95f, 1.0f, 1.0f)
        game.font.draw(game.batch, p1Name, p1CardX + (cardW - layout.width) / 2f, 120f)

        val p1Record = "VWA RECORD: ${tournament.wins}W - ${tournament.losses}L"
        layout.setText(game.font, p1Record)
        game.font.color = Color(0.7f, 0.75f, 0.85f, 1.0f)
        game.font.draw(game.batch, p1Record, p1CardX + (cardW - layout.width) / 2f, 100f)

        // Opponent Info
        val oppCardX = virtualWidth - 210f
        game.font.data.setScale(1.0f)
        val oppName = oppData.name.uppercase()
        layout.setText(game.font, oppName)
        game.font.color = Color(1.0f, 0.8f, 0.8f, 1.0f)
        game.font.draw(game.batch, oppName, oppCardX + (cardW - layout.width) / 2f, 120f)

        val oppTech = "TECH: ${oppData.signatureMoveName.uppercase()}"
        layout.setText(game.font, oppTech)
        game.font.color = Color(0.85f, 0.7f, 0.7f, 1.0f)
        game.font.draw(game.batch, oppTech, oppCardX + (cardW - layout.width) / 2f, 100f)
    }

    private fun renderVersusText() {
        game.font.color = Color(1.0f, 0.2f, 0.2f, 1.0f)
        game.font.data.setScale(2.5f)
        layout.setText(game.font, "VS")
        game.font.draw(game.batch, "VS", (virtualWidth - layout.width) / 2f, 185f)
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        shapeRenderer.dispose()
    }
}
