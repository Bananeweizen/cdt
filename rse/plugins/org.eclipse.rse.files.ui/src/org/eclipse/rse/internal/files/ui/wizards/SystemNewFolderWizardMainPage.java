/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * Kevin Doyle (IBM) - Changed name Validator to ValidatorFileUniqueName
 * Martin Oberhuber (Wind River) - [186773] split ISystemRegistryUI from ISystemRegistry
 * David McKnight   (IBM)        - [216252] cleaning up system message ids and strings
 ********************************************************************************/

package org.eclipse.rse.internal.files.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.rse.internal.files.ui.FileResources;
import org.eclipse.rse.internal.files.ui.ISystemFileConstants;
import org.eclipse.rse.services.clientserver.messages.SystemMessage;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFile;
import org.eclipse.rse.subsystems.files.core.subsystems.IRemoteFileSubSystem;
import org.eclipse.rse.subsystems.files.core.util.ValidatorFileUniqueName;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemWidgetHelpers;
import org.eclipse.rse.ui.messages.ISystemMessageLine;
import org.eclipse.rse.ui.validators.ISystemValidator;
import org.eclipse.rse.ui.validators.ValidatorUniqueString;
import org.eclipse.rse.ui.wizards.AbstractSystemWizardPage;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;


/**
 * Default main page of the "New Folder" wizard.
 * This page asks for the following information:
 * <ul>
 *   <li>New File name
 * </ul> 
 */

