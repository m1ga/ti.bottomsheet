/**
 * ti.bottomsheet
 *
 * Copyright (c) 2021-2026 Michael Gangolf. All rights reserved.
 */

#import "TiBottomsheetBottomSheet.h"
#import "TiBottomsheetModule.h"
#import "TiDimension.h"
#import "TiUtils.h"
#import "TiViewProxy.h"

static const CGFloat kTiBottomsheetDefaultPeakHeight = 32.0;
static const CGFloat kTiBottomsheetHandleAreaHeight = 20.0;
static const CGFloat kTiBottomsheetHandleWidth = 36.0;
static const CGFloat kTiBottomsheetHandleHeight = 5.0;
static const CGFloat kTiBottomsheetFlingVelocity = 300.0;
static const NSTimeInterval kTiBottomsheetAnimationDuration = 0.25;

@implementation TiBottomsheetBottomSheet {
  UIView *sheetView;
  UIView *contentView;
  UIView *dragHandle;
  UIPanGestureRecognizer *panGesture;
  TiDimension peakDimension;
  CGFloat peakHeight;
  CGFloat dragStartY;
  BOOL expanded;
  NSInteger currentState;
}

#pragma mark - Setup

- (void)initializeState
{
  [super initializeState];

  peakDimension = TiDimensionMake(TiDimensionTypeDip, kTiBottomsheetDefaultPeakHeight);
  peakHeight = kTiBottomsheetDefaultPeakHeight;
  expanded = NO;
  currentState = TiBottomsheetStatePeak;

  self.backgroundColor = [UIColor clearColor];

  sheetView = [[UIView alloc] initWithFrame:CGRectZero];
  sheetView.backgroundColor = [UIColor clearColor];
  sheetView.clipsToBounds = YES;
  [self addSubview:sheetView];

  contentView = [[UIView alloc] initWithFrame:CGRectZero];
  contentView.backgroundColor = [UIColor clearColor];
  [sheetView addSubview:contentView];

  panGesture = [[UIPanGestureRecognizer alloc] initWithTarget:self action:@selector(handlePan:)];
  panGesture.delegate = self;
  [sheetView addGestureRecognizer:panGesture];
}

- (UIView *)contentView
{
  return contentView;
}

- (BOOL)isExpanded
{
  return expanded;
}

#pragma mark - Layout

- (void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
  [super frameSizeChanged:frame bounds:bounds];
  peakHeight = TiDimensionCalculateValue(peakDimension, bounds.size.height);
  [self layoutSheet];
}

- (CGFloat)collapsedY
{
  return MAX(0, self.bounds.size.height - peakHeight);
}

- (void)layoutSheet
{
  CGRect bounds = self.bounds;
  CGFloat y = expanded ? 0 : [self collapsedY];
  sheetView.frame = CGRectMake(0, y, bounds.size.width, bounds.size.height);

  CGFloat contentTop = 0;
  if (dragHandle != nil) {
    dragHandle.frame = CGRectMake((bounds.size.width - kTiBottomsheetHandleWidth) / 2.0,
        (kTiBottomsheetHandleAreaHeight - kTiBottomsheetHandleHeight) / 2.0,
        kTiBottomsheetHandleWidth, kTiBottomsheetHandleHeight);
    contentTop = kTiBottomsheetHandleAreaHeight;
  }
  contentView.frame = CGRectMake(0, contentTop, bounds.size.width, MAX(0, bounds.size.height - contentTop));

  // Mirrors Android: a sheet with peakHeight 0 disappears completely when collapsed.
  sheetView.hidden = (!expanded && peakHeight <= 0);
}

// Let touches that miss the sheet reach the views underneath the overlay.
- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event
{
  UIView *hit = [super hitTest:point withEvent:event];
  if (hit == self) {
    return nil;
  }
  return hit;
}

#pragma mark - State

- (void)updateState:(NSInteger)state
{
  if (state == currentState) {
    return;
  }
  currentState = state;
  [self fireSheetEvent:@"stateChanged" withObject:@{ @"state" : @(state) }];
}

- (void)fireSheetEvent:(NSString *)name withObject:(NSDictionary *)object
{
  if ([self.proxy _hasListeners:name]) {
    [self.proxy fireEvent:name withObject:object];
  }
}

