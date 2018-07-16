# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

#项目路径和opencv路径

set(pathToProject D:/ACam)
set(pathToOpenCv D:/openCV/OpenCV-android-sdk)
#支持-std=gnu++11
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
#配置加载native依赖
include_directories(${pathToOpenCv}/sdk/native/jni/include)
#动态方式加载
add_library( lib_opencv SHARED IMPORTED )

#引入libopencv_java3.so文件
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathToProject}/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so)

