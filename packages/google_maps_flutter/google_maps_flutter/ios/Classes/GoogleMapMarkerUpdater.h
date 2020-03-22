//
//  GoogleMapMarkerUpdater.h
//  e2e
//
//  Created by kris on 19/03/2020.
//

#import <Foundation/Foundation.h>
#import <Flutter/Flutter.h>
#import "GoogleMapMarkerController.h"
#import "GoogleMapMarkerUpdater.h"

NS_ASSUME_NONNULL_BEGIN

@protocol FLTBitmapCache
    - (void) evictAll;
    - (id _Nullable) get:(id) key;
    - (void) put:(id) key value:(id) value;
@end

@interface FLTLruBitmapCache : NSObject<FLTBitmapCache>

- (instancetype)initWithSize: (NSInteger) size;

@end

@interface FLTMarkersUpdater : NSObject

- (instancetype)init:(FlutterMethodChannel*)methodChannel
                markersController:(FLTMarkersController*) markerController
                registrar:(NSObject<FlutterPluginRegistrar>*)registrar
                cache:(NSObject<FLTBitmapCache>*) cache;

- (void) addMarkers:(NSArray*)initialMarkers;

- (void) markersUpdate:(NSArray* _Nullable) markersToAdd
         markersToChange:(NSArray* _Nullable) markersToChange
         markerIdsToRemove:(NSArray* _Nullable) markerIdsToRemove
         methodResult:(FlutterResult _Nullable) result;

- (void) clearCache;

@end


NS_ASSUME_NONNULL_END
