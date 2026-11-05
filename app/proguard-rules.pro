# Add project-specific ProGuard rules here.
# Keep Jsoup selectors intact in minified builds.
-keeppackagenames org.jsoup.**
-keep class org.jsoup.** { *; }
