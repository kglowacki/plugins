//
//  GoogleMapMarkerUpdater.m
//  e2e
//
//  Created by kris on 19/03/2020.
//

#import "GoogleMapMarkerUpdater.h"
#import <LruCache/LruCache.h>

extern  UIImage* ExtractIcon(NSObject<FlutterPluginRegistrar>* registrar, NSArray* icon);

static NSString * const ICON = @"icon";
static NSString * const RESOLVABLE_BITMAP = @"resolvable";
static NSString * const RESOLVE_BITMAPS_CALLBACK = @"marker#onResolveBitmaps";
static NSString * const RESOLVE_BITMAPS_CALLBACK_ARG = @"keys";

@implementation FLTMarkersUpdater {
    FLTMarkersController* _markersController;
    FlutterMethodChannel* _methodChannel;
    NSObject<FlutterPluginRegistrar>* _registrar;
    NSObject<FLTBitmapCache>* _bitmapCache;
}

- (instancetype) init:(FlutterMethodChannel*)methodChannel
            markersController:(FLTMarkersController*) markersController
            registrar:(NSObject<FlutterPluginRegistrar>*) registrar
                cache:(NSObject<FLTBitmapCache>*) cache {
    if (self = [super init]) {
        _markersController = markersController;
        _methodChannel = methodChannel;
        _registrar = registrar;
        _bitmapCache = cache;
    }
    return self;
}

- (void) addMarkers:(NSArray*)initialMarkers {
    [self markersUpdate:initialMarkers markersToChange:nil markerIdsToRemove:nil methodResult:nil];
}

/**
 Set<Object> iconDescriptors = new HashSet<>();
        iconDescriptors.addAll(getIconDescriptors(markersToAdd));
        iconDescriptors.addAll(getIconDescriptors(markersToChange));

        fetchIcons(iconDescriptors, new Consumer<Map<Object, Object>>(){
            @Override
            public void accept(Map<Object, Object> resolvedIcons) {
                applyIcons(markersToAdd, resolvedIcons);
                applyIcons(markersToChange, resolvedIcons);

                markersController.addMarkers(markersToAdd);
                markersController.changeMarkers(markersToChange);
                markersController.removeMarkers(markerIdsToRemove);
                if (result != null) result.success(null);
            }
        });
 */
- (void) markersUpdate:(NSArray* _Nullable)markersToAdd
         markersToChange:(NSArray* _Nullable)markersToChange
         markerIdsToRemove:(NSArray* _Nullable) markerIdsToRemove
         methodResult:(FlutterResult _Nullable) result {
 
    NSSet* iconKeys = [[NSSet alloc] init];
    iconKeys = [iconKeys setByAddingObjectsFromSet:[self getIconDescriptors:markersToAdd]];
    iconKeys = [iconKeys setByAddingObjectsFromSet:[self getIconDescriptors:markersToChange]];
    NSLog(@"resolvable icons = %lu", [iconKeys count]);
    [self fetchIcons:iconKeys consumer:^(NSDictionary * resolvedIcons) {
        /**
         applyIcons(markersToAdd, resolvedIcons);
                        applyIcons(markersToChange, resolvedIcons);

                        markersController.addMarkers(markersToAdd);
                        markersController.changeMarkers(markersToChange);
                        markersController.removeMarkers(markerIdsToRemove);
                        if (result != null) result.success(null);
         */
        NSLog(@"resolved icons = %lu", [resolvedIcons count]);
        [self applyIcons:markersToAdd resolvedIcons:resolvedIcons];
        [self applyIcons:markersToChange resolvedIcons:resolvedIcons];
        [self->_markersController addMarkers:markersToAdd];
        [self->_markersController changeMarkers:markersToChange];
        [self->_markersController removeMarkerIds:markerIdsToRemove];
        NSLog(@"markers updated (added: %lu, changed: %lu, removed: %lu)",
              [markersToAdd count], [markersToChange count], [markerIdsToRemove count]);
        if (result != nil) result(nil);
    }];
}

