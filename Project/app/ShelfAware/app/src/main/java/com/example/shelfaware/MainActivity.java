package com.example.shelfaware;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
                        Log.d("MainActivity", "Switching to HomeFragment with data: " + classification + ", Expiration: " + expirationDate);
                        switchToHomeFragment(imageBytes, classification, expirationDate);

                    }


                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference itemsRef = database.getReference("items");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);

        bottomNavigationView.setBackground(null);

        replaceFragment(new HomeFragment(), "HomeFragment");
        homeFragment = (HomeFragment) getSupportFragmentManager().findFragmentById(R.id.frame_layout);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.home) {
                replaceFragment(new HomeFragment(), "HomeFragment");  // Correct tag usage
            } else if (itemId == R.id.recipes) {
                replaceFragment(new RecipesFragment(), "RecipesFragment");  // Assign correct tag
            } else if (itemId == R.id.profile) {
                replaceFragment(new ProfileFragment(), "ProfileFragment");  // Assign correct tag
            } else if (itemId == R.id.settings) {
                replaceFragment(new SettingsFragment(), "SettingsFragment");  // Assign correct tag
            }
            return true;
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PictureActivity.class);
                pictureActivityLauncher.launch(intent);
            }
        });
    }

    private void replaceFragment(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment, tag);
        fragmentTransaction.commit();
    }
    private void switchToHomeFragment(byte[] imageBytes, String classification, String expirationDate) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        HomeFragment homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("HomeFragment");

        if (homeFragment == null || !homeFragment.isAdded()) {
            homeFragment = new HomeFragment();

            fragmentManager.beginTransaction()
                    .add(R.id.frame_layout, homeFragment, "HomeFragment")
                    .commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }

        // Wait for HomeFragment to be fully attached before adding an item
        fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentResumed(FragmentManager fm, Fragment f) {
                if (f instanceof HomeFragment) {
                    ((HomeFragment) f).addNewItem(imageBytes, classification, expirationDate);
                    fragmentManager.unregisterFragmentLifecycleCallbacks(this); // Clean up callback
                }
            }
        }, false);

        bottomNavigationView.setSelectedItemId(R.id.home);
    }




}