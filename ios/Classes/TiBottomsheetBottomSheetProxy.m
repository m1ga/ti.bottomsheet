/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiBottomsheetBottomSheetProxy.h"
#import "TiApp.h"
#import "TiBase.h"
#import "TiBottomsheetBottomSheet.h"
#import "TiBottomsheetBottomSheetDialog.h"
#import "TiBottomsheetModule.h"
#import "TiBottomsheetSheetController.h"
#import "TiUtils.h"

static const CGFloat kTiBottomsheetDefaultPeakHeight = 32.0;

@interface TiBottomsheetBottomSheetProxy () <TiBottomsheetSheetControllerDelegate>
@end

@implementation TiBottomsheetBottomSheetProxy {
  BOOL useNativeSheet;
  UIViewController *controller;
  BOOL expandedRequested;
  BOOL tearingDown;
  CGSize lastContentSize;
  NSInteger currentState;
}

- (void)_initWithProperties:(NSDictionary *)properties
{
  // Same defaults as the Android proxy: pinned to the bottom, above other views.
  [self initializeProperty:@"bottom" defaultValue:@(0)];
  [self initializeProperty:@"zIndex" defaultValue:@(10000)];
  [self initializeProperty:@"peakHeight" defaultValue:@(kTiBottomsheetDefaultPeakHeight)];
  [self initializeProperty:@"nestedScrolling" defaultValue:@(NO)];
  [self initializeProperty:@"dragIndicator" defaultValue:@(NO)];
  [self initializeProperty:@"nativeSheet" defaultValue:@(YES)];

  useNativeSheet = [TiUtils boolValue:[properties objectForKey:@"nativeSheet"] def:YES];
  if (@available(iOS 15.0, *)) {
    // supported
  } else {
    useNativeSheet = NO;
  }
  currentState = TiBottomsheetStatePeak;

  [super _initWithProperties:properties];
}

- (NSString *)apiName
{
  return @"ti.bottomsheet.BottomSheet";
}

- (TiUIView *)newView
{
  if (useNativeSheet) {
    // Invisible, touch-transparent placeholder in the window; the children live in the sheet.
    return [[TiBottomsheetBottomSheetDialog alloc] init];
  }
  return [[TiBottomsheetBottomSheet alloc] init];
}

- (UIView *)parentViewForChild:(TiViewProxy *)child
{
  if (useNativeSheet) {
    if (@available(iOS 15.0, *)) {
      return [(TiBottomsheetSheetController *)controller contentArea];
    }
    return nil;
  }
  return [(TiBottomsheetBottomSheet *)[self view] contentView];
}

- (TiBottomsheetBottomSheet *)customSheet
{
  if (useNativeSheet || ![self viewAttached]) {
    return nil;
  }
  return (TiBottomsheetBottomSheet *)[self view];
}

#pragma mark - State events

- (void)updateState:(NSInteger)state
{
  if (state == currentState) {
    return;
  }
  currentState = state;

  if ([self _hasListeners:@"stateChanged"]) {
    [self fireEvent:@"stateChanged" withObject:@{ @"state" : @(state) }];
  }
  // Same as Android: expanded fires "open", collapsed fires "close".
  NSString *name = nil;
  if (state == TiBottomsheetStateOpen) {
    name = @"open";
  } else if (state == TiBottomsheetStatePeak) {
    name = @"close";
  }
  if (name != nil && [self _hasListeners:name]) {
    [self fireEvent:name withObject:@{}];
  }
}

#pragma mark - Native sheet

- (BOOL)hasValue:(id)value
{
  return value != nil && ![value isKindOfClass:[NSNull class]];
}

- (id)peakHeightValue
{
  id peak = [self valueForUndefinedKey:@"peakHeight"];
  return [self hasValue:peak] ? peak : @(kTiBottomsheetDefaultPeakHeight);
}

// peakHeight 0 has no detent equivalent: the sheet is simply not presented while collapsed.
- (BOOL)peakIsHidden
{
  return [TiUtils floatValue:[self peakHeightValue] def:kTiBottomsheetDefaultPeakHeight] <= 0;
}

- (NSString *)peakDetentIdentifier API_AVAILABLE(ios(15.0))
{
  if (@available(iOS 16.0, *)) {
    return TiBottomsheetPeakDetentIdentifier;
  }
  return UISheetPresentationControllerDetentIdentifierMedium;
}

