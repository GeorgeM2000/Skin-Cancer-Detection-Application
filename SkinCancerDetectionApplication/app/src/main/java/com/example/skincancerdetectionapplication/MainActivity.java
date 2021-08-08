package com.example.skincancerdetectionapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.skincancerdetectionapplication.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    // Initialize variable
    private Button takePictureBtn, predictDiseaseBtn, displayPhotoBtn, selectPicture;
    private TextView textView;
    private ImageView imageView;
    private String currentImagePath = null;
    public static final int IMAGE_REQUEST = 1;
    private Bitmap img;
    private boolean callonActivityResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Assign variable
        takePictureBtn = findViewById(R.id.take_picture);
        predictDiseaseBtn = findViewById(R.id.predict_disease);
        displayPhotoBtn = findViewById(R.id.display_picture);
        selectPicture = findViewById(R.id.select_picture);
        textView = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView);

        // Request for Camera permission if not granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.CAMERA
                    }, 100);
        }

    }

    public void takePicture(View v){
        // Open Camera
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null){
            File imageFile = null;
            try {
                imageFile = getImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(imageFile != null){
                Uri imageUri = FileProvider.getUriForFile(this, "com.example.skincancerdetectionapplication.provider", imageFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                callonActivityResult = false;
                startActivityForResult(takePictureIntent, IMAGE_REQUEST);
            }
        }

    }


    public void selectPicture(View v){
        Intent selectPictureIntent = new Intent(Intent.ACTION_GET_CONTENT);
        selectPictureIntent.setType("image/*");
        callonActivityResult = true;
        startActivityForResult(selectPictureIntent, IMAGE_REQUEST);
    }


    public void displayPhoto(View v){
        Intent displayPhotoIntent = new Intent(this, DisplayPhoto.class);
        displayPhotoIntent.putExtra("image_path", currentImagePath);
        startActivity(displayPhotoIntent);
    }

    public void predictDisease(View v){

        img = Bitmap.createScaledBitmap(img, 28, 28, true);

        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 28, 28, 3}, DataType.FLOAT32);

            TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
            tensorImage.load(img);
            ByteBuffer byteBuffer = tensorImage.getBuffer();


            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Releases model resources if no longer used.
            model.close();

            String[] classes = {"Actinic keratoses and intraepithelial carcinomae"
                    , "basal cell carcinoma",
                    "benign keratosis-like lesions",
                    "dermatofibroma",
                    "melanocytic nevi",
                    "pyogenic granulomas and hemorrhage",
                    "melanoma"};
            float max = (float) 0.0;
            int index = 0;
            for(int i=0; i < outputFeature0.getFloatArray().length; i++){
                if(outputFeature0.getFloatArray()[i] > max){
                    max = outputFeature0.getFloatArray()[i];
                    index = i;
                }
            }
            textView.setText("Predicted Disease " + classes[index]);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private File getImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageName = "jpg_"+timeStamp+"_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File imageFile = File.createTempFile(imageName, ".jpg", storageDir);
        currentImagePath = imageFile.getAbsolutePath();
        return imageFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(callonActivityResult){
            super.onActivityResult(requestCode, resultCode, data);

            if(requestCode == IMAGE_REQUEST)
            {
                imageView.setImageURI(data.getData());

                Uri uri = data.getData();
                try {
                    img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}