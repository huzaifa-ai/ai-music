package com.holisticapp.fitness.ui.facial

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.holisticapp.fitness.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.random.Random

class FacialExpressionActivity : AppCompatActivity() {
    
    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var retakeButton: Button
    private lateinit var analyzeButton: Button
    private lateinit var capturedImageView: ImageView
    private lateinit var resultTextView: TextView
    private lateinit var emotionEmojiView: TextView
    private lateinit var confidenceTextView: TextView
    
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedImageFile: File? = null
    
    // Facial expressions with emojis and descriptions
    private val expressions = listOf(
        FacialExpression("Happy", "😊", "You're showing signs of joy and positivity!", "#4CAF50"),
        FacialExpression("Sad", "😢", "You seem to be feeling down. Consider some uplifting activities.", "#2196F3"),
        FacialExpression("Angry", "😠", "You appear stressed or frustrated. Try some relaxation techniques.", "#F44336"),
        FacialExpression("Surprised", "😲", "You look surprised! Something unexpected happened?", "#FF9800"),
        FacialExpression("Neutral", "😐", "You have a calm, neutral expression.", "#9E9E9E"),
        FacialExpression("Anxious", "😰", "You seem worried or anxious. Take some deep breaths.", "#9C27B0"),
        FacialExpression("Focused", "🤔", "You appear concentrated and thoughtful.", "#607D8B")
    )
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required for facial analysis", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_facial_expression)
        
        initViews()
        setupClickListeners()
        
        // Request camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        retakeButton = findViewById(R.id.retakeButton)
        analyzeButton = findViewById(R.id.analyzeButton)
        capturedImageView = findViewById(R.id.capturedImageView)
        resultTextView = findViewById(R.id.resultTextView)
        emotionEmojiView = findViewById(R.id.emotionEmojiView)
        confidenceTextView = findViewById(R.id.confidenceTextView)
        
        // Initially hide result views
        hideResultViews()
    }
    
    private fun setupClickListeners() {
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        captureButton.setOnClickListener {
            takePhoto()
        }
        
        retakeButton.setOnClickListener {
            retakePhoto()
        }
        
        analyzeButton.setOnClickListener {
            analyzeExpression()
        }
    }
    
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            imageCapture = ImageCapture.Builder()
                .build()
            
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            "$name.jpg"
        )
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@FacialExpressionActivity, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImageFile = photoFile
                    showCapturedImage(photoFile)
                    Toast.makeText(this@FacialExpressionActivity, "Photo captured successfully", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    private fun showCapturedImage(photoFile: File) {
        // Hide camera preview and show captured image
        previewView.visibility = android.view.View.GONE
        capturedImageView.visibility = android.view.View.VISIBLE
        
        // Load and display the captured image
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val rotatedBitmap = rotateBitmap(bitmap, 270f) // Adjust rotation as needed
        capturedImageView.setImageBitmap(rotatedBitmap)
        
        // Update button visibility
        captureButton.visibility = android.view.View.GONE
        retakeButton.visibility = android.view.View.VISIBLE
        analyzeButton.visibility = android.view.View.VISIBLE
    }
    
    private fun retakePhoto() {
        // Show camera preview and hide captured image
        previewView.visibility = android.view.View.VISIBLE
        capturedImageView.visibility = android.view.View.GONE
        
        // Update button visibility
        captureButton.visibility = android.view.View.VISIBLE
        retakeButton.visibility = android.view.View.GONE
        analyzeButton.visibility = android.view.View.GONE
        
        // Hide result views
        hideResultViews()
        
        // Delete the captured file
        capturedImageFile?.delete()
        capturedImageFile = null
    }
    
    private fun analyzeExpression() {
        // Simulate facial expression analysis
        val randomExpression = expressions.random()
        val confidence = Random.nextInt(75, 96) // Random confidence between 75-95%
        
        // Show results
        showResultViews()
        emotionEmojiView.text = randomExpression.emoji
        resultTextView.text = randomExpression.name
        confidenceTextView.text = "Confidence: $confidence%"
        
        // Show description
        Toast.makeText(this, randomExpression.description, Toast.LENGTH_LONG).show()
        
        // Simulate processing delay
        analyzeButton.isEnabled = false
        analyzeButton.text = "Analyzing..."
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            analyzeButton.isEnabled = true
            analyzeButton.text = "Analyze Again"
        }, 2000)
    }
    
    private fun hideResultViews() {
        emotionEmojiView.visibility = android.view.View.GONE
        resultTextView.visibility = android.view.View.GONE
        confidenceTextView.visibility = android.view.View.GONE
    }
    
    private fun showResultViews() {
        emotionEmojiView.visibility = android.view.View.VISIBLE
        resultTextView.visibility = android.view.View.VISIBLE
        confidenceTextView.visibility = android.view.View.VISIBLE
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    data class FacialExpression(
        val name: String,
        val emoji: String,
        val description: String,
        val color: String
    )
} 