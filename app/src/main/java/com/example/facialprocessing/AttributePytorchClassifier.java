package com.example.facialprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class AttributePytorchClassifier {
    private static final String TAG = "CelebaAttributes";

    private Module module=null;
    private List<String> labels;
    private int width=224;
    private int height=width;
    private float threshold=0.1f;
    private static final String MODEL_FILE = "celeba_model.ptl";

    private float[] NORM_MEAN_RGB = new float[] {0.5063f, 0.4258f, 0.3832f};
    private float[] NORM_STD_RGB = new float[] {0.2644f, 0.2436f, 0.2397f};

    public AttributePytorchClassifier(final Context context) throws IOException {
        module= LiteModuleLoader.load(assetFilePath(context, MODEL_FILE));
        loadLabels(context);
    }
    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
    private void loadLabels(final Context context){
        BufferedReader br = null;
        labels=new ArrayList<>();
        try {
            br = new BufferedReader(new InputStreamReader(context.getAssets().open("celeba_attributes.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                String[] categoryInfo=line.trim().split(":");
                String category=categoryInfo[1];
                labels.add(category);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException("Problem reading celeba attributes file!" , e);
        }
    }
    public String recognize(Bitmap bitmap){
        final float[] scores =  classifyImage(bitmap);
        StringBuilder str=new StringBuilder();
        str.append("");
        int[] idx = {5, 8, 9, 11, 15, 22, 24, 31, 31,33, 39};
        for (int i = 0; i < idx.length; i++) {
            if (sigmoid(scores[idx[i]]) > threshold) {
                str.append(labels.get(i)+"\n");
            }
        }
        return str.toString();
    }

    private float[] classifyImage(Bitmap bitmap) {
        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                NORM_MEAN_RGB, NORM_STD_RGB);
        final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
        final float[] scores = outputTensor.getDataAsFloatArray();
        return scores;
    }

    public static float sigmoid(float x) {
        return (float) (1 / (1+Math.exp(-x)));
    }
}
