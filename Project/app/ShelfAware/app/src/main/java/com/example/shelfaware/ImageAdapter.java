package com.example.shelfaware;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

        if (imageItem.isExpiringSoon()) {
            holder.expiringSoonTextView.setVisibility(View.VISIBLE);
        } else {
            holder.expiringSoonTextView.setVisibility(View.GONE);
        }

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
                imageItemList.remove(adapterPosition);
                notifyItemRemoved(adapterPosition);
                Toast.makeText(v.getContext(), "Item deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageItemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView;
        public TextView expirationDateTextView;
        public TextView expiringSoonTextView;
        public FloatingActionButton deleteFab;
        public CheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.classifiedImageView);
            textView = itemView.findViewById(R.id.classificationTextView);
            expirationDateTextView = itemView.findViewById(R.id.expirationDateTextView);
            expiringSoonTextView = itemView.findViewById(R.id.expiringSoonTextView);
            deleteFab = itemView.findViewById(R.id.deleteFab);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
