package ti.bottomsheet;

import android.app.Activity;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import java.util.HashMap;

@Kroll.proxy(creatableInModule = TiBottomsheetModule.class,
        propertyAccessors = {
                "peakHeight"
        })
public class BottomSheetProxy extends TiViewProxy {
    private static final String TAG = "BottomSheetProxy";
    private static int id_toolbar;
    private TiUIBottomSheetView bottomSheet;
    private int peakHeight = 32;

    public BottomSheetProxy() {
        super();
        defaultValues.put(TiC.PROPERTY_BOTTOM, 0);
        defaultValues.put("peakHeight", peakHeight);
        defaultValues.put(TiC.PROPERTY_ZINDEX, 10000);
    }

    @Override
    public void handleCreationDict(KrollDict options) {
        super.handleCreationDict(options);

        if (options.containsKeyAndNotNull("peakHeight")) {
            peakHeight = TiConvert.toInt(options.get("peakHeight"));
        }

    }

    @Override
    public TiUIView createView(Activity activity) {
        bottomSheet = new TiUIBottomSheetView(this);
        return bottomSheet;
    }

    @Kroll.method
    public void toggle() {
        bottomSheet.toggle();
    }

    @Kroll.method
    public void hide(@Kroll.argument(optional = true) HashMap options) {
        bottomSheet.collapse();
    }

    @Kroll.method
    public void close(@Kroll.argument(optional = true) HashMap options) {
        bottomSheet.collapse();
    }

    @Kroll.method
    public void show(@Kroll.argument(optional = true) HashMap options) {
        bottomSheet.expand();
    }

    @Kroll.method
    public void open(@Kroll.argument(optional = true) HashMap options) {
        bottomSheet.expand();
    }

    @Kroll.getProperty
    public boolean nestedScrolling() {
        return bottomSheet.nestedScrolling;
    }

    @Kroll.setProperty
    public void nestedScrolling(boolean value) {
        bottomSheet.setNestedScrolling(value);
    }

    @Override
    public String getApiName() {
        return "Ti.UI.BottomSheet";
    }
}
