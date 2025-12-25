package lk.javainstitute.booknest;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import lk.javainstitute.booknest.databinding.ActivityAdminDashboardBinding;

public class AdminDashboardActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityAdminDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarAdminDashboard.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_admin_dashboard);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_manage_books_admin,
                R.id.nav_manage_users_admin,
                R.id.nav_messages,
                R.id.nav_profile)
                .setOpenableLayout(drawer)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        loadUserData(navigationView);
    }

    private void loadUserData(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.userNameNavC);
        TextView navUserEmail = headerView.findViewById(R.id.userEmailNavC);
        ImageView navUserImage = headerView.findViewById(R.id.userProfileNavC);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("user")
                .document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()){
                                String fname = documentSnapshot.getString("fname");
                                String lname = documentSnapshot.getString("lname");
                                String email = documentSnapshot.getString("email");
                                String image = documentSnapshot.getString("profileImageUrl");

                                navUserName.setText(fname + " " + lname);
                                navUserEmail.setText(email);

                                if (image != null && !image.isEmpty()) {
                                    Glide.with(headerView.getContext())
                                            .load(image)
                                            .into(navUserImage);
                                } else {
                                    navUserImage.setImageResource(R.drawable.user);
                                }
                            }
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.admin_dashboard, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_admin_dashboard);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}