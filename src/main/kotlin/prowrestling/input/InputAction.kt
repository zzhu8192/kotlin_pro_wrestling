package prowrestling.input

import prowrestling.core.Direction

sealed interface InputAction

data class MoveAction(val dir: Direction) : InputAction
data class RunAction(val dir: Direction) : InputAction
data object AttackA : InputAction // Kick / Suplex / Pin
data object AttackB : InputAction // Punch / Body Slam / Stomp
data object StartAction : InputAction
data object SelectAction : InputAction
data object IdleAction : InputAction
