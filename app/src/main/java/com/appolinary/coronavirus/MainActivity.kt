package com.appolinary.coronavirus


import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.MediaActionSound
import android.net.Uri
import android.os.*
import com.example.coronavirusar.R
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.sceneform.*
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {
    private val WATER_MARK: String = "Coronavirus Augmented Reality app \u00A9"
    private val PRIVACY_URL: String = "https://coronavirus-ar.flycricket.io/privacy.html"
    lateinit var fragment: ArFragment
    lateinit var mediaFile: File
    lateinit var mainContainer: CoordinatorLayout
    lateinit var arSceneView: ArSceneView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = this.findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment
        mainContainer = findViewById(R.id.main_cont)
        arSceneView = fragment.arSceneView

        val fab = this.findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            addObject(Uri.parse("scene.sfb"))
        }
        val fab2 = this.findViewById<FloatingActionButton>(R.id.fab2)
        fab2.setOnClickListener {
            playCameraSound()
            takePhoto()
        }
    }

    private fun playCameraSound() {
        val sound = MediaActionSound()
        sound.play(MediaActionSound.SHUTTER_CLICK)
    }

    private fun takePhoto() {
        val bitmp: Bitmap =
            Bitmap.createBitmap(arSceneView.width, arSceneView.height, Bitmap.Config.ARGB_8888)
        val handlerThread: HandlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        val handler1 = Handler(handlerThread.looper)
        PixelCopy.request(arSceneView, bitmp, object : PixelCopy.OnPixelCopyFinishedListener {
            override fun onPixelCopyFinished(copyResult: Int) {
                if (copyResult == PixelCopy.SUCCESS) {
                    try {
                        saveBitmapToDisk(bitmp);
                    } catch (e: IOException) {
                        Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    showShareSnackbar(mainContainer)
                    return
                } else {
                    Toast.makeText(this@MainActivity,
                        "Failed to take screenshot",
                        Toast.LENGTH_LONG).show();
                }
                handlerThread.quitSafely()
            }
        }, handler1)
    }

    private fun showShareSnackbar(view: View) {
        val snackbar = Snackbar.make(view, "Saved in /Photos", Snackbar.LENGTH_LONG)
            .setAction("Share", object : View.OnClickListener {
                override fun onClick(v: View?) {
                    sharePhoto()
                }
            })
        snackbar.show()
    }

    private fun sharePhoto() {
        try {
            //just remove few barriers - https://stackoverflow.com/a/48851566/8619606
            val builder: StrictMode.VmPolicy.Builder = StrictMode.VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())

            val mime = MimeTypeMap.getSingleton()
            val ext = mediaFile.name.substring(mediaFile.name.lastIndexOf(".") + 1)
            val type = mime.getMimeTypeFromExtension(ext)
            val sharingIntent = Intent("android.intent.action.SEND")
            sharingIntent.type = type
            sharingIntent.putExtra("android.intent.extra.STREAM", Uri.fromFile(mediaFile))
            startActivity(Intent.createChooser(sharingIntent, "Share using..."))
        } catch (e: Exception) {
        }

    }


    private fun saveBitmapToDisk(bitmp: Bitmap) {

        val bitmapWithWatermarks = addWaterMarkToBitmap(bitmp, WATER_MARK)

        // need to request permissions one more time during runtime
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1000)
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 2000)
        }
        val storageDir =
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Camera");
        storageDir.mkdirs(); // make sure you call mkdirs() and not mkdir()
        val prefix = getFromattedDateAsFilename()
        mediaFile = File.createTempFile(prefix,  // prefix
            ".jpg",         // suffix
            storageDir      // directory
        )

        val fileOutputStream = FileOutputStream(mediaFile)
        bitmapWithWatermarks.compress(Bitmap.CompressFormat.JPEG, 70, fileOutputStream)
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    private fun getFromattedDateAsFilename(): String? {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_")
        return current.format(formatter)
    }

    private fun addWaterMarkToBitmap(src: Bitmap, watermark: String): Bitmap {
        val w = src.width
        val h = src.height
        val result = Bitmap.createBitmap(w, h, src.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(src, 0f, 0f, null)
        val paint = Paint()
        paint.setColor(Color.RED)
        paint.setTextSize(20f)
        paint.setAntiAlias(true)
        paint.setUnderlineText(false)
        canvas.drawText(watermark, 20f, 25f, paint)

        return result;

    }

    private fun addObject(parse: Uri) {
        val frame = fragment.arSceneView.arFrame
        val point = getScreenCenter()
        if (frame != null) {
            val hits = frame.hitTest(point.x.toFloat(), point.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
                    placeObject(fragment, hit.createAnchor(), parse)
                    break
                }
            }
        }

    }

    private fun placeObject(fragment: ArFragment, createAnchor: Anchor, model: Uri) {
        ModelRenderable.builder().setSource(fragment.context, model).build().thenAccept {
            addNodeToScene(fragment, createAnchor, it)
        }.exceptionally {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(it.message).setTitle("error!")
            val dialog = builder.create()
            dialog.show()
            return@exceptionally null
        }
    }

    private fun addNodeToScene(fragment: ArFragment,
        createAnchor: Anchor,
        renderable: ModelRenderable) {
        val anchorNode = AnchorNode(createAnchor)

        val rotatingNode = RotatingNode(fragment.transformationSystem)
        val transformableNode = TransformableNode(fragment.transformationSystem)
        //        transformableNode.scaleController.maxScale = 0.05f
        //        transformableNode.scaleController.minScale = 0.01f

        rotatingNode.renderable = renderable
        rotatingNode.addChild(transformableNode) //set transformableNode as child -
        rotatingNode.setParent(anchorNode)
        //        rotatingNode.setOnTapListener(this)
        fragment.arSceneView.scene.addChild(anchorNode)

//        fragment.arSceneView.scene.addOnPeekTouchListener(this)


        transformableNode.select()

    }

    private fun getScreenCenter(): Point {
        val vw = findViewById<View>(android.R.id.content)
        return Point(vw.width / 2, vw.height / 2)
    }

    //    override fun onTap(hitTestResult: HitTestResult?, motionEvent: MotionEvent?) {
    //        var node = hitTestResult!!.node as RotatingNode
    //        node.pullUp()
    //        //        if(motionEvent == MotionEvent.)
    //    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater;
        inflater.inflate(R.menu.actionbar_menu, menu);
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.about -> {
                showAboutDialogFragment();
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private val message =
        "This augmented reality app will let you to get know Coronavirus COVID-19 during your self-isolation. Before start help your smartphone to find a plane. After you will see white spots you will able to place your own coronaviruses inside any premises. First version offers you only opportunities to set your own coronaviruses in your house via augmented reality, force them to jump and take a photo and share it with your friends. Soon will be released new cool features - you will can to scale, move, play and even kick you own coronaviruses at home."

    private fun showAboutDialogFragment() {
        var alert = AlertDialog.Builder(this).setTitle(R.string.About).setMessage(message)
            .setCancelable(false)
            .setPositiveButton("CLOSE", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    dialog!!.dismiss()
                }
            })
            // A null listener allows the button to dismiss the dialog and take no further action.
            .setNegativeButton("SHARE", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    shareApp()
                }
            })
            // A null listener allows the button to dismiss the dialog and take no further action.
            .setNeutralButton("PRIVACY", object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    gotoPrivacyPolicy()
                }
            }).setIcon(R.drawable.logo_small_50_50).show();


        val buttonNeg = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
        buttonNeg.setTextColor(Color.RED);
    }

    private fun gotoPrivacyPolicy() {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL));
        startActivity(browserIntent);
    }
    //<br/><a href="https://developer.android.com/training/secure-file-sharing/share-file">Privacy policy</a>

    private fun shareApp() {
        val sendIntent = Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
            "Hey check out this cool Coronavirus Augmented Reality android app at: https://play.google.com/store/apps/details?id=com.appolinary.coronavirus");
        sendIntent.type = "text/plain";
        startActivity(sendIntent);
    }

