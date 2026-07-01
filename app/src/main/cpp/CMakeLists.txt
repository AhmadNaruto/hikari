cmake_minimum_required(VERSION 3.22.1)

project("hikari-image")

# Enable NEON support for armeabi-v7a (ARM64 has it enabled by default)
set(CMAKE_ANDROID_ARM_NEON TRUE)

# Enable C++17 support
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include directories for libvips and glib
set(VIPS_INCLUDE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/include)
set(GLIB_INCLUDE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/include/glib-2.0)

# Directory containing the precompiled libraries
set(VIPS_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# Import precompiled libvips and its dependencies
add_library(vips SHARED IMPORTED)
set_target_properties(vips PROPERTIES IMPORTED_LOCATION ${VIPS_LIB_DIR}/libvips.so)

add_library(glib-2.0 SHARED IMPORTED)
set_target_properties(glib-2.0 PROPERTIES IMPORTED_LOCATION ${VIPS_LIB_DIR}/libglib-2.0.so)

add_library(gobject-2.0 SHARED IMPORTED)
set_target_properties(gobject-2.0 PROPERTIES IMPORTED_LOCATION ${VIPS_LIB_DIR}/libgobject-2.0.so)

add_library(gio-2.0 SHARED IMPORTED)
set_target_properties(gio-2.0 PROPERTIES IMPORTED_LOCATION ${VIPS_LIB_DIR}/libgio-2.0.so)

add_library(hikari-image SHARED 
        hikari-image-pipeline.cpp
        vips_jni.cpp
)

if(NOT MSVC)
    target_compile_options(hikari-image PRIVATE -O3 -ffast-math)
endif()

find_library(jnigraphics-lib jnigraphics)
find_library(log-lib log)
find_library(android-lib android)

target_include_directories(hikari-image PRIVATE
        ${VIPS_INCLUDE_DIR}
        ${GLIB_INCLUDE_DIR}
)

target_link_libraries(hikari-image
        ${jnigraphics-lib}
        ${log-lib}
        ${android-lib}
        vips
        glib-2.0
        gobject-2.0
        gio-2.0
)

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
