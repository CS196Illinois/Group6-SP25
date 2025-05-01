package com.example.shelfaware;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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

    public int getExpirationStatus() {
        try {
            // Ensure the expiration date is not null or empty
            if (expirationDate == null || expirationDate.isEmpty()) {
                return 0; // Default to "No urgency" if date is missing
            }

            SimpleDateFormat sdf;

            // Detect and use the correct date format
            if (expirationDate.contains("/")) {
                sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()); // U.S. format
            } else {
                sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()); // Standard format
            }

            Date expiry = sdf.parse(expirationDate);
            if (expiry == null) return 0; // Parsing failed, default to "No urgency"

            // Get today's date (set time to midnight to avoid time-related inaccuracies)
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            // Calculate the number of days until expiration
            long diffInMillis = expiry.getTime() - today.getTimeInMillis();
            long daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(diffInMillis);
            if (diffInMillis < 0) {
                daysUntilExpiry -= 1; // Adjust for possible rounding errors
            }

            // Log for debugging
            System.out.println("Expiration Date: " + expirationDate);
            System.out.println("Days Until Expiry: " + daysUntilExpiry);
            Log.d("ImageItem", "Parsed Expiration Date: " + sdf.format(expiry));

            // Determine expiration status
            if (daysUntilExpiry < 0) {
                return -1; // Expired
            } else if (daysUntilExpiry <= 3) {
                return 2; // High urgency
            } else if (daysUntilExpiry <= 7) {
                return 1; // Medium urgency
            } else {
                return 0; // No urgency
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return 0; // Default if parsing fails
        }
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
