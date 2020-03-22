package io.flutter.plugins.googlemaps;

import com.google.android.gms.maps.model.BitmapDescriptor;

public interface BitmapCache {

    BitmapDescriptor get(Object key);
    void put(Object key, BitmapDescriptor value);
    void evictAll();
    long size();

}
