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

package pro.archiemeng.waifu2x.utils

import android.content.res.AssetManager
import android.graphics.Bitmap
import java.nio.ByteBuffer

interface Processor {

    val useGPU: Boolean
    val scale: Int
    var tileSize: Int

    fun init(
        assetManager: AssetManager,
        useGPU: Boolean = false,
        model: String,
        scale: Int,
        noise: Int,
        ttaMode: Boolean = false,
        numThreads: Int = 1,
        tileSize: Int = 0,
    )

    fun process(bitmap: Bitmap): Bitmap
}

fun Bitmap.toBytes(): ByteArray {
    val byteBuffer = ByteBuffer.allocate(allocationByteCount)
    this.copyPixelsToBuffer(byteBuffer)
    return byteBuffer.array()
}

fun Bitmap.toInts(): IntArray {
    val intArray = IntArray(allocationByteCount)
    getPixels(intArray, 0, width, 0, 0, width, height)
    return intArray
}