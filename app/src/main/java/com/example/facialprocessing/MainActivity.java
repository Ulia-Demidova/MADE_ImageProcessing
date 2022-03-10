package com.example.facialprocessing;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;

import com.example.facialprocessing.mtcnn.Box;
import com.example.facialprocessing.mtcnn.MTCNNModel;
import com.bumptech.glide.Glide;

import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final int REQUEST_CODE_PERMISSION = 101;
    private ImageView imageView;
    private Bitmap sampledImage=null;
    private static int minFaceSize=40;
    private MTCNNModel mtcnnFaceDetector=null;
    private AgeGenderEthnicityTfLiteClassifier facialAttributeClassifier=null;
    private EmotionTfLiteClassifier emotionClassifierTfLite =null;
    private AttributePytorchClassifier attributeCelebaClassifier=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        imageView=findViewById(R.id.inputImageView);

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            Log.i(TAG, "!!!!!!!!!!!!!!!!! Request Permission");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
        } else {
            init();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    private void init(){
        try {
            mtcnnFaceDetector = MTCNNModel.Companion.create(getAssets());
        } catch (final Exception e) {
            Log.e(TAG, "Exception initializing MTCNNModel!"+e);
        }
        try {
            facialAttributeClassifier = new AgeGenderEthnicityTfLiteClassifier(getApplicationContext());
        } catch (final Exception e) {
            Log.e(TAG, "Exception initializing AgeGenderEthnicityTfLiteClassifier!", e);
        }

        try {
            emotionClassifierTfLite = new EmotionTfLiteClassifier(getApplicationContext());
        } catch (final Exception e) {
            Log.e(TAG, "Exception initializing EmotionTfLiteClassifier!", e);
        }

        try {
            attributeCelebaClassifier = new AttributePytorchClassifier(getApplicationContext());
        } catch (final Exception e) {
            Log.e(TAG, "Exception initializing EmotionTfLiteClassifier!", e);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grandResults);

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grandResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read external storage permission granted", Toast.LENGTH_SHORT).show();
                init();
            } else {
                Toast.makeText(this, "Read external storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static final int SELECT_PICTURE = 1;
    private void openImageFile(int requestCode){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"),requestCode);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_openGallery:
                openImageFile(SELECT_PICTURE);
                return true;

            case R.id.action_agegender:
                if(isImageLoaded()) {
                    mtcnnDetectionAndAttributesRecognition(facialAttributeClassifier);
                }
                return true;
            case R.id.action_emotion_tf:
                if(isImageLoaded()) {
                    mtcnnDetectionAndAttributesRecognition(emotionClassifierTfLite);
                }
                return true;
            case R.id.action_group_by_gender:
                GroupPhotos();
                return true;
            case R.id.celeba_attr:
                if(isImageLoaded()) {
                    mtcnnDetectionAndCelebaAttributesRecognizer();
                }
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    private void GroupPhotos() {
        Intent intent = new Intent(this, GroupByGender.class);
        startActivity(intent);
    }

    private boolean isImageLoaded(){
        if(sampledImage==null)
            Toast.makeText(getApplicationContext(),
                    "It is necessary to open image firstly",
                    Toast.LENGTH_SHORT).show();
        return sampledImage!=null;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData(); //The uri with the location of the file
                Log.d(TAG, "uri" + selectedImageUri);
                Glide.with(this).load(selectedImageUri).into(imageView);
                try {
                    sampledImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void mtcnnDetectionAndAttributesRecognition(TfLiteClassifier classifier){
        Bitmap bmp = sampledImage;

        Bitmap resizedBitmap=bmp;
        double minSize=600.0;
        double scale=Math.min(bmp.getWidth(),bmp.getHeight())/minSize;
        if(scale>1.0) {
            resizedBitmap = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth()/scale), (int)(bmp.getHeight()/scale), false);
            bmp=resizedBitmap;
        }
        long startTime = SystemClock.uptimeMillis();
        Vector<Box> bboxes = mtcnnFaceDetector.detectFaces(resizedBitmap, minFaceSize);//(int)(bmp.getWidth()*MIN_FACE_SIZE));
        Log.i(TAG, "Timecost to run mtcnn: " + Long.toString(SystemClock.uptimeMillis() - startTime));

        Bitmap tempBmp = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tempBmp);
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setDither(true);
        p.setColor(Color.BLUE);
        p.setStrokeWidth(5);

        Paint p_text = new Paint();
        p_text.setColor(Color.WHITE);
        p_text.setStyle(Paint.Style.FILL);
        p_text.setColor(Color.GREEN);
        p_text.setTextSize(24);

        c.drawBitmap(bmp, 0, 0, null);

        for (Box box : bboxes) {

            p.setColor(Color.RED);
            android.graphics.Rect bbox = new android.graphics.Rect(Math.max(0,bmp.getWidth()*box.left() / resizedBitmap.getWidth()),
                    Math.max(0,bmp.getHeight()* box.top() / resizedBitmap.getHeight()),
                    bmp.getWidth()* box.right() / resizedBitmap.getWidth(),
                    bmp.getHeight() * box.bottom() / resizedBitmap.getHeight()
            );

            c.drawRect(bbox, p);

            if(classifier!=null && bbox.width()>0 && bbox.height()>0) {
                Bitmap faceBitmap = Bitmap.createBitmap(bmp, bbox.left, bbox.top, bbox.width(), bbox.height());
                Bitmap resultBitmap = Bitmap.createScaledBitmap(faceBitmap, classifier.getImageSizeX(), classifier.getImageSizeY(), false);
                ClassifierResult res = classifier.classifyFrame(resultBitmap);
                c.drawText(res.toString(), bbox.left, Math.max(0, bbox.top - 20), p_text);
                Log.i(TAG, res.toString());
            }
        }
        imageView.setImageBitmap(tempBmp);
    }
    private void mtcnnDetectionAndCelebaAttributesRecognizer() {
        Bitmap bmp = sampledImage;

        Bitmap resizedBitmap=bmp;
        double minSize=600.0;
        double scale=Math.min(bmp.getWidth(),bmp.getHeight())/minSize;
        if(scale>1.0) {
            resizedBitmap = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth()/scale), (int)(bmp.getHeight()/scale), false);
            bmp=resizedBitmap;
        }
        long startTime = SystemClock.uptimeMillis();
        Vector<Box> bboxes = mtcnnFaceDetector.detectFaces(resizedBitmap, minFaceSize);//(int)(bmp.getWidth()*MIN_FACE_SIZE));
        Log.i(TAG, "Timecost to run mtcnn: " + Long.toString(SystemClock.uptimeMillis() - startTime));

        Bitmap tempBmp = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(tempBmp);
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setDither(true);
        p.setColor(Color.BLUE);
        p.setStrokeWidth(5);

        TextPaint p_text = new TextPaint();
        p_text.setColor(Color.WHITE);
        p_text.setStyle(Paint.Style.FILL);
        p_text.setColor(Color.GREEN);
        p_text.setTextSize(24);

        c.drawBitmap(bmp, 0, 0, null);

        for (Box box : bboxes) {

            p.setColor(Color.RED);
            android.graphics.Rect bbox = new android.graphics.Rect(Math.max(0,bmp.getWidth()*box.left() / resizedBitmap.getWidth()),
                    Math.max(0,bmp.getHeight()* box.top() / resizedBitmap.getHeight()),
                    bmp.getWidth()* box.right() / resizedBitmap.getWidth(),
                    bmp.getHeight() * box.bottom() / resizedBitmap.getHeight()
            );

            c.drawRect(bbox, p);

            if(attributeCelebaClassifier!=null && bbox.width()>0 && bbox.height()>0) {
                Bitmap faceBitmap = Bitmap.createBitmap(bmp, bbox.left, bbox.top, bbox.width(), bbox.height());
                String res=attributeCelebaClassifier.recognize(faceBitmap);

                StaticLayout staticLayout = new StaticLayout(res, p_text, bmp.getWidth() - bbox.right,
                        Layout.Alignment.ALIGN_NORMAL, 1, 0, false);

                c.save();
                c.translate(bbox.right, Math.max(0, bbox.top));
                staticLayout.draw(c);
                c.restore();
//                c.drawText(res, bbox.right, Math.max(0, bbox.top - 20), p_text);
                Log.i(TAG, res);
            }
        }
        imageView.setImageBitmap(tempBmp);

    }
}