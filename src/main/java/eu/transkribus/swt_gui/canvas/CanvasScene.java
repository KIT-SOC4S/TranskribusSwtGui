package eu.transkribus.swt_gui.canvas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.pagecontent.TextStyleType;
import eu.transkribus.core.model.beans.pagecontent_trp.ITrpShapeType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpBaselineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpPrintSpaceType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTableCellType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextLineType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpTextRegionType;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpWordType;
import eu.transkribus.core.util.CoreUtils;
import eu.transkribus.core.util.TextStyleTypeUtils;
import eu.transkribus.swt.util.Colors;
import eu.transkribus.swt_gui.canvas.editing.ShapeEditOperation;
import eu.transkribus.swt_gui.canvas.editing.ShapeEditOperation.ShapeEditType;
import eu.transkribus.swt_gui.canvas.listener.ICanvasSceneListener;
import eu.transkribus.swt_gui.canvas.listener.ICanvasSceneListener.SceneEvent;
import eu.transkribus.swt_gui.canvas.listener.ICanvasSceneListener.SceneEventType;
import eu.transkribus.swt_gui.canvas.shapes.CanvasPolyline;
import eu.transkribus.swt_gui.canvas.shapes.CanvasQuadPolygon;
import eu.transkribus.swt_gui.canvas.shapes.ICanvasShape;
import eu.transkribus.swt_gui.dialogs.ChangeReadingOrderDialog;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.settings.TrpSettings;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;
import eu.transkribus.swt_gui.table_editor.TableUtils;
import eu.transkribus.swt_gui.util.GuiUtil;

/**
 * The scene contains all objects to be drawn, i.e. the main image, sub images
 * and a set of shapes stored in a TreeSet s.t. they are ordered according to
 * their position. <br>
 * Note that images are not intended to be actually stored in the scene - it
 * just stores a reference to them. You have to create and dispose them by
 * yourself!
 */
public class CanvasScene {
	private final static Logger logger = LoggerFactory.getLogger(CanvasScene.class);
	
	public static void updateParentShape(ITrpShapeType st) {
			ICanvasShape shape = GuiUtil.getCanvasShape(st);
			if (shape == null)
				return;
					
			if (st.getParentShape() != null) {
				ICanvasShape pShape = (ICanvasShape) st.getParentShape().getData();
	//			logger.debug("parent shape: "+pShape);
				shape.setParentAndAddAsChild(pShape);
			} else {
				shape.setParent(null);
			}
		}

	public static void updateParentInfo(ICanvasShape shape, boolean recursive) {
		ITrpShapeType st = GuiUtil.getTrpShape(shape);
		if (st == null)
			return;
		
		updateParentShape(st);
		if (recursive) {
			for (ITrpShapeType childSt : st.getChildren(recursive)) {
				updateParentShape(childSt);
			}
		}
	}

	protected SWTCanvas canvas;
	protected CanvasImage mainImage = null;
	protected int mainImageWidth = 0;
	protected int mainImageHeight = 0;

	protected HashSet<Image> subimages = new HashSet<Image>();

//	protected TreeSet<ICanvasShape> shapes = new TreeSet<ICanvasShape>();
	protected List<ICanvasShape> shapes = new ArrayList<ICanvasShape>();

	protected List<ICanvasShape> selected = new ArrayList<ICanvasShape>();
	
	boolean allRO = false;
	boolean regionsRO = false;
	boolean linesRO = false;
	boolean wordsRO = false;
	
	boolean transcriptionMode = false;
	
	protected List<ICanvasSceneListener> sceneListener = new ArrayList<>();
	
	public CanvasScene(SWTCanvas canvas) {
		this.canvas = canvas;
	}

	// public void loadMainImage(String urlStr) throws Exception {
	// try {
	// loadMainImage(new URL(urlStr));
	// } catch (MalformedURLException e) {
	// loadMainImage(new URL("file://"+urlStr));
	// }
	// }

	// public void loadMainImage(URL url) throws Exception {
	// CanvasImage im = imCache.getOrPut(url);
	//
	// setMainImage(im);
	// }

	/** Sets the reference to the main image to null */
	public void clearMainImage() {
		setMainImage(null);
	}

	/** Sets the reference to the main image */
	public void setMainImage(CanvasImage img) {
		mainImage = img;
		if (mainImage != null)
			logger.debug("nr of pixels in loaded image = " + mainImage.nPixels);
	}