- (void)setExpanded:(BOOL)value animated:(BOOL)animated
{
  expanded = value;
  sheetView.hidden = NO;

  CGRect target = sheetView.frame;
  target.origin.y = expanded ? 0 : [self collapsedY];

  __weak TiBottomsheetBottomSheet *weakSelf = self;
  void (^finish)(void) = ^{
    TiBottomsheetBottomSheet *strongSelf = weakSelf;
    if (strongSelf == nil) {
      return;
    }
    [strongSelf layoutSheet];
    BOOL isExpanded = [strongSelf isExpanded];
    [strongSelf updateState:isExpanded ? TiBottomsheetStateOpen : TiBottomsheetStatePeak];
    [strongSelf fireSheetEvent:isExpanded ? @"open" : @"close" withObject:@{}];
  };

  if (!animated) {
    sheetView.frame = target;
    finish();
    return;
  }

  [self updateState:TiBottomsheetStateSliding];
  [UIView animateWithDuration:kTiBottomsheetAnimationDuration
      delay:0
      options:UIViewAnimationOptionCurveEaseOut | UIViewAnimationOptionBeginFromCurrentState
      animations:^{
        self->sheetView.frame = target;
      }
      completion:^(BOOL finished) {
        finish();
      }];
}

- (void)expandAnimated:(BOOL)animated
{
  [self setExpanded:YES animated:animated];
}

- (void)collapseAnimated:(BOOL)animated
{
  [self setExpanded:NO animated:animated];
}

- (void)toggle
{
  [self setExpanded:!expanded animated:YES];
}

#pragma mark - Dragging

- (void)handlePan:(UIPanGestureRecognizer *)gesture
{
  CGFloat maxY = [self collapsedY];

  switch (gesture.state) {
  case UIGestureRecognizerStateBegan:
    dragStartY = sheetView.frame.origin.y;
    [self updateState:TiBottomsheetStateDragging];
    break;

  case UIGestureRecognizerStateChanged: {
    CGFloat translation = [gesture translationInView:self].y;
    CGFloat y = MIN(maxY, MAX(0, dragStartY + translation));
    CGRect frame = sheetView.frame;
    frame.origin.y = y;
    sheetView.frame = frame;
    break;
  }

  case UIGestureRecognizerStateEnded:
  case UIGestureRecognizerStateCancelled:
  case UIGestureRecognizerStateFailed: {
    CGFloat velocity = [gesture velocityInView:self].y;
    BOOL shouldExpand;
    if (fabs(velocity) > kTiBottomsheetFlingVelocity) {
      shouldExpand = velocity < 0;
    } else {
      shouldExpand = sheetView.frame.origin.y < (maxY / 2.0);
    }
    [self setExpanded:shouldExpand animated:YES];
    break;
  }

  default:
    break;
  }
}

- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer
{
  if (gestureRecognizer != panGesture) {
    return YES;
  }
  // Only claim mostly-vertical drags so horizontal gestures inside the sheet keep working.
  CGPoint velocity = [panGesture velocityInView:self];
  return fabs(velocity.y) > fabs(velocity.x);
}

#pragma mark - Properties

- (void)setPeakHeight_:(id)value
{
  peakDimension = TiDimensionFromObject(value);
  if (TiDimensionIsUndefined(peakDimension)) {
    peakDimension = TiDimensionMake(TiDimensionTypeDip, kTiBottomsheetDefaultPeakHeight);
  }
  peakHeight = TiDimensionCalculateValue(peakDimension, self.bounds.size.height);
  // Same as Android: changing the peek height collapses the sheet.
  expanded = NO;
  [self layoutSheet];
}

- (void)setBackgroundColor_:(id)value
{
  sheetView.backgroundColor = [[TiUtils colorValue:value] _color];
}

- (void)setDragIndicator_:(id)value
{
  BOOL enabled = [TiUtils boolValue:value def:NO];
  if (enabled == (dragHandle != nil)) {
    return;
  }
  if (enabled) {
    dragHandle = [[UIView alloc] initWithFrame:CGRectZero];
    dragHandle.backgroundColor = [UIColor colorWithRed:0.78 green:0.78 blue:0.8 alpha:1.0];
    dragHandle.layer.cornerRadius = kTiBottomsheetHandleHeight / 2.0;
    dragHandle.userInteractionEnabled = NO;
    [sheetView addSubview:dragHandle];
  } else {
    [dragHandle removeFromSuperview];
    dragHandle = nil;
  }
  [self layoutSheet];
  // The content area moved, so children need a new layout pass.
  [(TiViewProxy *)self.proxy layoutChildren:NO];
}

- (void)setNestedScrolling_:(id)value
{
  // Android only. Scroll views inside the sheet already coexist with the drag gesture on iOS.
}

@end
