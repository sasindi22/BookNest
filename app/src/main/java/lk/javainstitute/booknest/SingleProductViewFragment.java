package lk.javainstitute.booknest;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
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

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

import lk.javainstitute.booknest.databinding.FragmentSellerMessagesBinding;
import lk.javainstitute.booknest.databinding.FragmentSingleProductViewBinding;
import lk.javainstitute.booknest.ui.seller.SellerBookListingFragment;

public class SingleProductViewFragment extends Fragment {

    private FragmentSingleProductViewBinding binding;
    private int minQty = 1;
    private  int maxQty = 1;
    private int selectedQty = 1;
    private int finalPrice;
    private int bookPrice;
    private String bookId;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSingleProductViewBinding.inflate(inflater, container, false);

        setupQuantityButtons();

        Bundle bundle = getArguments();
        if (bundle!=null){
            String bookId = bundle.getString("bookId");
            loadBookData(bookId);
        }

        binding.addToCartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToCart();
            }
        });

        binding.addToWishlistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addToWishlist();
            }
        });

        return binding.getRoot();
    }

    private void loadBookData(String bookId) {
        this.bookId = bookId;
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("books")
                .document(bookId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        DocumentSnapshot doc = task.getResult();

                        binding.bookNameSP.setText(doc.getString("title"));
                        binding.bookDescSP.setText(doc.getString("description"));
                        binding.authorSP.setText(doc.getString("author"));

                        Object priceObj = doc.get("price");
                        bookPrice = priceObj != null ? Integer.parseInt(String.valueOf(priceObj)) : 0;
                        finalPrice = bookPrice;
                        updatePriceText();

                        Object qtyObj = doc.get("quantity");
                        maxQty = qtyObj != null ? Math.max(Integer.parseInt(String.valueOf(qtyObj)), 1) : 1;
                        selectedQty = 1;
                        binding.qtySP.setText(String.valueOf(selectedQty));

                        String imageUrl = doc.getString("imageUrl");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(requireContext()).load(imageUrl).into(binding.bookImageSP);
                        } else {
                            binding.bookImageSP.setImageResource(R.drawable.ic_menu_gallery);
                        }
                    }
                });
    }


    private void setupQuantityButtons() {
        binding.upBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedQty< maxQty){
                    selectedQty++;
                    binding.qtySP.setText(String.valueOf(selectedQty));
                    updatePrice();
                }
            }
        });
        binding.downBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedQty> minQty){
                    selectedQty--;
                    binding.qtySP.setText(String.valueOf(selectedQty));
                    updatePrice();
                }
            }
        });
    }

    private void updatePrice() {
        finalPrice = bookPrice * selectedQty;
        updatePriceText();
    }

    private void updatePriceText() {
        binding.priceSP.setText("Price: " + finalPrice);
    }

    private void addToCart(){
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("cart")
                .whereEqualTo("userId", userId)
                .whereEqualTo("bookId", bookId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot cart = task.getResult().getDocuments().get(0);
                            int currentQuantity = cart.getLong("quantity").intValue();
                            int updateQuantity = currentQuantity + selectedQty;

                            cart.getReference()
                                    .update("bookId", bookId, "quantity", updateQuantity, "price", finalPrice * updateQuantity)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Toast.makeText(getContext(), "Item quantity updated in Cart!", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getContext(), "Failed to update Cart", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                    } else {
                            HashMap<String, Object> document = new HashMap<>();
                            document.put("userId", userId);
                            document.put("bookId", bookId);
                            document.put("quantity", selectedQty);
                            document.put("price", finalPrice * selectedQty);

                            firestore.collection("cart")
                                    .add(document)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            Toast.makeText(getContext(), "Added to Cart", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getContext(), "Failed to add to Cart", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                });

    }

    private void addToWishlist(){
        SharedPreferences prefs = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        HashMap<String, Object> document = new HashMap<>();
        document.put("userId", userId);
        document.put("bookId", bookId);
        document.put("price", finalPrice);

        firestore.collection("wishlist")
                .whereEqualTo("userId", userId)
                .whereEqualTo("bookId", bookId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            Toast.makeText(getContext(), "Already in Wishlist", Toast.LENGTH_SHORT).show();
                        } else {
                            firestore.collection("wishlist")
                                    .add(document)
                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                        @Override
                                        public void onSuccess(DocumentReference documentReference) {
                                            Toast.makeText(getContext(), "Added to Wishlist", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(getContext(), "Failed to add to Wishlist", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}