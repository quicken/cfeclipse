/*
 * Created on Nov 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.rohanclan.cfml.views.explorer;



import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.jface.viewers.StructuredSelection;


import com.rohanclan.cfml.ftp.FtpConnection;
import com.rohanclan.cfml.ftp.FtpConnectionProperties;
import com.rohanclan.cfml.views.explorer.ftp.FtpConnectionDialog;
import com.sun.rsasign.s;


import org.eclipse.jface.action.IStatusLineManager;

/**
 * @author Stephen Milligan
 *
 * The explorer view is suposed to be a faithful replication
 * of the file explorer tab in Homesite/CFStudio.
 */
public class FileExplorerView extends ViewPart {

    private MenuItem disconnectItem,connectItem,manageItem;
    
    private final class ComboSelectionListener implements ISelectionChangedListener {
        ViewPart viewpart = null;
        public ComboSelectionListener(ViewPart viewpart) {
            this.viewpart = viewpart;
        }
        
        public void selectionChanged(SelectionChangedEvent e) {
        	try {
        	    StructuredSelection sel = (StructuredSelection)e.getSelection();
        		directoryTreeViewer.setInput(sel.getFirstElement());
        		fileViewer.setInput(sel.getFirstElement());
        		if (sel.getFirstElement() instanceof FtpConnectionProperties) {
        		    connectItem.setEnabled(true);
        		    disconnectItem.setEnabled(true);
        		}
        		else {
        		    connectItem.setEnabled(false);
        		    disconnectItem.setEnabled(false);
        		}
        	}
        	catch (Exception ex) {
        		ex.printStackTrace();
        	}
        }
    }

    private final class DirectorySelectionListener implements ISelectionChangedListener {
        ViewPart viewpart = null;
        public DirectorySelectionListener(ViewPart viewpart) {
            this.viewpart = viewpart;
        }
        public void selectionChanged(SelectionChangedEvent e) {
            fileViewer.setInput(e.getSelection());
        }
    }

    private final class MenuMouseListener implements MouseListener {
        Menu menu = null;

        public MenuMouseListener (Menu menu) {
            this.menu = menu;
        }

        public void mouseUp(MouseEvent e) {
            
        	menu.setVisible(true);
        }

        public void mouseDown(MouseEvent e) {
        	
        }

        public void mouseDoubleClick(MouseEvent e) {
        	
        }
    }

