package prowrestling

import org.junit.Assert.*
import org.junit.Test
import prowrestling.ai.AiPersonality
import prowrestling.ai.CpuController
import prowrestling.ai.DifficultyTier
import prowrestling.animation.*
import prowrestling.core.*
import prowrestling.input.*

class CoreLogicTest {

    @Test
    fun testWrestlerRosterAndData() {
        val hayabusa = WrestlerData.forId("hayabusa")
        assertEquals("Fighter Hayabusa", hayabusa.name)
        assertEquals("Back Brain Kick", hayabusa.signatureMoveName)
        assertEquals(240, hayabusa.hp)
        assertFalse(hayabusa.isMasked)

        val starman = WrestlerData.forId("starman")
        assertEquals("Star Man", starman.name)
        assertTrue(starman.isMasked)

        val puma = WrestlerData.forId("great_puma")
        assertEquals("The Great Puma", puma.name)
        assertTrue(puma.isMasked)
    }

    @Test
    fun testRingGeometryAndTurnbuckles() {
        val ring = Ring(480f, 360f)
        assertTrue(ring.turnbuckles.size == 4)
        assertTrue(ring.isInsideRopes(240f, 160f))
        assertTrue(ring.isOnMat(240f, 160f))
        assertFalse(ring.isOutsideRing(240f, 160f))
        assertTrue(ring.isOutsideRing(20f, 20f))
    }

    @Test
    fun testTournamentProgression() {
        val tourn = TournamentState("hayabusa")
        assertEquals(1, tourn.currentMatchIndex)
        assertFalse(tourn.hasVwaTitle)
        assertFalse(tourn.hasVwfTitle)

        // 5 elimination matches
        for (i in 1..5) {
            tourn.recordWin()
        }
        assertTrue(tourn.hasVwaTitle)
        assertEquals(6, tourn.currentMatchIndex)

        // 10 defense matches
        for (i in 6..15) {
            tourn.recordWin()
        }
        assertEquals(16, tourn.currentMatchIndex)
        assertEquals("great_puma", tourn.currentOpponentId)
        assertEquals(DifficultyTier.GreatPumaMaster, tourn.currentDifficultyTier)

        tourn.recordWin()
        assertTrue(tourn.hasVwfTitle)
        assertTrue(tourn.isCompleted)
    }

    @Test
    fun testDamageTableAndSignatures() {
        val hayabusaSig = DamageTable.getSignatureMove("hayabusa")
        assertEquals("enzuigiri", hayabusaSig.id)
        assertTrue(hayabusaSig.knocksDown)

        val starmanSig = DamageTable.getSignatureMove("starman")
        assertEquals("somersault_kick", starmanSig.id)

        val pumaSig = DamageTable.getSignatureMove("great_puma")
        assertTrue(pumaSig.name.startsWith("Puma "))
    }

    @Test
    fun testGameStateSimulation() {
        val p1 = Wrestler("hayabusa", isPlayer1 = true, x = 200f, y = 160f)
        val p2 = Wrestler("starman", isPlayer1 = false, x = 240f, y = 160f)
        val ring = Ring()
        val gs = GameState(p1, p2, Referee(240f, 195f), ring)

        // Simulate 60 ticks of delta = 1/60s
        for (i in 0 until 60) {
            gs.update(AttackA, null, IdleAction, null, 1f / 60f)
        }

        // Wrestlers should remain in valid states
        assertNotNull(p1.state)
        assertNotNull(p2.state)
        assertFalse(gs.isMatchOver)
    }

    @Test
    fun testCpuControllerDecisionMaking() {
        val p1 = Wrestler("hayabusa", isPlayer1 = true, x = 200f, y = 160f)
        val p2 = Wrestler("starman", isPlayer1 = false, x = 240f, y = 160f)
        val gs = GameState(p1, p2, Referee(240f, 195f), Ring())

        val cpu = CpuController(AiPersonality.forCharacter("starman"), DifficultyTier.Tier1)
        val decision = cpu.decide(p2, p1, gs)
        assertNotNull(decision.first)
    }

    @Test
    fun testStrikeCollision2DHitboxAndAlignment() {
        val attacker = Wrestler("hayabusa", isPlayer1 = true, x = 200f, y = 160f)
        attacker.facingDirection = Direction.Right
        val defender = Wrestler("starman", isPlayer1 = false, x = 224f, y = 160f)

        // When horizontally aligned (same Y plane, defender in front within reach) -> HIT!
        assertTrue(MoveResolver.canStrikeHit(attacker, defender, DamageTable.Punch))
        assertTrue(MoveResolver.canStrikeHit(attacker, defender, DamageTable.Kick))

        // When NOT horizontally aligned (Y diff = 25f > 10f maxDy) -> MISS!
        defender.y = 185f
        assertFalse(MoveResolver.canStrikeHit(attacker, defender, DamageTable.Punch))
        assertFalse(MoveResolver.canStrikeHit(attacker, defender, DamageTable.Kick))

        // When behind attacker -> MISS!
        defender.y = 160f
        defender.x = 180f
        assertFalse(MoveResolver.canStrikeHit(attacker, defender, DamageTable.Punch))
    }

