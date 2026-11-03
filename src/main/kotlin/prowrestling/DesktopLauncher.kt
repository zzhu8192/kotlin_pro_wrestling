package prowrestling

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object DesktopLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setTitle("Pro Wrestling Reimagined")
            setWindowedMode(960, 720) // 4:3 aspect ratio (2x scale of 480x360 virtual)
            useVsync(true)
            setForegroundFPS(60)
        }
        Lwjgl3Application(ProWrestlingGame(), config)
    }
}

fun main(args: Array<String>) {
    DesktopLauncher.main(args)
}
