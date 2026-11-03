package prowrestling.animation

data class FrameDef(
    val frameIndex: Int,
    val durationFrames: Int,
    val flipX: Boolean = false,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class AnimationDef(
    val name: String,
    val frames: List<FrameDef>,
    val loop: Boolean = true
) {
    val totalFrames: Int = frames.sumOf { it.durationFrames }
}

object AnimationCatalog {
    val Idle = AnimationDef("idle", listOf(FrameDef(0, 60)), loop = true)
    val IdleBreathe = AnimationDef("idle_breathe", listOf(FrameDef(0, 30), FrameDef(1, 30)), loop = true)

    val WalkUp = AnimationDef("walk_up", listOf(FrameDef(2, 8), FrameDef(3, 8)), loop = true)
    val WalkDown = AnimationDef("walk_down", listOf(FrameDef(4, 8), FrameDef(5, 8)), loop = true)
    val WalkLeft = AnimationDef("walk_left", listOf(FrameDef(6, 8), FrameDef(7, 8)), loop = true)
    val WalkRight = AnimationDef("walk_right", listOf(FrameDef(6, 8), FrameDef(7, 8)), loop = true)

    val RunLeft = AnimationDef("run_left", listOf(FrameDef(8, 4), FrameDef(9, 4)), loop = true)
    val RunRight = AnimationDef("run_right", listOf(FrameDef(8, 4), FrameDef(9, 4)), loop = true)

    val Punch = AnimationDef("punch", listOf(FrameDef(10, 8), FrameDef(11, 14)), loop = false)
    val Kick = AnimationDef("kick", listOf(FrameDef(12, 12), FrameDef(13, 18)), loop = false)

    val GrappleLockup = AnimationDef("grapple_lockup", listOf(FrameDef(14, 8), FrameDef(15, 8)), loop = true)

    val BodySlam = AnimationDef("body_slam", listOf(
        FrameDef(18, 12),
        FrameDef(19, 12),
        FrameDef(81, 14),
        FrameDef(20, 12),
        FrameDef(21, 10)
    ), loop = false)
    val Brainbuster = AnimationDef("brainbuster", listOf(
        FrameDef(25, 12),
        FrameDef(85, 12),
        FrameDef(26, 16),
        FrameDef(27, 12),
        FrameDef(28, 12)
    ), loop = false)
    val Piledriver = AnimationDef("piledriver", listOf(
        FrameDef(38, 12),
        FrameDef(83, 14),
        FrameDef(39, 16),
        FrameDef(40, 18)
    ), loop = false)
    val BackSuplex = AnimationDef("back_suplex", listOf(
        FrameDef(32, 12),
        FrameDef(87, 14),
        FrameDef(33, 16),
        FrameDef(34, 18)
    ), loop = false)
    val IrishWhip = AnimationDef("irish_whip", listOf(FrameDef(36, 16), FrameDef(37, 24)), loop = false)

    // Dedicated Animations for Wrestler Being Thrown (calibrated to move duration)
    val ThrownBodySlam = AnimationDef("thrown_body_slam", listOf(
        FrameDef(22, 12),
        FrameDef(23, 14),
        FrameDef(82, 12),
        FrameDef(24, 12),
        FrameDef(49, 10)
    ), loop = false)
    val ThrownVerticalSuplex = AnimationDef("thrown_vertical_suplex", listOf(
        FrameDef(29, 12),
        FrameDef(86, 14),
        FrameDef(30, 14),
        FrameDef(31, 12),
        FrameDef(49, 12)
    ), loop = false)
    val ThrownPiledriver = AnimationDef("thrown_piledriver", listOf(
        FrameDef(29, 12),
        FrameDef(84, 14),
        FrameDef(30, 16),
        FrameDef(41, 18)
    ), loop = false)
    val ThrownBackSuplex = AnimationDef("thrown_back_suplex", listOf(
        FrameDef(29, 12),
        FrameDef(64, 14),
        FrameDef(65, 16),
        FrameDef(49, 18)
    ), loop = false)
    val ThrownIrishWhip = AnimationDef("thrown_irish_whip", listOf(FrameDef(66, 16), FrameDef(67, 24)), loop = false)
    val KickOutBridge = AnimationDef("kick_out_bridge", listOf(FrameDef(65, 8), FrameDef(49, 12)), loop = false)
    val ThrownKickOut = AnimationDef("thrown_kick_out", listOf(FrameDef(23, 8), FrameDef(51, 6), FrameDef(52, 6)), loop = false)

    val Clothesline = AnimationDef("clothesline", listOf(FrameDef(42, 8), FrameDef(43, 16)), loop = false)
    val Dropkick = AnimationDef("dropkick", listOf(
        FrameDef(44, 6),
        FrameDef(45, 12),
        FrameDef(56, 6),
        FrameDef(57, 6)
    ), loop = false)

    val Flinch = AnimationDef("flinch", listOf(FrameDef(51, 6), FrameDef(52, 6), FrameDef(53, 6)), loop = false)
    val FlyingCrossChop = AnimationDef("flying_cross_chop", listOf(FrameDef(54, 6), FrameDef(55, 6)), loop = true)
    val RopeRebound = AnimationDef("rope_rebound", listOf(FrameDef(79, 6), FrameDef(80, 6), FrameDef(8, 4), FrameDef(9, 4)), loop = false)

    val Stunned = AnimationDef("stunned", listOf(FrameDef(46, 8)), loop = true)
    val Downed = AnimationDef("downed", listOf(FrameDef(49, 60)), loop = true)

    val ClimbTurnbuckle = AnimationDef("climb_turnbuckle", listOf(FrameDef(58, 8), FrameDef(59, 8), FrameDef(60, 8)), loop = false)
    val OnTurnbuckle = AnimationDef("on_turnbuckle", listOf(FrameDef(61, 60)), loop = true)
    val DivingSplash = AnimationDef("diving_splash", listOf(FrameDef(62, 8), FrameDef(63, 22)), loop = false)

    // Ground Pickup Animations (attacker reaches low and pulls opponent up; defender rises from mat into lockup)
    val GroundPickupAttacker = AnimationDef(
        "ground_pickup_attacker",
        listOf(FrameDef(68, 18), FrameDef(69, 18), FrameDef(70, 16)),
        loop = false
    )
    val GroundPickupDefender = AnimationDef(
        "ground_pickup_defender",
        listOf(FrameDef(75, 14), FrameDef(76, 14), FrameDef(77, 14), FrameDef(78, 10)),
        loop = false
    )

    // Character Signatures (calibrated to 60 frames = 1 second)
    val Enzuigiri = AnimationDef("enzuigiri", listOf(FrameDef(71, 15), FrameDef(72, 15), FrameDef(73, 15), FrameDef(74, 15)), loop = false)
    val SomersaultKick = AnimationDef("somersault_kick", listOf(FrameDef(71, 15), FrameDef(72, 15), FrameDef(73, 15), FrameDef(74, 15)), loop = false)
    val MongolianChop = AnimationDef("mongolian_chop", listOf(FrameDef(71, 8), FrameDef(72, 8), FrameDef(73, 8)), loop = false)
    val IronClaw = AnimationDef("iron_claw", listOf(FrameDef(71, 15), FrameDef(72, 15), FrameDef(73, 15), FrameDef(74, 15)), loop = false)
    val PiranhaBite = AnimationDef("piranha_bite", listOf(FrameDef(71, 15), FrameDef(72, 15), FrameDef(73, 15), FrameDef(74, 15)), loop = false)
    val Backbreaker = AnimationDef("backbreaker", listOf(FrameDef(71, 15), FrameDef(72, 15), FrameDef(73, 15), FrameDef(74, 15)), loop = false)

    fun forMove(moveId: String): AnimationDef = when (moveId) {
        "ground_pickup" -> GroundPickupAttacker
        "punch", "headbutt" -> Punch
        "kick", "karate_kick", "knee_lift" -> Kick
        "clothesline" -> Clothesline
        "flying_cross_chop" -> FlyingCrossChop
        "dropkick" -> Dropkick
        "body_slam" -> BodySlam
        "piledriver" -> Piledriver
        "brainbuster", "vertical_suplex" -> Brainbuster
        "back_suplex" -> BackSuplex
        "irish_whip" -> IrishWhip
        "kick_out" -> KickOutBridge
        "enzuigiri" -> Enzuigiri
        "somersault_kick" -> SomersaultKick
        "mongolian_chop" -> MongolianChop
        "iron_claw" -> IronClaw
        "piranha_bite", "outlaw_choke" -> PiranhaBite
        "backbreaker" -> Backbreaker
        "diving_splash", "double_knee_drop" -> DivingSplash
        else -> BodySlam
    }

    fun forThrown(moveId: String): AnimationDef = when (moveId) {
        "ground_pickup" -> GroundPickupDefender
        "body_slam" -> ThrownBodySlam
        "brainbuster", "vertical_suplex" -> ThrownVerticalSuplex
        "piledriver" -> ThrownPiledriver
        "back_suplex" -> ThrownBackSuplex
        "irish_whip" -> ThrownIrishWhip
        "kick_out" -> ThrownKickOut
        else -> ThrownBodySlam
    }
}
