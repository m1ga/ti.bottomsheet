/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiBottomsheetBottomSheetDialogProxy.h"
#import "TiApp.h"
#import "TiBase.h"
#import "TiBottomsheetModule.h"
#import "TiBottomsheetSheetController.h"
#import "TiUtils.h"

static const CGFloat kTiBottomsheetRowPadding = 15.0;
static const CGFloat kTiBottomsheetRowFontSize = 18.0;

@interface TiBottomsheetBottomSheetDialogProxy () <TiBottomsheetSheetControllerDelegate>
@end

@implementation TiBottomsheetBottomSheetDialogProxy {
  UIViewController *controller;
  BOOL managesWindowLifecycle;
  CGSize lastContentSize;
}

- (void)_initWithProperties:(NSDictionary *)properties
{
  [self initializeProperty:@"cancelable" defaultValue:@(NO)];
  [self initializeProperty:@"dragIndicator" defaultValue:@(NO)];
  [super _initWithProperties:properties];
}

- (NSString *)apiName
{
  return @"ti.bottomsheet.BottomSheetDialog";
}

// Children are only ever placed inside the presented sheet. While the dialog is
// not shown they have no parent view, so nesting the dialog in a window (which
// Alloy does for nested XML) renders nothing.
- (UIView *)parentViewForChild:(TiViewProxy *)child
{
  if (@available(iOS 15.0, *)) {
    return [(TiBottomsheetSheetController *)controller contentArea];
  }
  return nil;
}

#pragma mark - Helpers

- (BOOL)hasValue:(id)value
{
  return value != nil && ![value isKindOfClass:[NSNull class]];
}

- (UIColor *)colorFromValue:(id)value fallback:(UIColor *)fallback
{
  if (![self hasValue:value]) {
    return fallback;
  }
  NSString *string = [TiUtils stringValue:value];
  if (string.length == 0) {
    return fallback;
  }
  UIColor *color = [[TiUtils colorValue:string] _color];
  return color ?: fallback;
}

#pragma mark - Header rows

- (UIView *)titleRowWithText:(NSString *)text
{
  UILabel *label = [[UILabel alloc] init];
  label.text = text;
  label.font = [UIFont systemFontOfSize:kTiBottomsheetRowFontSize];
  label.numberOfLines = 0;
  label.textColor = [self colorFromValue:[self valueForUndefinedKey:@"titleColor"] fallback:[UIColor labelColor]];

  UIStackView *row = [[UIStackView alloc] initWithArrangedSubviews:@[ label ]];
  row.axis = UILayoutConstraintAxisVertical;
  row.layoutMarginsRelativeArrangement = YES;
  row.layoutMargins = UIEdgeInsetsMake(kTiBottomsheetRowPadding, kTiBottomsheetRowPadding,
      kTiBottomsheetRowPadding, kTiBottomsheetRowPadding);
  return row;
}

- (UIButton *)optionRowWithTitle:(NSString *)title color:(UIColor *)color image:(UIImage *)image index:(NSInteger)index API_AVAILABLE(ios(15.0))
{
  UIButtonConfiguration *config = [UIButtonConfiguration plainButtonConfiguration];
  config.attributedTitle = [[NSAttributedString alloc] initWithString:title ?: @""
                                                           attributes:@{
                                                             NSFontAttributeName : [UIFont systemFontOfSize:kTiBottomsheetRowFontSize],
                                                             NSForegroundColorAttributeName : color
                                                           }];
  config.baseForegroundColor = color;
  config.image = image;
  config.imagePadding = kTiBottomsheetRowPadding;
  config.imagePlacement = NSDirectionalRectEdgeLeading;
  config.contentInsets = NSDirectionalEdgeInsetsMake(kTiBottomsheetRowPadding, kTiBottomsheetRowPadding,
      kTiBottomsheetRowPadding, kTiBottomsheetRowPadding);
  config.titleLineBreakMode = NSLineBreakByTruncatingTail;

  UIButton *button = [UIButton buttonWithConfiguration:config primaryAction:nil];
  button.contentHorizontalAlignment = UIControlContentHorizontalAlignmentLeading;
  button.tag = index;
  [button addTarget:self action:@selector(optionTapped:) forControlEvents:UIControlEventTouchUpInside];
  return button;
}