	public CanvasImage getMainImage() {
		return mainImage;
	}

	public boolean hasDataToPaint() {
		return mainImage != null;
	}

	public void paint(GC gc) {
		// Draw main image:
		if (mainImage != null && !mainImage.isDisposed())
			mainImage.paint(gc, canvas);
		else {
			logger.debug("mainImage: " + mainImage + ", isDisposed: " + mainImage.isDisposed());
		}

		// Draw sub images:
		for (Image image : subimages) {
			gc.drawImage(image, 0, 0);
		}

		// Draw shapes:
		// first draw non-selected shapes:
		for (ICanvasShape s : shapes) {
			
			//during transcription only the selected baseline is drawn, lines and regions are always drawn if visible
			if (isTranscriptionMode()) {
				ITrpShapeType trpShape = (ITrpShapeType) s.getData();
				if (!(trpShape instanceof TrpRegionType) && !(trpShape instanceof TrpTextLineType))
					continue;
			}
			
			if (s.isVisible() && !selected.contains(s)){
				s.draw(canvas, gc);
			}
		}
		
		// then draw selected shape over them (to make borders visible for
		// overlapping regions!):
		for (ICanvasShape s : selected) {
			if (s.isVisible())
				s.draw(canvas, gc);
		}

//		for (ICanvasShape s : readingOrderShapes) {
//			//if (s.isVisible())
//				s.draw(canvas, gc);
//		}
		
		
		// TEST - draw border types of table cells
		for (ICanvasShape s : shapes) {
			TrpTableCellType c = TableUtils.getTableCell(s);
			if (c != null) {
				CanvasQuadPolygon qp = (CanvasQuadPolygon) s;
				CanvasSettings sets = canvas.getSettings();
				
				for (int i=0; i<4; ++i) {
					List<java.awt.Point> pts = qp.getPointsOfSegment(i, true);
					int[] ptArr = CoreUtils.getPointArray(pts);

					if (i == 0 && c.isLeftBorderVisible() || i == 1 && c.isBottomBorderVisible() || i == 2 && c.isRightBorderVisible()
							|| i == 3 && c.isTopBorderVisible()) {
						gc.setAlpha(sets.getForegroundAlpha());
						if (s.isSelected()) { // if selected:
							gc.setLineWidth(sets.getSelectedLineWidth() + 4); // set selected line with
							gc.setBackground(s.getColor()); // set background color
						} else {
							gc.setLineWidth(sets.getDrawLineWidth() + 4);
							gc.setBackground(s.getColor());
						}
						gc.setForeground(s.getColor());
						
						// TEST
						gc.setBackground(Colors.getSystemColor(SWT.COLOR_BLACK));
						gc.setForeground(Colors.getSystemColor(SWT.COLOR_BLACK));
						
						gc.setLineStyle(canvas.getSettings().getLineStyle());
	
						gc.setAlpha(sets.getForegroundAlpha());
						gc.drawPolyline(ptArr);
	
					}
				}
			}
		}
		// END OF TEST
		
	}
	

	/**
	 * Returns the bounds of the <em>original</em> image, i.e. not the bounds of
	 * the probably scaled one (which would be getMainImage().img.getBounds())
	 */
	public Rectangle getBounds() {
		if (mainImage != null && !mainImage.isDisposed())
			return mainImage.getBounds();
		else
			return new Rectangle(0, 0, 0, 0);
	}

	public Point getCenter() {
		return new Point(getBounds().width / 2, getBounds().height / 2);
	}

	// public TreeSet<ICanvasShape> getShapes() {
	// return shapes;
	// }
	public List<ICanvasShape> getShapes() {
		return shapes;
	}
	
//	public List<ICanvasShape> getShapesInReversedOrder() {
//		Collections.reverse(shapes);
//		List<ICanvasShape> reversedShapes = shapes;
//		sortShapes();
//		return reversedShapes;
//	}

	public ICanvasShape[] getShapesArray() {
		return (ICanvasShape[]) shapes.toArray();
	}
	
	public void sortShapes() {
		Collections.sort(shapes);
//		logger.debug("sorted shapes: ");
//		for (ICanvasShape s : shapes) {
//			logger.debug("s = "+s);
//			
//		}
	}
	