    @Test
    fun testFlinchingHitReactionAndKnockdown() {
        val p1 = Wrestler("hayabusa", isPlayer1 = true, x = 200f, y = 160f)
        val p2 = Wrestler("starman", isPlayer1 = false, x = 222f, y = 160f)
        val ring = Ring()
        val gs = GameState(p1, p2, Referee(240f, 195f), ring)

        // P1 lands a kick on P2
        gs.update(AttackA, null, IdleAction, null, 1f / 60f)
        // P2 should be flinching from the impact, and will knock down
        val flinchState = p2.state
        assertTrue("P2 should be in Flinching state after taking a hit", flinchState is Flinching)
        val flinching = flinchState as Flinching
        assertTrue(flinching.willKnockDown)

        // Advance physics until flinch completes
        for (i in 0 until flinching.totalFrames) {
            PhysicsStep.step(p2, IdleAction, ring, 1f / 60f)
        }
        assertTrue("P2 should transition to Downed after flinching from knockdown kick", p2.state is Downed)
    }

    @Test
    fun testRunningSpriteDirectionalFacing() {
        // Verify RunRight and WalkRight do not have flipX = true (WrestlerRenderer handles facingDirection)
        for (f in AnimationCatalog.RunRight.frames) {
            assertFalse("RunRight frames should not be pre-flipped", f.flipX)
        }
        for (f in AnimationCatalog.WalkRight.frames) {
            assertFalse("WalkRight frames should not be pre-flipped", f.flipX)
        }

        val w = Wrestler("starman", isPlayer1 = true, x = 200f, y = 160f)
        val ring = Ring()

        // Double-tap right -> run right
        PhysicsStep.step(w, RunAction(Direction.Right), ring, 1f / 60f)
        assertEquals(Direction.Right, w.facingDirection)
        assertTrue(w.isRunning)

        // Rebound off left -> facing right
        w.state = RopeRebound(Direction.Right, framesLeft = 1)
        PhysicsStep.step(w, IdleAction, ring, 1f / 60f)
        assertEquals(Direction.Right, w.facingDirection)

        // Rebound off right -> facing left
        w.state = RopeRebound(Direction.Left, framesLeft = 1)
        PhysicsStep.step(w, IdleAction, ring, 1f / 60f)
        assertEquals(Direction.Left, w.facingDirection)
    }

    @Test
    fun testStarmanRopeReboundFlyingCrossChop() {
        val starman = Wrestler("starman", isPlayer1 = true, x = 400f, y = 160f)
        val opponent = Wrestler("hayabusa", isPlayer1 = false, x = 240f, y = 160f)
        val ring = Ring()
        val gs = GameState(starman, opponent, Referee(240f, 195f), ring)

        // When Starman finishes a rope rebound:
        starman.state = RopeRebound(Direction.Left, framesLeft = 1)
        PhysicsStep.step(starman, IdleAction, ring, 1f / 60f)

        // Starman should enter FlyingCrossChop dive tackle!
        assertTrue("Starman should enter FlyingCrossChop when rebounding off ropes", starman.state is FlyingCrossChop)
        assertEquals(Direction.Left, starman.facingDirection)

        // When other wrestlers rebound, they enter normal Running
        val hayabusa = Wrestler("hayabusa", isPlayer1 = true, x = 400f, y = 160f)
        hayabusa.state = RopeRebound(Direction.Left, framesLeft = 1)
        PhysicsStep.step(hayabusa, IdleAction, ring, 1f / 60f)
        assertTrue("Hayabusa should enter Running when rebounding off ropes", hayabusa.state is Running)

        // Test collision priority of FlyingCrossChop:
        // Position Starman close to opponent while in FlyingCrossChop
        starman.x = 260f
        starman.y = 160f
        val oppHpBefore = opponent.hp
        opponent.state = Walking(Direction.Right) // Opponent is doing an action

        // Update GameState to resolve FlyingCrossChop dive collision
        gs.update(IdleAction, null, IdleAction, null, 1f / 60f)

        // Opponent takes damage, their action is overridden into Flinching with knockdown!
        assertTrue("Opponent should take damage from FlyingCrossChop", opponent.hp < oppHpBefore)
        assertTrue("Opponent should be in Flinching state", opponent.state is Flinching)
        assertTrue("Flinch should lead to knockdown", (opponent.state as Flinching).willKnockDown)
        assertEquals("Starman should land into Idle", Idle, starman.state)
    }

