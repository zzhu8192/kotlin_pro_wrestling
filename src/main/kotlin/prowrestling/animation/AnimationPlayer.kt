package prowrestling.animation

class AnimationPlayer(var currentAnim: AnimationDef = AnimationCatalog.Idle) {
    var currentFrameIndex: Int = 0
    var frameTimer: Float = 0f
    var isFinished: Boolean = false

    fun play(anim: AnimationDef) {
        if (currentAnim.name != anim.name) {
            currentAnim = anim
            currentFrameIndex = 0
            frameTimer = 0f
            isFinished = false
        }
    }

    fun update(delta: Float) {
        if (isFinished) return

        val framesDelta = delta * 60f
        frameTimer += framesDelta

        val currentDef = currentFrame
        if (frameTimer >= currentDef.durationFrames) {
            if (currentFrameIndex < currentAnim.frames.size - 1) {
                currentFrameIndex += 1
                frameTimer -= currentDef.durationFrames
            } else if (currentAnim.loop) {
                currentFrameIndex = 0
                frameTimer -= currentDef.durationFrames
            } else {
                isFinished = true
            }
        }
    }

    val currentFrame: FrameDef
        get() {
            return if (currentAnim.frames.isEmpty()) FrameDef(0, 60)
            else currentAnim.frames[Math.min(currentFrameIndex, currentAnim.frames.size - 1)]
        }

    val nextFrame: FrameDef
        get() {
            return if (currentAnim.frames.isEmpty()) FrameDef(0, 60)
            else if (currentFrameIndex < currentAnim.frames.size - 1) {
                currentAnim.frames[currentFrameIndex + 1]
            } else if (currentAnim.loop) {
                currentAnim.frames[0]
            } else {
                currentFrame
            }
        }

    /**
     * Progress of current frame (0.0f to 1.0f).
     * Used for smooth sub-frame sprite interpolation.
     */
    val frameProgress: Float
        get() {
            val dur = Math.max(1, currentFrame.durationFrames).toFloat()
            return Math.max(0f, Math.min(1.0f, frameTimer / dur))
        }
}
