package ti.bottomsheet;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDragHandleView;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiColorHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.view.TiDrawableReference;
import org.appcelerator.titanium.view.TiUIView;

import java.util.HashMap;

public class TiUIBottomSheetDialogView extends TiUIView {
    private static final String TAG = "TiUIBottomSheetDialogView";

    private final LinearLayoutCompat layout;
    private final float density = TiApplication.getInstance().getResources().getDisplayMetrics().density;

    private BottomSheetDialog dialog;
    private Activity dialogActivity;
    private boolean suppressEvents = false;

    private boolean cancelable = false;
    private float dimAmount = -1;
    private int peekHeightPx = -1;
    private int backgroundColor = Color.WHITE;
    private Object borderRadius;
    private BottomSheetDragHandleView dragHandle;

    private final BottomSheetBehavior.BottomSheetCallback callback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (suppressEvents) {
                return;
            }
            KrollDict kd = new KrollDict();
            kd.put("state", newState);
            fireEvent("stateChanged", kd);

            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                fireEvent("open", new KrollDict());
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                fireEvent("peak", new KrollDict());
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    public TiUIBottomSheetDialogView(TiViewProxy proxy) {
        super(proxy);

        int layoutId = 0;
        int sheetId = 0;
        try {
            layoutId = TiRHelper.getResource("layout.titanium_ui_bottomsheet_dialog");
            sheetId = TiRHelper.getResource("id.bottomSheetDialog");
        } catch (TiRHelper.ResourceNotFoundException e) {
            Log.e(TAG, "Bottom sheet dialog layout resources not found", e);
        }

        Activity activity = TiBottomSheetUtil.activityFor(proxy);
        LinearLayoutCompat inflated = (LinearLayoutCompat) LayoutInflater.from(activity).inflate(layoutId, null);
        LinearLayoutCompat sheet = inflated.findViewById(sheetId);
        layout = sheet != null ? sheet : inflated;

        // Title and options are creation-only, so they are built exactly once here.
        buildRows(proxy.getProperties(), activity);
    }

    private void buildRows(KrollDict d, Activity activity) {
        int destructive = -1;
        if (d.containsKeyAndNotNull("destructive")) {
            destructive = TiConvert.toInt(d.get("destructive"), -1);
        }

        if (d.containsKeyAndNotNull(TiC.PROPERTY_TITLE)) {
            AppCompatTextView tf = new AppCompatTextView(activity);
            tf.setId(View.generateViewId());
            tf.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                    LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT));
            tf.setText(TiConvert.toString(d.get(TiC.PROPERTY_TITLE)));

            Configuration config = TiApplication.getInstance().getResources().getConfiguration();
            boolean nightMode = (config.uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            tf.setTextColor(nightMode ? Color.WHITE : Color.BLACK);
            if (d.containsKeyAndNotNull(TiC.PROPERTY_TITLE_COLOR)) {
                tf.setTextColor(TiColorHelper.parseColor(TiConvert.toString(d.get(TiC.PROPERTY_TITLE_COLOR))));
            }

            int paddingValue = (int) (15 * density);
            tf.setPadding(paddingValue, paddingValue, paddingValue, paddingValue);
            tf.setTextSize(18);
            layout.addView(tf);
        }

