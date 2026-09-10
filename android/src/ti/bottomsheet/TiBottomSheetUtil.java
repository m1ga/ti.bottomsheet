package ti.bottomsheet;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

/**
 * Helpers shared by the sheet and the dialog view.
 */
final class TiBottomSheetUtil {

    private TiBottomSheetUtil() {
    }

    /**
     * Returns a usable activity for the proxy, preferring the activity the
     * proxy was created for and falling back to the currently visible one.
     */
    static Activity activityFor(TiViewProxy proxy) {
        Activity activity = proxy.getActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            activity = TiApplication.getAppCurrentActivity();
        }
        return activity;
    }

    /**
     * Converts a Titanium dimension (number, "10dp", "50%", ...) to pixels.
     */
    static int toPixels(Object value, int type, View reference) {
        if (value == null) {
            return 0;
        }
        TiDimension dimension = TiConvert.toTiDimension(TiConvert.toString(value), type);
        if (dimension == null) {
            return 0;
        }
        return dimension.getAsPixels(reference);
    }


    /**
     * Returns the outer native view of a proxy, detached from any previous parent.
     */
    static View detachedNativeView(TiViewProxy viewProxy) {
        TiUIView view = viewProxy.getOrCreateView();
        if (view == null) {
            return null;
        }
        View nativeView = outerView(view);
        if (nativeView == null) {
            return null;
        }
        ViewGroup parentViewGroup = (ViewGroup) nativeView.getParent();
        if (parentViewGroup != null) {
            parentViewGroup.removeView(nativeView);
        }
        return nativeView;
    }

    static View outerView(TiUIView view) {
        View outer = view.getOuterView();
        return outer != null ? outer : view.getNativeView();
    }

    /**
     * Wraps the child's native view in a {@link TiCompositeLayout} that sizes
     * itself to its content. The child keeps its own Titanium layout params, so
     * the SDK's layout code keeps handling width/height/top/left/... including
     * changes made after the child was added. The returned wrapper is what gets
     * added to the native container.
     */
    static TiCompositeLayout wrapChild(TiUIView child) {
        View nativeView = detachedNativeView(child.getProxy());
        if (nativeView == null) {
            return null;
        }
        TiCompositeLayout wrapper = new TiCompositeLayout(nativeView.getContext());
        wrapper.addView(nativeView, child.getLayoutParams());
        return wrapper;
    }

    /**
     * Returns the view that was added to the native container for this child:
     * the wrapper created by {@link #wrapChild(TiUIView)}, or the child's own
     * outer view if it is not wrapped.
     */
    static View containerViewOf(TiUIView child) {
        View nativeView = outerView(child);
        if (nativeView == null) {
            return null;
        }
        ViewParent parent = nativeView.getParent();
        return parent instanceof TiCompositeLayout ? (View) parent : nativeView;
    }
}
