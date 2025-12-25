package lk.javainstitute.booknest;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        // window insets setup ...

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        EditText email = findViewById(R.id.emailTextLogin);
        EditText password = findViewById(R.id.passwordTextLogin);

        Button login = findViewById(R.id.loginButton);
        login.setOnClickListener(view -> {
            String emailText = email.getText().toString().trim();
            String passwordText = password.getText().toString();

            if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                email.setError("Email is Required!");
                email.requestFocus();
                return;
            }
            if (passwordText.isEmpty()) {
                password.setError("Password is Required!");
                password.requestFocus();
                return;
            }

            mAuth.signInWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            // Fetch user role from Firestore by uid
                            firestore.collection("user")
                                    .document(uid)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String userRole = documentSnapshot.getString("role");
                                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putString("userId", uid);
                                            editor.putString("userRole", userRole != null ? userRole : "");
                                            editor.putBoolean("isLoggedIn", true);
                                            editor.apply();

                                            Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();

                                            if ("Admin".equalsIgnoreCase(userRole)) {
                                                startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                                            } else if ("Seller".equalsIgnoreCase(userRole)) {
                                                startActivity(new Intent(LoginActivity.this, SellerDashboardActivity.class));
                                            } else {
                                                startActivity(new Intent(LoginActivity.this, CustomerDashboardActivity.class));
                                            }
                                            finish();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "User profile missing. Contact support.", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this, "Failed to fetch profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });

                        } else {
                            String err = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                            Toast.makeText(LoginActivity.this, "Login failed: " + err, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        TextView toRegister = findViewById(R.id.toRegisterText);
        toRegister.setOnClickListener(view -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // Forgot password flow — keep using mAuth.sendPasswordResetEmail (works)
        TextView forgotPassword = findViewById(R.id.forgotPasswordText);
        forgotPassword.setOnClickListener(view -> {
            String emailText = email.getText().toString().trim();
            if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
                email.setError("Enter a valid Email!");
                email.requestFocus();
                return;
            }
            mAuth.sendPasswordResetEmail(emailText)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(LoginActivity.this, "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}
