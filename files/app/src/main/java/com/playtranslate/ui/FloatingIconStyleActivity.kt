package com.playtranslate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.playtranslate.CaptureService
import com.playtranslate.R
import com.playtranslate.applyTheme

/** Lets the user pick an image and tune the floating-ball appearance. */
class FloatingIconStyleActivity : AppCompatActivity() {
    private lateinit var prefs: FloatingIconStylePrefs
    private lateinit var preview: ImageView
    private lateinit var opacityValue: TextView
    private lateinit var scaleValue: TextView
    private lateinit var opacitySeek: SeekBar
    private lateinit var scaleSeek: SeekBar
    private var selectedUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            selectedUri = uri
            showPreview(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_floating_icon_style)

        prefs = FloatingIconStylePrefs(this)
        selectedUri = prefs.imageUri

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        preview = findViewById(R.id.preview)
        opacityValue = findViewById(R.id.opacityValue)
        scaleValue = findViewById(R.id.scaleValue)
        opacitySeek = findViewById<SeekBar>(R.id.opacitySeek).apply {
            max = 80
            progress = ((prefs.opacity - 0.2f) * 100).toInt().coerceIn(0, max)
            setOnSeekBarChangeListener(simpleListener { updateLabels() })
        }
        scaleSeek = findViewById<SeekBar>(R.id.scaleSeek).apply {
            max = 45
            progress = ((prefs.scale - 0.55f) * 100).toInt().coerceIn(0, max)
            setOnSeekBarChangeListener(simpleListener { updateLabels() })
        }

        selectedUri?.let(::showPreview)
        updateLabels()

        findViewById<MaterialButton>(R.id.btnChoose).setOnClickListener {
            pickImage.launch(arrayOf("image/png", "image/webp", "image/jpeg"))
        }
        findViewById<MaterialButton>(R.id.btnReset).setOnClickListener {
            selectedUri = null
            preview.setImageResource(R.drawable.ic_launcher_foreground)
            opacitySeek.progress =
                ((FloatingIconStylePrefs.DEFAULT_OPACITY - 0.2f) * 100).toInt()
            scaleSeek.progress =
                ((FloatingIconStylePrefs.DEFAULT_SCALE - 0.55f) * 100).toInt()
            updateLabels()
        }
        findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            prefs.save(selectedUri, opacity(), scale())
            CaptureService.instance?.refreshFloatingIconStyle()
            finish()
        }
    }

    private fun opacity(): Float = 0.2f + opacitySeek.progress / 100f
    private fun scale(): Float = 0.55f + scaleSeek.progress / 100f

    private fun updateLabels() {
        opacityValue.text = "${(opacity() * 100).toInt()}%"
        scaleValue.text = "${(scale() * 100).toInt()}%"
        preview.alpha = opacity()
        preview.scaleX = scale()
        preview.scaleY = scale()
    }

    private fun showPreview(uri: Uri) {
        preview.setImageBitmap(decodeFloatingIconBitmap(this, uri))
    }

    private fun simpleListener(onChange: () -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = onChange()
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }
}
