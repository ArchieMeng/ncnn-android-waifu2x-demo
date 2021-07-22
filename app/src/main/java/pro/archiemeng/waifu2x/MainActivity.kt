// Copyright (C) 2021  ArchieMeng <archiemeng@protonmail.com>
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package pro.archiemeng.waifu2x

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pro.archiemeng.waifu2x.utils.Processor
import pro.archiemeng.waifu2x.utils.toInts
import java.io.File
import java.io.FileNotFoundException
import java.lang.Integer.min

class MainActivity : Activity() {
    private var imageView: ImageView? = null
    private var bitmap: Bitmap? = null
    private var yourSelectedImage: Bitmap? = null
    private var numThreads: Int = 1
    private val upscaler = Waifu2x()
    private val processJob = Job()
    private val coroutineScope = CoroutineScope(processJob)
    private var lastImageViewCoroutine: Job?=null

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        imageView = findViewById(R.id.imageView)
        val buttonImage = findViewById<Button>(R.id.buttonImage)

        imageView?.setOnClickListener {

        }

        buttonImage.setOnClickListener {
            val i = Intent(Intent.ACTION_PICK)
            i.type = "image/*"
            startActivityForResult(i, SELECT_IMAGE)
        }
        val buttonDetect = findViewById<Button>(R.id.buttonDetect)
        buttonDetect.setOnClickListener {
            yourSelectedImage?.also {
                coroutineScope.launch {
                    numThreads = 8
                    processBitmap(upscaler, yourSelectedImage!!, false)
                }
            }
        }
        val buttonDetectGPU = findViewById<Button>(R.id.buttonDetectGPU)
        buttonDetectGPU.setOnClickListener {
            yourSelectedImage?.also {
                coroutineScope.launch {
                    numThreads = 1
                    processBitmap(upscaler, yourSelectedImage!!, true)
                }
            }
        }
    }

    private fun showBitmapImage(bitmapImage: Bitmap?) {
        if (bitmapImage == null) {
            return
        }
        // draw objects on bitmap
        runOnUiThread {
            Glide.with(this)
                .load(bitmapImage)
                .into(imageView!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val selectedImage = data?.data
            selectedImage?.let {
                try {
                    if (requestCode == SELECT_IMAGE) {
                        bitmap = decodeUri(selectedImage)
                        yourSelectedImage = bitmap?.copy(Bitmap.Config.ARGB_8888, true)
                        bitmap?.let { imageView?.setImageBitmap(bitmap) }
                    }
                } catch (e: FileNotFoundException) {
                    Log.e("MainActivity", "FileNotFoundException")
                    return
                }
            }
        }
    }

    private fun processBitmap(upscaler: Processor, bitmapInput: Bitmap, useGPU: Boolean) {
        val startTime = System.currentTimeMillis()
        upscaler.init(
            assets,
            useGPU,
            "models-upconv_7_anime_style_art_rgb",
            2,
            -1,
            false,
            numThreads,
            0,
        )

        if (!upscaler.useGPU)
            upscaler.tileSize = 256

        val bitmapOutput = Bitmap.createBitmap(
            bitmapInput.width * upscaler.scale,
            bitmapInput.height * upscaler.scale, Bitmap.Config.ARGB_8888
        )

        for (y in 0 until bitmapInput.height step upscaler.tileSize) {
            val height =
                min(bitmapInput.height - y, upscaler.tileSize)
            for (x in 0 until bitmapInput.width step upscaler.tileSize) {
                val width = min(bitmapInput.width - x, upscaler.tileSize)
                var tileBitmap = Bitmap.createBitmap(
                    bitmapInput,
                    x, y,
                    width, height
                )
                tileBitmap = upscaler.process(tileBitmap)
                bitmapOutput.setPixels(
                    tileBitmap.toInts(),
                    0, tileBitmap.width,
                    x * upscaler.scale, y * upscaler.scale,
                    tileBitmap.width, tileBitmap.height
                )
                val currentResult = Bitmap.createBitmap(
                    bitmapOutput,
                    0, 0,
                    bitmapOutput.width, (y + height) * upscaler.scale
                )
                lastImageViewCoroutine = lastImageViewCoroutine?.apply {
                    if (!this.isActive)
                        coroutineScope.launch { showBitmapImage(currentResult) }
                } ?: coroutineScope.launch { showBitmapImage(currentResult) }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val tempFile = File.createTempFile("temp", ".webp")
            bitmapOutput.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, tempFile.outputStream())
        } else {
            val tempFile = File.createTempFile("temp", ".png")
            bitmapOutput.compress(Bitmap.CompressFormat.PNG, 100, tempFile.outputStream())
        }

        bitmapOutput.recycle()
        Log.d(this.toString(), "image elapsed time:${System.currentTimeMillis() - startTime}ms")
    }

    @Throws(FileNotFoundException::class)
    private fun decodeUri(selectedImage: Uri): Bitmap? {
        // Decode image size
        return BitmapFactory.decodeStream(contentResolver.openInputStream(selectedImage))
    }

    companion object {
        private const val SELECT_IMAGE = 1
    }
}