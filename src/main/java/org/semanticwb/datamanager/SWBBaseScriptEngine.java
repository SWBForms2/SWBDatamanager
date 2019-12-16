/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.semanticwb.datamanager;

import com.mongodb.util.JSON;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.semanticwb.datamanager.datastore.SWBDataStore;
import org.semanticwb.datamanager.filestore.SWBFileSource;
import org.semanticwb.datamanager.monitor.SWBMonitorMgr;
import org.semanticwb.datamanager.script.ScriptObject;
import javax.script.Compilable;

/**
 *
 * @author javiersolis
 */
public class SWBBaseScriptEngine implements SWBScriptEngine 
{
    private static final Logger logger = Logger.getLogger(SWBBaseScriptEngine.class.getName());
    
    private static final ConcurrentHashMap<String,SWBBaseScriptEngine> engines=new ConcurrentHashMap();
    
//    private final HashMap<SWBUser, Bindings> users=new HashMap();    
    private HashMap<String,ScriptObject> dataSources=null;
    private HashMap<String,ScriptObject> dataStores=null;
    private HashMap<String,SWBDataStore> dataStoresCache=null;
    
    private HashMap<String, SWBFileSource> fileSources=null;
    
    private HashMap<String,DataExtractorBase> dataExtractors=null;
        
    private HashMap<String,Set<SWBDataService>> dataServices=null;
    private HashMap<String,Set<SWBDataProcessor>> dataProcessors=null;
    private HashMap<String,Set<SWBFormProcessor>> formProcessors=null;
    
    private DataObject data=new DataObject();
    
    private ScriptEngine sengine=null;
    private ScriptObject sobject=null;
    private String source=null;    
    private transient long id;
    private transient long lastCheck;
    
    private ArrayList<SWBScriptFile> files=new ArrayList();
    private boolean needsReload=false;
    //private File file=null;    
    //private transient long updated;
        
    private boolean closed=false;
    private boolean internalSource=false;
    
    private boolean disabledDataTransforms=false;
    
    private SWBScriptUtils utils;
    
    private ProcessMgr processMgr;

    private SWBBaseScriptEngine(String source)
    {        
        this(source, false);
    }    
    
    private SWBBaseScriptEngine(String source, boolean internalSource)
    {
        this.source=source;
        this.internalSource=internalSource;
        utils=new SWBScriptUtils(this);
    }
    
