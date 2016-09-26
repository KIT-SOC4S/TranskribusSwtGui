package eu.transkribus.swt_canvas.util;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.wb.swt.SWTResourceManager;

public class Images {
	public static final Image BURGER = Images.getOrLoad("/icons/burger_16.png");
	public static final Image EYE = Images.getOrLoad("/icons/eye.png");
	public static final Image LOCK = getOrLoad("/icons/lock.png");
	public static final Image LOCK_OPEN = getOrLoad("/icons/lock_open.png");
	public static final Image DELETE = getOrLoad("/icons/delete.png");
	public static final Image CROSS = getOrLoad("/icons/cross.png");
	public static final Image ADD = Images.getOrLoad("/icons/add.png");
	public static final Image APPLICATION_DOUBLE = Images.getOrLoad("/icons/application_double.png");
	public static final Image LOADING_IMG = Images.getOrLoad("/icons/loading.gif");
	public static final Image ERROR_IMG = Images.getOrLoad("/icons/broken_image.png");
	public static final Image COMMENT = Images.getOrLoad("/icons/comment.png");
	public static final Image CONTROL_EQUALIZER = Images.getOrLoad("/icons/control_equalizer.png");
	
	public static final Image DISK1 = Images.getOrLoad("/icons/disk.png");

	public static final Image IMAGE_EDIT = Images.getOrLoad("/icons/image_edit.png");
	public static final Image IMAGE_DELETE = Images.getOrLoad("/icons/image_delete.png");

	public static final Image CONTRAST = Images.getOrLoad("/icons/contrast.png");

	public static final Image APPLICATION = getOrLoad("/icons/application.png");
	public static final Image APPLICATION_SIDE_CONTRACT = getOrLoad("/icons/application_side_contract.png");
	public static final Image APPLICATION_SIDE_EXPAND = getOrLoad("/icons/application_side_expand.png");
	public static final Image APPLICATION_SIDE_PUT = getOrLoad("/icons/application_put.png");
	public static final Image REFRESH = getOrLoad("/icons/refresh.png");

	public static final Image ARROW_UP = getOrLoad("/icons/arrow_up.png");
	public static final Image ARROW_DOWN = getOrLoad("/icons/arrow_down.png");
	public static final Image ARROW_LEFT = getOrLoad("/icons/arrow_left.png");
	public static final Image ARROW_UNDO = getOrLoad("/icons/arrow_undo.png");
	public static final Image ARROW_REDO = getOrLoad("/icons/arrow_redo.png");
	
	public static final Image BORDER_NONE = getOrLoad("/icons/border_none.png");
	public static final Image BORDER_ALL = getOrLoad("/icons/border_all.png");
	public static final Image BORDER_CLOSED = getOrLoad("/icons/border_closed.png");
	
	public static final Image BORDER_LEFT = getOrLoad("/icons/border_left.png");
	public static final Image BORDER_RIGHT = getOrLoad("/icons/border_right.png");
	public static final Image BORDER_LEFT_RIGHT = getOrLoad("/icons/border_left_right.png");
	
	public static final Image BORDER_BOTTOM = getOrLoad("/icons/border_bottom.png");
	public static final Image BORDER_TOP = getOrLoad("/icons/border_top.png");
	public static final Image BORDER_BOTTOM_TOP = getOrLoad("/icons/border_bottom_top.png");
	
	public static final Image BORDER_HORIZONTAL_CLOSED = getOrLoad("/icons/border_horizontal_closed.png");
	public static final Image BORDER_HORIZONTAL_OPEN = getOrLoad("/icons/border_horizontal_open.png");
	
	public static final Image BORDER_VERTICAL_CLOSED = getOrLoad("/icons/border_vertical_closed.png");
	public static final Image BORDER_VERTICAL_OPEN = getOrLoad("/icons/border_vertical_open.png");


	public static final Image TICK = getOrLoad("/icons/tick.png");
	public static final Image FIND = getOrLoad("/icons/find.png");

	public static final Image DISK = getOrLoad("/icons/disk.png");
	public static final Image DISK_MESSAGE = getOrLoad("/icons/disk_message.png");
	
	public static final Image PAGE_NEXT = getOrLoad("/icons/page-next.gif");
	public static final Image PAGE_PREV = getOrLoad("/icons/page-prev.gif");

	public static final Image PENCIL = getOrLoad("/icons/pencil.png");

	public static final Image GROUP = getOrLoad("/icons/group.png");

	static HashMap<String, Image> imageMap;

	public static Image getSystemImage(int swtSysImg) {
		return Display.getDefault().getSystemImage(swtSysImg);
	}

	public static Image getOrLoad(String path) {
		if (imageMap == null)
			imageMap = new HashMap<String, Image>();

		Image img = imageMap.get(path);
		if (img == null) {
			img = SWTResourceManager.getImage(Images.class, path);
			imageMap.put(path, img);
		}
		return img;
	}

	public static Image resize(Image image, int width, int height) {
		Image scaled = new Image(Display.getDefault(), width, height);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		int origX = image.getBounds().width;
		int origY = image.getBounds().height;
		
		double xScale = (double)width/origX;
        double yScale = (double)height/origY;
        double scale = Math.min(xScale, yScale);
		
        int destX = new Double(origX*scale).intValue();
        int destY = new Double(origY*scale).intValue();
		gc.drawImage(image, 0, 0, origX, origY, 0, 0, destX, destY);
		gc.dispose();
		image.dispose();
		return scaled;
	}
}
