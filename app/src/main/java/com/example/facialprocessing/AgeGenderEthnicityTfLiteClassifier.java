package com.example.facialprocessing;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class AgeGenderEthnicityTfLiteClassifier extends TfLiteClassifier{

    private static final String TAG = "AgeGenderTfLite";

    private static final String MODEL_FILE ="age_gender_ethnicity_224_deep-03-0.13-0.97-0.88.tflite";

    public AgeGenderEthnicityTfLiteClassifier(final Context context) throws IOException {
        super(context,MODEL_FILE);
    }

    protected void addPixelValue(int val) {
        imgData.putFloat((val & 0xFF) - 103.939f);
        imgData.putFloat(((val >> 8) & 0xFF) - 116.779f);
        imgData.putFloat(((val >> 16) & 0xFF) - 123.68f);
    }

    protected ClassifierResult getResults(float[][][] outputs) {

        //age
        final float[] age_features = outputs[0][0];
        int max_index = 2;
        float[] probabs = new float[max_index];
        ArrayList<Integer> indices = new ArrayList<>();

        for (int j = 0; j < max_index; ++j) {
            int bestInd=-1;
            float maxVal=-1;
            for(int i=0;i<age_features.length;++i){
                if(maxVal<age_features[i] && !indices.contains(i)){
                    maxVal=age_features[i];
                    bestInd=i;
                }
            }
            if(bestInd!=-1)
                indices.add(bestInd);
        }
        float sum = 0;
        for (int j = 0; j < max_index; ++j) {
            probabs[j] = age_features[indices.get(j)];
            sum += probabs[j];
        }
        double age = 0;
        for (int j = 0; j < max_index; ++j) {
            age += (indices.get(j) + 0.5) * probabs[j] / sum;
        }

        float gender = outputs[1][0][0];
        final float[] ethnicity_scores = outputs[2][0];

        float[] features = outputs[3][0];
        sum = 0;
        for (int i = 0; i < features.length; ++i) {
            sum += features[i] * features[i];
        }
        sum = (float) Math.sqrt(sum);
        if(sum>0) {
            for (int i = 0; i < features.length; ++i)
                features[i] /= sum;
        }
        Log.i(TAG, "!!!!!!!!!!!!!!!!!!!!!!!!! end feature extraction first feat=" + features[0] + " last feat=" + features[features.length - 1]);


        FaceData res=new FaceData(age, gender, ethnicity_scores,features);
        return res;
    }
}
