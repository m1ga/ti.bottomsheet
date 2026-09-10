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
const view = Ti.UI.createScrollView({
	height: 300,
	width: Ti.UI.FILL,
	backgroundColor: "#ffffff",
	bottom: 5
})

const tf = Ti.UI.createTextField({
	width: 200,
	backgroundColor:"#333",
	bottom: 4
})
win.add(lbl);
bs.add(view);
view.add(tf);
win.addEventListener("click", function() {
	bs.open();
})
bs.addEventListener("close", function(e) {
	console.log("close");
})
bs.addEventListener("stateChanged", function(e) {
	console.log("stateChanged");
	tf.bottom = 10
})

win.open();
