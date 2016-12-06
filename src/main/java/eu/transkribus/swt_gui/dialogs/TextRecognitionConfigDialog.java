package eu.transkribus.swt_gui.dialogs;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swtchart.Chart;
import org.swtchart.IAxisSet;
import org.swtchart.ISeries;
import org.swtchart.ISeries.SeriesType;
import org.swtchart.ISeriesSet;
import org.swtchart.internal.series.LineSeries;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.exceptions.NoConnectionException;
import eu.transkribus.core.model.beans.TrpHtr;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.htr.HtrTableWidget;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.storage.Storage;

public class TextRecognitionConfigDialog extends Dialog {
	private static final Logger logger = LoggerFactory.getLogger(TextRecognitionConfigDialog.class);

	private Storage store = Storage.getInstance();
	
	private CTabFolder folder;
	private CTabItem uroTabItem, ocrItem;
	
	private Group dictGrp;
	
	private HtrTableWidget htw;
	private Text nameTxt, langTxt, descTxt;
	private Button showTrainSetBtn, showTestSetBtn, showCharSetBtn;
	private Chart cerChart;
	
	private String charSetTitle, charSet;
	private Integer trainSetId, testSetId;
	
	private DocImgViewerDialog trainDocViewer, testDocViewer = null;
	
	Combo htrDictCombo, ocrLangCombo;
	
	private TrpHtr htr;
	
	List<String> htrDicts;
	
	public TextRecognitionConfigDialog(Shell parent) {
		super(parent);
	}
    
