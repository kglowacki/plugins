package io.flutter.plugins.googlemaps;

import android.util.Log;
import android.util.LruCache;

import androidx.core.util.Consumer;

import com.google.android.gms.maps.model.BitmapDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

class Mod {

    private static final String ICON = "icon";
    private static final String GENERATED_TYPE = "generated";
    private static LruCache<Object, BitmapDescriptor> BITMAP_DESCRIPTOR_CACHE = new LruCache<Object, BitmapDescriptor>(1024) {
//        protected int sizeOf(String key, Bitmap value) {
//            return value.getByteCount();
//        }
    };

    private static final String TAG = "GoogleMapControllerMod";
    private static final String MARKER_GENERATE_ICON_FUNCTION = "marker#generateIcons";

    static void markersUpdate(MethodChannel methodChannel, MethodCall call, final MethodChannel.Result result, final MarkersController markersController) {
        final List<Object> markersToAdd = call.argument("markersToAdd");
        final List<Object> markersToChange = call.argument("markersToChange");
        final List<Object> markerIdsToRemove = call.argument("markerIdsToRemove");

        Log.d(TAG, "markertsToAdd="+(markersToAdd==null?0:markersToAdd.size())+", markersToChange="+(markersToChange==null?0:markersToChange.size()));

        Set<Object> iconDescriptors = new HashSet<>();
        iconDescriptors.addAll(getIconDescriptors(markersToAdd));
        iconDescriptors.addAll(getIconDescriptors(markersToChange));

        fetchIcons(methodChannel, iconDescriptors, new Consumer<Map<Object, Object>>(){
            @Override
            public void accept(Map<Object, Object> resolvedIcons) {
                applyIcons(markersToAdd, resolvedIcons);
                applyIcons(markersToChange, resolvedIcons);

                markersController.addMarkers(markersToAdd);
                markersController.changeMarkers(markersToChange);
                markersController.removeMarkers(markerIdsToRemove);
                result.success(null);
            }
        });
    }

    private static void fetchIcons(MethodChannel methodChannel, Set<Object> iconDescriptors, final Consumer<Map<Object, Object>> mapConsumer) {
        if (iconDescriptors.isEmpty()) {
            mapConsumer.accept(Collections.emptyMap());
        } else {
            Log.d(TAG, "fetch "+iconDescriptors.size()+" icons");
            final Map<String, Object> arguments = new HashMap<>(2);
            arguments.put("descriptors", new ArrayList<>(iconDescriptors)); // set is not supported
            methodChannel.invokeMethod(MARKER_GENERATE_ICON_FUNCTION, arguments, new MethodChannel.Result() {
                @Override
                public void success(Object result) {
                    Log.d(TAG, "marker#onGenerateIcon:fetched!");
                    mapConsumer.accept((Map) result);
                }

                @Override
                public void error(String errorCode, String errorMessage, Object errorDetails) {
                    Log.e(TAG, "marker#onGenerateIcon:error " + errorMessage);
                    mapConsumer.accept(Collections.emptyMap());
                }

                @Override
                public void notImplemented() {
                    Log.e(TAG, "marker#onGenerateIcon - not implemented");
                }
            });
        }
    }

    private static Set<Object> getIconDescriptors(List<Object> markers) {
        Set<Object> descriptors = new HashSet<>();
        if (markers != null) {
            int cacheHits = 0;
            for (Object marker : markers) {
                if (marker != null) {
                    Map data = (Map) marker;
                    final Object icon = data.get(ICON);
                    if (icon != null) {
                        final List<Object> args = (List<Object>) icon;
                        if (GENERATED_TYPE.equals(args.get(0))) {
                            Object desc = args.get(1);
                            Object cached = BITMAP_DESCRIPTOR_CACHE.get(desc);
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
            Log.d(TAG, "icons cache hits: "+cacheHits+", cache size: "+BITMAP_DESCRIPTOR_CACHE.size());
        }
        return descriptors;
    }

    private static Set<Object> applyIcons(List<Object> markers, Map<Object, Object> resolvedIcons) {
        Set<Object> descriptors = new HashSet<>();
        if (markers != null && resolvedIcons != null) {
            for (Object marker : markers) {
                if (marker != null) {
                    Map data = (Map) marker;
                    Object icon = data.get(ICON);
                    if (icon instanceof List) {
                        final List<Object> args = (List<Object>) icon;
                        if (GENERATED_TYPE.equals(args.get(0))) {
                            Object descriptor = args.get(1);
                            icon = resolvedIcons.get(descriptor);
                            if (icon != null) {
                                BitmapDescriptor bitmapDescriptor = Convert.toBitmapDescriptor(icon);
                                BITMAP_DESCRIPTOR_CACHE.put(descriptor, bitmapDescriptor);
                                data.put(ICON, bitmapDescriptor);
                            } else {
                                data.put(ICON, null);
                            }
                        }
                    }
                }
            }
        }
        return descriptors;
    }

    public static void clearCache() {
        BITMAP_DESCRIPTOR_CACHE.evictAll();
    }
}
