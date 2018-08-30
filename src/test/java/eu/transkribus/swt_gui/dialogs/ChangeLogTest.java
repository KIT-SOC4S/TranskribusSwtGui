package eu.transkribus.swt_gui.dialogs;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

public class ChangeLogTest {

	public static void main(String[] args) {
		ApplicationWindow aw = new ApplicationWindow(null) {
			@Override
			protected Control createContents(Composite parent) {

				getShell().setSize(600, 600);
				ChangeLogDialog changelog = new ChangeLogDialog(getShell(), SWT.NONE);
				changelog.open();
				return parent;
			}
		};
		aw.setBlockOnOpen(false);
		aw.open();

		Display.getCurrent().dispose();
	}

}
