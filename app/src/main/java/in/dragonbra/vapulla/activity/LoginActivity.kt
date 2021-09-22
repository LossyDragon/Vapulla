package `in`.dragonbra.vapulla.activity

import `in`.dragonbra.vapulla.R
import `in`.dragonbra.vapulla.anim.AutoParallelTransition
import `in`.dragonbra.vapulla.anim.TransitionListener
import `in`.dragonbra.vapulla.anim.VectorAnimCompat
import `in`.dragonbra.vapulla.databinding.ActivityLoginBinding
import `in`.dragonbra.vapulla.databinding.LoginLogoMiddleBinding
import `in`.dragonbra.vapulla.extension.*
import `in`.dragonbra.vapulla.presenter.LoginPresenter
import `in`.dragonbra.vapulla.util.Utils
import `in`.dragonbra.vapulla.view.LoginView
import android.content.Intent
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.Transition
import androidx.transition.TransitionManager
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : VapullaBaseActivity<LoginView, LoginPresenter>(), LoginView {

    @Inject
    lateinit var loginPresenter: LoginPresenter

    lateinit var handler: Handler

    private lateinit var binding: ActivityLoginBinding
    private lateinit var logoBinding: LoginLogoMiddleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        logoBinding = LoginLogoMiddleBinding.bind(binding.root)

        val view = binding.root
        setContentView(view)

        handler = Handler(Looper.getMainLooper())

        val textFactory = TextSwitcher.factory(this)
        binding.loadingText.loadingText.setFactory(textFactory)

        binding.loginButton.login.click { login() }

        binding.steamGuardButton.steamGuardButton.click {
            val code = binding.steamGuardLayout.steamGuardInput.text.toString()

            if (code.length < 5) {
                val errorText = getString(R.string.editTextErrorSteamGuard)
                binding.steamGuardLayout.steamGuardLayout.error = errorText
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

        binding.retryButton.retryButton.click {
            binding.loadingText.loadingText.setText(null)

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_loading)
            TransitionManager.beginDelayedTransition(binding.rootLayout, AutoParallelTransition())
            constraintSet.applyTo(binding.rootLayout)

            val faceAnim = getCompatDrawable(R.drawable.animated_vapulla_from_face)
            logoBinding.vapullaLogoBottom.setImageDrawable(faceAnim)

            VectorAnimCompat.registerAnimationCallback(
                faceAnim as Animatable,
                object : Animatable2Compat.AnimationCallback() {
                    override fun onAnimationEnd(drawable: Drawable) {
                        val drawableTop = getCompatDrawable(R.drawable.vapulla_top)
                        logoBinding.vapullaLogoTop.setImageDrawable(drawableTop)
                        logoBinding.vapullaLogoBottom.show()
                        logoBinding.vapullaLogoMiddle.show()
                        startLoadingAnimation()
                        presenter.retry()
                    }
                }
            )

            faceAnim.start()
        }

        binding.cancelButton.steamGuardButtonCancel.click {
            presenter.cancelSteamGuard()
        }

        binding.usernameLayout.username.bindLayout()
        binding.passwordLayout.password.bindLayout()
        binding.steamGuardLayout.steamGuardInput.bindLayout()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLoadingAnimation()
    }

    override fun createPresenter(): LoginPresenter = loginPresenter

    override fun showLoading(text: String) {
        runOnUiThread {
            binding.loadingText.loadingText.setText(text)
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
        val loginIntent = Intent(this, HomeActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)
        finish()
    }

    override fun showSteamGuard(is2Fa: Boolean, errorMessage: String?) {
        runOnUiThread {
            stopLoadingAnimation()
            binding.loadingText.loadingText.setText("")

            errorMessage?.let {
                binding.errorText.errorText.text = errorMessage
            }

            val loadingText = if (is2Fa)
                getString(R.string.loadingTextSteamGuardMobile)
            else
                getString(R.string.loadingTextSteamGuardEmail)

            binding.loadingText.loadingText.setText(loadingText)

            val layout = errorMessage?.let { R.layout.activity_login_frame_steamguard_error }
                ?: run { R.layout.activity_login_frame_steamguard }

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, layout)
            TransitionManager.beginDelayedTransition(binding.rootLayout, AutoParallelTransition())
            constraintSet.applyTo(binding.rootLayout)
        }
    }

    override fun showLoginForm(errorMessage: String?) {
        runOnUiThread {
            stopLoadingAnimation()
            binding.loadingText.loadingText.setText("")

            errorMessage?.let {
                binding.errorText.errorText.text = errorMessage
            }

            val layout = errorMessage?.let { R.layout.activity_login_frame_form_error }
                ?: run { R.layout.activity_login_frame_form }

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, layout)
            TransitionManager.beginDelayedTransition(binding.rootLayout, AutoParallelTransition())
            constraintSet.applyTo(binding.rootLayout)
        }
    }

    override fun showFailedScreen() {
        runOnUiThread {
            stopLoadingAnimation()

            logoBinding.vapullaLogoBottom.invisible()
            logoBinding.vapullaLogoMiddle.invisible()

            val faceAnim = getCompatDrawable(R.drawable.animated_vapulla_to_face)
            logoBinding.vapullaLogoTop.setImageDrawable(faceAnim)
            (faceAnim as Animatable).start()

            binding.loadingText.loadingText.setText(getString(R.string.loadingTextFailed))

            val constraintSet = ConstraintSet()
            constraintSet.clone(this, R.layout.activity_login_frame_failed)
            TransitionManager.beginDelayedTransition(binding.rootLayout, AutoParallelTransition())
            constraintSet.applyTo(binding.rootLayout)
        }
    }

    private fun login() {
        val username = binding.usernameLayout.username.text.toString()
        if (username.isEmpty()) {
            binding.usernameLayout.usernameLayout.error = getString(R.string.editTextErrorUsername)
            return
        }

        val password = binding.passwordLayout.password.text.toString()
        if (password.isEmpty()) {
            binding.passwordLayout.passwordLayout.error = getString(R.string.editTextErrorPassword)
            return
        }

        Utils.hideKeyboardFrom(this@LoginActivity, binding.loginButton.login)

        startLoading { presenter.login(username, password) }
    }

    private fun startLoadingAnimation() {
        val d = logoBinding.vapullaLogoMiddle.drawable as Animatable
        val d2 = logoBinding.vapullaLogoBottom.drawable as Animatable

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
            }
        )
        d.start()
        handler.postDelayed({ d2.start() }, 300)
    }

    private fun stopLoadingAnimation() {
        handler.removeCallbacksAndMessages(null)

        val d = logoBinding.vapullaLogoMiddle.drawable as Animatable
        val d2 = logoBinding.vapullaLogoBottom.drawable as Animatable

        VectorAnimCompat.clearAnimationCallbacks(d)
        VectorAnimCompat.clearAnimationCallbacks(d2)

        d.stop()
        d2.stop()
    }
}
