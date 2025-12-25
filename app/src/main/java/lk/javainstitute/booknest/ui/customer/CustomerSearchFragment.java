package lk.javainstitute.booknest.ui.customer;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.SingleProductViewFragment;
import lk.javainstitute.booknest.databinding.FragmentCustomerSearchBinding;
import lk.javainstitute.booknest.ui.admin.AdminManageBooksFragment;

public class CustomerSearchFragment extends Fragment {

    private FragmentCustomerSearchBinding binding;
    private ArrayList<Book> bookList = new ArrayList<>();
    private Adapter1 adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCustomerSearchBinding.inflate(inflater, container, false);

        Spinner genreSpinner = binding.spinnerGenre;
        String genre[] = new String[]{"All", "Fantasy", "Sci-Fi", "Dystopian", "Romance ","Mystery/Thriller"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.user_type_spinner_item,
                R.id.userEmailNavC,
                genre
        );
        genreSpinner.setAdapter(arrayAdapter);
        genreSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String category = (String) parentView.getItemAtPosition(position);
                fetchBookData("", category.equals("All") ? "" : category);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                fetchBookData("", "");
            }
        });

        RecyclerView recyclerView = binding.recyclerViewBooks;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        adapter = new Adapter1(bookList);
        recyclerView.setAdapter(adapter);

        fetchBookData("", "");

        Button searchButton = binding.searchBtn;
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText searchField = binding.searchTextField;
                String searchText = searchField.getText().toString();
                String selectedGenre = genreSpinner.getSelectedItem().toString();

                if (selectedGenre.equals("genre")){
                    selectedGenre = "";
                }

                fetchBookData(searchText, selectedGenre);
            }
        });

        return binding.getRoot();
    }

    private void fetchBookData(String query, String genre) {
        CollectionReference booksRef = FirebaseFirestore.getInstance().collection("books");
        bookList.clear();

        booksRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<Book> tempBookList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String title = document.getString("title");
                    String author = document.getString("author");
                    String price = document.getString("price");
                    String cover = document.getString("imageUrl");
                    String bookGenre = document.getString("genre");

                    boolean matchesQuery = query.isEmpty() || title.toLowerCase().contains(query.toLowerCase()) || author.toLowerCase().contains(query.toLowerCase());
                    boolean matchesGenre = genre.isEmpty() || bookGenre.equalsIgnoreCase(genre) || genre.equals("All");

                    if (matchesQuery && matchesGenre) {
                        tempBookList.add(new Book(document.getId(), title, author, "Rs. " + price, cover));
                    }
                }

                bookList.clear();
                bookList.addAll(tempBookList);
                adapter.notifyDataSetChanged();

            } else {
                Log.e("FirestoreError", "Failed to fetch books", task.getException());
                Toast.makeText(getContext(), "Failed to fetch books: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        bookList.clear();
    }

    static class Book {
        String bookId;
        String bookTitle;
        String authorName;
        String bookPrice;
        String bookCoverUrl;

        Book(String bookId, String bookTitle, String authorName, String bookPrice, String bookCoverUrl) {
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.authorName = authorName;
            this.bookPrice = bookPrice;
            this.bookCoverUrl = bookCoverUrl;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<CustomerSearchFragment.Adapter1.BookViewHolder> {

        private ArrayList<CustomerSearchFragment.Book> bookList;

        Adapter1(ArrayList<CustomerSearchFragment.Book> books) {
            this.bookList = books;
        }

        @NonNull
        @Override
        public CustomerSearchFragment.Adapter1.BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.book_card_item, parent, false);
            return new CustomerSearchFragment.Adapter1.BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomerSearchFragment.Adapter1.BookViewHolder holder, int position) {
            CustomerSearchFragment.Book book = bookList.get(position);
            holder.bookTitle.setText(book.bookTitle);
            holder.authorName.setText(book.authorName);
            holder.bookPrice.setText(book.bookPrice);

            Glide.with(holder.itemView.getContext())
                    .load(book.bookCoverUrl)
                    .placeholder(R.drawable.ic_menu_gallery)
                    .error(R.drawable.delete)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.cover);

            holder.wishlist.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();

                    SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    String userId = sharedPreferences.getString("userId", null);

                    HashMap<String,Object> document = new HashMap<>();
                    document.put("userId", userId);
                    document.put("bookId", book.bookId);

                    firestore.collection("wishlist")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("bookId", book.bookId)
                            .get()
                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if (task.isSuccessful() && !task.getResult().isEmpty()){
                                        Toast.makeText(getContext(), "Already in Wishlist", Toast.LENGTH_SHORT).show();
                                    } else {
                                        HashMap<String, Object> document = new HashMap<>();
                                        document.put("userId", userId);
                                        document.put("bookId", book.bookId);

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
                                                        Toast.makeText(getContext(), "Failed to add", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            });
                }
            });

            holder.cart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
                    String userId = sharedPreferences.getString("userId", null);

                    HashMap<String, Object> document = new HashMap<>();
                    document.put("userId", userId);
                    document.put("bookId", book.bookId);
                    document.put("quantity", 1);

                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                    firestore.collection("cart")
                            .whereEqualTo("userId", userId)
                            .whereEqualTo("bookId", book.bookId)
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

            holder.viewMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SingleProductViewFragment fragmentNewProduct = new SingleProductViewFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("bookId", book.bookId);
                    fragmentNewProduct.setArguments(bundle);

                    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    fragmentTransaction.replace(R.id.constraintLayoutSP, fragmentNewProduct);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            });

        }

        @Override
        public int getItemCount() {
            return bookList.size();
        }

        class BookViewHolder extends RecyclerView.ViewHolder {
            TextView bookTitle, authorName, bookPrice;
            Button viewMore, wishlist, cart;
            ImageView cover;

            BookViewHolder(View itemView) {
                super(itemView);
                bookTitle = itemView.findViewById(R.id.titleBook);
                authorName = itemView.findViewById(R.id.nameAuthor);
                bookPrice = itemView.findViewById(R.id.priceBook);
                cover = itemView.findViewById(R.id.bookCoverSearch);
                viewMore = itemView.findViewById(R.id.viewMoreBtn);
                wishlist = itemView.findViewById(R.id.addToWL);
                cart = itemView.findViewById(R.id.addToC);
            }
        }
    }

}