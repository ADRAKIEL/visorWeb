package mx.com.httpltte.webVisor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.util.Locale;




import static android.widget.Toast.LENGTH_SHORT;
import static java.lang.System.currentTimeMillis;



public class MainActivity extends Activity {

    private WebView visorWeb;
    final Activity activity = this;
    final static String exitPage = "http://byprice.ltte.com.mx/Salir.html";
    final static String mainPage = "http://byprice.ltte.com.mx/Loading.html";
    final static String artoo = "javascript: (function(){var t={},e=!0;if(\"object\"==typeof this.artoo&&(artoo.settings.reload||(artoo.log.verbose(\"artoo already exists within this page. No need to inject him again.\"),artoo.loadSettings(t),artoo.exec(),e=!1)),e){var o=document.getElementsByTagName(\"body\")[0];o||(o=document.createElement(\"body\"),document.documentElement.appendChild(o));var a=document.createElement(\"script\");console.log(\"artoo.js is loading...\"),a.src=\"//medialab.github.io/artoo/public/dist/artoo-latest.min.js\",a.type=\"text/javascript\",a.id=\"artoo_injected_script\",a.setAttribute(\"settings\",JSON.stringify(t)),o.appendChild(a)}}).call(this);";
    final static String buy = "javascript: (alert('Added to cart');)";

    String NOTIFICATION_CHANNEL_ID = "notify_channel_id_01";

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createNotification(String message) {
        // BEGIN_INCLUDE(notificationCompat)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        // END_INCLUDE(notificationCompat)

        // BEGIN_INCLUDE(intent)
        //Create Intent to launch this Activity again if the notification is clicked.
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(intent);
        // END_INCLUDE(intent)

        // BEGIN_INCLUDE(ticker)
        // Sets the ticker text
        builder.setTicker(getResources().getString(R.string.control_message).concat(message));

        // Sets the small icon for the ticker
        builder.setSmallIcon(R.drawable.icon_casa);
        // END_INCLUDE(ticker)

        // BEGIN_INCLUDE(buildNotification)
        // Cancel the notification when clicked
        builder.setAutoCancel(true);

        // Build the notification
        Notification notification = builder.build();
        // END_INCLUDE(buildNotification)

        // BEGIN_INCLUDE(customLayout)
        // Inflate the notification layout as RemoteViews
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notification);

        // Set text on a TextView in the RemoteViews programmatically.
        // final String time = DateFormat.getTimeInstance().format(new Date()).toString();
        final String textCollapse = getResources().getString(R.string.collapsed, message);
        contentView.setTextViewText(R.id.textView, textCollapse);

        /* Workaround: Need to set the content view here directly on the notification.
         * NotificationCompatBuilder contains a bug that prevents this from working on platform
         * versions HoneyComb.
         * See https://code.google.com/p/android/issues/detail?id=30495
         */
        notification.contentView = contentView;

        // Add a big content view to the notification if supported.
        // Support for expanded notifications was added in API level 16.
        // (The normal contentView is shown when the notification is collapsed, when expanded the
        // big content view set here is displayed.)
        if (Build.VERSION.SDK_INT >= 16) {
            // Inflate and set the layout for the expanded notification view
            RemoteViews expandedView = new RemoteViews(getPackageName(), R.layout.notification_expanded);
            final String textExpanded = getResources().getString(R.string.expanded, message);

            expandedView.setTextViewText(R.id.textView, textExpanded);
            notification.bigContentView = expandedView;
        }
        // END_INCLUDE(customLayout)

        // START_INCLUDE(notify)
        // Use the NotificationManager to show the notification
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(0, notification);
        // END_INCLUDE(notify)
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //this.getWindow().requestFeature(Window.FEATURE_PROGRESS);

        setContentView(R.layout.activity_main);

        createNotificationChannel();

        visorWeb = (WebView) findViewById(R.id.webView);
        visorWeb.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        // force web view to open inside application
        visorWeb.setWebViewClient(new MyWebViewClient());
        //openURL();

