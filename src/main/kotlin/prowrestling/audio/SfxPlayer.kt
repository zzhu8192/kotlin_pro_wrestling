package prowrestling.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound

/**
 * Manages one-shot sound effects using libGDX Sound system.
 * Lazily loads and caches WAV audio samples.
 */
object SfxPlayer {

    private val soundCache = mutableMapOf<String, Sound?>()
    private var isMuted: Boolean = false
    private var masterVolume: Float = 1.0f

    const val BellRing = "bell_ring"
    const val PunchHit = "punch_hit"
    const val KickHit = "kick_hit"
    const val SlamImpact = "slam_impact"
    const val BodyDrop = "body_drop"
    const val RopeBounce = "rope_bounce"
    const val TurnbuckleClimb = "turnbuckle_climb"
    const val DiveCrash = "dive_crash"
    const val RefereeSlap = "referee_slap"
    const val CrowdRoar = "crowd_roar"

    fun play(sfxId: String, volumeMultiplier: Float = 1.0f) {
        if (isMuted) return
        if (Gdx.audio == null) return

        try {
            val sound = soundCache.getOrPut(sfxId) { loadSound(sfxId) }
            if (sound != null) {
                val vol = Math.max(0f, Math.min(1.0f, masterVolume * volumeMultiplier))
                sound.play(vol)
            }
        } catch (_: Throwable) {
            // Graceful catch for audio hardware unavailable
        }
    }

    private fun loadSound(sfxId: String): Sound? {
        return try {
            val path = "assets/audio/sfx/$sfxId.wav"
            if (Gdx.files != null && Gdx.files.internal(path).exists()) {
                Gdx.audio.newSound(Gdx.files.internal(path))
            } else if (Gdx.files != null && Gdx.files.classpath(path).exists()) {
                Gdx.audio.newSound(Gdx.files.classpath(path))
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun setVolume(volume: Float) {
        masterVolume = Math.max(0f, Math.min(1.0f, volume))
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun preloadAll() {
        val allSfx = listOf(
            BellRing, PunchHit, KickHit, SlamImpact, BodyDrop,
            RopeBounce, TurnbuckleClimb, DiveCrash, RefereeSlap, CrowdRoar
        )
        for (id in allSfx) {
            soundCache.getOrPut(id) { loadSound(id) }
        }
    }

    fun dispose() {
        soundCache.values.filterNotNull().forEach { sound ->
            try {
                sound.dispose()
            } catch (_: Throwable) {}
        }
        soundCache.clear()
    }
}