    @Test
    fun testGrappleMoveResolutions() {
        val hayabusa = Wrestler("hayabusa", isPlayer1 = true)

        // 1. Body slam: B pressed with neutral, Up, or Down
        assertEquals(DamageTable.BodySlam, MoveResolver.resolveGrappleMove(hayabusa, AttackB, null))
        assertEquals(DamageTable.BodySlam, MoveResolver.resolveGrappleMove(hayabusa, AttackB, Direction.Up))
        assertEquals(DamageTable.BodySlam, MoveResolver.resolveGrappleMove(hayabusa, AttackB, Direction.Down))

        // 2. Irish whip: B pressed with Left or Right
        assertEquals(DamageTable.IrishWhip, MoveResolver.resolveGrappleMove(hayabusa, AttackB, Direction.Left))
        assertEquals(DamageTable.IrishWhip, MoveResolver.resolveGrappleMove(hayabusa, AttackB, Direction.Right))

        // 3. Belly-to-back suplex (Backdrop): A pressed with Left or Right
        assertEquals(DamageTable.BackSuplex, MoveResolver.resolveGrappleMove(hayabusa, AttackA, Direction.Left))
        assertEquals(DamageTable.BackSuplex, MoveResolver.resolveGrappleMove(hayabusa, AttackA, Direction.Right))

        // 4. Vertical suplex (Brainbuster): A pressed with Up or neutral
        assertEquals(DamageTable.VerticalSuplex, MoveResolver.resolveGrappleMove(hayabusa, AttackA, Direction.Up))
        assertEquals(DamageTable.VerticalSuplex, MoveResolver.resolveGrappleMove(hayabusa, AttackA, null))

        // 5. Piledriver: A pressed with Down
        assertEquals(DamageTable.Piledriver, MoveResolver.resolveGrappleMove(hayabusa, AttackA, Direction.Down))

        // 6. Signatures:
        // Starman Down+A -> Somersault Kick
        val starman = Wrestler("starman", isPlayer1 = true)
        assertEquals(DamageTable.SomersaultKick, MoveResolver.resolveGrappleMove(starman, AttackA, Direction.Down))

        // Giant Panther Left/Right+A -> Iron Claw, Down+A -> Headbutt
        val panther = Wrestler("giant_panther", isPlayer1 = true)
        assertEquals(DamageTable.IronClaw, MoveResolver.resolveGrappleMove(panther, AttackA, Direction.Left))
        assertEquals(DamageTable.Headbutt, MoveResolver.resolveGrappleMove(panther, AttackA, Direction.Down))

        // Amazon Left/Right+A -> Piranha Bite, Down+A -> Outlaw Choke
        val amazon = Wrestler("amazon", isPlayer1 = true)
        assertEquals(DamageTable.PiranhaBite, MoveResolver.resolveGrappleMove(amazon, AttackA, Direction.Right))
        assertEquals(DamageTable.OutlawChoke, MoveResolver.resolveGrappleMove(amazon, AttackA, Direction.Down))

        // King Slender A -> Backbreaker
        val slender = Wrestler("king_slender", isPlayer1 = true)
        assertEquals(DamageTable.Backbreaker, MoveResolver.resolveGrappleMove(slender, AttackA, null))
        assertEquals(DamageTable.Backbreaker, MoveResolver.resolveGrappleMove(slender, AttackA, Direction.Up))
    }

