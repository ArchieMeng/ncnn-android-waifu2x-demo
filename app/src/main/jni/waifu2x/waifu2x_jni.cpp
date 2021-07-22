// Copyright (C) 2021  ArchieMeng <archiemeng@protonmail.com>
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//        limitations under the License.

#include <android/asset_manager_jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <jni.h>

#include <string>
#include <vector>

// ncnn
#include "layer.h"
#include "net.h"
#include "benchmark.h"
#include "waifu2x.h"

static ncnn::UnlockedPoolAllocator g_blob_pool_allocator;
static ncnn::PoolAllocator g_workspace_pool_allocator;

static Waifu2x* waifu2x;
static auto gpu_mode = true;

extern "C" {
char *TAG = "Waifu2x-ncnn-Vulkan";
JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnLoad");

    ncnn::create_gpu_instance();

    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "JNI_OnUnload");

    ncnn::destroy_gpu_instance();
}
}

extern "C"
JNIEXPORT void JNICALL
Java_pro_archiemeng_waifu2x_Waifu2x_RawInit(JNIEnv *env, jobject thiz,
                                            jboolean use_gpu, jboolean tta_mode, jint num_threads) {
    waifu2x = new Waifu2x(use_gpu ? ncnn::get_default_gpu_index() : -1, tta_mode, num_threads);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "waifu2x class: %p successfully initialized", waifu2x);
    gpu_mode = use_gpu;
    if (use_gpu) {
        uint32_t heap_budget_size = ncnn::get_gpu_device()->get_heap_budget();
        __android_log_print(ANDROID_LOG_DEBUG, TAG, "heap budget size: %u", heap_budget_size);
        if (heap_budget_size > 3900) {
            waifu2x->tilesize = 256;
        } else if (heap_budget_size > 1000) {
            waifu2x->tilesize = 128;
        } else if (heap_budget_size > 250){
            waifu2x->tilesize = 64;
        } else {
            waifu2x->tilesize = 32;
        }
    } else {
        waifu2x->tilesize = -1;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_pro_archiemeng_waifu2x_Waifu2x_RawLoad(JNIEnv *env, jobject thiz,
                                            jobject asset_manager,
                                            jstring param_path, jstring model_path) {
    std::string parampath = std::string(env->GetStringUTFChars(param_path, JNI_FALSE),
                                        (size_t) env->GetStringLength(param_path));
    std::string modelpath = std::string(env->GetStringUTFChars(model_path, JNI_FALSE),
                                        (size_t) env->GetStringLength(model_path));
    int scale = waifu2x->scale;
    int noise = waifu2x->noise;
    int prepadding = 0;
    if (modelpath.find("models-cunet") != std::string::npos)
    {
        if (noise == -1)
        {
            prepadding = 18;
        }
        else if (scale == 1)
        {
            prepadding = 28;
        }
        else if (scale & (scale - 1))
        {
            prepadding = 18;
        }
    }
    else if (modelpath.find("models-upconv_7_anime_style_art_rgb") != std::string ::npos
    || modelpath.find("models-upconv_7_photo") != std::string::npos)
    {
        prepadding = 7;
    }
    else
    {
        fprintf(stderr, "unknown model dir type\n");
        return;
    }
    waifu2x->prepadding = prepadding;
    waifu2x->load(env, asset_manager, parampath, modelpath);
}

extern "C"
JNIEXPORT void JNICALL
Java_pro_archiemeng_waifu2x_Waifu2x_RawProcess(JNIEnv *env, jobject thiz,
                                               jbyteArray in_buffer, jint in_width, jint in_height,
                                               jbyteArray out_buffer, jint out_width, jint out_height){
    jbyte* in_bytes = env->GetByteArrayElements(in_buffer, nullptr);
    jbyte* out_bytes = env->GetByteArrayElements(out_buffer, nullptr);
    const ncnn::Mat in = ncnn::Mat(in_width, in_height, (void*) in_bytes, (size_t) 4, 4);
    ncnn::Mat out = ncnn::Mat(out_width, out_height, (void*) out_bytes, (size_t) 4, 4);

    if (!gpu_mode) {
        // configure correct tilesize for cpu mode
        waifu2x->tilesize = in_width > in_height ? in_width : in_height;
    }
    waifu2x->process(in, out);
    env->ReleaseByteArrayElements(in_buffer, in_bytes, JNI_ABORT);
    env->ReleaseByteArrayElements(out_buffer, out_bytes, 0);
    __android_log_print(ANDROID_LOG_DEBUG, TAG, "process done");
}extern "C"
JNIEXPORT void JNICALL
Java_pro_archiemeng_waifu2x_Waifu2x_SetScale(JNIEnv *env, jobject thiz, jint scale) {
    waifu2x->scale = scale > 1 ? 2 : 1;
}extern "C"
JNIEXPORT void JNICALL
Java_pro_archiemeng_waifu2x_Waifu2x_SetNoise(JNIEnv *env, jobject thiz, jint noise) {
    waifu2x->noise = noise;
}extern "C"
JNIEXPORT jint JNICALL
Java_pro_archiemeng_waifu2x_Waifu2x_GetTileSize(JNIEnv *env, jobject thiz) {
    return waifu2x->tilesize;
}extern "C"
JNIEXPORT void JNICALL
Java_pro_archiemeng_waifu2x_Waifu2x_SetTileSize(JNIEnv *env, jobject thiz, jint tile_size) {
    if(tile_size >= 32) {
        waifu2x->tilesize = tile_size;
    } else {
        //Todo: Throw exception for incorrect tilesize
        __android_log_print(ANDROID_LOG_ERROR, TAG, "tile size too short: cannot be %d", tile_size);
    }
}extern "C"
JNIEXPORT jboolean JNICALL
Java_pro_archiemeng_waifu2x_Waifu2x_GetGPUMode(JNIEnv *env, jobject thiz) {
    return gpu_mode;
}