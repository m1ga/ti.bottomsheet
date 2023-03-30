package ti.bottomsheet;

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
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
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
    private static final String TAG = "TiUIBottomSheetView";
    int id_drawer_layout = 0;
    int id_bottomSheet = 0;
    LinearLayoutCompat layout;
    AppCompatActivity appCompatActivity;
    BottomSheetBehavior bottomSheetBehavior;
    LinearLayoutCompat bsLayout;
    int peakHeight = 32;
    BottomSheetDialog dialog;
    float density = TiApplication.getInstance().getResources().getDisplayMetrics().density;
    int destructive = -1;
    boolean cancelable = false;

    public TiUIBottomSheetDialogView(TiViewProxy proxy) {
        super(proxy);
        appCompatActivity = (AppCompatActivity) proxy.getActivity();
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

    private void addViews(TiUIView child) {
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


    @Override
    public void processProperties(KrollDict d) {
        super.processProperties(d);

        try {
            id_drawer_layout = TiRHelper.getResource("layout.titanium_ui_bottomsheet_dialog");
            id_bottomSheet = TiRHelper.getResource("id.bottomSheetDialog");
        } catch (TiRHelper.ResourceNotFoundException e) {
            //
        }
        LayoutInflater inflater = LayoutInflater.from(proxy.getActivity());
        layout = (LinearLayoutCompat) inflater.inflate(id_drawer_layout, null);
        bsLayout = layout.findViewById(id_bottomSheet);


        if (d.containsKeyAndNotNull(TiC.PROPERTY_TITLE)) {
            AppCompatTextView tf = new AppCompatTextView(proxy.getActivity());
            tf.setId(View.generateViewId());
            LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(
                    LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
            tf.setLayoutParams(lp);
            tf.setText(d.getString(TiC.PROPERTY_TITLE));
            Configuration config = TiApplication.getInstance().getResources().getConfiguration();
            tf.setTextColor(Color.WHITE);
            if ((config.uiMode & Configuration.UI_MODE_NIGHT_YES) != 0) {
                //
            } else {
                tf.setTextColor(Color.BLACK);
            }

            if (destructive > -1) {
                tf.setTextColor(Color.RED);
            }

            if (d.containsKeyAndNotNull(TiC.PROPERTY_TITLE_COLOR)) {
                tf.setTextColor(TiColorHelper.parseColor(d.getString(TiC.PROPERTY_TITLE_COLOR)));
            }
            int paddingValue = (int) (15 * density);
            tf.setPadding(paddingValue, paddingValue, paddingValue, paddingValue);
            tf.setTextSize(18);
            bsLayout.addView(tf);
        }

        if (d.containsKeyAndNotNull(TiC.PROPERTY_CANCELABLE)) {
            cancelable = d.getBoolean(TiC.PROPERTY_CANCELABLE);
        }

        if (d.containsKeyAndNotNull("destructive")) {
            destructive = d.getInt("destructive");
        }

        if (d.containsKeyAndNotNull(TiC.PROPERTY_OPTIONS)) {
            // display options

            Object obj = d.get(TiC.PROPERTY_OPTIONS);
            if (obj instanceof String[]) {
                String[] options = d.getStringArray(TiC.PROPERTY_OPTIONS);

                for (int i = 0, len = options.length; i < len; i++) {
                    AppCompatTextView tf = createOption(options[i], i);
                }
            } else {
                Object[] options = (Object[]) d.get(TiC.PROPERTY_OPTIONS);
                for (int i = 0, len = options.length; i < len; i++) {
                    HashMap map = (HashMap) options[i];
                    AppCompatTextView tf = createOption((String) map.get("title"), i);

                    if (!map.get("color").equals("")) {
                        tf.setTextColor(TiColorHelper.parseColor((String) map.get("color")));
                    }

                    tf.setCompoundDrawablePadding((int) (15 * density));
                    Object value = map.get("image");
                    TiDrawableReference drawableRef = null;
                    if (value instanceof String) {
                        drawableRef = TiDrawableReference.fromUrl(proxy, (String) value);
                    } else if (value instanceof TiBlob) {
                        drawableRef = TiDrawableReference.fromBlob(proxy.getActivity(), (TiBlob) value);
                    }
                    if (drawableRef != null) {
                        Drawable image = drawableRef.getDensityScaledDrawable();
                        tf.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);
                    }
                }
            }
        }


        dialog = new BottomSheetDialog(TiApplication.getAppCurrentActivity(), R.style.BottomSheetDialog);
        dialog.setContentView(layout);
        dialog.setCancelable(cancelable);
        if(d.containsKeyAndNotNull("dimAmount"))
        {
            dialog.getWindow().setDimAmount(TiConvert.toFloat(d.get("dimAmount")));
        }
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                fireEvent("close", new KrollDict());
            }
        });
        bottomSheetBehavior = BottomSheetBehavior.from((View) layout.getParent());

        if (d.containsKeyAndNotNull(TiC.PROPERTY_BACKGROUND_COLOR)) {
            layout.setBackgroundTintList(ColorStateList.valueOf(TiConvert.toColor(d.getString(TiC.PROPERTY_BACKGROUND_COLOR),
                    TiApplication.getAppCurrentActivity())));
        }

        if (d.containsKeyAndNotNull(TiC.PROPERTY_BORDER_RADIUS)) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            float radius = 0;
            if (d.get(TiC.PROPERTY_BORDER_RADIUS) instanceof Object[]) {
                Object[] obj = (Object[]) d.get(TiC.PROPERTY_BORDER_RADIUS);
                radius = (float) obj[0];
            } else {
                radius = d.getInt(TiC.PROPERTY_BORDER_RADIUS).floatValue();
            }
            shape.setCornerRadii(new float[]{radius, radius, radius, radius, 0, 0, 0, 0});
            layout.setBackground(shape);
        }

        if (d.containsKeyAndNotNull("peakHeight")) {
            bottomSheetBehavior.setPeekHeight(d.getInt("peakHeight"));
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

                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    fireEvent("peak", new KrollDict());
                }

                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    //fireEvent("close", new KrollDict());
                }
            }


            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        try {
            View touchOutside = dialog.getWindow().getDecorView().findViewById(
                    TiRHelper.getResource("id.touch_outside"));

            if (touchOutside != null) {
                touchOutside.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (dialog.isShowing()) {
                            if (cancelable) {
                                fireEvent("cancel", new KrollDict());
                                dialog.dismiss();
                            }
                        }
                    }
                });
            }
        } catch (TiRHelper.ResourceNotFoundException e) {
            //
        }
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

    private AppCompatTextView createOption(String title, int position) {
        AppCompatTextView tf = new AppCompatTextView(proxy.getActivity());
        tf.setId(View.generateViewId());
        LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.VERTICAL_GRAVITY_MASK;
        tf.setLayoutParams(lp);
        tf.setText(title);
        int paddingValue = (int) (15 * density);
        tf.setPadding(paddingValue, paddingValue, paddingValue, paddingValue);
        tf.setTextSize(18);
        tf.setMaxLines(1);

        bsLayout.addView(tf);

        int finalI = position;
        tf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KrollDict event = new KrollDict();
                event.put("index", finalI);
                event.put("cancel", false);
                fireEvent("click", event);
                dialog.dismiss();
            }
        });
        return tf;
    }


    public void hide() {
        dialog.dismiss();
    }

    public void show() {
        dialog.show();
    }
}