        if (d.containsKeyAndNotNull(TiC.PROPERTY_OPTIONS)) {
            Object obj = d.get(TiC.PROPERTY_OPTIONS);
            Object[] options = obj instanceof Object[] ? (Object[]) obj : new Object[0];

            for (int i = 0; i < options.length; i++) {
                Object option = options[i];
                if (option instanceof HashMap) {
                    HashMap map = (HashMap) option;
                    AppCompatTextView tf = createOption(TiConvert.toString(map.get("title")), i, activity);
                    if (i == destructive) {
                        tf.setTextColor(Color.RED);
                    }

                    Object color = map.get("color");
                    if (color != null && !TiConvert.toString(color).isEmpty()) {
                        tf.setTextColor(TiColorHelper.parseColor(TiConvert.toString(color)));
                    }

                    Object image = map.get("image");
                    TiDrawableReference drawableRef = null;
                    if (image instanceof String) {
                        drawableRef = TiDrawableReference.fromUrl(proxy, (String) image);
                    } else if (image instanceof TiBlob) {
                        drawableRef = TiDrawableReference.fromBlob(activity, (TiBlob) image);
                    }
                    if (drawableRef != null) {
                        Drawable drawable = drawableRef.getDensityScaledDrawable();
                        tf.setCompoundDrawablePadding((int) (15 * density));
                        tf.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    }
                } else {
                    AppCompatTextView tf = createOption(TiConvert.toString(option), i, activity);
                    if (i == destructive) {
                        tf.setTextColor(Color.RED);
                    }
                }
            }
        }
    }

    private AppCompatTextView createOption(String title, final int position, Activity activity) {
        AppCompatTextView tf = new AppCompatTextView(activity);
        tf.setId(View.generateViewId());
        tf.setLayoutParams(new LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT));
        tf.setText(title);
        int paddingValue = (int) (15 * density);
        tf.setPadding(paddingValue, paddingValue, paddingValue, paddingValue);
        tf.setTextSize(18);
        tf.setMaxLines(1);
        tf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KrollDict event = new KrollDict();
                event.put("index", position);
                event.put("cancel", false);
                fireEvent("click", event);
                hide();
            }
        });

        layout.addView(tf);
        return tf;
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
        layout.addView(wrapper, new LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT));

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
            layout.removeView(view);
        }
        children.remove(child);
        child.setParent(null);
    }

    @Override
    public void processProperties(KrollDict d) {
        super.processProperties(d);

        if (d.containsKeyAndNotNull(TiC.PROPERTY_CANCELABLE)) {
            setCancelable(TiConvert.toBoolean(d.get(TiC.PROPERTY_CANCELABLE), false));
        }
        if (d.containsKeyAndNotNull("dimAmount")) {
            setDimAmount(TiConvert.toFloat(d.get("dimAmount")));
        }
        if (d.containsKeyAndNotNull("peakHeight")) {
            setPeakHeight(d.get("peakHeight"));
        }
        if (d.containsKeyAndNotNull(TiC.PROPERTY_BACKGROUND_COLOR)) {
            setBackgroundColor(d.get(TiC.PROPERTY_BACKGROUND_COLOR));
        }
        if (d.containsKeyAndNotNull(TiC.PROPERTY_BORDER_RADIUS)) {
            setBorderRadius(d.get(TiC.PROPERTY_BORDER_RADIUS));
        }
        if (d.containsKeyAndNotNull("dragIndicator")) {
            setDragIndicator(TiConvert.toBoolean(d.get("dragIndicator"), false));
        }
    }

    @Override
    public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
        if (key.equals("peakHeight")) {
            setPeakHeight(newValue);
        } else if (key.equals(TiC.PROPERTY_CANCELABLE)) {
            setCancelable(TiConvert.toBoolean(newValue, false));
        } else if (key.equals("dimAmount")) {
            setDimAmount(TiConvert.toFloat(newValue));
        } else if (key.equals(TiC.PROPERTY_BACKGROUND_COLOR)) {
            setBackgroundColor(newValue);
        } else if (key.equals(TiC.PROPERTY_BORDER_RADIUS)) {
            setBorderRadius(newValue);
        } else if (key.equals("dragIndicator")) {
            setDragIndicator(TiConvert.toBoolean(newValue, false));
        } else {
            super.propertyChanged(key, oldValue, newValue, proxy);
        }
    }

    /**
     * Shows or hides the native Material drag handle above the title and options.
     */
    private void setDragIndicator(boolean enabled) {
        if (enabled == (dragHandle != null)) {
            return;
        }
        if (enabled) {
            dragHandle = new BottomSheetDragHandleView(TiBottomSheetUtil.activityFor(proxy));
            dragHandle.setId(View.generateViewId());
            LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(
                    LinearLayoutCompat.LayoutParams.WRAP_CONTENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            layout.addView(dragHandle, 0, lp);
        } else {
            layout.removeView(dragHandle);
            dragHandle = null;
        }
    }

    private void setCancelable(boolean value) {
        cancelable = value;
        if (dialog != null) {
            dialog.setCancelable(cancelable);
            dialog.setCanceledOnTouchOutside(cancelable);
        }
    }

    private void setDimAmount(float value) {
        dimAmount = value;
        if (dialog != null && dimAmount >= 0) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(dimAmount);
            }
        }
    }

    private void setPeakHeight(Object value) {
        peekHeightPx = TiBottomSheetUtil.toPixels(value, TiDimension.TYPE_HEIGHT, layout);
        if (dialog != null) {
            dialog.getBehavior().setPeekHeight(peekHeightPx);
        }
    }

    private void setBackgroundColor(Object value) {
        backgroundColor = TiConvert.toColor(TiConvert.toString(value), TiBottomSheetUtil.activityFor(proxy));
        layout.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        if (borderRadius != null) {
            setBorderRadius(borderRadius);
        }
    }

    private void setBorderRadius(Object value) {
        borderRadius = value;
        float topLeft;
        float topRight;
        if (value instanceof Object[]) {
            Object[] values = (Object[]) value;
            topLeft = values.length > 0 ? TiBottomSheetUtil.toPixels(values[0], TiDimension.TYPE_WIDTH, layout) : 0;
            topRight = values.length > 1 ? TiBottomSheetUtil.toPixels(values[1], TiDimension.TYPE_WIDTH, layout) : topLeft;
        } else {
            topLeft = TiBottomSheetUtil.toPixels(value, TiDimension.TYPE_WIDTH, layout);
            topRight = topLeft;
        }

        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(backgroundColor);
        shape.setCornerRadii(new float[] { topLeft, topLeft, topRight, topRight, 0, 0, 0, 0 });
        layout.setBackground(shape);
    }

    /**
     * Creates the dialog on demand, bound to the activity that is alive right
     * now, so showing it after the original activity was destroyed still works.
     */
    private void ensureDialog() {
        Activity activity = TiApplication.getAppCurrentActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            activity = TiBottomSheetUtil.activityFor(proxy);
        }
        if (activity == null) {
            Log.w(TAG, "No activity available to show the bottom sheet dialog");
            return;
        }

        if (dialog != null) {
            boolean oldActivityAlive = dialogActivity != null && !dialogActivity.isFinishing()
                    && !dialogActivity.isDestroyed();
            if (dialogActivity == activity && oldActivityAlive) {
                return;
            }
            destroyDialog(oldActivityAlive);
        }

        ViewGroup parent = (ViewGroup) layout.getParent();
        if (parent != null) {
            parent.removeView(layout);
        }

        dialog = new BottomSheetDialog(activity, R.style.BottomSheetDialog);
        dialogActivity = activity;
        dialog.setContentView(layout);
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(cancelable);
        if (dimAmount >= 0) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setDimAmount(dimAmount);
            }
        }
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (!suppressEvents) {
                    fireEvent("cancel", new KrollDict());
                }
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                if (!suppressEvents) {
                    fireEvent("close", new KrollDict());
                }
            }
        });

        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        if (peekHeightPx >= 0) {
            behavior.setPeekHeight(peekHeightPx);
        }
        behavior.addBottomSheetCallback(callback);
    }

    private void destroyDialog(boolean dismiss) {
        if (dialog == null) {
            return;
        }
        suppressEvents = true;
        try {
            dialog.getBehavior().removeBottomSheetCallback(callback);
            if (dismiss && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to tear down bottom sheet dialog: " + e.getMessage());
        } finally {
            suppressEvents = false;
        }
        dialog = null;
        dialogActivity = null;
    }

    public void hide() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void show() {
        ensureDialog();
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    @Override
    public void release() {
        destroyDialog(true);
        super.release();
    }
}
