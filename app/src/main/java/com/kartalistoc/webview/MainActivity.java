package com.kartalistoc.webview;

import android.Manifest;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import com.google.firebase.messaging.FirebaseMessaging;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.DownloadListener;
import android.app.DownloadManager;
import android.os.Environment;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import android.content.res.Configuration;
import com.bumptech.glide.Glide;
import android.print.PrintManager;
import android.print.PrintAttributes;
import org.json.JSONObject;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout scannerContainer;
    private DecoratedBarcodeView barcodeScanner;
    private ImageButton btnCloseScanner;
    private FrameLayout splashOverlay;
    private ImageView splashLogo;

    // Listeler (İsim değişti) Paneli UI
    private androidx.cardview.widget.CardView invoicesPanel;
    private LinearLayout invoicesListContainer;
    private ProgressBar invoicesLoading;
    private TextView txtInvoicesError;
    private ImageButton btnCloseInvoices;
    private TextView txtPanelTitle;
    private LinearLayout txtInvoicesErrorArea;

    // İbico Paneli UI
    private androidx.cardview.widget.CardView ibicoPanel;
    private androidx.recyclerview.widget.RecyclerView ibicoProductGrid;
    private ProgressBar ibicoLoading;
    private ImageButton btnCloseIbico;

    // Üst Bar UI
    private RelativeLayout topBar;
    private TextView txtCustomerName, txtSalespersonName, txtTopBalance;
    private ImageButton btnRefreshBalance;

    private boolean isLoggedIn = false;
    private ProductAdapter ibicoAdapter;
    private java.util.List<Product> ibicoProducts = new java.util.ArrayList<>();
    private int ibicoPage = 1;
    private boolean isLoadingProducts = false;
    private boolean hasMoreProducts = true;
    
    private static final String URL = "https://kartalistoc.com";
    private static final int CAMERA_PERMISSION_CODE = 100;
    
    private float dX, dY;
    private int lastAction;
    private SharedPreferences prefs;
    private boolean isScannerActive = false;
    
    private ValueCallback<Uri[]> uploadMessage;
    private ActivityResultLauncher<Intent> fileChooserLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("KartalIstoc", MODE_PRIVATE);
        
        // İlk açılışta izinleri iste
        checkAndRequestPermissions();

        webView = findViewById(R.id.webview);
        swipeRefreshLayout = findViewById(R.id.swipeContainer);
        scannerContainer = findViewById(R.id.scannerContainer);
        barcodeScanner = findViewById(R.id.barcodeScanner);
        btnCloseScanner = findViewById(R.id.btnCloseScanner);
        splashOverlay = findViewById(R.id.splashOverlay);
        splashLogo = findViewById(R.id.splashLogo);
        // bottomNavigation referansı yeni butonlar için toplu olarak kullanılıyor
        
        // Alt menü ve diğer UI bileşenleri setupNavigation() içinde kurulur.

        // Listeler Paneli
        invoicesPanel = findViewById(R.id.invoicesPanel);
        invoicesListContainer = findViewById(R.id.invoicesListContainer);
        invoicesLoading = findViewById(R.id.invoicesLoading);
        txtPanelTitle = findViewById(R.id.txtPanelTitle);
        txtInvoicesError = findViewById(R.id.txtInvoicesError);
        btnCloseInvoices = findViewById(R.id.btnCloseInvoices);
        txtInvoicesErrorArea = findViewById(R.id.txtInvoicesErrorArea);

        // İbico Paneli
        ibicoPanel = findViewById(R.id.ibicoPanel);
        ibicoProductGrid = findViewById(R.id.ibicoProductGrid);
        ibicoLoading = findViewById(R.id.ibicoLoading);
        btnCloseIbico = findViewById(R.id.btnCloseIbico);

        // Üst Bar
        topBar = findViewById(R.id.topBar);
        txtCustomerName = findViewById(R.id.txtCustomerName);
        txtSalespersonName = findViewById(R.id.txtSalespersonName);
        txtTopBalance = findViewById(R.id.txtTopBalance);
        btnRefreshBalance = findViewById(R.id.btnRefreshBalance);

        btnCloseInvoices.setOnClickListener(v -> hideInvoicesPanel());
        btnCloseIbico.setOnClickListener(v -> hideIbicoPanel());
        btnRefreshBalance.setOnClickListener(v -> fetchTopBarData());

        // Panel çekerek kapatma (Drag down to close)
        setupPanelDragToClose();
        
        setupNavigation();

        // Ödeme Butonu
        View btnPayment = findViewById(R.id.btnPayment);
        if (btnPayment != null) {
            btnPayment.setOnClickListener(v -> {
                webView.loadUrl("https://kartalistoc.com/b2b/account/open_payment");
            });
        }

        // Dosya seçici başlatıcı
        fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (uploadMessage == null) return;
                    Uri[] results = null;
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String dataString = result.getData().getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                    uploadMessage.onReceiveValue(results);
                    uploadMessage = null;
                });

        setupWebView();
        setupSwipeRefresh();
        setupInlineScanner();
        
        // BİLDİRİM YÖNLENDİRMESİ
        String targetUrl = URL;
        if (getIntent() != null && getIntent().getExtras() != null) {
            String notificationUrl = getIntent().getExtras().getString("url");
            if (notificationUrl != null && !notificationUrl.isEmpty()) {
                targetUrl = notificationUrl;
            }
        }
        
        webView.loadUrl(targetUrl);

        // Güvenlik Zaman Aşımı: Site 10 saniye içinde yüklenmezse splash'i zorla kapat
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (splashOverlay != null && splashOverlay.getVisibility() == View.VISIBLE) {
                splashOverlay.animate()
                        .alpha(0f)
                        .setDuration(1000)
                        .withEndAction(() -> splashOverlay.setVisibility(View.GONE))
                        .start();
            }
        }, 10000);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void checkAndRequestPermissions() {
        java.util.List<String> permissionsNeeded = new java.util.ArrayList<>();
        
        // Android 13+ için bildirim izni
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        // Kamera izni (Barkod için önceden almak kullanıcı deneyimini iyileştirir)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (!permissionsNeeded.isEmpty()) {
            boolean isFirstRun = prefs.getBoolean("first_run_permissions", true);
            if (isFirstRun) {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("İzin Gerekiyor")
                    .setMessage("Uygulamamızın size bildirim gönderebilmesi ve barkod tarayıcıyı kullanabilmesi için gerekli izinleri onaylamanız gerekmektedir.")
                    .setCancelable(false)
                    .setPositiveButton("Anladım", (dialog, which) -> {
                        prefs.edit().putBoolean("first_run_permissions", false).apply();
                        ActivityCompat.requestPermissions(MainActivity.this, 
                            permissionsNeeded.toArray(new String[0]), 101);
                    })
                    .show();
            } else {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), 101);
            }
        }
    }

    private void showNoInternetDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Bağlantı Hatası")
            .setMessage("İnternet bağlantınız bulunmuyor. Lütfen kontrol edip tekrar deneyin.")
            .setCancelable(false)
            .setPositiveButton("Tekrar Dene", (dialog, which) -> {
                if (isNetworkAvailable()) {
                    webView.reload();
                } else {
                    showNoInternetDialog();
                }
            })
            .show();
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }
        
        applyDarkMode();
        
        // Android-JS Köprüsü
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        // MIXED CONTENT (HTTP/HTTPS) İzni - Beyaz ekranı önler
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            
            // ÇEREZ YÖNETİMİ (Login kalıcı olsun)
            android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.flush();
            }
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return handleUri(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (splashOverlay != null && splashOverlay.getVisibility() == View.VISIBLE) {
                    splashOverlay.setVisibility(View.GONE);
                }
                // Sadece ana sayfa hatalarında offline sayfasını göster
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    if (request.isForMainFrame()) {
                        view.loadUrl("file:///android_asset/offline.html");
                        showNoInternetDialog();
                    }
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                swipeRefreshLayout.setRefreshing(false);
                
                // Canlı Giriş Kontrolü Enjeksiyonu (Daha kapsamlı ve güvenilir)
                String liveDetectJs = "(function() {" +
                        "  var checkStatus = function() {" +
                        "    var isLoggedIn = (typeof b2bSignedIn === 'function') ? b2bSignedIn() : !!document.querySelector('li.nav-item.dropdown a#drop1');" +
                        "    Android.onStateChanged(isLoggedIn);" +
                        "    if (isLoggedIn && typeof getB2bUserData === 'function') {" +
                        "      var user = getB2bUserData();" +
                        "      if (user) {" +
                        "        Android.onUserDataReceived(JSON.stringify(user));" +
                        "        if (user.balance_data && user.balance_data.bakiye) {" +
                        "          Android.onBalanceReceived(user.balance_data.bakiye);" +
                        "        }" +
                        "      }" +
                        "    }" +
                        "  };" +
                        "  checkStatus();" +
                        "  var observer = new MutationObserver(checkStatus);" +
                        "  observer.observe(document.body, { childList: true, subtree: true });" +
                        "})();";
                webView.evaluateJavascript(liveDetectJs, null);

                // Cookie sync
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    android.webkit.CookieManager.getInstance().flush();
                }

                // Splash ekranı hala görünürse, yumuşakça kapat
                if (splashOverlay != null && splashOverlay.getVisibility() == View.VISIBLE) {
                    splashOverlay.animate()
                            .alpha(0f)
                            .setDuration(600)
                            .withEndAction(() -> splashOverlay.setVisibility(View.GONE))
                            .start();
                }

                // Çerezleri diske yazmayı zorla
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    android.webkit.CookieManager.getInstance().flush();
                }

                // Sayfa yüklendiğinde kullanıcı adı ve bakiyeyi çek
                updateUserInfoAndBalance();

                // --- CANLI SEPET SİSTEMİ ENJEKSİYONU ---
                injectCartSystem();
            }

            
            // CRASH KORUMASI: Web motoru çökerse (beyaz ekran) uygulamayı kapatma, sayfayı yeniden yükle
            @Override
            public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                if (detail.didCrash()) {
                    // Renderer göçtü, sayfayı yeniden yüklemeyi dene (Sadece bir kez)
                    Toast.makeText(MainActivity.this, "Sistem yenileniyor...", Toast.LENGTH_SHORT).show();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> recreate(), 1000);
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(view, url);
            }

            private boolean handleUri(WebView view, String url) {
                if (url == null) return false;

                // Offline sayfasındaki butona izin ver
                if (url.equals("file:///android_asset/offline.html")) {
                    return false;
                }

                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false; // Standart linkleri WebView açsın
                }

                // WhatsApp, Tel, Mailto vb. linkleri dışarı aktar
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return true; // Uygulama bulunamazsa hata verme
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // Progress bar kaldırıldı
            }

            // Dosya yükleme desteği (Android 5.0+)
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                }
                uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    fileChooserLauncher.launch(intent);
                } catch (Exception e) {
                    uploadMessage = null;
                    return false;
                }
                return true;
            }
        });
        
        // Dosya İndirme Desteği
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                String cookies = android.webkit.CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", userAgent);
                request.setDescription("Dosya indiriliyor...");
                request.setTitle(android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, android.webkit.URLUtil.guessFileName(url, contentDisposition, mimetype));
                
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Dosya indiriliyor...", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());
        
        // Sadece sayfa en tepedeyken yenilemeye izin ver (Çakışmayı önler)
        webView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            swipeRefreshLayout.setEnabled(webView.getScrollY() == 0);
        });
    }

    private void setupInlineScanner() {
        // Kamera ayarlarını optimize et
        com.journeyapps.barcodescanner.camera.CameraSettings settings = new com.journeyapps.barcodescanner.camera.CameraSettings();
        settings.setRequestedCameraId(0); // Arka kamera
        settings.setAutoFocusEnabled(true);
        settings.setContinuousFocusEnabled(true);
        barcodeScanner.getBarcodeView().setCameraSettings(settings);

        // Barkod okunduğunda callback
        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null && isScannerActive) {
                    // Titreşim (Gelişmiş Hissiyat)
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(100);
                        }
                    }
                    
                    // Sonucu işle
                    String barcode = result.getText();
                    hideScanner();
                    insertBarcodeToSearch(barcode);
                    
                    Toast.makeText(MainActivity.this, "✓ " + barcode, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Kapatma butonu
        btnCloseScanner.setOnClickListener(v -> hideScanner());
    }

    private void showScanner() {
        isScannerActive = true;
        
        // Animasyon yok - ANINDA AÇILIS
        scannerContainer.setVisibility(View.VISIBLE);
        scannerContainer.setScaleX(1f);
        scannerContainer.setScaleY(1f);
        scannerContainer.setAlpha(1f);
        
        barcodeScanner.resume();
    }

    private void hideScanner() {
        isScannerActive = false;
        barcodeScanner.pause();
        
        // Animasyon yok - ANINDA KAPANIŞ
        scannerContainer.setVisibility(View.GONE);
    }

    private void setupNavigation() {
        View btnNavHome = findViewById(R.id.btnNavHome);
        View btnNavIbico = findViewById(R.id.btnNavIbico);
        View btnNavBarcode = findViewById(R.id.btnNavBarcode);
        View btnNavLists = findViewById(R.id.btnNavLists);
        View btnNavCart = findViewById(R.id.btnNavCart);

        btnNavHome.setOnClickListener(v -> {
            if (webView.getUrl() == null || !webView.getUrl().equals(URL)) {
                webView.loadUrl(URL);
            }
            updateNavColors(R.id.btnNavHome);
        });

        btnNavIbico.setOnClickListener(v -> {
            String url = "https://kartalistoc.com/urunlerimiz-c6149";
            if (webView.getUrl() == null || !webView.getUrl().contains(url)) {
                webView.loadUrl(url);
            }
            updateNavColors(R.id.btnNavIbico);
        });

        btnNavLists.setOnClickListener(v -> {
            webView.evaluateJavascript("(function() { return (typeof b2bSignedIn === 'function') ? b2bSignedIn() : !!document.querySelector('li.nav-item.dropdown a#drop1'); })();", value -> {
                boolean actuallyLoggedIn = "true".equalsIgnoreCase(value);
                if (actuallyLoggedIn) {
                    isLoggedIn = true;
                    showInvoicesPanel();
                    updateNavColors(R.id.btnNavLists);
                    updateUIVisibility();
                } else {
                    isLoggedIn = false;
                    updateUIVisibility();
                    Toast.makeText(this, "Bu özelliği kullanmak için giriş yapmanız gerekmektedir.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnNavCart.setOnClickListener(v -> {
            webView.evaluateJavascript("if(typeof openCartPanel === 'function') openCartPanel();", null);
            updateNavColors(R.id.btnNavCart);
        });

        btnNavBarcode.setOnClickListener(v -> checkCameraAndScan());
        
        updateUIVisibility();
    }

    private void updateNavColors(int activeId) {
        ImageView imgHome = findViewById(R.id.imgNavHome);
        TextView txtHome = findViewById(R.id.txtNavHome);
        ImageView imgIbico = findViewById(R.id.imgNavIbico);
        TextView txtIbico = findViewById(R.id.txtNavIbico);
        ImageView imgLists = findViewById(R.id.imgNavLists);
        TextView txtLists = findViewById(R.id.txtNavLists);
        ImageView imgCart = findViewById(R.id.imgNavCart);
        TextView txtCartTotal = findViewById(R.id.txtNavCartTotal);

        int activeColor = Color.parseColor("#1976D2");
        int passiveColor = Color.parseColor("#424242");
        int disabledColor = Color.parseColor("#BDBDBD");

        if (imgHome != null) imgHome.setColorFilter(activeId == R.id.btnNavHome ? activeColor : passiveColor);
        if (txtHome != null) txtHome.setTextColor(activeId == R.id.btnNavHome ? activeColor : passiveColor);
        
        if (imgIbico != null) imgIbico.setColorFilter(activeId == R.id.btnNavIbico ? activeColor : passiveColor);
        if (txtIbico != null) txtIbico.setTextColor(activeId == R.id.btnNavIbico ? activeColor : passiveColor);

        if (imgLists != null) {
            if (isLoggedIn) {
                imgLists.setColorFilter(activeId == R.id.btnNavLists ? activeColor : passiveColor);
            } else {
                imgLists.setColorFilter(disabledColor);
            }
        }
        if (txtLists != null) {
            if (isLoggedIn) {
                txtLists.setTextColor(activeId == R.id.btnNavLists ? activeColor : passiveColor);
            } else {
                txtLists.setTextColor(disabledColor);
            }
        }

        if (imgCart != null) imgCart.setColorFilter(activeId == R.id.btnNavCart ? activeColor : passiveColor);
        if (txtCartTotal != null) txtCartTotal.setTextColor(activeId == R.id.btnNavCart ? activeColor : passiveColor);
    }


    private void checkCameraAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            if (isScannerActive) {
                hideScanner();
            } else {
                showScanner();
            }
        } else {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_CODE);
        }
    }

    private void insertBarcodeToSearch(String barcode) {
        // Javascript injection YERİNE doğrudan arama sayfasına git
        if (barcode != null && !barcode.isEmpty()) {
            String searchUrl = "https://kartalistoc.com/search?query=" + barcode;
            webView.loadUrl(searchUrl);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showScanner();
            } else {
                Toast.makeText(this, "Barkod taramak için kamera izni gerekli!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isScannerActive) {
            barcodeScanner.resume();
        }
        // Uygulamaya dönüldüğünde login durumunu tazele (Hataları önler)
        if (webView != null) {
            webView.evaluateJavascript("(function() { return (typeof b2bSignedIn === 'function') ? b2bSignedIn() : !!document.querySelector('li.nav-item.dropdown a#drop1'); })();", value -> {
                isLoggedIn = "true".equalsIgnoreCase(value);
                updateUIVisibility();
                if (isLoggedIn) updateUserInfoAndBalance();
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScanner.pause();
    }

    @Override
    public void onBackPressed() {
        if (invoicesPanel.getVisibility() == View.VISIBLE) {
            hideInvoicesPanel();
        } else if (isScannerActive) {
            hideScanner();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // --- FATURA LİSTESİ MANTIĞI ---

    private void updateUIVisibility() {
        if (topBar != null) {
            topBar.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        }
        
        ImageView imgNavLists = findViewById(R.id.imgNavLists);
        TextView txtNavLists = findViewById(R.id.txtNavLists);
        
        if (imgNavLists != null && txtNavLists != null) {
            // Aktif renk #1976D2 (Mavi), Pasif #424242, Devre Dışı #BDBDBD
            int color = isLoggedIn ? Color.parseColor("#424242") : Color.parseColor("#BDBDBD");
            imgNavLists.setColorFilter(color);
            txtNavLists.setTextColor(color);
        }
        
        // Login durumunda çerezleri hemen kaydet
        if (isLoggedIn) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                android.webkit.CookieManager.getInstance().flush();
            }
        }
    }

    private void fetchTopBarData() {
        String js = "(function() {" +
                "  if (typeof getB2bUserData === 'function') {" +
                "    var user = getB2bUserData();" +
                "    if (user) {" +
                "      Android.onUserDataReceived(JSON.stringify(user));" +
                "      if (user.balance_data && user.balance_data.bakiye) {" +
                "        Android.onBalanceReceived(user.balance_data.bakiye);" +
                "      }" +
                "    }" +
                "  } else {" +
                "    fetch('/api/tr/v1/data/blocks/3-1-2.json').then(r => r.json()).then(d => {" +
                "      Android.onUserDataReceived(JSON.stringify(d.b2b_user_data));" +
                "      if (d.balance_data) Android.onBalanceReceived(d.balance_data.bakiye);" +
                "    }).catch(e => console.log('Fetch error'));" +
                "  }" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    private void showInvoicesPanel() {
        txtPanelTitle.setText("Listelerim");
        invoicesPanel.setVisibility(View.VISIBLE);
        invoicesPanel.setTranslationY(1000f);
        invoicesPanel.animate().translationY(0).setDuration(300).start();
        
        fetchInvoicesFromWeb();
    }

    private void hideInvoicesPanel() {
        invoicesPanel.animate().translationY(1000f).setDuration(300).withEndAction(() -> {
            invoicesPanel.setVisibility(View.GONE);
        }).start();
    }

    private void showIbicoPanel() {
        ibicoPanel.setVisibility(View.VISIBLE);
        ibicoPanel.setTranslationY(1000f);
        ibicoPanel.animate().translationY(0).setDuration(300).start();

        if (ibicoAdapter == null) {
            setupIbicoGrid();
            fetchIbicoProducts(1);
        }
    }

    private void hideIbicoPanel() {
        ibicoPanel.animate().translationY(1000f).setDuration(300).withEndAction(() -> {
            ibicoPanel.setVisibility(View.GONE);
        }).start();
    }

    private void setupIbicoGrid() {
        ibicoProductGrid.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 2));
        ibicoAdapter = new ProductAdapter(ibicoProducts);
        ibicoProductGrid.setAdapter(ibicoAdapter);

        ibicoProductGrid.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(1) && !isLoadingProducts && hasMoreProducts) {
                    fetchIbicoProducts(++ibicoPage);
                }
            }
        });
    }

    private void setupPanelDragToClose() {
        View dragHandle = findViewById(R.id.dragHandle);
        if (dragHandle == null) return;

        dragHandle.setOnTouchListener(new View.OnTouchListener() {
            private float initialY, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialY = invoicesPanel.getTranslationY();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float diff = event.getRawY() - initialTouchY;
                        if (diff > 0) {
                            invoicesPanel.setTranslationY(initialY + diff);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        float finalDiff = event.getRawY() - initialTouchY;
                        if (finalDiff > 200) {
                            hideInvoicesPanel();
                        } else {
                            invoicesPanel.animate().translationY(0).setDuration(200).start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void fetchIbicoProducts(int page) {
        isLoadingProducts = true;
        ibicoLoading.setVisibility(View.VISIBLE);
        
        String js = "(function() {" +
                "  var url = '/api/tr/v1/layouts/b2b/products/search.json?brand_ids[]=467&page[number]=" + page + "&page[size]=10';" +
                "  fetch(url).then(r => r.json()).then(d => {" +
                "    Android.onProductsReceived(JSON.stringify(d));" +
                "  }).catch(e => Android.onProductsReceived('{}'));" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    private void handleProductsReceived(String json) {
        isLoadingProducts = false;
        ibicoLoading.setVisibility(View.GONE);
        try {
            org.json.JSONObject root = new org.json.JSONObject(json);
            org.json.JSONArray data = root.optJSONArray("data");
            org.json.JSONArray included = root.optJSONArray("included");

            if (data == null || data.length() == 0) {
                hasMoreProducts = false;
                return;
            }

            // JSON:API included bloğunu haritaya ata (Resimler için)
            java.util.Map<String, String> imageMap = new java.util.HashMap<>();
            if (included != null) {
                for (int i = 0; i < included.length(); i++) {
                    org.json.JSONObject item = included.getJSONObject(i);
                    if ("image".equals(item.optString("type"))) {
                        String id = item.optString("id");
                        org.json.JSONObject attr = item.optJSONObject("attributes");
                        if (attr != null) imageMap.put(id, attr.optString("url"));
                    }
                }
            }

            for (int i = 0; i < data.length(); i++) {
                org.json.JSONObject productObj = data.getJSONObject(i);
                org.json.JSONObject attributes = productObj.optJSONObject("attributes");
                if (attributes == null) continue;

                Product p = new Product();
                p.id = productObj.optString("id");
                p.name = attributes.optString("name");
                
                // Resim eşleştirmesi (relationships -> main_image)
                org.json.JSONObject rels = productObj.optJSONObject("relationships");
                if (rels != null) {
                    org.json.JSONObject mainImg = rels.optJSONObject("main_image");
                    if (mainImg != null) {
                        org.json.JSONObject imgData = mainImg.optJSONObject("data");
                        if (imgData != null) {
                            String imgId = imgData.optString("id");
                            p.imageUrl = imageMap.get(imgId);
                        }
                    }
                }
                
                p.url = "/tr/produkts/" + attributes.optString("slug"); // API'den gelen slug'a göre detay linki
                ibicoProducts.add(p);
            }
            ibicoAdapter.notifyDataSetChanged();
            if (data.length() < 10) hasMoreProducts = false;
        } catch (Exception e) {
            android.util.Log.e("KartalIstoc", "JSON:API parse error", e);
        }
    }

    private void fetchInvoicesFromWeb() {
        txtPanelTitle.setText("Listelerim");
        invoicesListContainer.removeAllViews();
        invoicesLoading.setVisibility(View.VISIBLE);
        txtInvoicesErrorArea.setVisibility(View.GONE);

        // Arka planda JSON API'yi çağır
        String js = "(function() {" +
                "  var apiUrl = window.location.origin + '/api/tr/v1/layouts/b2b/account/invoices.json';" +
                "  console.log('Fetching invoices from:', apiUrl);" +
                "  fetch(apiUrl, { method: 'GET', credentials: 'include', headers: { 'Accept': 'application/json' } })" +
                "  .then(function(res) {" +
                "    console.log('Invoice API status:', res.status);" +
                "    if (!res.ok) throw new Error('HTTP ' + res.status);" +
                "    return res.json();" +
                "  })" +
                "  .then(function(data) {" +
                "    console.log('Invoice data received, count:', data.invoices ? data.invoices.length : 0);" +
                "    Android.onInvoicesReceived(JSON.stringify(data));" +
                "  })" +
                "  .catch(function(err) {" +
                "    console.log('Invoice fetch error:', err.message);" +
                "    Android.onInvoicesError(err.message);" +
                "  });" +
                "})();";
        
        webView.evaluateJavascript(js, null);
    }

    private void updateUserInfoAndBalance() {
        if (isLoggedIn) {
            fetchTopBarData();
        }
    }

    private double parseFormattedNumber(String input) {
        if (input == null || input.isEmpty()) return 0;
        // Temizleme: TL simgesi veya harfleri kaldır
        String clean = input.replaceAll("[^0-9,.\\-]", "").trim();
        
        try {
            // Hem nokta hem virgül varsa (1.234,56 veya 1,234.56)
            if (clean.contains(",") && clean.contains(".")) {
                if (clean.lastIndexOf(",") > clean.lastIndexOf(".")) {
                    // TR Format (Virgül en sağda)
                    return java.text.NumberFormat.getInstance(new java.util.Locale("tr", "TR")).parse(clean).doubleValue();
                } else {
                    // US Format (Nokta en sağda)
                    return java.text.NumberFormat.getInstance(java.util.Locale.US).parse(clean).doubleValue();
                }
            }
            
            // Sadece virgül varsa -> TR Decimal (24036,44)
            if (clean.contains(",")) {
                return java.text.NumberFormat.getInstance(new java.util.Locale("tr", "TR")).parse(clean).doubleValue();
            }
            
            // Sadece nokta varsa -> US Decimal / Standart (24036.44)
            if (clean.contains(".")) {
                return Double.parseDouble(clean);
            }
            
            // Saf sayı
            return Double.parseDouble(clean);
        } catch (Exception e) {
            // Son çare: Manuel temizlik ve parse
            try {
                String b = clean.replace(".", "").replace(",", ".");
                return Double.parseDouble(b);
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    private String formatCurrency(double value) {
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00", new java.text.DecimalFormatSymbols(new java.util.Locale("tr", "TR")));
        return df.format(value) + " TL";
    }

    private String formatBalance(double value) {
        boolean isNegative = value < 0;
        double absValue = Math.abs(value);
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0.00", new java.text.DecimalFormatSymbols(new java.util.Locale("tr", "TR")));
        return df.format(absValue) + " TL " + (isNegative ? "(B)" : "(A)");
    }

    // JavaScript'ten çağrılacak metodlar
    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void updateCartSummary(int count, String total) {
            runOnUiThread(() -> {
                TextView badge = findViewById(R.id.txtCartBadge);
                TextView totalView = findViewById(R.id.txtNavCartTotal);
                if (badge != null) {
                    badge.setText(String.valueOf(count));
                    badge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                }
                if (totalView != null) {
                    totalView.setText(total);
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void onStateChanged(boolean loggedIn) {
            runOnUiThread(() -> {
                isLoggedIn = loggedIn;
                updateUIVisibility();
                if (loggedIn) {
                    fetchTopBarData();
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void onUserDataReceived(String json) {
            runOnUiThread(() -> {
                try {
                    org.json.JSONObject data = new org.json.JSONObject(json);
                    
                    // Müşteri Adı
                    if (data.has("name")) {
                        txtCustomerName.setText(data.getString("name"));
                    }
                    
                    // Plasiyer Adı
                    if (data.has("salesperson")) {
                        org.json.JSONObject sp = data.getJSONObject("salesperson");
                        if (sp.has("name") && !sp.getString("name").isEmpty()) {
                            txtSalespersonName.setText("Satış Temsilcisi: " + sp.getString("name"));
                            txtSalespersonName.setVisibility(View.VISIBLE);
                        } else {
                            txtSalespersonName.setVisibility(View.GONE);
                        }
                    } else {
                        txtSalespersonName.setVisibility(View.GONE);
                    }
                    
                    // Bakiye (Hem direct hem de balance_data içinden okuma)
                    String balanceStr = "";
                    if (data.has("balance")) {
                        double bal = data.getDouble("balance");
                        balanceStr = formatBalance(bal);
                    } else if (data.has("balance_data")) {
                        org.json.JSONObject bData = data.getJSONObject("balance_data");
                        if (bData.has("bakiye")) {
                            String bRaw = bData.getString("bakiye");
                            try {
                                double bal = parseFormattedNumber(bRaw);
                                balanceStr = formatBalance(bal);
                            } catch (Exception e) {
                                balanceStr = bRaw + " TL";
                            }
                        }
                    }
                    if (!balanceStr.isEmpty()) txtTopBalance.setText(balanceStr);
                } catch (Exception e) {
                    android.util.Log.e("KartalIstoc", "User data parse error", e);
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void onBalanceReceived(String balance) {
            runOnUiThread(() -> {
                if (balance != null && !balance.isEmpty()) {
                    try {
                        double bal = parseFormattedNumber(balance);
                        String formatted = formatBalance(bal);
                        txtTopBalance.setText(formatted);
                    } catch (Exception e) {
                        txtTopBalance.setText(balance + " TL");
                    }
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void onProductsReceived(String json) {
            runOnUiThread(() -> {
                handleProductsReceived(json);
            });
        }

        @android.webkit.JavascriptInterface
        public void onInvoicesReceived(String json) {
            android.util.Log.d("KartalIstoc", "onInvoicesReceived: " + json.substring(0, Math.min(json.length(), 500)));
            runOnUiThread(() -> {
                try {
                    invoicesLoading.setVisibility(View.GONE);
                    org.json.JSONObject data = new org.json.JSONObject(json);
                    org.json.JSONArray invoices = data.optJSONArray("invoices");

                    if (invoices == null || invoices.length() == 0) {
                        txtInvoicesErrorArea.setVisibility(View.VISIBLE);
                        txtInvoicesError.setText("Fatura bulunamadı.");
                        return;
                    }

                    int addedCount = 0;
                    for (int i = 0; i < invoices.length(); i++) {
                        org.json.JSONObject inv = invoices.getJSONObject(i);
                        String type = inv.optString("invoice_type", "");
                        if (type.equals("Toptan Satış") || type.equals("Toptan Satış İade")) {
                            addInvoiceToUI(inv);
                            addedCount++;
                        }
                    }
                    android.util.Log.d("KartalIstoc", "Added " + addedCount + " invoices to UI");
                    
                    if (addedCount == 0) {
                        txtInvoicesErrorArea.setVisibility(View.VISIBLE);
                        txtInvoicesError.setText("Gösterilecek fatura yok.");
                    }
                } catch (Exception e) {
                    android.util.Log.e("KartalIstoc", "Invoice parse error: " + e.getMessage());
                    txtInvoicesErrorArea.setVisibility(View.VISIBLE);
                    txtInvoicesError.setText("Veri işlenemedi: " + e.getMessage());
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void onInvoicesError(String error) {
            runOnUiThread(() -> {
                invoicesLoading.setVisibility(View.GONE);
                txtInvoicesErrorArea.setVisibility(View.VISIBLE);
                txtInvoicesError.setText("Bağlantı hatası veya oturum kapalı.");
            });
        }

        @android.webkit.JavascriptInterface
        public void onPdfReceived(String base64Data, String fileName) {
            runOnUiThread(() -> {
                try {
                    byte[] pdfBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT);
                    java.io.File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    java.io.File pdfFile = new java.io.File(downloadsDir, fileName);
                    
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(pdfFile);
                    fos.write(pdfBytes);
                    fos.close();
                    
                    Toast.makeText(MainActivity.this, "✓ Fatura indirildi: " + fileName, Toast.LENGTH_LONG).show();
                    
                    // Bildirim Oluştur
                    String channelId = "downloads_channel";
                    android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        android.app.NotificationChannel channel = new android.app.NotificationChannel(
                            channelId, "İndirmeler", android.app.NotificationManager.IMPORTANCE_DEFAULT);
                        nm.createNotificationChannel(channel);
                    }
                    
                    // Dosyayı açmak için Intent
                    Uri fileUri = androidx.core.content.FileProvider.getUriForFile(MainActivity.this, 
                        getPackageName() + ".provider", pdfFile);
                    
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                        MainActivity.this, 0, intent, 
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                    
                    androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(MainActivity.this, channelId)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle("Fatura İndirildi")
                        .setContentText(fileName)
                        .setAutoCancel(true)
                        .setOngoing(false)
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pendingIntent);
                        
                    nm.notify((int) System.currentTimeMillis(), builder.build());
                    
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Dosya kaydedilemedi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    android.util.Log.e("KartalIstoc", "PDF Save Error", e);
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void onPdfError(String error) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "PDF indirme hatası: " + error, Toast.LENGTH_LONG).show();
            });
        }

        @android.webkit.JavascriptInterface
        public void generateCartPdf(String cartJson) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "PDF Hazırlanıyor...", Toast.LENGTH_SHORT).show();
                try {
                    org.json.JSONObject data = new org.json.JSONObject(cartJson);
                    generatePdfFromHtml(data);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "PDF Hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void addInvoiceToUI(org.json.JSONObject inv) throws Exception {
        View view = getLayoutInflater().inflate(R.layout.item_invoice, invoicesListContainer, false);
        
        TextView txtDate = view.findViewById(R.id.txtInvoiceDate);
        TextView txtNo = view.findViewById(R.id.txtInvoiceNo);
        TextView txtPrice = view.findViewById(R.id.txtInvoicePrice);
        ImageButton btnDownload = view.findViewById(R.id.btnDownloadInvoice);

        // Tarih Formatı GG/AA/YYYY (ISO ve karmaşık formatları temizleyerek)
        String rawDate = inv.optString("invoice_date", "");
        if (rawDate != null && rawDate.length() >= 10) {
            // Sadece YYYY-MM-DD kısmını al (örn: 2025-12-31)
            String cleanDate = rawDate.substring(0, 10);
            String[] parts = cleanDate.split("-");
            if (parts.length == 3) {
                txtDate.setText(parts[2] + "/" + parts[1] + "/" + parts[0]);
            } else {
                txtDate.setText(cleanDate);
            }
        } else {
            txtDate.setText(rawDate);
        }

        txtNo.setText("Sipariş No: " + inv.optString("doc_no2", inv.optString("doc_no", "---")));
        txtPrice.setText(String.format("%.2f TL", inv.optDouble("total_price", 0.0)));

        String diaKey = inv.optString("dia_key_scf_fatura", "");
        btnDownload.setOnClickListener(v -> {
            if (!diaKey.isEmpty()) {
                downloadInvoice(diaKey, inv.optString("doc_no2", inv.optString("doc_no", "Fatura")));
            }
        });

        invoicesListContainer.addView(view);
    }

    private void downloadInvoice(String diaKey, String invNo) {
        if (diaKey == null || diaKey.isEmpty()) {
            Toast.makeText(this, "Bu fatura indirilemez (anahtar yok).", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Fatura hazırlanıyor...", Toast.LENGTH_SHORT).show();

        // Blob olarak indir, Base64'e çevir ve Java'ya gönder
        String js = "(function() {" +
                "  var apiUrl = window.location.origin + '/api/tr/v1/data/reports/fatura_fisi?key=" + diaKey + "';" +
                "  fetch(apiUrl, {" +
                "    method: 'POST'," +
                "    headers: { 'Content-Type': 'application/x-www-form-urlencoded' }," +
                "    body: 'commit=pdf'," +
                "    credentials: 'include'" +
                "  })" +
                "  .then(function(res) {" +
                "    if (!res.ok) throw new Error('HTTP ' + res.status);" +
                "    return res.blob();" +
                "  })" +
                "  .then(function(blob) {" +
                "    if (blob.size < 100) {" +
                "      Android.onPdfError('Dosya çok küçük veya geçersiz');" +
                "      return;" +
                "    }" +
                "    var reader = new FileReader();" +
                "    reader.onloadend = function() {" +
                "      var base64 = reader.result.split(',')[1];" +
                "      Android.onPdfReceived(base64, '" + invNo + ".pdf');" +
                "    };" +
                "    reader.readAsDataURL(blob);" +
                "  })" +
                "  .catch(function(err) {" +
                "    Android.onPdfError(err.message);" +
                "  });" +
                "})();";

        webView.evaluateJavascript(js, null);
    }

    private void applyDarkMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            }
        }
    }

    // --- Product Models and Adapter ---
    public static class Product {
        String id, name, imageUrl, price, url;
    }

    private class ProductAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ProductAdapter.ViewHolder> {
        private java.util.List<Product> products;

        ProductAdapter(java.util.List<Product> products) { this.products = products; }

        @androidx.annotation.NonNull
        @Override
        public ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_product, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull ViewHolder holder, int position) {
            Product p = products.get(position);
            holder.txtName.setText(p.name);
            
            Glide.with(MainActivity.this)
                .load(p.imageUrl)
                .placeholder(R.drawable.ic_cart)
                .into(holder.img);

            holder.itemView.setOnClickListener(v -> {
                hideIbicoPanel();
                webView.loadUrl(URL + p.url);
            });
        }

        @Override
        public int getItemCount() { return products.size(); }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView img; TextView txtName;
            ViewHolder(View v) {
                super(v);
                img = v.findViewById(R.id.imgProduct);
                txtName = v.findViewById(R.id.txtProductName);
            }
        }
    }
    private void injectCartSystem() {
        String cartJs = "(function() {" +
                "    if (window.kartalCartApp) return;" +
                "    const style = document.createElement('style');" +
                "    style.innerHTML = `" +
                "        #cart-panel-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); z-index: 9999998; display: none; opacity: 0; transition: opacity 0.3s; backdrop-filter: blur(4px); }" +
                "        #cart-panel { position: fixed; bottom: -85%; left: 0; right: 0; height: 85%; background: #ffffff; border-radius: 20px 20px 0 0; z-index: 9999999; transition: bottom 0.3s cubic-bezier(0.19, 1, 0.22, 1); display: flex; flex-direction: column; box-shadow: 0 -10px 40px rgba(0,0,0,0.3); overflow: hidden; }" +
                "        .p-header { padding: 16px 20px; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; background: #fff; }" +
                "        .p-title { font-size: 18px; font-weight: 700; color: #111; }" +
                "        .p-close { background: #f5f5f5; border: none; font-size: 24px; color: #666; width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; cursor: pointer; }" +
                "        .p-content { flex: 1; overflow-y: auto; padding: 0 20px; -webkit-overflow-scrolling: touch; }" +
                "        .c-item { display: flex; align-items: center; padding: 16px 0; border-bottom: 1px solid #f8f8f8; }" +
                "        .c-img { width: 50px; height: 50px; min-width: 50px; border-radius: 6px; object-fit: contain; background: #fafafa; border: 1px solid #eee; }" +
                "        .c-info { flex: 1; margin-left: 12px; min-width: 0; }" +
                "        .c-name { font-size: 14px; font-weight: 700; color: #111; margin-bottom: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }" +
                "        .c-sku { font-size: 11px; color: #777; text-transform: uppercase; letter-spacing: 0.5px; background: #f5f5f5; padding: 2px 5px; border-radius: 4px; display: inline-block; }" +
                "        .c-qty-price { margin-top: 6px; font-size: 14px; color: #444; font-weight: 600; }" +
                "        .c-qty-highlight { color: #1565c0; font-weight: 800; font-size: 16px; }" +
                "        .c-total { font-weight: 800; color: #000; font-size: 15px; margin-left: 8px; }" +
                "        .p-footer { padding: 20px; border-top: 1px solid #eee; background: #fff; box-shadow: 0 -4px 12px rgba(0,0,0,0.05); }" +
                "        .f-row { display: flex; justify-content: space-between; margin-bottom: 12px; }" +
                "        .f-total-label { font-size: 14px; color: #666; font-weight: 500; }" +
                "        .f-total-val { font-size: 22px; font-weight: 800; color: #1565c0; }" +
                "        .f-btn { width: 100%; height: 50px; background: #1565c0; color: #fff; border: none; border-radius: 12px; font-size: 15px; font-weight: 700; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; }" +
                "        .empty-cart { padding: 60px 20px; text-align: center; color: #999; font-size: 14px; }" +
                "    `;" +
                "    document.head.appendChild(style);" +
                "    const overlay = document.createElement('div'); overlay.id = 'cart-panel-overlay';" +
                "    const panel = document.createElement('div'); panel.id = 'cart-panel';" +
                "    panel.innerHTML = `<div class='p-header'><span class='p-title'>Sepetiniz</span><button class='p-close' onclick='window.closeCartPanel()'>&times;</button></div><div class='p-content' id='cart-list-body'></div><div class='p-footer' id='cart-list-footer'></div>`;" +
                "    document.documentElement.appendChild(overlay);" +
                "    document.documentElement.appendChild(panel);" +
                "    let currentData = null;" +
                "    const formatTL = (num) => new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(num).replace('TRY', '₺').trim();" +
                "    window.closeCartPanel = function() { overlay.style.opacity = '0'; panel.style.bottom = '-85%'; setTimeout(() => { overlay.style.display = 'none'; }, 300); };" +
                "    window.openCartPanel = function() { overlay.style.display = 'block'; setTimeout(() => { overlay.style.opacity = '1'; }, 50); panel.style.bottom = '0'; refreshCart(); };" +
                "    overlay.onclick = window.closeCartPanel;" +
                "    function refreshCart() {" +
                "        fetch('https://kartalistoc.com/api/tr/v1/layouts/b2b/carts.json', {credentials: 'include'})" +
                "            .then(r => r.json()).then(data => {" +
                "                if (data && data.cart_detail) { currentData = data; renderUI(data.cart_detail); }" +
                "            }).catch(e => console.error('Cart refresh error:', e));" +
                "    }" +
                "    function renderUI(detail) {" +
                "        const cart = detail.cart; const count = cart.item_count || 0; const total = formatTL(cart.total_price);" +
                "        Android.updateCartSummary(count, total);" +
                "        const body = document.getElementById('cart-list-body'); const footer = document.getElementById('cart-list-footer');" +
                "        if (!body || !footer) return;" +
                "        if (count === 0) { body.innerHTML = '<div class=\"empty-cart\">Sepetinizde ürün bulunmamaktadır.</div>'; footer.innerHTML = ''; return; }" +
                "        let html = ''; const items = detail.cart_items || [];" +
                "        for (let i = 0; i < items.length; i++) {" +
                "            const item = items[i]; const p = item.product; const img = (p.images && p.images[0]) ? p.images[0].url : '';" +
                "            html += `<div class=\"c-item\"><img src=\"${img}\" class=\"c-img\" loading=\"lazy\"><div class=\"c-info\"><div class=\"c-name\">${p.name}</div><div class=\"c-sku\">Stok Kodu: ${p.sku || p.stock_code || '---'}</div><div class=\"c-qty-price\"><span class=\"c-qty-highlight\">${item.quantity} Adet</span> x ${formatTL(item.unit_price)}</div></div><div class=\"c-total\">${formatTL(item.total_price)}</div></div>`;" +
                "        }" +
                "        body.innerHTML = html;" +
                "        footer.innerHTML = `<div class=\"f-row\"><span class=\"f-total-label\">Genel Toplam</span><span class=\"f-total-val\">${total}</span></div>`;" +
                "        const btn = document.createElement('button');" +
                "        btn.className = 'f-btn';" +
                "        btn.innerHTML = '<span>SEPETİ PDF OLARAK İNDİR</span>';" +
                "        btn.onclick = function() {" +
                "            if (currentData) {" +
                "                Android.generateCartPdf(JSON.stringify(currentData));" +
                "            } else {" +
                "                alert('Sepet verisi henüz yüklenmedi, lütfen bekleyiniz.');" +
                "            }" +
                "        };" +
                "        footer.appendChild(btn);" +
                "    }" +
                "    setInterval(refreshCart, 15000); refreshCart(); window.addEventListener('focus', refreshCart);" +
                "    const originFetch = window.fetch; window.fetch = function() { return originFetch.apply(this, arguments).then(r => { if (arguments[0] && arguments[0].includes('cart')) setTimeout(refreshCart, 1000); return r; }); };" +
                "    window.kartalCartApp = true;" +
                "})();";
        webView.evaluateJavascript(cartJs, null);
    }

    private void generatePdfFromHtml(org.json.JSONObject data) {
        try {
            org.json.JSONObject detail = data.getJSONObject("cart_detail");
            org.json.JSONObject cart = detail.getJSONObject("cart");
            org.json.JSONArray items = detail.getJSONArray("cart_items");

            StringBuilder html = new StringBuilder();
            html.append("<html><head><meta charset='UTF-8'><style>");
            html.append("body{font-family:'Helvetica',sans-serif;padding:20px;color:#333;margin:0;} ");
            html.append(".header{border-bottom:2px solid #1565c0;padding:20px;background:#f8f9fa;margin-bottom:20px;} ");
            html.append("h1{color:#1565c0;margin:0;font-size:24px;} ");
            html.append(".info-grid{display:table;width:100%;margin-bottom:20px;} ");
            html.append(".info-cell{display:table-cell;width:50%;padding:10px;vertical-align:top;font-size:11px;background:#fff;border:1px solid #eee;} ");
            html.append("table{width:100%;border-collapse:collapse;margin:10px 0;} ");
            html.append("th{background:#1565c0;color:white;padding:10px;text-align:left;font-size:10px;text-transform:uppercase;} ");
            html.append("td{border-bottom:1px solid #eee;padding:8px;text-align:left;font-size:10px;vertical-align:middle;word-break:break-word;} ");
            html.append(".prod-img{width:35px;height:35px;object-fit:contain;margin-right:8px;background:#fff;border:1px solid #f0f0f0;} ");
            html.append(".total-area{margin-top:30px;padding:20px;background:#f8f9fa;border-top:2px solid #1565c0;text-align:right;} ");
            html.append(".grand-total{font-size:18px;font-weight:bold;color:#1565c0;} ");
            html.append("</style></head><body>");

            html.append("<div class='header'><h1>Sipariş / Sepet Özeti</h1>");
            html.append("<p style='font-size:12px;margin-top:5px;'>ID: #<b>").append(cart.optString("id")).append("</b> | Tarih: ").append(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date())).append("</p></div>");

            html.append("<div class='info-grid'>");
            html.append("<div class='info-cell'><b>TESLİMAT ADRESİ:</b><br>")
                .append(detail.optJSONObject("shipping_address") != null ? detail.getJSONObject("shipping_address").optString("address_detail", "---") : "---").append("</div>");
            html.append("<div class='info-cell'><b>FATURA ADRESİ:</b><br>")
                .append(detail.optJSONObject("billing_address") != null ? detail.getJSONObject("billing_address").optString("address_detail", "---") : "---").append("</div>");
            html.append("</div>");

            html.append("<table><thead><tr><th>Görsel</th><th>Ürün Bilgisi</th><th style='text-align:center'>Adet</th><th>Birim</th><th>Toplam</th></tr></thead><tbody>");
            for (int i = 0; i < items.length(); i++) {
                org.json.JSONObject item = items.getJSONObject(i);
                org.json.JSONObject prod = item.getJSONObject("product");
                String imgUrl = (prod.has("images") && prod.getJSONArray("images").length() > 0) ? prod.getJSONArray("images").getJSONObject(0).optString("url") : "";

                html.append("<tr>")
                    .append("<td><img class='prod-img' src='").append(imgUrl).append("'></td>")
                    .append("<td><b>").append(prod.optString("name")).append("</b><br><span style='font-size:9px;color:#666'>Stok Kodu: ").append(prod.optString("sku", prod.optString("stock_code", "---"))).append("</span></td>")
                    .append("<td style='text-align:center'><b>").append(item.optInt("quantity")).append("</b></td>")
                    .append("<td style='white-space:nowrap'>").append(formatCurrency(item.optDouble("unit_price"))).append("</td>")
                    .append("<td style='white-space:nowrap'><b>").append(formatCurrency(item.optDouble("total_price"))).append("</b></td>")
                    .append("</tr>");
            }
            html.append("</tbody></table>");

            html.append("<div class='total-area'>");
            html.append("<div class='grand-total'>GENEL TOPLAM: ").append(formatCurrency(cart.optDouble("total_price"))).append("</div>");
            html.append("</div></body></html>");

            final String pdfContent = html.toString();

            runOnUiThread(() -> {
                WebView printWebView = new WebView(this);
                printWebView.loadDataWithBaseURL(null, pdfContent, "text/html", "utf-8", null);
                printWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        try {
                            android.print.PrintManager printManager = (android.print.PrintManager) getSystemService(PRINT_SERVICE);
                            android.print.PrintAttributes attributes = new android.print.PrintAttributes.Builder()
                                    .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4)
                                    .setResolution(new android.print.PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                                    .setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS).build();
                            String printName = "KartalSepet_" + cart.optString("id");
                            printManager.print(printName, view.createPrintDocumentAdapter(printName), attributes);
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, "PDF hatası", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            });
        } catch (Exception e) {
            Toast.makeText(this, "PDF oluşturulamadı", Toast.LENGTH_SHORT).show();
        }
    }
}
