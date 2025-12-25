package lk.javainstitute.booknest.ui.admin;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.SingleProductViewFragment;
import lk.javainstitute.booknest.databinding.FragmentAdminManageBooksBinding;

public class AdminManageBooksFragment extends Fragment {

    private FragmentAdminManageBooksBinding binding;
    private ArrayList<Book> bookList = new ArrayList<>();
    private Adapter1 adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentAdminManageBooksBinding.inflate(inflater, container, false);

        Spinner genreSpinner = binding.spinnerGenreAdmin;
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

        RecyclerView recyclerView = binding.recyclerViewBooksAdmin;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        adapter = new Adapter1(bookList);
        recyclerView.setAdapter(adapter);

        fetchBookData("", "");

        Button searchButton = binding.searchAdminBtn;
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText searchField = binding.searchAdmin;
                String searchText = searchField.getText().toString().trim();
                String selectedGenre = genreSpinner.getSelectedItem().toString();

                if (searchText.isEmpty()) {
                    fetchBookData("", selectedGenre.equals("All") ? "" : selectedGenre);
                } else {
                    fetchBookData(searchText, selectedGenre.equals("All") ? "" : selectedGenre);
                }
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

    class Adapter1 extends RecyclerView.Adapter<AdminManageBooksFragment.Adapter1.BookViewHolder> {

        private ArrayList<AdminManageBooksFragment.Book> bookList;

        Adapter1(ArrayList<AdminManageBooksFragment.Book> books) {
            this.bookList = books;
        }

        @NonNull
        @Override
        public AdminManageBooksFragment.Adapter1.BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.book_card_item_admin, parent, false);
            return new AdminManageBooksFragment.Adapter1.BookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AdminManageBooksFragment.Adapter1.BookViewHolder holder, int position) {
            AdminManageBooksFragment.Book book = bookList.get(position);
            holder.bookTitle.setText(book.bookTitle);
            holder.authorName.setText(book.authorName);
            holder.bookPrice.setText(book.bookPrice);

            Glide.with(holder.itemView.getContext())
                    .load(book.bookCoverUrl)
                    .placeholder(R.drawable.ic_menu_gallery)
                    .error(R.drawable.delete)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.cover);

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

            holder.remove.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                    firestore.collection("books").document(book.bookId)
                            .delete()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    int position = holder.getAdapterPosition();
                                    if (position != RecyclerView.NO_POSITION) {
                                        bookList.remove(position);
                                        notifyItemRemoved(position);
                                    }
                                    Toast.makeText(getActivity(), "Book removed successfully", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getActivity(), "Failed to remove book", Toast.LENGTH_SHORT).show();
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
            Button viewMore, remove;
            ImageView cover;

            BookViewHolder(View itemView) {
                super(itemView);
                bookTitle = itemView.findViewById(R.id.titleBook2);
                authorName = itemView.findViewById(R.id.nameAuthor2);
                bookPrice = itemView.findViewById(R.id.priceBook2);
                cover = itemView.findViewById(R.id.imageBookAdmin);
                viewMore = itemView.findViewById(R.id.viewMoreBtnAdmin);
                remove = itemView.findViewById(R.id.removeBookAdmin);
            }
        }
    }
}