    @Test
    fun testGrappleSystemImmediateResolution() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 230f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 250f, y = 160f)
        val session = GrappleSystem.startSession(w1, w2)

        // When P1 presses AttackB (neutral dir) -> immediate GrappleWin with BodySlam on that frame!
        val result = GrappleSystem.updateSession(session, AttackB, null, IdleAction, null)
        assertNotNull("Grapple should resolve immediately when P1 inputs", result)
        assertTrue(result is GrappleWin)
        val win = result as GrappleWin
        assertEquals(w1, win.winner)
        assertEquals(w2, win.loser)
        assertEquals(DamageTable.BodySlam, win.move)
    }

    @Test
    fun testPhysicsStepProtectsExecutingAndBeingThrownFromMoveCancellation() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 200f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 220f, y = 160f)
        val ring = Ring()

        // Put w1 in Executing and w2 in BeingThrown
        w1.state = Executing("body_slam", currentFrame = 5, totalFrames = 60)
        w2.state = BeingThrown("body_slam", currentFrame = 5, totalFrames = 60)

        // If player holds a directional key (MoveAction), PhysicsStep should NOT cancel the move!
        PhysicsStep.step(w1, MoveAction(Direction.Up), ring, 1f / 60f)
        PhysicsStep.step(w2, MoveAction(Direction.Left), ring, 1f / 60f)

        assertTrue("w1 should remain in Executing state, not be overwritten to Walking", w1.state is Executing)
        assertTrue("w2 should remain in BeingThrown state, not be overwritten to Walking", w2.state is BeingThrown)

        // Even with RunAction, should not cancel
        PhysicsStep.step(w1, RunAction(Direction.Right), ring, 1f / 60f)
        assertTrue("w1 should remain in Executing state, not be overwritten to Running", w1.state is Executing)
    }

    @Test
    fun testIrishWhipExecutionAndRopeRebound() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 240f, y = 160f)
        w1.facingDirection = Direction.Right
        val w2 = Wrestler("starman", isPlayer1 = false, x = 255f, y = 160f)
        val ring = Ring()
        val gs = GameState(w1, w2, Referee(240f, 195f), ring)

        // Put w1 in Executing("irish_whip") and w2 in BeingThrown("irish_whip")
        val totalFrames = DamageTable.IrishWhip.animationFrames
        w1.state = Executing("irish_whip", currentFrame = 0, totalFrames = totalFrames)
        w2.state = BeingThrown("irish_whip", currentFrame = 0, totalFrames = totalFrames)

        // Simulate until Irish Whip completes
        for (f in 0..totalFrames) {
            gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        }

        // After Irish Whip finishes:
        assertEquals("Attacker should be Idle", Idle, w1.state)
        assertTrue("Defender should enter IrishWhipped running toward right ropes", w2.state is IrishWhipped)
        assertEquals(Direction.Right, (w2.state as IrishWhipped).direction)

        // Defender cannot be interrupted by MoveAction while IrishWhipped
        PhysicsStep.step(w2, MoveAction(Direction.Left), ring, 1f / 60f)
        assertTrue("Defender should remain in IrishWhipped even if MoveAction is received", w2.state is IrishWhipped)

        // Simulate until defender hits right ropes and rebounds
        while (w2.state is IrishWhipped) {
            PhysicsStep.step(w2, IdleAction, ring, 1f / 60f)
        }
        assertTrue("Defender should enter RopeRebound after hitting the ropes", w2.state is RopeRebound)
    }

    @Test
    fun testIrishWhipNearRopesThrowsTowardRingAndRebounds() {
        val ring = Ring()
        // Wrestlers near right rope (ring center is 240f, right rope is at 393f)
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 360f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 376f, y = 160f)
        val gs = GameState(w1, w2, Referee(240f, 195f), ring)

        // Start grapple
        gs.activeGrapple = GrappleSystem.startSession(w1, w2)

        // P1 executes Irish Whip (B pressed near right ropes, neutral direction defaults toward ring = Left)
        gs.update(AttackB, Direction.Left, IdleAction, null, 1f / 60f)
        assertNull("Grapple resolves into Irish Whip", gs.activeGrapple)
        assertEquals("Attacker throws toward ring (Left)", Direction.Left, w1.facingDirection)

        // Advance through throw animation
        val totalFrames = DamageTable.IrishWhip.animationFrames
        for (f in 0..totalFrames) {
            gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        }

        // Defender is now IrishWhipped toward the Left into the ring
        assertTrue("Defender is IrishWhipped", w2.state is IrishWhipped)
        assertEquals("Whip direction is Left toward ring", Direction.Left, (w2.state as IrishWhipped).direction)

        // Defender runs across ring, hits left rope and rebounds back toward right
        var rebounded = false
        for (f in 0 until 120) {
            gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
            if (w2.state is RopeRebound) {
                rebounded = true
                break
            }
        }
        assertTrue("Defender should rebound off left ropes back into ring", rebounded)
        assertTrue("Defender position remains inside ring", w2.x >= ring.innerBounds.left && w2.x <= ring.innerBounds.right)
    }

    @Test
    fun testFullGrappleExecutionToDamageAndDowned() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 230f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 248f, y = 160f)
        val ring = Ring()
        val gs = GameState(w1, w2, Referee(240f, 195f), ring)

        val w2HpBefore = w2.hp

        // 1. Walk together to trigger grapple collision
        gs.update(MoveAction(Direction.Right), Direction.Right, IdleAction, null, 1f / 60f)
        assertNotNull("Grapple session should be active", gs.activeGrapple)
        assertEquals(Grappling, w1.state)
        assertEquals(Grappling, w2.state)

        // 2. P1 presses Down + A (Piledriver)
        gs.update(AttackA, Direction.Down, IdleAction, null, 1f / 60f)
        assertNull("Active grapple should resolve", gs.activeGrapple)
        assertTrue("Attacker should enter Executing piledriver", w1.state is Executing)
        assertEquals("piledriver", (w1.state as Executing).moveId)
        assertTrue("Defender should enter BeingThrown piledriver", w2.state is BeingThrown)

        // 3. Advance execution through full animation
        for (i in 0 until 70) {
            gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        }

        // 4. Verify outcome:
        assertEquals("Attacker should return to Idle", Idle, w1.state)
        assertTrue("Defender should be Downed on canvas", w2.state is Downed)
        assertTrue("Defender took full Piledriver damage", w2.hp <= w2HpBefore - DamageTable.Piledriver.damage)
    }

    @Test
    fun testFootBaselineAndSliverShadowAlignment() {
        val wrestler = Wrestler("starman", isPlayer1 = true, x = 200f, y = 140f)
        // Verify wrestler foot bounding box is centered at wrestler.y
        assertEquals(140f, wrestler.footBounds.centerY, 0.001f)
        assertEquals(140f, wrestler.y, 0.001f)
        assertEquals(140f, wrestler.bodyBounds.y, 0.001f)

        // Verify renderer instance initializes cleanly
        val renderer = prowrestling.render.WrestlerRenderer()
        val refereeRenderer = prowrestling.render.RefereeRenderer()
        assertNotNull(renderer)
        assertNotNull(refereeRenderer)
    }

    @Test
    fun testRunningSpeedReductionAndStarmanTravelLimit() {
        val wrestler = Wrestler("starman", isPlayer1 = true, x = 200f, y = 160f)
        // Verify running speed is reduced by 25% (3.6f vs previous 4.8f)
        assertEquals(3.6f, wrestler.runSpeed, 0.001f)
        assertEquals(4.6f, wrestler.reboundSpeed, 0.001f)

        // Verify Starman's Flying Cross Chop travels at most 1/5th of the ring width
        val ring = Ring()
        val maxAllowedDistance = ring.innerBounds.width / 5f // 306 / 5 = 61.2f
        val startX = 200f
        wrestler.x = startX
        wrestler.y = 160f
        wrestler.state = FlyingCrossChop(Direction.Right, framesLeft = 18)

        while (wrestler.state is FlyingCrossChop) {
            PhysicsStep.step(wrestler, IdleAction, ring, 1f / 60f)
        }

        val actualDistance = wrestler.x - startX
        assertTrue(
            "Flying cross chop must travel <= 1/5th ring width ($maxAllowedDistance), got $actualDistance",
            actualDistance <= maxAllowedDistance + 0.1f
        )
        assertEquals(Idle, wrestler.state)
    }

    @Test
    fun testDownedDurationScalesWithDamage() {
        val w = Wrestler("hayabusa", isPlayer1 = true)

        // 1. Full health (240 HP) -> minimum stun duration (~1.5s / 90 frames)
        w.hp = 240
        assertEquals(90, w.calculateDownedFrames(90))

        // 2. Half health (120 HP) -> halfway scaled (~5.75s / 345 frames)
        w.hp = 120
        assertEquals(345, w.calculateDownedFrames(90))

        // 3. Zero health (0 HP / no energy) -> 10.0 seconds (600 frames)
        w.hp = 0
        assertEquals(600, w.calculateDownedFrames(90))
    }

    @Test
    fun testPickUpOpponentFromMatIntoGrapple() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 230f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 246f, y = 160f)
        val ring = Ring()
        val gs = GameState(w1, w2, Referee(240f, 195f), ring)

        // Put opponent down on the mat
        w2.state = Downed(framesLeft = 300)
        assertTrue(w2.isGrounded)

        // P1 presses B near downed opponent -> initiates ground pickup animation!
        gs.update(AttackB, null, IdleAction, null, 1f / 60f)

        assertNull("Grapple session should NOT be active during pickup animation", gs.activeGrapple)
        assertTrue("Attacker should be in ground_pickup Executing state", w1.state is Executing && (w1.state as Executing).moveId == "ground_pickup")
        assertTrue("Defender should be in ground_pickup BeingThrown state", w2.state is BeingThrown && (w2.state as BeingThrown).moveId == "ground_pickup")
        assertFalse("Defender is no longer grounded", w2.isGrounded)

        // While pickup animation is playing, pressing B a second time MUST NOT buffer or trigger Body Slam!
        gs.update(AttackB, null, IdleAction, null, 1f / 60f)
        assertNull("Additional inputs during pickup MUST NOT start grapple or buffer move", gs.activeGrapple)
        assertEquals("ground_pickup", (w1.state as Executing).moveId)

        // Advance through the remaining pickup animation frames while mashing B
        var pickupFrameCount = 0
        while (w1.state is Executing && (w1.state as Executing).moveId == "ground_pickup") {
            gs.update(AttackB, null, IdleAction, null, 1f / 60f) // mashing B during pickup
            pickupFrameCount++
        }
        assertTrue("Pickup animation should last roughly 50-52 frames", pickupFrameCount in 50..52)

        // Now full animation is completed -> transitioned into active grapple!
        assertNotNull("Grapple session should now be active after pickup animation completes", gs.activeGrapple)
        assertEquals("Attacker should be Grappling", Grappling, w1.state)
        assertEquals("Defender should be pulled up into Grappling", Grappling, w2.state)

        // Now in fully grappled mode: P1 presses AttackB -> executes Body Slam cleanly!
        gs.update(AttackB, null, IdleAction, null, 1f / 60f)
        assertNull("Grapple should resolve", gs.activeGrapple)
        assertTrue("W1 executes body slam", w1.state is Executing)
        assertEquals("body_slam", (w1.state as Executing).moveId)
        assertTrue("W2 is being thrown", w2.state is BeingThrown)
        assertEquals("body_slam", (w2.state as BeingThrown).moveId)
    }

    @Test
    fun testPinKickOutWithEnergySinglePress() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 240f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 240f, y = 160f)
        w2.hp = 180 // Healthy (> 25% energy)
        val ring = Ring()
        val gs = GameState(w1, w2, Referee(240f, 195f), ring)

        // Initiate pin: W1 pins W2
        val pin = gs.rules.initiatePin(w1, w2)
        assertEquals(1, pin.requiredPresses)
        assertEquals(Pinning, w1.state)
        assertTrue(w2.state is BeingPinned)

        // A single press from defender W2 immediately initiates the multi-frame kickout
        gs.update(IdleAction, null, AttackA, null, 1f / 60f)

        assertNull("Pin should be broken after 1 press", gs.rules.activePin)
        assertTrue("Defender executes kickout motion", w2.state is Executing && (w2.state as Executing).moveId == "kick_out")
        assertTrue("Attacker is thrown off via BeingThrown", w1.state is BeingThrown && (w1.state as BeingThrown).moveId == "kick_out")

        // Advance frames through the kickout motion
        val initialAttackerX = w1.x
        while (w2.state is Executing && (w2.state as Executing).moveId == "kick_out") {
            gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        }

        assertNotEquals("Attacker position moved during kickout", initialAttackerX, w1.x, 0.1f)
        assertTrue("Defender is Downed briefly after completing kickout", w2.state is Downed)
        assertTrue("Attacker is Stunned after landing from kickout", w1.state is Stunned)
    }

    @Test
    fun testPinKickOutWithLowEnergyTwoToThreePresses() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 240f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 240f, y = 160f)
        val ring = Ring()
        val gs = GameState(w1, w2, Referee(240f, 195f), ring)

        // 1. Defender has 45 HP (< 25% energy, >= 30 HP) -> 2 presses needed
        w2.hp = 45
        var pin = gs.rules.initiatePin(w1, w2)
        assertEquals(2, pin.requiredPresses)

        // Press 1
        gs.update(IdleAction, null, AttackA, null, 1f / 60f)
        assertNotNull("Still pinned after 1 press", gs.rules.activePin)
        assertEquals(1, pin.pressCount)

        // Holding button should NOT increment press count (edge detector)
        gs.update(IdleAction, null, AttackA, null, 1f / 60f)
        assertEquals("Holding button does not increment press count", 1, pin.pressCount)
        assertNotNull("Still pinned while holding", gs.rules.activePin)

        // Release button
        gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        assertEquals(1, pin.pressCount)

        // Press 2 -> Kicks out!
        gs.update(IdleAction, null, AttackA, null, 1f / 60f)
        assertNull("Pin broken after 2nd press", gs.rules.activePin)
        assertTrue("Defender enters kickout motion", w2.state is Executing && (w2.state as Executing).moveId == "kick_out")
        assertTrue("Attacker enters BeingThrown kickout", w1.state is BeingThrown && (w1.state as BeingThrown).moveId == "kick_out")

        // 2. Defender has 15 HP (< 12.5% energy) -> 3 presses needed
        w2.hp = 15
        pin = gs.rules.initiatePin(w1, w2)
        assertEquals(3, pin.requiredPresses)

        // Press 1
        gs.update(IdleAction, null, AttackA, null, 1f / 60f)
        gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        assertEquals(1, pin.pressCount)
        assertNotNull(gs.rules.activePin)

        // Press 2
        gs.update(IdleAction, null, AttackB, null, 1f / 60f)
        gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        assertEquals(2, pin.pressCount)
        assertNotNull(gs.rules.activePin)

        // Press 3 -> Kicks out!
        gs.update(IdleAction, null, AttackA, null, 1f / 60f)
        assertNull("Pin broken after 3rd press", gs.rules.activePin)
        assertTrue("Defender enters kickout motion", w2.state is Executing && (w2.state as Executing).moveId == "kick_out")
        assertTrue("Attacker enters BeingThrown kickout", w1.state is BeingThrown && (w1.state as BeingThrown).moveId == "kick_out")
    }

    @Test
    fun testPinZeroEnergyCannotKickOut() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 240f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 240f, y = 160f)
        w2.hp = 0 // Zero energy
        val referee = Referee(240f, 160f) // Ref already in position
        referee.state = RefereeCounting(0, 0)
        val ring = Ring()
        val gs = GameState(w1, w2, referee, ring)

        val pin = gs.rules.initiatePin(w1, w2)
        assertEquals(999, pin.requiredPresses)

        // Mash buttons repeatedly
        for (i in 1..10) {
            gs.update(IdleAction, null, AttackA, null, 1f / 60f)
            gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        }
        assertNotNull("Cannot kick out at 0 energy", gs.rules.activePin)

        // Simulate referee counting to 3
        referee.state = RefereeCounting(2, referee.countDurationFrames)
        gs.update(IdleAction, null, IdleAction, null, 1f / 60f)

        assertTrue("Match is won by pinfall", gs.rules.matchResult is MatchWonByPin)
        val won = gs.rules.matchResult as MatchWonByPin
        assertEquals(w1, won.winner)
        assertEquals(w2, won.loser)
    }

    @Test
    fun testDoubleTapDetectorOnlyLeftRightAndRequiresRelease() {
        val detector = prowrestling.input.DoubleTapDetector(windowSeconds = 0.22f)

        // 1. Single press and hold (or OS auto-repeat): should NEVER initiate running
        assertFalse("First press should not trigger running", detector.onDirectionJustPressed(Direction.Right))
        assertFalse("Already held: repeat press without release should not trigger running", detector.onDirectionJustPressed(Direction.Right))
        assertFalse("Already held: repeat press without release should not trigger running", detector.onDirectionJustPressed(Direction.Right))
        detector.update(0.10f)
        assertFalse("Still held: repeat press should not trigger running", detector.onDirectionJustPressed(Direction.Right))
        detector.update(0.20f) // Timer expired while held
        assertFalse("After timeout while held, still should not run", detector.currentlyRunning)

        // 2. Legitimate Double-Tap Right (press, release, press again within window)
        detector.reset()
        assertFalse("First tap press", detector.onDirectionJustPressed(Direction.Right))
        detector.onDirectionReleased(Direction.Right)
        detector.update(0.05f) // 50ms later
        assertTrue("Second tap press within window MUST initiate running", detector.onDirectionJustPressed(Direction.Right))
        assertTrue("Currently running", detector.currentlyRunning)

        detector.onDirectionReleased(Direction.Right)
        assertFalse("Releasing key stops running", detector.currentlyRunning)

        // 3. Legitimate Double-Tap Left
        detector.reset()
        assertFalse("First tap Left", detector.onDirectionJustPressed(Direction.Left))
        detector.onDirectionReleased(Direction.Left)
        detector.update(0.05f)
        assertTrue("Second tap Left MUST initiate running", detector.onDirectionJustPressed(Direction.Left))
        assertTrue("Currently running Left", detector.currentlyRunning)

        // 4. Double-tap Up or Down should NEVER initiate running
        detector.reset()
        assertFalse("Up press 1", detector.onDirectionJustPressed(Direction.Up))
        detector.onDirectionReleased(Direction.Up)
        assertFalse("Up press 2 - should NEVER run up", detector.onDirectionJustPressed(Direction.Up))
        assertFalse(detector.currentlyRunning)

        detector.reset()
        assertFalse("Down press 1", detector.onDirectionJustPressed(Direction.Down))
        detector.onDirectionReleased(Direction.Down)
        assertFalse("Down press 2 - should NEVER run down", detector.onDirectionJustPressed(Direction.Down))
        assertFalse(detector.currentlyRunning)
    }

    @Test
    fun testPhysicsStepRunningOnlyLeftRight() {
        val ring = Ring()
        val w = Wrestler("starman", isPlayer1 = true, x = 240f, y = 160f)

        // RunAction with Right -> Running
        PhysicsStep.step(w, RunAction(Direction.Right), ring, 1f / 60f)
        assertEquals(Direction.Right, w.facingDirection)
        assertTrue("Wrestler is running Right", w.state is Running && (w.state as Running).direction == Direction.Right)

        // RunAction with Left -> Running
        PhysicsStep.step(w, RunAction(Direction.Left), ring, 1f / 60f)
        assertEquals(Direction.Left, w.facingDirection)
        assertTrue("Wrestler is running Left", w.state is Running && (w.state as Running).direction == Direction.Left)

        // RunAction with Up -> Must NOT run! Should walk instead
        w.state = Idle
        PhysicsStep.step(w, RunAction(Direction.Up), ring, 1f / 60f)
        assertFalse("Wrestler must NOT run Up", w.state is Running)
        assertTrue("Wrestler walks instead of running Up", w.state is Walking)

        // RunAction with Down -> Must NOT run!
        w.state = Idle
        PhysicsStep.step(w, RunAction(Direction.Down), ring, 1f / 60f)
        assertFalse("Wrestler must NOT run Down", w.state is Running)
        assertTrue("Wrestler walks instead of running Down", w.state is Walking)
    }

    @Test
    fun testRunningClotheslineTriggeringAndResolution() {
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 200f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 250f, y = 160f)

        // 1. In neutral (neither running nor opponent whipped): AttackA resolves standing kick, NOT clothesline!
        val neutralKick = MoveResolver.resolveStrike(w1, w2, AttackA, Direction.Right)
        assertNotNull(neutralKick)
        assertEquals(DamageTable.Enzuigiri, neutralKick)

        // 2. When attacker is running: AttackA resolves RunningClothesline!
        w1.state = Running(Direction.Right)
        val runningMove = MoveResolver.resolveStrike(w1, w2, AttackA, Direction.Right)
        assertNotNull(runningMove)
        assertEquals(DamageTable.RunningClothesline, runningMove)

        // 3. When opponent is in IrishWhipped: holding direction + AttackA resolves RunningClothesline!
        w1.state = Idle
        w2.state = IrishWhipped(Direction.Right)
        val moveWhipped = MoveResolver.resolveStrike(w1, w2, AttackA, Direction.Right)
        assertNotNull(moveWhipped)
        assertEquals(DamageTable.RunningClothesline, moveWhipped)

        // 4. When opponent is in RopeRebound: AttackA resolves RunningClothesline!
        w2.state = RopeRebound(Direction.Left, framesLeft = 14)
        val moveRebound = MoveResolver.resolveStrike(w1, w2, AttackA, null)
        assertNotNull(moveRebound)
        assertEquals(DamageTable.RunningClothesline, moveRebound)
    }

    @Test
    fun testIrishWhippedCannotBeBrokenOutAndHasOneBounceLimit() {
        val ring = Ring()
        val w1 = Wrestler("hayabusa", isPlayer1 = true, x = 200f, y = 160f)
        val w2 = Wrestler("starman", isPlayer1 = false, x = 240f, y = 160f)
        val gs = GameState(w1, w2, Referee(240f, 195f), ring)
        val cpu = prowrestling.ai.CpuController(prowrestling.ai.AiPersonality.forCharacter("starman"))

        // Put defender in IrishWhipped running Right towards right rope (rope at 393f)
        w2.state = IrishWhipped(Direction.Right)

        // 1. Defender cannot be broken out of run by MoveAction
        PhysicsStep.step(w2, MoveAction(Direction.Left), ring, 1f / 60f)
        assertTrue("w2 must remain in IrishWhipped despite MoveAction", w2.state is IrishWhipped)

        // 2. CPU controller cannot break out of IrishWhipped
        val cpuDecision = cpu.decide(w2, w1, gs)
        assertEquals("CPU must return IdleAction during IrishWhipped", IdleAction, cpuDecision.first)

        // 3. Simulate until w2 reaches right ropes and rebounds (guaranteed 1st bounce)
        var hitRope = false
        for (i in 0 until 60) {
            PhysicsStep.step(w2, IdleAction, ring, 1f / 60f)
            if (w2.state is RopeRebound) {
                hitRope = true
                assertTrue("Rope rebound must be marked as Irish whipped", (w2.state as RopeRebound).isIrishWhipped)
                break
            }
        }
        assertTrue("Defender must hit rope and rebound", hitRope)

        // 4. Advance through rope rebound (14 frames)
        while (w2.state is RopeRebound) {
            PhysicsStep.step(w2, IdleAction, ring, 1f / 60f)
        }

        // Post-rebound state must be IrishWhipped with hasBounced = true
        assertTrue("Post-rebound state must be IrishWhipped", w2.state is IrishWhipped)
        val whippedAfterRebound = w2.state as IrishWhipped
        assertTrue("hasBounced must be true after 1st rope bounce", whippedAfterRebound.hasBounced)
        assertEquals("Direction must be Left towards opposite rope", Direction.Left, whippedAfterRebound.direction)

        // 5. Defender STILL cannot be broken out by MoveAction or CPU
        PhysicsStep.step(w2, MoveAction(Direction.Right), ring, 1f / 60f)
        assertTrue("Defender must still remain in IrishWhipped", w2.state is IrishWhipped)
        val cpuDecisionPost = cpu.decide(w2, w1, gs)
        assertEquals("CPU must return IdleAction during post-bounce IrishWhipped", IdleAction, cpuDecisionPost.first)

        // 6. Simulate run across the entire ring towards left rope (inner left = 87f)
        var naturalStop = false
        for (i in 0 until 180) {
            PhysicsStep.step(w2, IdleAction, ring, 1f / 60f)
            if (w2.state == Idle) {
                naturalStop = true
                break
            }
            // Must NEVER bounce a second time!
            assertFalse("Defender must NEVER bounce off the second rope!", w2.state is RopeRebound)
        }

        assertTrue("Defender must naturally stop into Idle before hitting second rope", naturalStop)
        val halfW = w2.width / 2f
        val distToLeftRope = (w2.x - halfW) - ring.innerBounds.left
        assertTrue("Stop position must be near rope without penetrating (dist: $distToLeftRope)", distToLeftRope >= 0f && distToLeftRope <= 26f)
    }

    @Test
    fun testVoluntaryRunningHasOneBounceLimitAndStopsNaturally() {
        val ring = Ring()
        val w = Wrestler("hayabusa", isPlayer1 = true, x = 320f, y = 160f)

        // Wrestler double-taps to run Right
        w.state = Running(Direction.Right)

        // Runs Right and hits right rope
        var hitRope = false
        for (i in 0 until 30) {
            PhysicsStep.step(w, IdleAction, ring, 1f / 60f)
            if (w.state is RopeRebound) {
                hitRope = true
                break
            }
        }
        assertTrue("Wrestler must hit rope and rebound", hitRope)

        // Finish rebound frames
        while (w.state is RopeRebound) {
            PhysicsStep.step(w, IdleAction, ring, 1f / 60f)
        }

        assertTrue("Wrestler must enter Running with hasBounced = true", w.state is Running && (w.state as Running).hasBounced)

        // Runs Left across ring and naturally stops near left rope without bouncing
        var naturalStop = false
        for (i in 0 until 180) {
            PhysicsStep.step(w, IdleAction, ring, 1f / 60f)
            if (w.state == Idle) {
                naturalStop = true
                break
            }
            assertFalse("Wrestler must NOT bounce off the second rope!", w.state is RopeRebound)
        }

        assertTrue("Wrestler must naturally stop into Idle near opposite rope", naturalStop)
    }

    @Test
    fun testRunningClotheslineConnectsAndDownsWhippedOpponent() {
        val gs = GameState()
        val w1 = gs.wrestler1
        val w2 = gs.wrestler2
        val initialHp = w2.hp

        // Position wrestlers: w1 on left (200f), w2 rebounding from right ropes towards left (280f)
        w1.x = 200f
        w1.y = 160f
        w1.facingDirection = Direction.Right
        w1.state = Idle

        w2.x = 280f
        w2.y = 160f
        w2.facingDirection = Direction.Left
        w2.state = RopeRebound(Direction.Left, framesLeft = 14)

        // Player 1 holds Right and presses AttackA
        gs.update(AttackA, Direction.Right, IdleAction, null, 1f / 60f)

        // w1 enters Executing("clothesline")
        assertTrue("Attacker should enter clothesline execution", w1.state is Executing && (w1.state as Executing).moveId == "clothesline")
        assertEquals(Direction.Right, w1.facingDirection)

        // Simulate frame steps as attacker charges right and defender charges left into each other
        var hitOccurred = false
        for (i in 0 until 20) {
            gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
            if (w2.state is Flinching && (w2.state as Flinching).willKnockDown) {
                hitOccurred = true
                break
            }
        }

        assertTrue("Clothesline must connect and knock down the running opponent", hitOccurred)
        assertTrue("Opponent HP must be reduced by clothesline damage", w2.hp < initialHp)
        assertEquals(initialHp - DamageTable.RunningClothesline.damage, w2.hp)

        // Advance frames until flinching completes -> opponent is downed on the mat
        for (i in 0 until 20) {
            gs.update(IdleAction, null, IdleAction, null, 1f / 60f)
        }
        assertTrue("Opponent must end up Downed on the mat", w2.state is Downed)
    }
}

