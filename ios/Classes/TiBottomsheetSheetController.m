/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiBottomsheetSheetController.h"
#import "TiApp.h"
#import "TiDimension.h"
#import "TiUtils.h"

NSString *const TiBottomsheetPeakDetentIdentifier = @"ti.bottomsheet.peak";
NSString *const TiBottomsheetContentDetentIdentifier = @"ti.bottomsheet.content";
const CGFloat TiBottomsheetGrabberInset = 20.0;

// Measures the proxy's children the way TiViewProxy lays them out. Declared
// in TiViewProxy.m but not exported in its header.
@interface TiViewProxy (TiBottomsheetMeasure)
- (CGFloat)autoHeightForSize:(CGSize)size;
@end

@interface TiBottomsheetSheetController ()

@property (nonatomic, strong, readwrite) UIView *contentArea;
@property (nonatomic, assign) BOOL dismissing;
@property (nonatomic, assign) NSUInteger detentCount;
@property (nonatomic, assign) BOOL didLayoutContent;
@property (nonatomic, assign) BOOL usesContentDetent;
@property (nonatomic, assign) BOOL contentInvalidationPending;
/** Values from the last content detent resolution, used to skip needless invalidations. */
@property (nonatomic, assign) CGFloat lastContentDetentMaximum;
@property (nonatomic, assign) CGFloat lastContentDetentHeight;

@end

@implementation TiBottomsheetSheetController

#pragma mark - View lifecycle

- (void)loadView
{
  UIView *root = [[UIView alloc] initWithFrame:[UIScreen mainScreen].bounds];
  root.backgroundColor = self.sheetBackgroundColor ?: [UIColor systemBackgroundColor];

  self.contentArea = [[UIView alloc] initWithFrame:root.bounds];
  self.contentArea.clipsToBounds = YES;
  [root addSubview:self.contentArea];

  if (self.headerView != nil) {
    [root addSubview:self.headerView];
  }

  TiViewProxy *proxy = self.layoutProxy;
  if (self.hostsProxyView && proxy != nil) {
    [self.contentArea addSubview:[proxy view]];
  }

  self.view = root;
}

- (void)setTopInset:(CGFloat)topInset
{
  if (_topInset == topInset) {
    return;
  }
  _topInset = topInset;
  if (self.isViewLoaded) {
    [self.view setNeedsLayout];
  }
}

- (void)setSheetBackgroundColor:(UIColor *)sheetBackgroundColor
{
  _sheetBackgroundColor = sheetBackgroundColor;
  if (self.isViewLoaded) {
    self.view.backgroundColor = sheetBackgroundColor ?: [UIColor systemBackgroundColor];
  }
}

- (CGFloat)headerHeightForWidth:(CGFloat)width
{
  if (self.headerView == nil) {
    return 0;
  }
  CGSize size = [self.headerView systemLayoutSizeFittingSize:CGSizeMake(width, UILayoutFittingCompressedSize.height)
                                 withHorizontalFittingPriority:UILayoutPriorityRequired
                                       verticalFittingPriority:UILayoutPriorityFittingSizeLevel];
  return ceil(size.height);
}

- (void)viewDidLayoutSubviews
{
  [super viewDidLayoutSubviews];

  CGRect bounds = self.view.bounds;
  CGFloat headerHeight = [self headerHeightForWidth:bounds.size.width];
  self.headerView.frame = CGRectMake(0, self.topInset, bounds.size.width, headerHeight);

  CGFloat bottomInset = self.view.safeAreaInsets.bottom;
  CGFloat contentTop = self.topInset + headerHeight;
  CGRect contentFrame = CGRectMake(0, contentTop, bounds.size.width,
      MAX(0, bounds.size.height - contentTop - bottomInset));
  BOOL changed = !CGRectEqualToRect(self.contentArea.frame, contentFrame);
  self.contentArea.frame = contentFrame;

  TiViewProxy *proxy = self.layoutProxy;
  if (proxy == nil) {
    return;
  }

  if (self.hostsProxyView) {
    if (!CGRectEqualToRect([proxy sandboxBounds], self.contentArea.bounds)) {
      [proxy setSandboxBounds:self.contentArea.bounds];
      [proxy relayout];
      [proxy layoutChildren:NO];
    }
  } else if (changed || !self.didLayoutContent) {
    self.didLayoutContent = YES;
    [self.sheetDelegate sheetController:self didLayoutContentArea:self.contentArea.bounds];
  }
}

