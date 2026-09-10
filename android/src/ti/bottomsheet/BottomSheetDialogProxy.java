package ti.bottomsheet;

import android.app.Activity;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import java.util.HashMap;

@Kroll.proxy(creatableInModule = TiBottomsheetModule.class,
        propertyAccessors = {
                "peakHeight",
                "cancelable",
                "dimAmount",
                "dragIndicator"
        })
public class BottomSheetDialogProxy extends TiViewProxy {

    public BottomSheetDialogProxy() {
        super();
        defaultValues.put("dragIndicator", false);
    }

    @Override
    public TiUIView createView(Activity activity) {
        return new TiUIBottomSheetDialogView(this);
    }

    private TiUIBottomSheetDialogView dialog(boolean create) {
        TiUIView view = create ? getOrCreateView() : peekView();
        return view instanceof TiUIBottomSheetDialogView ? (TiUIBottomSheetDialogView) view : null;
    }

    @Kroll.method
    public void show(@Kroll.argument(optional = true) HashMap options) {
        open(options);
    }

    @Kroll.method
    public void open(@Kroll.argument(optional = true) HashMap options) {
        TiUIBottomSheetDialogView dialog = dialog(true);
        if (dialog != null) {
            dialog.show();
        }
    }

    @Kroll.method
    public void hide(@Kroll.argument(optional = true) HashMap options) {
        close(options);
    }

    @Kroll.method
    public void close(@Kroll.argument(optional = true) HashMap options) {
        TiUIBottomSheetDialogView dialog = dialog(false);
        if (dialog != null) {
            dialog.hide();
        }
    }

    @Override
    public String getApiName() {
        return "ti.bottomsheet.BottomSheetDialog";
    }
}
