package prowrestling.render

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import prowrestling.animation.AnimationCatalog
import prowrestling.animation.AnimationDef
import prowrestling.animation.AnimationPlayer
import prowrestling.animation.FrameInterpolator
import prowrestling.core.*

class WrestlerRenderer {

    private val defaultSkinColor = Color(0.92f, 0.70f, 0.68f, 1.0f)
    private val skinShadowColor = Color(0.78f, 0.52f, 0.50f, 1.0f)
    private val bootsColor = Color(0.12f, 0.12f, 0.14f, 1.0f)
    private val hairColor = Color(0.10f, 0.10f, 0.12f, 1.0f)
    private val shadowColor = Color(0f, 0.02f, 0.05f, 0.13f)
    private val outlineColor = Color(0.08f, 0.08f, 0.10f, 1.0f)

    private var animTimer = 0f
    private val players = mutableMapOf<String, AnimationPlayer>()

    fun getAnimationPlayer(wrestler: Wrestler): AnimationPlayer {
        val key = "${wrestler.characterId}_${if (wrestler.isPlayer1) "p1" else "p2"}"
        return players.getOrPut(key) { AnimationPlayer(AnimationCatalog.Idle) }
    }

    fun animForState(wrestler: Wrestler): AnimationDef {
        val dir = wrestler.facingDirection
        return when (val state = wrestler.state) {
            Idle -> AnimationCatalog.Idle
            is Walking -> when (state.direction) {
                Direction.Up -> AnimationCatalog.WalkUp
                Direction.Down -> AnimationCatalog.WalkDown
                Direction.Left, Direction.UpLeft, Direction.DownLeft -> AnimationCatalog.WalkLeft
                Direction.Right, Direction.UpRight, Direction.DownRight -> AnimationCatalog.WalkRight
            }
            is Running -> when (state.direction) {
                Direction.Left -> AnimationCatalog.RunLeft
                Direction.Right -> AnimationCatalog.RunRight
                else -> if (dir == Direction.Left) AnimationCatalog.RunLeft else AnimationCatalog.RunRight
            }
            is IrishWhipped -> when (state.direction) {
                Direction.Left -> AnimationCatalog.RunLeft
                Direction.Right -> AnimationCatalog.RunRight
                else -> if (dir == Direction.Left) AnimationCatalog.RunLeft else AnimationCatalog.RunRight
            }
            is RopeRebound -> AnimationCatalog.RopeRebound
            is Executing -> AnimationCatalog.forMove(state.moveId)
            Grappling -> AnimationCatalog.GrappleLockup
            is Flinching -> AnimationCatalog.Flinch
            is FlyingCrossChop -> AnimationCatalog.FlyingCrossChop
            is Stunned -> AnimationCatalog.Stunned
            is Downed -> AnimationCatalog.Downed
            is ClimbingTurnbuckle -> AnimationCatalog.ClimbTurnbuckle
            is OnTurnbuckle -> AnimationCatalog.OnTurnbuckle
            is Diving -> AnimationCatalog.DivingSplash
            is BeingThrown -> AnimationCatalog.forThrown(state.moveId)
            Pinning, is BeingPinned -> AnimationCatalog.Downed
            else -> AnimationCatalog.Idle
        }
    }

    fun update(delta: Float) {
        animTimer += delta
    }

    fun update(delta: Float, wrestlers: Iterable<Wrestler>) {
        animTimer += delta
        for (w in wrestlers) {
            val player = getAnimationPlayer(w)
            val targetAnim = animForState(w)
            player.play(targetAnim)
            player.update(delta)
        }
    }

