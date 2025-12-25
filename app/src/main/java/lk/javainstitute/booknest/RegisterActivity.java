package lk.javainstitute.booknest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

// imports unchanged
public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        // window insets setup (same as yours) ...

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        EditText fname = findViewById(R.id.fnameText);
        EditText lname = findViewById(R.id.lnameText);
        EditText email = findViewById(R.id.emailText);
        EditText mobile = findViewById(R.id.mobileText);
        EditText password = findViewById(R.id.passwordText);
        Spinner spinner1 = findViewById(R.id.spinner);
        String role[] = new String[]{"Seller", "Customer"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                RegisterActivity.this,
                R.layout.user_type_spinner_item,
                R.id.userEmailNavC,
                role
        );
        spinner1.setAdapter(arrayAdapter);

        Button register = findViewById(R.id.registerButton);
        register.setOnClickListener(view -> {
            String fnameText = fname.getText().toString().trim();
            String lnameText = lname.getText().toString().trim();
            String emailText = email.getText().toString().trim();
            String mobileText = mobile.getText().toString().trim();
            String passwordText = password.getText().toString();
            String roleSelect = spinner1.getSelectedItem().toString();

            // Validation (same as yours)...
            if (fnameText.isEmpty()) { fname.setError("First Name is Required!"); fname.requestFocus(); return; }
            if (lnameText.isEmpty()) { lname.setError("Last Name is Required!"); lname.requestFocus(); return; }
            if (emailText.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) { email.setError("Enter a valid Email!"); email.requestFocus(); return; }
            if (mobileText.isEmpty() || !mobileText.matches("\\d{10}")) { mobile.setError("Enter a valid Mobile Number!"); mobile.requestFocus(); return; }
            if (passwordText.isEmpty() || passwordText.length() < 6) { password.setError("Password must be at least 6 characters long!"); password.requestFocus(); return; }

            // Create with FirebaseAuth
            mAuth.createUserWithEmailAndPassword(emailText, passwordText)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            Map<String, Object> userDoc = new HashMap<>();
                            userDoc.put("fname", fnameText);
                            userDoc.put("lname", lnameText);
                            userDoc.put("email", emailText);
                            userDoc.put("mobile", mobileText);
                            userDoc.put("role", roleSelect);
                            // DO NOT store password plaintext — remove it
                            // userDoc.put("password", passwordText); // <- avoid

                            firestore.collection("user")
                                    .document(uid)
                                    .set(userDoc)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this, "Registration successful. Please login.", Toast.LENGTH_LONG).show();
                                        Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
                                        startActivity(i);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Consider removing created auth user if firestore write fails (cleanup)
                                        mAuth.getCurrentUser().delete();
                                        Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            // show error
                            String err = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                            Toast.makeText(RegisterActivity.this, "Error: " + err, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        TextView toLogin = findViewById(R.id.toLoginText);
        toLogin.setOnClickListener(view -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }
}
