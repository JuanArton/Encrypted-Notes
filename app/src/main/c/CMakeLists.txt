cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
        native-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ../cpp/native-lib.cpp)

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the NDK library that you want to link.
        log )

target_link_libraries( # Specifies the target library.
        native-lib

        # Links the log library to your native library.
        ${log-lib} )
