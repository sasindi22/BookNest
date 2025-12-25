package lk.javainstitute.booknest.ui.seller;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentSellerManageBooksBinding;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class SellerManageBooksFragment extends Fragment {

    private FragmentSellerManageBooksBinding binding;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Uri selectedImageUri;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSellerManageBooksBinding.inflate(inflater, container, false);

        setupSpinner();
        setupImagePicker();
        setupAddButton();

        return binding.getRoot();
    }

    private void setupSpinner() {
        String[] genre = {"Fantasy", "Sci-Fi", "Dystopian", "Romance", "Mystery/Thriller"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.user_type_spinner_item,
                R.id.userEmailNavC,
                genre
        );
        binding.genreSpinner.setAdapter(adapter);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        binding.bookImageField.setImageURI(uri);
                    }
                }
        );

        binding.bookImageField.setOnClickListener(v ->
                imagePickerLauncher.launch("image/*")
        );
    }

    private void setupAddButton() {
        binding.addToStoreButton.setOnClickListener(v -> addBook());
    }

    private void addBook() {
        String title = binding.titleField.getText().toString().trim();
        String desc = binding.descField.getText().toString().trim();
        String author = binding.authorField.getText().toString().trim();
        String lang = binding.langField.getText().toString().trim();
        String genre = binding.genreSpinner.getSelectedItem().toString();

        int price;
        int qty;

        try {
            price = Integer.parseInt(binding.priceField.getText().toString().trim());
            qty = Integer.parseInt(binding.qtyField.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid price or quantity", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences prefs =
                requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        if (userId == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_LONG).show();
            return;
        }

        HashMap<String, Object> book = new HashMap<>();
        book.put("title", title);
        book.put("description", desc);
        book.put("author", author);
        book.put("language", lang);
        book.put("genre", genre);
        book.put("price", price);
        book.put("quantity", qty);
        book.put("sellerId", userId);

        FirebaseFirestore.getInstance()
                .collection("books")
                .add(book)
                .addOnSuccessListener(ref -> {
                    if (selectedImageUri != null) {
                        uploadImage(ref);
                    } else {
                        Toast.makeText(getContext(),
                                "Book added (no image)",
                                Toast.LENGTH_SHORT).show();
                        clearUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(),
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("BOOK_ADD", "Failed", e);
                });
    }

    private void uploadImage(DocumentReference ref) {
        if (selectedImageUri == null) return;

        String cloudName = "dwamm5s0g";
        String presetName = "booknest";

        try {
            InputStream is = requireContext().getContentResolver().openInputStream(selectedImageUri);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);

            MultipartBody.Part body = MultipartBody.Part.createFormData(
                    "file", "image.jpg",
                    RequestBody.create(bytes)
            );

            OkHttpClient client = new OkHttpClient();
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "image.jpg",
                            RequestBody.create(bytes))
                    .addFormDataPart("upload_preset", presetName)
                    .build();


            Request request = new Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(@NonNull okhttp3.Call call, @NonNull IOException e) {
                    Log.e("CLOUDINARY", "Upload failed: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull okhttp3.Call call, @NonNull okhttp3.Response response) throws IOException {
                    String resp = response.body().string();
                    // Parse JSON to get uploaded image URL
                    try {
                        JSONObject json = new JSONObject(resp);
                        String imageUrl = json.getString("secure_url");

                        // Save to Firestore
                        ref.update("imageUrl", imageUrl).addOnSuccessListener(aVoid ->
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(), "Book & image uploaded", Toast.LENGTH_SHORT).show()
                                )
                        );
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void clearUI() {
        binding.titleField.setText("");
        binding.descField.setText("");
        binding.authorField.setText("");
        binding.priceField.setText("");
        binding.langField.setText("");
        binding.qtyField.setText("");
        binding.genreSpinner.setSelection(0);
        binding.bookImageField.setImageResource(R.drawable.ic_menu_gallery);
        selectedImageUri = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
