/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiBottomsheetModule.h"
#import "TiBase.h"
#import "TiHost.h"
#import "TiUtils.h"

@implementation TiBottomsheetModule

#pragma mark Internal

// This is generated for your module, please do not change it
- (id)moduleGUID
{
  return @"408496a9-b140-4ba0-a223-80d91b001fae";
}

// This is generated for your module, please do not change it
- (NSString *)moduleId
{
  return @"ti.bottomsheet";
}

#pragma mark Lifecycle

- (void)startup
{
  [super startup];
}

#pragma mark Constants

MAKE_SYSTEM_PROP(STATE_DRAGGING, TiBottomsheetStateDragging);
MAKE_SYSTEM_PROP(STATE_SLIDING, TiBottomsheetStateSliding);
MAKE_SYSTEM_PROP(STATE_OPEN, TiBottomsheetStateOpen);
MAKE_SYSTEM_PROP(STATE_PEAK, TiBottomsheetStatePeak);
MAKE_SYSTEM_PROP(STATE_CLOSE, TiBottomsheetStateClose);

@end
