package com.example.shelfaware;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton fab;
    BottomNavigationView bottomNavigationView;
    private HomeFragment homeFragment;

    private final ActivityResultLauncher<Intent> pictureActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    String classification = data.getStringExtra("classification");
                    String expirationDate = data.getStringExtra("expirationDate");
                    byte[] imageBytes = data.getByteArrayExtra("image");

                    Fragment homeFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
                    if (homeFragment instanceof HomeFragment) {
                        ((HomeFragment) homeFragment).addNewItem(imageBytes, classification, expirationDate);
                    } else {
                        Log.e("MainActivity", "HomeFragment not found or not attached!");
                    }

                    /*
                    Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    // Pass data to HomeFragment

                    HomeFragment homeFragment = new HomeFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("image", imageBitmap);
                    bundle.putString("classification", classification);
                    bundle.putString("expirationDate", expirationDate);
                    homeFragment.setArguments(bundle);

                    // Replace current fragment with HomeFragment
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_layout, homeFragment)
                            .commit();

                     */


                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);

        bottomNavigationView.setBackground(null);

        replaceFragment(new HomeFragment());
        homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.frame_layout);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.recipes) {
                replaceFragment(new RecipesFragment());
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment());
            } else if (itemId == R.id.settings) {
                replaceFragment(new SettingsFragment());
            }
            return true;
        });


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PictureActivity.class);
                //startActivity(intent);
                pictureActivityLauncher.launch(intent);
            }
        });



    }
    /*
    private final ActivityResultLauncher<Intent> pictureActivityLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    String classification = data.getStringExtra("classification");
                    String expirationDate = data.getStringExtra("expirationDate");
                    byte[] imageBytes = data.getByteArrayExtra("image");

                    Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                    // Pass data to HomeFragment
                    HomeFragment homeFragment = new HomeFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("image", imageBitmap);
                    bundle.putString("classification", classification);
                    bundle.putString("expirationDate", expirationDate);
                    homeFragment.setArguments(bundle);

                    // Replace current fragment with HomeFragment
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.frame_layout, homeFragment)
                            .commit();
                }
            });

     */

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }




}