	public ShapeEditOperation mergeSelected(boolean sendSignal) {
		List<ICanvasShape> selectedShapes = getSelectedAsNewArray();
		if (selectedShapes.size() < 2)
			return null;
		
		logger.debug("merging "+selectedShapes.size()+" shapes");
		
		if (sendSignal) {
			if (notifyOnBeforeShapesMerged(selectedShapes))
				return null;
		}
		 				
		clearSelected();
		ICanvasShape merged = selectedShapes.get(0).copy();
		
		for (int i=1; i<selectedShapes.size(); ++i) {
			merged = merged.merge(selectedShapes.get(i));
			logger.debug("merged = "+merged);
			if (merged == null)
				return null;
			
			removeShape(selectedShapes.get(i), false, false);
			for (ICanvasShape child : selectedShapes.get(i).getChildren(false)) {
				merged.addChild(child);
			}
		}
		
		removeShape(selectedShapes.get(0), false, false);
		ShapeEditOperation opa = addShape(merged, null, false);
//		logger.debug("merge added: "+opa);
		
		if (opa == null) {
			addShape(selectedShapes.get(0), null, false);
			logger.warn("unable to add merged shape: "+merged);
			return null;
		}
		
		ShapeEditOperation op = 
				new ShapeEditOperation(ShapeEditType.MERGE, selectedShapes.size()+" shapes merged", selectedShapes);
		op.addNewShape(merged);
		
		if (sendSignal) {
			notifyOnShapesMerged(op);
		}
		
		canvas.redraw();
		
		return op;
		
	}
	
	/**
	 * Splits the given shape by the line running through [x1,y1], [x1, y2]
	 * @param shape The shape to split
	 * @param sendSignal True if event signal shall be sent
	 * @param p1 The parent shape for the first (left) split; if null, the parent shape of the splitted shape is used
	 * @param p2 The parent shape for the second (right) split; if null, the parent shape of the splitted shape is used
	 * @param isFollowUp Indicates that this is a follow-up split, i.e. a split occuring from splitting a parent shape!
	 * @return A ShapeEditOperation object that contains information on the performed split or null if there was some error
	 */
	public ShapeEditOperation splitShape(ICanvasShape shape, CanvasPolyline pl, boolean sendSignal, ICanvasShape p1, ICanvasShape p2, boolean isFollowUp) {
		if (shape == null)
			return null;
		
		logger.debug("splitting shape "+shape);
		ShapeEditOperation op = new ShapeEditOperation(ShapeEditType.SPLIT, "Shape splitted", shape);
		op.setFollowUp(isFollowUp);
		
		if (sendSignal) {
			if (notifyOnBeforeShapeSplitted(op))
				return null;
		}
		
		Pair<ICanvasShape, ICanvasShape> splits = shape.splitByPolyline(pl);
		logger.debug("splits "+splits);
		if (splits == null)
			return null;
		
		ICanvasShape s1 = splits.getLeft();
		ICanvasShape s2 = splits.getRight();
		
		// remove old shape from parent and set parent for new shapes; also, remove children from new shapes:
		shape.removeFromParent();
		
		if (p1 == null)
			p1 = shape.getParent();
		if (p2 == null)
			p2 = shape.getParent();
		
		s1.setParentAndAddAsChild(p1);
		s2.setParentAndAddAsChild(p2);			
		
		s1.removeChildren();
		s2.removeChildren();		

		// split up children between the two split shapes s1, s2	
		for (ICanvasShape child : shape.getChildren(false)) {
			// partition children upon splits by 1st area of intersection and 2nd distance to the center of the split shapes
			double a1 = child.intersectionArea(s1);
			double a2 = child.intersectionArea(s2);
			if (a1 < a2) {
				child.setParentAndAddAsChild(s2);
			} else if (a1 > a2) {
				child.setParentAndAddAsChild(s1);
			} else {				
				double d1 = child.distanceToCenter(s1);
				double d2 = child.distanceToCenter(s2);
				if (d1 < d2) {
					child.setParentAndAddAsChild(s1);
				} else {
					child.setParentAndAddAsChild(s2);
				}
			}
		}
				
		logger.debug("splitted, shape n-children: "+shape.getChildren(false).size());
		logger.debug("splitted, s1 n-children: "+s1.getChildren(false).size());
		logger.debug("splitted, s2 n-children: "+s2.getChildren(false).size());
		
		op.addNewShape(s1);
		op.addNewShape(s2);
				
		// add the new shapes to the canvas:
		addShape(s1, p1, false);
		addShape(s2, p2, false);
		// remove the old shape from the canvas:
		removeShape(shape, false, false);
		
		if (sendSignal) {
			notifyOnShapeSplitted(op);
		}
		
		return op;
	}
	