    ComboViewer comboViewer = null;
    TreeViewer directoryTreeViewer = null;
    TableViewer fileViewer = null;
    
    
    public void createPartControl(Composite parent) {

	    FtpConnection.getInstance().setViewPart(this);
    	this.initializeStatusBar();
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout containerLayout = new GridLayout();
        containerLayout.numColumns = 2;
        container.setLayout(containerLayout);
        
        // Combo viewer
        comboViewer = new ComboViewer(container, SWT.READ_ONLY);
        comboViewer.addSelectionChangedListener(new ComboSelectionListener(this));
        comboViewer.setLabelProvider(new ComboLabelProvider());
        comboViewer.setContentProvider(new ComboContentProvider());
        final Combo combo = comboViewer.getCombo();
        final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		
        
        combo.setLayoutData(gridData);
        comboViewer.setInput(new LocalFileSystem());
        
        Menu menu = new Menu(container.getShell(), SWT.POP_UP);
        createMenuItems(menu);
        Button menuButton = new Button(container,SWT.ARROW | SWT.RIGHT);
        menuButton.addMouseListener(new MenuMouseListener(menu));
        
        
        
        // This is what makes the controls resizable
        SashForm sash = new SashForm(container,SWT.VERTICAL);
        GridData sashData = new GridData(GridData.FILL_BOTH);
        sashData.horizontalSpan = 2;
        sash.setLayoutData(sashData);
        
        // Directory tree
        directoryTreeViewer = new TreeViewer(sash, SWT.BORDER);
        directoryTreeViewer.addSelectionChangedListener(new DirectorySelectionListener(this));
        directoryTreeViewer.setSorter(new DirectorySorter());
        directoryTreeViewer.setLabelProvider(new DirectoryLabelProvider());
        directoryTreeViewer.setContentProvider(new DirectoryContentProvider(this));
        final Tree tree = directoryTreeViewer.getTree();
        directoryTreeViewer.setComparer(new FileComparer());
        tree.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        
        directoryTreeViewer.setInput(new LocalFileSystem());
        

        FileContentProvider fileProvider = new FileContentProvider(this);
        fileViewer = new TableViewer(sash, SWT.BORDER);
        final Table fileTable = fileViewer.getTable();
        
        fileViewer.addDoubleClickListener(new FileDoubleClickListener(fileProvider)); 

        fileViewer.setSorter(new Sorter());
        fileViewer.setLabelProvider(new FileLabelProvider());
        fileViewer.setContentProvider(fileProvider);
        fileViewer.setComparer(new FileComparer());
        final Table table = fileViewer.getTable();
        table.setLayoutData(new GridData(GridData.FILL_BOTH));
        fileViewer.setInput(new LocalFileSystem());
        
        createActions();
        initializeToolBar();
        initializeMenu();
    	
    }
    
    
    private void createMenuItems(Menu menu) {

        manageItem = new MenuItem(menu,SWT.CASCADE);
        manageItem.setText("Manage FTP Connections");
        manageItem.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event e) {
	        	try {
	            String connectionID = null;
	            StructuredSelection sel = (StructuredSelection)comboViewer.getSelection();
	            if (sel.getFirstElement() instanceof FtpConnectionProperties) {
	                connectionID = ((FtpConnectionProperties)sel.getFirstElement()).getConnectionid();
	            }
                FtpConnectionDialog dialog = new FtpConnectionDialog(e.widget.getDisplay().getActiveShell(),connectionID);
            	if (dialog.open() == IDialogConstants.OK_ID) {
            		comboViewer.setInput(dialog.connectionProperties);
            		
            	}
	        	}
	        	catch (Exception ex) {
	        		ex.printStackTrace();
	        	}
            }
        });
        
        disconnectItem = new MenuItem(menu,SWT.CASCADE);
        disconnectItem.setText("Disconnect");
        disconnectItem.setEnabled(false);
        disconnectItem.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event e) {
	            try {
		            FtpConnection.getInstance().disconnect();
		            disconnectItem.setEnabled(false);
		            connectItem.setEnabled(true);
		            
	            }
	            catch (Exception ex) {
	                ex.printStackTrace();
	            }
            }
        });
        
        connectItem = new MenuItem(menu,SWT.CASCADE);
        connectItem.setText("Re-Connect");
        connectItem.setEnabled(false);
        connectItem.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event e) {
	            try {
		            FtpConnection.getInstance().connect();
		            disconnectItem.setEnabled(true);
		            connectItem.setEnabled(false);
	            }
	            catch (Exception ex) {
	                ex.printStackTrace();
	            }
            }
        });
        
    }

    
    
    private void createActions() {
    }

    private void initializeToolBar() {
        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
    }

    private void initializeMenu() {
        IMenuManager manager = getViewSite().getActionBars().getMenuManager();
    }
    
    private void initializeStatusBar() {
        IStatusLineManager manager = getViewSite().getActionBars().getStatusLineManager();
        //manager.setMessage("TEST");
    }

    public void setFocus() {
    
    }
    
    public void dispose() {

		if (comboViewer.getContentProvider() != null) {
			comboViewer.getContentProvider().dispose();
		}
		if (directoryTreeViewer.getContentProvider() != null) {
			directoryTreeViewer.getContentProvider().dispose();
		}
		if (fileViewer.getContentProvider() != null) {
			fileViewer.getContentProvider().dispose();
		}

    	super.dispose();
    }
    
}