- (void)viewWillAppear:(BOOL)animated
{
  [super viewWillAppear:animated];
  // A cancelled interactive dismissal brings the sheet back.
  self.dismissing = NO;
}

- (void)viewDidAppear:(BOOL)animated
{
  [super viewDidAppear:animated];
  [self.sheetDelegate sheetControllerDidAppear:self];
}

- (void)viewWillDisappear:(BOOL)animated
{
  [super viewWillDisappear:animated];
  if (self.isBeingDismissed) {
    self.dismissing = YES;
  }
}

- (void)viewDidDisappear:(BOOL)animated
{
  [super viewDidDisappear:animated];
  if (self.dismissing || self.presentingViewController == nil) {
    [self.sheetDelegate sheetController:self didDisappearCancelled:self.dismissedByUser];
  }
}

#pragma mark - Detents

- (CGFloat)bottomSafeInset
{
  UIView *rootView = [[[TiApp app] controller] view];
  return rootView != nil ? rootView.safeAreaInsets.bottom : 0;
}

- (CGFloat)contentWidth
{
  CGFloat width = self.isViewLoaded ? self.view.bounds.size.width : 0;
  return width > 0 ? width : [UIScreen mainScreen].bounds.size.width;
}

/**
 * Height the sheet needs to show the header rows and the proxy's children
 * without scrolling: top inset + header + measured content + bottom safe area.
 * Children that fill their parent measure as the full available height, so
 * the result is capped at the maximum detent value.
 */
- (CGFloat)contentDetentHeightForMaximum:(CGFloat)maximum bottomInset:(CGFloat)bottomInset
{
  CGFloat width = [self contentWidth];
  CGFloat top = self.topInset + [self headerHeightForWidth:width];
  CGFloat available = MAX(0, maximum - top - bottomInset);

  CGFloat content = 0;
  TiViewProxy *proxy = self.layoutProxy;
  if (proxy != nil && [[proxy children] count] > 0) {
    content = ceil([proxy autoHeightForSize:CGSizeMake(width, available)]);
  }

  return MIN(maximum, MAX(1, top + content + bottomInset));
}

- (NSArray<UISheetPresentationControllerDetent *> *)detentsWithPeakHeight:(id)peakHeight sizeToContent:(BOOL)sizeToContent
{
  NSMutableArray<UISheetPresentationControllerDetent *> *detents = [NSMutableArray array];
  BOOL hasPeak = peakHeight != nil && ![peakHeight isKindOfClass:[NSNull class]];
  self.usesContentDetent = NO;

  if (hasPeak) {
    TiDimension peakDimension = TiDimensionFromObject(peakHeight);
    if (@available(iOS 16.0, *)) {
      [detents addObject:[UISheetPresentationControllerDetent customDetentWithIdentifier:TiBottomsheetPeakDetentIdentifier
                                                                                 resolver:^CGFloat(id<UISheetPresentationControllerDetentResolutionContext> context) {
                                                                                   return MAX(1, TiDimensionCalculateValue(peakDimension, context.maximumDetentValue));
                                                                                 }]];
    } else {
      [detents addObject:[UISheetPresentationControllerDetent mediumDetent]];
    }
    [detents addObject:[UISheetPresentationControllerDetent largeDetent]];
  } else if (sizeToContent) {
    if (@available(iOS 16.0, *)) {
      self.usesContentDetent = YES;
      __weak TiBottomsheetSheetController *weakSelf = self;
      CGFloat bottomInset = [self bottomSafeInset];
      [detents addObject:[UISheetPresentationControllerDetent customDetentWithIdentifier:TiBottomsheetContentDetentIdentifier
                                                                                 resolver:^CGFloat(id<UISheetPresentationControllerDetentResolutionContext> context) {
                                                                                   TiBottomsheetSheetController *strongSelf = weakSelf;
                                                                                   if (strongSelf == nil) {
                                                                                     return 1;
                                                                                   }
                                                                                   CGFloat height = [strongSelf contentDetentHeightForMaximum:context.maximumDetentValue bottomInset:bottomInset];
                                                                                   strongSelf.lastContentDetentMaximum = context.maximumDetentValue;
                                                                                   strongSelf.lastContentDetentHeight = height;
                                                                                   return height;
                                                                                 }]];
    } else {
      // iOS 15 has no custom detents: header-only sheets fit into medium,
      // sheets with children can at least be dragged to full height.
      [detents addObject:[UISheetPresentationControllerDetent mediumDetent]];
      if ([[self.layoutProxy children] count] > 0) {
        [detents addObject:[UISheetPresentationControllerDetent largeDetent]];
      }
    }
  } else {
    [detents addObject:[UISheetPresentationControllerDetent mediumDetent]];
    [detents addObject:[UISheetPresentationControllerDetent largeDetent]];
  }

  self.detentCount = detents.count;
  return detents;
}