    /**
     * Render drop shadow on ring canvas or outside floor.
     * Uses a subtle, realistic horizontal sliver directly under the wrestler's feet.
     */
    fun renderShadow(shape: ShapeRenderer, wrestler: Wrestler) {
        val x = wrestler.x
        val y = wrestler.y
        shape.color = shadowColor
        when (val state = wrestler.state) {
            is Downed -> {
                val w = 52f
                val h = 3.8f
                shape.ellipse(x - w / 2f, y + 2.5f - h / 2f, w, h)
            }
            is Executing -> {
                if (state.moveId == "kick_out") {
                    val w = 52f
                    val h = 3.8f
                    shape.ellipse(x - w / 2f, y + 2.5f - h / 2f, w, h)
                } else {
                    val w = 32f
                    val h = 2.9f
                    shape.ellipse(x - w / 2f, y - h / 2f, w, h)
                }
            }
            is ClimbingTurnbuckle, is OnTurnbuckle -> {
                val corner = when (state) {
                    is ClimbingTurnbuckle -> state.corner
                    is OnTurnbuckle -> state.corner
                }
                val w = 20f
                val h = 2.3f
                val cx = corner?.x ?: x
                val cy = corner?.y ?: y
                shape.ellipse(cx - w / 2f, cy - h / 2f, w, h)
            }
            is Diving -> {
                val w = 26f
                val h = 2.6f
                val shadowX = state.startX + (state.targetX - state.startX) * state.progress
                val shadowY = state.startY + (state.targetY - state.startY) * state.progress - 10f
                shape.ellipse(shadowX - w / 2f, shadowY - h / 2f, w, h)
            }
            is FlyingCrossChop -> {
                val w = 35f
                val h = 2.6f
                shape.ellipse(x - w / 2f, y - 12f - h / 2f, w, h)
            }
            is Running, is RopeRebound, is IrishWhipped -> {
                val w = 35f
                val h = 2.6f
                shape.ellipse(x - w / 2f, y - h / 2f, w, h)
            }
            is BeingThrown -> {
                if (state.moveId == "ground_pickup") {
                    val progress = state.currentFrame.toFloat() / Math.max(1, state.totalFrames)
                    if (progress < 0.4f) {
                        val w = 52f
                        val h = 3.8f
                        shape.ellipse(x - w / 2f, y + 2.5f - h / 2f, w, h)
                    } else {
                        val w = 32f
                        val h = 2.9f
                        shape.ellipse(x - w / 2f, y - h / 2f, w, h)
                    }
                } else {
                    val w = 32f
                    val h = 2.9f
                    shape.ellipse(x - w / 2f, y - h / 2f, w, h)
                }
            }
            else -> {
                val w = 32f
                val h = 2.9f
                shape.ellipse(x - w / 2f, y - h / 2f, w, h)
            }
        }
    }

    /**
     * Primary sprite rendering using character atlas texture regions and FrameInterpolator.
     * Falls back seamlessly to procedural shape rendering if sprites are unavailable.
     */
    fun render(batch: SpriteBatch, shapeFallback: ShapeRenderer, wrestler: Wrestler) {
        val sheet: SpriteSheet? = SpriteSheet.load(wrestler.characterId)
        if (sheet != null) {
            val player = getAnimationPlayer(wrestler)
            val interp = FrameInterpolator.interpolate(player, enableSubFrameBlending = false)
            val region = sheet.getFrame(interp.frameIndex)

            val isFacingLeft = wrestler.facingDirection == Direction.Left
            val sign = if (isFacingLeft) -1f else 1f
            val drawW = 66f
            val drawH = 102f
            val originX = drawW / 2f
            val originY = 0f
            val rotation = 0f

            // Atlas cell is 96x192 with boot soles ending at row 162 (30px bottom padding).
            // Shifting drawY by 16f (102 * 30 / 192) aligns boot soles directly at wrestler.y.
            val spriteFootOffset = 16f
            val drawX = wrestler.x - originX + (interp.offsetX * sign)
            val drawY = wrestler.y - spriteFootOffset + interp.offsetY
            val scaleX = if (isFacingLeft xor interp.flipX) -1f else 1f

            // Tinting for Flinching / Stunned
            when (val state = wrestler.state) {
                is Flinching -> if ((state.framesLeft / 2) % 2 == 0) {
                    batch.color = Color(1.0f, 0.45f, 0.45f, 1.0f)
                } else {
                    batch.color = Color.WHITE
                }
                is Stunned -> if ((state.framesLeft / 4) % 2 == 0) {
                    batch.color = Color(1.0f, 0.85f, 0.2f, 0.9f)
                } else {
                    batch.color = Color.WHITE
                }
                else -> batch.color = Color.WHITE
            }

            // Running / Cross Chop speed trail ghost
            if (wrestler.state is FlyingCrossChop) {
                val trailOffset1 = 18f * sign
                val trailOffset2 = 36f * sign
                batch.color = Color(0.3f, 0.6f, 1.0f, 0.35f)
                batch.draw(region, drawX - trailOffset2, drawY, originX, originY, drawW, drawH, scaleX, 1f, rotation)
                batch.color = Color(0.6f, 0.2f, 0.8f, 0.5f)
                batch.draw(region, drawX - trailOffset1, drawY, originX, originY, drawW, drawH, scaleX, 1f, rotation)
                batch.color = Color.WHITE
            } else if (wrestler.isRunning || (wrestler.state is Executing && ((wrestler.state as Executing).moveId == "clothesline" || (wrestler.state as Executing).moveId == "dropkick"))) {
                val trailOffset = 15f * sign
                val trunksColor = colorFromHex(wrestler.trunksColorHex)
                batch.color = Color(trunksColor.r, trunksColor.g, trunksColor.b, 0.35f)
                batch.draw(region, drawX - trailOffset, drawY, originX, originY, drawW, drawH, scaleX, 1f, rotation)
                batch.color = Color.WHITE
            }

            batch.draw(region, drawX, drawY, originX, originY, drawW, drawH, scaleX, 1f, rotation)
            batch.color = Color.WHITE
        } else {
            val wasDrawing = batch.isDrawing
            if (wasDrawing) batch.end()
            shapeFallback.begin(ShapeRenderer.ShapeType.Filled)
            render(shapeFallback, wrestler)
            shapeFallback.end()
            if (wasDrawing) batch.begin()
        }
    }

