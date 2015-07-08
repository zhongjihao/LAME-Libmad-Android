LOCAL_PATH := $(call my-dir)
 
include $(CLEAR_VARS)
 
LOCAL_MODULE    	:= tcpsocket

LOCAL_SRC_FILES 	:= \
	./jni_wrapper.cpp \
	./nativesocket/CMp3ReceiverThread.cpp \
	./nativesocket/CMp3SendThread.cpp

LOCAL_STATIC_LIBRARIES := baseclass

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
