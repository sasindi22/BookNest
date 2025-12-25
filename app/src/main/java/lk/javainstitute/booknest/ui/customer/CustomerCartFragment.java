package lk.javainstitute.booknest.ui.customer;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.SingleProductViewFragment;
import lk.javainstitute.booknest.databinding.FragmentCustomerCartBinding;
import lk.javainstitute.booknest.databinding.FragmentCustomerSearchBinding;

public class CustomerCartFragment extends Fragment {

    private FragmentCustomerCartBinding binding;
    private Adapter1 adapter;
    private ArrayList<Book> bookList = new ArrayList<>();
    private FirebaseFirestore firestore;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCustomerCartBinding.inflate(inflater, container, false);

        RecyclerView recyclerView = binding.recycleViewCart;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new Adapter1(bookList);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        loadCartItems();

        Button btnCheckout = binding.checkoutBtn;
        btnCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomerCheckoutFragment fragmentCheckout = new CustomerCheckoutFragment();
                Bundle bundle = new Bundle();
                fragmentCheckout.setArguments(bundle);

                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.constraintLayoutSP, fragmentCheckout);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        });

        return binding.getRoot();
    }

    private void loadCartItems(){
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);
        firestore.collection("cart")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        ArrayList<String> bookIds = new ArrayList<>();
                        ArrayList<Integer> quantities = new ArrayList<>();

                        for (QueryDocumentSnapshot document: queryDocumentSnapshots){
                            String bookId = document.getString("bookId");
                            Long quantityLong = document.getLong("quantity");

                            int quantity = quantityLong != null? quantityLong.intValue():0;

                            if (bookId != null){
                                bookIds.add(bookId);
                                quantities.add(quantity);
                            }
                        }
                        if (!bookIds.isEmpty()){
                            loadBooks(bookIds, quantities);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failed to load cart!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadBooks(ArrayList<String> bookIds, ArrayList<Integer> quantities){
        ArrayList<Book> newBooks = new ArrayList<>();

        for (int i = 0; i < bookIds.size(); i++) {
            String bookId = bookIds.get(i);
            int quantity = quantities.get(i);

            firestore.collection("books")
                    .document(bookId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()){
                                String title = documentSnapshot.getString("title");
                                String author = documentSnapshot.getString("author");
                                String price = documentSnapshot.getString("price");
                                String imageUrl = documentSnapshot.getString("imageUrl");

                                boolean exists = false;
                                for (Book book : bookList) {
                                    if (book.bookId.equals(bookId)) {
                                        exists = true;
                                        break;
                                    }
                                }

                                if (!exists) {
                                    Book book = new Book(bookId, title, imageUrl, author, price, quantity);
                                    newBooks.add(book);
                                }

                                if (newBooks.size() == bookIds.size()) {
                                    bookList.clear();
                                    bookList.addAll(newBooks);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), "Error loading book data!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static class Book {
        String bookId;
        String bookTitle;
        String authorName;
        String bookPrice;
        String totalPrice;
        String imageUrl;
        int quantity;

        Book(String bookId, String bookTitle,String imageUrl, String authorName, String bookPrice, int quantity) {
            this.bookId = bookId;
            this.imageUrl = imageUrl;
            this.bookTitle = bookTitle;
            this.authorName = authorName;
            this.bookPrice = "Rs. " + bookPrice;
            this.quantity = quantity;
            this.totalPrice = "Rs. " + (quantity * Integer.parseInt(bookPrice));
        }
    }

    class Adapter1 extends RecyclerView.Adapter<CustomerCartFragment.Adapter1.BookViewHolder> {

        private ArrayList<CustomerCartFragment.Book> bookList;

        Adapter1(ArrayList<CustomerCartFragment.Book> books) {
            this.bookList = books;
        }

        @NonNull
        @Override
        public CustomerCartFragment.Adapter1.BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cart_card_item, parent, false);
            return new CustomerCartFragment.Adapter1.BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomerCartFragment.Adapter1.BookViewHolder holder, int position) {
            CustomerCartFragment.Book books = bookList.get(position);
            holder.bookTitle.setText(books.bookTitle);
            holder.authorName.setText(books.authorName);
            holder.bookPrice.setText(books.bookPrice);
            holder.totalPrice.setText("Total: " + books.totalPrice);
            holder.quantityText.setText(String.valueOf(books.quantity));

            Glide.with(holder.imageUrl.getContext())
                    .load(books.imageUrl)
                    .placeholder(R.drawable.ic_menu_gallery)
                    .error(R.drawable.delete)
                    .into(holder.imageUrl);

            holder.increaseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int currentQuantity = books.quantity;
                    currentQuantity++;

                    books.quantity = currentQuantity;
                    books.totalPrice = "Rs. " + (currentQuantity * Integer.parseInt(books.bookPrice.replace("Rs. ", "")));

                    holder.quantityText.setText(String.valueOf(currentQuantity));
                    holder.totalPrice.setText("Total: " + books.totalPrice);

                    updateCartQuantity(books.bookId, currentQuantity);
                }
            });

            holder.decreaseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int currentQuantity = books.quantity;
                    if ( currentQuantity > 1 ){
                        currentQuantity--;

                        books.quantity = currentQuantity;
                        books.totalPrice = "Rs. " + (currentQuantity * Integer.parseInt(books.bookPrice.replace("Rs. ", "")));

                        holder.quantityText.setText(String.valueOf(currentQuantity));
                        holder.totalPrice.setText("Total: " + books.totalPrice);
                        updateCartQuantity(books.bookId, currentQuantity);
                    }
                }
            });

            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int currentPosition = holder.getAdapterPosition();

                    firestore.collection("cart")
                            .whereEqualTo("bookId", books.bookId)
                            .get()
                            .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                @Override
                                public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                    if (!queryDocumentSnapshots.isEmpty()){
                                        for (DocumentSnapshot documentSnapshot: queryDocumentSnapshots.getDocuments()){
                                            documentSnapshot.getReference().delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            bookList.remove(currentPosition);
                                                            notifyItemRemoved(currentPosition);
                                                            notifyItemRangeChanged(currentPosition, bookList.size());
                                                            Toast.makeText(getContext(), "Item Deleted!", Toast.LENGTH_LONG).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(getContext(), "Error deleting from the database!", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getContext(), "Error deleting item!", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });

        }

        @Override
        public int getItemCount() {
            return bookList.size();
        }

        private void updateCartQuantity(String bookId, int newQuantity) {
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String userId = sharedPreferences.getString("userId", null);

            firestore.collection("cart")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("bookId", bookId)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (!queryDocumentSnapshots.isEmpty()){
                                for (DocumentSnapshot documentSnapshot: queryDocumentSnapshots.getDocuments()){
                                    documentSnapshot.getReference().update("quantity", newQuantity)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Toast.makeText(getContext(), "Quantity updated!", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(getContext(), "Failed to update quantity!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), "Error updating quantity!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        class BookViewHolder extends RecyclerView.ViewHolder {
            TextView bookTitle, authorName, bookPrice, totalPrice, quantityText;
            Button increaseButton, decreaseButton, delete;
            ImageView imageUrl;

            BookViewHolder(View itemView) {
                super(itemView);
                bookTitle = itemView.findViewById(R.id.titleWL2);
                authorName = itemView.findViewById(R.id.authorWL2);
                bookPrice = itemView.findViewById(R.id.priceWL2);
                totalPrice = itemView.findViewById(R.id.totalPrice);
                quantityText = itemView.findViewById(R.id.quantityText);
                increaseButton = itemView.findViewById(R.id.increaseButton);
                decreaseButton = itemView.findViewById(R.id.decreaseButton);
                delete = itemView.findViewById(R.id.deleteBtnWL2);
                imageUrl = itemView.findViewById(R.id.imageCart);
            }
        }
    }

}