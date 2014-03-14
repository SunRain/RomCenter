LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305 libGoogleAnalyticsV2 nineoldandroids-library-2.4.0.jar

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src)
        
LOCAL_SRC_FILES += $(call all-java-files-under, ../ResideMenu/src )

LOCAL_PACKAGE_NAME := RomCenter-mk
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_ENABLED := full
TARGET_BUILD_VARIANT := userdebug
LOCAL_PROGUARD_FLAG_FILES := proguard-android.txt

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS := --extra-packages com.special.ResideMenu \
                    --auto-add-overlay

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_RESOURCE_DIR += $(LOCAL_PATH)/../ResideMenu/res


include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))
