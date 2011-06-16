/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.wizards.datatransfer;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPOutputStream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

/**
 * Exports resources to a .tar.gz file.
 *
 * @since 3.1
 */
public class TarFileExporter implements IFileExporter {
    private TarOutputStream outputStream;
    private GZIPOutputStream gzipOutputStream;
    private BZip2CompressorOutputStream bzip2OutputStream;
    
    public static final int UNCOMPRESSED = 0;
    public static final int GZIP = 1;
    public static final int BZIP2 = 2;
    //public static final int XZ = 3;

    /**
     *	Create an instance of this class.
     *
     *	@param filename java.lang.String
     *	@param compress boolean
     *	@exception java.io.IOException
     */
    public TarFileExporter(String filename, boolean compress) throws IOException {
    	if(compress) {
    		gzipOutputStream = new GZIPOutputStream(new FileOutputStream(filename));
    		outputStream = new TarOutputStream(new BufferedOutputStream(gzipOutputStream));
    	} else {
    		outputStream = new TarOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
    	}
    }
    
	/**
	 * Create an instance of this class and set the compression mode of the
	 * exporter.
	 * 
	 * Supported modes/parameter values are:<br />
	 * TarFileExporter.UNCOMPRESSED for uncompressed .tar files<br />
	 * TarFileExporter.GZIP for Gzip compressed .tar.gz files<br />
	 * TarFileExporter.BZIP2 for Bzip2 compressed .tar.bz2 files<br />
	 * 
	 * @param mode
	 * @exception java.lang.IllegalArgumentException
	 * @exception java.io.IOException
	 */
	public TarFileExporter(String filename, int compressMode)
			throws IOException, IllegalArgumentException {
		switch (compressMode) {
		case UNCOMPRESSED:
			outputStream = new TarOutputStream(new BufferedOutputStream(
					new FileOutputStream(filename)));
			break;
		case GZIP:
			gzipOutputStream = new GZIPOutputStream(new FileOutputStream(
					filename));
			outputStream = new TarOutputStream(new BufferedOutputStream(
					gzipOutputStream));
			break;
		case BZIP2:
			bzip2OutputStream = new BZip2CompressorOutputStream(
					new FileOutputStream(filename));
			outputStream = new TarOutputStream(new BufferedOutputStream(
					bzip2OutputStream));
			break;
		// case XZ
		default:
			throw new IllegalArgumentException();// TO DO: DEFINE MESSAGE IN THE
													// UTILITY CLASS!
		}
	}
    
    /**
     *	Do all required cleanup now that we're finished with the
     *	currently-open .tar.gz
     *
     *	@exception java.io.IOException
     */
    public void finished() throws IOException {
        outputStream.close();
        if(gzipOutputStream != null) {
        	gzipOutputStream.close();
        }else if(bzip2OutputStream != null)
        {
        	bzip2OutputStream.close();
        }
    }

    /**
     *	Write the contents of the file to the tar archive.
     *
     *	@param entry
     *	@param contents
     *  @exception java.io.IOException
     *  @exception org.eclipse.core.runtime.CoreException
     */
    private void write(TarEntry entry, IFile contents) throws IOException, CoreException {
		final URI location = contents.getLocationURI();
		if (location == null) {
			throw new FileNotFoundException(contents.getFullPath().toOSString());
		}
    	
    	InputStream contentStream = contents.getContents(false);
    	entry.setSize(EFS.getStore(location).fetchInfo().getLength());
    	outputStream.putNextEntry(entry);
        try {
            int n;
            byte[] readBuffer = new byte[4096];
            while ((n = contentStream.read(readBuffer)) > 0) {
                outputStream.write(readBuffer, 0, n);
            }
        } finally {
            if (contentStream != null) {
				contentStream.close();
			}
        }

    	outputStream.closeEntry();    	
    }

    public void write(IContainer container, String destinationPath)
            throws IOException {
        TarEntry newEntry = new TarEntry(destinationPath);
        if(container.getLocalTimeStamp() != IResource.NULL_STAMP) {
        	newEntry.setTime(container.getLocalTimeStamp() / 1000);
        }
        ResourceAttributes attributes = container.getResourceAttributes();
        if (attributes != null && attributes.isExecutable()) {
        	newEntry.setMode(newEntry.getMode() | 0111);
        }
        if (attributes != null && attributes.isReadOnly()) {
        	newEntry.setMode(newEntry.getMode() & ~0222);
        }
        newEntry.setFileType(TarEntry.DIRECTORY);
        outputStream.putNextEntry(newEntry);
    }
    
    /**
     *  Write the passed resource to the current archive.
     *
     *  @param resource org.eclipse.core.resources.IFile
     *  @param destinationPath java.lang.String
     *  @exception java.io.IOException
     *  @exception org.eclipse.core.runtime.CoreException
     */
    public void write(IFile resource, String destinationPath)
            throws IOException, CoreException {
        TarEntry newEntry = new TarEntry(destinationPath);
        if(resource.getLocalTimeStamp() != IResource.NULL_STAMP) {
        	newEntry.setTime(resource.getLocalTimeStamp() / 1000);
        }
        ResourceAttributes attributes = resource.getResourceAttributes();
        if (attributes != null && attributes.isExecutable()) {
        	newEntry.setMode(newEntry.getMode() | 0111);
        }
        if (attributes != null && attributes.isReadOnly()) {
        	newEntry.setMode(newEntry.getMode() & ~0222);
        }
        write(newEntry, resource);
    }
    
}
