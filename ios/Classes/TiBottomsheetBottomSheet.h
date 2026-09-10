/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiUIView.h"

/**
 * Inline bottom sheet: a transparent full-size overlay that hosts a draggable
 * sheet pinned to the bottom edge. Touches outside the sheet fall through.
 */
@interface TiBottomsheetBottomSheet : TiUIView <UIGestureRecognizerDelegate>

/** The view Titanium children are added to (inside the sheet, below the drag handle). */
- (UIView *)contentView;

- (void)expandAnimated:(BOOL)animated;
- (void)collapseAnimated:(BOOL)animated;
- (void)toggle;
- (BOOL)isExpanded;

@end
