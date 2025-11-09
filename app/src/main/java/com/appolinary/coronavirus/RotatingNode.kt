package com.appolinary.coronavirus

import android.animation.Animator
import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.ScaleGestureDetector
import android.view.animation.LinearInterpolator
import androidx.core.view.MotionEventCompat
import com.appolinary.coronavirus.enums.ObjectState
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.QuaternionEvaluator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem

class RotatingNode(transformationSystem: TransformationSystem) :
    TransformableNode(transformationSystem)/*, Node.OnTouchListener,
    ScaleGestureDetector.OnScaleGestureListener , Node.OnTapListener*/ {
    // We'll use Property Animation to make this node rotate.
    private var position: ObjectState = ObjectState.DOWN

    private var rotationAnimation: ObjectAnimator? = null
    private var degreesPerSecond = 70.0f //было 90

    private var lastSpeedMultiplier = 1.0f

    private val animationDuration: Long
        get() = (1000 * 360 / (degreesPerSecond * speedMultiplier)).toLong()

    private val speedMultiplier: Float
        get() = 1.0f

    private val dragRotationController =
        DragRotationController(this, transformationSystem.dragRecognizer)

    init {
        translationController.isEnabled = false
        removeTransformationController(translationController)
        removeTransformationController(rotationController)
        addTransformationController(dragRotationController)
    }

    override fun onUpdate(frameTime: FrameTime?) {
        super.onUpdate(frameTime)
        // Animation hasn't been set up.
        if (rotationAnimation == null) {
            return
        }
        // Check if we need to change the speed of rotation.
        val speedMultiplier = speedMultiplier
        // Nothing has changed. Continue rotating at the same speed.
        if (lastSpeedMultiplier == speedMultiplier) {
            return
        }
        if (speedMultiplier == 0.0f) {
            rotationAnimation!!.pause()
        } else {
            rotationAnimation!!.resume()
            val animatedFraction = rotationAnimation!!.animatedFraction
            rotationAnimation!!.duration = animationDuration
            rotationAnimation!!.setCurrentFraction(animatedFraction)
        }
        lastSpeedMultiplier = speedMultiplier
    }

    /** Sets rotation speed  */
    fun setDegreesPerSecond(degreesPerSecond: Float) {
        this.degreesPerSecond = degreesPerSecond
    }

    override fun onActivate() {
        startAnimation()
    }

    override fun onDeactivate() {
        stopAnimation()
    }

    private fun startAnimation() {
        if (rotationAnimation != null) {
            return
        }
        rotationAnimation = createAnimator()
        rotationAnimation!!.target = this
        rotationAnimation!!.duration = animationDuration
        rotationAnimation!!.start()
    }

    private fun stopAnimation() {
        if (rotationAnimation == null) {
            return
        }
        rotationAnimation!!.cancel()
        rotationAnimation = null
    }

    /** Returns an ObjectAnimator that makes this node rotate.  */
    private fun createAnimator(): ObjectAnimator {
        // Node's setLocalRotation method accepts Quaternions as parameters.
        // First, set up orientations that will animate a circle.
        val orientation1 = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), 0f)
        val orientation2 = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), 120f)
        val orientation3 = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), 240f)
        val orientation4 = Quaternion.axisAngle(Vector3(0.0f, 1.0f, 0.0f), 360f)

        val rotationAnimation = ObjectAnimator()
        rotationAnimation.setObjectValues(orientation1, orientation2, orientation3, orientation4)

        // Next, give it the localRotation property.
        rotationAnimation.propertyName = "localRotation"

        // Use Sceneform's QuaternionEvaluator.
        rotationAnimation.setEvaluator(QuaternionEvaluator())

        //  Allow rotationAnimation to repeat forever
        rotationAnimation.repeatCount = ObjectAnimator.INFINITE
        rotationAnimation.repeatMode = ObjectAnimator.RESTART
        rotationAnimation.interpolator = LinearInterpolator()
        rotationAnimation.setAutoCancel(true)

        return rotationAnimation
    }

//    override fun onTap(p0: HitTestResult?, p1: MotionEvent?) {
//        pullUp() //To change body of created functions use File | Settings | File Templates.
//    }

    fun pullUp() {
        // If not moving up or already moved up, start animation
        if (position != ObjectState.MOVING_UP && position != ObjectState.UP) {
            animatePullUp()

        }
    }

    // 3
    private fun localPositionAnimator(vararg values: Any?): ObjectAnimator {
        return ObjectAnimator().apply {
            target = this@RotatingNode
            propertyName = "localPosition"
            duration = 250
            interpolator = LinearInterpolator()

            setAutoCancel(true)
            // * = Spread operator, this will pass N `Any?` values instead of a single list `List<Any?>`
            setObjectValues(*values)
            // Always apply evaluator AFTER object values or it will be overwritten by a default one

            setEvaluator(Vector3Evaluator())
        }
    }

    var low: Vector3 = Vector3()
    var high: Vector3 = Vector3()
    private fun animatePullUp() {
        // No matter where you start (i.e. start from .3 instead of 0F),
        // you will always arrive at .4F
        low = Vector3(localPosition)
        high = Vector3(localPosition).apply { y = +.1F }

        val animation = localPositionAnimator(low, high)

        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                position = ObjectState.UP
                animatePullDown()
            }

            override fun onAnimationStart(animation: Animator?) {
                position = ObjectState.MOVING_UP
            }

        })
        animation.start()
    }

    private fun animatePullDown() {
        high = Vector3(localPosition)
        low = Vector3(localPosition).apply { y = 0F }
        val animation = localPositionAnimator(high, low)

        animation.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {}
            override fun onAnimationCancel(animation: Animator?) {}

            override fun onAnimationEnd(animation: Animator?) {
                position = ObjectState.DOWN
            }

            override fun onAnimationStart(animation: Animator?) {
                position = ObjectState.MOVING_DOWN
            }
        })
        animation.start()
    }


}
