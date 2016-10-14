package eu.transkribus.swt_gui.search.text_and_tags;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.core.model.beans.TrpTranscriptMetadata;
import eu.transkribus.core.model.beans.customtags.CustomTag;
import eu.transkribus.core.model.beans.pagecontent_trp.TrpPageType;
import eu.transkribus.swt.progress.ProgressBarDialog;
import eu.transkribus.swt.util.CustomTagPropertyTable;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

class TagNormalizationWidget extends Composite {
	private final static Logger logger = LoggerFactory.getLogger(TagNormalizationWidget.class);
	
	CustomTagPropertyTable propertyTable;
	Button normalizeBtn;
	Label label;
	
	List<CustomTag> selectedTags;

	public TagNormalizationWidget(Composite parent, int style) {
		super(parent, style);
		this.setLayout(new GridLayout(1, false));
		
		label = new Label(this, 0);
		label.setText("Normalization: ");
		
		propertyTable = new CustomTagPropertyTable(this, 0);
		propertyTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		normalizeBtn = new Button(this, 0);
		normalizeBtn.setText("Normalize selected tags!");
		normalizeBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		normalizeBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				normalizeSelectedTags();
			}
		});
	}
	
	static boolean isSameTags(Collection<CustomTag> tags) {
		String tn = null;
		for (CustomTag t : tags) {
			if (tn == null)
				tn = t.getTagName();
			else if (!tn.equals(t.getTagName())){
				return false;
			}
		}
		return true;
	}
	
	void normalizeSelectedTags() {
		if (selectedTags == null)
			return;
		
		final Storage s = Storage.getInstance();
		
		try {
			ProgressBarDialog.open(getShell(), new IRunnableWithProgress() {
				
				@Override public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//					monitor.setTaskName("Saving");
					logger.info("normalizing tags: "+selectedTags.size());
					
					monitor.beginTask("Updating tag values", selectedTags.size());
					int c=0;
					Map<TrpTranscriptMetadata, TrpPageType> affectedPages = new HashMap<>();
					for (CustomTag t : selectedTags) {
						if (monitor.isCanceled())
							return;
						
						t.setAttributes(propertyTable.getSelectedTag(), false, true);
						
						affectedPages.put(t.getCustomTagList().getShape().getPage().getMd(), t.getCustomTagList().getShape().getPage());
						
//						t.getCustomTagList().getShape().getPage();
						monitor.worked(c++);
					}
					
					logger.debug("nr of affected transcripts: "+affectedPages.size());
					
					try {
						ATextAndSearchComposite.saveAffectedPages(monitor, s.getCurrentDocumentCollectionId(), affectedPages.values());
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					}
				}
			}, "Normalizing tag values", true);
		} catch (Throwable e) {
			TrpMainWidget.getInstance().onError("Error normalizing tag values", e.getMessage(), e, true, false);
		}
	}
	
	void setInput(List<CustomTag> selectedTags) {
		if (selectedTags != null && !selectedTags.isEmpty() && isSameTags(selectedTags)) {
			this.selectedTags = selectedTags;
			
			CustomTag protoTag = selectedTags.get(0).copy();

			propertyTable.setInput(protoTag, protoTag);
		} else {
			this.selectedTags = null;
			propertyTable.setInput(null, null);
		}
	}

}
