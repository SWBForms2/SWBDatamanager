/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.semanticwb.datamanager;

import java.util.Iterator;
import org.semanticwb.datamanager.script.ScriptObject;

/**
 *
 * @author javier.solis
 */
public class SWBFormProcessor implements Comparable<SWBFormProcessor>
{
    /**
     *
     */
    public static final String METHOD_REQUEST="request";

    
    private String name=null;
    private ScriptObject script=null;
    
    /**
     *
     * @param name
     * @param script
     */
    protected SWBFormProcessor(String name, ScriptObject script)
    {
        this.name=name;
        this.script=script;
    }

    /**
     * Regresa Nombre del DataSource
     * @return String
     */
    public String getName() {
        return name;
    }
    
    /**
     * Regresa ScriptObject con el script con la definición del datasource definida el el archivo js
     * @return ScriptObject
     */
    public ScriptObject getFormProcessorScript()
    {
        return script;
    }      

    @Override
    public int compareTo(SWBFormProcessor o) 
    {
        return script.getInt("order")>o.getFormProcessorScript().getInt("order")?1:-1;
    }
}
