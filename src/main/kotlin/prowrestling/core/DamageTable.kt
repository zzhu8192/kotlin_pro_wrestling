package prowrestling.core

data class MoveInfo(
    val id: String,
    val name: String,
    val damage: Int,
    val stunFrames: Int,
    val knocksDown: Boolean,
    val range: Float,
    val animationFrames: Int,
    val isSignature: Boolean = false
)

object DamageTable {
    // Strikes
    val Punch: MoveInfo = MoveInfo("punch", "Punch", damage = 6, stunFrames = 24, knocksDown = false, range = 28f, animationFrames = 22)
    val Kick: MoveInfo = MoveInfo("kick", "Kick", damage = 14, stunFrames = 90, knocksDown = true, range = 36f, animationFrames = 30)
    val RunningClothesline: MoveInfo = MoveInfo("clothesline", "Running Clothesline", damage = 22, stunFrames = 95, knocksDown = true, range = 52f, animationFrames = 24)
    val RunningDropkick: MoveInfo = MoveInfo("dropkick", "Running Dropkick", damage = 31, stunFrames = 110, knocksDown = true, range = 42f, animationFrames = 28)
    val RunningKneeLift: MoveInfo = MoveInfo("knee_lift", "Running Knee Lift", damage = 24, stunFrames = 95, knocksDown = true, range = 36f, animationFrames = 24)
    val KarateKick: MoveInfo = MoveInfo("karate_kick", "Karate Kick", damage = 18, stunFrames = 90, knocksDown = true, range = 38f, animationFrames = 30)

    // Grapple Moves (calibrated to ~1 second with 4 distinct discernible frames)
    val BodySlam: MoveInfo = MoveInfo("body_slam", "Body Slam", damage = 18, stunFrames = 100, knocksDown = true, range = 26f, animationFrames = 60)
    val IrishWhip: MoveInfo = MoveInfo("irish_whip", "Irish Whip", damage = 0, stunFrames = 0, knocksDown = false, range = 26f, animationFrames = 40)
    val VerticalSuplex: MoveInfo = MoveInfo("brainbuster", "Vertical Suplex", damage = 28, stunFrames = 120, knocksDown = true, range = 26f, animationFrames = 64)
    val BackSuplex: MoveInfo = MoveInfo("back_suplex", "Belly-to-Back Suplex", damage = 24, stunFrames = 110, knocksDown = true, range = 26f, animationFrames = 60)
    val Piledriver: MoveInfo = MoveInfo("piledriver", "Piledriver", damage = 32, stunFrames = 130, knocksDown = true, range = 26f, animationFrames = 60)

    // Character Signature Moves (for all 7 wrestlers, ~1 second duration)
    val Enzuigiri: MoveInfo = MoveInfo("enzuigiri", "Back Brain Kick", damage = 30, stunFrames = 130, knocksDown = true, range = 26f, animationFrames = 60, isSignature = true)
    val SomersaultKick: MoveInfo = MoveInfo("somersault_kick", "Somersault Kick", damage = 28, stunFrames = 125, knocksDown = true, range = 26f, animationFrames = 60, isSignature = true)
    val MongolianChop: MoveInfo = MoveInfo("mongolian_chop", "Mongolian Chop", damage = 18, stunFrames = 28, knocksDown = false, range = 28f, animationFrames = 24, isSignature = true)
    val IronClaw: MoveInfo = MoveInfo("iron_claw", "Iron Claw", damage = 32, stunFrames = 135, knocksDown = true, range = 26f, animationFrames = 60, isSignature = true)
    val PiranhaBite: MoveInfo = MoveInfo("piranha_bite", "Piranha Bite", damage = 28, stunFrames = 125, knocksDown = true, range = 26f, animationFrames = 60, isSignature = true)
    val Backbreaker: MoveInfo = MoveInfo("backbreaker", "Slender Bender Backbreaker", damage = 36, stunFrames = 140, knocksDown = true, range = 26f, animationFrames = 60, isSignature = true)
    val Headbutt: MoveInfo = MoveInfo("headbutt", "Headbutt", damage = 24, stunFrames = 115, knocksDown = true, range = 26f, animationFrames = 60, isSignature = true)
    val OutlawChoke: MoveInfo = MoveInfo("outlaw_choke", "Outlaw Choke", damage = 30, stunFrames = 130, knocksDown = true, range = 26f, animationFrames = 60, isSignature = true)
    val FlyingCrossChop: MoveInfo = MoveInfo("flying_cross_chop", "Flying Cross Chop", damage = 26, stunFrames = 100, knocksDown = true, range = 40f, animationFrames = 26, isSignature = true)

    // Turnbuckle Aerial Moves
    val DivingSplash: MoveInfo = MoveInfo("diving_splash", "Flying Body Splash", damage = 48, stunFrames = 130, knocksDown = true, range = 32f, animationFrames = 30)
    val DoubleKneeDrop: MoveInfo = MoveInfo("double_knee_drop", "Double Knee Drop", damage = 42, stunFrames = 120, knocksDown = true, range = 30f, animationFrames = 28)
    val DiveCrash: MoveInfo = MoveInfo("dive_crash", "Missed Dive Crash", damage = 20, stunFrames = 120, knocksDown = true, range = 0f, animationFrames = 24)

    // Ground Moves
    val GroundStomp: MoveInfo = MoveInfo("ground_stomp", "Ground Stomp", damage = 29, stunFrames = 80, knocksDown = false, range = 34f, animationFrames = 24)

    fun getSignatureMove(characterId: String): MoveInfo = when (characterId.lowercase()) {
        "hayabusa" -> Enzuigiri
        "starman" -> SomersaultKick
        "kincornkarn" -> MongolianChop
        "giant_panther" -> IronClaw
        "amazon" -> PiranhaBite
        "king_slender" -> Backbreaker
        "great_puma" -> {
            // Master execution: picks from top signatures with 1.35x boss multiplier!
            val sigs = arrayOf(Backbreaker, IronClaw, Enzuigiri, SomersaultKick)
            val picked = sigs[(Math.random() * sigs.size).toInt()]
            picked.copy(name = "Puma ${picked.name}", damage = (picked.damage * 1.35f).toInt())
        }
        else -> Piledriver
    }
}
