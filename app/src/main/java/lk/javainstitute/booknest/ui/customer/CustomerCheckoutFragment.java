package lk.javainstitute.booknest.ui.customer;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentCustomerCheckoutBinding;
import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;
import lk.payhere.androidsdk.model.StatusResponse;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomerCheckoutFragment extends Fragment {

    private FragmentCustomerCheckoutBinding binding;
    private FirebaseFirestore firestore;
    private Adapter1 adapter;
    private ArrayList<Book> bookList = new ArrayList<>();

    private static final String TAG = "PayHereDemo";

    private final ActivityResultLauncher<Intent> payHereLauncher = registerForActivityResult(

            new ActivityResultContracts.StartActivityForResult(),
            result -> {

                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                    Intent data = result.getData();

                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)){
                        Serializable serializable = data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                        if (serializable instanceof PHResponse){
                            PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) serializable;

                            if (response.isSuccess()) {
                                String msg = "Payment Success: " + response.getData();
                                Log.d(TAG, msg);

                                double totalSum = calculateTotalSum(bookList);
                                String address = binding.addressCheckout.getText().toString();
                                String orderId = firestore.collection("orders").document().getId();
                                saveOrder(totalSum, address, orderId);

                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .detach(CustomerCheckoutFragment.this)
                                        .attach(CustomerCheckoutFragment.this)
                                        .commit();

                            } else {
                                String msg = "Payment Failed: " + response;
                                Log.d(TAG, msg);
                            }
                        }
                    }
                } else if (result.getResultCode() ==  Activity.RESULT_CANCELED){
                    Log.d(TAG, "Failed to do the Payment!");
                }
            }
    );

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCustomerCheckoutBinding.inflate(inflater, container, false);

        RecyclerView recyclerView = binding.recyclerViewCheckout;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new Adapter1(bookList);
        recyclerView.setAdapter(adapter);

        EditText name = binding.nameCheckout;
        EditText mobile = binding.mobileCheckout;
        EditText email = binding.emailCheckout;
        EditText address = binding.addressCheckout;
        TextView totalCheckout = binding.totalCheckout;

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        firestore = FirebaseFirestore.getInstance();
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
                                String  lname = documentSnapshot.getString("lname");

                                name.setText(fname + " " + lname);
                                email.setText(documentSnapshot.getString("email"));
                                mobile.setText(documentSnapshot.getString("mobile"));
                                address.setText(documentSnapshot.getString("address"));

                                mobile.setEnabled(false);
                                email.setEnabled(false);
                            }
                        }
                    }
                });

        firestore.collection("cart")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            for (DocumentSnapshot documentSnapshot: task.getResult()){
                                String bookId = documentSnapshot.getString("bookId");
                                int quantity = documentSnapshot.getLong("quantity").intValue();

                                firestore.collection("books")
                                        .document(bookId)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()){
                                                    DocumentSnapshot document = task.getResult();
                                                    if (document.exists()){
                                                        String bookId = document.getId();
                                                        String title = document.getString("title");
                                                        String price = document.getString("price");

                                                        bookList.add(new Book(bookId, title, price, quantity));
                                                        adapter.notifyDataSetChanged();

                                                        double totalSum = calculateTotalSum(bookList);
                                                        totalCheckout.setText(String.valueOf(totalSum));
                                                    }
                                                } else {
                                                    Toast.makeText(getContext(), "Failed to fetch book details!", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to fetch cart items!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        Button checkoutBtn = binding.checkoutButton;
        checkoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initiatePayment();
            }
        });

        return binding.getRoot();
    }

    private void initiatePayment(){

        String name = binding.nameCheckout.getText().toString();
        String email = binding.emailCheckout.getText().toString();
        String mobile = binding.mobileCheckout.getText().toString();
        String address = binding.addressCheckout.getText().toString();

        String[] names = name.split(" ");
        String firstName = names.length > 0 ? names[0] : "";
        String lastName = names.length > 1 ? names[1] : "";


        double totalSum = calculateTotalSum(bookList);

        String orderId = firestore.collection("orders").document().getId();

        InitRequest req = new InitRequest();
        req.setMerchantId("1227368"); // Replace with your actual Merchant ID
        req.setCurrency("LKR");
        req.setAmount(totalSum); // Example amount
        req.setOrderId(orderId);
        req.setItemsDescription("BookNest Payment!");

        req.getCustomer().setFirstName(firstName);
        req.getCustomer().setLastName(lastName);
        req.getCustomer().setEmail(email);
        req.getCustomer().setPhone(mobile);
        req.getCustomer().getAddress().setAddress(address);
        req.getCustomer().getAddress().setCity("");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        req.getCustomer().getDeliveryAddress().setAddress(address);
        req.getCustomer().getDeliveryAddress().setCity("");
        req.getCustomer().getDeliveryAddress().setCountry("Sri Lanka");

        for (Book book : bookList) {
            double price = Double.parseDouble(book.bookPrice.replace("", ""));
            req.getItems().add(new Item(null, book.bookTitle, book.quantity, price));
        }

        Intent intent = new Intent(getContext(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);

        payHereLauncher.launch(intent);

    }

    private void saveOrder(double totalSum, String address, String orderId){
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        ArrayList<HashMap<String, Object>> bookDetails = new ArrayList<>();
        for (Book book : bookList) {
            HashMap<String, Object> bookInfo = new HashMap<>();
            bookInfo.put("bookId", book.bookId);
            bookInfo.put("quantity", book.quantity);
            bookDetails.add(bookInfo);
        }

        HashMap<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", orderId);
        orderData.put("userId", userId);
        orderData.put("bookDetails", bookDetails);
        orderData.put("address", address + ", Sri Lanka");
        orderData.put("date", currentDate);
        orderData.put("total", totalSum);
        orderData.put("status", "Paid");

        firestore.collection("orders")
                .document(orderId)
                .set(orderData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Log.d(TAG, "Order saved successfully!");
                            reduceStockQuantity(bookList);
                            clearUserCart(userId);
                        } else {
                            Log.e(TAG, "Failed to save orders: " + task.getException());
                        }
                    }
                });
    }

    private void reduceStockQuantity(ArrayList<Book> bookList) {
        for (Book book: bookList) {
            String bookId = book.bookId;
            int purchaseQuantity = book.quantity;

            firestore.collection("books")
                    .document(bookId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()){
                                DocumentSnapshot documentSnapshot = task.getResult();
                                if (documentSnapshot.exists()){
                                    String  currentQuantityString = documentSnapshot.getString("quantity");
                                    if (currentQuantityString != null && !currentQuantityString.isEmpty()){
                                        try {
                                            long currentQuantity = Long.parseLong(currentQuantityString);
                                            long newQuantity = currentQuantity - purchaseQuantity;
                                            if (newQuantity<0){
                                                newQuantity=0;
                                            }
                                            firestore.collection("books")
                                                    .document(bookId)
                                                    .update("quantity", String.valueOf(newQuantity))
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task) {
                                                            if (task.isSuccessful()){
                                                                Log.d(TAG, "Stock quantity updated for book: " + bookId);
                                                            }
                                                        }
                                                    });
                                        } catch (NumberFormatException e){
                                            Log.e(TAG, "Failed to parse quantity for book: " + bookId, e);
                                        }
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private void clearUserCart(String userId){
        firestore.collection("cart")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            for (DocumentSnapshot documentSnapshot: task.getResult()){
                                documentSnapshot.getReference().delete();
                            }
                            Log.d(TAG, "Cart cleared successfully!");
                        } else {
                            Log.e(TAG, "Failed to clear cart: " + task.getException());
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private double calculateTotalSum(ArrayList<Book> bookList){
        double totalSum = 0.0;
        for (Book book:bookList){
            double price = Double.parseDouble(book.bookPrice.replace("",""));
            totalSum += (price * book.quantity);
        }
        return totalSum;
    }

    static class Book {
        String bookId;
        String bookTitle;
        String bookPrice;
        int quantity;

        Book(String bookId, String bookTitle, String bookPrice, int quantity) {
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.bookPrice = bookPrice;
            this.quantity = quantity;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<CustomerCheckoutFragment.Adapter1.BookViewHolder> {

        private ArrayList<CustomerCheckoutFragment.Book> bookList;

        Adapter1(ArrayList<CustomerCheckoutFragment.Book> books) {
            this.bookList = books;
        }

        @NonNull
        @Override
        public CustomerCheckoutFragment.Adapter1.BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.checkout_item, parent, false);
            return new CustomerCheckoutFragment.Adapter1.BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomerCheckoutFragment.Adapter1.BookViewHolder holder, int position) {
            CustomerCheckoutFragment.Book books = bookList.get(position);
            holder.bookTitle.setText(books.bookTitle);

            double price = Double.parseDouble(books.bookPrice.replace("",""));
            double totalPrice = price * books.quantity;

            String formattedTotalPrice = "Rs. " + totalPrice;
            holder.bookPrice.setText(formattedTotalPrice);
        }

        @Override
        public int getItemCount() {
            return bookList.size();
        }

        class BookViewHolder extends RecyclerView.ViewHolder {
            TextView bookTitle,bookPrice;

            BookViewHolder(View itemView) {
                super(itemView);
                bookTitle = itemView.findViewById(R.id.bookNameCheckout);
                bookPrice = itemView.findViewById(R.id.bookPriceCheckout);
            }
        }
    }

}