package `in`.dragonbra.vapulla.anim

import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionSet

class AutoParallelTransition : TransitionSet() {
    init {
        ordering = ORDERING_TOGETHER
        addTransition(Fade(Fade.OUT))
                .addTransition(ChangeBounds())
                .addTransition(Fade(Fade.IN))
    }
}