        if (!verificaConexion(this)) {
            Toast.makeText(getBaseContext(),
                    "Comprueba tu conexión a Internet... ", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        this.visorWeb = (WebView) this.findViewById(R.id.webView);

        // Bind a new interface between your JavaScript and Android code
        visorWeb.addJavascriptInterface(new WebAppInterface(this), "webVisor");
        // Enable JavaScript
        WebSettings webSettings = visorWeb.getSettings();

        webSettings.setJavaScriptEnabled(true);


        // Provide a WebViewClient for your WebView
        visorWeb.getSettings().setJavaScriptEnabled(true);




        visorWeb.setWebChromeClient(new WebChromeClient() {


            public void onProgressChanged(WebView view, int progress) {
                //activity.setTitle("Loading...");
                //activity.setProgress(progress * 100);

                final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);


                //ProgressBar progressBar = new ProgressBar(activity);
                progressBar.setProgress(progress * 100);

                if (progress == 100){
                    //activity.setTitle(R.string.app_name);
                    progressBar.setVisibility(View.GONE);
                }
                else
                {
                    progressBar.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result) {

                AlertDialog JsAlert = new AlertDialog.Builder(activity)
                        .setTitle("Visor Says:")
                        .setIcon( R.mipmap.ic_launcher)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok,
                                new AlertDialog.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        result.confirm();
                                    }
                                }).setCancelable(false)
                        .create();

                if(message.toLowerCase().contains("notification") || message.toLowerCase().contains("notificaciones")){
                    String textClear = message.toLowerCase().replace("notificaciones", "").replace("notification", "");
                    createNotification(textClear.toUpperCase());
                    JsAlert.show();
                } else {
                    JsAlert.show();
                }
                return true;
            }


        });

        visorWeb.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

                Toast.makeText(getBaseContext(),
                        "Error: (" + errorCode + ")", Toast.LENGTH_SHORT).show();

                view.stopLoading();

                if (!verificaConexion(activity)) {
                    Toast.makeText(getBaseContext(),
                            "Comprueba tu conexión a Internet. Saliendo... ", Toast.LENGTH_SHORT)
                            .show();
                    activity.finish();
                }


                activity.finish();
                /*
                if(errorCode == WebViewClient.ERROR_FILE_NOT_FOUND){
                }
                */
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                boolean isLoaded = false;
                if (!verificaConexion(activity)) {
                    Toast.makeText(getBaseContext(),
                            "Comprueba tu conexión a Internet. Saliendo... ", Toast.LENGTH_SHORT)
                            .show();
                    activity.finish();
                }
                
                if( url.equals(exitPage) ){
                    activity.finish();
                } else if( (url.contains("walmart")) ){
                    view.loadUrl(url);
                    view.loadUrl(artoo);
                    view.loadUrl(buy);
                } else {
                	view.loadUrl(url);
                	isLoaded = true;
                }
                
                return isLoaded;
            }
        });
	// load the url selected
        visorWeb.loadUrl(mainPage);
    }

    public void OnResume(Bundle savedInstanceState) {
        super.onResume();

    }

    public static boolean verificaConexion(Context contexto) {
        boolean esConectado = false;
        ConnectivityManager conexion = (ConnectivityManager) contexto.getSystemService(Context.CONNECTIVITY_SERVICE);
        // No sólo wifi, también GPRS
        NetworkInfo[] redes = conexion.getAllNetworkInfo();
        // bucle
        for (int i = 0; i < 2; i++) {
            // ¿Tenemos conexión? ponemos a true
            if (redes[i].getState() == NetworkInfo.State.CONNECTED) {
                esConectado = true;
            }
        }
        return esConectado;
    }

    @Override
    public void onBackPressed() {

        WebBackForwardList mWebBackForwardList = visorWeb.copyBackForwardList();
        String url = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex()-1).getUrl();

        //Toast.makeText(getBaseContext(), url, Toast.LENGTH_SHORT).show();
        //String url = visorWeb.getUrl();

        if (!verificaConexion(activity)) {
            Toast.makeText(getBaseContext(),
                    "Comprueba tu conexión a Internet. Saliendo... ", Toast.LENGTH_SHORT).show();
            activity.finish();
        }

        // Check if there's history
        if (this.visorWeb.canGoBack()){
            //super.onBackPressed();
            if (url.equals(mainPage) ){
                super.onBackPressed();
            } else {
                this.visorWeb.goBack();
            }
        }
        else {
            super.onBackPressed();
        }
    }

    private class MyWebViewClient extends WebViewClient {

        private long loadTime; // Web page loading time

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            boolean isUrl = false;

            if (Uri.parse(url).getHost().equals(mainPage)){
                isUrl = false;
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                isUrl = true;
            }
            catch (Exception ex){
                isUrl = false;
                Toast.makeText(getBaseContext(),
                        "Error... " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                activity.finish();
            }
            return isUrl;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            if (!verificaConexion(activity)) {
                Toast.makeText(getBaseContext(),
                        "Comprueba tu conexión a Internet. Saliendo... ", Toast.LENGTH_SHORT).show();
                activity.finish();
            }

            super.onPageStarted(view, url, favicon);
            // Save start time
            this.loadTime = currentTimeMillis();
            // Show a toast
            Toast.makeText(getApplicationContext(),
                    "Loading...", LENGTH_SHORT).show();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (!verificaConexion(activity)) {
                Toast.makeText(getBaseContext(),
                        "Comprueba tu conexión a Internet. Saliendo... ", Toast.LENGTH_SHORT).show();
                activity.finish();
            }
            super.onPageFinished(view, url);

            // Calculate load time
            this.loadTime = currentTimeMillis() - this.loadTime;

            // Convert milliseconds to date format
            String time = new SimpleDateFormat("mm:ss:SSS", Locale.getDefault())
                    .format(new Date(this.loadTime));

            // Show a toast
            Toast.makeText(getApplicationContext(),
                    "Loading time: " + time, LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.consult_register) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Descargue el manual de Usuario de: manual.pdf")
                    .setTitle("Atención!!")
                    .setCancelable(false)
                    .setNeutralButton("Aceptar",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();

            visorWeb.loadUrl("manual.pdf");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showNotificationClicked(View v) {
        createNotification("TEST");
    }

}
