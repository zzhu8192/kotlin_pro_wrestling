package prowrestling.core

/**
 * Pure model of the wrestling ring and surrounding arena floor.
 * Note: Keeps zero dependencies on libGDX or rendering code.
 */
data class Rect(val x: Float, val y: Float, val width: Float, val height: Float) {
    val left: Float get() = x
    val right: Float get() = x + width
    val bottom: Float get() = y
    val top: Float get() = y + height
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f

    fun contains(px: Float, py: Float): Boolean =
        px >= left && px <= right && py >= bottom && py <= top
}

enum class CornerPosition {
    TopLeft, TopRight, BottomLeft, BottomRight
}

data class Turnbuckle(
    val corner: CornerPosition,
    val x: Float,
    val y: Float,
    val climbZone: Rect,
    val padColorHex: Int
)

class Ring(val virtualWidth: Float = 480f, val virtualHeight: Float = 360f) {
    // Proportions scaled from NES original (256x240 screen, 180x120 mat) with 1.875x scale
    // Mat dimensions
    val matWidth: Float = 338f
    val matHeight: Float = 200f
    val matX: Float = (virtualWidth - matWidth) / 2f    // 71f, perfectly horizontally centered
    val matY: Float = 60f                              // Leaves ~100px at top for crowd & apron

    val matBounds: Rect = Rect(matX, matY, matWidth, matHeight)

    // Inset for ropes (wrestlers fight inside ropes)
    val ropeInsetX: Float = 16f
    val ropeInsetY: Float = 14f

    val innerBounds: Rect = Rect(
        matX + ropeInsetX,
        matY + ropeInsetY,
        matWidth - ropeInsetX * 2f,
        matHeight - ropeInsetY * 2f
    )

    // Apron (ledge of ring outside ropes)
    val apronBounds: Rect = Rect(
        matX - 6f,
        matY - 6f,
        matWidth + 12f,
        matHeight + 12f
    )

    // Arena floor bounds (where floor brawls happen outside the ring)
    val floorBounds: Rect = Rect(
        10f,
        10f,
        virtualWidth - 20f,
        matY + matHeight + 15f
    )

    // Turnbuckle corner posts & climb zones
    private val climbZoneSize: Float = 24f

    val turnbuckles: List<Turnbuckle> = listOf(
        Turnbuckle(
            CornerPosition.TopLeft,
            innerBounds.left,
            innerBounds.top,
            Rect(innerBounds.left - 4f, innerBounds.top - climbZoneSize + 4f, climbZoneSize, climbZoneSize),
            0x3465A4 // Blue pad
        ),
        Turnbuckle(
            CornerPosition.TopRight,
            innerBounds.right,
            innerBounds.top,
            Rect(innerBounds.right - climbZoneSize + 4f, innerBounds.top - climbZoneSize + 4f, climbZoneSize, climbZoneSize),
            0xCC0000 // Red pad
        ),
        Turnbuckle(
            CornerPosition.BottomLeft,
            innerBounds.left,
            innerBounds.bottom,
            Rect(innerBounds.left - 4f, innerBounds.bottom - 4f, climbZoneSize, climbZoneSize),
            0xCC0000 // Red pad
        ),
        Turnbuckle(
            CornerPosition.BottomRight,
            innerBounds.right,
            innerBounds.bottom,
            Rect(innerBounds.right - climbZoneSize + 4f, innerBounds.bottom - 4f, climbZoneSize, climbZoneSize),
            0x3465A4 // Blue pad
        )
    )

    fun isInsideRopes(x: Float, y: Float): Boolean = innerBounds.contains(x, y)

    fun isOnMat(x: Float, y: Float): Boolean = matBounds.contains(x, y)

    fun isOutsideRing(x: Float, y: Float): Boolean = !matBounds.contains(x, y) && floorBounds.contains(x, y)

    fun nearestTurnbuckle(x: Float, y: Float, maxDist: Float = 28f): Turnbuckle? {
        return turnbuckles.find { tb ->
            val dx = tb.x - x
            val dy = tb.y - y
            Math.sqrt((dx * dx + dy * dy).toDouble()) <= maxDist
        }
    }
}
