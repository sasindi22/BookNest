package lk.javainstitute.booknest.ui.seller;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.SingleProductViewFragment;
import lk.javainstitute.booknest.databinding.FragmentSellerBookListingBinding;

public class SellerBookListingFragment extends Fragment {

    private FragmentSellerBookListingBinding binding;
    private ArrayList<Book> bookList = new ArrayList<>();
    private Adapter1 adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSellerBookListingBinding.inflate(inflater, container, false);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        RecyclerView recyclerView1 = binding.recycleViewBookList;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView1.setLayoutManager(linearLayoutManager);

        adapter = new Adapter1(bookList);
        recyclerView1.setAdapter(adapter);

        fetchBookData(userId);

        return binding.getRoot();
    }

    private void fetchBookData(String userId){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("books")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null){
                        bookList.clear();
                        for (DocumentSnapshot doc : task.getResult()){
                            String title = doc.getString("title");
                            String author = doc.getString("author");

                            Object priceObj = doc.get("price");
                            String price = priceObj != null ? String.valueOf(priceObj) : "0";

                            Object stockObj = doc.get("quantity");
                            String stock = stockObj != null ? String.valueOf(stockObj) : "0";

                            String cover = doc.getString("imageUrl");
                            if (cover == null) cover = "";

                            bookList.add(new Book(doc.getId(), title, author, "Rs. "+ price, cover, stock));
                        }
                        adapter.notifyDataSetChanged();
                    }else{
                        Toast.makeText(getContext(), "Failed to fetch books!", Toast.LENGTH_SHORT).show();
                    }
                });
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
        String bookCoverUrl;
        String stock;

        Book(String bookId, String bookTitle, String authorName, String bookPrice, String bookCoverUrl, String stock) {
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.authorName = authorName;
            this.bookPrice = bookPrice;
            this.bookCoverUrl = bookCoverUrl;
            this.stock = stock;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<SellerBookListingFragment.Adapter1.BookViewHolder> {

        private ArrayList<SellerBookListingFragment.Book> bookList;

        Adapter1(ArrayList<SellerBookListingFragment.Book> books) {
            this.bookList = books;
        }

        @NonNull
        @Override
        public SellerBookListingFragment.Adapter1.BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.book_card_item1, parent, false);
            return new SellerBookListingFragment.Adapter1.BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SellerBookListingFragment.Adapter1.BookViewHolder holder, int position) {
            SellerBookListingFragment.Book book = bookList.get(position);
            holder.bookTitle.setText(book.bookTitle);
            holder.authorName.setText(book.authorName);
            holder.bookPrice.setText(book.bookPrice);

            Glide.with(holder.itemView.getContext())
                    .load(book.bookCoverUrl)
                    .placeholder(R.drawable.ic_menu_gallery)
                    .error(R.drawable.delete)
                    .into(holder.bookCover);

            holder.more.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SingleProductViewFragment fragment = new SingleProductViewFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("bookId", book.bookId);
                    fragment.setArguments(bundle);

                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.constraintLayoutSP, fragment);
                    transaction.commit();

                }
            });

            holder.update.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    View dialogView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.books_update_dialog, null);

                    EditText newStock = dialogView.findViewById(R.id.newStockUpdate);
                    TextView totalStock = dialogView.findViewById(R.id.totalStockUpdated);
                    EditText newPrice = dialogView.findViewById(R.id.newPriceUpdate);
                    EditText newDescription = dialogView.findViewById(R.id.newDescriptionUpdate);
                    Button updateBook = dialogView.findViewById(R.id.bookDataUpdateBtn);

                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setView(dialogView);
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    updateBook.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                            String newStockString = newStock.getText().toString();
                            String newPriceString = newPrice.getText().toString();
                            String newDescString = newDescription.getText().toString();

                            Map<String, Object> updates = new HashMap<>();

                            if (!newStockString.isEmpty()) {
                                try {
                                    int currentStock = Integer.parseInt(book.stock);
                                    int additionalStock = Integer.parseInt(newStockString);
                                    int updatedStock = currentStock + additionalStock;
                                    updates.put("quantity", String.valueOf(updatedStock));
                                    book.stock = String.valueOf(updatedStock);
                                } catch (NumberFormatException e) {
                                    Toast.makeText(view.getContext(), "Invalid stock value!", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }

                            if (!newPriceString.isEmpty()) {
                                updates.put("price", newPriceString);
                                book.bookPrice = "Rs. " + newPriceString;
                            }

                            if (!newDescString.isEmpty()) {
                                updates.put("description", newDescString);
                            }

                            if (!updates.isEmpty()) {
                                firestore.collection("books")
                                        .document(book.bookId)
                                        .update(updates)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void unused) {
                                                Toast.makeText(view.getContext(), "Book Details Updated!", Toast.LENGTH_SHORT).show();
                                                adapter.notifyDataSetChanged();
                                                dialog.dismiss();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Toast.makeText(view.getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(view.getContext(), "No changes to update!", Toast.LENGTH_SHORT).show();
                            }

                            if (!newStockString.isEmpty()) {
                                totalStock.setText("Total Stock After Update: " + book.stock);
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
            TextView bookTitle, authorName, bookPrice;
            ImageView bookCover;
            Button more, update;

            BookViewHolder(View itemView) {
                super(itemView);
                bookTitle = itemView.findViewById(R.id.bookTitleBL);
                authorName = itemView.findViewById(R.id.authorNameBL);
                bookPrice = itemView.findViewById(R.id.bookPriceBL);
                bookCover = itemView.findViewById(R.id.bookCoverBL);
                more = itemView.findViewById(R.id.moreBL);
                update = itemView.findViewById(R.id.updateBL);
            }
        }
    }
}
