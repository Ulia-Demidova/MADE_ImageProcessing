package com.example.facialprocessing;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class ChildItemAdapter extends RecyclerView.Adapter<ChildItemAdapter.ChildViewHolder>{
    private static final String TAG = "ChildAdapter";

    private List<String> images;
    private Context context;

    public ChildItemAdapter(Context context, List<String> images) {
        this.images = images;
        this.context = context;
        Log.i(TAG, "!!!!!!!!!!!!! Images size from child adapter: " + this.images.size());
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChildViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.child_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        String image = images.get(position);

        Glide.with(context).load(image).into(holder.image);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public class ChildViewHolder extends RecyclerView.ViewHolder {
        ImageView image;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.image);
        }
    }
}
