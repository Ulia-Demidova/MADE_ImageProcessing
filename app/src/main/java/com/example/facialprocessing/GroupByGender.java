package com.example.facialprocessing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.facialprocessing.mtcnn.Box;
import com.example.facialprocessing.mtcnn.MTCNNModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class GroupByGender extends AppCompatActivity {

    private static final String TAG = "GroupByGenderActivity";

    RecyclerView parentRecyclerView;
    List<String> images;
    List<String> male;
    List<String> female;

    private static int minFaceSize=40;
    private MTCNNModel mtcnnFaceDetector=null;
    private AgeGenderEthnicityTfLiteClassifier facialAttributeClassifier=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_by_gender);

        parentRecyclerView = findViewById(R.id.parent_recyclerview);

        Log.i(TAG, "!!!!!!!!!!!!!!!!! In onCreate");

        init();
    }

    private void init() {
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


        parentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        Runnable runnable = new Runnable() {
            public void run() {
                images = ImageGallery.listOfImages(GroupByGender.this);
                groupByGender();
                Log.i(TAG, "!!!!!!!!!!!!!!!!! Images size: " + images.size());
                handler.sendEmptyMessage(0);
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
//        loadImages();
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            parentRecyclerView.setAdapter(new ParentItemAdapter(GroupByGender.this, ParentItemList()));
        }
    };

    private void loadImages() {
        images = ImageGallery.listOfImages(this);
        groupByGender();
        Log.i(TAG, "!!!!!!!!!!!!!!!!! Images size: " + images.size());
        parentRecyclerView.setAdapter(new ParentItemAdapter(this, ParentItemList()));
    }

    private void groupByGender() {
        male = new ArrayList<>();
        female = new ArrayList<>();

        int gender;
        String image_path;
        for (int i = 0; i < images.size(); i++) {
            image_path = images.get(i);
            gender = get_gender(image_path);
            if (gender > 0) {
                female.add(image_path);
                if (gender == 2)
                    male.add(image_path);
            } else if (gender == 0) {
                male.add(image_path);
            } else {
                Log.i(TAG, "!!!!!!!!!!!!!!! There is no people on the photo: " + image_path);
            }
        }
    }

    private int get_gender(String image_path) {
        int gender = -1;
        Bitmap bmp = BitmapFactory.decodeFile(image_path);

        if (bmp == null) {
            Log.e(TAG, "!!!!!!!!!!! Can't decode file " + image_path);
            finish();
        }

        Bitmap resizedBitmap = bmp;
        double minSize = 600.0;
        double scale=Math.min(bmp.getWidth(), bmp.getHeight()) / minSize;
        if (scale > 1.0) {
            resizedBitmap = Bitmap.createScaledBitmap(bmp, (int)(bmp.getWidth()/scale),
                    (int)(bmp.getHeight()/scale), false);
            bmp = resizedBitmap;
        }
        Vector<Box> bboxes = mtcnnFaceDetector.detectFaces(resizedBitmap, minFaceSize);

        for (Box box : bboxes) {
            android.graphics.Rect bbox = new android.graphics.Rect(Math.max(0,bmp.getWidth()*box.left() / resizedBitmap.getWidth()),
                    Math.max(0,bmp.getHeight()* box.top() / resizedBitmap.getHeight()),
                    bmp.getWidth()* box.right() / resizedBitmap.getWidth(),
                    bmp.getHeight() * box.bottom() / resizedBitmap.getHeight()
            );

            if(facialAttributeClassifier!=null && bbox.width()>0 && bbox.height()>0) {
                Bitmap faceBitmap = Bitmap.createBitmap(bmp, bbox.left, bbox.top, bbox.width(), bbox.height());
                Bitmap resultBitmap = Bitmap.createScaledBitmap(faceBitmap, facialAttributeClassifier.getImageSizeX(), facialAttributeClassifier.getImageSizeY(), false);
                FaceData res = (FaceData) (facialAttributeClassifier.classifyFrame(resultBitmap));
                if (res.isMale()) {
                    if (gender == -1) {
                        gender = 0;
                    } else if (gender == 1) {
                        gender = 2;
                        break;
                    }
                } else {
                    if (gender == -1) {
                        gender = 1;
                    } else if (gender == 0) {
                        gender = 2;
                        break;
                    }
                }
            }
        }
        return gender;
    }

    private List<ParentItem> ParentItemList() {
        List<ParentItem> itemList = new ArrayList<>();
        itemList.add(new ParentItem("Male", male));
        itemList.add(new ParentItem("Female", female));
        return itemList;
    }


}