    /**
     * Procedural shape fallback rendering.
     */
    fun render(shape: ShapeRenderer, wrestler: Wrestler) {
        val x = wrestler.x
        val y = wrestler.y
        val isFacingLeft = wrestler.facingDirection == Direction.Left
        val sign = if (isFacingLeft) -1f else 1f

        val trunksColor = colorFromHex(wrestler.trunksColorHex)

        // 1. Drop shadow beneath feet (or body if downed)
        renderShadow(shape, wrestler)

        // 2. State-specific poses
        when (val state = wrestler.state) {
            is Downed -> {
                renderDownedPose(shape, wrestler, trunksColor)
                return
            }

            is ClimbingTurnbuckle -> {
                renderClimbingPose(shape, wrestler, state.framesLeft, trunksColor, sign)
                return
            }

            is OnTurnbuckle -> {
                renderOnTurnbucklePose(shape, wrestler, trunksColor, sign)
                return
            }

            is Diving -> {
                renderDivingPose(shape, wrestler, state.progress, trunksColor, sign)
                return
            }

            is BeingThrown -> {
                if (state.moveId == "ground_pickup") {
                    renderBeingPickedUpPose(shape, wrestler, state.currentFrame, state.totalFrames, trunksColor, sign)
                } else {
                    renderBeingThrownPose(shape, wrestler, state.currentFrame, state.totalFrames, trunksColor)
                }
                return
            }

            is Flinching -> {
                renderFlinchPose(shape, wrestler, trunksColor, sign)
                return
            }

            is FlyingCrossChop -> {
                renderFlyingCrossChopPose(shape, wrestler, trunksColor, sign)
                return
            }

            is Stunned -> {
                if ((state.framesLeft / 4) % 2 == 0) {
                    shape.color = Color(1.0f, 0.85f, 0.2f, 1.0f)
                    shape.rect(x - wrestler.width / 2f, y, wrestler.width, wrestler.height)
                    return
                }
            }

            Grappling -> {
                renderGrapplePose(shape, wrestler, trunksColor, sign)
                return
            }

            is Executing -> {
                renderAttackPose(shape, wrestler, state.moveId, state.currentFrame, state.totalFrames, trunksColor, sign)
                return
            }

            else -> {}
        }

        // 3. Standard Standing / Walking / Running pose
        val bob = when (wrestler.state) {
            is Walking -> Math.sin(animTimer * 12.0).toFloat() * 2.5f
            is Running, is RopeRebound, is IrishWhipped -> Math.sin(animTimer * 22.0).toFloat() * 4.0f
            else -> Math.sin(animTimer * 3.0).toFloat() * 0.8f
        }

        val baseY = y + Math.max(0f, bob)

        if (wrestler.isRunning) {
            val trailOffset = 12f * sign
            shape.color = Color(trunksColor.r, trunksColor.g, trunksColor.b, 0.3f)
            shape.rect(x - trailOffset - wrestler.width / 2f, y + 4f, wrestler.width, wrestler.height * 0.8f)
        }

        val legSep = 7f
        val bootH = 14f

        shape.color = defaultSkinColor
        shape.rect(x - legSep - 3f, baseY + bootH, 6f, 14f)
        shape.rect(x + legSep - 3f, baseY + bootH, 6f, 14f)
        shape.color = bootsColor
        shape.rect(x - legSep - 4f, baseY, 8f, bootH)
        shape.rect(x + legSep - 4f, baseY, 8f, bootH)

        val trunksY = baseY + 24f
        shape.color = trunksColor
        shape.rect(x - 12f, trunksY, 24f, 12f)
        shape.color = Color(0.95f, 0.95f, 0.95f, 1.0f)
        shape.rect(x - 12f, trunksY + 10f, 24f, 3f)

        val torsoY = trunksY + 12f
        val torsoH = 18f
        val torsoW = 24f
        shape.color = defaultSkinColor
        shape.rect(x - torsoW / 2f, torsoY, torsoW, torsoH)

        val shoulderY = torsoY + torsoH - 4f
        val armW = 7f
        val armH = 16f
        shape.color = skinShadowColor
        shape.rect(x - (torsoW / 2f + armW - 2f) * sign, shoulderY - armH + 4f, armW, armH)

        shape.color = defaultSkinColor
        val frontArmX = x + (torsoW / 2f - 2f) * sign
        val armOffset = if (wrestler.isRunning) 6f * sign else 2f * sign
        shape.rect(frontArmX, shoulderY - armH + 4f + bob, armW, armH)
        shape.circle(frontArmX + (armW / 2f) + armOffset, shoulderY - armH + 4f + bob, 4.5f, 8)

        renderHead(shape, wrestler, x, torsoY + torsoH, sign)
    }