- (UIStackView *)buildHeader API_AVAILABLE(ios(15.0))
{
  NSString *title = [TiUtils stringValue:[self valueForUndefinedKey:@"title"]];
  id options = [self valueForUndefinedKey:@"options"];
  BOOL hasOptions = [options isKindOfClass:[NSArray class]] && [(NSArray *)options count] > 0;
  if (title.length == 0 && !hasOptions) {
    return nil;
  }

  UIStackView *stack = [[UIStackView alloc] init];
  stack.axis = UILayoutConstraintAxisVertical;
  stack.alignment = UIStackViewAlignmentFill;
  stack.distribution = UIStackViewDistributionFill;

  if (title.length > 0) {
    [stack addArrangedSubview:[self titleRowWithText:title]];
  }

  if (hasOptions) {
    NSInteger destructive = [TiUtils intValue:[self valueForUndefinedKey:@"destructive"] def:-1];
    NSArray *optionList = (NSArray *)options;
    for (NSUInteger index = 0; index < optionList.count; index++) {
      id option = optionList[index];
      NSString *optionTitle = nil;
      UIColor *color = ((NSInteger)index == destructive) ? [UIColor systemRedColor] : [UIColor labelColor];
      UIImage *image = nil;

      if ([option isKindOfClass:[NSDictionary class]]) {
        NSDictionary *map = (NSDictionary *)option;
        optionTitle = [TiUtils stringValue:map[@"title"]];
        color = [self colorFromValue:map[@"color"] fallback:color];
        if ([self hasValue:map[@"image"]]) {
          image = [TiUtils toImage:map[@"image"] proxy:self];
        }
      } else {
        optionTitle = [TiUtils stringValue:option];
      }

      [stack addArrangedSubview:[self optionRowWithTitle:optionTitle color:color image:image index:(NSInteger)index]];
    }
  }

  return stack;
}

#pragma mark - Presentation

- (void)presentSheet
{
  if (controller != nil) {
    return;
  }

  if (@available(iOS 15.0, *)) {
    TiBottomsheetSheetController *sheetController = [[TiBottomsheetSheetController alloc] init];
    sheetController.layoutProxy = self;
    sheetController.sheetDelegate = self;
    sheetController.hostsProxyView = NO;
    sheetController.headerView = [self buildHeader];
    sheetController.sheetBackgroundColor = [self colorFromValue:[self valueForUndefinedKey:@"backgroundColor"] fallback:nil];
    sheetController.modalPresentationStyle = UIModalPresentationPageSheet;
    sheetController.modalInPresentation = ![TiUtils boolValue:[self valueForUndefinedKey:@"cancelable"] def:NO];

    UISheetPresentationController *sheet = sheetController.sheetPresentationController;
    sheet.delegate = sheetController;

    // Without a peakHeight the sheet is sized to its header rows and children.
    NSArray<UISheetPresentationControllerDetent *> *detents =
        [sheetController detentsWithPeakHeight:[self valueForUndefinedKey:@"peakHeight"] sizeToContent:YES];
    sheet.detents = detents;
    BOOL grabber = [TiUtils boolValue:[self valueForUndefinedKey:@"dragIndicator"] def:NO];
    sheet.prefersGrabberVisible = grabber;
    // The grabber is drawn over the content, so push header and content down a bit.
    sheetController.topInset = grabber ? TiBottomsheetGrabberInset : 0;

    id radius = [self valueForUndefinedKey:@"borderRadius"];
    if ([radius isKindOfClass:[NSArray class]]) {
      radius = [(NSArray *)radius firstObject];
    }
    if ([self hasValue:radius]) {
      sheet.preferredCornerRadius = [TiUtils floatValue:radius def:0];
    }

    // iOS cannot dim partially: 0 disables the dimming, anything else keeps the system dim.
    id dim = [self valueForUndefinedKey:@"dimAmount"];
    if ([self hasValue:dim] && [TiUtils floatValue:dim def:1] <= 0) {
      sheet.largestUndimmedDetentIdentifier = [sheetController identifierForLastDetent:detents];
    }

    controller = sheetController;
    lastContentSize = CGSizeZero;
    [self rememberSelf];

    // A dialog that was added to a window (Alloy) is opened/closed by that window.
    // A standalone dialog has to run the window lifecycle itself so layout works.
    managesWindowLifecycle = (self.parent == nil);
    if (managesWindowLifecycle) {
      [self setSandboxBounds:CGRectZero];
      [self parentWillShow];
      [self windowWillOpen];
      // Make sure the view tree (and with it the children's views) exists.
      [self view];
    }

    [[TiApp app] showModalController:sheetController animated:YES];
  } else {
    NSLog(@"[ERROR] ti.bottomsheet: BottomSheetDialog requires iOS 15 or newer");
  }
}

