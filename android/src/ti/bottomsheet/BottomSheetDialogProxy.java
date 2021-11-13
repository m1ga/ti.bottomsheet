package ti.bottomsheet;

import android.app.Activity;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import java.util.HashMap;

@Kroll.proxy(creatableInModule = TiBottomsheetModule.class,
	propertyAccessors = {
		"peakHeight"
	})
public class BottomSheetDialogProxy extends TiViewProxy
{
	private static final String TAG = "BottomSheetProxy";
	private static int id_toolbar;
	private TiUIBottomSheetDialogView bottomSheet;
	private int peakHeight = 32;

	public BottomSheetDialogProxy()
	{
		super();
		defaultValues.put("peakHeight", peakHeight);
	}

	@Override
	public void handleCreationDict(KrollDict options)
	{
		super.handleCreationDict(options);

		if (options.containsKeyAndNotNull("peakHeight")) {
			peakHeight = TiConvert.toInt(options.get("peakHeight"));
		}

	}

	@Override
	public TiUIView createView(Activity activity)
	{
		bottomSheet = new TiUIBottomSheetDialogView(this);
		return bottomSheet;
	}

	@Kroll.method
	public void show(@Kroll.argument(optional=true) HashMap options)
	{
		bottomSheet.show();
	}

	@Kroll.method
	public void open(@Kroll.argument(optional=true) HashMap options)
	{
		bottomSheet.show();
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.BottomSheet";
	}
}
