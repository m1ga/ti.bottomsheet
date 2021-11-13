# Ti.BottomSheet & Ti.BottomSheetDialog for Android


__work in progress__ methods/properties/events might change before the first release.


## Example

```xml
<BottomSheet id="bs" module="ti.bottomsheet">
  <!-- add your content -->
</BottomSheet>

<BottomSheetDialog id="bsd" module="ti.bottomsheet">
  <!-- add your content -->
</BottomSheetDialog>
```


```js
$.bs.show();
$.bsd.show();
```

## BottomSheet

Can be used for a peaking view at the bottom that can be extended.

### Methods

* show
* hide
* toggle
* add

### Properties

* peakHeight

### Events
* open
* close
* stateChanged: e.state (see Constants)

### Constants

* BottomSheet.STATE_DRAGGING
* BottomSheet.STATE_SLIDING
* BottomSheet.STATE_OPEN
* BottomSheet.STATE_PEAK
* BottomSheet.STATE_CLOSE


---
## BottomSheetDialog

The dialog version will dimm the background when it is shown.

### Methods

* show
* hide
* add

### Properties

* backgroundColor
* borderRadius
* cancelable
* peakHeight

### Events

* dismissed
* open
* close
* stateChanged: e.state (see Constants)
* peak

### Constants

* BottomSheet.STATE_DRAGGING
* BottomSheet.STATE_SLIDING
* BottomSheet.STATE_OPEN
* BottomSheet.STATE_PEAK
* BottomSheet.STATE_CLOSE
