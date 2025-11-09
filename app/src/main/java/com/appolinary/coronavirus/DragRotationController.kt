package com.appolinary.coronavirus

import android.util.Log
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.ux.BaseTransformableNode
import com.google.ar.sceneform.ux.BaseTransformationController
import com.google.ar.sceneform.ux.DragGesture
import com.google.ar.sceneform.ux.DragGestureRecognizer

class DragRotationController(transformableNode: BaseTransformableNode, gestureRecognizer: DragGestureRecognizer) :
    BaseTransformationController<DragGesture>(transformableNode, gestureRecognizer) {

    // Rate that the node rotates in degrees per degree of twisting.
    private var rotationRateDegrees = 0.5f

    public override fun canStartTransformation(gesture: DragGesture): Boolean {
        return transformableNode.isSelected
    }

    public override fun onContinueTransformation(gesture: DragGesture) {
        Log.d("AndreyTest", "gesture = " + gesture)
        var localRotation = transformableNode.localRotation
        val rotationAmountX = gesture.delta.x * rotationRateDegrees
        val rotationDeltaX = Quaternion(Vector3.up(), rotationAmountX)
        localRotation = Quaternion.multiply(localRotation, rotationDeltaX)
        transformableNode.localRotation = localRotation
    }

    public override fun onEndTransformation(gesture: DragGesture) {}

//    override fun onTap(p0: HitTestResult?, p1: MotionEvent?) {
//        (transformableNode as RotatingNode).pullUp()
//    }
}