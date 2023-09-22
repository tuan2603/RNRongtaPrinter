
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNRNRongtaPrinterSpec.h"

@interface RNRongtaPrinter : NSObject <NativeRNRongtaPrinterSpec>
#else
#import <React/RCTBridgeModule.h>

@interface RNRongtaPrinter : NSObject <RCTBridgeModule>
#endif

@end
