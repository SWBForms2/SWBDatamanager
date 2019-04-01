/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.semanticwb.datamanager;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.semanticwb.datamanager.extractors.ScriptExtractor;
import org.semanticwb.datamanager.script.ScriptObject;

/**
 *
 * @author javiersolis
 */
public class DataExtractorBaseImp implements DataExtractorBase
{
    private static final Logger logger = Logger.getLogger(DataExtractorBaseImp.class.getName());
    private String name=null;
    private SWBScriptEngine scriptEngine=null;
    private ScriptObject scriptObject=null;
    private SWBDataSource dataSource=null;
    private Timer timer=null;
    private DataExtractor extractor=null;

    /**
     *
     * @param name
     * @param script
     * @param engine
     */
    protected DataExtractorBaseImp(String name, ScriptObject script, SWBScriptEngine engine)
    {
        this.name=name;
        this.scriptEngine=engine;
        this.scriptObject=script;        
        String dataSourceName=this.scriptObject.getString("dataSource");
        this.dataSource=engine.getDataSource(dataSourceName);        
        if(this.dataSource==null)throw new NoSuchFieldError("DataSource not found:"+dataSourceName);
        
        logger.log(Level.INFO,"Loading DataExtractor:"+name);
        String dataClass=script.getString("class");
        if(dataClass!=null)
        {
            try
            {
                Class cls=Class.forName(dataClass);
                Constructor c=cls.getConstructor();
                extractor=(DataExtractor)c.newInstance();
            }catch(Exception e){e.printStackTrace();} 
        }else 
        {
            extractor=new ScriptExtractor();
        }
    }

    /**
     *
     * @param data
     * @throws IOException
     */
    public void store(DataObject data) throws IOException
    {
        dataSource.add(data);
    }

    /**
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     */
    @Override
    public SWBScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    /**
     *
     * @param scriptEngine
     */
    public void setScriptEngine(SWBScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    /**
     *
     * @return
     */
    @Override
    public ScriptObject getScriptObject() {
        return scriptObject;
    }

    /**
     *
     * @param scriptObject
     */
    public void setScriptObject(ScriptObject scriptObject) {
        this.scriptObject = scriptObject;
    }

    /**
     *
     * @return
     */
    public SWBDataSource getDataSource() {
        return dataSource;
    }

    /**
     *
     * @param dataSource
     */
    public void setDataSource(SWBDataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    /**
     *
     * @throws IOException
     */
    public void extract() throws IOException
    {
        extractor.extract(this);
    }
    
    /**
     *
     */
    public void start()
    {
        ScriptObject t=getScriptObject().get("timer");
        if(t!=null)
        {
            long time=t.getInt("time");
            String unit=t.getString("unit");
            if(unit!=null)
            {
                if(unit.equals("s"))time=time*1000;
                if(unit.equals("m"))time=time*1000*60;
                if(unit.equals("h"))time=time*1000*60*60;
                if(unit.equals("d"))time=time*1000*60*60*24;
            }
            
            final DataExtractorBase base=this;
            
            timer=new Timer();
            timer.schedule(new TimerTask()
            {
                @Override
                public void run() {
                    try
                    {
                        if(!scriptEngine.isDisabledDataTransforms())
                        {
                            extractor.extract(base);
                        }
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }   
            },time,time);
        }
        extractor.start(this);
    }
    
    /**
     *
     */
    public void stop()
    {
        if(timer!=null)
        {
            timer.cancel();
        }
        extractor.stop(this);
    }    

    /**
     *
     * @param data
     * @throws IOException
     */
    @Override
    public void store(ScriptObjectMirror data) throws IOException 
    {
        store(DataUtils.toDataObject(data));
    }
    
}
