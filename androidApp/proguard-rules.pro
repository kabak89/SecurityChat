-keep class org.webrtc.** { *; }
-keepclasseswithmembers,includedescriptorclasses class org.jni_zero.** {
    @org.jni_zero.CalledByNative* <methods>;
}
