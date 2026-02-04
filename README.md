# Kartal Istoc WebView Android Application

Bu proje, `www.kartalistoc.com` sitesini bir Android uygulaması içinde görüntülemek için oluşturulmuş basit bir WebView uygulamasıdır.

## Kurulum ve Kullanım

1.  **Android Studio**'yu açın.
2.  **File > Open** (Dosya > Aç) menüsüne gidin.
3.  Bu klasörü (`c:\Users\Kartal\Desktop\android`) seçin ve projeyi içe aktarın.
4.  Gradle senkronizasyonunun tamamlanmasını bekleyin.
5.  Uygulamayı bir emülatörde veya gerçek bir cihazda çalıştırın.

## Özellikler

*   **Tam Ekran WebView**: Web sitesi tüm ekranı kaplar.
*   **JavaScript Desteği**: Sitedeki interaktif özelliklerin çalışması için JS etkindir.
*   **Geri Tuşu Yönetimi**: Android geri tuşuna basıldığında uygulama kapanmaz, web sitesinde bir önceki sayfaya döner.
*   **DOM Storage**: Modern web sitelerinin düzgün çalışması için gerekli depolama alanı etkindir.

## Dosya Yapısı

*   `app/src/main/AndroidManifest.xml`: Uygulama izinleri (İnternet) ve temel ayarlar.
*   `app/src/main/res/layout/activity_main.xml`: Kullanıcı arayüzü tasarımı (WebView bileşeni).
*   `app/src/main/java/com/kartalistoc/webview/MainActivity.java`: Web sitesini yükleyen ve ayarları yöneten Java kodu.
*   `build.gradle`: Proje bağımlılıkları ve uygulama kimliği.
