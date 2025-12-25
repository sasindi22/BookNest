package lk.javainstitute.booknest;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import lk.javainstitute.booknest.databinding.FragmentSellerMessagesBinding;
import lk.javainstitute.booknest.databinding.FragmentUserProfileBinding;

public class UserProfileFragment extends Fragment {

    private FragmentUserProfileBinding binding;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentUserProfileBinding.inflate(inflater, container, false);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        binding.profileImage.setImageURI(uri);
                    }
                }
        );

        ImageView imageView = binding.profileImage;
        imageView.setOnClickListener(view -> selectImage());

        EditText fname = binding.fnameUserP;
        EditText lname = binding.lnameUserP;
        EditText email = binding.emailUserP;
        EditText mobile = binding.mobileUserP;
        EditText address = binding.addressUserP;
        EditText role = binding.roleUserP;

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        Button update = binding.updateUserBtn;

        if (userId != null) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("user")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()) {
                                fname.setText(documentSnapshot.getString("fname"));
                                lname.setText(documentSnapshot.getString("lname"));
                                email.setText(documentSnapshot.getString("email"));
                                mobile.setText(documentSnapshot.getString("mobile"));
                                role.setText(documentSnapshot.getString("role"));

                                email.setEnabled(false);
                                role.setEnabled(false);

                                String addressValue = documentSnapshot.getString("address");
                                if (addressValue != null) {
                                    address.setText(addressValue);
                                } else {
                                    address.setHint("Address");
                                }

                                String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                                if (profileImageUrl != null) {
                                    Glide.with(binding.profileImage.getContext())
                                            .load(profileImageUrl)
                                            .placeholder(R.drawable.profile_user)
                                            .error(R.drawable.delete)
                                            .into(binding.profileImage);
                                }
                            } else {
                                Toast.makeText(getContext(), "Failed to fetch user data!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

            update.setOnClickListener(view -> {
                String updateAddress = address.getText().toString();
                if (!updateAddress.isEmpty()) {
                    firestore.collection("user")
                            .document(userId)
                            .update("address", updateAddress)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(getContext(), "Address Updated", Toast.LENGTH_SHORT).show();
                                if (selectedImageUri != null) {
                                    uploadImageToStorage(userId, selectedImageUri);
                                }
                            })
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update address!", Toast.LENGTH_SHORT).show());
                }
            });

        }

        Button logout = binding.logoutButton;
        logout.setOnClickListener(view -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Toast.makeText(requireContext(), "You have successfully logged out!", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return binding.getRoot();
    }

    private void selectImage() {
        imagePickerLauncher.launch("image/*");
    }

    private void uploadImageToStorage(String userId, Uri selectedImageUri) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profile_images/" + userId + "user.jpg");
        storageReference.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    FirebaseFirestore.getInstance()
                            .collection("user")
                            .document(userId)
                            .update("profileImageUrl", uri.toString())
                            .addOnSuccessListener(unused -> Toast.makeText(getContext(), "Profile Image uploaded successfully!", Toast.LENGTH_LONG).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update image URL!", Toast.LENGTH_LONG).show());
                }).addOnFailureListener(e -> Toast.makeText(getContext(), "Image upload failed. Try again!", Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to upload image!", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}