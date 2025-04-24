package com.example.grocerystorev2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    Button camera, gallery;
    ImageView imageView;
    TextView result;
    int imageSize = 224;
    private Interpreter tfliteInterpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);
        imageView = findViewById(R.id.imageView);
        result = findViewById(R.id.result);

        try {
            Log.d("MainActivity", "Loading model...");
            tfliteInterpreter = new Interpreter(loadModelFile());
            Log.d("MainActivity", "Model loaded successfully");
        } catch (IOException e) {
            Log.e("MainActivity", "Error initializing model: " + e.getMessage());
        }

        camera.setOnClickListener(view -> {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 3);
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
        });

        gallery.setOnClickListener(view -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(galleryIntent, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bitmap image = null;

            if (requestCode == 3) { // Camera
                image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
            } else if (requestCode == 1) { // Gallery
                Uri dat = data.getData();
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (image != null) {
                imageView.setImageBitmap(image);
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
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

    public void classifyImage(Bitmap image) {
        String[] classes = {
                "Apple", "Apricot", "Asparagus", "Avocado", "Banana", "Beetroot", "Blackberries", "Blueberries",
                "Broccoli", "Cabbage", "Cantaloupe", "Carrots", "Cauliflower", "Celery", "Cherries", "Chestnuts",
                "Coconut", "Corn", "Cucumber", "Dates", "Eggplant", "Fennel", "Figs", "Garlic", "Ginger", "Grapefruit",
                "Grapes", "Green beans", "Guava", "Hazelnut", "Honeydew melon", "Kale", "Kiwi", "Leek", "Lemon", "Lettuce",
                "Lime", "Lychee", "Mandarin", "Mango", "Mushrooms", "Nectarine", "Okra", "Onion", "Orange", "Papaya",
                "Parsley", "Passion fruit", "Peach", "Pear", "Peas", "Pepper", "Pineapple", "Plum", "Pomegranate",
                "Potato", "Pumpkin", "Radish", "Raspberry", "Red cabbage", "Redcurrant", "Rhubarb", "Spinach", "Squash",
                "Strawberries", "Sweet Potato", "Tomato", "Turnip", "Walnut", "Watermelon", "Yam", "Yoghurt", "Zucchini",
                "Milk", "Soy milk", "Oat milk", "Juice", "Oatghurt", "Soyghurt", "Sour cream", "Sour milk", "Red Grapefruit"
        };

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
            //result.setText("Test");
            Log.d("Classifier", "Predicted: " + classes[maxPos] + " (" + maxConfidence + ")");

        } catch (Exception e) {
            Log.e("Classifier", "Error during classification: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tfliteInterpreter != null) {
            tfliteInterpreter.close();
        }
    }
}
