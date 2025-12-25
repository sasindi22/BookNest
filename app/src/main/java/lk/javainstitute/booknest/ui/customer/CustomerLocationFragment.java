package lk.javainstitute.booknest.ui.customer;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import lk.javainstitute.booknest.R;
import lk.javainstitute.booknest.databinding.FragmentCustomerLocationBinding;

public class CustomerLocationFragment extends Fragment {

    private FragmentCustomerLocationBinding binding;
    private GoogleMap googleMap;

    private final LatLng colombo = new LatLng(6.9173690368368845, 79.85479122143389);
    private final LatLng kandy = new LatLng(7.2906, 80.6337);
    private final LatLng gampaha = new LatLng(7.0912, 79.9948);
    private final LatLng galle = new LatLng(6.0535, 80.2210);
    private final LatLng kalutara = new LatLng(6.5833, 79.9603);

    private final String[] locations = {"Colombo", "Kandy", "Gampaha", "Galle", "Kalutara"};
    private final LatLng[] latLngs = {colombo, kandy, gampaha, galle, kalutara};

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentCustomerLocationBinding.inflate(inflater, container, false);

        Spinner locationSpinner = binding.spinnerLocation;
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.user_type_spinner_item,
                R.id.userEmailNavC,
                locations
        );
        locationSpinner.setAdapter(arrayAdapter);

        SupportMapFragment supportMapFragment = new SupportMapFragment();
        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.frameLayoutMap, supportMapFragment);
        fragmentTransaction.commit();

        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap map) {
                googleMap = map;
                Log.i("booknestLogMap", "Map is Ready!");

                updateMapLocation(colombo, "Colombo - BookNest Branch");

                locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                        updateMapLocation(latLngs[position], locations[position] + " - BookNest Branch");
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                        Toast.makeText(getContext(), "Error: Invalid Action!", Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });

        return binding.getRoot();
    }

    private void updateMapLocation(LatLng location, String title){
        if (googleMap != null){
            googleMap.clear();
            googleMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                            new CameraPosition.Builder()
                                    .target(location)
                                    .zoom(16)
                                    .build()
                    )
            );
            googleMap.addMarker(
                    new MarkerOptions()
                            .position(location)
                            .title(title)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}