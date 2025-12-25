package lk.javainstitute.booknest.ui.customer;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentCustomerWishlistBinding;
import lk.javainstitute.booknest.databinding.FragmentSellerMessagesBinding;
import lk.javainstitute.booknest.ui.seller.SellerPromotionsFragment;

public class CustomerWishlistFragment extends Fragment {

    private FragmentCustomerWishlistBinding binding;
    private Adapter1 adapter;
    private ArrayList<Book> bookList = new ArrayList<>();
    private FirebaseFirestore firestore;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCustomerWishlistBinding.inflate(inflater, container, false);

        RecyclerView recyclerView = binding.recycleViewWL;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new Adapter1(bookList);
        recyclerView.setAdapter(adapter);

        firestore = FirebaseFirestore.getInstance();

        loadWishlist();

        return binding.getRoot();
    }

    private void loadWishlist(){
        firestore.collection("wishlist")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        ArrayList<String> bookIds = new ArrayList<>();
                        for (QueryDocumentSnapshot document: queryDocumentSnapshots){
                            String bookId = document.getString("bookId");
                            if (bookId != null){
                                bookIds.add(bookId);
                            }
                        }
                        if (!bookIds.isEmpty()){
                            loadBooks(bookIds);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failed to load wishlist!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadBooks(ArrayList<String> bookIds){
        for (String bookId : bookIds){
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
                                String itemCount = documentSnapshot.getString("quantity");

                                Book book = new Book(bookId, imageUrl,title, author, price, itemCount);
                                bookList.add(book);
                                adapter.notifyDataSetChanged();
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
        String imageUrl;
        String bookTitle;
        String authorName;
        String bookPrice;
        String itemCount;

        Book(String bookId, String imageUrl, String bookTitle, String authorName, String bookPrice, String itemCount) {
            this.bookId = bookId;
            this.imageUrl = imageUrl;
            this.bookTitle = bookTitle;
            this.authorName = authorName;
            this.bookPrice = bookPrice;
            this.itemCount = itemCount;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<CustomerWishlistFragment.Adapter1.BookViewHolder> {

        private ArrayList<CustomerWishlistFragment.Book> bookList;

        Adapter1(ArrayList<CustomerWishlistFragment.Book> books) {
            this.bookList = books;
        }

        @NonNull
        @Override
        public CustomerWishlistFragment.Adapter1.BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.wishlist_card_item, parent, false);
            return new CustomerWishlistFragment.Adapter1.BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomerWishlistFragment.Adapter1.BookViewHolder holder, int position) {
            CustomerWishlistFragment.Book books = bookList.get(position);
            holder.bookTitle.setText(books.bookTitle);
            holder.authorName.setText(books.authorName);
            holder.bookPrice.setText("Rs. " + books.bookPrice);
            holder.itemCount.setText("Item Count : " + books.itemCount);

            Glide.with(holder.imageUrl.getContext())
                            .load(books.imageUrl)
                            .into(holder.imageUrl);

            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    int currentPosition = holder.getAdapterPosition();

                    if (currentPosition != RecyclerView.NO_POSITION){
                        String bookId = books.bookId;

                        firestore.collection("wishlist")
                                .whereEqualTo("bookId", bookId)
                                .get()
                                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        for (DocumentSnapshot documentSnapshot:queryDocumentSnapshots.getDocuments()){
                                            documentSnapshot.getReference().delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void unused) {
                                                            bookList.remove(currentPosition);
                                                            notifyItemRemoved(currentPosition  );
                                                            Toast.makeText(getContext(), "Item Deleted!", Toast.LENGTH_LONG).show();
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(getContext(), "Error deleting item from database!", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getContext(), "Error fetching item for deletion!", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }
            });

            holder.addToCart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    String userId = sharedPreferences.getString("userId", null);

                    HashMap<String, Object> document = new HashMap<>();
                    document.put("userId", userId);
                    document.put("bookId", books.bookId);
                    document.put("quantity", 1);

                    firestore.collection("cart")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("bookId", books.bookId)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                    if (task.isSuccessful() && !task.getResult().isEmpty()){

                                        DocumentSnapshot cart = task.getResult().getDocuments().get(0);
                                        int currentQuantity = cart.getLong("quantity").intValue();
                                        int updateQuantity = currentQuantity + 1;

                                        cart.getReference().update("quantity", updateQuantity)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        Toast.makeText(getContext(), "Item quantity updated in Cart!", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Toast.makeText(getContext(), "Failed to update item quantity", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    } else {
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
                                                        Toast.makeText(getContext(), "Failed to add item to Cart", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            });

                }
            });

        }

        @Override
        public int getItemCount() {
            return bookList.size();
        }

        class BookViewHolder extends RecyclerView.ViewHolder {
            TextView bookTitle, authorName, bookPrice, itemCount;
            Button delete, addToCart;
            ImageView imageUrl;

            BookViewHolder(View itemView) {
                super(itemView);
                bookTitle = itemView.findViewById(R.id.titleWL);
                authorName = itemView.findViewById(R.id.authorWL);
                bookPrice = itemView.findViewById(R.id.priceWL);
                itemCount = itemView.findViewById(R.id.countWL);
                delete = itemView.findViewById(R.id.deleteBtnWL);
                addToCart = itemView.findViewById(R.id.cartBtnWL);
                imageUrl = itemView.findViewById(R.id.imageWL);
            }
        }
    }

}