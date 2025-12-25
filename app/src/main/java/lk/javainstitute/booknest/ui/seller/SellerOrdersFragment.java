package lk.javainstitute.booknest.ui.seller;

import static android.content.Context.DISPLAY_HASH_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentSellerOrdersBinding;

public class SellerOrdersFragment extends Fragment {

    private FragmentSellerOrdersBinding binding;
    private ArrayList<Order> orderList = new ArrayList<>();
    private Adapter1 adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSellerOrdersBinding.inflate(inflater, container, false);

        RecyclerView recyclerView1 = binding.recylerView1;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView1.setLayoutManager(linearLayoutManager);

        adapter = new Adapter1(orderList);
        recyclerView1.setAdapter(adapter);

        fetchOrders();

        return binding.getRoot();
    }

    private void fetchOrders() {
        orderList.clear();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("orders")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot orderDoc : task.getResult()) {
                                String orderId = orderDoc.getId();
                                String date = orderDoc.getString("date");
                                String customerId = orderDoc.getString("userId");
                                ArrayList<HashMap<String, Object>> bookDetails = (ArrayList<HashMap<String, Object>>) orderDoc.get("bookDetails");
                                double totalAmount = orderDoc.getDouble("total");
                                String orderStatus = orderDoc.getString("status");

                                Log.d("SellerOrdersFragment", "Fetched order: " + orderId + ", bookDetails: " + bookDetails + ", totalAmount: " + totalAmount);

                                ArrayList<String> bookIds = new ArrayList<>();
                                for (HashMap<String, Object> bookDetail : bookDetails) {
                                    String bookId = (String) bookDetail.get("bookId");
                                    if (bookId != null) {
                                        bookIds.add(bookId);
                                    }
                                }

                                firestore.collection("user").document(customerId)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot userDoc = task.getResult();
                                                    String customerName = userDoc.getString("fname") + " " + userDoc.getString("lname");

                                                    fetchBookTitles(orderId, date, customerName, bookIds, totalAmount, orderStatus);
                                                }
                                            }
                                        });
                            }
                        } else {
                            Log.e("SellerOrdersFragment", "Error fetching orders", task.getException());
                        }
                    }
                });
    }

    private void fetchBookTitles(String orderId, String date, String customerName, ArrayList<String> bookIds, double totalAmount, String orderStatus) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("books")
                .whereIn("__name__", bookIds)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            StringBuilder bookTitles = new StringBuilder();

                            for (DocumentSnapshot bookDoc : task.getResult()) {
                                String bookTitle = bookDoc.getString("title");
                                bookTitles.append(bookTitle).append(", ");
                            }

                            if (bookTitles.length() > 0) {
                                bookTitles.setLength(bookTitles.length() - 2);
                            }

                            orderList.add(new Order(orderId, customerName, date, String.valueOf(totalAmount), bookTitles.toString(), orderStatus));
                            adapter.notifyDataSetChanged();

                            Log.d("SellerOrdersFragment", "Added order: " + orderId + ", totalAmount: " + totalAmount + ", bookTitles: " + bookTitles);
                        } else {
                            Log.e("SellerOrdersFragment", "Error fetching books", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static class Order {
        String orderId;
        String customerName;
        String orderDate;
        String orderPrice;
        String bookList;
        String orderStatus;

        Order(String orderId, String customerName, String orderDate, String orderPrice, String bookList, String orderStatus) {
            this.orderId = orderId;
            this.customerName = customerName;
            this.orderDate = orderDate;
            this.orderPrice = orderPrice;
            this.bookList = bookList;
            this.orderStatus = orderStatus;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<Adapter1.OrderViewHolder> {

        private ArrayList<Order> orderList;

        Adapter1(ArrayList<Order> orders) {
            this.orderList = orders;
        }

        @NonNull
        @Override
        public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.order_card_item, parent, false);
            return new OrderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
            Order order = orderList.get(position);
            holder.orderId.setText(order.orderId);
            holder.customerName.setText(order.customerName);
            holder.orderDate.setText(order.orderDate);
            holder.orderPrice.setText(order.orderPrice);
            holder.bookList.setText(order.bookList);
            holder.orderStatus.setText(order.orderStatus);

            holder.update.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    View dialogView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.order_status_update_dialog, null);
                    RadioGroup radioGroup = dialogView.findViewById(R.id.radioGroupUpdate);
                    Button btnStatusUpdate = dialogView.findViewById(R.id.buttonUpdateStatus);

                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setView(dialogView);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    btnStatusUpdate.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            int selectedId = radioGroup.getCheckedRadioButtonId();
                            String selectedStatus = "";

                            if (selectedId == R.id.radioButtonPaid){
                                selectedStatus = "Paid";
                            } else if (selectedId == R.id.radioButtonShipped) {
                                selectedStatus = "Shipped";
                            } else if (selectedId == R.id.radioButtonDelivered) {
                                selectedStatus = "Delivered";
                            } else if (selectedId == R.id.radioButtonCancelled) {
                                selectedStatus = "Canceled";
                            }

                            if (!selectedStatus.isEmpty()){
                                updateOrderStatus(order.orderId, selectedStatus);
                                dialog.dismiss();
                            } else {
                                Toast.makeText(holder.itemView.getContext(), "Please select a status", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });
        }

        private void updateOrderStatus(String orderId, String status){
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("orders")
                    .document(orderId)
                    .update("status", status)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(getContext(), "Status updated to " + status, Toast.LENGTH_SHORT).show();
                                fetchOrders();
                            } else {
                                Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        @Override
        public int getItemCount() {
            return orderList.size();
        }

        class OrderViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, customerName, orderDate, orderPrice, bookList, orderStatus;
            Button update;

            OrderViewHolder(View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.orderId);
                orderDate = itemView.findViewById(R.id.orderDateSeller);
                customerName = itemView.findViewById(R.id.customerName);
                bookList = itemView.findViewById(R.id.orderListSeller);
                orderPrice = itemView.findViewById(R.id.orderPrice);
                orderStatus = itemView.findViewById(R.id.orderStatus);
                update = itemView.findViewById(R.id.updateButton);
            }
        }
    }
}