	public ShapeEditOperation addShape(ICanvasShape newShape, ICanvasShape parentShape, boolean sendSignal) {
		if (sendSignal) {
			if (notifyOnBeforeShapeAdded(newShape))
				return null;
		}
		
		if (!shapes.contains(newShape)) { // dont add the same shape twice
			shapes.add(newShape);
			newShape.setParentAndAddAsChild(parentShape);
			
			sortShapes();
			
			if (sendSignal) {
				notifyOnShapeAdded(newShape);
			}

			return new ShapeEditOperation(ShapeEditType.ADD, "Shape added", newShape);
		} else {
			logger.warn("Could not add shape: " + newShape);
			return null;			
		}
	}

	public boolean removeShape(ICanvasShape shape, boolean removeChildren, boolean sendSignal) {
		if (shape==null)
			return false;
		
		if (sendSignal) {
			if (notifyOnBeforeShapeRemoved(shape))
				return false;
		}
		
		boolean wasRemoved = shapes.remove(shape);
//		logger.debug("after removed, contains = "+shapes.contains(shape));
		if (wasRemoved) {
			shape.removeFromParent();
			selected.remove(shape);
			shape.setSelected(false);
			
			if (removeChildren) {
				List<ICanvasShape> children = shape.getChildren(true);
	//			logger.debug("removing, nr of children: "+children.size());
				for (ICanvasShape c : children) {
					shapes.remove(c);
					selected.remove(c);
					c.setSelected(false);
				}
			}
		} else {
			logger.warn("Could not remove shape: " + shape
					+ " (maybe its parent was removed at the same time and thus this element was removed automatically before?)");
		}
		sortShapes();
		
		if (wasRemoved && sendSignal)
			notifyOnShapeRemoved(shape);
		return wasRemoved;
	}

	public boolean moveShape(ICanvasShape shape, int tx, int ty, boolean sendSignal) {
		if (sendSignal) {
			if (notifyOnBeforeShapeMoved(shape, tx, ty))
				return false;
		}

		shape.translate(tx, ty);

		if (sendSignal)
			notifyOnShapeMoved(shape, tx, ty);

		return true;
	}

	public boolean hasShape(ICanvasShape shape) {
		return shape!=null && shapes.contains(shape);
	}
	
	public void clear() {
		clearShapes();
		clearMainImage();
	}
	
	public void clearShapes() {
		selectObject(null, true, false);
		shapes.clear();
		clearSelected();
		
		canvas.getUndoStack().clear();
	}

	public int nShapes() {
		return shapes.size();
	}

	public boolean addSubImage(Image image) {
		return subimages.add(image);
	}

	public boolean removeSubImage(Image image) {
		return subimages.remove(image);
	}

	public boolean hasSubImage(Image image) {
		return subimages.contains(image);
	}

	public void removeAllImages() {
		subimages.clear();
		clearMainImage();
	}
	
	public void clearSelected() {
		for (ICanvasShape sel : shapes) {
			sel.setSelected(false);
		}		
		for (ICanvasShape sel : selected) {
			sel.setSelected(false);
		}
		selected.clear();
	}	
	
//	public void clearReadingOrder() {
//		for (ICanvasShape sel : shapes) {
//			if (sel.isShowReadingOrder()){
//				sel.setShowReadingOrder(!sel.isShowReadingOrder());
//			}
//		}
//	}

//	private void deselectAll() {
//		clearSelected();
//	}

	/**
	 * Iterates through all shapes and returns the list of selected objects
	 */
	public List<ICanvasShape> getSelectedAsNewArray() {
		return new ArrayList<ICanvasShape>(selected);
	}
	
	public List<Object> getSelectedData() {
		List<Object> sd = new ArrayList<Object>();
		for (ICanvasShape s : selected) {
			sd.add(s.getData());
		}
		return sd;
	}
	
