/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiBottomsheetBottomSheetDialog.h"

@implementation TiBottomsheetBottomSheetDialog

- (void)initializeState
{
  [super initializeState];
  self.backgroundColor = [UIColor clearColor];
}

// The sheet background is applied by the presenting controller. When this view
// sits in a window as a placeholder (Alloy nests the dialog in the window), it
// has to stay invisible.
- (void)setBackgroundColor_:(id)color
{
  self.backgroundColor = [UIColor clearColor];
}

- (void)setBackgroundImage_:(id)image
{
}

// The container itself never swallows touches; only its children do. This also
// keeps it harmless if someone adds the dialog to a window like on Android.
- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event
{
  UIView *hit = [super hitTest:point withEvent:event];
  if (hit == self) {
    return nil;
  }
  return hit;
}

@end
