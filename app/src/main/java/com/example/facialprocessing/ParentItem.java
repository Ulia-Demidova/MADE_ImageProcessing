package com.example.facialprocessing;

import java.util.List;

public class ParentItem {
    private String ParentItemTitle;
    private List<String> images;

    public ParentItem(String ParentItemTitle, List<String> images) {
        this.ParentItemTitle = ParentItemTitle;
        this.images = images;
    }

    public String getParentItemTitle() {
        return ParentItemTitle;
    }

    public List<String> getImages() {
        return images;
    }
}
