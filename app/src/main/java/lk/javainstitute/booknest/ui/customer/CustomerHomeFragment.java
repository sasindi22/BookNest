package lk.javainstitute.booknest.ui.customer;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.Guideline;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentCustomerHomeBinding;
import lk.javainstitute.booknest.databinding.FragmentSellerMessagesBinding;
import lk.javainstitute.booknest.ui.seller.SellerBookListingFragment;
import lk.javainstitute.booknest.ui.seller.SellerPromotionsFragment;


public class CustomerHomeFragment extends Fragment {

    private FragmentCustomerHomeBinding binding;
    private ArrayList<Promotion> promotionList = new ArrayList<>();
    private Adapter1 adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCustomerHomeBinding.inflate(inflater, container, false);

        fetchBook1();

        VideoView videoView = binding.videoView2;
        String videoUrl = "https://drive.google.com/uc?export=download&id=17HzIxUYlYSQX5IcOgSDOZep8l7gmKJQs";
        videoView.setVideoURI(Uri.parse(videoUrl));
        videoView.start();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                videoView.start();
            }
        });

        RecyclerView recyclerView = binding.recylerViewPromo;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new Adapter1(promotionList);
        recyclerView.setAdapter(adapter);

        fetchPromotionsData();

        return binding.getRoot();
    }

    private void fetchBook1(){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
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

        View bestSeller1 = LayoutInflater.from(getContext()).inflate(R.layout.best_seller, binding.bestSeller1, false);
        View bestSeller2 = LayoutInflater.from(getContext()).inflate(R.layout.best_seller, binding.bestSeller2, false);

        binding.bestSeller1.addView(bestSeller1);
        binding.bestSeller2.addView(bestSeller2);

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

    private void fetchPromotionsData(){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("promotions")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            promotionList.clear();
                            for (DocumentSnapshot documentSnapshot:task.getResult()){
                                String title = documentSnapshot.getString("title");
                                String description = documentSnapshot.getString("description");
                                promotionList.add(new Promotion(title, description));
                            }
                            adapter.notifyDataSetChanged();
                        }else{
                            Toast.makeText(getContext(), "Failed to fetch promotions!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static class Promotion {
        String promoTitle;
        String promoDesc;

        Promotion(String promoTitle, String promoDesc) {
            this.promoTitle = promoTitle;
            this.promoDesc = promoDesc;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<CustomerHomeFragment.Adapter1.PromotionViewHolder> {

        private ArrayList<CustomerHomeFragment.Promotion> promotionList;

        Adapter1(ArrayList<CustomerHomeFragment.Promotion> promotions) {
            this.promotionList = promotions;
        }

        @NonNull
        @Override
        public CustomerHomeFragment.Adapter1.PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.promotion_item, parent, false);
            return new CustomerHomeFragment.Adapter1.PromotionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomerHomeFragment.Adapter1.PromotionViewHolder holder, int position) {
            CustomerHomeFragment.Promotion promotion = promotionList.get(position);
            holder.promoTitle.setText(promotion.promoTitle);
            holder.promoDesc.setText(promotion.promoDesc);

        }

        @Override
        public int getItemCount() {
            return promotionList.size();
        }

        class PromotionViewHolder extends RecyclerView.ViewHolder {
            TextView promoTitle, promoDesc;

            PromotionViewHolder(View itemView) {
                super(itemView);
                promoTitle = itemView.findViewById(R.id.promoTitle2);
                promoDesc = itemView.findViewById(R.id.promoDesc2);
            }
        }
    }
}