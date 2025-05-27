package com.example.shelfaware;

import static androidx.core.content.ContentProviderCompat.requireContext;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private List<ImageItem> imageItemList;

    public ImageAdapter(List<ImageItem> imageItemList) {
        this.imageItemList = imageItemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image, parent, false); // Link to your image_item.xml
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageItem imageItem = imageItemList.get(position);

        // Set image and text
        holder.imageView.setImageURI(Uri.parse(imageItem.getImageUri()));
        holder.textView.setText(imageItem.getTitle());
        holder.expirationDateTextView.setText(imageItem.getExpirationDate());

        int expirationStatus = imageItem.getExpirationStatus();
        int backgroundColor;
        int textColor;

        if (expirationStatus == -1) { // Expired
            backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.expiredMaroon);
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.peach);
            holder.expiringSoonTextView.setVisibility(View.VISIBLE);
            holder.expiringSoonTextView.setText("Expired!");
        } else if (expirationStatus == 2) { // High urgency (≤3 days)
            backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.warningOrange);
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.peach);
            holder.expiringSoonTextView.setVisibility(View.VISIBLE);
            holder.expiringSoonTextView.setText("Expiring Soon!");
        } else if (expirationStatus == 1) { // Medium urgency (≤7 days)
            backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.warningPeach);
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.forest);
            holder.expiringSoonTextView.setVisibility(View.VISIBLE);
            holder.expiringSoonTextView.setText("Best Used Soon!");
        } else { // No urgency
            backgroundColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.peach);
            textColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.forest);
            holder.expiringSoonTextView.setVisibility(View.GONE);
        }
        holder.cardView.setCardBackgroundColor(backgroundColor);
        holder.textView.setTextColor(textColor);
        holder.expirationDateTextView.setTextColor(textColor);
        holder.expiringSoonTextView.setTextColor(textColor);
        holder.checkBox.setButtonTintList(ColorStateList.valueOf(textColor));
        holder.deleteFab.setImageTintList(ColorStateList.valueOf(textColor));



        // Set checkbox state
        holder.checkBox.setChecked(imageItem.isChecked());

        // Handle checkbox toggle
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            imageItem.setChecked(isChecked);
        });

        // Handle delete button click
        holder.deleteFab.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                ImageItem itemToDelete = imageItemList.get(adapterPosition);
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    DatabaseReference userItemsRef = FirebaseDatabase.getInstance().getReference("items").child(currentUser.getUid());

                    if (userItemsRef != null && itemToDelete.getItemId() != null) {
                        userItemsRef.child(itemToDelete.getItemId()).removeValue().addOnSuccessListener(aVoid -> {
                            if (imageItemList.contains(itemToDelete)) {
                                imageItemList.remove(adapterPosition);
                                removeFromLocalStorage(v.getContext(), itemToDelete);
                                notifyItemRemoved(adapterPosition);
                            }
                        }).addOnFailureListener(e -> {
                            Log.e("ImageAdapter", "Failed to delete item: " + e.getMessage());
                            Toast.makeText(v.getContext(), "Failed to delete item", Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Log.e("ImageAdapter", "Item ID is null!");
                        Toast.makeText(v.getContext(), "Error: Cannot find item ID", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageItemList.size();
    }

    public void setData(List<ImageItem> itemList) {
        imageItemList.clear();
        imageItemList.addAll(itemList);
        notifyDataSetChanged(); // Refresh RecyclerView
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CardView cardView;
        public ImageView imageView;
        public TextView textView;
        public TextView expirationDateTextView;
        public TextView expiringSoonTextView;
        public FloatingActionButton deleteFab;
        public CheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            imageView = itemView.findViewById(R.id.classifiedImageView);
            textView = itemView.findViewById(R.id.classificationTextView);
            expirationDateTextView = itemView.findViewById(R.id.expirationDateTextView);
            expiringSoonTextView = itemView.findViewById(R.id.expiringSoonTextView);
            deleteFab = itemView.findViewById(R.id.deleteFab);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
    private void removeFromLocalStorage(Context context, ImageItem itemToDelete) {
        SharedPreferences prefs = context.getSharedPreferences("local_data", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();

        // Retrieve existing local items
        String json = prefs.getString("items", "[]");
        Type type = new TypeToken<List<ImageItem>>() {}.getType();
        List<ImageItem> localItems = gson.fromJson(json, type);

        // Remove item and save changes
        localItems.removeIf(item -> item.getImageUri().equals(itemToDelete.getImageUri()) &&
                item.getTitle().equals(itemToDelete.getTitle()));

        editor.putString("items", gson.toJson(localItems));
        editor.apply();
    }


}