    private void init()
    {
        logger.log(Level.INFO,"Initializing ScriptEngine: "+source);
        try
        {
            needsReload=false;
            closed=false;            
            
            ArrayList<String> sources=new ArrayList();
            if(!source.equals("[GLOBAL]"))
            {
                int i=source.indexOf("[");
                if(i>-1)
                {
                    String base=source.substring(0,i);
                    String arr=source.substring(i);
                    List<String> o=(List)JSON.parse(arr);
                    //System.out.println(o);
                    o.forEach(s->{
                        if(s.startsWith("/"))
                        {
                            sources.add(s);
                        }else
                        {
                            sources.add(base+s);
                        }
                    });
                }else
                {
                    sources.add(source);
                }
            }            
            
            files.clear();
            if(!sources.isEmpty())
            {                
                sources.forEach(s->{
                    File f=new File(DataMgr.getApplicationPath()+s); 
                    files.add(new SWBScriptFile(f));
                });
            }
            
            ScriptEngine engine=DataMgr.getNativeScriptEngine();     
            engine.put("seng", this);
            
            engine=DataMgr.loadLocalScript("/global.js", engine);
            
            String baseDS=DataMgr.getBaseInstance().getBaseDatasource();
            if(baseDS!=null)
            {
                engine=DataMgr.loadScript(baseDS, engine);
            }      
                
            Iterator<String> it2=sources.iterator();
            while (it2.hasNext()) {
                String f = it2.next();
                if(internalSource)
                    engine=DataMgr.loadLocalScript(f, engine); 
                else
                    engine=DataMgr.loadScript(f, engine);                 
            }
            
//            if(!source.equals("[GLOBAL]"))
//            {
//                if(internalSource)
//                    engine=DataMgr.loadLocalScript(source, engine); 
//                else
//                    engine=DataMgr.loadScript(source, engine);  
//            }
            
            ScriptObject eng=new ScriptObject(engine.get("eng"));
            
            //Load Routes
            ScriptObject ros = eng.get("routes");
            RoutesMgr.parseRouter(ros);
              
            //Load DataStores
            HashMap<String,ScriptObject> dataStores=new HashMap();          
            {
                ScriptObject dss=eng.get("dataStores");   
                Iterator<String> it=dss.keySet().iterator();
                while (it.hasNext()) {
                    String dsname = it.next();
                    logger.log(Level.INFO,"Loading DataStore:"+dsname);                    
                    ScriptObject dataStore=dss.get(dsname);
                    dataStores.put(dsname, dataStore);
                }
            }            
            
            //Load DataSources
            HashMap<String,ScriptObject> dataSources=new HashMap();
            {
                ScriptObject dss=eng.get("dataSources");
                Iterator<String> it=dss.keySet().iterator();
                while (it.hasNext()) {
                    String dsname = it.next();
                    logger.log(Level.INFO,"Loading DataSource:"+dsname);                    
                    ScriptObject so=dss.get(dsname);
                    dataSources.put(dsname, so);
                }
            }
            
            //Load DataProcessors
            HashMap<String,Set<SWBDataProcessor>> dataProcessors=new HashMap();          
            {
                ScriptObject dss=eng.get("dataProcessors");   
                Iterator<String> it=dss.keySet().iterator();
                while(it.hasNext())
                {
                    String key=it.next();
                    ScriptObject data=dss.get(key);
                    logger.log(Level.INFO,"Loading DataProcessor:"+key);
                    SWBDataProcessor dataProcessor=new SWBDataProcessor(key,data);
                    
                    Iterator<ScriptObject> dsit=data.get("dataSources").values().iterator();
                    while (dsit.hasNext()) 
                    {
                        ScriptObject dsname = dsit.next();
                        Iterator<ScriptObject> acit=data.get("actions").values().iterator();
                        while (acit.hasNext()) 
                        {
                            String action = acit.next().getValue().toString();
                            String name=dsname.getValue().toString();
                            
                            if(name.equals("*"))
                            {
                                Iterator<String> itds=dataSources.keySet().iterator();
                                while (itds.hasNext()) 
                                {
                                    name = itds.next();
                                    String k=name+"-"+action;
                                    Set<SWBDataProcessor> arr=dataProcessors.get(k);
                                    if(arr==null)
                                    {
                                        arr=new TreeSet();
                                        dataProcessors.put(k, arr);
                                    }
                                    arr.add(dataProcessor);
                                }
                                
                            }else
                            {
                                String k=name+"-"+action;
                                Set<SWBDataProcessor> arr=dataProcessors.get(k);
                                if(arr==null)
                                {
                                    arr=new TreeSet();
                                    dataProcessors.put(k, arr);
                                }
                                arr.add(dataProcessor);
                            }                            

                        }
                    }
                }
            }

            //Load FormProcessors
            HashMap<String,Set<SWBFormProcessor>> formProcessors=new HashMap();          
            {
                ScriptObject dss=eng.get("formProcessors");   
                Iterator<String> it=dss.keySet().iterator();
                while(it.hasNext())
                {
                    String key=it.next();
                    ScriptObject data=dss.get(key);
                    logger.log(Level.INFO,"Loading FormProcessor:"+key);
                    SWBFormProcessor formProcessor=new SWBFormProcessor(key,data);
                    
                    Iterator<ScriptObject> dsit=data.get("dataSources").values().iterator();
                    while (dsit.hasNext()) 
                    {
                        ScriptObject dsname = dsit.next();
                        Iterator<ScriptObject> acit=data.get("actions").values().iterator();
                        while (acit.hasNext()) 
                        {
                            String action = acit.next().getValue().toString();
                            String name=dsname.getValue().toString();
                            
                            if(name.equals("*"))
                            {
                                Iterator<String> itds=dataSources.keySet().iterator();
                                while (itds.hasNext()) 
                                {
                                    name = itds.next();
                                    String k=name+"-"+action;
                                    Set<SWBFormProcessor> arr=formProcessors.get(k);
                                    if(arr==null)
                                    {
                                        arr=new TreeSet();
                                        formProcessors.put(k, arr);
                                    }
                                    arr.add(formProcessor);
                                }
                                
                            }else
                            {
                                String k=name+"-"+action;
                                Set<SWBFormProcessor> arr=formProcessors.get(k);
                                if(arr==null)
                                {
                                    arr=new TreeSet();
                                    formProcessors.put(k, arr);
                                }
                                arr.add(formProcessor);
                            }                            
                        }
                    }
                }
            }            
            
            //Load DataServices
            HashMap<String,Set<SWBDataService>> dataServices=new HashMap();         
            {
                ScriptObject dss=eng.get("dataServices");   
                Iterator<String> it=dss.keySet().iterator();
                while(it.hasNext())
                {
                    String key=it.next();
                    ScriptObject data=dss.get(key);
                    logger.log(Level.INFO,"Loading DataService:"+key);
                    SWBDataService dataService=new SWBDataService(key,data);
                    
                    Iterator<ScriptObject> dsit=data.get("dataSources").values().iterator();
                    while (dsit.hasNext()) 
                    {
                        ScriptObject dsname = dsit.next();
                        Iterator<ScriptObject> acit=data.get("actions").values().iterator();
                        while (acit.hasNext()) 
                        {
                            String action = acit.next().getValue().toString();
                            String name=dsname.getValue().toString();
                            
                            if(name.equals("*"))
                            {
                                Iterator<String> itds=dataSources.keySet().iterator();
                                while (itds.hasNext()) 
                                {
                                    name = itds.next();
                                    String k=name+"-"+action;
                                    Set<SWBDataService> arr=dataServices.get(k);
                                    if(arr==null)
                                    {
                                        arr=new TreeSet();
                                        dataServices.put(k, arr);
                                    }
                                    arr.add(dataService);
                                }
                                
                            }else
                            {
                                String k=name+"-"+action;
                                Set<SWBDataService> arr=dataServices.get(k);
                                if(arr==null)
                                {
                                    arr=new TreeSet();
                                    dataServices.put(k, arr);
                                }
                                arr.add(dataService);
                            }
                        }
                    }
                }
            }            
            
            //Load DataExtractors
            dataExtractorsStop();              
            HashMap<String,DataExtractorBase> dataExtractors=new HashMap();
            {
                ScriptObject ext=eng.get("dataExtractors");   
                logger.log(Level.INFO,"Loading Extractors");
                Iterator<String> it=ext.keySet().iterator();
                while(it.hasNext())
                {
                    String key=it.next();
                    ScriptObject data=ext.get(key);
                    String scriptEng=data.getString("scriptEngine");                            
                    if(scriptEng==null || source.equals(scriptEng))
                    //if(source.equals("/admin/ds/admin.js"))
                    {
                        try
                        {
                            DataExtractorBase dext=new DataExtractorBaseImp(key,data,this);
                            dataExtractors.put(key,dext);
                        }catch(Exception e){e.printStackTrace();} 
                    }                    
                }
            }                            
            
//            //Load UserRepository
//            {
//                ScriptObject ur=eng.get("userRepository");   
//                System.out.println("Loading UserRepository");
//                userRep=new SWBUserRepository(ur, this);
//            } 


            HashMap<String, SWBFileSource> fileSources = new HashMap();
            {
                ScriptObject dss=eng.get("fileSources");
                Iterator<String> it=dss.keySet().iterator();
                while (it.hasNext()) {
                    String dsname = it.next();
                    ScriptObject fileSource=dss.get(dsname); 
                    String fileSourceClass=fileSource.getString("class");
                    String dataStore=fileSource.getString("dataStore");
                    ScriptObject ds = eng.get("dataStores");
                    if (null!=ds){
                        ds = ds.get(dataStore);
                    } else {
                        ds = null;
                    }
                    try
                    {
                        Class cls=Class.forName(fileSourceClass);
                        Constructor c=cls.getConstructor(ScriptObject.class, ScriptObject.class);
                        logger.log(Level.INFO,"Loading FileSource:"+dsname); 
                        fileSources.put(dsname,(SWBFileSource)c.newInstance(fileSource, ds));
                    }catch(Exception e){e.printStackTrace();}        
                }
            }
            
            this.sobject=eng;      
            this.sengine=engine;
            this.dataStores=dataStores;              
            this.dataStoresCache=new HashMap();              
            this.dataSources=dataSources;     
            this.dataProcessors=dataProcessors;  
            this.formProcessors=formProcessors;  
            this.dataServices=dataServices;   
            this.dataExtractors=dataExtractors;
            this.fileSources=fileSources;
            
            lastCheck=System.currentTimeMillis();  
            id=lastCheck;      
            
            //Load DataSourceIndexes
            {
                ScriptObject dss=eng.get("dataSourceIndexes");   
                Iterator<String> it=dss.keySet().iterator();
                while (it.hasNext()) {
                    String key = it.next();
                    try
                    {
                        logger.log(Level.INFO,"Loading DataSourceIndex:"+key);                    
                        DataObject data=dss.get(key).toDataObject();
                        String scriptEng=data.getString("scriptEngine");                            
                        if(scriptEng==null || source.equals(scriptEng))
                        {
                            //{"dataSource":"User", "index":{"email":1, "birthday":-1, "fullname":"text"}}    
                            DataObject index=data.getDataObject("index");
                            if(index.size()>0)getDataSource(data.getString("dataSource")).createIndex(key, index);
                        }
                    }catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }              
            
            //loadProcesses
            if(source.equals("/admin/ds/admin.js"))
            {
                processMgr=ProcessMgr.createInstance(this);
            }            
            
            dataExtractorsStart();         
            
        }catch(Throwable e)
        {
            e.printStackTrace();
        }
    }
    
    private void dataExtractorsStart()
    {
        if(dataExtractors!=null)
        {
            Iterator<DataExtractorBase> it=dataExtractors.values().iterator();
            while (it.hasNext()) {
                DataExtractorBaseImp dataExtractor = (DataExtractorBaseImp)it.next();
                dataExtractor.start();
            }
        }           
    }
    
    private void dataExtractorsStop()
    {
        if(dataExtractors!=null)
        {
            Iterator<DataExtractorBase> it=dataExtractors.values().iterator();
            while (it.hasNext()) {
                DataExtractorBaseImp dataExtractor = (DataExtractorBaseImp)it.next();
                dataExtractor.stop();
            }
        }           
    }    
    
    /**
     *
     * @param name
     * @return
     */
    public ScriptObject getDataSourceScript(String name)
    {
        return dataSources.get(name);
    }
    
    /**
     *
     * @return
     */
    public ScriptObject getScriptObject()
    {
        return sobject;
    }
    
    /**
     *
     * @return
     */
    public Set<String> getDataSourceNames()
    {
        return dataSources.keySet();   
    }
    
    /**
     *
     * @param name
     * @return
     */
    @Override
    public SWBDataSource getDataSource(String name)
    {        
        return getDataSource(name,null);
    }
    
    /**
     *
     * @param name
     * @param modelid
     * @return
     */
    @Override
    public SWBDataSource getDataSource(String name, String modelid)
    {
        ScriptObject so=getDataSourceScript(name);
        if(so!=null)
        {
            return new SWBDataSource(name,modelid,so,this);
        }
        return null;
    }    
    
    /**
     *
     * @param name
     * @return
     */
    @Override
    public SWBDataStore getDataStore(String name)
    {
        SWBDataStore dataStoreInst=dataStoresCache.get(name);
        if(dataStoreInst==null)
        {
            synchronized(this)
            {
                dataStoreInst=dataStoresCache.get(name);
                if(dataStoreInst==null)                
                {
                    ScriptObject dataStore=dataStores.get(name);
                    String dataStoreClass=dataStore.getString("class");
                    try
                    {
                        Class cls=Class.forName(dataStoreClass);
                        Constructor c=cls.getConstructor(ScriptObject.class);
                        logger.log(Level.INFO,"Create DataStore:"+name);
                        dataStoreInst=(SWBDataStore)c.newInstance(dataStore);
                        dataStoresCache.put(name,dataStoreInst);
                    }catch(Exception e){e.printStackTrace();}                
                }
            }
        }        
        return dataStoreInst;
    }    

    /**
     * Busca los objetos SWBDataService relacionados a un especifico DataSource y una accion 
     * @param dataSource
     * @param action
     * @return Lista de SWBDataService o null si no hay SWBDataService relacionados
     */
    @Override
    public Set<SWBDataService> findDataServices(String dataSource, String action)
    {
        return dataServices.get(dataSource+"-"+action);
    }
    
    /**
     *
     * @param dataSource
     * @param action
     * @param request
     * @param response
     */
    @Override
    public void invokeDataServices(String dataSource, String action, DataObject request, DataObject response, DataObject trxParams)
    {
        invokeDataServices(this, dataSource, action, request, response, trxParams);
    }
       
    /**
     *
     * @param userengine
     * @param dataSource
     * @param action
     * @param request
     * @param response
     */
    protected void invokeDataServices(SWBScriptEngine userengine, String dataSource, String action, DataObject request, DataObject response, DataObject trxParams)
    {
        if(SWBMonitorMgr.active)SWBMonitorMgr.startMonitor("/dv/"+dataSource+"/"+action);
        long time=System.currentTimeMillis();
        if(disabledDataTransforms)return;
        
        Set<SWBDataService> set=findDataServices(dataSource, action);
        if(set!=null)
        {
            Iterator<SWBDataService> dsit=set.iterator();
            while(dsit.hasNext())
            {
                SWBDataService dsrv=dsit.next();
                ScriptObject func=dsrv.getDataServiceScript().get(SWBDataService.METHOD_SERVICE);
                if(func!=null && func.isFunction())
                {
                    try
                    {
                        func.invoke(userengine,request,response.get("response"),dataSource,action,trxParams);
                    }catch(Throwable e)
                    {
                        e.printStackTrace();
                    }
                }
            }            
            if(SWBMonitorMgr.active)SWBMonitorMgr.endMonitor();
        }else
        {
            if(SWBMonitorMgr.active)SWBMonitorMgr.cancelMonitor();
        }
    }
    
//    @Override
//    public SWBUserRepository getUserRepository()
//    {
//        return userRep;
//    }
    
    /**
     * Busca los objetos SWBDataProcessor relacionados a un especifico DataSource y una accion 
     * @param dataSource
     * @param action
     * @return Lista de SWBDataProcessor o null si no hay SWBDataService relacionados
     */
    
    @Override
    public Set<SWBDataProcessor> findDataProcessors(String dataSource, String action)
    {
        return dataProcessors.get(dataSource+"-"+action);
    }   
    
    /**
     * Busca los objetos SWBFormProcessor relacionados a un especifico DataSource y una accion 
     * @param dataSource
     * @param action
     * @return Lista de SWBDataProcessor o null si no hay SWBDataService relacionados
     */
    
    @Override
    public Set<SWBFormProcessor> findFormProcessors(String dataSource, String action)
    {
        return formProcessors.get(dataSource+"-"+action);
    }     

    /**
     *
     * @param dataSource
     * @param action
     * @param method
     * @param obj
     * @return
     */
    @Override
    public DataObject invokeDataProcessors(String dataSource, String action, String method, DataObject obj, DataObject trxParams)
    {
        return invokeDataProcessors(this, dataSource, action, method, obj, trxParams);
    }
    
    /**
     *
     * @param userengine
     * @param dataSource
     * @param action
     * @param method
     * @param obj
     * @return
     */
    protected DataObject invokeDataProcessors(SWBScriptEngine userengine, String dataSource, String action, String method, DataObject obj, DataObject trxParams)
    {
        if(disabledDataTransforms)return obj;
        if(SWBMonitorMgr.active)SWBMonitorMgr.startMonitor("/dp/"+dataSource+"/"+action+"/"+method);
        
        Set<SWBDataProcessor> set=findDataProcessors(dataSource, action);
        if(set!=null)
        {
            Iterator<SWBDataProcessor> dsit=set.iterator();
            while(dsit.hasNext())
            {
                SWBDataProcessor dsrv=dsit.next();
                ScriptObject func=dsrv.getDataProcessorScript().get(method);
                //System.out.println("func:"+func);
                if(func!=null && func.isFunction())
                {
                    try
                    {
                        ScriptObject r=func.invoke(userengine,obj,dataSource,action,trxParams);
                        if(r!=null && r.getValue() instanceof DataObject)
                        {
                            obj=(DataObject)r.getValue();
                        }
                    }catch(jdk.nashorn.internal.runtime.ECMAException ecma)
                    {
                        if(SWBMonitorMgr.active)SWBMonitorMgr.cancelMonitor();
                        throw ecma;
                    }catch(Throwable e)
                    {
                        e.printStackTrace();
                    }
                }
            }            
            if(SWBMonitorMgr.active)SWBMonitorMgr.endMonitor();
        }else
        {
            if(SWBMonitorMgr.active)SWBMonitorMgr.cancelMonitor();
        }
        return obj;
    }
    
    /**
     *
     * @param dataSource
     * @param action
     * @param method
     * @param obj
     * @return
     */
    @Override
    public DataObject invokeFormProcessors(String dataSource, String action, String method, DataObject obj, DataObject trxParams)
    {
        return invokeFormProcessors(this, dataSource, action, method, obj, trxParams);
    }
    
    /**
     *
     * @param userengine
     * @param dataSource
     * @param action
     * @param method
     * @param obj
     * @return
     */
    protected DataObject invokeFormProcessors(SWBScriptEngine userengine, String dataSource, String action, String method, DataObject obj, DataObject trxParams)
    {
        if(disabledDataTransforms)return obj;
        if(SWBMonitorMgr.active)SWBMonitorMgr.startMonitor("/dp/"+dataSource+"/"+action+"/"+method);
        
        Set<SWBFormProcessor> set=findFormProcessors(dataSource, action);
        if(set!=null)
        {
            Iterator<SWBFormProcessor> dsit=set.iterator();
            while(dsit.hasNext())
            {
                SWBFormProcessor dsrv=dsit.next();
                ScriptObject func=dsrv.getFormProcessorScript().get(method);
                //System.out.println("func:"+func);
                if(func!=null && func.isFunction())
                {
                    try
                    {
                        ScriptObject r=func.invoke(userengine,obj,dataSource,action,trxParams);
                        if(r!=null && r.getValue() instanceof DataObject)
                        {
                            obj=(DataObject)r.getValue();
                        }
                    }catch(jdk.nashorn.internal.runtime.ECMAException ecma)
                    {
                        if(SWBMonitorMgr.active)SWBMonitorMgr.cancelMonitor();
                        throw ecma;
                    }catch(Throwable e)
                    {
                        e.printStackTrace();
                    }
                }
            }            
            if(SWBMonitorMgr.active)SWBMonitorMgr.endMonitor();
        }else
        {
            if(SWBMonitorMgr.active)SWBMonitorMgr.cancelMonitor();
        }
        return obj;
    }    
    
    /**
     *
     */
    @Override
    public void reloadScriptEngine()
    {
        try
        {
            close();
            init();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    
    /**
     *
     */
    public void needsReloadAllScriptEngines()
    {
        Iterator<SWBBaseScriptEngine> it=engines.values().iterator();
        while (it.hasNext()) {
            SWBBaseScriptEngine eng = it.next();
            eng.needsReloadScriptEngine();
        }        
    }
    
    /**
     *
     */
    public void reloadAllScriptEngines()
    {
        Iterator<SWBBaseScriptEngine> it=engines.values().iterator();
        while (it.hasNext()) {
            SWBBaseScriptEngine eng = it.next();
            eng.reloadScriptEngine();
        }        
    }        
    
    /**
     * Mark script engine as changed for reload
     */
    public void needsReloadScriptEngine()
    {
        this.needsReload=true;
        lastCheck=System.currentTimeMillis();;        
    }
    
    /**
     *
     * @return
     */
    public boolean isNeedsReloadScriptEngine()
    {
        return needsReload;
    }
    
    /**
     *
     * @return
     */
    @Override
    public ScriptEngine getNativeScriptEngine()
    {
        return sengine;
    }
    
    /**
     *
     * @param script
     * @return
     * @throws ScriptException
     */
    @Override
    public Object eval(String script) throws ScriptException
    {
        return sengine.eval(script);
    }
    
    /**
     *
     * @param script
     * @param bind
     * @return
     * @throws ScriptException
     */
    @Override
    public Object eval(String script, Bindings bind) throws ScriptException
    {
        return sengine.eval(script, bind);
    }
    
    /**
     *
     * @param script
     * @return
     * @throws ScriptException
     */
    @Override
    public Object eval(Reader script) throws ScriptException
    {
        return sengine.eval(script);
    }    
    
    /**
     *
     * @param script
     * @param bind
     * @return
     * @throws ScriptException
     */
    @Override
    public Object eval(Reader script, Bindings bind) throws ScriptException
    {
        return sengine.eval(script,bind);
    }        
    
    /**
     *
     */
    @Override
    public boolean chechUpdates()
    {        
        boolean ret=false;
        {
            long time=System.currentTimeMillis();
            if((time-lastCheck)>10000)
            {
                lastCheck=time;
                if(needsReload)
                {
                    synchronized(this)
                    {
                        if(needsReload)
                        {
                            needsReload=false;
                            Thread t=new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    logger.log(Level.INFO,"reload ScriptEngine:"+source);
                                    reloadScriptEngine();
                                }
                            });
                            t.start();
                            ret=true;
                        }
                    }
                }                
                
                Iterator<SWBScriptFile> it=files.iterator();
                while (it.hasNext()) {
                    SWBScriptFile f = it.next();
                    if(f.chechUpdates())break;
                }
            }
        }
        return ret;
    }
    