	public <T> List<T> getSelectedData(Class<T> clazz) {
		List<T> sd = new ArrayList<>();
		for (Object o : getSelectedData()) {
			if (clazz.isAssignableFrom(o.getClass()))
//			if (o.getClass().equals(clazz))
				sd.add((T) o);
		}
		return sd;
	}
	
	public <T> List<ICanvasShape> getSelectedShapesWithData(Class<T> clazz) {
		List<ICanvasShape> sd = new ArrayList<>();
		for (ICanvasShape s : getSelectedAsNewArray()) {
			if (s.getData()!=null && clazz.isAssignableFrom(s.getData().getClass()))
				sd.add(s);
		}
		return sd;
	}
	
	

	/**
	 * Iterates through all shapes and returns the list of selected objects
	 * sorted by time of selection
	 */
	// public List<ICanvasShape> getSelectedSortedByTime() {
	// List<ICanvasShape> sel = getSelected();
	// Collections.sort(sel, new Comparator<ICanvasShape>() {
	// @Override
	// public int compare(ICanvasShape o1, ICanvasShape o2) {
	// return new Long(o1.getSelectedTime()).compareTo(new
	// Long(o2.getSelectedTime()));
	// }
	// });
	// return sel;
	// }

	/** Clears selection out of all shapes except the first selected item */
	public void clearMultiSelection() {
		if (selected.isEmpty())
			return;
		
		ICanvasShape firstSel = selected.get(0);
		clearSelected();
		selected.add(firstSel);
	}

	/**
	 * Returns the edit focused element, i.e. the first or last selected element
	 * when multiple shapes are selected. Whether the first or last element is returned
	 * depends on the focusFirstSelected property in the CanvasSettings
	 */
	public ICanvasShape getEditFocused() {
		return (canvas.getSettings().isFocusFirstSelected()) ? getFirstSelected() : getLastSelected();
	}

	public boolean isEditFocused(ICanvasShape shape) {
		if (shape == null)
			return false;

		return getEditFocused() == shape;
	}

	/**
	 * Returns the shape that was selected first when using multiselect
	 */
	public ICanvasShape getFirstSelected() {
		if (selected.isEmpty())
			return null;
		else
			return selected.get(0);

		// OLD VERSION:
		// return first in list of selected objects or return null if list is
		// empty:
		// List<ICanvasShape> sel = getSelectedSortedByTime();
		// if (sel.size()>0) {
		// return sel.get(0);
		// }
		// return null;
	}

	/**
	 * Returns the shape that was selected last when using multiselect
	 */
	public ICanvasShape getLastSelected() {
		if (selected.isEmpty())
			return null;
		else
			return selected.get(selected.size() - 1);

		// OLD VERSION:
		// return first in list of selected objects or return null if list is
		// empty:
		// List<ICanvasShape> sel = getSelectedSortedByTime();
		// if (sel.size()>0) {
		// return sel.get(sel.size()-1);
		// }
		// return null;
	}

	/**
	 * Returns the shape containing the specified date or null if not found
	 */
	public ICanvasShape findShapeWithData(Object data) {
		for (ICanvasShape shape : getShapes()) {
			if (shape.getData() == data)
				return shape;
		}
		return null;
	}

	/**
	 * Finds the shape that overlaps with the given shape and has the specified
	 * dataType.<br>
	 * Returns null if no such shape is found. <br>
	 * If two or more overlapping shapes are found the one whose
	 * <em>bounding box</em> overlaps most is returned.
	 */
	public <T> ICanvasShape findOverlappingShapeWithDataType(ICanvasShape shape, Class<T> dataType) {
		ICanvasShape maxShape = null;
		double maxArea = Double.MIN_VALUE;

		for (ICanvasShape s : getShapes()) {
			// logger.debug("dataType: "+s.getData().getClass()+", assform: "+dataType.isAssignableFrom(s.getData().getClass()));
			if (s.getData() == null || !(dataType.isAssignableFrom(s.getData().getClass())))
				continue;

			double area = s.intersectionArea(shape);
			if (area > 0 && area > maxArea) {
				maxArea = area;
				maxShape = s;
			}
		}
		return maxShape;
	}

	public ICanvasShape selectObjectWithData(Object data, boolean sendSignal, boolean multiselect) {
		ICanvasShape shape = findShapeWithData(data);
		if (shape != null) {
			selectObject(shape, sendSignal, multiselect);
		}
		return shape;
	}

