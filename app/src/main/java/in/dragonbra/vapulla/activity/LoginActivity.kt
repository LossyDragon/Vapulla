package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.javasteam.util.Strings
import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.anim.AutoParallelTransition
import `in`.dragonbra.vapulla.anim.TransitionListener
import `in`.dragonbra.vapulla.anim.VectorAnimCompat
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.LoginView
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import `in`.dragonbra.vapulla.databinding.ActivityLoginBinding
import `in`.dragonbra.vapulla.util.clearTask
import `in`.dragonbra.vapulla.util.info
import `in`.dragonbra.vapulla.util.intentFor
import `in`.dragonbra.vapulla.util.newTask
import javax.inject.Inject


class LoginActivity : VapullaBaseActivity<LoginView, LoginPresenter>(), LoginView {

    @Inject
    lateinit var loginPresenter: LoginPresenter

    lateinit var handler: Handler

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        vapulla().graph.inject(this)
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler = Handler()

        binding.loginLoadingText.loadingText.setFactory(TextSwitcher.factory(this))
        binding.loginLoadingText.loadingText.inAnimation =
            AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        binding.loginLoadingText.loadingText.outAnimation =
            AnimationUtils.loadAnimation(this, android.R.anim.fade_out)

        binding.loginButton.login.click { login() }