    private fun renderHead(shape: ShapeRenderer, wrestler: Wrestler, x: Float, headY: Float, sign: Float) {
        val headRadius = 9f
        val headCenterX = x + (2f * sign)
        val headCenterY = headY + headRadius - 1f

        if (wrestler.isMasked) {
            shape.color = Color(0.60f, 0.15f, 0.65f, 1.0f)
            shape.circle(headCenterX, headCenterY, headRadius, 16)
            shape.color = Color(1.0f, 1.0f, 1.0f, 1.0f)
            shape.circle(headCenterX + (4.0f * sign), headCenterY + 1f, 2.5f, 8)
            shape.color = Color(0.1f, 0.1f, 0.1f, 1.0f)
            shape.circle(headCenterX + (4.5f * sign), headCenterY + 1f, 1.2f, 6)
            shape.color = Color(1.0f, 0.9f, 0.2f, 1.0f)
            shape.circle(headCenterX - (1f * sign), headCenterY + 3.5f, 2.5f, 6)
        } else {
            shape.color = defaultSkinColor
            shape.circle(headCenterX, headCenterY, headRadius, 16)
            shape.color = Color(0.1f, 0.1f, 0.1f, 1f)
            shape.circle(headCenterX + (4.5f * sign), headCenterY + 1f, 1.8f, 6)
            shape.color = hairColor
            shape.rect(headCenterX - 8f, headCenterY + 3f, 16f, 7f)
            shape.circle(headCenterX - (2f * sign), headCenterY + 5f, 6.5f, 12)
        }
    }

    private fun renderDownedPose(shape: ShapeRenderer, wrestler: Wrestler, trunksColor: Color) {
        val x = wrestler.x
        val y = wrestler.y
        val bodyL = 44f
        val bodyH = 14f

        shape.color = defaultSkinColor
        shape.rect(x - bodyL / 2f, y, bodyL, bodyH)
        shape.color = trunksColor
        shape.rect(x - 6f, y, 16f, bodyH)
        shape.color = bootsColor
        shape.rect(x + bodyL / 2f - 10f, y, 10f, bodyH)
        shape.color = defaultSkinColor
        shape.circle(x - bodyL / 2f, y + bodyH / 2f, 8f, 12)
    }