	public void makeShapeVisible(ICanvasShape shape) {
		if (shape != null && !isShapeVisible(shape)) {
			canvas.focusShape(shape);
		}
		canvas.redraw();
	}

	public boolean isShapeVisible(ICanvasShape shape) {
		if (shape == null) return false;
		
		java.awt.Rectangle r = canvas.getPersistentTransform().transform(shape.getBounds());

		return (canvas.getClientArea().contains(r.x, r.y) && canvas.getClientArea().contains(r.x + r.width, r.y)
				&& canvas.getClientArea().contains(r.x + r.width, r.y + r.height) && canvas.getClientArea().contains(r.x, r.y + r.height));
	}
	
	/**
	 * Selects the given shape if it is contained in this scene
	 * 
	 * @param sendSignal
	 *            Determines a selection event is fired
	 */
	public void selectObject(ICanvasShape shape, boolean sendSignal, boolean multiselect) {
		if (!multiselect) {
			clearSelected();
		}
		
		
		if (hasShape(shape)) {
			logger.debug("selecting, sendSignal: "+sendSignal+", multiselect: " + multiselect+", isSelected: "+shape.isSelected());
			shape.setSelected(!shape.isSelected());
			if (shape.isSelected()){
				selected.add(shape);
				
			}
			else{
				
				selected.remove(shape);
			}
		}

		if (sendSignal)
			notifyOnSelectionChanged(shape);
	}

	/**
	 * Selects the given shape if it is contained in this scene
	 * 
	 * @param sendSignal
	 *            Determines a selection event is fired
	 */
//	public void selectObject(ICanvasShape shape, boolean sendSignal, boolean multiselect) {
//		if (!multiselect) {
//			clearSelected();
//			clearReadingOrder();
//			readingOrderShapes.clear();
//		}
//		
//		if( selected.size() == 0){
//			//delete the reading order shapes for text regions when no shape was selected
//			clearReadingOrder();
//			readingOrderShapes.clear();
//		}
////		if (shape == null && !multiselect) {
////			clearSelected();
////		} 
//		if (hasShape(shape)) {
//			logger.debug("selecting, sendSignal: "+sendSignal+", multiselect: " + multiselect+", isSelected: "+shape.isSelected());
//			shape.setSelected(!shape.isSelected());
//			if (shape.isSelected()){
//				selected.add(shape);
//				
//				//add reading order shapes (for all child shapes of the selected shape) to the canvas
//				ITrpShapeType trpShape = (ITrpShapeType) shape.getData();
//
//				for (int i = 0; i < trpShape.getChildren(false).size(); i++){
//					ITrpShapeType currChild = trpShape.getChildren(false).get(i);
//					ICanvasShape cs = (ICanvasShape)currChild.getData();
//					cs.setShowReadingOrder(true);
////					java.awt.Rectangle boundingRect = cs.getBounds();
////					CanvasRect cr = new CanvasRect(boundingRect.x-30, boundingRect.y, 20, 20);
////					if (!readingOrderShapes.contains(cr)){
////						readingOrderShapes.add(cr);
////					}
//				}
//
//			}
//			else{
//				
//				selected.remove(shape);
//
//				//remove all shape childs of the deselected shapes
//				List<ICanvasShape> children = shape.getChildren(true);
//				for (ICanvasShape c : children) {
//					c.setShowReadingOrder(false);
//				}
//			}
//		}
//
//		if (sendSignal)
//			notifyOnSelectionChanged(shape);
//	}
		
	public List<ICanvasShape> selectObjects(Rectangle rect, boolean sendSignal, boolean multiselect) {
		return selectObjects(rect.x, rect.y, rect.width, rect.height, sendSignal, multiselect);
	}
	
	public List<ICanvasShape> selectObjects(int x, int y, int w, int h, boolean sendSignal, boolean multiselect) {
		if (!multiselect) 
			clearSelected();
		
		List<ICanvasShape> selected = new ArrayList<ICanvasShape>();
//		boolean first=true;
		for (ICanvasShape s : shapes) {
			if (!s.isVisible() || !s.isSelectable())
				continue;			
			
			boolean intersects = s.intersects(new java.awt.Rectangle(x, y, w, h));
			
			if (intersects) {
				selected.add(s);
				
				selectObject(s, sendSignal, true);
//				first = false;
			}
		}
		return selected;
	}

