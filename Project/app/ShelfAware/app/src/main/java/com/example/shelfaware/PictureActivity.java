package com.example.shelfaware;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.Manifest;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.DataType;

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
    private Interpreter tfliteInterpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        fab = findViewById(R.id.fab_picture);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PictureActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

        result.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    result.setError("Classification cannot be empty!");
                }
            }
        });

        result.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                result.setCursorVisible(true); // Show cursor when user clicks inside
            } else {
                result.setCursorVisible(false);
                result.clearFocus(); // Remove focus and stop blinking when clicking outside
            }
        });
        findViewById(android.R.id.content).setOnTouchListener((v, event) -> {
            result.clearFocus();
            result.setCursorVisible(false);
            v.performClick(); // This ensures accessibility and proper handling
            return false;
        });

        try {
            Log.d("MainActivity", "Loading model...");
            tfliteInterpreter = new Interpreter(loadModelFile());
            Log.d("MainActivity", "Model loaded successfully");
        } catch (IOException e) {
            Log.e("MainActivity", "Error initializing model: " + e.getMessage());
        }

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

        String[] classes = {"Golden-Delicious", "Granny-Smith", "Pink-Lady", "Red-Delicious", "Royal-Gala", "Avocado",
                "Banana", "Kiwi", "Lemon", "Lime", "Mango", "Cantaloupe", "Galia-Melon", "Honeydew-Melon", "Watermelon",
                "Nectarine", "Orange", "Papaya", "Passion-Fruit", "Peach", "Anjou", "Conference", "Kaiser", "Pineapple",
                "Plum", "Pomegranate", "Red-Grapefruit", "Satsumas", "Bravo-Apple-Juice", "Bravo-Orange-Juice", "God-Morgon-Apple-Juice",
                "God-Morgon-Orange-Juice", "God-Morgon-Orange-Red-Grapefruit-Juice", "God-Morgon-Red-Grapefruit-Juice", "Tropicana-Apple-Juice",
                "Tropicana-Golden-Grapefruit", "Tropicana-Juice-Smooth", "Tropicana-Mandarin-Morning", "Arla-Ecological-Medium-Fat-Milk", "Arla-Lactose-Medium-Fat-Milk",
                "Arla-Medium-Fat-Milk", "Arla-Standard-Milk", "Garant-Ecological-Medium-Fat-Milk", "Garant-Ecological-Standard-Milk", "Oatly-Natural-Oatghurt", "Oatly-Oat-Milk",
                "Arla-Ecological-Sour-Cream", "Arla-Sour-Cream", "Arla-Sour-Milk", "Alpro-Blueberry-Soyghurt", "Alpro-Vanilla-Soyghurt", "Alpro-Fresh-Soy-Milk",
                "Alpro-Shelf-Soy-Milk", "Arla-Mild-Vanilla-Yoghurt", "Arla-Natural-Mild-Low-Fat-Yoghurt", "Arla-Natural-Yoghurt", "Valio-Vanilla-Yoghurt",
                "Yoggi-Strawberry-Yoghurt", "Yoggi-Vanilla-Yoghurt", "Asparagus", "Aubergine", "Cabbage", "Carrots", "Cucumber", "Garlic", "Ginger", "Leek",
                "Brown-Cap-Mushroom", "Yellow-Onion", "Green-Bell-Pepper", "Orange-Bell-Pepper", "Red-Bell-Pepper", "Yellow-Bell-Pepper", "Floury-Potato",
                "Solid-Potato", "Sweet-Potato", "Red-Beet", "Beef-Tomato", "Regular-Tomato", "Vine-Tomato", "Zucchini"};

        try {
            result.setText("Classifying...");

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

            int pixel = 0;
            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    int val = intValues[pixel++];
                    byteBuffer.putFloat(((val >> 16) & 0xFF) / 127.5f - 1.0f); // R
                    byteBuffer.putFloat(((val >> 8) & 0xFF) / 127.5f - 1.0f);  // G
                    byteBuffer.putFloat((val & 0xFF) / 127.5f - 1.0f);         // B
                }
            }

            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(
                    new int[]{1, imageSize, imageSize, 3}, org.tensorflow.lite.DataType.FLOAT32);
            inputFeature0.loadBuffer(byteBuffer);

            TensorBuffer outputFeature0 = TensorBuffer.createFixedSize(
                    new int[]{1, 81}, org.tensorflow.lite.DataType.FLOAT32); // 81 classes
            tfliteInterpreter.run(inputFeature0.getBuffer(), outputFeature0.getBuffer());

            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }


            result.setText(classes[maxPos]);
            Log.d("Classifier", "Predicted: " + classes[maxPos] + " (" + maxConfidence + ")");
        } catch (Exception e) {
            Log.e("Classifier", "Error during classification: " + e.getMessage());
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
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd("grocery_model.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
        }
    }

}