package lk.javainstitute.booknest.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentAdminHomeBinding;

public class AdminHomeFragment extends Fragment {

    private FragmentAdminHomeBinding binding;
    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();

        fetchBook1();
        fetchOrdersAndUpdateChart();

        return binding.getRoot();
    }

    private void fetchBook1(){
        firestore.collection("books")
                .orderBy("quantity")
                .limit(2)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().size() >= 2){
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            DocumentSnapshot documentSnapshot2 = task.getResult().getDocuments().get(1);

                            String bookTitle = documentSnapshot.getString("title");
                            String bookAuthor = documentSnapshot.getString("author");
                            String bookImageUrl  = documentSnapshot.getString("imageUrl");

                            String bookTitle2 = documentSnapshot2.getString("title");
                            String bookAuthor2 = documentSnapshot2.getString("author");
                            String bookImageUrl2  = documentSnapshot2.getString("imageUrl");

                            if (bookTitle!=null && bookAuthor!=null && bookImageUrl != null &&
                                    bookTitle2 != null && bookAuthor2 != null && bookImageUrl2 != null){
                                displayBook(bookTitle, bookAuthor, bookImageUrl, bookTitle2, bookAuthor2, bookImageUrl2);
                            } else {
                                Toast.makeText(getContext(), "No book data available!", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to fetch the relevant book!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void displayBook (String bookTitle, String bookAuthor, String bookImageUrl, String bookTitle2, String bookAuthor2, String bookImageUrl2){

        View bestSeller1 = LayoutInflater.from(getContext()).inflate(R.layout.best_seller, binding.bestSellerAdmin1, false);
        View bestSeller2 = LayoutInflater.from(getContext()).inflate(R.layout.best_seller, binding.bestSellerAdmin2, false);

        binding.bestSellerAdmin1.addView(bestSeller1);
        binding.bestSellerAdmin2.addView(bestSeller2);

        TextView book = bestSeller1.findViewById(R.id.titleBS);
        TextView author = bestSeller1.findViewById(R.id.authorBS);
        ImageView bookCover = bestSeller1.findViewById(R.id.bookCoverHome);

        TextView book2 = bestSeller2.findViewById(R.id.titleBS);
        TextView author2 = bestSeller2.findViewById(R.id.authorBS);
        ImageView bookCover2 = bestSeller2.findViewById(R.id.bookCoverHome);

        book.setText(bookTitle);
        author.setText(bookAuthor);
        Glide.with(getContext())
                .load(bookImageUrl)
                .placeholder(R.drawable.ic_menu_gallery)
                .error(R.drawable.delete)
                .into(bookCover);

        book2.setText(bookTitle2);
        author2.setText(bookAuthor2);
        Glide.with(getContext())
                .load(bookImageUrl2)
                .placeholder(R.drawable.ic_menu_gallery)
                .error(R.drawable.delete)
                .into(bookCover2);


    }

    private void fetchOrdersAndUpdateChart() {
        firestore.collection("orders")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            HashMap<String, Integer> sellerSalesMap = new HashMap<>();
                            AtomicInteger totalTasks = new AtomicInteger(0);
                            AtomicInteger completedTasks = new AtomicInteger(0);

                            for (DocumentSnapshot orderDoc : task.getResult()) {
                                ArrayList<HashMap<String, Object>> bookDetails = (ArrayList<HashMap<String, Object>>) orderDoc.get("bookDetails");

                                if (bookDetails != null) {
                                    totalTasks.addAndGet(bookDetails.size());

                                    for (HashMap<String, Object> bookDetail : bookDetails) {
                                        String bookId = (String) bookDetail.get("bookId");
                                        Long quantityLong = (Long) bookDetail.get("quantity");
                                        int quantity = (quantityLong != null) ? quantityLong.intValue() : 0;

                                        if (bookId != null && quantity > 0) {
                                            fetchSellerIdForBook(bookId, quantity, sellerSalesMap, () -> {
                                                completedTasks.incrementAndGet();
                                                if (completedTasks.get() == totalTasks.get()) {
                                                    updateBarChart(sellerSalesMap);
                                                }
                                            });
                                        } else {
                                            completedTasks.incrementAndGet();
                                            if (completedTasks.get() == totalTasks.get()) {
                                                updateBarChart(sellerSalesMap);
                                            }
                                        }
                                    }
                                }
                            }

                            if (totalTasks.get() == 0) {
                                Toast.makeText(getContext(), "No valid orders found!", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to fetch orders!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void fetchSellerIdForBook(String bookId, int quantity, HashMap<String, Integer> sellerSalesMap, Runnable onComplete) {
        firestore.collection("books").document(bookId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot bookDoc = task.getResult();
                            String sellerId = bookDoc.getString("sellerId");

                            if (sellerId != null) {
                                if (sellerSalesMap.containsKey(sellerId)) {
                                    sellerSalesMap.put(sellerId, sellerSalesMap.get(sellerId) + quantity);
                                } else {
                                    sellerSalesMap.put(sellerId, quantity);
                                }
                            }
                        }
                        onComplete.run();
                    }
                });
    }

    private void updateBarChart(HashMap<String, Integer> sellerSalesMap) {
        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> sellerNames = new ArrayList<>();

        final int[] indexWrapper = {0};

        for (Map.Entry<String, Integer> entry : sellerSalesMap.entrySet()) {
            String sellerId = entry.getKey();
            int salesCount = entry.getValue();

            firestore.collection("user").document(sellerId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                DocumentSnapshot userDoc = task.getResult();
                                String sellerName = userDoc.getString("fname");

                                sellerNames.add(sellerName);
                                barEntries.add(new BarEntry(indexWrapper[0], salesCount));
                                indexWrapper[0]++;

                                if (sellerNames.size() == sellerSalesMap.size()) {
                                    updateBarChartUI(barEntries);
                                }
                            }
                        }
                    });
        }
    }

    private void updateBarChartUI(ArrayList<BarEntry> barEntries) {
        BarChart barChart = binding.barChartAdmin;

        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        barDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        barDataSet.setValueTextSize(18f);
        barDataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.darkBrown));

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.animateY(2000, Easing.EaseOutCubic);
        barChart.getDescription().setEnabled(false);
        barChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}