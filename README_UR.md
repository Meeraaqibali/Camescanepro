# Cam Scan Pro — Android project

یہ ایک ابتدائی native Android project ہے، ویب سائٹ نہیں۔

## موجودہ فیچرز
- Camera سے تصویر لینا
- موبائل سے متعدد تصاویر import کرنا
- کئی صفحات کو ایک PDF میں تبدیل کرنا
- PDF کو Downloads/CamScanPro میں محفوظ کرنا (Android 10+)
- پہلے صفحے سے OCR کے ذریعے متن نکالنا اور share کرنا
- منتخب صفحات کو remove یا clear کرنا

## APK بنانے کا طریقہ
1. کمپیوٹر میں Android Studio انسٹال کریں۔
2. اس ZIP کو extract کریں۔
3. Android Studio میں `CamScanPro` فولڈر کھولیں۔
4. Gradle sync مکمل ہونے دیں۔
5. فون کو USB debugging کے ذریعے جوڑیں یا Android Emulator چلائیں۔
6. `Build > Build APK(s)` منتخب کریں۔
7. APK عام طور پر `app/build/outputs/apk/debug/app-debug.apk` میں بنے گا۔

## اہم بات
اس ماحول میں Android SDK/Gradle build setup موجود نہیں، اس لیے یہاں سے تیار APK build نہیں کیا گیا؛ ZIP میں source project ہے۔ یہ CamScanner کی نقل نہیں بلکہ اس سے متاثر ایک الگ نام اور ابتدائی implementation ہے۔ اس ورژن میں advanced automatic edge detection/crop، perspective correction، filters، password-protected PDF اور searchable multi-page OCR ابھی شامل نہیں۔ انہیں اگلے ورژن میں شامل کیا جا سکتا ہے۔