    /**
     *
     * @param engine
     * @return
     */
    public Bindings getUserBindings(SWBUserScriptEngine engine)
    {
        Bindings b=null;
//        if(user==null)return null;
//        Bindings b = users.get(user);
//        System.out.println("getUserBindings:"+engine);
//        if(b==null)
//        {
//            synchronized(users)
//            {           
//                b = users.get(user);
//                if(b==null)
//                {
                    b = new SimpleBindings();     
                    Bindings enginescope=sengine.getBindings(ScriptContext.ENGINE_SCOPE);            
                    Iterator<Map.Entry<String,Object>> set=enginescope.entrySet().iterator();
                    while (set.hasNext()) {
                        Map.Entry<String, Object> entry = set.next();
                        b.put(entry.getKey(), entry.getValue());
//                        System.out.println(engine.getUser()+" prop:"+entry.getKey()+" "+entry.getValue().hashCode());
                    }
                    //b.put("_swbf_user", user);  
                    b.put("sengine", engine);
//                    users.put(user, b);
//                }
//            }
//        }
        return b;        
    }

    /**
     *
     * @return
     */
    public Bindings getUserBindings() {
        return null;
    }   

    /**
     *
     * @return
     */
    @Override
    public SWBScriptUtils getUtils() {
        return utils;
    }
    