- (void)dismissSheet
{
  if (controller == nil) {
    return;
  }
  if (@available(iOS 15.0, *)) {
    ((TiBottomsheetSheetController *)controller).dismissedByUser = NO;
  }
  [[TiApp app] hideModalController:controller animated:YES];
}

- (void)fireStateEvent:(TiBottomsheetState)state
{
  if ([self _hasListeners:@"stateChanged"]) {
    [self fireEvent:@"stateChanged" withObject:@{ @"state" : @(state) }];
  }

  NSString *name = nil;
  switch (state) {
  case TiBottomsheetStateOpen:
    name = @"open";
    break;
  case TiBottomsheetStatePeak:
    name = @"peak";
    break;
  case TiBottomsheetStateClose:
    name = @"close";
    break;
  default:
    break;
  }
  if (name != nil && [self _hasListeners:name]) {
    [self fireEvent:name withObject:@{}];
  }
}

- (void)optionTapped:(UIButton *)sender
{
  if ([self _hasListeners:@"click"]) {
    [self fireEvent:@"click" withObject:@{ @"index" : @(sender.tag), @"cancel" : @(NO) }];
  }
  [self dismissSheet];
}

#pragma mark - Layout

// Called by TiViewProxy when children are added, removed or resized. A
// content-sized sheet has to follow its content.
- (void)contentsWillChange
{
  [super contentsWillChange];
  if (controller == nil) {
    return;
  }
  if (@available(iOS 15.0, *)) {
    TiThreadPerformOnMainThread(^{
      [(TiBottomsheetSheetController *)self->controller contentSizeDidChange];
    }, NO);
  }
}

#pragma mark - TiBottomsheetSheetControllerDelegate

- (void)sheetControllerDidAppear:(TiBottomsheetSheetController *)sheetController
{
  if (managesWindowLifecycle) {
    [self windowDidOpen];
  }
  // A single, content-sized detent is already "open"; otherwise we start at the peek detent.
  [self fireStateEvent:[sheetController isAtLargestDetent] ? TiBottomsheetStateOpen : TiBottomsheetStatePeak];
}

- (void)sheetController:(TiBottomsheetSheetController *)sheetController didSelectDetent:(NSString *)identifier
{
  [self fireStateEvent:[sheetController isAtLargestDetent] ? TiBottomsheetStateOpen : TiBottomsheetStatePeak];
}

- (void)sheetController:(TiBottomsheetSheetController *)sheetController didDisappearCancelled:(BOOL)cancelled
{
  if (controller == nil) {
    return;
  }
  controller = nil;

  if (managesWindowLifecycle) {
    [self windowWillClose];
    [self windowDidClose];
    [self parentWillHide];
  }

  if (cancelled && [self _hasListeners:@"cancel"]) {
    [self fireEvent:@"cancel" withObject:@{}];
  }
  [self fireStateEvent:TiBottomsheetStateClose];
  [self forgetSelf];
}

- (void)sheetController:(TiBottomsheetSheetController *)sheetController didLayoutContentArea:(CGRect)bounds
{
  if (!CGSizeEqualToSize(bounds.size, lastContentSize)) {
    lastContentSize = bounds.size;
    if (self.parent != nil) {
      // Placeholder in a window: size it like the content area so the children
      // are measured against the bounds they are positioned in.
      [self setValue:@(bounds.size.width) forKey:@"width"];
      [self setValue:@(bounds.size.height) forKey:@"height"];
    } else {
      // Standalone: give the proxy the content bounds directly.
      [self setSandboxBounds:bounds];
      [self relayout];
    }
  }
  // Children (re)attach to the content area and lay out inside it.
  [self layoutChildren:NO];
}

#pragma mark - Public API

- (void)show:(id)args
{
  TiThreadPerformOnMainThread(^{
    [self presentSheet];
  }, NO);
}

- (void)open:(id)args
{
  [self show:args];
}

- (void)hide:(id)args
{
  TiThreadPerformOnMainThread(^{
    [self dismissSheet];
  }, NO);
}

- (void)close:(id)args
{
  [self hide:args];
}

@end
