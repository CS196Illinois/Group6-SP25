package com.example.shelfaware;

import android.graphics.Bitmap;

public class ImageItem {
    private final Bitmap imageBitmap;

    private String classification;
    private String expirationDate;

    public Bitmap getImageBitmap() {
        return imageBitmap;
    }

    public String getClassification() {
        return classification;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public ImageItem(Bitmap imageBitmap, String classification, String expirationDate) {
        this.imageBitmap = imageBitmap;
        this.classification = classification;
        this.expirationDate = expirationDate;
    }

}