	/**
	 * Selects an object by coordinates <em>without any transformation</em> (has
	 * to inverted before!)
	 * 
	 * @param sendSignal Determines if a selection event is fired
	 */
	public ICanvasShape selectObject(int x, int y, boolean sendSignal, boolean multiselect) {
		ICanvasShape minShape = null;
		double minDist = Double.MAX_VALUE;	
		boolean found = false;
		for (ICanvasShape s : shapes) {
			
			if (s.isReadingOrderVisible() && s.getReadingOrderCircle().contains(x, y) && !found){
				//logger.debug("reading order selected is true for mouse point " + x + " , " + y );
				//Display display = canvas.getDisplay();
				Shell shell = canvas.getShell();
				ChangeReadingOrderDialog diag = new ChangeReadingOrderDialog(shell);
				String changedRo = diag.open(x, y);
				if (changedRo != null && !changedRo.equals("")){
					logger.debug(" new reading order is " + changedRo);
					notifyOnReadingOrderChanged(s, changedRo);
				}
				found = true;
			}
			
			if (!s.isVisible() || !s.isSelectable())
				continue;

			double dist = s.distance(x, y, true);

			if (dist < 0) { // dist < 0 --> inside!
			 logger.debug("s.data = "+s.getData().toString()+" dist = "+dist+" level = "+s.getLevel());

				dist *= -1;
				// prioritize shapes with a larger level:
				dist += (100 - s.getLevel()) * 1e5;
				logger.debug("dist after prior: "+dist);

				if (dist < minDist) {
					minDist = dist;
					minShape = s;
				}
			}
		}

		selectObject(minShape, sendSignal, multiselect);
		return minShape;
	}
	
	public boolean isTranscriptionMode() {
		return transcriptionMode;
	}

	public void setTranscriptionMode(boolean transcriptionMode) {
		this.transcriptionMode = transcriptionMode;
	}

	public void setAllEditable(boolean val) {
		for (ICanvasShape s : getShapes()) {
			s.setEditable(val);
		}
		canvas.redraw();
	}

	// event stuff:
	public void addCanvasSceneListener(ICanvasSceneListener listener) {
		sceneListener.add(listener);
	}

	public void removeCanvasSceneListener(ICanvasSceneListener listener) {
		sceneListener.remove(listener);
	}

	public boolean notifyOnSelectionChanged(ICanvasShape shape) {
		SceneEvent e = new SceneEvent(SceneEventType.SELECTION_CHANGED, this, shape);
		canvas.onSelectionChanged(shape);
		return notifyAllListener(e);
	}
	
	public boolean notifyOnBeforeUndo(ShapeEditOperation op) {
		return notifyAllListener(new SceneEvent(SceneEventType.BEFORE_UNDO, this, op));
	}

	public boolean notifyOnUndo(ShapeEditOperation op) {
		return notifyAllListener(new SceneEvent(SceneEventType.UNDO, this, op));
	}	

	public boolean notifyOnBeforeShapeAdded(ICanvasShape shape) {
		return notifyAllListener(new SceneEvent(SceneEventType.BEFORE_ADD, this, shape));
	}

	public boolean notifyOnShapeAdded(ICanvasShape shape) {
		return notifyAllListener(new SceneEvent(SceneEventType.ADD, this, shape));
	}

	public boolean notifyOnBeforeShapeRemoved(ICanvasShape shape) {
		SceneEvent e = new SceneEvent(SceneEventType.BEFORE_REMOVE, this, shape);
		return notifyAllListener(e);
	}

	public boolean notifyOnShapeRemoved(ICanvasShape shape) {
		SceneEvent e = new SceneEvent(SceneEventType.REMOVE, this, shape);
		return notifyAllListener(e);
	}

	public boolean notifyOnBeforeShapeMoved(ICanvasShape shape, int tx, int ty) {
		SceneEvent e = new SceneEvent(SceneEventType.BEFORE_MOVE, this, shape);
		e.data = new Point(tx, ty);
		return notifyAllListener(e);
	}

	public boolean notifyOnShapeMoved(ICanvasShape shape, int tx, int ty) {
		SceneEvent e = new SceneEvent(SceneEventType.MOVE, this, shape);
		e.data = new Point(tx, ty);
		return notifyAllListener(e);
	}
	
