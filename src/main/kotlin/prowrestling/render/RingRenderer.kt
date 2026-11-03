package prowrestling.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import prowrestling.core.*

class RingRenderer(val ring: Ring) {

    // Colors based on authentic NES Pro Wrestling palette
    private val floorColor = Color(0.11f, 0.13f, 0.18f, 1f)       // Ringside concrete
    private val apronMatColor = Color(0.18f, 0.22f, 0.32f, 1f)     // Outside padded floor
    private val ringSkirtColor = Color(0.08f, 0.22f, 0.45f, 1f)    // Apron drop cloth
    private val ringCanvasColor = Color(0.72f, 0.80f, 0.88f, 1f)   // Iconic pale blue-gray canvas
    private val ringCanvasBorder = Color(0.55f, 0.65f, 0.75f, 1f)  // Mat perimeter stitching
    private val postSteelColor = Color(0.85f, 0.85f, 0.90f, 1f)    // Turnbuckle steel posts
    private val postShadowColor = Color(0.40f, 0.40f, 0.45f, 1f)

    // 3-rope strands: Red (top), White (mid), Blue (bottom)
    private val topRopeColor = Color(0.92f, 0.20f, 0.20f, 1f)
    private val midRopeColor = Color(0.96f, 0.96f, 0.96f, 1f)
    private val botRopeColor = Color(0.20f, 0.45f, 0.90f, 1f)

    private val redPadColor = Color(0.88f, 0.15f, 0.15f, 1f)
    private val bluePadColor = Color(0.18f, 0.40f, 0.85f, 1f)

    /**
     * Stage 1: Rendered behind characters.
     * Floor, mat, apron skirt, back corner posts, and back ropes (top edge & upper sides).
     */
    fun renderBack(shape: ShapeRenderer) {
        shape.set(ShapeRenderer.ShapeType.Filled)

        // 1. Arena floor outside the ring
        shape.color = floorColor
        shape.rect(0f, 0f, ring.virtualWidth, 262f)

        // 2. Ringside protective mats (perimeter around apron)
        shape.color = apronMatColor
        shape.rect(
            ring.floorBounds.x,
            ring.floorBounds.y,
            ring.floorBounds.width,
            ring.floorBounds.height
        )

        // 3. Ring base skirt / apron drop (simulates elevated ring structure)
        shape.color = ringSkirtColor
        shape.rect(
            ring.matBounds.x - 10f,
            ring.matBounds.y - 12f,
            ring.matBounds.width + 20f,
            ring.matBounds.height + 14f
        )

        // 4. Ring canvas mat (the playing field)
        shape.color = ringCanvasBorder
        shape.rect(
            ring.matBounds.x - 2f,
            ring.matBounds.y - 2f,
            ring.matBounds.width + 4f,
            ring.matBounds.height + 4f
        )

        shape.color = ringCanvasColor
        shape.rect(
            ring.matBounds.x,
            ring.matBounds.y,
            ring.matBounds.width,
            ring.matBounds.height
        )

        // Canvas subtle center ring emblem / circle
        shape.color = Color(0.66f, 0.75f, 0.84f, 1f)
        shape.circle(ring.matBounds.centerX, ring.matBounds.centerY, 36f, 32)
        shape.color = ringCanvasColor
        shape.circle(ring.matBounds.centerX, ring.matBounds.centerY, 33f, 32)

        // 5. Back corner turnbuckle posts (Top-Left & Top-Right)
        for (tb in ring.turnbuckles) {
            if (tb.corner == CornerPosition.TopLeft || tb.corner == CornerPosition.TopRight) {
                drawTurnbucklePost(shape, tb)
            }
        }

        // 6. Back ropes: Top edge (all 3 strands) and upper half of Left/Right ropes
        drawBackRopes(shape)
    }

    /**
     * Stage 2: Rendered in front of characters.
     * Front corner posts (Bottom-Left, Bottom-Right), front ropes, and front turnbuckle pads.
     */
    fun renderFront(shape: ShapeRenderer) {
        shape.set(ShapeRenderer.ShapeType.Filled)

        // 1. Front ropes: Bottom edge (all 3 strands) and lower half of Left/Right ropes
        drawFrontRopes(shape)

        // 2. Front corner turnbuckle posts (Bottom-Left & Bottom-Right)
        for (tb in ring.turnbuckles) {
            if (tb.corner == CornerPosition.BottomLeft || tb.corner == CornerPosition.BottomRight) {
                drawTurnbucklePost(shape, tb)
            }
        }
    }

    private fun drawTurnbucklePost(shape: ShapeRenderer, tb: Turnbuckle) {
        val postX = tb.x - 3f
        val postHeight = 32f
        val postY = tb.y - 4f

        // Post shadow
        shape.color = postShadowColor
        shape.rect(postX + 4f, postY, 2f, postHeight)

        // Main steel post
        shape.color = postSteelColor
        shape.rect(postX, postY, 4f, postHeight)

        // Top cap
        shape.circle(postX + 2f, postY + postHeight, 3.5f, 8)

        // Turnbuckle pads (3 pads per corner for 3 rope strands)
        val padCol = if (tb.padColorHex == 0xCC0000) redPadColor else bluePadColor
        shape.color = padCol
        for (i in 0 until 3) {
            val padY = postY + 8f + i * 8f
            shape.rect(postX - 2f, padY, 8f, 6f)
        }
    }

    private fun drawBackRopes(shape: ShapeRenderer) {
        val b = ring.innerBounds

        // 3 back rope strands (Top edge)
        val strandOffsets = floatArrayOf(16f, 10f, 4f)
        val strandColors = arrayOf(topRopeColor, midRopeColor, botRopeColor)

        for (i in 0 until 3) {
            val y = b.top + strandOffsets[i]
            shape.color = strandColors[i]
            shape.rectLine(b.left, y, b.right, y, 2.5f)

            // Upper segments of left & right ropes
            val midY = (b.top + b.bottom) / 2f
            shape.rectLine(b.left, b.top + strandOffsets[i], b.left, midY + strandOffsets[i], 2.0f)
            shape.rectLine(b.right, b.top + strandOffsets[i], b.right, midY + strandOffsets[i], 2.0f)
        }
    }

    private fun drawFrontRopes(shape: ShapeRenderer) {
        val b = ring.innerBounds

        // 3 front rope strands (Bottom edge)
        val strandOffsets = floatArrayOf(16f, 10f, 4f)
        val strandColors = arrayOf(topRopeColor, midRopeColor, botRopeColor)

        for (i in 0 until 3) {
            val y = b.bottom + strandOffsets[i]
            shape.color = strandColors[i]
            shape.rectLine(b.left, y, b.right, y, 2.5f)

            // Lower segments of left & right ropes
            val midY = (b.top + b.bottom) / 2f
            shape.rectLine(b.left, midY + strandOffsets[i], b.left, b.bottom + strandOffsets[i], 2.0f)
            shape.rectLine(b.right, midY + strandOffsets[i], b.right, b.bottom + strandOffsets[i], 2.0f)
        }
    }
}