- (UIColor *)sheetBackgroundColor
{
  id value = [self valueForUndefinedKey:@"backgroundColor"];
  if (![self hasValue:value]) {
    return nil;
  }
  return [[TiUtils colorValue:value] _color];
}

- (void)applyNativeSheetSettings:(TiBottomsheetSheetController *)sheetController API_AVAILABLE(ios(15.0))
{
  UISheetPresentationController *sheet = sheetController.sheetPresentationController;
  if ([self peakIsHidden]) {
    sheet.detents = @[ [UISheetPresentationControllerDetent largeDetent] ];
  } else {
    sheet.detents = [sheetController detentsWithPeakHeight:[self peakHeightValue] sizeToContent:NO];
  }
  // Non-modal: nothing is dimmed and the window underneath stays interactive.
  sheet.largestUndimmedDetentIdentifier = UISheetPresentationControllerDetentIdentifierLarge;
  BOOL grabber = [TiUtils boolValue:[self valueForUndefinedKey:@"dragIndicator"] def:NO];
  sheet.prefersGrabberVisible = grabber;
  // The grabber is drawn over the content, so push the content down a bit.
  sheetController.topInset = grabber ? TiBottomsheetGrabberInset : 0;
  sheet.prefersScrollingExpandsWhenScrolledToEdge = YES;
  sheetController.sheetBackgroundColor = [self sheetBackgroundColor];
}

- (void)presentNativeSheet
{
  if (controller != nil || ![self viewAttached]) {
    return;
  }
  if (@available(iOS 15.0, *)) {
    TiBottomsheetSheetController *sheetController = [[TiBottomsheetSheetController alloc] init];
    sheetController.layoutProxy = self;
    sheetController.sheetDelegate = self;
    sheetController.hostsProxyView = NO;
    sheetController.modalPresentationStyle = UIModalPresentationPageSheet;
    // The inline sheet can never be swiped away, like Android's non-hideable behavior.
    sheetController.modalInPresentation = YES;

    UISheetPresentationController *sheet = sheetController.sheetPresentationController;
    sheet.delegate = sheetController;
    [self applyNativeSheetSettings:sheetController];
    if (expandedRequested || [self peakIsHidden]) {
      sheet.selectedDetentIdentifier = UISheetPresentationControllerDetentIdentifierLarge;
    }

    controller = sheetController;
    lastContentSize = CGSizeZero;
    tearingDown = NO;
    [[TiApp app] showModalController:sheetController animated:YES];
  }
}

- (void)updateNativeSheet
{
  if (controller == nil) {
    return;
  }
  if (@available(iOS 15.0, *)) {
    TiBottomsheetSheetController *sheetController = (TiBottomsheetSheetController *)controller;
    UISheetPresentationController *sheet = sheetController.sheetPresentationController;
    [sheet animateChanges:^{
      [self applyNativeSheetSettings:sheetController];
    }];
    if ([self peakIsHidden] && !expandedRequested) {
      [self dismissNativeSheetSilently:NO];
    }
  }
}

- (void)dismissNativeSheetSilently:(BOOL)silently
{
  if (controller == nil) {
    return;
  }
  tearingDown = silently;
  [[TiApp app] hideModalController:controller animated:YES];
}

- (void)expandNative
{
  expandedRequested = YES;
  if (controller == nil) {
    [self presentNativeSheet];
    return;
  }
  if (@available(iOS 15.0, *)) {
    [(TiBottomsheetSheetController *)controller selectDetent:UISheetPresentationControllerDetentIdentifierLarge];
  }
  [self updateState:TiBottomsheetStateOpen];
}

- (void)collapseNative
{
  expandedRequested = NO;
  if (controller == nil) {
    return;
  }
  if ([self peakIsHidden]) {
    [self dismissNativeSheetSilently:NO];
    return;
  }
  if (@available(iOS 15.0, *)) {
    [(TiBottomsheetSheetController *)controller selectDetent:[self peakDetentIdentifier]];
  }
  [self updateState:TiBottomsheetStatePeak];
}

