package com.example.shelfaware;

public class ImageItem {
    private String imageUri;
    private String title;
    private String expirationDate;
    private boolean expiringSoon;
    private boolean isChecked;

    public ImageItem(String imageUri, String title, String expirationDate, boolean expiringSoon) {
        this.imageUri = imageUri;
        this.title = title;
        this.expirationDate = expirationDate;
        this.expiringSoon = expiringSoon;
        this.isChecked = false;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getTitle() {
        return title;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public boolean isExpiringSoon() {
        return expiringSoon;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }
}
