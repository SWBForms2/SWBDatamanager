/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.semanticwb.datamanager.datastore;

import java.io.IOException;
import org.semanticwb.datamanager.DataObject;
import org.semanticwb.datamanager.DataObjectIterator;
import org.semanticwb.datamanager.SWBDataSource;

/**
 *
 * @author javiersolis
 */
public interface SWBDataStore 
{

    /**
     *
     * @param json
     * @param dataSource
     * @return
     * @throws IOException
     */
    public DataObjectIterator find(DataObject json, SWBDataSource dataSource) throws IOException;
    
    /**
     *
     * @param json
     * @param dataSource
     * @return
     * @throws IOException
     */
    public DataObject fetch(DataObject json, SWBDataSource dataSource) throws IOException;

    /**
     *
     * @param json
     * @param dataSource
     * @return
     * @throws IOException
     */
    public DataObject aggregate(DataObject json, SWBDataSource dataSource) throws IOException;
    
    /**
     *
     * @param json
     * @param dataSource
     * @return
     * @throws IOException
     */
    public DataObject add(DataObject json, SWBDataSource dataSource) throws IOException;

    /**
     *
     * @param json
     * @param dataSource
     * @return
     * @throws IOException
     */
    public DataObject remove(DataObject json, SWBDataSource dataSource) throws IOException;

    /**
     *
     * @param json
     * @param dataSource
     * @return
     * @throws IOException
     */
    public DataObject update(DataObject json, SWBDataSource dataSource) throws IOException;

    /**
     * Close the DataStore
     */
    public void close();
    
    /**
     * returns true if the datastore is created and persist in the database
     */
    public boolean existModel(String modelid);
    
    
    /**
     * Create an index if not exist
     * @param name
     * @param index
     * @param dataSource
     * @return true if the index was created or false if the index exist
     * @throws IOException 
     */
    public boolean createIndex(String name, DataObject index, SWBDataSource dataSource) throws IOException;
    
    /**
     * Remove an Index of the datasource
     * @param name the name of the index
     * @param dataSource
     * @throws IOException 
     */
    public void dropIndex(String name, SWBDataSource dataSource) throws IOException;
}