public class SystemNewFolderWizardMainPage 
 	   extends AbstractSystemWizardPage
{  

	protected String fileNameStr; 
	protected Text folderName, connectionName, newfolderName;
	protected Combo folderNames;
	protected SystemMessage errorMessage;
	protected ISystemValidator nameValidator;
	protected IRemoteFile[] parentFolders;
	protected ISystemMessageLine msgLine;
	public String [] allnames;
		  
	/**
	 * Constructor.
	 */
	public SystemNewFolderWizardMainPage(Wizard wizard, IRemoteFile[] parentFolders)
	{
		super(wizard, "NewFolder",  //$NON-NLS-1$
				FileResources.RESID_NEWFOLDER_PAGE1_TITLE, 
				FileResources.RESID_NEWFOLDER_PAGE1_DESCRIPTION);
	//	nameValidator = new ValidatorProfileName(RSECorePlugin.getTheSystemRegistry().getAllSystemProfileNamesVector());
	    nameValidator = new ValidatorUniqueString(allnames, true);
	    this.parentFolders = parentFolders; 
	}

	/**
	 * CreateContents is the one method that must be overridden from the parent class.
	 * In this method, we populate an SWT container with widgets and return the container
	 *  to the caller (JFace). This is used as the contents of this page.
	 */
	public Control createContents(Composite parent)
	{

		int nbrColumns = 2;
		Composite composite_prompts = SystemWidgetHelpers.createComposite(parent, nbrColumns);	

        // Connection name
		connectionName = SystemWidgetHelpers.createLabeledTextField(composite_prompts, null, FileResources.RESID_NEWFOLDER_CONNECTIONNAME_LABEL, FileResources.RESID_NEWFOLDER_CONNECTIONNAME_TIP);
		
		//labelConnectionName.	

        // FolderName		
        if (parentFolders.length == 1)
	      folderName = SystemWidgetHelpers.createLabeledTextField(composite_prompts,null, FileResources.RESID_NEWFOLDER_FOLDER_LABEL, FileResources.RESID_NEWFOLDER_FOLDER_TIP);
	    else
	      folderNames = SystemWidgetHelpers.createLabeledReadonlyCombo(composite_prompts, null, FileResources.RESID_NEWFOLDER_FOLDER_LABEL, FileResources.RESID_NEWFOLDER_FOLDER_TIP);	      
	
		// New Folder Name
		newfolderName = SystemWidgetHelpers.createLabeledTextField(composite_prompts, null, FileResources.RESID_NEWFOLDER_NAME_LABEL,  FileResources.RESID_NEWFOLDER_NAME_TOOLTIP);
		
		initializeInput();
		
		newfolderName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validateNameInput();
				}
			}
		);			
    		
		SystemWidgetHelpers.setCompositeHelp(composite_prompts, RSEUIPlugin.HELPPREFIX+ISystemFileConstants.NEW_FOLDER_WIZARD);	
		
		return composite_prompts;		

	}
	
	/**
	 * Return the Control to be given initial focus.
	 * Override from parent. Return control to be given initial focus.
	 */
	protected Control getInitialFocusControl()
	{
        return newfolderName;
	}
	
	/**
	 * Init values using input data
	 */
	protected void initializeInput()
	{
		connectionName.setEditable(false);
		// get existing names
		if (parentFolders != null && parentFolders.length > 0)
		{
			IRemoteFile parentFolder = parentFolders[0];
			nameValidator = new ValidatorFileUniqueName(getShell(),parentFolder, true);
		}
		else
		{
			nameValidator = null;
		}
		if (parentFolders == null)
		{
		    folderName.setEditable(false);
		    newfolderName.setEditable(false);
		    setPageComplete(false);
		    return;
		}
		IRemoteFileSubSystem rfss = parentFolders[0].getParentRemoteFileSubSystem(); 
        connectionName.setText(rfss.getHostAliasName());
        connectionName.setToolTipText((rfss.getHost()).getHostName());

		if (folderName != null)
		{
		   folderName.setText(parentFolders[0].getAbsolutePath());	
		   folderName.setEditable(false);	
		}
		else
		{
			String[] names = new String[parentFolders.length];
			for (int idx=0; idx<names.length; idx++)
			   names[idx] = parentFolders[idx].getAbsolutePath();
			folderNames.setItems(names);
			folderNames.select(0);
		}
	}
	
  	/**
	 * This hook method is called whenever the text changes in the input field.
	 * The default implementation delegates the request to an <code>ISystemValidator</code> object.
	 * If the <code>ISystemValidator</code> reports an error the error message is displayed
	 * in the Dialog's message line.
	 * 
	 */	
	protected SystemMessage validateNameInput() 
	{	
		errorMessage = null;
		this.clearErrorMessage();
	//	this.setDescription(SystemResources.RESID_NEWFILE_PAGE1_DESCRIPTION));		
	    if (nameValidator != null)	    
	      errorMessage= nameValidator.validate(newfolderName.getText());
	    if (errorMessage != null)
		  setErrorMessage(errorMessage);		
		setPageComplete(errorMessage==null);
		return errorMessage;		
	}
	
	/**
	 * Completes processing of the wizard. If this 
	 * method returns true, the wizard will close; 
	 * otherwise, it will stay active.
	 * This method is an override from the parent Wizard class. 
	 *
	 * @return whether the wizard finished successfully
	 */
	public boolean performFinish() 
	{
	    return true;
	}
    
	// --------------------------------- //
	// METHODS FOR EXTRACTING USER DATA ... 
	// --------------------------------- //
	/**
	 * Return user-entered new file name.
	 * Call this after finish ends successfully.
	 */
	public String getfolderName()
	{
		return newfolderName.getText();
	}    
	/**
	 * Return the parent folder selected by the user
	 */
	public IRemoteFile getParentFolder()
	{
		if (folderName != null)
		  return parentFolders[0];
		else
		{
			int selIdx = folderNames.getSelectionIndex();
			if (selIdx == -1)
			  selIdx = 0;
			return parentFolders[selIdx];
		}
	}	
	/**
	 * Return true if the page is complete, so to enable Finish.
	 * Called by wizard framework.
	 */
	public boolean isPageComplete()
	{
		return (errorMessage==null) && (newfolderName.getText().trim().length()>0);
	}
	
}