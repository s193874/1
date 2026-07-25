package com.playtranslate.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.playtranslate.CaptureService
import com.playtranslate.R

/** Lets the user pick a transparent PNG/WebP and tune the floating-ball appearance. */
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
        super.onCreate(savedInstanceState)
        prefs = FloatingIconStylePrefs(this)
        selectedUri = prefs.imageUri

        val dp = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((24 * dp).toInt(), (32 * dp).toInt(), (24 * dp).toInt(), (24 * dp).toInt())
        }
        root.addView(TextView(this).apply {
            setText(R.string.floating_icon_style_title)
            textSize = 22f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = true
            setBackgroundColor(0x22000000)
            contentDescription = getString(R.string.floating_icon_style_preview)
        }
        root.addView(preview, LinearLayout.LayoutParams((112 * dp).toInt(), (112 * dp).toInt()).apply {
            topMargin = (20 * dp).toInt()
            bottomMargin = (18 * dp).toInt()
        })
        selectedUri?.let(::showPreview)

        root.addView(Button(this).apply {
            setText(R.string.floating_icon_style_choose)
            isAllCaps = false
            setOnClickListener { pickImage.launch(arrayOf("image/png", "image/webp", "image/jpeg")) }
        })

        opacityValue = TextView(this)
        opacitySeek = SeekBar(this).apply {
            max = 80
            progress = ((prefs.opacity - 0.2f) * 100).toInt().coerceIn(0, max)
            setOnSeekBarChangeListener(simpleListener { updateLabels() })
        }
        root.addView(opacityValue, rowParams(dp))
        root.addView(opacitySeek, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        scaleValue = TextView(this)
        scaleSeek = SeekBar(this).apply {
            max = 45
            progress = ((prefs.scale - 0.55f) * 100).toInt().coerceIn(0, max)
            setOnSeekBarChangeListener(simpleListener { updateLabels() })
        }
        root.addView(scaleValue, rowParams(dp))
        root.addView(scaleSeek, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        updateLabels()

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        buttons.addView(Button(this).apply {
            setText(R.string.floating_icon_style_reset)
            isAllCaps = false
            setOnClickListener {
                selectedUri = null
                preview.setImageDrawable(null)
                opacitySeek.progress = ((FloatingIconStylePrefs.DEFAULT_OPACITY - 0.2f) * 100).toInt()
                scaleSeek.progress = ((FloatingIconStylePrefs.DEFAULT_SCALE - 0.55f) * 100).toInt()
                updateLabels()
            }
        })
        buttons.addView(Button(this).apply {
            setText(R.string.floating_icon_style_save)
            isAllCaps = false
            setOnClickListener {
                prefs.save(selectedUri, opacity(), scale())
                CaptureService.instance?.refreshFloatingIconStyle()
                finish()
            }
        })
        root.addView(buttons, rowParams(dp))
        setContentView(ScrollView(this).apply {
            addView(root, ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ))
        })
    }

    private fun opacity(): Float = 0.2f + opacitySeek.progress / 100f
    private fun scale(): Float = 0.55f + scaleSeek.progress / 100f

    private fun updateLabels() {
        if (::opacityValue.isInitialized) {
            opacityValue.text = getString(R.string.floating_icon_style_opacity, (opacity() * 100).toInt())
        }
        if (::scaleValue.isInitialized) {
            scaleValue.text = getString(R.string.floating_icon_style_scale, (scale() * 100).toInt())
        }
        if (::preview.isInitialized) preview.alpha = opacity()
    }

    private fun showPreview(uri: Uri) {
        val bitmap = decodeFloatingIconBitmap(this, uri)
        preview.setImageBitmap(bitmap)
    }

    private fun rowParams(dp: Float) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = (16 * dp).toInt() }

    private fun simpleListener(onChange: () -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) = onChange()
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }
}
