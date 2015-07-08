LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	./mp3decoder/mad/version.c \
	./mp3decoder/mad/fixed.c \
	./mp3decoder/mad/bit.c \
	./mp3decoder/mad/timer.c \
	./mp3decoder/mad/stream.c \
	./mp3decoder/mad/frame.c  \
	./mp3decoder/mad/synth.c \
	./mp3decoder/mad/decoder.c \
	./mp3decoder/mad/layer12.c \
	./mp3decoder/mad/layer3.c \
	./mp3decoder/mad/huffman.c \
	./jni_mad_mp3_wrap.cpp \
	./mp3decoder/NativeFileOperator.c \
	./mp3decoder/NativeMP3Decoder.c 

LOCAL_ARM_MODE := arm

LOCAL_MODULE:= mp3decoder

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/android 

LOCAL_CFLAGS := -DHAVE_CONFIG_H -DFPM_ARM -ffast-math -O3

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