    private fun renderClimbingPose(shape: ShapeRenderer, wrestler: Wrestler, frames: Int, trunksColor: Color, sign: Float) {
        val x = wrestler.x
        val y = wrestler.y
        renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
        shape.color = defaultSkinColor
        shape.circle(x - 8f, y + 36f, 4f, 6)
        shape.circle(x + 8f, y + 42f, 4f, 6)
    }

    private fun renderOnTurnbucklePose(shape: ShapeRenderer, wrestler: Wrestler, trunksColor: Color, sign: Float) {
        val x = wrestler.x
        val y = wrestler.y

        shape.color = bootsColor
        shape.rect(x - 8f, y, 16f, 8f)
        shape.color = trunksColor
        shape.rect(x - 10f, y + 8f, 20f, 12f)
        shape.color = defaultSkinColor
        shape.rect(x - 9f, y + 20f, 18f, 14f)
        shape.rectLine(x - 14f, y + 28f, x + 14f, y + 28f, 4f)
        renderHead(shape, wrestler, x, y + 34f, sign)
    }

    private fun renderDivingPose(shape: ShapeRenderer, wrestler: Wrestler, progress: Float, trunksColor: Color, sign: Float) {
        val x = wrestler.x
        val y = wrestler.y
        val altitude = Math.sin(progress * Math.PI).toFloat() * 36f
        val currentY = y + altitude

        val bodyW = 40f
        val bodyH = 14f
        shape.color = defaultSkinColor
        shape.rect(x - bodyW / 2f, currentY, bodyW, bodyH)
        shape.color = trunksColor
        shape.rect(x - 6f, currentY, 14f, bodyH)

        shape.color = defaultSkinColor
        shape.rectLine(x - 4f, currentY + bodyH, x - 4f, currentY + bodyH + 12f, 5f)
        shape.rectLine(x + 4f, currentY + bodyH, x + 4f, currentY + bodyH + 12f, 5f)

        shape.color = bootsColor
        shape.rect(x - bodyW / 2f - 4f, currentY + 2f, 8f, 10f)

        renderHead(shape, wrestler, x + bodyW / 2f, currentY, sign)
    }

    private fun renderGrapplePose(shape: ShapeRenderer, wrestler: Wrestler, trunksColor: Color, sign: Float) {
        val x = wrestler.x
        val y = wrestler.y

        shape.color = bootsColor
        shape.rect(x - 8f, y, 7f, 14f)
        shape.rect(x + 1f, y, 7f, 14f)
        shape.color = trunksColor
        shape.rect(x - 10f, y + 14f, 20f, 12f)
        shape.color = defaultSkinColor
        shape.rect(x - 9f + (3f * sign), y + 26f, 20f, 18f)

        shape.color = skinShadowColor
        shape.rectLine(x + (8f * sign), y + 36f, x + (18f * sign), y + 36f, 5f)
        renderHead(shape, wrestler, x, y + 44f, sign)
    }

