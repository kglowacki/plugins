package io.flutter.plugins.googlemaps;

import android.util.Log;

import androidx.core.util.Consumer;

import com.google.android.gms.maps.model.BitmapDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.flutter.plugin.common.MethodChannel;

/**
 * kris mod.
 */
class MarkersUpdater {

    private final MethodChannel methodChannel;
    private final MarkersController markersController;

    private static final String ICON = "icon";
    private static final String RESOLVABLE = "resolvable";
    private static final String MARKER_GENERATE_ICON_FUNCTION = "marker#onResolveBitmaps";
    private static final String MARKER_GENERATE_ICON_FUNCTION_ARG = "keys";

    private BitmapCache bitmapCache;

    private static final String TAG = "MarkersUpdater";

    MarkersUpdater(MethodChannel methodChannel, MarkersController markersController, BitmapCache bitmapCache) {
        this.methodChannel = methodChannel;
        this.markersController = markersController;
        this.bitmapCache = bitmapCache;
    }

    void addMarkers(final List<Object> markersToAdd) {
        markersUpdate(markersToAdd, null, null, null);
    }

    void markersUpdate(final List<Object> markersToAdd, final List<Object> markersToChange, final List<Object> markerIdsToRemove, final MethodChannel.Result result) {
        Log.d(TAG,
                "toAdd="+(markersToAdd==null?0:markersToAdd.size())+
                ", toChange="+(markersToChange==null?0:markersToChange.size())+
                ", toRemove="+(markerIdsToRemove==null?0:markerIdsToRemove.size()));

        final Set<Object> keys = new HashSet<>();
        keys.addAll(getIconDescriptors(markersToAdd));
        keys.addAll(getIconDescriptors(markersToChange));

        Log.d(TAG, "resolvable icons = "+keys.size());

        fetchIcons(keys, new Consumer<Map<Object, Object>>(){
            @Override
            public void accept(Map<Object, Object> resolvedIcons) {

                Log.d(TAG, "resolved icons = "+resolvedIcons.size());
                applyIcons(markersToAdd, resolvedIcons);
                applyIcons(markersToChange, resolvedIcons);

                markersController.addMarkers(markersToAdd);
                markersController.changeMarkers(markersToChange);
                markersController.removeMarkers(markerIdsToRemove);
                if (result != null) result.success(null);
            }
        });
    }

    void fetchIcons(Set<Object> iconDescriptors, final Consumer<Map<Object, Object>> mapConsumer) {
        if (iconDescriptors.isEmpty()) {
            mapConsumer.accept(Collections.emptyMap());
        } else {
            final Map<String, Object> arguments = new HashMap<>(2);
            arguments.put(MARKER_GENERATE_ICON_FUNCTION_ARG, new ArrayList<>(iconDescriptors)); // set is not supported
            methodChannel.invokeMethod(MARKER_GENERATE_ICON_FUNCTION, arguments, new MethodChannel.Result() {
                @Override
                public void success(Object result) {
                    Log.d(TAG, MARKER_GENERATE_ICON_FUNCTION+":fetched!");
                    mapConsumer.accept((Map) result);
                }

                @Override
                public void error(String errorCode, String errorMessage, Object errorDetails) {
                    Log.e(TAG, MARKER_GENERATE_ICON_FUNCTION+":error " + errorMessage);
                    mapConsumer.accept(Collections.emptyMap());
                }

                @Override
                public void notImplemented() {
                    Log.e(TAG, MARKER_GENERATE_ICON_FUNCTION+" - not implemented");
                }
            });
        }
    }

    private Set<Object> getIconDescriptors(List<Object> markers) {
        Set<Object> descriptors = new HashSet<>();
        if (markers != null && !markers.isEmpty()) {
            int cacheHits = 0;
            for (Object marker : markers) {
                if (marker != null) {
                    Map data = (Map) marker;
                    final Object icon = data.get(ICON);
                    if (icon != null) {
                        final List<Object> args = (List<Object>) icon;
                        if (RESOLVABLE.equals(args.get(0))) {
                            Object desc = args.get(1);
                            BitmapDescriptor cached = bitmapCache.get(desc);
                            if (cached == null) {
                                descriptors.add(desc);
                            } else {
                                data.put(ICON, cached);
                                cacheHits++;
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "icons cache hits: "+cacheHits+", cache size: "+bitmapCache.size());
        }
        return descriptors;
    }

    private void applyIcons(List<Object> markers, Map<Object, Object> resolvedIcons) {
        if (markers != null && resolvedIcons != null) {
            for (Object marker : markers) {
                if (marker != null) {
                    Map data = (Map) marker;
                    Object icon = data.get(ICON);
                    if (icon instanceof List) {
                        final List<Object> args = (List<Object>) icon;
                        if (RESOLVABLE.equals(args.get(0))) {
                            Object descriptor = args.get(1);
                            icon = resolvedIcons.get(descriptor);
                            if (icon != null) {
                                BitmapDescriptor bitmapDescriptor = Convert.toBitmapDescriptor(icon);
                                bitmapCache.put(descriptor, bitmapDescriptor);
                                data.put(ICON, bitmapDescriptor);
                            } else {
                                data.put(ICON, null);
                            }
                        }
                    }
                }
            }
        }
    }

    void clearCache() {
        bitmapCache.evictAll();
    }
}