	public void setVisible() {
		if(super.getShell() != null && !super.getShell().isDisposed()) {
			super.getShell().setVisible(true);
		}
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite cont = (Composite) super.createDialogArea(parent);
		
		SashForm mainSash = new SashForm(cont, SWT.HORIZONTAL);
		mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainSash.setLayout(new GridLayout(2, false));
		
		folder = new CTabFolder(mainSash, SWT.BORDER | SWT.FLAT);
		folder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		uroTabItem = new CTabItem(folder, SWT.NONE);
		uroTabItem.setText("CITlab RNN HTR");
		
		SashForm uroSash = new SashForm(folder, SWT.HORIZONTAL);
		uroSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		uroSash.setLayout(new GridLayout(2, false));
//		sash.setWeights(new int[] {40, 60});
		
		htw = new HtrTableWidget(uroSash, SWT.BORDER);
		htw.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener() {
			@Override public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) htw.getTableViewer().getSelection();
				TrpHtr htr = (TrpHtr) sel.getFirstElement();		
				updateDetails(htr);
			}
		});
		
		Group detailGrp = new Group(uroSash, SWT.BORDER);
		detailGrp.setText("Details");
		detailGrp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		detailGrp.setLayout(new GridLayout(4, false));
		
		Label nameLbl = new Label(detailGrp, SWT.NONE);
		nameLbl.setText("Name:");
		nameTxt = new Text(detailGrp, SWT.BORDER | SWT.READ_ONLY);
		nameTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		Label langLbl = new Label(detailGrp, SWT.NONE);
		langLbl.setText("Language:");
		langTxt = new Text(detailGrp, SWT.BORDER | SWT.READ_ONLY);
		langTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		
		Label descLbl = new Label(detailGrp, SWT.NONE);
		descLbl.setText("Description:");
		descTxt = new Text(detailGrp, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY);
		descTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		
		new Label(detailGrp, SWT.NONE);
		showTrainSetBtn = new Button(detailGrp, SWT.PUSH);
		showTrainSetBtn.setText("Show Train Set");
		showTrainSetBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		showTrainSetBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(trainDocViewer != null) {
					trainDocViewer.setVisible();
				} else {
					try {
						trainDocViewer = new DocImgViewerDialog(getParentShell(), "Train Set", store.getTrainSet(htr));
						trainDocViewer.open();
					} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException
							| NoConnectionException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					trainDocViewer = null;
				}
				super.widgetSelected(e);
			}
		});
		
		showTestSetBtn = new Button(detailGrp, SWT.PUSH);
		showTestSetBtn.setText("Show Test Set");
		showTestSetBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		showTestSetBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(testDocViewer != null) {
					testDocViewer.setVisible();
				} else {
					try {
						testDocViewer = new DocImgViewerDialog(getParentShell(), "Test Set", store.getTestSet(htr));
						testDocViewer.open();
					} catch (SessionExpiredException | ClientErrorException | IllegalArgumentException
							| NoConnectionException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					testDocViewer = null;
				}
				super.widgetSelected(e);
			}
		});
		
		showCharSetBtn = new Button(detailGrp, SWT.PUSH);
		showCharSetBtn.setText("Show Character Set");
		showCharSetBtn.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		showCharSetBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				DialogUtil.showMessageDialog(getShell(), charSetTitle, charSet, null, null, new String[] { "Close" }, 0);				
			}
		});	
		
		Label cerLbl = new Label(detailGrp, SWT.NONE);
		cerLbl.setText("Train Curve:");
		
		cerChart = new Chart(detailGrp, SWT.BORDER);
		cerChart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		cerChart.getTitle().setVisible(false);
		cerChart.getAxisSet().getXAxis(0).getTitle().setText("Epochs");
		cerChart.getAxisSet().getYAxis(0).getTitle().setText("CER");
		
		uroTabItem.setControl(uroSash);
		
		ocrItem = new CTabItem(folder, SWT.NONE);
		ocrItem.setText("Abbyy Finereader OCR");
		
		dictGrp = new Group(mainSash, SWT.NONE);
		dictGrp.setLayout(new GridLayout(1, false));
		dictGrp.setText("Dictionary");
		
		folder.setSelection(uroTabItem); //FIXME
		folder.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateDictGroup();
				super.widgetSelected(e);
			}
		});
		
		updateHtrs();
		loadHtrDicts();
		updateDictGroup();
		
		mainSash.setWeights(new int[]{80, 20});
		
		return cont;
	}
	
	private void updateDetails(TrpHtr htr) {
		nameTxt.setText(htr.getName());
		langTxt.setText(htr.getLanguage());
		descTxt.setText(htr.getDescription());
		
		charSetTitle = "Character Set of Model: " + htr.getName();
		charSet = htr.getCharList() == null || htr.getCharList().isEmpty() ? "N/A" : htr.getCharList();
		
		this.htr = htr;
		
		if(htr.getTestGtDocId() == 0) {
			showTestSetBtn.setEnabled(false);
		}
		
		updateChart(htr.getCerString());
	}

	private void updateChart(String cerString) {
		String[] cerStrs = cerString.split(" ");
		double[] cerVals = new double[cerStrs.length];
		for(int i = 0; i < cerStrs.length; i++) {
			try {
				cerVals[i] = Double.parseDouble(cerStrs[i].replace(',', '.'));
			} catch(NumberFormatException e) {
				logger.error("Could not parse CER String: " + cerStrs[i]);
			}
		}
		
		ISeriesSet seriesSet = cerChart.getSeriesSet();
		ISeries series = seriesSet.createSeries(SeriesType.LINE, "CER series");
		series.setVisibleInLegend(false);
		((LineSeries)series).setAntialias(SWT.ON);
		series.setYSeries(cerVals);
		
		IAxisSet axisSet = cerChart.getAxisSet();
		axisSet.getXAxis(0).getTitle().setText("Epochs");
		axisSet.getYAxis(0).getTitle().setText("CER");
		axisSet.adjustRange();
		
		cerChart.redraw();
		cerChart.update();
	}

	private void updateHtrs() {
		List<TrpHtr> uroHtrs = new ArrayList<>(0);
		try {
			uroHtrs = store.listHtrs("CITlab");
		} catch (SessionExpiredException | ServerErrorException | ClientErrorException | NoConnectionException e1) {
			DialogUtil.showErrorMessageBox(this.getParentShell(), "Error", "Could not load HTR model list!");
			return;
		}
		
		htw.refreshList(uroHtrs);
	}

	private void updateDictGroup() {
		for(Control c : dictGrp.getChildren()) {
			c.dispose();
		}
		if(folder.getSelection() == null) {
			return; 
		} else if(folder.getSelection().equals(uroTabItem)) {
			htrDictCombo = new Combo(dictGrp, SWT.READ_ONLY);
			htrDictCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			htrDictCombo.setItems(this.htrDicts.toArray(new String[this.htrDicts.size()]));
			htrDictCombo.select(0); //FIXME
		} else if (folder.getSelection().equals(ocrItem)) {
			
		}
		dictGrp.layout();
	}
	
	private void loadHtrDicts(){
		try {
			this.htrDicts = store.getHtrDicts();
		} catch (SessionExpiredException | ServerErrorException | IllegalArgumentException
				| NoConnectionException e) {
			TrpMainWidget.getInstance().onError("Error", "Could not load HTR model list!", e);
			htrDicts = new ArrayList<>(0);
		}
	}
	
	@Override
	protected void okPressed() {
		super.okPressed();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Text Recognition Configuration");
		newShell.setMinimumSize(800, 600);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(1024, 768);
	}

	@Override
	protected void setShellStyle(int newShellStyle) {
		super.setShellStyle(SWT.CLOSE | SWT.MAX | SWT.RESIZE | SWT.TITLE);
		// setBlockOnOpen(false);
	}
}