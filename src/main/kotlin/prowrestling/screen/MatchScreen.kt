package prowrestling.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.utils.viewport.FitViewport
import prowrestling.ProWrestlingGame
import prowrestling.ai.AiPersonality
import prowrestling.ai.CpuController
import prowrestling.ai.DifficultyTier
import prowrestling.audio.MusicPlayer
import prowrestling.audio.SfxPlayer
import prowrestling.core.*
import prowrestling.input.*
import prowrestling.render.HudRenderer
import prowrestling.render.MatchRenderer
import prowrestling.render.ScreenTransition
import java.util.Random

class MatchScreen(
    val game: ProWrestlingGame,
    val p1Id: String = "hayabusa",
    val p2Id: String = "starman",
    var isP2Cpu: Boolean = true,
    val tournament: TournamentState? = null
) : ScreenAdapter() {

    private val virtualWidth = 480f
    private val virtualHeight = 360f

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(virtualWidth, virtualHeight, camera)

    private val ring = Ring(virtualWidth, virtualHeight)
    private val matchRenderer = MatchRenderer(ring)
    private val hudRenderer = HudRenderer(virtualWidth, virtualHeight)

    // Wrestlers & Game state
    private var p1 = Wrestler(p1Id, isPlayer1 = true, x = 190f, y = 160f)
    private var p2 = Wrestler(p2Id, isPlayer1 = false, x = 290f, y = 160f)
    private var gameState = GameState(p1, p2, Referee(240f, 195f), ring)

    // Input mappers
    private val inputMapperP1 = prowrestling.input.InputMapper(isPlayer1 = true)
    private val inputMapperP2 = prowrestling.input.InputMapper(isPlayer1 = false)

    // AI & Modes
    private var currentTier: DifficultyTier = tournament?.currentDifficultyTier ?: DifficultyTier.Contender
    private var cpuPersonality = AiPersonality.forCharacter(p2.characterId)
    private var cpuController = CpuController(cpuPersonality, currentTier)

    // Screen shake & Polish
    private var shakeTime: Float = 0f
    private var shakeIntensity: Float = 0f
    private var matchOverTimer: Float = 0f
    private var hasTriggeredVictoryTransition: Boolean = false
    private val transition = ScreenTransition(0.35f)
    private val rng = Random()

    override fun show() {
        SfxPlayer.preloadAll()
        prowrestling.render.SpriteSheet.load("referee")
        prowrestling.render.SpriteSheet.load(p1Id)
        prowrestling.render.SpriteSheet.load(p2Id)
        MusicPlayer.play("match_bgm", loop = true)
    }

    override fun render(delta: Float) {
        // 0. Screen shake & camera handling
        applyScreenShake(delta)

        // ESC to return to Title
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            MusicPlayer.stop()
            game.screen = TitleScreen(game)
            return
        }

        // F11 Fullscreen toggle
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            if (Gdx.graphics.isFullscreen) {
                Gdx.graphics.setWindowedMode(960, 720)
            } else {
                Gdx.graphics.setFullscreenMode(Gdx.graphics.displayMode)
            }
        }

        // 'C' toggles CPU opponent vs 2-Player keyboard control
        if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            isP2Cpu = !isP2Cpu
        }

        // 'D' cycles AI Difficulty Tier (if in free match)
        if (Gdx.input.isKeyJustPressed(Input.Keys.D) && tournament == null) {
            val allTiers = DifficultyTier.AllTiers
            val nextIdx = (allTiers.indexOf(currentTier) + 1) % allTiers.size
            currentTier = allTiers[nextIdx]
            cpuController = CpuController(cpuPersonality, currentTier)
        }

        // 'F' triggers flashbulbs in crowd
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            matchRenderer.crowdRenderer.triggerFlashburst(14)
        }

        // F10 or 'M' (in single player against CPU) toggles background music mute/unmute
        if (Gdx.input.isKeyJustPressed(Input.Keys.F10) || (isP2Cpu && Gdx.input.isKeyJustPressed(Input.Keys.M))) {
            MusicPlayer.toggleMute()
        }

        // 1. Poll Inputs
        val p1Action = if (gameState.activeGrapple != null) {
            if (Gdx.input.isKeyJustPressed(inputMapperP1.keyA)) AttackA
            else if (Gdx.input.isKeyJustPressed(inputMapperP1.keyB)) AttackB
            else inputMapperP1.poll(delta)
        } else if (gameState.rules.activePin != null) {
            if (inputMapperP1.isAnyJustPressed()) AttackA
            else inputMapperP1.poll(delta)
        } else {
            inputMapperP1.poll(delta)
        }
        val p1Dir = getHeldDirection(isPlayer1 = true)

        val p2Decision = if (isP2Cpu) {
            cpuController.decide(p2, p1, gameState)
        } else {
            val p2Action = if (gameState.activeGrapple != null) {
                if (Gdx.input.isKeyJustPressed(inputMapperP2.keyA)) AttackA
                else if (Gdx.input.isKeyJustPressed(inputMapperP2.keyB)) AttackB
                else inputMapperP2.poll(delta)
            } else if (gameState.rules.activePin != null) {
                if (inputMapperP2.isAnyJustPressed()) AttackA
                else inputMapperP2.poll(delta)
            } else {
                inputMapperP2.poll(delta)
            }
            Pair(p2Action, getHeldDirection(isPlayer1 = false))
        }
        val p2Action = p2Decision.first
        val p2Dir = p2Decision.second

        // 2. Update Game State (Combat, Grapples, Movement, Referee, Pin System)
        gameState.update(p1Action, p1Dir, p2Action, p2Dir, delta)

        // Drain and trigger SFX events
        val sfxEvents = gameState.pollSfxEvents()
        for (sfx in sfxEvents) {
            SfxPlayer.play(sfx)
            if (sfx == "slam_impact" || sfx == "dive_crash") {
                shakeTime = 0.32f
                shakeIntensity = 5.5f
                matchRenderer.crowdRenderer.triggerFlashburst(12)
            }
        }

        // 3. Update Renderers & HUD
        hudRenderer.update(delta, p1.hp, p2.hp)
        matchRenderer.update(delta, listOf(p1, p2))

        // 4. Render arena, ring, wrestlers, and referee
        camera.update()
        matchRenderer.render(camera, game.batch, listOf(p1, p2), gameState.referee)

        // 5. Render Combat HUD & Pin Status
        hudRenderer.render(
            game.batch,
            matchRenderer.shapeRenderer,
            game.font,
            camera,
            p1,
            p2,
            gameState.rules,
            gameState.matchTime,
            isP2Cpu
        )

        // 6. Match Completion Transition
        if (gameState.isMatchOver) {
            matchOverTimer += delta
            if (!hasTriggeredVictoryTransition && (matchOverTimer >= 2.2f || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.Z) || Gdx.input.isKeyJustPressed(Input.Keys.X))) {
                hasTriggeredVictoryTransition = true
                val winnerLoser = when (val result = gameState.rules.matchResult) {
                    is MatchWonByPin -> Pair(result.winner, if (result.winner.isPlayer1) p2 else p1)
                    is MatchWonByCountOut -> Pair(result.winner, if (result.winner.isPlayer1) p2 else p1)
                    else -> Pair(p1, p2)
                }
                val winner = winnerLoser.first
                val loser = winnerLoser.second
                val isCountOut = gameState.rules.matchResult is MatchWonByCountOut
                transition.startFadeOut {
                    game.screen = VictoryScreen(game, winner, loser, gameState.matchTime, isCountOut, tournament)
                }
            }
        }

        // 7. Render Screen Transition Fade Overlay
        transition.update(delta)
        transition.render(matchRenderer.shapeRenderer, virtualWidth, virtualHeight)
    }

    private fun applyScreenShake(delta: Float) {
        if (shakeTime > 0f) {
            shakeTime -= delta
            val offsetX = (rng.nextFloat() - 0.5f) * 2f * shakeIntensity
            val offsetY = (rng.nextFloat() - 0.5f) * 2f * shakeIntensity
            camera.position.set(virtualWidth / 2f + offsetX, virtualHeight / 2f + offsetY, 0f)
        } else {
            camera.position.set(virtualWidth / 2f, virtualHeight / 2f, 0f)
        }
    }

    private fun getHeldDirection(isPlayer1: Boolean): Direction? {
        return if (isPlayer1) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) Direction.Up
            else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) Direction.Down
            else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) Direction.Left
            else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) Direction.Right
            else null
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.I)) Direction.Up
            else if (Gdx.input.isKeyPressed(Input.Keys.K)) Direction.Down
            else if (Gdx.input.isKeyPressed(Input.Keys.J)) Direction.Left
            else if (Gdx.input.isKeyPressed(Input.Keys.L)) Direction.Right
            else null
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        matchRenderer.dispose()
    }
}
