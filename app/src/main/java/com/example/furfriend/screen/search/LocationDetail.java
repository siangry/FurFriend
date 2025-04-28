package com.example.furfriend.screen.search;

import android.os.Bundle;
import android.text.SpannableString;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.furfriend.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.LocalTime;
import com.google.android.libraries.places.api.model.Period;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class LocationDetail extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView locationImage, openingIcon, expandIcon;
    private TextView tvName, tvAddress, tvPhone, tvCategory, tvRating, tvOpening;
    private MapView mapView;
    private GoogleMap map;
    private PlacesClient placesClient;
    private ImageButton backBtn;
    private boolean expanded = false;
    private List<String> weekdayTextList;
    private boolean isOpenNow = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_detail);

        backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(v -> finish());

        initViews(savedInstanceState);
        initPlacesClient();
        loadPlaceDetails();
    }

    private void initViews(Bundle savedInstanceState) {
        locationImage = findViewById(R.id.locationDetailImg);
        tvName = findViewById(R.id.locationDetailName);
        tvAddress = findViewById(R.id.locationDetailAddress);
        tvPhone = findViewById(R.id.locationDetailContact);
        tvCategory = findViewById(R.id.locationDetailCategory);
        tvOpening = findViewById(R.id.locationDetailOpening);
        openingIcon = findViewById(R.id.openingIcon);
        expandIcon = findViewById(R.id.expandIcon);
        tvRating = findViewById(R.id.locationDetailRating);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void initPlacesClient() {
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyD8auLWM0uCJkS9i01gPmunjA2NWzXUD2U");
        }
        placesClient = Places.createClient(this);
    }

    private void loadPlaceDetails() {
        String placeId = getIntent().getStringExtra("PLACE_ID");
        String category = getIntent().getStringExtra("CATEGORY");

        if (category != null) {
            if (category.equalsIgnoreCase("Veterinary Care")) {
                tvCategory.setText(getString(R.string.veterinaryCare));
            } else if (category.equalsIgnoreCase("Pet Store")) {
                tvCategory.setText(getString(R.string.petStore));
            } else if (category.equalsIgnoreCase("Pet Friendly Cafe")) {
                tvCategory.setText(getString(R.string.petfriendlyCafe));
            } else {
                tvCategory.setText(category); // fallback
            }
        } else {
            tvCategory.setText(getString(R.string.petRelated));
        }

        fetchPlaceDetails(placeId);
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.RATING,
                Place.Field.PHONE_NUMBER,
                Place.Field.WEBSITE_URI,
                Place.Field.PHOTO_METADATAS,
                Place.Field.OPENING_HOURS,
                Place.Field.TYPES
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            Place place = response.getPlace();
            updatePlaceUI(place);

            if (place.getLatLng() != null && map != null) {
                updateMap(place);
            }

        }).addOnFailureListener((exception) -> {
            Toast.makeText(this, "Error loading place details", Toast.LENGTH_SHORT).show();
            android.util.Log.e("PlacesAPI", "Place details error: " + exception.getMessage());
        });
    }

    private void updatePlaceUI(Place place) {
        tvName.setText(place.getName());
        tvAddress.setText(place.getAddress() != null ? place.getAddress() : getString(R.string.addressNotAvailable));
        tvPhone.setText(place.getPhoneNumber() != null ? place.getPhoneNumber() : getString(R.string.phoneNotAvailable));

        if (place.getRating() != null) {
            tvRating.setText(String.format("★ %.1f/5.0", place.getRating()));
        } else {
            tvRating.setText(getString(R.string.ratingNotAvailable));
        }

        if (place.getOpeningHours() != null && place.getOpeningHours().getWeekdayText() != null && !place.getOpeningHours().getWeekdayText().isEmpty()) {
            weekdayTextList = place.getOpeningHours().getWeekdayText();
            isOpenNow = checkIsOpenNow(place.getOpeningHours().getPeriods());

            tvOpening.setText(isOpenNow ? getString(R.string.openNow) : getString(R.string.closedNow));
            expandIcon.setImageResource(R.drawable.ic_expand);
            openingIcon.setImageResource(R.drawable.ic_paw_orange);

            tvOpening.setOnClickListener(v -> toggleOpeningHours());
            expandIcon.setOnClickListener(v -> toggleOpeningHours());

            tvOpening.setTextColor(ContextCompat.getColor(this, isOpenNow ? R.color.dark_green : R.color.red));
        } else {
            weekdayTextList = null;
            tvOpening.setText(getString(R.string.hoursNotAvailable));
            tvOpening.setTextColor(ContextCompat.getColor(this, R.color.grey));
        }

        if (place.getPhotoMetadatas() != null && !place.getPhotoMetadatas().isEmpty()) {
            loadPlacePhoto(place.getPhotoMetadatas().get(0));
        } else {
            locationImage.setImageResource(R.drawable.ic_logo);
        }
    }

    private boolean checkIsOpenNow(List<Period> periods) {
        if (periods == null || periods.isEmpty()) return false;

        Calendar now = Calendar.getInstance();
        int today = now.get(Calendar.DAY_OF_WEEK) - 1;

        for (Period period : periods) {
            if (period.getOpen() != null && period.getClose() != null
                    && period.getOpen().getDay().ordinal() == today) {

                LocalTime openTime = period.getOpen().getTime();
                LocalTime closeTime = period.getClose().getTime();

                Calendar openCal = (Calendar) now.clone();
                openCal.set(Calendar.HOUR_OF_DAY, openTime.getHours());
                openCal.set(Calendar.MINUTE, openTime.getMinutes());

                Calendar closeCal = (Calendar) now.clone();
                closeCal.set(Calendar.HOUR_OF_DAY, closeTime.getHours());
                closeCal.set(Calendar.MINUTE, closeTime.getMinutes());

                if (closeCal.before(openCal)) {
                    closeCal.add(Calendar.DAY_OF_YEAR, 1);
                }

                if (now.after(openCal) && now.before(closeCal)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleOpeningHours() {
        if (weekdayTextList == null) return;

        if (!expanded) {
            String status = isOpenNow ? getString(R.string.openNow) : getString(R.string.closedNow);
            StringBuilder timeslotBuilder = new StringBuilder();
            for (String day : weekdayTextList) {
                timeslotBuilder.append(day).append("\n");
            }

            String fullText = status + "\n" + timeslotBuilder.toString().trim();

            SpannableString spannable = new SpannableString(fullText);

            // Set status (first line) to green/red
            int statusColor = ContextCompat.getColor(this, isOpenNow ? R.color.dark_green : R.color.red);
            spannable.setSpan(new android.text.style.ForegroundColorSpan(statusColor), 0, status.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set the rest (timeslots) to black
            spannable.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, android.R.color.black)),
                    status.length() + 1, fullText.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvOpening.setText(spannable);

            expandIcon.setImageResource(R.drawable.ic_fold);
            expanded = true;

        } else {
            tvOpening.setText(isOpenNow ? "Open Now" : "Closed Now");
            tvOpening.setTextColor(ContextCompat.getColor(this, isOpenNow ? R.color.dark_green : R.color.red));
            expandIcon.setImageResource(R.drawable.ic_expand);
            expanded = false;
        }
    }

    private void loadPlacePhoto(PhotoMetadata photoMetadata) {
        FetchPhotoRequest photoRequest = FetchPhotoRequest.builder(photoMetadata)
                .setMaxWidth(1000)
                .setMaxHeight(500)
                .build();

        placesClient.fetchPhoto(photoRequest).addOnSuccessListener(fetchPhotoResponse -> {
            locationImage.setImageBitmap(fetchPhotoResponse.getBitmap());
        }).addOnFailureListener(exception -> {
            locationImage.setImageResource(R.drawable.ic_logo);
        });
    }

    private void updateMap(Place place) {
        LatLng location = place.getLatLng();
        map.clear();
        map.addMarker(new MarkerOptions().position(location).title(place.getName()));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
