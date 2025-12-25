package lk.javainstitute.booknest.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentAdminManageUsersBinding;

public class AdminManageUsersFragment extends Fragment {

    private FragmentAdminManageUsersBinding binding;
    private ArrayList<User> userList = new ArrayList<>();
    private UserAdapter adapter;
    private FirebaseFirestore firestore;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAdminManageUsersBinding.inflate(inflater, container, false);
        firestore = FirebaseFirestore.getInstance();

        setupSpinner();
        setupRecyclerView();
        fetchUsers("", "");
        setupSearchButton();

        return binding.getRoot();
    }

    private void setupSpinner() {
        Spinner usersSpinner = binding.spinnerUsersAdmin;
        String[] roles = new String[]{"All", "Admin", "Seller", "Customer"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.user_type_spinner_item,
                R.id.userEmailNavC,
                roles
        );
        usersSpinner.setAdapter(arrayAdapter);

        usersSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String role = (String) parentView.getItemAtPosition(position);
                fetchUsers("", role.equals("All") ? "" : role);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                fetchUsers("", "");
            }
        });
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewUsersAdmin;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserAdapter(userList);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchButton() {
        Button searchButton = binding.searchAdminBtnUsers;
        searchButton.setOnClickListener(view -> {
            EditText searchField = binding.searchAdminUsers;
            String searchText = searchField.getText().toString().toLowerCase();
            fetchUsers(searchText, "");
        });
    }

    private void fetchUsers(String query, String role) {
        CollectionReference usersRef = firestore.collection("user");
        userList.clear();

        usersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ArrayList<User> tempUserList = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String fname = document.getString("fname");
                    String lname = document.getString("lname");
                    String userRole = document.getString("role");

                    boolean matchesQuery = query.isEmpty() || fname.toLowerCase().contains(query) || lname.toLowerCase().contains(query);
                    boolean matchesRole = role.isEmpty() || userRole.equals(role);

                    if (matchesQuery && matchesRole) {
                        addUserToList(tempUserList, document);
                    }
                }

                userList.clear();
                userList.addAll(tempUserList);
                adapter.notifyDataSetChanged();

            } else {
                Log.e("FirestoreError", "Failed to fetch users", task.getException());
                Toast.makeText(getContext(), "Failed to fetch users: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addUserToList(ArrayList<User> tempUserList, QueryDocumentSnapshot document) {
        String id = document.getId();
        String fname = document.getString("fname");
        String lname = document.getString("lname");
        String role = document.getString("role");
        String mobile = document.getString("mobile");
        String name = fname + " " + lname;
        String userField = name + " (" + role + ") ";

        User user = new User(userField, id, mobile);
        if (!tempUserList.contains(user)) {
            tempUserList.add(user);
        }
    }

    private void removeUser(String userId) {
        firestore.collection("user").document(userId)
                .delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "User removed successfully", Toast.LENGTH_SHORT).show();
                        fetchUsers("", "");
                    } else {
                        Toast.makeText(getContext(), "Failed to remove user", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static class User {
        String name;
        String id;
        String mobile;

        User(String name, String id, String mobile) {
            this.name = name;
            this.id = id;
            this.mobile = mobile;
        }
    }

    class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private ArrayList<User> userList;

        UserAdapter(ArrayList<User> userList) {
            this.userList = userList;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.admin_user_card_item, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = userList.get(position);
            holder.name.setText(user.name);
            holder.id.setText(user.id);
            holder.mobile.setText(user.mobile);

            holder.remove.setOnClickListener(view -> removeUser(user.id));
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView name, id, mobile;
            Button remove;

            UserViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.userNameAdmin);
                id = itemView.findViewById(R.id.idUserAdmin);
                mobile = itemView.findViewById(R.id.mobileUserAdmin);
                remove = itemView.findViewById(R.id.deleteUserAdminBtn);
            }
        }
    }
}