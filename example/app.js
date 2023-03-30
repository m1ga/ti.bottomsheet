const win = Ti.UI.createWindow();
const BottomSheet = require("ti.bottomsheet");
const lbl = Ti.UI.createLabel({
	text: "click to open"
});
const bs = BottomSheet.createBottomSheetDialog({
	backgroundColor: "#ffffff",
	dimAmount: 0.75,
	cancelable: true
})
const view = Ti.UI.createView({
	height: 300,
	width: Ti.UI.FILL,
	backgroundColor: "#ffffff"
})
win.add([lbl, bs]);
bs.add(view);
win.addEventListener("click", function() {
	bs.open();
})
bs.addEventListener("close", function(e) {
	console.log("close");
})
bs.addEventListener("stateChanged", function(e) {
	console.log("stateChanged");
})

win.open();