    private fun renderAttackPose(
        shape: ShapeRenderer,
        wrestler: Wrestler,
        moveId: String,
        curr: Int,
        total: Int,
        trunksColor: Color,
        sign: Float
    ) {
        val x = wrestler.x
        val y = wrestler.y

        when (moveId) {
            "punch" -> {
                renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
                shape.color = defaultSkinColor
                val punchReach = 20f * sign
                shape.rectLine(x, y + 38f, x + punchReach, y + 38f, 6f)
                shape.circle(x + punchReach, y + 38f, 5f, 8)
            }

            "kick" -> {
                renderTorsoAndHead(shape, wrestler, x, y, trunksColor, sign)
                shape.color = bootsColor
                shape.rect(x - (4f * sign), y, 8f, 18f)
                shape.color = defaultSkinColor
                val kickReach = 26f * sign
                shape.rectLine(x, y + 24f, x + kickReach, y + 24f, 6f)
                shape.color = bootsColor
                shape.rect(x + kickReach - (4f * sign), y + 20f, 8f, 8f)
            }

            "clothesline" -> {
                renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
                shape.color = defaultSkinColor
                val armReach = 24f * sign
                shape.rectLine(x - (6f * sign), y + 36f, x + armReach, y + 36f, 7f)
            }

            "dropkick" -> {
                val airY = y + 14f
                shape.color = trunksColor
                shape.rect(x - (10f * sign), airY + 6f, 16f, 10f)
                shape.color = defaultSkinColor
                shape.rect(x - (22f * sign), airY + 8f, 14f, 8f)
                val bootReach = 24f * sign
                shape.color = bootsColor
                shape.rect(x + bootReach, airY + 4f, 10f, 14f)
                renderHead(shape, wrestler, x - (26f * sign), airY + 8f, sign)
            }

            "enzuigiri" -> {
                val airY = y + 18f
                shape.color = trunksColor
                shape.rect(x - 8f, airY + 8f, 16f, 12f)
                shape.color = defaultSkinColor
                shape.rect(x - 7f, airY + 20f, 14f, 16f)
                val kickReach = 28f * sign
                shape.color = bootsColor
                shape.rect(x + kickReach, airY + 28f, 12f, 8f)
                renderHead(shape, wrestler, x - (6f * sign), airY + 24f, sign)
            }

            "somersault_kick" -> {
                val airY = y + 22f
                shape.color = trunksColor
                shape.rect(x - 8f, airY + 12f, 16f, 12f)
                shape.color = bootsColor
                shape.rect(x - (18f * sign), airY + 24f, 8f, 10f)
                shape.rect(x + (18f * sign), airY + 24f, 8f, 10f)
                renderHead(shape, wrestler, x, airY + 2f, sign)
            }

            "mongolian_chop" -> {
                renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
                shape.color = defaultSkinColor
                shape.rectLine(x - 6f, y + 42f, x + (18f * sign), y + 36f, 5f)
                shape.rectLine(x + 2f, y + 42f, x + (22f * sign), y + 36f, 5f)
            }

            "iron_claw" -> {
                renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
                shape.color = defaultSkinColor
                shape.rectLine(x, y + 38f, x + (20f * sign), y + 44f, 6f)
                shape.circle(x + (20f * sign), y + 44f, 6f, 8)
            }

            "piranha_bite" -> {
                renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
                shape.color = defaultSkinColor
                shape.circle(x + (14f * sign), y + 42f, 7f, 8)
            }

            "backbreaker" -> {
                renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
                shape.color = defaultSkinColor
                shape.rectLine(x - 12f, y + 32f, x + 12f, y + 32f, 8f)
            }

            "diving_splash" -> {
                renderDownedPose(shape, wrestler, trunksColor)
            }

            "ground_pickup" -> {
                renderGroundPickupAttackerShape(shape, wrestler, curr, total, trunksColor, sign)
            }

            else -> {
                renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
                shape.color = defaultSkinColor
                shape.rect(x - 10f, y + 44f, 6f, 16f)
                shape.rect(x + 4f, y + 44f, 6f, 16f)
            }
        }
    }