	public boolean notifyOnBeforeShapeSplitted(ShapeEditOperation op) {
		SceneEvent e = new SceneEvent(SceneEventType.BEFORE_SPLIT, this, op);
		return notifyAllListener(e);
	}

	public boolean notifyOnShapeSplitted(ShapeEditOperation op) {
		SceneEvent e = new SceneEvent(SceneEventType.SPLIT, this, op);
		return notifyAllListener(e);
	}
	
	public boolean notifyOnAfterShapeSplitted(ShapeEditOperation op) {
		SceneEvent e = new SceneEvent(SceneEventType.AFTER_SPLIT, this, op);
		return notifyAllListener(e);
	}
	
	public boolean notifyOnBeforeShapesMerged(List<ICanvasShape> merged) {
		SceneEvent e = new SceneEvent(SceneEventType.BEFORE_MERGE, this, merged);
		return notifyAllListener(e);
	}
	
	public boolean notifyOnReadingOrderChanged(ICanvasShape changed, String newRo) {
		SceneEvent e = new SceneEvent(SceneEventType.READING_ORDER_CHANGED, this, changed);
		e.data = newRo;
		return notifyAllListener(e);
	}

	public boolean notifyOnShapesMerged(ShapeEditOperation op) {
		SceneEvent e = new SceneEvent(SceneEventType.MERGE, this, op);
		return notifyAllListener(e);
	}

	public void updateSegmentationViewSettings() {
		TrpSettings sets = TrpMainWidget.getInstance().getTrpSets();
		logger.trace("trpsets: " + sets.toString());
	
		for (ICanvasShape s : getShapes()) {
			if (s.hasDataType(TrpPrintSpaceType.class)) {
				s.setVisible(sets.isShowPrintSpace());
			}
			if (s.hasDataType(TrpTextRegionType.class)) {
				s.setVisible(sets.isShowTextRegions());
			}
			if (s.hasDataType(TrpTextLineType.class)) {
				s.setVisible(sets.isShowLines());
			}
			if (s.hasDataType(TrpBaselineType.class)) {
				s.setVisible(sets.isShowBaselines());
				s.setBaselineVisibiliy(!sets.isShowOnlySelectedBaseline());
			}
			if (s.hasDataType(TrpWordType.class)) {
				s.setVisible(sets.isShowWords());
			}
		}
		
		canvas.redraw();
	}

	public ICanvasShape selectObjectWithId(String id, boolean sendSignal, boolean multiselect) {
		for (ICanvasShape s : getShapes()) {
			if (s.getData() instanceof ITrpShapeType) {
				ITrpShapeType st = (ITrpShapeType) s.getData();
				if (st.getId().equals(id)) {
					selectObject(s, sendSignal, multiselect);
					return s;
				}
			}
		}
		return null;
	}

	public TextStyleType getCommonTextStyleOfSelected() {
		TextStyleType textStyle = null;
		for (int i=0; i<selected.size(); ++i) {
			ITrpShapeType st = GuiUtil.getTrpShape(selected.get(i));
			if (st == null)
				continue;
			
			if (textStyle == null) {
				textStyle = st.getTextStyle();
			}
			else {
				textStyle = TextStyleTypeUtils.mergeEqualTextStyleTypeFields(textStyle, st.getTextStyle());
			}
		}
		
		return textStyle;
	}
	
	/** Returns true is stopping is requested. **/
	public boolean notifyAllListener(SceneEvent e) {
		boolean stop = false;
		
		for (ICanvasSceneListener sl : sceneListener) {
			if (sl.triggerEventMethod(sl, e)) {
				stop = true;
			}
		}
		return stop;
	}

	public int getNSelected() {
		return selected.size();
	}

	public void updateAllShapesParentInfo() {
		Storage store = Storage.getInstance();
		
		if (!store.hasTranscript() || store.getTranscript().getPage()==null)
			return;
		
		for (ICanvasShape s : shapes) {
			s.setParent(null);
			s.removeChildren();
		}
		
		for (ITrpShapeType st : store.getTranscript().getPage().getAllShapes(true)) {
			updateParentShape(st);
		}
	}

	public List<TrpTableCellType> getSelectedTableCells() {
		return getSelectedData(TrpTableCellType.class);
	}

	public List<ICanvasShape> getSelectedTableCellShapes() {
		return getSelectedShapesWithData(TrpTableCellType.class);
	}

}
