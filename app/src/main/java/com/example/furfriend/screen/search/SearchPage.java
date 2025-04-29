package com.example.furfriend.screen.search;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.furfriend.R;
import com.example.furfriend.screen.search.LocationDetail;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchPage extends Fragment {

    private LinearLayout suggestionContainer;
    private EditText searchBar;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat = 0.0, userLon = 0.0;
    private List<Map<String, String>> allPlaces = new ArrayList<>();
    private Button vetButton, shopButton, cafeButton;
    private String selectedCategory = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.screen_search_page, container, false);

        searchBar = view.findViewById(R.id.searchBar);
        suggestionContainer = view.findViewById(R.id.suggestionContainer);
        vetButton = view.findViewById(R.id.vetButton);
        shopButton = view.findViewById(R.id.shopButton);
        cafeButton = view.findViewById(R.id.cafeButton);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPlaces(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        vetButton.setOnClickListener(v -> toggleCategory("Veterinary Care", vetButton));
        shopButton.setOnClickListener(v -> toggleCategory("Pet Store", shopButton));
        cafeButton.setOnClickListener(v -> toggleCategory("Pet Friendly Cafe", cafeButton));

        checkLocationPermission();
        return view;
    }

    private void filterByCategory(String categoryName) {
        List<Map<String, String>> filtered = new ArrayList<>();
        for (Map<String, String> place : allPlaces) {
            if (place.get("category").equalsIgnoreCase(categoryName)) {
                filtered.add(place);
            }
        }
        showFilteredPlaces(filtered);
    }
    private void toggleCategory(String category, Button button) {
        if (selectedCategory.equals(category)) {
            // Already selected -> Unselect
            selectedCategory = "";
            resetButtonStyles();
            showPlaces(allPlaces);
        } else {
            selectedCategory = category;
            resetButtonStyles();
            button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.orange)); // your default button color
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
            filterByCategory(category);
        }
    }

    private void resetButtonStyles() {
        // Set all buttons back to white background, black text
        vetButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.white));
        vetButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));

        shopButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.white));
        shopButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));

        cafeButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.white));
        cafeButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
    }


    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getNearbyPlaces();
        }
    }

    private void getNearbyPlaces() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLon = location.getLongitude();
                searchNearbyPetPlaces();
            } else {
                Toast.makeText(getContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchNearbyPetPlaces() {
        OkHttpClient client = new OkHttpClient();

        String[] urls = {
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                        + "?location=" + userLat + "," + userLon
                        + "&radius=7000"
                        + "&type=veterinary_care"
                        + "&key=AIzaSyD8auLWM0uCJkS9i01gPmunjA2NWzXUD2U",
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                        + "?location=" + userLat + "," + userLon
                        + "&radius=7000"
                        + "&type=pet_store"
                        + "&key=AIzaSyD8auLWM0uCJkS9i01gPmunjA2NWzXUD2U",
                "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                        + "?location=" + userLat + "," + userLon
                        + "&radius=7000"
                        + "&type=cafe"
                        + "&keyword=pet-friendly"
                        + "&key=AIzaSyD8auLWM0uCJkS9i01gPmunjA2NWzXUD2U",
        };

        String[] categories = {"Veterinary Care", "Pet Store", "Pet Friendly Cafe"};

        new Thread(() -> {
            try {
                allPlaces.clear();

                for (int j = 0; j < urls.length; j++) {
                    String url = urls[j];
                    String currentCategory = categories[j];

                    Request request = new Request.Builder().url(url).build();
                    Response response = client.newCall(request).execute();
                    String jsonData = response.body().string();

                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray results = jsonObject.getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject place = results.getJSONObject(i);
                        String name = place.optString("name", "No Name").toLowerCase();
                        String address = place.optString("vicinity", "No Address");
                        double lat = place.getJSONObject("geometry").getJSONObject("location").getDouble("lat");
                        double lng = place.getJSONObject("geometry").getJSONObject("location").getDouble("lng");
                        String placeId = place.optString("place_id", "");
                        String rating = place.has("rating") ? String.valueOf(place.getDouble("rating")) : "--";

                        // Calculate the distance here and save it with the place data
                        double dist = calculateDistance(userLat, userLon, lat, lng);

                        String photoRef = null;
                        if (place.has("photos")) {
                            JSONArray photos = place.getJSONArray("photos");
                            if (photos.length() > 0) {
                                photoRef = photos.getJSONObject(0).optString("photo_reference", null);
                            }
                        }

                        Map<String, String> item = new HashMap<>();
                        item.put("name", name);
                        item.put("address", address);
                        item.put("lat", String.valueOf(lat));
                        item.put("lng", String.valueOf(lng));
                        item.put("rating", rating);
                        item.put("photo", photoRef);
                        item.put("placeId", placeId);
                        item.put("category", currentCategory);
                        item.put("distance", String.valueOf(dist)); // Store the calculated distance

                        allPlaces.add(item);
                    }
                }

                // Sort the places by distance before displaying
                sortPlacesByDistance(allPlaces);

                requireActivity().runOnUiThread(() -> showPlaces(allPlaces));

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error loading places", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // New method to sort places by distance
    private void sortPlacesByDistance(List<Map<String, String>> places) {
        places.sort((place1, place2) -> {
            double dist1 = Double.parseDouble(place1.get("distance"));
            double dist2 = Double.parseDouble(place2.get("distance"));
            return Double.compare(dist1, dist2);
        });
    }

    private void showPlaces(List<Map<String, String>> placesToShow) {
        suggestionContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map<String, String> place : placesToShow) {
            View card = inflater.inflate(R.layout.item_suggestion, suggestionContainer, false);

            TextView name = card.findViewById(R.id.locationName);
            TextView category = card.findViewById(R.id.locationCategory);
            TextView rating = card.findViewById(R.id.locationRating);
            TextView distance = card.findViewById(R.id.locationDistance);
            ImageView img = card.findViewById(R.id.locationImg);

            name.setText(place.get("name"));
            String categoryKey = place.get("category");
            if (categoryKey.equalsIgnoreCase("Veterinary Care")) {
                category.setText(getString(R.string.veterinaryCare));
            } else if (categoryKey.equalsIgnoreCase("Pet Store")) {
                category.setText(getString(R.string.petStore));
            } else if (categoryKey.equalsIgnoreCase("Pet Friendly Cafe")) {
                category.setText(getString(R.string.petfriendlyCafe));
            }
            rating.setText("★ " + place.get("rating"));

            double lat = Double.parseDouble(place.get("lat"));
            double lng = Double.parseDouble(place.get("lng"));
            double dist = calculateDistance(userLat, userLon, lat, lng);
            distance.setText(String.format("%.2f km", dist));

            String photoRef = place.get("photo");
            if (photoRef != null && !photoRef.isEmpty()) {
                String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference=" + photoRef + "&key=AIzaSyD8auLWM0uCJkS9i01gPmunjA2NWzXUD2U";
                Picasso.get().load(photoUrl).into(img);
            } else {
                img.setImageResource(R.drawable.ic_logo);
            }


            card.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LocationDetail.class);
                intent.putExtra("PLACE_ID", place.get("placeId"));
                intent.putExtra("CATEGORY", place.get("category"));
                startActivity(intent);

            });

            suggestionContainer.addView(card);
        }
    }


    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] result = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, result);
        return result[0] / 1000.0;
    }

    private void filterPlaces(String query) {
        List<Map<String, String>> filtered = new ArrayList<>();
        for (Map<String, String> place : allPlaces) {
            // Check if query matches either name or category (case insensitive)
            if (place.get("name").toLowerCase().contains(query.toLowerCase()) ||
                    place.get("category").toLowerCase().contains(query.toLowerCase())) {
                filtered.add(place);
            }
        }
        showFilteredPlaces(filtered);
    }

    private void showFilteredPlaces(List<Map<String, String>> placesToShow) {
        suggestionContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (Map<String, String> place : placesToShow) {
            View card = inflater.inflate(R.layout.item_suggestion, suggestionContainer, false);

            TextView name = card.findViewById(R.id.locationName);
            TextView category = card.findViewById(R.id.locationCategory);
            TextView rating = card.findViewById(R.id.locationRating);
            TextView distance = card.findViewById(R.id.locationDistance);
            ImageView img = card.findViewById(R.id.locationImg);

            name.setText(place.get("name"));
            String categoryKey = place.get("category");
            if (categoryKey.equalsIgnoreCase("Veterinary Care")) {
                category.setText(getString(R.string.veterinaryCare));
            } else if (categoryKey.equalsIgnoreCase("Pet Store")) {
                category.setText(getString(R.string.petStore));
            } else if (categoryKey.equalsIgnoreCase("Pet Friendly Cafe")) {
                category.setText(getString(R.string.petfriendlyCafe));
            }
            rating.setText("★ " + place.get("rating"));

            double lat = Double.parseDouble(place.get("lat"));
            double lng = Double.parseDouble(place.get("lng"));
            double dist = calculateDistance(userLat, userLon, lat, lng);
            distance.setText(String.format("%.2f km", dist));

            String photoRef = place.get("photo");
            if (photoRef != null) {
                String photoUrl = "https://maps.googleapis.com/maps/api/place/photo?maxwidth=400&photo_reference="
                        + photoRef + "&key=AIzaSyD8auLWM0uCJkS9i01gPmunjA2NWzXUD2U";
                Picasso.get().load(photoUrl).into(img);
            } else {
                img.setImageResource(R.drawable.ic_logo);
            }

            card.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LocationDetail.class);
                intent.putExtra("PLACE_ID", place.get("placeId"));
                intent.putExtra("CATEGORY", place.get("category"));
                startActivity(intent);
            });
            suggestionContainer.addView(card);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getNearbyPlaces();
        } else {
            Toast.makeText(getContext(), "Permission required to fetch places", Toast.LENGTH_SHORT).show();
        }
    }
}