    private fun renderGroundPickupAttackerShape(
        shape: ShapeRenderer,
        wrestler: Wrestler,
        curr: Int,
        total: Int,
        trunksColor: Color,
        sign: Float
    ) {
        val x = wrestler.x
        val y = wrestler.y
        val progress = curr.toFloat() / Math.max(1, total)

        if (progress < 0.35f) {
            // Deep crouch and waist bend reaching down to mat
            shape.color = bootsColor
            shape.rect(x - (10f * sign), y, 8f, 10f)
            shape.rect(x - (2f * sign), y, 8f, 10f)
            shape.color = defaultSkinColor
            shape.rect(x - (11f * sign), y + 10f, 9f, 10f)
            shape.rect(x - (3f * sign), y + 10f, 9f, 10f)
            shape.color = trunksColor
            shape.rect(x - (10f * sign), y + 20f, 18f, 10f)
            shape.color = defaultSkinColor
            shape.rect(x - (6f * sign), y + 28f, 18f, 12f)
            renderHead(shape, wrestler, x + (4f * sign), y + 36f, sign)
            // Arms reaching low to ground
            shape.rectLine(x - (2f * sign), y + 32f, x + (14f * sign), y + 12f, 5.5f)
        } else if (progress < 0.70f) {
            // Semi-crouched lifting pose
            shape.color = bootsColor
            shape.rect(x - (8f * sign), y, 7f, 12f)
            shape.rect(x + (1f * sign), y, 7f, 12f)
            shape.color = defaultSkinColor
            shape.rect(x - (7f * sign), y + 12f, 7f, 12f)
            shape.rect(x + (1f * sign), y + 12f, 7f, 12f)
            shape.color = trunksColor
            shape.rect(x - (8f * sign), y + 22f, 18f, 11f)
            shape.color = defaultSkinColor
            shape.rect(x - (5f * sign), y + 31f, 18f, 14f)
            renderHead(shape, wrestler, x + (2f * sign), y + 42f, sign)
            // Arms bent hauling upward
            shape.rectLine(x, y + 36f, x + (14f * sign), y + 24f, 5.5f)
        } else {
            // Standing pull into lockup
            renderStandingBase(shape, wrestler, x, y, trunksColor, sign)
            shape.color = defaultSkinColor
            shape.rectLine(x + (2f * sign), y + 36f, x + (16f * sign), y + 36f, 6f)
        }
    }

    private fun renderBeingPickedUpPose(
        shape: ShapeRenderer,
        wrestler: Wrestler,
        curr: Int,
        total: Int,
        trunksColor: Color,
        sign: Float
    ) {
        val x = wrestler.x
        val y = wrestler.y
        val progress = curr.toFloat() / Math.max(1, total)

        if (progress < 0.28f) {
            // Phase 1: On mat, head and upper torso hoisted upward
            val bodyL = 40f
            val bodyH = 12f
            shape.color = bootsColor
            shape.rect(x - (bodyL / 2f * sign) - 4f, y, 10f, bodyH)
            shape.color = trunksColor
            shape.rect(x - (6f * sign), y, 14f, bodyH)
            shape.color = defaultSkinColor
            shape.rectLine(x - (2f * sign), y + 6f, x + (12f * sign), y + 16f, 8f)
            renderHead(shape, wrestler, x + (14f * sign), y + 18f, sign)
        } else if (progress < 0.55f) {
            // Phase 2: Kneeling on all fours (knees on mat, supporting arm braced)
            shape.color = bootsColor
            shape.rect(x - (14f * sign), y, 8f, 8f)
            shape.color = defaultSkinColor
            shape.rect(x - (10f * sign), y, 10f, 9f) // Knees on canvas
            shape.rect(x - (8f * sign), y + 9f, 10f, 8f) // Thighs angled
            shape.color = trunksColor
            shape.rect(x - (4f * sign), y + 16f, 14f, 9f)
            shape.color = defaultSkinColor
            shape.rectLine(x - (2f * sign), y + 20f, x + (8f * sign), y + 28f, 8f) // Torso angled up
            // Supporting arm down to canvas
            shape.rectLine(x + (4f * sign), y + 22f, x + (6f * sign), y + 2f, 4.5f)
            renderHead(shape, wrestler, x + (10f * sign), y + 30f, sign)
        } else if (progress < 0.80f) {
            // Phase 3: Rising on one knee (one knee on canvas, front foot planted, rising crouch)
            shape.color = bootsColor
            shape.rect(x - (12f * sign), y, 8f, 8f) // Back knee down
            shape.rect(x + (4f * sign), y, 8f, 12f) // Front foot flat on mat
            shape.color = defaultSkinColor
            shape.rect(x - (10f * sign), y + 8f, 8f, 10f)
            shape.rect(x + (4f * sign), y + 12f, 7f, 10f)
            shape.color = trunksColor
            shape.rect(x - (4f * sign), y + 20f, 16f, 10f)
            shape.color = defaultSkinColor
            shape.rect(x - (2f * sign), y + 28f, 16f, 12f)
            renderHead(shape, wrestler, x + (6f * sign), y + 38f, sign)
            shape.rectLine(x + (4f * sign), y + 30f, x + (12f * sign), y + 28f, 4.5f)
        } else {
            // Phase 4: Standing hunched forward dazed into lockup
            renderFlinchPose(shape, wrestler, trunksColor, sign)
        }
    }

