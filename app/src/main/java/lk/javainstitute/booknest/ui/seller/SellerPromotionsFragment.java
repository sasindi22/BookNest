package lk.javainstitute.booknest.ui.seller;

import static android.content.Context.MODE_PRIVATE;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentSellerMessagesBinding;
import lk.javainstitute.booknest.databinding.FragmentSellerPromotionsBinding;

public class SellerPromotionsFragment extends Fragment {

    private FragmentSellerPromotionsBinding binding;
    private Adapter1 adapter1;
    private ArrayList<Promotion> promotionList = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSellerPromotionsBinding.inflate(inflater, container, false);

        EditText title = binding.promotionTitle;
        EditText description = binding.promotionDesc;
        Button save = binding.savePromotionBtn;

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("userId", null);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String titleText = title.getText().toString();
                String descText = description.getText().toString();

                if (titleText.isEmpty() || descText.isEmpty()) {
                    Toast.makeText(getContext(), "Please fill in both fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                HashMap<String, String> document = new HashMap<>();
                document.put("title", titleText);
                document.put("description", descText);
                document.put("sellerId", userId);

                firestore.collection("promotions")
                        .add(document)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Toast.makeText(getContext(), "Promotion Saved!", Toast.LENGTH_SHORT).show();
                                title.setText("");
                                description.setText("");
                                fetchPromotions(userId);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getContext(), "Error saving promotion", Toast.LENGTH_SHORT).show();
                            }
                        });

            }
        });

        setupRecyclerView();
        fetchPromotions(userId);

        RecyclerView recyclerView1 = binding.recyclerViewPromo;

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView1.setLayoutManager(linearLayoutManager);

        return binding.getRoot();
    }

    private void setupRecyclerView(){
        RecyclerView recyclerView1 = binding.recyclerViewPromo;
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView1.setLayoutManager(linearLayoutManager);
        adapter1 = new Adapter1(promotionList);
        recyclerView1.setAdapter(adapter1);
    }

    private void fetchPromotions(String userId){
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("promotions")
                .whereEqualTo("sellerId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            promotionList.clear();
                            for (DocumentSnapshot documentSnapshot: task.getResult()){
                                String promoId = documentSnapshot.getId();
                                String title = documentSnapshot.getString("title");
                                String description = documentSnapshot.getString("description");
                                promotionList.add(new Promotion(title, description, promoId));
                            }
                            adapter1.notifyDataSetChanged();
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
        String promoId;

        Promotion(String promoTitle, String promoDesc, String promoId) {
            this.promoTitle = promoTitle;
            this.promoDesc = promoDesc;
            this.promoId = promoId;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<SellerPromotionsFragment.Adapter1.PromotionViewHolder> {

        private final ArrayList<Promotion> promotionList;

        Adapter1(ArrayList<Promotion> promotions) {
            this.promotionList = promotions;
        }

        @NonNull
        @Override
        public PromotionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.new_promotion_item, parent, false);
            return new PromotionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PromotionViewHolder holder, int position) {
            Promotion promotion = promotionList.get(position);
            holder.promoTitle.setText(promotion.promoTitle);
            holder.promoDesc.setText(promotion.promoDesc);

            holder.delete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        String promoId = promotion.promoId;

                        FirebaseFirestore firestore =  FirebaseFirestore.getInstance();
                        firestore.collection("promotions")
                                .document(promoId)
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(getContext(), "Promotion Deleted!", Toast.LENGTH_SHORT).show();
                                        promotionList.remove(adapterPosition);
                                        notifyItemRemoved(adapterPosition);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(getContext(), "Error deleting promotion", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }
            });

        }

        @Override
        public int getItemCount() {
            return promotionList.size();
        }

        class PromotionViewHolder extends RecyclerView.ViewHolder {
            TextView promoTitle, promoDesc;
            Button delete;

            PromotionViewHolder(View itemView) {
                super(itemView);
                promoTitle = itemView.findViewById(R.id.promoTitle);
                promoDesc = itemView.findViewById(R.id.promoDesc);
                delete = itemView.findViewById(R.id.deletePromotion);
            }
        }
    }

}





