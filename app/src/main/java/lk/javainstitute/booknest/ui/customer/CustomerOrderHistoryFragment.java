package lk.javainstitute.booknest.ui.customer;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Map;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentCustomerOrderHistoryBinding;

public class CustomerOrderHistoryFragment extends Fragment {

    private FragmentCustomerOrderHistoryBinding binding;
    private ArrayList<Order> orderList = new ArrayList<>();
    private Adapter1 adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCustomerOrderHistoryBinding.inflate(inflater, container, false);

        RecyclerView recyclerView = binding.recyclerViewOH;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        orderList = new ArrayList<>();
        adapter = new Adapter1(orderList);
        recyclerView.setAdapter(adapter);

        fetchOrderData();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void fetchOrderData(){
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            orderList.clear();
                            for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                String orderId = documentSnapshot.getString("orderId");
                                String date = documentSnapshot.getString("date");
                                String status = documentSnapshot.getString("status");
                                double amount = documentSnapshot.getDouble("total");
                                String address = documentSnapshot.getString("address");

                                ArrayList<Map<String, Object>> bookDetails = (ArrayList<Map<String, Object>>) documentSnapshot.get("bookDetails");
                                ArrayList<String> bookIds = new ArrayList<>();
                                if (bookDetails != null) {
                                    for (Map<String, Object> bookDetail : bookDetails) {
                                        String bookId = (String) bookDetail.get("bookId");
                                        if (bookId != null) {
                                            bookIds.add(bookId);
                                        }
                                    }
                                }

                                fetchBookTitles(orderId, date, status, amount, address, bookIds);
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getContext(), "Failed to fetch orders: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchBookTitles(String orderId, String date, String status, double amount, String address, ArrayList<String> bookIds) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        ArrayList<String> bookTitles = new ArrayList<>();

        if (bookIds == null || bookIds.isEmpty()) {
            orderList.add(new Order(orderId, date, status, String.valueOf(amount), address, "No Books"));
            adapter.notifyDataSetChanged();
            return;
        }

        for (String bookId: bookIds){
            firestore.collection("books")
                    .document(bookId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()){
                                bookTitles.add(task.getResult().getString("title"));
                            } else {
                                bookTitles.add("Unknown Book");
                            }

                            if (bookTitles.size() == bookIds.size()){
                                String bookList = String.join(",", bookTitles);
                                orderList.add(new Order(orderId, date, status, String.valueOf(amount), address, bookList));
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }


    static class Order {
        String orderId;
        String orderDate;
        String orderStatus;
        String totalAmount;
        String orderAddress;
        String bookTitles;

        Order(String orderId, String orderDate, String orderStatus, String totalAmount, String orderAddress, String bookTitles) {
            this.orderId = orderId;
            this.orderDate = orderDate;
            this.orderStatus = orderStatus;
            this.totalAmount = totalAmount;
            this.orderAddress = orderAddress;
            this.bookTitles = bookTitles;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<Adapter1.OrderViewHolder> {

        private ArrayList<Order> orderList;

        Adapter1(ArrayList<Order> order) {
            this.orderList = order;
        }

        @NonNull
        @Override
        public Adapter1.OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.customer_order_history_item, parent, false);
            return new Adapter1.OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Adapter1.OrderViewHolder holder, int position) {
            Order order = orderList.get(position);
            holder.orderId.setText("Order Id: " + order.orderId);
            holder.orderDate.setText("Ordered Date: " + order.orderDate);
            holder.orderList.setText(order.bookTitles);
            holder.orderStatus.setText("Status: " + order.orderStatus);
            holder.totalAmount.setText("Total: Rs. " + order.totalAmount);
            holder.address.setText(order.orderAddress);

        }

        @Override
        public int getItemCount() {

            return orderList.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, orderDate, orderList, orderStatus, totalAmount, address;

            OrderViewHolder(View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.orderIdOH);
                orderDate = itemView.findViewById(R.id.dateOH);
                orderList = itemView.findViewById(R.id.orderList);
                orderStatus = itemView.findViewById(R.id.statusOH);
                totalAmount = itemView.findViewById(R.id.totalAmountOH);
                address = itemView.findViewById(R.id.addressOH);
            }
        }
    }

}