/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiViewProxy.h"

/**
 * Inline bottom sheet added to a window.
 *
 * Two implementations, selected with the creation-only `nativeSheet` property:
 *  - nativeSheet: true (default, iOS 15+/16 for custom peek heights): a non-modal
 *    UISheetPresentationController sheet that is presented as soon as the window
 *    opens. The proxy's own view stays in the window as an invisible placeholder
 *    and the children are hosted inside the sheet.
 *  - nativeSheet: false: the custom draggable overlay view (TiBottomsheetBottomSheet).
 */
@interface TiBottomsheetBottomSheetProxy : TiViewProxy {
}

- (void)show:(id)args;
- (void)open:(id)args;
- (void)hide:(id)args;
- (void)close:(id)args;
- (void)toggle:(id)args;

@end
