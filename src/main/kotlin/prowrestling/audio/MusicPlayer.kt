package prowrestling.audio

import java.io.InputStream
import java.util.concurrent.Executors
import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequence
import javax.sound.midi.Sequencer

/**
 * Manages MIDI music playback using Java's built-in MidiSystem.
 * Gracefully handles headless environments or missing sound hardware.
 * Offloads MIDI sequence loading and player control to a dedicated background
 * daemon thread so the LibGDX render loop is never blocked or stalled.
 */
object MusicPlayer {

    private var sequencer: Sequencer? = null
    @Volatile private var currentTrack: String? = null
    @Volatile private var isMuted: Boolean = false
    private var masterVolume: Float = 0.8f

    private var initialized: Boolean = false
    private val sequenceCache = mutableMapOf<String, Sequence>()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MusicPlayer-Worker").apply { isDaemon = true }
    }

    private fun initSequencer() {
        if (initialized) return
        initialized = true
        try {
            val seq = MidiSystem.getSequencer()
            if (seq != null) {
                seq.open()
                sequencer = seq
            }
        } catch (e: Throwable) {
            System.err.println("[MusicPlayer] MIDI sequencer unavailable: ${e.message}")
            sequencer = null
        }
    }

    fun play(trackId: String, loop: Boolean = true) {
        if (isMuted) return
        if (currentTrack == trackId && sequencer?.isRunning == true) {
            return
        }
        currentTrack = trackId

        executor.submit {
            try {
                if (isMuted) return@submit
                initSequencer()
                val seq = sequencer ?: return@submit

                val sequence = sequenceCache.getOrPut(trackId) {
                    val resourcePath = "/assets/audio/music/$trackId.mid"
                    var stream: InputStream? = javaClass.getResourceAsStream(resourcePath)
                    if (stream == null) {
                        stream = javaClass.getResourceAsStream("assets/audio/music/$trackId.mid")
                    }
                    if (stream != null) {
                        val seqObj = MidiSystem.getSequence(stream)
                        stream.close()
                        seqObj
                    } else {
                        System.err.println("[MusicPlayer] Could not find MIDI file for track: $trackId")
                        return@submit
                    }
                }

                if (isMuted) return@submit

                if (seq.isRunning) {
                    seq.stop()
                }
                seq.sequence = sequence
                seq.loopCount = if (loop) Sequencer.LOOP_CONTINUOUSLY else 0
                seq.start()
            } catch (e: Throwable) {
                System.err.println("[MusicPlayer] Error playing track '$trackId': ${e.message}")
            }
        }
    }

    fun stop() {
        currentTrack = null
        executor.submit {
            try {
                sequencer?.let { seq ->
                    if (seq.isRunning) seq.stop()
                }
            } catch (_: Throwable) {}
        }
    }

    fun pause() {
        executor.submit {
            try {
                sequencer?.let { seq ->
                    if (seq.isRunning) seq.stop()
                }
            } catch (_: Throwable) {}
        }
    }

    fun resume() {
        executor.submit {
            try {
                sequencer?.let { seq ->
                    if (!seq.isRunning && currentTrack != null) seq.start()
                }
            } catch (_: Throwable) {}
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (isMuted) stop()
    }

    fun isMuted(): Boolean = isMuted

    fun toggleMute(): Boolean {
        val prevTrack = currentTrack
        isMuted = !isMuted
        if (isMuted) {
            stop()
            currentTrack = prevTrack
        } else if (prevTrack != null) {
            play(prevTrack, loop = true)
        }
        return isMuted
    }

    fun getCurrentTrack(): String? = currentTrack

    fun dispose() {
        stop()
        executor.submit {
            try {
                sequencer?.close()
            } catch (_: Throwable) {}
            sequencer = null
            initialized = false
            sequenceCache.clear()
        }
    }
}
