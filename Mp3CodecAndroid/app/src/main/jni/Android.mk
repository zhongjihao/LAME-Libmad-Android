######include $(call all-subdir-makefiles)

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

subdirs := $(LOCAL_PATH)/baseclass/Android.mk
subdirs += $(LOCAL_PATH)/mp3encoder/Android.mk
subdirs += $(LOCAL_PATH)/mp3decoder/Android.mk

include $(subdirs)
