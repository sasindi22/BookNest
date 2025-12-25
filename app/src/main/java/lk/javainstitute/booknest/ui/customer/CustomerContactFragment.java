package lk.javainstitute.booknest.ui.customer;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.Context;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentCustomerContactBinding;
import lk.javainstitute.booknest.databinding.FragmentSellerMessagesBinding;

public class CustomerContactFragment extends Fragment {

    private FragmentCustomerContactBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCustomerContactBinding.inflate(inflater, container, false);

        ImageView banner = binding.banner;
        WebView webView = binding.webView;
        TextView webLink = binding.webBtn;
        Button call = binding.callBtn;

        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.i("booknestLog", "Page loaded: " + url);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                Log.i("booknestLog", "HTTP Error: " + errorResponse.getStatusCode());
            }
        });

        webLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                banner.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl("https://google.com");
                Log.i("booknestLog", "WebLink clicked, loading URL...");
            }
        });

        call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_DIAL);
                Uri uri = Uri.parse("tel: 0705839524");
                i.setData(uri);
                startActivity(i);
            }
        });

        return binding.getRoot();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}