- (void)contentSizeDidChange
{
  if (!self.usesContentDetent || self.contentInvalidationPending || self.dismissing) {
    return;
  }
  self.contentInvalidationPending = YES;
  // Coalesce: add/remove/resizes usually arrive in bursts, and the proxies
  // need their new state applied before they can be measured.
  __weak TiBottomsheetSheetController *weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    TiBottomsheetSheetController *strongSelf = weakSelf;
    if (strongSelf == nil) {
      return;
    }
    strongSelf.contentInvalidationPending = NO;
    UISheetPresentationController *sheet = strongSelf.sheetPresentationController;
    if (sheet == nil || strongSelf.dismissing) {
      return;
    }
    if (@available(iOS 16.0, *)) {
      if (strongSelf.lastContentDetentMaximum > 0) {
        CGFloat height = [strongSelf contentDetentHeightForMaximum:strongSelf.lastContentDetentMaximum
                                                       bottomInset:[strongSelf bottomSafeInset]];
        if (fabs(height - strongSelf.lastContentDetentHeight) < 0.5) {
          return;
        }
      }
      [sheet animateChanges:^{
        [sheet invalidateDetents];
      }];
    }
  });
}

- (NSString *)identifierForLastDetent:(NSArray<UISheetPresentationControllerDetent *> *)detents
{
  if (@available(iOS 16.0, *)) {
    return detents.lastObject.identifier;
  }
  return detents.count > 1 ? UISheetPresentationControllerDetentIdentifierLarge : UISheetPresentationControllerDetentIdentifierMedium;
}

- (void)selectDetent:(NSString *)identifier
{
  UISheetPresentationController *sheet = self.sheetPresentationController;
  if (sheet == nil || identifier == nil) {
    return;
  }
  [sheet animateChanges:^{
    sheet.selectedDetentIdentifier = identifier;
  }];
}

- (BOOL)isAtLargestDetent
{
  UISheetPresentationController *sheet = self.sheetPresentationController;
  NSString *selected = sheet.selectedDetentIdentifier;
  if (selected == nil) {
    // nil means the first detent is selected.
    return self.detentCount <= 1;
  }
  return [selected isEqualToString:[self identifierForLastDetent:sheet.detents]];
}

#pragma mark - UISheetPresentationControllerDelegate

- (void)presentationControllerWillDismiss:(UIPresentationController *)presentationController
{
  // Swipe down or tap outside. Programmatic dismissal resets this flag first.
  self.dismissedByUser = YES;
}

- (void)sheetPresentationControllerDidChangeSelectedDetentIdentifier:(UISheetPresentationController *)sheetPresentationController
{
  [self.sheetDelegate sheetController:self didSelectDetent:sheetPresentationController.selectedDetentIdentifier];
}

@end
