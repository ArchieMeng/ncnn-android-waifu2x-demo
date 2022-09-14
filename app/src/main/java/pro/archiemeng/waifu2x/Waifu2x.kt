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

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import pro.archiemeng.waifu2x.utils.Processor

// all native class properties settings are normally written in JNI
class Waifu2x : Processor {

    private var realScale: Int = 1

    companion object {
        init {
            System.loadLibrary("waifu2x")
        }
    }

    private external fun RawInit(
        useGPU: Boolean,
        ttaMode: Boolean,
        numThreads: Int,
    )

    private external fun RawLoad(assetManager: AssetManager, paramPath: String, modelPath: String)

    private external fun RawProcess(inBitmap: Bitmap, outBitmap: Bitmap)

    private external fun SetScale(scale: Int)

    private external fun SetNoise(noise: Int)

    private external fun GetTileSize(): Int

    private external fun SetTileSize(tileSize: Int)

    private external fun GetGPUMode(): Boolean

    override val useGPU: Boolean
        get() = this.GetGPUMode()

    override val scale: Int
        get() = this.realScale

    override var tileSize: Int
        get() = this.GetTileSize()
        set(value) = this.SetTileSize(value)


    override fun init(
        assetManager: AssetManager,
        useGPU: Boolean,
        model: String,
        scale: Int,
        noise: Int,
        ttaMode: Boolean,
        numThreads: Int,
        tileSize: Int
    ) {
        this.realScale = scale
        this.RawInit(useGPU, ttaMode, numThreads)
        assert(-1 <= noise && noise <= 3)
        SetScale(scale)
        SetNoise(noise)
        SetTileSize(tileSize)
        lateinit var paramPath: String
        lateinit var modelPath: String

        if (noise == -1) {
            paramPath = "${model}/scale2.0x_model.param"
            modelPath = "${model}/scale2.0x_model.bin"
        } else if (scale == 1) {
            paramPath = "${model}/noise${noise}_model.param"
            modelPath = "${model}/noise${noise}_model.param"
        } else {
            paramPath = "${model}/noise${noise}_scale2.0x_model.param"
            modelPath = "${model}/noise${noise}_scale2.0x_model.param"
        }
        this.RawLoad(assetManager, paramPath, modelPath)
    }

    override fun process(bitmap: Bitmap): Bitmap {
        var curScaler = 1
        var input = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        lateinit var output: Bitmap

        Log.d("$this", "input bitmap size: ${input.byteCount}")

        // run processing as least for once for denoising process
        while (curScaler < scale || curScaler == 1) {
            output = Bitmap.createBitmap(
                input.width * 2,
                input.height * 2,
                Bitmap.Config.ARGB_8888,
            )
            Log.d("$this", "output bitmap size: ${output.byteCount}")
            this.RawProcess(input, output)
            input = output
            curScaler *= 2
        }
        return output
    }
}
