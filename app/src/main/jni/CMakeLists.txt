# Copyright (C) 2021  ArchieMeng <archiemeng@protonmail.com>
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

project(Waifu2x)

cmake_minimum_required(VERSION 3.4.1)

include_directories(${CMAKE_CURRENT_BINARY_DIR})
# NCNN
if (USE_PREBUILT_NCNN)
    if (USE_SHARED_NCNN)
        add_library(ncnn SHARED IMPORTED)
        set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan-shared/${ANDROID_ABI}/lib/libncnn.so)
        include_directories(ncnn ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan-shared/${ANDROID_ABI}/include/ncnn)
    else ()
        set(ncnn_DIR ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/lib/cmake/ncnn)
        include_directories(.)
        find_package(ncnn REQUIRED)
        # GLSLANG
        set_target_properties(glslang PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/lib/libglslang.a)
        set_target_properties(OGLCompiler PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/lib/libOGLCompiler.a)
        set_target_properties(OSDependent PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/lib/libOSDependent.a)
        set_target_properties(SPIRV PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/ncnn-android-vulkan/${ANDROID_ABI}/lib/libSPIRV.a)
    endif ()

    if (NOT TARGET ncnn)
        message(WARNING "ncnn target not found! USE_PREBUILT_NCNN will be turned off.")
        set(USE_PREBUILT_NCNN OFF)
    endif ()
else ()
    # build ncnn library
    if (NOT EXISTS "${CMAKE_SOURCE_DIR}/ncnn/CMakeLists.txt")
        message(FATAL_ERROR "The submodules were not downloaded! Please update submodules with \"git submodule update --init --recursive\" and try again.")
    endif ()

    if (USE_SHARED_NCNN)
        option(NCNN_SHARED_LIB "" ON)
    endif ()

    add_subdirectory(ncnn)
endif ()

# waifu2x shader
macro(waifu2x_add_shader SHADER_SRC)
    get_filename_component(SHADER_SRC_NAME_WE ${SHADER_SRC} NAME_WE)
    set(SHADER_COMP_HEADER ${CMAKE_CURRENT_BINARY_DIR}/${SHADER_SRC_NAME_WE}.comp.hex.h)

    add_custom_command(
            OUTPUT ${SHADER_COMP_HEADER}
            COMMAND ${CMAKE_COMMAND} -DSHADER_SRC=${CMAKE_SOURCE_DIR}/waifu2x/${SHADER_SRC} -DSHADER_COMP_HEADER=${SHADER_COMP_HEADER} -P "${CMAKE_SOURCE_DIR}/waifu2x/generate_shader_comp_header.cmake"
            DEPENDS ${CMAKE_SOURCE_DIR}/waifu2x/${SHADER_SRC}
            COMMENT "Preprocessing shader source ${SHADER_SRC_NAME_WE}.comp"
            VERBATIM
    )
    set_source_files_properties(${SHADER_COMP_HEADER} PROPERTIES GENERATED TRUE)

    list(APPEND SHADER_SPV_HEX_FILES ${SHADER_COMP_HEADER})
endmacro()

# openmp
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fopenmp")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fopenmp")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -fopenmp")

if (DEFINED ANDROID_NDK_MAJOR AND ${ANDROID_NDK_MAJOR} GREATER 20)
    set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -static-openmp")
endif ()

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fomit-frame-pointer -fstrict-aliasing -ffast-math")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fomit-frame-pointer -fstrict-aliasing -ffast-math")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -fvisibility-inlines-hidden")

# disable rtti and exceptions
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fno-rtti -fno-exceptions")

# waifu2x add shader
waifu2x_add_shader(waifu2x_preproc.comp)
waifu2x_add_shader(waifu2x_postproc.comp)
waifu2x_add_shader(waifu2x_preproc_tta.comp)
waifu2x_add_shader(waifu2x_postproc_tta.comp)

add_custom_target(generate-spirv DEPENDS ${SHADER_SPV_HEX_FILES})

# Waifu2x link library
add_library(waifu2x SHARED waifu2x/waifu2x_jni.cpp waifu2x/waifu2x.cpp)
add_dependencies(waifu2x generate-spirv)
if (USE_PREBUILT_NCNN AND NOT USE_SHARED_NCNN)
    target_link_libraries(waifu2x
            ncnn

            glslang SPIRV OGLCompiler OSDependent

            android
            z
            log
            jnigraphics
            vulkan
            )
else ()
    target_link_libraries(waifu2x
            ncnn

            android
            z
            log
            jnigraphics
            vulkan
            )
endif ()