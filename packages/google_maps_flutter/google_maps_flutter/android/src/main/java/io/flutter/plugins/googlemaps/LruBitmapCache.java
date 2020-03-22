package io.flutter.plugins.googlemaps;

import android.util.LruCache;

import com.google.android.gms.maps.model.BitmapDescriptor;

public class LruBitmapCache implements BitmapCache {

    private final LruCache<Object, BitmapDescriptor> cache;

    LruBitmapCache(int maxSize) {
         cache = new LruCache<Object, BitmapDescriptor>(maxSize) {
//  we cannot really determine mem size of BitmapDescriptor, since it is exactly that:
//  a descriptor. Only if created from bitmap it has real size, other types of descriptors
//  are lazy-loaded by google map renderer

//        protected int sizeOf(String key, BitmapDescriptor value) {
//            return value.????();
//        }
        };
    }

    @Override
    public BitmapDescriptor get(Object key) {
        return cache.get(key);
    }

    @Override
    public void put(Object key, BitmapDescriptor value) {
        cache.put(key, value);
    }

    @Override
    public void evictAll() {
        cache.evictAll();
    }

    @Override
    public long size() {
        return cache.size();
    }
}
