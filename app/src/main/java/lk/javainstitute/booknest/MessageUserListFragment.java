package lk.javainstitute.booknest;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import lk.javainstitute.booknest.databinding.FragmentMessageUserListBinding;

public class MessageUserListFragment extends Fragment {

    private FragmentMessageUserListBinding binding;
    private ArrayList<User> userList = new ArrayList<>();
    private Adapter1 adapter;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessageUserListBinding.inflate(inflater, container, false);

        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUserId = sharedPreferences.getString("userId", null);

        RecyclerView recyclerView = binding.userListMsg;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new Adapter1(userList, currentUserId);
        recyclerView.setAdapter(adapter);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("user")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                String userId = document.getId();
                                if (!userId.equals(currentUserId)) {
                                    String firstName = document.getString("fname");
                                    String lastName = document.getString("lname");
                                    String role = document.getString("role");
                                    String mobile = document.getString("mobile");
                                    String imageUrl = document.getString("profileImageUrl");
                                    String userData = firstName + " " + lastName + " (" + role + ")";
                                    userList.add(new User(userData, mobile, imageUrl, userId));
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to load users", Toast.LENGTH_SHORT).show();
                    }
                });

        return binding.getRoot();
    }

    public class User {
        String userData;
        String mobile;
        String profileImageUrl;
        String userId;

        public User(String userData, String mobile, String profileImageUrl, String userId) {
            this.userData = userData;
            this.mobile = mobile;
            this.profileImageUrl = profileImageUrl;
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }
    }

    class Adapter1 extends RecyclerView.Adapter<Adapter1.UserViewHolder> {

        private ArrayList<User> userList;
        private String currentUserId;

        Adapter1( ArrayList<User> users, String currentUserId) {
            this.userList = users;
            this.currentUserId = currentUserId;
        }

        @Override
        public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.user_msg_item, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            User user = userList.get(position);
            holder.userNameData.setText(user.userData);
            holder.userMobileData.setText(user.mobile);

            holder.viewMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String phoneNumber = user.mobile;
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phoneNumber));
                    startActivity(intent);
                }
            });

            Glide.with(holder.itemView.getContext())
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.profile_user)
                    .error(R.drawable.profile_user)
                    .into(holder.userPfp);
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView userMobileData, userNameData;
            ImageView userPfp, viewMsg;

            UserViewHolder(View itemView) {
                super(itemView);
                userMobileData = itemView.findViewById(R.id.userMobileData);
                userNameData = itemView.findViewById(R.id.userNameData);
                userPfp = itemView.findViewById(R.id.userPfp);
                viewMsg = itemView.findViewById(R.id.expandChatBtn);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}