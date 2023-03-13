#ifdef RCT_NEW_ARCH_ENABLED
#import <RNStepCounterSpec/RNStepCounterSpec.h>
NS_ASSUME_NONNULL_BEGIN

@interface RNStepCounter : NSObject <NativeStepCounterSpec>
#else
#import <Foundation/Foundation.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import "SOMotionDetecter.h"

@interface RNStepCounter : RCTEventEmitter<RCTBridgeModule>
#endif

@end
NS_ASSUME_NONNULL_END