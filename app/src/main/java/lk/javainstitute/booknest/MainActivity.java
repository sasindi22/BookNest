package lk.javainstitute.booknest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
        String userRole = sharedPreferences.getString("userRole", "");

        if (isLoggedIn) {
            if ("Admin".equalsIgnoreCase(userRole)) {
                Intent i = new Intent(MainActivity.this, AdminDashboardActivity.class);
                startActivity(i);
            } else if ("Seller".equalsIgnoreCase(userRole)) {
                Intent i = new Intent(MainActivity.this, SellerDashboardActivity.class);
                startActivity(i);
            } else if ("Customer".equalsIgnoreCase(userRole)) {
                Intent i = new Intent(MainActivity.this, CustomerDashboardActivity.class);
                startActivity(i);
            } else {
                return;
            }
            finish();
        }

        Button button = findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

    }
}