- (void) clearCache {
    [_bitmapCache evictAll];
}

/**
 private Set<Object> getIconDescriptors(List<Object> markers) {
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
 */
- (NSSet*) getIconDescriptors:(NSArray*) markers {
    NSMutableSet* descriptors = [[NSMutableSet alloc] init];
    if (markers) {
        for (NSMutableDictionary* marker in markers) {
            if (marker) {
                NSArray* icon = [marker objectForKey:ICON];
                if (icon) {
                    id arg0 = [icon objectAtIndex:0];
                    if ([arg0 isKindOfClass:[NSString class]] && [arg0 isEqualToString:RESOLVABLE_BITMAP]) {
                        id descriptor = [icon objectAtIndex:1];
                        NSArray* bitmapDescriptor = [_bitmapCache get:descriptor];
                        if (bitmapDescriptor) {
                            [marker setObject:bitmapDescriptor forKey:ICON];
                        } else {
                            [descriptors addObject:descriptor];
                        }
                    };
                }
            }
        }
    }
    return descriptors;
}

/**
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
 */
- (void) fetchIcons:(NSSet*) iconDescriptors
            consumer:(void (^)(NSDictionary*)) mapConsumer {

    if ([iconDescriptors count] == 0) {
        mapConsumer([[NSDictionary alloc] init]);
    } else {
        NSDictionary* arguments = [[NSDictionary alloc] initWithObjectsAndKeys:[NSMutableArray arrayWithArray:[iconDescriptors allObjects]], RESOLVE_BITMAPS_CALLBACK_ARG, nil];
        [self->_methodChannel invokeMethod:RESOLVE_BITMAPS_CALLBACK
                              arguments:arguments
                              result:^(id  _Nullable result) {
            if ([result isKindOfClass:[FlutterError class]]) {
                mapConsumer([[NSDictionary alloc] init]);
            } else if ([result isKindOfClass:[NSDictionary class]]) {
                mapConsumer(result);
            }
        }];
    }
}


/**
 private Set<Object> applyIcons(List<Object> markers, Map<Object, Object> resolvedIcons) {
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
 */
- (void) applyIcons:(NSArray*) markers resolvedIcons:(NSDictionary*) resolvedIcons {
    if (markers && resolvedIcons) {
        for (NSMutableDictionary* marker in markers) {
            if (marker) {
                id icon = [marker objectForKey:ICON];
                if ([icon isKindOfClass:[NSArray class]]) {
                    if ([[icon objectAtIndex:0] isEqual:RESOLVABLE_BITMAP]) {
                        id descriptor = [icon objectAtIndex:1];
                        id bitmapDescriptor = [resolvedIcons objectForKey: descriptor];
                        if (bitmapDescriptor) {
                            // we want to transform bitmapdescriptor to proper image here and cache it
                            // it is more efficient than storing bitmapdescriptor in cache and transforming it into UIImage
                            // for each marker that is using it
                            UIImage* image = ExtractIcon(_registrar, bitmapDescriptor);
                            bitmapDescriptor = [NSArray arrayWithObject:image];
                            [_bitmapCache put:descriptor value:bitmapDescriptor];
                            [marker setObject:bitmapDescriptor forKey:ICON];
                        }
                    }
                }
            }
        }
    }
}

@end

/**
 this implementation is using LruCache - headers suggest NSString keys only,
 but it works with any key type;
 */
@implementation FLTLruBitmapCache {
    LruCache* _cache;
}

- (instancetype) initWithSize: (NSInteger) size {
    if (self = [super init]) {
        _cache = [[LruCache alloc] initWithMaxSize:size];
    }
    return self;
}

- (void)evictAll {
    [_cache evictAll];
}

- (id)get:(nonnull id)key {
    return [_cache get:key];
}

- (void)put:(nonnull id)key value:(nonnull id)value {
    [_cache put:key value:value];
}

@end
