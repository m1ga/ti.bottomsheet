/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiModule.h"

// Shared with the Android module so `e.state` carries the same values on both platforms.
typedef NS_ENUM(NSInteger, TiBottomsheetState) {
  TiBottomsheetStateDragging = 1,
  TiBottomsheetStateSliding = 2,
  TiBottomsheetStateOpen = 3,
  TiBottomsheetStatePeak = 4,
  TiBottomsheetStateClose = 5,
};

@interface TiBottomsheetModule : TiModule {
}

@end
