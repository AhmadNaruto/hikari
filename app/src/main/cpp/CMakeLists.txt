cmake_minimum_required(VERSION 3.22.1)

project("hikari-image")

# Enable C++17 support
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

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
