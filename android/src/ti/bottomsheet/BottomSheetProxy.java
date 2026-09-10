package ti.bottomsheet;

import android.app.Activity;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import java.util.HashMap;

@Kroll.proxy(creatableInModule = TiBottomsheetModule.class,
        propertyAccessors = {
                "peakHeight",
                "nestedScrolling",
                "dragIndicator"
        })
public class BottomSheetProxy extends TiViewProxy {

    public BottomSheetProxy() {
        super();
        defaultValues.put(TiC.PROPERTY_BOTTOM, 0);
        defaultValues.put("peakHeight", TiUIBottomSheetView.DEFAULT_PEAK_HEIGHT);
        defaultValues.put("nestedScrolling", false);
        defaultValues.put("dragIndicator", false);
        defaultValues.put(TiC.PROPERTY_ZINDEX, 10000);
    }

    @Override
    public TiUIView createView(Activity activity) {
        return new TiUIBottomSheetView(this);
    }

    private TiUIBottomSheetView sheet() {
        TiUIView view = peekView();
        return view instanceof TiUIBottomSheetView ? (TiUIBottomSheetView) view : null;
    }

    @Kroll.method
    public void toggle() {
        TiUIBottomSheetView sheet = sheet();
        if (sheet != null) {
            sheet.toggle();
        }
    }

    @Kroll.method
    public void hide(@Kroll.argument(optional = true) HashMap options) {
        close(options);
    }

    @Kroll.method
    public void close(@Kroll.argument(optional = true) HashMap options) {
        TiUIBottomSheetView sheet = sheet();
        if (sheet != null) {
            sheet.collapse();
        }
    }

    @Kroll.method
    public void show(@Kroll.argument(optional = true) HashMap options) {
        open(options);
    }

    @Kroll.method
    public void open(@Kroll.argument(optional = true) HashMap options) {
        TiUIBottomSheetView sheet = sheet();
        if (sheet != null) {
            sheet.expand();
        }
    }

    @Override
    public String getApiName() {
        return "ti.bottomsheet.BottomSheet";
    }
}
