package com.example.shelfaware;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.Manifest;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.core.content.ContextCompat;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.shelfaware.ml.Model;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.TimeZone;

public class PictureActivity extends AppCompatActivity {

    Button camera, gallery;
    ImageView imageView;
    TextView result;
    FloatingActionButton fab;
    int imageSize = 224; // depends on the model

    Button selectExpirationDate;
    Button addItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        fab = findViewById(R.id.fab_picture);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PictureActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        camera = findViewById(R.id.take);
        gallery = findViewById(R.id.upload);
        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);

        selectExpirationDate = findViewById(R.id.selectDate);
        selectExpirationDate.setVisibility(View.INVISIBLE);

        addItem = findViewById(R.id.add_item);
        addItem.setVisibility(View.INVISIBLE);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent,1);
            }
        });


        selectExpirationDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Create the MaterialDatePicker instance
                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select Expiration Date") // Dialog title
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds()) // Default to today
                        .build();

                // Handle the selected date
                datePicker.addOnPositiveButtonClickListener(selection -> {
                    // Convert the selected timestamp into a readable date
                    TimeZone timeZone = TimeZone.getDefault();
                    Calendar calendar = Calendar.getInstance(timeZone);
                    calendar.setTimeInMillis(selection);

                    calendar.add(Calendar.DAY_OF_MONTH, 1);

                    int year = calendar.get(Calendar.YEAR);
                    int month = calendar.get(Calendar.MONTH);
                    int day = calendar.get(Calendar.DAY_OF_MONTH);

                    String formattedDate = (month + 1) + "/" + day + "/" + year;
                    selectExpirationDate.setText(formattedDate); // Update button text
                });

                // Show the MaterialDatePicker
                datePicker.show(getSupportFragmentManager(), "MATERIAL_DATE_PICKER");
            }
        });

        addItem.setOnClickListener(view -> {
            String classification = result.getText().toString();
            String expirationDate = selectExpirationDate.getText().toString();

            if (expirationDate.equals("Select Expiration Date") || expirationDate.isEmpty()) {
                Toast.makeText(this, "Please select an expiration date", Toast.LENGTH_SHORT).show();
                return;
            }

            Bitmap imageBitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
            // if SQLite/Room:
            Intent intent = new Intent();
            intent.putExtra("classification", classification);
            intent.putExtra("expirationDate", expirationDate);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] byteArray = stream.toByteArray();


            intent.putExtra("image", byteArray);
            setResult(RESULT_OK, intent);
            new Handler().postDelayed(() -> finish(), 200);
        });

        /*
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
         */

    }

    public void classifyImage(Bitmap image) {
        try {
            Model model = Model.newInstance(getApplicationContext());
            // create inputs for reference
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3); // 4 b/c that's how many bytes a float takes up
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            // iterate over each pixel and extract RGB values; add those values individually to the byte buffer
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++]; // R G B
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1)); // divides by 1 b/c tflite model already handles the preprocessing when it divided by 255
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // runs model inference and gets result
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find index of class with biggest confidence
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }

            String[] classes = {"Apple", "Banana", "Orange"};
            result.setText(classes[maxPos]);

            // releases model resources if no longer used
            model.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data == null || data.getExtras() == null) {
            Log.e("PictureActivity", "Camera data is null!");
        }

        if (resultCode == RESULT_OK) {
            if (requestCode == 3) {
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);

                selectExpirationDate.setVisibility(View.VISIBLE);
                addItem.setVisibility(View.VISIBLE);
            } else {
                // handles getting image from gallery
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);

                selectExpirationDate.setVisibility(View.VISIBLE);
                addItem.setVisibility(View.VISIBLE);
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}