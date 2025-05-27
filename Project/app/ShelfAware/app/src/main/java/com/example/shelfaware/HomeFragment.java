package com.example.shelfaware;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private List<ImageItem> imageItemList;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        imageItemList = new ArrayList<>();

        // Setup Adapter
        adapter = new ImageAdapter(imageItemList);
        recyclerView.setAdapter(adapter);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences("local_data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
        }

        try {
            loadItemsFromFirebase(); // call to retrieve data when the fragment is created
        } catch (Exception e) {
            Log.e("Firebase", "Firebase is not set up: " + e.getMessage());
            Toast.makeText(getContext(), "Firebase setup error. Some features may not work.", Toast.LENGTH_SHORT).show();
        }


        // Setup Generate Recipe Button
        Button generateRecipeButton = view.findViewById(R.id.generateRecipeButton);
        generateRecipeButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Generating recipe . . .", Toast.LENGTH_SHORT).show(); // DEBUG TOAST

            List<String> selectedIngredients = new ArrayList<>();
            for (ImageItem item : imageItemList) {
                if (item.isChecked()) {
                    selectedIngredients.add(item.getTitle());
                }
            }

            if (!selectedIngredients.isEmpty()) {
                StringBuilder enteredIngredients = new StringBuilder();
                for (int i = 0; i < selectedIngredients.size(); i++) {
                    enteredIngredients.append(selectedIngredients.get(i));
                    if (i != selectedIngredients.size() - 1) {
                        enteredIngredients.append(", ");
                    }
                }

                new Thread(() -> {
                    String recipe = CS124H.getRecipes(enteredIngredients.toString());
                    requireActivity().runOnUiThread(() -> {
                        showRecipeDialog(recipe);
                    });
                }).start();

            } else {
                Toast.makeText(getContext(), "Please select at least one ingredient!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showRecipeDialog(String recipe) {
        if (recipe == null || recipe.isEmpty()) {
            recipe = "Error: No recipe found. Please check your internet connection or try again.";
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Generated Recipe")
                .setMessage(recipe)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Add New Item (from PictureActivity result)
    public void addNewItem(byte[] imageBytes, String classification, String expirationDate) {
        Log.d("HomeFragment", "Adding item: " + classification + ", Expiration: " + expirationDate);
        Uri imageUri = saveImageBytesToCache(imageBytes);

        Log.d("HomeFragment", "Generated Image URI: " + imageUri);
        if (imageBytes == null || imageBytes.length == 0) {
            Log.e("HomeFragment", "Camera Image Bytes are NULL or EMPTY!");
            Toast.makeText(getContext(), "Image data is missing!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri != null) {
            ImageItem newItem = new ImageItem(imageUri.toString(), classification, expirationDate, false);
            imageItemList.add(newItem);

            Collections.sort(imageItemList, (item1, item2) -> {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                    Date date1 = inputFormat.parse(item1.getExpirationDate());
                    Date date2 = inputFormat.parse(item2.getExpirationDate());

                    if (date1 == null || date2 == null) return 0;

                    // First, compare expiration dates
                    int dateComparison = date1.compareTo(date2);
                    if (dateComparison != 0) return dateComparison; // If dates are different, sort by date

                    // If expiration dates are the same, sort alphabetically by title
                    return item1.getTitle().compareToIgnoreCase(item2.getTitle());
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            });

            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(imageItemList.size() - 1); // Scroll to new item

            saveItem(imageUri.toString(), classification, expirationDate, newItem.isExpiringSoon());
        } else {
            Toast.makeText(getContext(), "Failed to save image.", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri saveImageBytesToCache(byte[] imageBytes) {
        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "image_" + System.currentTimeMillis() + ".png");

            FileOutputStream stream = new FileOutputStream(file);
            stream.write(imageBytes);
            stream.close();

            if (!file.exists()) {
                Log.e("SaveImage", "File doesn't exist after creation!");
                return null;
            }

            return FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".provider", file);
        } catch (IOException e) {
            Log.e("SaveImage", "Error saving image: " + e.getMessage());
            return null;
        }
    }
    public void saveItem(String imageUri, String title, String expirationDate, boolean expiringSoon) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            saveToLocalStorage(new ImageItem(null, imageUri, title, expirationDate, expiringSoon));
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userItemsRef = FirebaseDatabase.getInstance().getReference("items").child(userId);

        String itemId = userItemsRef.push().getKey(); // Generate unique ID
        ImageItem newItem = new ImageItem(itemId, imageUri, title, expirationDate, expiringSoon);

        userItemsRef.child(itemId).setValue(newItem); // Stores item under the user's UID in Firebase
    }

    private void loadItemsFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            loadFromLocalStorage();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userItemsRef = FirebaseDatabase.getInstance().getReference("items").child(userId);

        userItemsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!isAdded()) return;
                imageItemList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ImageItem item = snapshot.getValue(ImageItem.class);
                    imageItemList.add(item);
                }

                Collections.sort(imageItemList, (item1, item2) -> {
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                        Date date1 = inputFormat.parse(item1.getExpirationDate());
                        Date date2 = inputFormat.parse(item2.getExpirationDate());

                        if (date1 == null || date2 == null) return 0;

                        int dateComparison = date1.compareTo(date2);
                        if (dateComparison != 0) return dateComparison;

                        return item1.getTitle().compareToIgnoreCase(item2.getTitle());
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0;
                    }
                });
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "Error retrieving data: " + error.getMessage());
                if (isAdded()) {
                    loadFromLocalStorage();
                }
            }
        });
    }
    private void loadFromLocalStorage() {
        SharedPreferences prefs = requireContext().getSharedPreferences("local_data", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("items", "[]"); // Retrieve stored data
        Type type = new TypeToken<List<ImageItem>>() {}.getType();
        List<ImageItem> localItems = gson.fromJson(json, type);

        imageItemList.clear();
        imageItemList.addAll(localItems);
        adapter.notifyDataSetChanged(); // Refresh RecyclerView
        Toast.makeText(getContext(), "Showing offline data", Toast.LENGTH_SHORT).show();
    }
    private void saveToLocalStorage(ImageItem newItem) {
        SharedPreferences prefs = requireContext().getSharedPreferences("local_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();

        // Retrieve existing local items
        String json = prefs.getString("items", "[]");
        Type type = new TypeToken<List<ImageItem>>() {}.getType();
        List<ImageItem> localItems = gson.fromJson(json, type);

        // Add new item and save
        localItems.add(newItem);
        editor.putString("items", gson.toJson(localItems));
        editor.apply();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (imageItemList.isEmpty()) {
            loadItemsFromFirebase();
        }
    }
}


