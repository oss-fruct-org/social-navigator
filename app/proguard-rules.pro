# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/ivashov/android/android-studio/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class org.xmlpull.v1.** { *; }

# osmdroid
-dontwarn org.apache.http.entity.mime.**

# graphhopper
-dontwarn javax.xml.stream.**
-dontwarn org.apache.xmlgraphics.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn org.apache.avalon.**
-dontwarn org.apache.log4j.**
-dontwarn org.slf4j.impl.**
-dontwarn org.openstreetmap.osmosis.**
-dontwarn com.google.protobuf.**
-dontwarn java.lang.management.**
-dontwarn com.sun.management.**
-dontwarn org.apache.log.**
-dontwarn sun.misc.Unsafe


-dontwarn org.xmlpull.v1.XmlPullParser
-dontwarn org.xmlpull.v1.XmlSerializer
-dontwarn com.caverock.androidsvg.**