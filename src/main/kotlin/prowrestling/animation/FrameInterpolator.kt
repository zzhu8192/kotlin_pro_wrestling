package prowrestling.animation

/**
 * Handles sub-frame interpolation between consecutive animation keyframes.
 * Supports linear interpolation of positional offsets and cross-fading alpha.
 *
 * Fully decoupled from libGDX for pure headless unit testing.
 */
object FrameInterpolator {

    data class InterpolatedFrame(
        val frameIndex: Int,
        val nextFrameIndex: Int,
        val flipX: Boolean,
        val offsetX: Float,
        val offsetY: Float,
        val alpha: Float,
        val nextAlpha: Float,
        val blendProgress: Float
    )

    /**
     * Linear interpolation between two float values.
     */
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * Computes the interpolated frame properties given an AnimationPlayer and smoothing flag.
     *
     * @param player Current state of the animation player
     * @param enableSubFrameBlending Whether cross-fade alpha blending is enabled
     * @return InterpolatedFrame containing position offsets and frame alphas
     */
    fun interpolate(player: AnimationPlayer, enableSubFrameBlending: Boolean = false): InterpolatedFrame {
        val curr = player.currentFrame
        val next = player.nextFrame
        val progress = player.frameProgress

        val interpOffsetX = lerp(curr.offsetX, next.offsetX, progress)
        val interpOffsetY = lerp(curr.offsetY, next.offsetY, progress)

        return if (enableSubFrameBlending && curr.frameIndex != next.frameIndex) {
            InterpolatedFrame(
                frameIndex = curr.frameIndex,
                nextFrameIndex = next.frameIndex,
                flipX = curr.flipX,
                offsetX = interpOffsetX,
                offsetY = interpOffsetY,
                alpha = 1.0f - progress,
                nextAlpha = progress,
                blendProgress = progress
            )
        } else {
            InterpolatedFrame(
                frameIndex = curr.frameIndex,
                nextFrameIndex = curr.frameIndex,
                flipX = curr.flipX,
                offsetX = interpOffsetX,
                offsetY = interpOffsetY,
                alpha = 1.0f,
                nextAlpha = 0.0f,
                blendProgress = 0.0f
            )
        }
    }

    /**
     * Interpolate world coordinates between physics frames for 60fps/120fps smoothness.
     */
    fun interpolatePosition(x0: Float, y0: Float, x1: Float, y1: Float, alpha: Float): Pair<Float, Float> {
        return Pair(lerp(x0, x1, alpha), lerp(y0, y1, alpha))
    }
}
