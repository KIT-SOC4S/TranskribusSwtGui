package eu.transkribus.swt_gui.canvas;

import eu.transkribus.swt_canvas.canvas.CanvasMode;

public class TrpCanvasAddMode extends CanvasMode {
	public final static CanvasMode ADD_PRINTSPACE = new TrpCanvasAddMode(true, false, false, true, "Printspace");
	public final static CanvasMode ADD_TEXTREGION = new TrpCanvasAddMode(true, false, false, true, "Text-Region");
	public final static CanvasMode ADD_LINE = new TrpCanvasAddMode(true, false, false, true, "Line");
	public final static CanvasMode ADD_BASELINE = new TrpCanvasAddMode(true, false, false, true, "Baseline");
	public final static CanvasMode ADD_WORD = new TrpCanvasAddMode(true, false, false, true, "Word");
		
	public final static CanvasMode ADD_OTHERREGION = new TrpCanvasAddMode(true, false, false, true, "Region");
	
	
	public final static CanvasMode ADD_TABLECELL = new TrpCanvasAddMode(true, false, false, true, "TableCell");
	
	protected TrpCanvasAddMode(boolean isEditOperation, boolean highlightPointsRequired, boolean endsWithMouseUp, boolean isAddOperation, String description) {
		super(isEditOperation, highlightPointsRequired, endsWithMouseUp, isAddOperation, description);
	}
}
