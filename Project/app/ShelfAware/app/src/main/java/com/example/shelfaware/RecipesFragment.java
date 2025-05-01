package com.example.shelfaware;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import android.app.AlertDialog;

import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
// import com.example.shelfaware.Cs124hproject;
import com.example.shelfaware.CS124H;

public class RecipesFragment extends Fragment {

    Button inputIngredients;
    EditText ingredientsBox;
    LinearLayout inputIngredientsContainer;

    public RecipesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);
        inputIngredients = view.findViewById(R.id.inputIngredients);
        ingredientsBox = view.findViewById(R.id.ingredientsBox);
        inputIngredientsContainer = view.findViewById(R.id.inputIngredientsContainer);

        TextView loadingText = view.findViewById(R.id.loadingText);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);


        ingredientsBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(ingredientsBox.getWindowToken(), 0);
                ingredientsBox.clearFocus();
                return true;
            }
            return false;
        });


        inputIngredients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredText = ingredientsBox.getText().toString().trim();
                if (enteredText.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter ingredients!", Toast.LENGTH_SHORT).show();
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    loadingText.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                });
                new Thread(() -> {
                    try {
                        String recipe = CS124H.getRecipes(enteredText);
                        requireActivity().runOnUiThread(() -> {
                            loadingText.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            showRecipeDialog(recipe);

                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        requireActivity().runOnUiThread(() -> {
                            loadingText.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "API Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            showRecipeDialog("Something went wrong. Please try again.");
                        });

                    }
                }).start();
            }
        });


        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (ingredientsBox != null && ingredientsBox.hasFocus()) {
                    InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(ingredientsBox.getWindowToken(), 0);
                    ingredientsBox.clearFocus();
                }
                return false;
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
}