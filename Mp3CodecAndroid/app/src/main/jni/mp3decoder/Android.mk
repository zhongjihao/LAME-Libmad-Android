LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	./mad/version.c \
	./mad/fixed.c \
	./mad/bit.c \
	./mad/timer.c \
	./mad/stream.c \
	./mad/frame.c  \
	./mad/synth.c \
	./mad/decoder.c \
	./mad/layer12.c \
	./mad/layer3.c \
	./mad/huffman.c \
	./jni_mad_mp3_wrap.cpp \
	./NativeFileOperator.c \
	./NativeMP3Decoder.c

LOCAL_ARM_MODE := arm

LOCAL_MODULE:= mp3decoder

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/android 

#LOCAL_CFLAGS := -DHAVE_CONFIG_H -DFPM_ARM -ffast-math -O3

LOCAL_CFLAGS := -DHAVE_CONFIG_H -DFPM_DEFAULT

LOCAL_STATIC_LIBRARIES := baseclass

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
