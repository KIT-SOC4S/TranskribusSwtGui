package eu.transkribus.swt_gui.pagination_tables;

import java.util.List;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.transkribus.client.util.SessionExpiredException;
import eu.transkribus.core.model.beans.TrpDocMetadata;
import eu.transkribus.core.model.beans.job.TrpJobStatus;
import eu.transkribus.swt.util.DialogUtil;
import eu.transkribus.swt_gui.mainwidget.Storage;
import eu.transkribus.swt_gui.mainwidget.TrpMainWidget;
import eu.transkribus.swt_gui.mainwidget.listener.IStorageListener;

public class JobTableWidgetListener extends SelectionAdapter implements IStorageListener, IDoubleClickListener {
	private final static Logger logger = LoggerFactory.getLogger(JobTableWidgetListener.class);

	JobTableWidgetPagination jw;
	Storage storage = Storage.getInstance();
	
	TableViewer tv;

	public JobTableWidgetListener(JobTableWidgetPagination jw) {
		Assert.assertNotNull("JobTablWidgetPagination cannot be null!", jw);
		
		this.jw = jw;
		
//		jobOverviewWidget.reloadBtn.addSelectionListener(this);
		jw.getShowAllJobsBtn().addSelectionListener(this);
		jw.getCancelBtn().addSelectionListener(this);
		
		this.tv = jw.getPageableTable().getViewer();
		
		tv.addDoubleClickListener(this);
		
		Storage.getInstance().addListener(this);
	}
	
	@Override public void doubleClick(DoubleClickEvent event) {
		TrpMainWidget mw = TrpMainWidget.getInstance();
		
		TrpJobStatus jobStatus = jw.getFirstSelected();
		logger.debug("double click on transcript: "+jobStatus);
		
		if (jobStatus!=null) {
			logger.debug("Loading doc: " + jobStatus.getDocId());
			int col = 0;
			TrpDocMetadata el = null;
			try {
				List<TrpDocMetadata> docList = 
						storage.getConnection().findDocuments(0, jobStatus.getDocId(), "", "", "", "", true, false, 0, 0, null, null);
				if (docList != null && docList.size() > 0){
					col = docList.get(0).getColList().get(0).getColId();
					el = docList.get(0);
				}
						
			} catch (SessionExpiredException | ServerErrorException
					| ClientErrorException | IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String pages = jobStatus.getPages();
			int pageNr = ( (pages == null || pages.equals("") || pages.contains("-") ) ? 0 : Integer.parseInt(pages));
			mw.loadRemoteDoc(jobStatus.getDocId(), col, pageNr-1);
			mw.getUi().getServerWidget().setSelectedCollection(col, true);
			mw.getUi().getServerWidget().getDocTableWidget().loadPage("docId", jobStatus.getDocId(), true);
			
		}		
	}
	
	@Override public void widgetSelected(SelectionEvent e) {
		TrpMainWidget mw = TrpMainWidget.getInstance();
		
		Object s = e.getSource();
		if (s.equals(jw.getShowAllJobsBtn())) {
			jw.reloadJobList();
		} else if(s.equals(jw.getCancelBtn())) {
			TrpJobStatus job = jw.getFirstSelected();
			if(job != null && !job.getState().equals(TrpJobStatus.FINISHED) &&
					!job.getState().equals(TrpJobStatus.CANCELED) &&
					!job.getState().equals(TrpJobStatus.FAILED)){
				logger.debug("Canceling job with id = " + job.getJobId());
				mw.cancelJob(job.getJobId());
			}
		}
	}

	@Override public void handleLoginOrLogout(LoginOrLogoutEvent arg) {
		boolean visible = Storage.getInstance().isLoggedIn() && Storage.getInstance().getUser().isAdmin();
		jw.getShowAllJobsBtn().setVisible(visible);
		jw.refreshPage(true);
	}
	
	@Override public void handleJobUpdate(JobUpdateEvent jue) {
		TrpMainWidget mw = TrpMainWidget.getInstance();
		
		TrpJobStatus job = jue.job;
		
		boolean isThisDocOpen = true;
		if (job != null) { // specific job was updated
			logger.debug("specific job update: "+job);
			
			tv.update(job, null);
			
			isThisDocOpen = storage.isDocLoaded() && storage.getDoc().getId()==job.getDocId();	
			
			// reload current page if page job for this page is finished:
			// TODO: only ask question to reload page!!
			if (isThisDocOpen && job.isFinished()) {
				if (!job.isSuccess()) {
					logger.error("a job for the current page failed: "+job);
					DialogUtil.showErrorMessageBox(mw.getShell(), "A job for this page failed", job.getDescription());
				} 
				else if (storage.getPageIndex() == (job.getPageNr()-1) || job.getPageNr()==-1) {
					// reload page if doc and page is open:					
					if (DialogUtil.showYesNoDialog(mw.getShell(), "A job for this page finished", "A job for this page just finished - do you want to reload the current page?") == SWT.YES) {
						logger.debug("reloading page!");
						mw.reloadCurrentPage(true);						
					}
				}
			}
		} else {
//			logger.debug("got "+storage.getJobs().size()+" jobs, thread = "+Thread.currentThread().getName());
//			jw.setInput(new ArrayList<>(storage.getJobs()));
			
			jw.reloadJobList();
		}
		
		mw.updatePageLock();
	}	
	
}