    /**
     *
     */
    @Override
    public void close() {
        if(!closed)
        {
            synchronized(this)
            {
                if(!closed)
                {
                    closed=true;
                    dataExtractorsStop();
                    //TODO: se movio el close al finalize del datastore
                    /*
                    //DataStores..
                    Iterator<SWBDataStore> it=dataStores.values().iterator();
                    while (it.hasNext()) {
                        SWBDataStore next = it.next();
                        next.close();
                    }
                    */
                    logger.log(Level.INFO,"Closed ScriptEngine: "+source);
                }
            }
        }
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isClosed() {
        return closed;
    }

    /**
     *
     * @return
     */
    @Override
    public DataObject getUser() {
        return null;
    }    
    
//******************************** static *****************************************************//    

    /**
     *
     * @param source
     * @param internal
     * @return
     */
    
    public static SWBBaseScriptEngine getScriptEngine(String source, boolean internal)
    {
        //System.out.println("getScriptEngine:"+source);
        SWBBaseScriptEngine engine=engines.get(source);        
        if(engine==null)
        {
            synchronized(engines)
            {
                engine=engines.get(source);
                if(engine==null)
                {
                    try
                    {
                        engine=new SWBBaseScriptEngine(source,internal);
                        engine.init();
                        engines.put(source, engine);
                    }catch(Throwable e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }else
        {
            engine.chechUpdates();
        }
        return engine;
    }

    /**
     *
     * @param name
     * @return
     */
    @Override
    public SWBFileSource getFileSource(String name) {
        return fileSources.get(name);
    }

    /**
     *
     * @param key
     * @return
     */
    @Override
    public Object getContextData(String key) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param key
     * @param data
     * @return
     */
    @Override
    public Object setContextData(String key, Object data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     *
     * @param role
     * @return
     */
    @Override
    public boolean hasUserRole(String role) {
        return false;
    }
    
    /**
     *
     * @param roles
     * @return
     */
    @Override
    public boolean hasUserAnyRole(String... roles)
    {
        return false;
    }
    
    /**
     *
     * @param roles
     * @return
     */
    @Override
    public boolean hasUserAnyRole(List<String> roles)
    {
        return false;
    }

    /**
     *
     * @param group
     * @return
     */
    @Override
    public boolean hasUserGroup(String group) {
        return false;
    }

    /**
     *
     */
    @Override
    public void removeUserPermissionCache() {        
    }

    /**
     *
     * @param permission
     * @return
     */
    @Override
    public boolean hasUserPermission(String permission) {
        return false;
    }

    /**
     *
     * @return
     */
    @Override
    public String getAppName() {
        ScriptObject config = getScriptObject().get("config");
        if (config != null) {
            return config.getString("appName");
        }
        return null;
    }
    
    /**
     *
     * @return
     */
    @Override
    public boolean getDSCache() {
        ScriptObject config = getScriptObject().get("config");
        if (config != null) {
            return Boolean.parseBoolean(config.getString("dsCache"));
        }
        return false;
    }    

    /**
     *
     * @return
     */
    @Override
    public long getId() {
        return id;
    }

    @Override
    public DataObject fetchObjectById(String id) throws IOException
    {
        //"_suri:"+modelid+":"+scls+":";
        String ids[]=id.split(":");
        if(ids.length==4)return getDataSource(ids[2], ids[1]).fetchObjById(id);
        return null;
    }

    @Override
    public DataObject getObjectById(String id) {
        //"_suri:"+modelid+":"+scls+":";
        String ids[]=id.split(":");
        if(ids.length==4)return getDataSource(ids[2], ids[1]).getObjectById(id);
        return null;        
    }

    public void setDisabledDataTransforms(boolean disabledDataTransforms) {
        this.disabledDataTransforms = disabledDataTransforms;
    }

    public boolean isDisabledDataTransforms() {
        return disabledDataTransforms;
    }

    @Override
    public DataObject getConfigData() {
        ScriptObject config = this.getScriptObject().get("config");        
        return config.toDataObject();
    }

    @Override
    public String compile(String code) {
        if(code!=null)
        {
            try {
                ((Compilable)sengine).compile(code);
            } catch (ScriptException ex) {
                return ex.getLocalizedMessage();
            }
        }
        return null;
    }

    @Override
    public ProcessMgr getProcessMgr() {
        return processMgr;
    }

    @Override
    public String getContextPath() {
        return DataMgr.getContextPath();
    }
    
    /**
     *
     */
    public class SWBScriptFile
    {
        private File file=null;      
        private transient long updated;    

        /**
         *
         * @param file
         */
        public SWBScriptFile(File file) {
            this.file=file;
            this.updated=file.lastModified();
        }
        
        /**
         *
         * @return
         */
        public boolean chechUpdates()
        {
            if(file!=null && updated!=file.lastModified())
            {
                synchronized(this)
                {
                    if(updated!=file.lastModified())
                    {
                        logger.log(Level.INFO,"Update ScriptEngine:"+source);
                        reloadScriptEngine();
                        return true;
                    }
                }
            }
            return false;
        }        
                
    }

    @Override
    public DataObject getData() {
        return data;
    }

    public String getSource() {
        return source;
    }
    
}
