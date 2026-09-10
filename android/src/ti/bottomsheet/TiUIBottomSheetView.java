package ti.bottomsheet;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.view.TiUIView;

public class TiUIBottomSheetView extends TiUIView {
    private static final String TAG = "TiUIBottomSheetView";
    static final int DEFAULT_PEAK_HEIGHT = 32;

    private final CoordinatorLayout layout;
    private final RelativeLayout bsLayout;
    private final BottomSheetBehavior<RelativeLayout> bottomSheetBehavior;
    private int peakHeightPx = -1;
    private BottomSheetDragHandleView dragHandle;

    public TiUIBottomSheetView(TiViewProxy proxy) {
        super(proxy);

        int layoutId = 0;
        int sheetId = 0;
        try {
            layoutId = TiRHelper.getResource("layout.titanium_ui_bottomsheet");
            sheetId = TiRHelper.getResource("id.bottomSheet");
        } catch (TiRHelper.ResourceNotFoundException e) {
            Log.e(TAG, "Bottom sheet layout resources not found", e);
        }

        LayoutInflater inflater = LayoutInflater.from(TiBottomSheetUtil.activityFor(proxy));
        layout = (CoordinatorLayout) inflater.inflate(layoutId, null);
        bsLayout = layout.findViewById(sheetId);
        bottomSheetBehavior = BottomSheetBehavior.from(bsLayout);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                KrollDict kd = new KrollDict();
                kd.put("state", newState);
                fireEvent("stateChanged", kd);

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    fireEvent("open", new KrollDict());
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (peakHeightPx == 0) {
                        layout.setVisibility(View.GONE);
                    }
                    fireEvent("close", new KrollDict());
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        setNativeView(layout);
    }

    @Override
    public void add(TiUIView child) {
        if (child == null) {
            return;
        }
        View wrapper = TiBottomSheetUtil.wrapChild(child);
        if (wrapper == null) {
            return;
        }
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (dragHandle != null) {
            lp.addRule(RelativeLayout.BELOW, dragHandle.getId());
        }
        bsLayout.addView(wrapper, lp);

        children.remove(child);
        children.add(child);
        child.setParent(proxy);
    }

    @Override
    public void remove(TiUIView child) {
        if (child == null) {
            return;
        }
        View view = TiBottomSheetUtil.containerViewOf(child);
        if (view != null) {
            bsLayout.removeView(view);
        }
        children.remove(child);
        child.setParent(null);
    }

    @Override
    public void processProperties(KrollDict d) {
        // backgroundColor belongs to the sheet itself, not to the full-screen overlay.
        KrollDict outer = new KrollDict(d);
        outer.remove(TiC.PROPERTY_BACKGROUND_COLOR);
        super.processProperties(outer);

        setPeakHeight(d.containsKeyAndNotNull("peakHeight") ? d.get("peakHeight") : DEFAULT_PEAK_HEIGHT);

        if (d.containsKeyAndNotNull(TiC.PROPERTY_BACKGROUND_COLOR)) {
            setBackgroundColor(d.get(TiC.PROPERTY_BACKGROUND_COLOR));
        }

        if (d.containsKeyAndNotNull("nestedScrolling")) {
            setNestedScrolling(TiConvert.toBoolean(d.get("nestedScrolling"), false));
        }

        if (d.containsKeyAndNotNull("dragIndicator")) {
            setDragIndicator(TiConvert.toBoolean(d.get("dragIndicator"), false));
        }
    }

    @Override
    public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
        if (key.equals("peakHeight")) {
            setPeakHeight(newValue);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (key.equals(TiC.PROPERTY_BACKGROUND_COLOR)) {
            setBackgroundColor(newValue);
        } else if (key.equals("nestedScrolling")) {
            setNestedScrolling(TiConvert.toBoolean(newValue, false));
        } else if (key.equals("dragIndicator")) {
            setDragIndicator(TiConvert.toBoolean(newValue, false));
        } else {
            super.propertyChanged(key, oldValue, newValue, proxy);
        }
    }

    private void setPeakHeight(Object value) {
        peakHeightPx = TiBottomSheetUtil.toPixels(value, TiDimension.TYPE_HEIGHT, layout);
        bottomSheetBehavior.setPeekHeight(peakHeightPx);
    }

    private void setBackgroundColor(Object value) {
        bsLayout.setBackgroundColor(TiConvert.toColor(TiConvert.toString(value), TiBottomSheetUtil.activityFor(proxy)));
    }

    public void toggle() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            collapse();
        } else {
            expand();
        }
    }

    public void expand() {
        layout.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void collapse() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    public void setNestedScrolling(boolean value) {
        bsLayout.setNestedScrollingEnabled(value);
    }

    /**
     * Shows or hides the native Material drag handle at the top of the sheet.
     * Children are laid out below the handle while it is visible.
     */
    private void setDragIndicator(boolean enabled) {
        if (enabled == (dragHandle != null)) {
            return;
        }
        if (enabled) {
            dragHandle = new BottomSheetDragHandleView(TiBottomSheetUtil.activityFor(proxy));
            dragHandle.setId(View.generateViewId());
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            bsLayout.addView(dragHandle, 0, lp);
        } else {
            bsLayout.removeView(dragHandle);
            dragHandle = null;
        }

        for (int i = 0; i < bsLayout.getChildCount(); i++) {
            View child = bsLayout.getChildAt(i);
            if (child == dragHandle) {
                continue;
            }
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) child.getLayoutParams();
            if (dragHandle != null) {
                lp.addRule(RelativeLayout.BELOW, dragHandle.getId());
            } else {
                lp.removeRule(RelativeLayout.BELOW);
            }
            child.setLayoutParams(lp);
        }
    }
}
