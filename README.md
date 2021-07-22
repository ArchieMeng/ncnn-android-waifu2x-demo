# waifu2x-ncnn-vulkan-android

This project is developed from the template [ncnn-android-deeplabv3plus](https://github.com/runrunrun1994/ncnn-android-deeplabv3plus) and reference the build method from  [ncnn-android-yolov5](https://github.com/nihui/ncnn-android-yolov5)

[Waifu2x-ncnn-vulkan](https://github.com/nihui/waifu2x-ncnn-vulkan) is a waifu2x converter [ncnn](https://github.com/Tencent/ncnn) version, runs fast on intel / amd / nvidia GPU with vulkan.

## how to build and run
### step1
https://github.com/Tencent/ncnn/releases

download ncnn-android-vulkan.zip or build ncnn for android

### step2
extract ncnn-android-vulkan.zip into app/src/main/jni or change the ncnn path to yours in app/src/main/jni/CMakeLists.txt

### step3
open this project with Android Studio, build it and enjoy!

## Result(Device:kirin810) // Todo
|Model|CPU|GPU|
|---|---|---|
|deeplabv3+|**434.59**ms|**454.16**ms|


|Original|Result|
|---|---|
|![org1](https://github.com/runrunrun1994/Image/blob/main/PersonSegmeantation/Android/pexels-photo-824109.jpg) |![res1](https://github.com/runrunrun1994/Image/blob/main/PersonSegmeantation/Android/pexels-photo-824109.png)|