        binding.loginSteamGuardButton.steamGuardButton.click {
            val code = binding.loginSteamGuard.steamGuardInput.text.toString()

            if (code.length < 5) {
                binding.loginSteamGuard.steamGuardLayout.error =
                    getString(R.string.editTextErrorSteamGuard)
                return@click
            }

            Utils.hideKeyboardFrom(this@LoginActivity, it)

            val transition = AutoParallelTransition()
            transition.addListener(object : TransitionListener() {
                override fun onTransitionEnd(transition: Transition) {
                    startLoadingAnimation()
                    presenter.login(code)
                }
            })

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_loading)
            TransitionManager.beginDelayedTransition(binding.rootLayout, transition)
            constraintSet.applyTo(binding.rootLayout)
        }

        binding.loginRetryButton.retryButton.click {
            binding.loginLoadingText.loadingText.setText(null)

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_loading)
            TransitionManager.beginDelayedTransition(binding.rootLayout, AutoParallelTransition())
            constraintSet.applyTo(binding.rootLayout)

            val faceAnim = getDrawable(R.drawable.animated_vapulla_from_face)
            binding.loginLogoMiddle.vapullaLogoBottom.setImageDrawable(faceAnim)

            VectorAnimCompat.registerAnimationCallback(
                faceAnim as Animatable,
                object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable) {
                        binding.loginLogoMiddle.vapullaLogoTop.setImageDrawable(getDrawable(R.drawable.vapulla_top))
                        binding.loginLogoMiddle.vapullaLogoBottom.show()
                        binding.loginLogoMiddle.vapullaLogoMiddle.show()
                        startLoadingAnimation()
                        presenter.retry()
                    }
                })

            faceAnim.start()
        }

        binding.loginSteamGuardButton.steamGuardButton.click {
            presenter.cancelSteamGuard()
        }

        binding.loginUsername.username.bindLayout()
        binding.loginPassword.password.bindLayout()
        binding.loginSteamGuard.steamGuardInput.bindLayout()
    }

    override fun createPresenter(): LoginPresenter = loginPresenter

    override fun showLoading(text: String) {
        runOnUiThread {
            binding.loginLoadingText.loadingText.setText(text)
        }
    }

    override fun startLoading(finishedAction: (() -> Unit)?) {
        val transition = AutoParallelTransition()
        /*transition.addListener(object : TransitionListener() {
            override fun onTransitionEnd(transition: Transition) {
                startLoadingAnimation()
                finishedAction?.invoke()
            }
        })*/

        val constraintSet = ConstraintSet()
        constraintSet.clone(this, R.layout.activity_login_frame_loading)
        TransitionManager.beginDelayedTransition(binding.rootLayout, transition)
        constraintSet.applyTo(binding.rootLayout)

        startLoadingAnimation()
        finishedAction?.invoke()
    }

    override fun loginSuccess() {
        startActivity(intentFor<HomeActivity>().newTask().clearTask())
        finish()
    }

    override fun showSteamGuard(is2Fa: Boolean, errorMessage: String?) {
        runOnUiThread {
            stopLoadingAnimation()
            binding.loginLoadingText.loadingText.setText("")

            errorMessage?.let {
                binding.loginErrorText.errorText.text = errorMessage
            }

            binding.loginLoadingText.loadingText.setText(
                if (is2Fa) getString(R.string.loadingTextSteamGuardMobile) else
                    getString(R.string.loadingTextSteamGuardEmail)
            )

            val layout = errorMessage?.let { R.layout.activity_login_frame_steamguard_error }
                ?: run { R.layout.activity_login_frame_steamguard }

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, layout)
            TransitionManager.beginDelayedTransition(binding.rootLayout, AutoParallelTransition())
            constraintSet.applyTo(binding.rootLayout)
        }
    }

    override fun showLoginForm(errorMessage: String?) {
        info("showLoginForm 1")
        runOnUiThread {
            info("showLoginForm 2")
            stopLoadingAnimation()
            binding.loginLoadingText.loadingText.setText("")
            info("showLoginForm 3")

            errorMessage?.let {
                info("showLoginForm 4")
                binding.loginErrorText.errorText.text = errorMessage
            }

            info("showLoginForm 5")
            val layout = errorMessage?.let { R.layout.activity_login_frame_form_error }
                ?: run { R.layout.activity_login_frame_form }

            info("showLoginForm 6")
            val constraintSet = ConstraintSet()
            constraintSet.clone(this, layout)
            TransitionManager.beginDelayedTransition(binding.rootLayout, AutoParallelTransition())
            constraintSet.applyTo(binding.rootLayout)
        }
    }

    override fun showFailedScreen() {
        runOnUiThread {
            stopLoadingAnimation()

            binding.loginLogoMiddle.vapullaLogoBottom.invisible()
            binding.loginLogoMiddle.vapullaLogoMiddle.invisible()

            val faceAnim = getDrawable(R.drawable.animated_vapulla_to_face)
            binding.loginLogoMiddle.vapullaLogoTop.setImageDrawable(faceAnim)
            (faceAnim as Animatable).start()

            binding.loginLoadingText.loadingText.setText(getString(R.string.loadingTextFailed))

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_failed)
            TransitionManager.beginDelayedTransition(binding.rootLayout, AutoParallelTransition())
            constraintSet.applyTo(binding.rootLayout)
        }
    }

    fun login() {
        val username = binding.loginUsername.username.text.toString()

        if (Strings.isNullOrEmpty(username)) {
            binding.loginUsername.usernameLayout.error = getString(R.string.editTextErrorUsername)
            return
        }

        val password = binding.loginPassword.password.text.toString()

        if (Strings.isNullOrEmpty(password)) {
            binding.loginPassword.passwordLayout.error = getString(R.string.editTextErrorPassword)
            return
        }

        Utils.hideKeyboardFrom(this@LoginActivity, binding.loginButton.login)

        startLoading({ presenter.login(username, password) })
    }

    private fun startLoadingAnimation() {
        val d = binding.loginLogoMiddle.vapullaLogoMiddle.drawable as Animatable
        val d2 = binding.loginLogoMiddle.vapullaLogoBottom.drawable as Animatable

        VectorAnimCompat.registerAnimationCallback(
            d,
            object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    d.start()
                    handler.postDelayed({
                        d2.stop()
                        d2.start()
                    }, 300)
                }
            })
        d.start()
        handler.postDelayed({ d2.start() }, 300)
    }

    private fun stopLoadingAnimation() {
        handler.removeCallbacksAndMessages(null)

        val d = binding.loginLogoMiddle.vapullaLogoMiddle.drawable as Animatable
        val d2 = binding.loginLogoMiddle.vapullaLogoBottom.drawable as Animatable

        VectorAnimCompat.clearAnimationCallbacks(d)
        VectorAnimCompat.clearAnimationCallbacks(d2)

        d.stop()
        d2.stop()
    }
}