//    override fun onPeekTouch(hitTesdtResult: HitTestResult?, ev: MotionEvent?) {
//        var mLastTouchX = 0f
//        var mLastTouchY = 0f
//        var mPosX = 0f
//        var mPosY = 0f
//
//        var mActivePointerId = INVALID_POINTER_ID
//
////        var mScaleDetector = ScaleGestureDetector(applicationContext, object : ScaleGestureDetector.OnScaleGestureListener{
////            override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
////                TODO("Not yet implemented")
////            }
////            override fun onScaleEnd(detector: ScaleGestureDetector?) {
////                TODO("Not yet implemented")
////            }
////            override fun onScale(detector: ScaleGestureDetector?): Boolean {
////                TODO("Not yet implemented")
////            }
////        })
////        // Let the ScaleGestureDetector inspect all events.
////        mScaleDetector.onTouchEvent(ev)
//
//        val action = MotionEventCompat.getActionMasked(ev)
//
//        when (action) {
//            MotionEvent.ACTION_DOWN -> {
//                MotionEventCompat.getActionIndex(ev).also { pointerIndex ->
//                    // Remember where we started (for dragging)
//                    mLastTouchX = MotionEventCompat.getX(ev, pointerIndex)
//                    mLastTouchY = MotionEventCompat.getY(ev, pointerIndex)
//                }
//
//                // Save the ID of this pointer (for dragging)
//                mActivePointerId = MotionEventCompat.getPointerId(ev, 0)
//            }
//
//            MotionEvent.ACTION_MOVE -> {
//                // Find the index of the active pointer and fetch its position
//                val (x: Float, y: Float) = MotionEventCompat.findPointerIndex(ev, mActivePointerId)
//                    .let { pointerIndex ->
//                        // Calculate the distance moved
//                        MotionEventCompat.getX(ev, pointerIndex) to MotionEventCompat.getY(ev,
//                            pointerIndex)
//                    }
//
//                mPosX += x - mLastTouchX
//                mPosY += y - mLastTouchY
//
//
//                // Remember this touch position for the next move event
//                mLastTouchX = x
//                mLastTouchY = y
//            }
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                mActivePointerId = INVALID_POINTER_ID
//            }
//            MotionEvent.ACTION_POINTER_UP -> {
//
//                MotionEventCompat.getActionIndex(ev).also { pointerIndex ->
//                    MotionEventCompat.getPointerId(ev, pointerIndex)
//                        .takeIf { it == mActivePointerId }?.run {
//                            // This was our active pointer going up. Choose a new
//                            // active pointer and adjust accordingly.
//                            val newPointerIndex = if (pointerIndex == 0) 1 else 0
//                            mLastTouchX = MotionEventCompat.getX(ev, newPointerIndex)
//                            mLastTouchY = MotionEventCompat.getY(ev, newPointerIndex)
//                            mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex)
//                        }
//                }
//            }
//
//        }
//
//
//    }


}


