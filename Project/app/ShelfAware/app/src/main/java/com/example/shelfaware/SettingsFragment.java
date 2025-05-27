package com.example.shelfaware;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import androidx.activity.result.contract.ActivityResultContracts;


public class SettingsFragment extends Fragment {

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleSignInResult(task);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize Firebase authentication
        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("554353278479-2nqs9cn597lh023s4qqfn59hht652e6i.apps.googleusercontent.com")
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        Button googleSignInButton = view.findViewById(R.id.googleSignInButton);
        Button logoutButton = view.findViewById(R.id.logoutButton);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            logoutButton.setVisibility(View.VISIBLE);
            googleSignInButton.setVisibility(View.GONE);
        } else {
            logoutButton.setVisibility(View.GONE);
            googleSignInButton.setVisibility(View.VISIBLE);
            googleSignInButton.setOnClickListener(v -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                signInLauncher.launch(signInIntent);
            });
        }

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Toast.makeText(requireContext(), "Logged out successfully!", Toast.LENGTH_SHORT).show();
                updateAuthUI();
            });
        });

        return view;
    }

    private void updateAuthUI() {
        FirebaseUser currentUser = auth.getCurrentUser();
        Button googleSignInButton = requireView().findViewById(R.id.googleSignInButton);
        Button logoutButton = requireView().findViewById(R.id.logoutButton);

        if (currentUser != null) {
            logoutButton.setVisibility(View.VISIBLE);
            googleSignInButton.setVisibility(View.GONE);
        } else {
            logoutButton.setVisibility(View.GONE);
            googleSignInButton.setVisibility(View.VISIBLE);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account);
        } catch (ApiException e) {
            Toast.makeText(requireContext(), "Sign-in failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential).addOnCompleteListener(requireActivity(), task -> {
            if (task.isSuccessful()) {
                Toast.makeText(requireContext(), "Sign-in successful!", Toast.LENGTH_SHORT).show();
                updateAuthUI();
            } else {
                Toast.makeText(requireContext(), "Sign-in failed!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}