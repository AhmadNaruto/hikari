cmake_minimum_required(VERSION 3.22.1)

project("hikari-image")

# Enable NEON support for armeabi-v7a (ARM64 has it enabled by default)
set(CMAKE_ANDROID_ARM_NEON TRUE)

# Enable C++17 support
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for high-performance math calculations (critical for AVIR/LANCIR)
if(NOT MSVC)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math")
endif()

find_library(jnigraphics-lib jnigraphics)
find_library(log-lib log)
find_library(android-lib android)

add_library(hikari-image SHARED hikari-image-pipeline.cpp)

target_link_libraries(hikari-image
        ${jnigraphics-lib}
        ${log-lib}
        ${android-lib})

# Add libbbf JNI library target
add_library(bbfjni SHARED
        bbf/vend/xxhash.c
        bbf/bbfcodec.cpp
        bbf/muxer/dedupemap.cpp
        bbf/muxer/stringpool.cpp
        bbf/bbfjni.cpp
)

target_include_directories(bbfjni PRIVATE
        bbf
        bbf/vend
        bbf/muxer
)

target_link_libraries(bbfjni PRIVATE
        ${log-lib}
)
