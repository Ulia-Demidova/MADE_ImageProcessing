package com.example.facialprocessing;


import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ParentItemAdapter extends RecyclerView.Adapter<ParentItemAdapter.ParentViewHolder>{
    private static final String TAG = "ParentAdapter";

    private RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
    private List<ParentItem> itemList;
    private Context context;

    public ParentItemAdapter(Context context, List<ParentItem> itemList) {
        this.context = context;
        this.itemList = itemList;
        Log.i(TAG, "!!!!!!!!!! Size parent item list: " + this.itemList.size());
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ParentViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.parent_item, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {
        ParentItem parentItem = itemList.get(position);
        holder.ParentItemTitle.setText(parentItem.getParentItemTitle());
        Log.i(TAG, "!!!!!!!!!!!!!!! Parent Title: " + holder.ParentItemTitle.getText());

        holder.ChildRecyclerView.setHasFixedSize(true);
        holder.ChildRecyclerView.setLayoutManager(
                new GridLayoutManager(
                        holder.ChildRecyclerView.getContext(), 1,
                        GridLayoutManager.HORIZONTAL, false
                ));

        ChildItemAdapter childItemAdapter = new ChildItemAdapter(context,
                parentItem.getImages());

        holder.ChildRecyclerView.setAdapter(childItemAdapter);
        holder.ChildRecyclerView.setRecycledViewPool(viewPool);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    class ParentViewHolder extends RecyclerView.ViewHolder {

        private TextView ParentItemTitle;
        private RecyclerView ChildRecyclerView;

        ParentViewHolder(final View itemView) {
            super(itemView);

            ParentItemTitle = itemView.findViewById(R.id.parent_item_title);
            ChildRecyclerView = itemView.findViewById(R.id.child_recyclerview);
        }
    }
}