    private fun renderBeingThrownPose(
        shape: ShapeRenderer,
        wrestler: Wrestler,
        curr: Int,
        total: Int,
        trunksColor: Color
    ) {
        val x = wrestler.x
        val progress = curr.toFloat() / Math.max(1, total)
        val apexY = wrestler.y + (Math.sin(progress * Math.PI).toFloat() * 36f)

        shape.color = trunksColor
        shape.rect(x - 8f, apexY + 16f, 16f, 12f)
        shape.color = defaultSkinColor
        shape.rect(x - 7f, apexY + 6f, 14f, 10f)

        shape.color = bootsColor
        shape.rect(x - 9f, apexY + 28f, 7f, 12f)
        shape.rect(x + 2f, apexY + 28f, 7f, 12f)
    }

    private fun renderStandingBase(
        shape: ShapeRenderer,
        wrestler: Wrestler,
        x: Float,
        y: Float,
        trunksColor: Color,
        sign: Float
    ) {
        shape.color = bootsColor
        shape.rect(x - 9f, y, 7f, 14f)
        shape.rect(x + 2f, y, 7f, 14f)
        shape.color = trunksColor
        shape.rect(x - 10f, y + 14f, 20f, 12f)
        renderTorsoAndHead(shape, wrestler, x, y, trunksColor, sign)
    }

    private fun renderTorsoAndHead(
        shape: ShapeRenderer,
        wrestler: Wrestler,
        x: Float,
        y: Float,
        trunksColor: Color,
        sign: Float
    ) {
        shape.color = defaultSkinColor
        shape.rect(x - 10f, y + 26f, 20f, 18f)
        renderHead(shape, wrestler, x, y + 44f, sign)
    }

    private fun renderFlinchPose(
        shape: ShapeRenderer,
        wrestler: Wrestler,
        trunksColor: Color,
        sign: Float
    ) {
        val x = wrestler.x
        val y = wrestler.y
        val recoilX = -6f * sign
        shape.color = bootsColor
        shape.rect(x - 11f * sign, y, 7f, 13f)
        shape.rect(x + 4f * sign, y, 7f, 13f)
        shape.color = trunksColor
        shape.rect(x - 10f + recoilX * 0.5f, y + 13f, 20f, 12f)
        shape.color = defaultSkinColor
        shape.rect(x - 10f + recoilX, y + 25f, 20f, 18f)
        renderHead(shape, wrestler, x + recoilX * 1.5f, y + 43f, sign)
        shape.rect(x - 16f * sign + recoilX, y + 30f, 6f, 14f)
        shape.rect(x - 22f * sign + recoilX, y + 36f, 8f, 6f)
    }

    private fun renderFlyingCrossChopPose(
        shape: ShapeRenderer,
        wrestler: Wrestler,
        trunksColor: Color,
        sign: Float
    ) {
        val x = wrestler.x
        val y = wrestler.y + 20f
        val suitColor = if (wrestler.characterId == "starman") Color(0.66f, 0.06f, 0.50f, 1f) else defaultSkinColor
        shape.color = bootsColor
        shape.rect(x - 22f * sign, y + 2f, 14f, 8f)
        shape.color = trunksColor
        shape.rect(x - 10f * sign, y + 1f, 14f, 10f)
        shape.color = suitColor
        shape.rect(x + 2f * sign, y, 18f, 12f)
        renderHead(shape, wrestler, x + 24f * sign, y + 2f, sign)
        shape.color = Color.WHITE
        shape.rect(x + 10f * sign, y - 2f, 6f, 16f)
        shape.rect(x + 6f * sign, y + 4f, 14f, 6f)
    }

    private fun colorFromHex(hex: Int): Color {
        val r = ((hex shr 16) and 0xFF) / 255f
        val g = ((hex shr 8) and 0xFF) / 255f
        val b = (hex and 0xFF) / 255f
        return Color(r, g, b, 1f)
    }
}
