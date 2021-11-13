package ti.bottomsheet;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;


@Kroll.module(name="TiBottomsheet", id="ti.bottomsheet")
public class TiBottomsheetModule extends KrollModule
{

	// Standard Debugging variables
	private static final String LCAT = "TiBottomsheetModule";
	private static final boolean DBG = TiConfig.LOGD;

	// You can define constants with @Kroll.constant, for example:
	@Kroll.constant public static final int STATE_DRAGGING = 1;
	@Kroll.constant public static final int STATE_SLIDING = 2;
	@Kroll.constant public static final int STATE_OPEN = 3;
	@Kroll.constant public static final int STATE_PEAK = 4;
	@Kroll.constant public static final int STATE_CLOSE = 5;


	public TiBottomsheetModule()
	{
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{

	}

}
