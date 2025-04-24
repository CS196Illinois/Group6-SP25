package com.example.shelfaware;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private List<ImageItem> items;
    public ImageAdapter(List<ImageItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        ImageItem item = items.get(position);
        holder.classifiedImageView.setImageBitmap(item.getImageBitmap());
        holder.classificationTextview.setText(item.getClassification());
        holder.expirationDateTextView.setText(item.getExpirationDate());

        holder.deleteFab.setOnClickListener(view -> {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView classifiedImageView;
        TextView classificationTextview;
        TextView expirationDateTextView;
        FloatingActionButton deleteFab;
        public ImageViewHolder(View itemView) {
            super(itemView);
            classifiedImageView = itemView.findViewById(R.id.classifiedImageView);
            classificationTextview = itemView.findViewById(R.id.classificationTextView);
            expirationDateTextView = itemView.findViewById(R.id.expirationDateTextView);
            deleteFab = itemView.findViewById(R.id.deleteFab);
        }
    }



}
