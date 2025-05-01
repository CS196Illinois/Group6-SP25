package com.example.shelfaware;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        if (imageUri != null) {
            ImageItem newItem = new ImageItem(imageUri.toString(), classification, expirationDate, false);
            imageItemList.add(newItem);

            Collections.sort(imageItemList, (item1, item2) -> {
                try {
                    SimpleDateFormat inputFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

                    // Convert expiration date format
                    Date date1 = inputFormat.parse(item1.getExpirationDate());
                    Date date2 = inputFormat.parse(item2.getExpirationDate());

                    if (date1 == null || date2 == null) return 0; // Prevents sorting errors

                    return date1.compareTo(date2); // Oldest date first
                } catch (ParseException e) {
                    e.printStackTrace();
                    return 0;
                }
            });

            adapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(imageItemList.size() - 1); // Scroll to new item
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
            return androidx.core.content.FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".provider", file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}


