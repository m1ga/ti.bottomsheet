package ti.bottomsheet;

import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.view.TiUIView;

public class TiUIBottomSheetView extends TiUIView {
    private static final String TAG = "TiUIBottomSheetView";
    public boolean nestedScrolling = false;
    int id_drawer_layout = 0;
    int id_bottomSheet = 0;
    CoordinatorLayout layout;
    AppCompatActivity appCompatActivity;
    BottomSheetBehavior<RelativeLayout> bottomSheetBehavior;
    RelativeLayout bsLayout;
    int peakHeight = 32;
    float oldY = -1;

    public TiUIBottomSheetView(TiViewProxy proxy) {
        super(proxy);
        if (this.nativeView == null) {
            processProperties(getProxy().getProperties());
        }
    }


    @Override
    public void add(TiUIView child) {
        View view = getNativeView(child.getProxy());

        Object width = TiC.LAYOUT_FILL;
        if (child.getProxy().hasProperty(TiC.PROPERTY_WIDTH)) {
            width = child.getProxy().getProperty(TiC.PROPERTY_WIDTH);
        }
        Object height = TiC.LAYOUT_FILL;
        if (child.getProxy().hasProperty(TiC.PROPERTY_HEIGHT)) {
            height = child.getProxy().getProperty(TiC.PROPERTY_HEIGHT);
        }
        int w = RelativeLayout.LayoutParams.MATCH_PARENT;
        if (width.equals(TiC.LAYOUT_SIZE)) {
            w = RelativeLayout.LayoutParams.WRAP_CONTENT;
        } else if (width.equals(TiC.LAYOUT_FILL)) {
            w = RelativeLayout.LayoutParams.MATCH_PARENT;
        } else {
            w = TiConvert.toTiDimension(TiConvert.toString(width), TiDimension.TYPE_WIDTH).getAsPixels(view);
        }

        int h = RelativeLayout.LayoutParams.MATCH_PARENT;
        if (height.equals(TiC.LAYOUT_SIZE)) {
            h = RelativeLayout.LayoutParams.WRAP_CONTENT;
        } else if (height.equals(TiC.LAYOUT_FILL)) {
            h = RelativeLayout.LayoutParams.MATCH_PARENT;
        } else {
            h = TiConvert.toTiDimension(TiConvert.toString(height), TiDimension.TYPE_HEIGHT).getAsPixels(view);
        }

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(w, h);
        bsLayout.addView(view, rlp);
    }

    private View getNativeView(TiViewProxy viewProxy) {
        TiUIView view = viewProxy.getOrCreateView();
        View outerView = view.getOuterView();
        View nativeView = outerView != null ? outerView : view.getNativeView();
        ViewGroup parentViewGroup = (ViewGroup) nativeView.getParent();
        if (parentViewGroup != null) {
            parentViewGroup.removeAllViews();
        }
        return nativeView;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void processProperties(KrollDict d) {
        super.processProperties(d);

        if (d.containsKeyAndNotNull("peakHeight")) {
            peakHeight = TiConvert.toInt(d.get("peakHeight"), 32);
        }

        try {
            id_drawer_layout = TiRHelper.getResource("layout.titanium_ui_bottomsheet");
            id_bottomSheet = TiRHelper.getResource("id.bottomSheet");
        } catch (TiRHelper.ResourceNotFoundException e) {
            //
        }
        LayoutInflater inflater = LayoutInflater.from(proxy.getActivity());
        layout = (CoordinatorLayout) inflater.inflate(id_drawer_layout, null);
        setNativeView(layout);

        bsLayout = (RelativeLayout) layout.findViewById(id_bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bsLayout);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        int localPeak = TiConvert.toTiDimension(TiConvert.toString(peakHeight),
                TiDimension.TYPE_HEIGHT).getAsPixels(getNativeView());
        bottomSheetBehavior.setPeekHeight(localPeak);


        if (d.containsKeyAndNotNull(TiC.PROPERTY_BACKGROUND_COLOR)) {
            bsLayout.setBackgroundColor(TiConvert.toColor(TiConvert.toString(d.get(TiC.PROPERTY_BACKGROUND_COLOR)),
                    TiApplication.getAppCurrentActivity()));
        }

        if (d.containsKeyAndNotNull("nestedScrolling")) {
            nestedScrolling = TiConvert.toBoolean(d.get("nestedScrolling"), false);
            bsLayout.setNestedScrollingEnabled(nestedScrolling);
        }

        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                KrollDict kd = new KrollDict();
                kd.put("state", newState);
                fireEvent("stateChanged", kd);

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    fireEvent("open", new KrollDict());
                }

                if (newState == BottomSheetBehavior.STATE_DRAGGING && peakHeight != 0) {
                    layout.setVisibility(View.VISIBLE);
                    layout.setY(0);
                    bsLayout.setGravity(Gravity.BOTTOM);
                    bsLayout.setY(oldY);
                }

                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    if (peakHeight == 0) {
                        layout.setVisibility(View.GONE);
                    } else {
                        int localPeak = TiConvert.toTiDimension(TiConvert.toString(peakHeight),
                                TiDimension.TYPE_HEIGHT).getAsPixels(getNativeView());
                        layout.setY(layout.getHeight() - localPeak);
                        bsLayout.setGravity(Gravity.TOP);
                        if (oldY == -1) {
                            oldY = bsLayout.getY();
                        }
                        bsLayout.setY(0);
                    }
                    fireEvent("close", new KrollDict());
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });
    }

    @Override
    public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
        super.propertyChanged(key, oldValue, newValue, proxy);

        if (key.equals("peakHeight")) {
            peakHeight = TiConvert.toInt(newValue, 32);
            int localPeak = TiConvert.toTiDimension(TiConvert.toString(peakHeight),
                    TiDimension.TYPE_HEIGHT).getAsPixels(getNativeView());

            bottomSheetBehavior.setPeekHeight(localPeak);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    public void toggle() {
        layout.setVisibility(View.VISIBLE);
        layout.setY(0);
        bsLayout.setGravity(Gravity.BOTTOM);
        bsLayout.setY(oldY);

        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    public void expand() {
        layout.setVisibility(View.VISIBLE);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    public void collapse() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setNestedScrolling(boolean value) {
        bsLayout.setNestedScrollingEnabled(value);
        nestedScrolling = value;
    }
}
