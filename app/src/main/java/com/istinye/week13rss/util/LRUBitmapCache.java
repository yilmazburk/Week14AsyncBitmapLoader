package com.istinye.week13rss.util;

import android.graphics.Bitmap;
import android.media.Image;
import android.util.LruCache;
import android.widget.ImageView;

import com.istinye.week13rss.R;

import java.net.URL;

import androidx.annotation.NonNull;

public class LRUBitmapCache extends LruCache<String, Bitmap> {

    // LRU Cache using kilobytes for its own size.

    public static int getCacheSize() {
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        return cacheSize;
    }

    public LRUBitmapCache(int maxSize) {
        super(maxSize);
    }

    public LRUBitmapCache(){
        this(getCacheSize());
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getByteCount() / 1024; // Kilobytes
    }

    //Thread safe and runs atomically
    public synchronized void addBitmapToMemoryCache(String key, Bitmap value){
        if (getBitmapFromMemoryCache(key) == null) {
            this.put(key, value);
        }
    }

    public Bitmap getBitmapFromMemoryCache(String key) {
        return this.get(key);
    }

    public void loadBitmap(@NonNull URL url, @NonNull ImageView imageView) {
        final Bitmap bitmap = getBitmapFromMemoryCache(url.toString());
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.place_holder);
            BitmapAsyncLoader task = new BitmapAsyncLoader(this, imageView);
            task.execute(url);
        }
    }

}
