/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiViewProxy.h"

/**
 * Modal bottom sheet backed by UISheetPresentationController (iOS 15+).
 * Same JS API as the Android BottomSheetDialog.
 */
@interface TiBottomsheetBottomSheetDialogProxy : TiViewProxy {
}

- (void)show:(id)args;
- (void)open:(id)args;
- (void)hide:(id)args;
- (void)close:(id)args;

@end
