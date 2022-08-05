# waifu2x-ncnn-vulkan-android

[中文版](README_CN.md)

## Introduction

This project is the first working version of Waifu2x ncnn Android app, which was initially developed
from [ncnn-android-deeplabv3plus](https://github.com/runrunrun1994/ncnn-android-deeplabv3plus) as a
template and reference the build method
from  [ncnn-android-yolov5](https://github.com/nihui/ncnn-android-yolov5). And it has been updated
with the latest JNI binding and build methods which is currently used by Waifu2x ncnn Android.

I hope that this project can be used as a reference by other developers to build more Android AI
apps on Waifu2x ncnn Vulkan, or even more ncnn projects.

## How to build

Just like other ncnn android demo projects, the only preparation is just about putting the
approperiate ncnn lib into app/src/main/jni . However, in Waifu2x ncnn Android, there are mainly two
options to choose from, and these result in different ncnn lib requirements. These two options are
CMake flags: **"USE_PREBUILT_NCNN"** and **"USE_SHARED_NCNN"**.

| \ |USE_PREBUILT_NCNN=ON (Default)|USE_PREBUILT_NCNN=OFF|
|---|---|---|
|**USE_SHARED_NCNN=ON**|ncnn-android-vulkan-shared|ncnn (git repo)|
|**USE_SHARED_NCNN=OFF (Default)**|ncnn-android-vulkan|ncnn (git repo)|

To choose the ncnn build type, modify the corresponding section in
the [app: build.gradle](app/build.gradle)

## Known issues

- Sometimes, build may not be successful. (.etc, waifu2x.h cannot found VkVulkan syntex).

## Screenshots

![img](img/screenshot.png)

## Aknowledgement

I feel very appreciated to the following people and projects.

- [nihui](https://github.com/nihui): Helped me to solve the critical issue in the JNI of Waifu2x
  ncnn Vulkan at the ever beginning of this project.
- [waifu2x-ncnn-vulkan](https://github.com/nihui/waifu2x-ncnn-vulkan): The original ncnn project of
  Waifu2x.
- [ncnn-android-yolov5](https://github.com/nihui/ncnn-android-yolov5): Provides the right way to use
  the current prebuilt ncnn library.
- [ncnn-android-deeplabv3plus](https://github.com/runrunrun1994/ncnn-android-deeplabv3plus): work as
  a template to build ncnn android app.