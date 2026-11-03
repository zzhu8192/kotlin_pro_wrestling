package prowrestling

import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import prowrestling.screen.TitleScreen

class ProWrestlingGame : Game() {
    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()
        setScreen(TitleScreen(this))
    }

    override fun dispose() {
        screen?.dispose()
        if (::batch.isInitialized) batch.dispose()
        if (::font.isInitialized) font.dispose()
        prowrestling.audio.MusicPlayer.dispose()
        prowrestling.audio.SfxPlayer.dispose()
        prowrestling.render.SpriteSheet.disposeAll()
        super.dispose()
    }
}
