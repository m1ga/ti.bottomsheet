/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiViewProxy.h"
#import <UIKit/UIKit.h>

extern NSString *const TiBottomsheetPeakDetentIdentifier;
extern NSString *const TiBottomsheetContentDetentIdentifier;

/** Vertical space reserved for the system grabber when dragIndicator is enabled. */
extern const CGFloat TiBottomsheetGrabberInset;

@class TiBottomsheetSheetController;

@protocol TiBottomsheetSheetControllerDelegate <NSObject>

- (void)sheetControllerDidAppear:(TiBottomsheetSheetController *)controller;
- (void)sheetController:(TiBottomsheetSheetController *)controller didDisappearCancelled:(BOOL)cancelled;
- (void)sheetController:(TiBottomsheetSheetController *)controller didSelectDetent:(NSString *)identifier;
- (void)sheetController:(TiBottomsheetSheetController *)controller didLayoutContentArea:(CGRect)bounds;

@end

/**
 * View controller presented through UISheetPresentationController. It renders
 * optional header rows (title/options) above a content area that hosts a
 * Titanium view proxy and drives that proxy's layout the way TiViewController does.
 *
 * Two hosting modes:
 *  - hostsProxyView = YES: the proxy's own view is placed in the content area (dialog).
 *  - hostsProxyView = NO: only the proxy's children are placed there, the proxy's
 *    view stays wherever Titanium put it (inline sheet added to a window).
 */
API_AVAILABLE(ios(15.0))
@interface TiBottomsheetSheetController : UIViewController <UISheetPresentationControllerDelegate>

@property (nonatomic, weak) TiViewProxy *layoutProxy;
@property (nonatomic, weak) id<TiBottomsheetSheetControllerDelegate> sheetDelegate;
@property (nonatomic, strong) UIStackView *headerView;
@property (nonatomic, strong, readonly) UIView *contentArea;
@property (nonatomic, strong) UIColor *sheetBackgroundColor;
/** Extra space above the header/content, e.g. to keep the grabber clear of the content. */
@property (nonatomic, assign) CGFloat topInset;
@property (nonatomic, assign) BOOL hostsProxyView;
@property (nonatomic, assign) BOOL dismissedByUser;

- (CGFloat)headerHeightForWidth:(CGFloat)width;

/**
 * Builds the detent list: a peek detent (custom on iOS 16, medium on iOS 15)
 * plus large when a peak height is given; otherwise a single detent sized to
 * the header rows and the layout proxy's children when sizeToContent is set
 * (iOS 16; medium, plus large with children, on iOS 15); medium + large otherwise.
 */
- (NSArray<UISheetPresentationControllerDetent *> *)detentsWithPeakHeight:(id)peakHeight sizeToContent:(BOOL)sizeToContent;

/**
 * Re-measures the content-sized detent and resizes the sheet when its height
 * changed, e.g. after children were added, removed or resized. No-op for
 * other detent setups.
 */
- (void)contentSizeDidChange;

/** Identifier of the last detent (works on iOS 15 where detents have no identifier property). */
- (NSString *)identifierForLastDetent:(NSArray<UISheetPresentationControllerDetent *> *)detents;

/** Animates to the given detent identifier. */
- (void)selectDetent:(NSString *)identifier;

/** YES when the selected detent is the last (largest) one. */
- (BOOL)isAtLargestDetent;

@end
