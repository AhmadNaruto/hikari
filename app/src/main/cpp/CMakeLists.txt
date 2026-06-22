cmake_minimum_required(VERSION 3.22.1)

project("hikari-image")

# Wajibkan C++17
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

add_library(bbfjni SHARED
    xxhash.c
    bbfcodec.cpp
    dedupemap.cpp
    stringpool.cpp
    bbfjni.cpp
)

target_include_directories(bbfjni PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_link_libraries(bbfjni PRIVATE ${log-lib})

