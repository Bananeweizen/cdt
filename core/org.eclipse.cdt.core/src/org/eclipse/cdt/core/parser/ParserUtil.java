/**********************************************************************
 * Copyright (c) 2002,2003 Rational Software Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM Rational Software - Initial API and implementation
***********************************************************************/
package org.eclipse.cdt.core.parser;

import org.eclipse.cdt.internal.core.model.IDebugLogConstants;
import org.eclipse.cdt.internal.core.parser.ParserLogService;

/**
 * @author jcamelon
 *
 */
public class ParserUtil
{
	
	public static IParserLogService getParserLogService()
	{
		return parserLogService;
	}
		
	private static IParserLogService parserLogService = new ParserLogService(IDebugLogConstants.PARSER );
	private static IParserLogService scannerLogService = new ParserLogService(IDebugLogConstants.SCANNER );

	/**
	 * @return
	 */
	public static IParserLogService getScannerLogService() {
		return scannerLogService;
	}
}