- (void)toggleNative
{
  BOOL expanded = NO;
  if (@available(iOS 15.0, *)) {
    expanded = controller != nil && [(TiBottomsheetSheetController *)controller isAtLargestDetent];
  }
  if (expanded) {
    [self collapseNative];
  } else {
    [self expandNative];
  }
}

#pragma mark - Window lifecycle (native mode)

- (void)windowDidOpen
{
  [super windowDidOpen];
  if (useNativeSheet && self.parent != nil) {
    // Let the window finish its own transition before presenting on top of it.
    dispatch_async(dispatch_get_main_queue(), ^{
      if (![self peakIsHidden] || self->expandedRequested) {
        [self presentNativeSheet];
      }
    });
  }
}

- (void)windowWillClose
{
  if (useNativeSheet) {
    [self dismissNativeSheetSilently:YES];
  }
  [super windowWillClose];
}

- (void)viewDidDetach
{
  if (useNativeSheet) {
    [self dismissNativeSheetSilently:YES];
  }
  [super viewDidDetach];
}

#pragma mark - TiBottomsheetSheetControllerDelegate

- (void)sheetControllerDidAppear:(TiBottomsheetSheetController *)sheetController
{
  [self updateState:[sheetController isAtLargestDetent] ? TiBottomsheetStateOpen : TiBottomsheetStatePeak];
}

- (void)sheetController:(TiBottomsheetSheetController *)sheetController didSelectDetent:(NSString *)identifier
{
  BOOL expanded = [sheetController isAtLargestDetent];
  expandedRequested = expanded;
  [self updateState:expanded ? TiBottomsheetStateOpen : TiBottomsheetStatePeak];
}

- (void)sheetController:(TiBottomsheetSheetController *)sheetController didDisappearCancelled:(BOOL)cancelled
{
  if (controller == nil) {
    return;
  }
  controller = nil;
  lastContentSize = CGSizeZero;
  if (!tearingDown) {
    // Collapsed with peakHeight 0: the sheet is gone but the proxy lives on.
    [self updateState:TiBottomsheetStatePeak];
  }
  tearingDown = NO;
}

- (void)sheetController:(TiBottomsheetSheetController *)sheetController didLayoutContentArea:(CGRect)bounds
{
  if (!CGSizeEqualToSize(bounds.size, lastContentSize)) {
    lastContentSize = bounds.size;
    // Size the invisible placeholder like the sheet content so the children
    // are measured against the same bounds they are positioned in.
    [self setValue:@(bounds.size.width) forKey:@"width"];
    [self setValue:@(bounds.size.height) forKey:@"height"];
  }
  // Children (re)attach to the content area and lay out inside it.
  [self layoutChildren:NO];
}

#pragma mark - Property setters shared by both modes

- (void)setPeakHeight:(id)value
{
  [self replaceValue:value forKey:@"peakHeight" notification:YES];
  if (useNativeSheet) {
    TiThreadPerformOnMainThread(^{
      [self updateNativeSheet];
    }, NO);
  }
}

- (void)setDragIndicator:(id)value
{
  [self replaceValue:value forKey:@"dragIndicator" notification:YES];
  if (useNativeSheet) {
    TiThreadPerformOnMainThread(^{
      [self updateNativeSheet];
    }, NO);
  }
}

- (void)setBackgroundColor:(id)value
{
  [self replaceValue:value forKey:@"backgroundColor" notification:YES];
  if (useNativeSheet) {
    TiThreadPerformOnMainThread(^{
      [self updateNativeSheet];
    }, NO);
  }
}

#pragma mark - Public API

- (void)show:(id)args
{
  TiThreadPerformOnMainThread(^{
    if (self->useNativeSheet) {
      [self expandNative];
    } else {
      [[self customSheet] expandAnimated:YES];
    }
  }, NO);
}

- (void)open:(id)args
{
  [self show:args];
}

- (void)hide:(id)args
{
  TiThreadPerformOnMainThread(^{
    if (self->useNativeSheet) {
      [self collapseNative];
    } else {
      [[self customSheet] collapseAnimated:YES];
    }
  }, NO);
}

- (void)close:(id)args
{
  [self hide:args];
}

- (void)toggle:(id)args
{
  TiThreadPerformOnMainThread(^{
    if (self->useNativeSheet) {
      [self toggleNative];
    } else {
      [[self customSheet] toggle];
    }
  }, NO);
}

@end
