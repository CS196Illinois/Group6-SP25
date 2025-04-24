package com.example.shelfaware;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
// import com.example.shelfaware.Cs124hproject;
import com.example.shelfaware.CS124H;

// could add a progress bar for when api call is being made
/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RecipesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecipesFragment extends Fragment {

    Button inputIngredients;
    EditText ingredientsBox;
    TextView displayText; // may not be needed later
    LinearLayout inputIngredientsContainer;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public RecipesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RecipesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RecipesFragment newInstance(String param1, String param2) {
        RecipesFragment fragment = new RecipesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);
        inputIngredients = view.findViewById(R.id.inputIngredients);
        ingredientsBox = view.findViewById(R.id.ingredientsBox);
        displayText = view.findViewById(R.id.displayText);
        inputIngredientsContainer = view.findViewById(R.id.inputIngredientsContainer);

        // show keyboard when clicking on ingredientsBox
        ingredientsBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ingredientsBox.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(ingredientsBox, InputMethodManager.SHOW_FORCED);
            }
        });


        /*
        inputIngredients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ingredientsBox.setVisibility(View.VISIBLE);
                ingredientsBox.requestFocus();
                // show keyboard
                InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(ingredientsBox, InputMethodManager.SHOW_FORCED);
                //imm.showSoftInput(ingredientsBox, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        */


        // retrieve text
        /*
        inputIngredients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredText = "temp";
                displayText.setText(enteredText);
                Cs124hproject.getRecipe(new MyCallback() {
                                            @Override
                                            public void onResult(String result) {
                                                displayText.setText(result);
                                            }
                                        });
                displayText.setText("ran");
            }
        });
        */



        inputIngredients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredText = ingredientsBox.getText().toString();
                new Thread(() -> {
                    try {
                        String recipe = CS124H.getRecipes(enteredText);
                        requireActivity().runOnUiThread(() -> displayText.setText(recipe));
                    } catch (Exception e) {
                        e.printStackTrace();
                        requireActivity().runOnUiThread(() -> displayText.setText("Error: " + e.getMessage()));
                    }
                }).start();
            }
        });



        /*
        ingredientsBox.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // get entered text
                    String enteredText = ingredientsBox.getText().toString();
                    // Toast.makeText(getActivity(), enteredText, Toast.LENGTH_SHORT).show();
                    displayText.setText(enteredText);
                }
            }
        });
        */
        /* the code below was original for enter ingredients button
        inputIngredients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String enteredText = ingredientsBox.getText().toString();
                displayText.setText(enteredText);
            }
        });

        */

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
}