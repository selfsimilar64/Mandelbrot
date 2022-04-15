JNI_PATH := $(call my-dir)

## native-reference
#GMP_WITH_CPLUSPLUS := yes
#include $(JNI_PATH)/gmp/Android.mk
#LOCAL_PATH := $(JNI_PATH)
#include $(CLEAR_VARS)
#LOCAL_MODULE := native-reference
#LOCAL_SRC_FILES := $(JNI_PATH)/../cpp/native-reference.cpp
#LOCAL_CPP_FEATURES += exceptions
#LOCAL_LDLIBS += -llog
#LOCAL_SHARED_LIBRARIES := gmp
#include $(BUILD_SHARED_LIBRARY)
#
## native-mandelbrot
#LOCAL_PATH := $(JNI_PATH)
#include $(CLEAR_VARS)
#LOCAL_MODULE := native-mandelbrot
#LOCAL_CFLAGS += -fopenmp -static-openmp
#LOCAL_LDFLAGS += -fopenmp -static-openmp
#LOCAL_SRC_FILES := $(JNI_PATH)/../cpp/native-mandelbrot.cpp $(JNI_PATH)/../cpp/native-sine1.cpp
#LOCAL_CPP_FEATURES += exceptions
#LOCAL_LDLIBS += -llog
#include $(BUILD_SHARED_LIBRARY)

# native-reference
GMP_WITH_CPLUSPLUS := yes
include $(JNI_PATH)/gmp/Android.mk
LOCAL_PATH := $(JNI_PATH)
include $(CLEAR_VARS)
LOCAL_MODULE := native-fractalimage
LOCAL_SRC_FILES := \
$(JNI_PATH)/../cpp/native-yuv.cpp \
$(JNI_PATH)/../cpp/toojpeg.cpp \
$(JNI_PATH)/../cpp/native-compress.cpp \
$(JNI_PATH)/../cpp/native-reference.cpp \
$(JNI_PATH)/../cpp/native-sine2.cpp
LOCAL_CPP_FEATURES += exceptions
LOCAL_LDLIBS += -llog
LOCAL_SHARED_LIBRARIES := gmp
LOCAL_CFLAGS += -DNDEBUG
include $(BUILD_SHARED_LIBRARY)