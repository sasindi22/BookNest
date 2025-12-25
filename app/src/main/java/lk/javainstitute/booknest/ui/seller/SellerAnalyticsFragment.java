package lk.javainstitute.booknest.ui.seller;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentSellerAnalyticsBinding;

public class SellerAnalyticsFragment extends Fragment {

    private FragmentSellerAnalyticsBinding binding;
    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSellerAnalyticsBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();

        initializePieChart(binding.pieChart);
        initializePieChart(binding.pieChart2);

        fetchBestSellingGenres();
        fetchBestSellingAuthors();

        return binding.getRoot();
    }

    private void initializePieChart(PieChart pieChart) {
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);

        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);

        pieChart.setHoleColor(android.R.color.transparent);
        pieChart.setTransparentCircleRadius(61f);
        pieChart.setEntryLabelColor(getResources().getColor(R.color.darkBrown));
        pieChart.setEntryLabelTextSize(12f);
    }

    private void fetchBestSellingGenres() {
        firestore.collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Integer> genreSales = new HashMap<>();

                        for (QueryDocumentSnapshot order : task.getResult()) {
                            List<HashMap<String, Object>> bookDetails = (List<HashMap<String, Object>>) order.get("bookDetails");

                            if (bookDetails != null) {
                                for (HashMap<String, Object> bookDetail : bookDetails) {
                                    String bookId = (String) bookDetail.get("bookId");
                                    int quantity = ((Long) bookDetail.get("quantity")).intValue();

                                    firestore.collection("books")
                                            .document(bookId)
                                            .get()
                                            .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                @Override
                                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                    String genre = documentSnapshot.getString("genre");
                                                    if (genre != null) {
                                                        genreSales.put(genre, genreSales.getOrDefault(genre, 0) + quantity);
                                                    }
                                                    updateGenrePieChart(genreSales);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e("FirestoreError", "Error fetching book data", e);
                                                }
                                            });
                                }
                            }
                        }
                    } else {
                        Log.e("FirestoreError", "Error fetching orders", task.getException());
                    }
                });
    }

    private void updateGenrePieChart(Map<String, Integer> genreSales) {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : genreSales.entrySet()) {
            pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "Best Selling Genres");
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(getResources().getColor(R.color.darkBrown));

        PieData pieData = new PieData(dataSet);
        binding.pieChart2.setData(pieData);
        binding.pieChart2.animateY(1000, Easing.EaseInOutQuad);
        binding.pieChart2.invalidate();
    }

    private void fetchBestSellingAuthors() {
        firestore.collection("orders")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Integer> authorSales = new HashMap<>();

                        for (QueryDocumentSnapshot order : task.getResult()) {
                            List<HashMap<String, Object>> bookDetails = (List<HashMap<String, Object>>) order.get("bookDetails");

                            if (bookDetails != null) {
                                for (HashMap<String, Object> bookDetail : bookDetails) {
                                    String bookId = (String) bookDetail.get("bookId");
                                    int quantity = ((Long) bookDetail.get("quantity")).intValue();

                                    firestore.collection("books")
                                            .document(bookId)
                                            .get()
                                            .addOnSuccessListener(bookDocument -> {
                                                String author = bookDocument.getString("author");
                                                if (author != null) {
                                                    authorSales.put(author, authorSales.getOrDefault(author, 0) + quantity);
                                                }
                                                updateAuthorPieChart(authorSales);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("FirestoreError", "Error fetching book data", e);
                                            });
                                }
                            }
                        }
                    } else {
                        Log.e("FirestoreError", "Error fetching orders", task.getException());
                    }
                });
    }

    private void updateAuthorPieChart(Map<String, Integer> authorSales) {
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : authorSales.entrySet()) {
            pieEntries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(pieEntries, "");
        dataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(getResources().getColor(R.color.darkBrown));

        PieData pieData = new PieData(dataSet);
        binding.pieChart.setData(pieData);
        binding.pieChart.animateY(1000, Easing.EaseInOutQuad);
        binding.pieChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}