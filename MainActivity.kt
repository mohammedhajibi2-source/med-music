package com.medmusic.app

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val bg = Color.rgb(12, 12, 15)
    private val panel = Color.rgb(27, 25, 30)
    private val red = Color.rgb(239, 35, 60)
    private val white = Color.rgb(247, 244, 246)
    private val muted = Color.rgb(167, 160, 169)
    private var player: ExoPlayer? = null
    private val songs = mutableListOf<Song>()
    private lateinit var root: LinearLayout
    private lateinit var list: LinearLayout
    private lateinit var nowTitle: TextView
    private lateinit var playButton: Button
    private lateinit var status: TextView
    private var currentIndex = -1

    data class Song(val id: Long, val title: String, val artist: String, val uri: Uri)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) loadSongs() else {
                status.text = "Allow audio access to show music stored on your phone."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (::playButton.isInitialized) playButton.text = if (isPlaying) "Ⅱ  PAUSE" else "▶  PLAY"
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentIndex = player?.currentMediaItemIndex ?: -1
                updateNowPlaying()
            }
        })
        buildUi()
        requestAudioPermission()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(20), dp(18), dp(20), dp(12))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "MED MUSIC"
            textSize = 25f
            setTextColor(white)
            typeface = android.graphics.Typeface.create("sans-serif-black", android.graphics.Typeface.BOLD)
            letterSpacing = .08f
        }
        val logo = TextView(this).apply {
            text = "  ●"
            textSize = 24f
            setTextColor(red)
        }
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(logo)
        root.addView(header)

        val tagline = TextView(this).apply {
            text = "YOUR SOUND. YOUR DARK SIDE."
            textSize = 10f
            letterSpacing = .16f
            setTextColor(muted)
            setPadding(0, dp(4), 0, dp(22))
        }
        root.addView(tagline)

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(red, 20)
        }
        hero.addView(TextView(this).apply {
            text = "SOUNDTRACK YOUR NIGHT"
            textSize = 11f
            letterSpacing = .12f
            setTextColor(Color.WHITE)
        })
        hero.addView(TextView(this).apply {
            text = "Find your\\nnext obsession."
            textSize = 27f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.create("sans-serif-black", 1)
            setPadding(0, dp(9), 0, dp(14))
        })
        hero.addView(Button(this).apply {
            text = "SCAN MY MUSIC  →"
            setTextColor(Color.WHITE)
            background = rounded(Color.rgb(25, 17, 21), 12)
            setOnClickListener { requestAudioPermission() }
        })
        root.addView(hero)

        val section = TextView(this).apply {
            text = "YOUR LIBRARY"
            textSize = 13f
            letterSpacing = .12f
            setTextColor(white)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(22), 0, dp(8))
        }
        root.addView(section)
        status = TextView(this).apply {
            text = "Looking for tracks on your device…"
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(10))
        }
        root.addView(status)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            addView(list)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val mini = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(8))
            background = rounded(panel, 16)
        }
        nowTitle = TextView(this).apply {
            text = "Nothing playing yet"
            textSize = 14f
            setTextColor(white)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        mini.addView(nowTitle)
        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        val previous = controlButton("⏮") { skip(-1) }
        playButton = controlButton("▶  PLAY") { togglePlay() }
        val next = controlButton("⏭") { skip(1) }
        controls.addView(previous, LinearLayout.LayoutParams(0, dp(42), 1f))
        controls.addView(playButton, LinearLayout.LayoutParams(0, dp(42), 2f))
        controls.addView(next, LinearLayout.LayoutParams(0, dp(42), 1f))
        mini.addView(controls)
        root.addView(mini, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        setContentView(root)
    }

    private fun controlButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(white)
            background = rounded(if (label.contains("PLAY")) red else panel, 10)
            setOnClickListener { action() }
        }

    private fun requestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadSongs()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun loadSongs() {
        songs.clear()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val cursor: Cursor? = contentResolver.query(collection, projection, selection, null, sort)
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Unknown track"
                val artist = it.getString(artistCol)?.takeIf { s -> s != "<unknown>" } ?: "Unknown artist"
                songs.add(Song(id, title, artist, ContentUris.withAppendedId(collection, id)))
            }
        }
        renderSongs()
    }

    private fun renderSongs() {
        list.removeAllViews()
        if (songs.isEmpty()) {
            status.text = "No local tracks found. Add audio files to your phone, then scan again."
            return
        }
        status.text = "${songs.size} TRACK${if (songs.size == 1) "" else "S"} FOUND"
        songs.forEachIndexed { index, song ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(9), dp(10), dp(9))
                background = rounded(panel, 12)
                setOnClickListener { playAt(index) }
            }
            val cover = TextView(this).apply {
                text = "♫"
                textSize = 23f
                gravity = Gravity.CENTER
                setTextColor(red)
                background = rounded(Color.rgb(47, 24, 31), 10)
            }
            row.addView(cover, LinearLayout.LayoutParams(dp(46), dp(46)))
            val labels = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
            }
            labels.addView(TextView(this).apply {
                text = song.title
                textSize = 14f
                setTextColor(white)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            labels.addView(TextView(this).apply {
                text = song.artist
                textSize = 12f
                setTextColor(muted)
                maxLines = 1
            })
            row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply {
                text = "▶"
                textSize = 16f
                setTextColor(red)
                setPadding(dp(10), 0, dp(5), 0)
            })
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(8)
            list.addView(row, params)
        }
    }

    private fun playAt(index: Int) {
        if (songs.isEmpty()) return
        currentIndex = index.coerceIn(0, songs.lastIndex)
        val items = songs.map { MediaItem.fromUri(it.uri) }
        player?.setMediaItems(items, currentIndex, 0L)
        player?.prepare()
        player?.play()
        updateNowPlaying()
    }

    private fun updateNowPlaying() {
        if (currentIndex in songs.indices) {
            nowTitle.text = "♫  ${songs[currentIndex].title}"
            playButton.text = if (player?.isPlaying == true) "Ⅱ  PAUSE" else "▶  PLAY"
        }
    }

    private fun togglePlay() {
        val p = player ?: return
        if (p.mediaItemCount == 0) {
            if (songs.isNotEmpty()) playAt(0)
            else Toast.makeText(this, "Scan your music first", Toast.LENGTH_SHORT).show()
        } else if (p.isPlaying) p.pause() else p.play()
    }

    private fun skip(direction: Int) {
        val p = player ?: return
        if (p.mediaItemCount == 0) {
            if (songs.isNotEmpty()) playAt(0)
            return
        }
        if (direction < 0) p.seekToPreviousMediaItem() else p.seekToNextMediaItem()
    }

    private fun rounded(color: Int, radiusDp: Int): android.graphics.drawable.GradientDrawable =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}
