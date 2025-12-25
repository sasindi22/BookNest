package lk.javainstitute.booknest.ui.seller;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lk.javainstitute.booknest.databinding.FragmentSellerHomeBinding;

public class SellerHomeFragment extends Fragment {

    private FragmentSellerHomeBinding binding;
    private FirebaseFirestore firestore;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSellerHomeBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        TextView sales = binding.salesValue;
        TextView orders = binding.noOfOrders;
        TextView promotions = binding.activePromotions;

        fetchSalesData(userId, sales);
        fetchOrdersData(orders, userId);
        fetchPromotionsData(promotions, userId);

        VideoView videoView1 = binding.videoView1;
        String videoUrl = "https://drive.google.com/uc?export=download&id=1hseIry2-OcjYN4QZZXdUKVYyt0Zfc0Q_";
        videoView1.setVideoURI(Uri.parse(videoUrl));
        videoView1.start();
        videoView1.setOnCompletionListener(mediaPlayer -> videoView1.start());

        return binding.getRoot();
    }

    private void fetchSalesData(String userId, TextView salesTextView) {
        firestore.collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int[] totalItemsSold = {0};

                        for (QueryDocumentSnapshot orderDocument : task.getResult()) {
                            Object bookDetailsObj = orderDocument.get("bookDetails");

                            if (!(bookDetailsObj instanceof List)) {
                                Log.w("SellerHomeFragment", "bookDetails is not a list for order: " + orderDocument.getId());
                                continue;
                            }

                            List<?> bookDetailsList = (List<?>) bookDetailsObj;

                            for (Object item : bookDetailsList) {
                                if (!(item instanceof Map)) continue;
                                Map<String, Object> bookDetail = (Map<String, Object>) item;

                                String bookId = (String) bookDetail.get("bookId");
                                int quantity = ((Long) bookDetail.get("quantity")).intValue();

                                firestore.collection("books")
                                        .document(bookId)
                                        .get()
                                        .addOnCompleteListener(bookTask -> {
                                            if (bookTask.isSuccessful() && bookTask.getResult() != null) {
                                                String sellerId = bookTask.getResult().getString("sellerId");

                                                if (userId.equals(sellerId)) {
                                                    totalItemsSold[0] += quantity;
                                                    salesTextView.setText(String.valueOf(totalItemsSold[0]));
                                                }
                                            } else {
                                                Log.e("SellerHomeFragment", "Failed to fetch book: " + bookId, bookTask.getException());
                                            }
                                        });
                            }
                        }
                    } else {
                        Log.e("SellerHomeFragment", "Failed to fetch orders", task.getException());
                        salesTextView.setText("0");
                    }
                });
    }

    private void fetchOrdersData(TextView ordersTextView, String userId) {
        firestore.collection("orders")
                .whereIn("status", Arrays.asList("Paid", "Shipped"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int[] sellerOrderCount = {0};

                        for (QueryDocumentSnapshot orderDocument : task.getResult()) {
                            Object bookDetailsObj = orderDocument.get("bookDetails");

                            if (!(bookDetailsObj instanceof List)) {
                                Log.w("SellerHomeFragment", "bookDetails is not a list for order: " + orderDocument.getId());
                                continue;
                            }

                            List<?> bookDetailsList = (List<?>) bookDetailsObj;

                            for (Object item : bookDetailsList) {
                                if (!(item instanceof Map)) continue;
                                Map<String, Object> bookDetail = (Map<String, Object>) item;

                                String bookId = (String) bookDetail.get("bookId");

                                firestore.collection("books")
                                        .document(bookId)
                                        .get()
                                        .addOnCompleteListener(bookTask -> {
                                            if (bookTask.isSuccessful() && bookTask.getResult() != null) {
                                                String sellerId = bookTask.getResult().getString("sellerId");

                                                if (userId.equals(sellerId)) {
                                                    sellerOrderCount[0]++;
                                                    ordersTextView.setText(String.valueOf(sellerOrderCount[0]));
                                                }
                                            } else {
                                                Log.e("SellerHomeFragment", "Failed to fetch book: " + bookId, bookTask.getException());
                                            }
                                        });
                            }
                        }
                    } else {
                        ordersTextView.setText("0");
                    }
                });
    }

    private void fetchPromotionsData(TextView promotions, String userId) {
        firestore.collection("promotions")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        promotions.setText(String.valueOf(task.getResult().size()));
                    } else {
                        promotions.setText("0");
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
