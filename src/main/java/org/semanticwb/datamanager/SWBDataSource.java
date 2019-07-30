/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.semanticwb.datamanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.semanticwb.datamanager.datastore.SWBDataStore;
import org.semanticwb.datamanager.monitor.SWBMonitorMgr;
import org.semanticwb.datamanager.script.ScriptObject;

/**
 *
 * @author javier.solis
 */
public class SWBDataSource
{

    /**
     *
     */
    public static final String ACTION_FETCH="fetch";

    /**
     *
     */
    public static final String ACTION_AGGREGATE="aggregate";

    /**
     *
     */
    public static final String ACTION_UPDATE="update";

    /**
     *
     */
    public static final String ACTION_ADD="add";

    /**
     *
     */
    public static final String ACTION_REMOVE="remove";

    /**
     *
     */
    public static final String ACTION_VALIDATE="validate";

    /**
     *
     */
    public static final String ACTION_LOGIN="login";

    /**
     *
     */
    public static final String ACTION_LOGOUT="logout";

    /**
     *
     */
    public static final String ACTION_USER="user";

    /**
     *
     */
    public static final String ACTION_CONTEXTDATA="contextData";
    
    private String name=null;
    private String dataStoreName=null;
    private SWBScriptEngine engine=null;
    private ScriptObject script=null;
    private SWBDataStore db=null;
    private String modelid=null;
    
    private HashMap<String,DataObject> cache=new HashMap();    
    private HashMap<String,ArrayList<ScriptObject>> scriptFields=new HashMap();    
    
    private HashMap<String,String> removeDependenceFields=null;
        
    /**
     *
     * @param name
     * @param modelid
     * @param script
     * @param engine
     */
    protected SWBDataSource(String name, String modelid, ScriptObject script, SWBScriptEngine engine)
    {
        this.name=name;
        this.engine=engine;
        this.script=script;      
        this.modelid=modelid;
        dataStoreName=this.script.getString("dataStore");
        this.db=engine.getDataStore(dataStoreName);        
        if(this.db==null)throw new NoSuchFieldError("DataStore not found:"+dataStoreName);
    }

    /**
     * Regresa Nombre del DataSource
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Regresa el SWBScriptEngine que contiene a este DataSource
     * @return SWBScriptEngine
     */
    public SWBScriptEngine getScriptEngine() {
        return engine;
    }
    
    /**
     * Regresa ScriptObject con el script con la definición del datasource definida el el archivo js
     * @return ScriptObject
     */
    public ScriptObject getDataSourceScript()
    {
        return script;
    }      
    
    /**
     *
     * @return
     * @throws IOException
     */
    public DataObject fetch() throws IOException
    {
        return fetch(new DataObject());
    }    
    
//    public DataObject fetch(String query) throws IOException
//    {
//        return fetch((DataObject)JSON.parse(query));
//    }

    /**
     *
     * @return
     * @throws IOException
     */
    
    public DataObjectIterator find() throws IOException
    {
        return find(new DataObject());
    }      
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObjectIterator find(DataObject json) throws IOException
    {
        SWBMonitorMgr.startMonitor("/ds/"+name+"/"+SWBDataSource.ACTION_FETCH);
        long time=System.currentTimeMillis();
        if(canDoAction(ACTION_FETCH))
        {
            DataObject trxParams=new DataObject();
            DataObject req=engine.invokeDataProcessors(name, SWBDataSource.ACTION_FETCH, SWBDataProcessor.METHOD_REQUEST, json, trxParams);
            DataObjectIterator res=db.find(req,this);
            SWBMonitorMgr.endMonitor();
            return res;
        }else
        {
            SWBMonitorMgr.cancelMonitor();
            return new DataObjectIterator();
        }
    }  
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObjectIterator find(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return find(DataUtils.toDataObject(json));
    }  
    
    /**
     *
     * @return
     * @throws IOException
     */
    public DataObject mapByNumId() throws IOException
    {
        return mapByNumId(new DataObject());
    }    
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject mapByNumId(DataObject json) throws IOException
    {
        DataObject ret=new DataObject();
        DataObjectIterator it = find(json);
        while (it.hasNext()) {
            DataObject dev = it.next();
            ret.put(dev.getNumId(), dev);
        }     
        return ret;
    }
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject mapByNumId(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return mapByNumId(DataUtils.toDataObject(json));
    } 
    
    /**
     *
     * @return
     * @throws IOException
     */
    public DataObject mapById() throws IOException
    {
        return mapByField("_id");
    }    
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject mapById(DataObject json) throws IOException
    {
        return mapByField(json, "_id");
    }
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject mapById(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return mapByField(DataUtils.toDataObject(json),"_id");
    }     
    
    /**
     *
     * @param field
     * @return
     * @throws IOException
     */
    public DataObject mapByField(String field) throws IOException
    {
        return mapByField(new DataObject(), field);
    }    
    
    /**
     *
     * @param json
     * @param field
     * @return
     * @throws IOException
     */
    public DataObject mapByField(DataObject json, String field) throws IOException
    {
        DataObject ret=new DataObject();
        DataObjectIterator it = find(json);
        while (it.hasNext()) {
            DataObject dev = it.next();
            ret.put(dev.getString(field), dev);
        }     
        return ret;
    } 
    
    /**
     *
     * @param json
     * @param field
     * @return
     * @throws IOException
     */
    public DataObject mapByField(jdk.nashorn.api.scripting.ScriptObjectMirror json, String field) throws IOException
    {        
        return mapByField(DataUtils.toDataObject(json),field);
    }     
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject fetch(DataObject json) throws IOException
    {
        SWBMonitorMgr.startMonitor("/ds/"+name+"/"+SWBDataSource.ACTION_FETCH);
        if(canDoAction(ACTION_FETCH))
        {
            DataObject trxParams=new DataObject();
            DataObject req=engine.invokeDataProcessors(name, SWBDataSource.ACTION_FETCH, SWBDataProcessor.METHOD_REQUEST, json, trxParams);
            if(json.getInt("endRow",0)==0 && json.getInt("startRow",0)==0)json.put("endRow", 1000);
            DataObject res=db.fetch(req,this);
            res=engine.invokeDataProcessors(name, SWBDataSource.ACTION_FETCH, SWBDataProcessor.METHOD_RESPONSE, res, trxParams);
            engine.invokeDataServices(name, SWBDataSource.ACTION_FETCH, req, res, trxParams);
            SWBMonitorMgr.endMonitor();
            return res;
        }else
        {
            SWBMonitorMgr.cancelMonitor();
            return getError(-5, "Forbidden Action");
        }
    }
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject fetch(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return fetch(DataUtils.toDataObject(json));
    }
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject aggregate(DataObject json) throws IOException
    {
        SWBMonitorMgr.startMonitor("/ds/"+name+"/"+SWBDataSource.ACTION_AGGREGATE);
        if(canDoAction(ACTION_AGGREGATE))
        {        
            DataObject trxParams=new DataObject();
            DataObject req=engine.invokeDataProcessors(name, SWBDataSource.ACTION_AGGREGATE, SWBDataProcessor.METHOD_REQUEST, json, trxParams);
            DataObject res=db.aggregate(req,this);
            res=engine.invokeDataProcessors(name, SWBDataSource.ACTION_AGGREGATE, SWBDataProcessor.METHOD_RESPONSE, res, trxParams);
            engine.invokeDataServices(name, SWBDataSource.ACTION_AGGREGATE, req, res, trxParams);
            SWBMonitorMgr.endMonitor();
            return res;
        }else
        {
            SWBMonitorMgr.cancelMonitor();
            return getError(-5, "Forbidden Action");
        }            
    }
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject aggregate(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return aggregate(DataUtils.toDataObject(json));
    }    

    /**
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public DataObject addObj(DataObject obj) throws IOException
    {
        DataObject ret=null;
        DataObject req=new DataObject();
        req.put("data", obj);        
        ret=add(req);
        return ret;
    }  
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject addObj(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return addObj(DataUtils.toDataObject(json));
    }
    
    /**
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public DataObject updateObj(DataObject obj) throws IOException
    {
        DataObject ret=null;
        DataObject req=new DataObject();
        req.put("data", obj);        
        ret=update(req);
        return ret;
    }
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject updateObj(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return updateObj(DataUtils.toDataObject(json));
    }
    
    /**
     *
     * @param obj
     * @return
     * @throws IOException
     */
    public DataObject removeObj(DataObject obj) throws IOException
    {
        DataObject ret=null;
        DataObject req=new DataObject();
        req.put("data", obj);        
        ret=remove(req);
        return ret;
    }
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject removeObj(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return removeObj(DataUtils.toDataObject(json));
    }    
    
    public DataObject fetchObjByNumId(String id, DataObject def) throws IOException
    {
        DataObject ret = fetchObjByNumId(id);
        if (ret != null) {
            return ret;
        }
        return def;
    }        
    
    /**
     *
     * @param id
     * @return
     * @throws IOException
     */
    public DataObject fetchObjByNumId(String id) throws IOException
    {
        return fetchObjById(getBaseUri()+id);
    }    
    
    public DataObject fetchObjById(String id, DataObject def) throws IOException
    {
        DataObject ret = fetchObjById(id);
        if (ret != null) {
            return ret;
        }
        return def;
    }     
    
    /**
     *
     * @param id
     * @return
     * @throws IOException
     */
    public DataObject fetchObjById(String id) throws IOException
    {
        DataObject ret=null;
        DataObject req=new DataObject();
        DataObject data=new DataObject();
        data.put("_id", id);
        req.put("data", data);

        DataObject r=(DataObject)fetch(req);
        if(r!=null)
        {
            DataObject res=(DataObject)r.get("response");       
            if(res!=null)
            {
                Object s=res.get("data");
                if(s instanceof DataList)
                {
                    DataList rdata=(DataList)s;
                    if(rdata!=null && rdata.size()>0)
                    {
                        ret=(DataObject)rdata.get(0);
                    }
                }else
                {
                    System.out.println("error:"+s);
                }
            }            
        }
        return ret;
    }
    
    /**
     * Regresa Objecto de cache NumID y si no lo tiene lo carga, de lo contrario regresa el valor por default
     * @param id
     * @return 
     */    
    public DataObject getObjectByNumId(String id, DataObject def) {
        DataObject ret = getObjectByNumId(id);
        if (ret != null) {
            return ret;
        }
        return def;
    }      
    
    /**
     * Regresa Objecto de cache NumID y si no lo tiene lo carga, de lo contrario regresa null
     * @param id
     * @return 
     */
    public DataObject getObjectByNumId(String id)
    {
        return getObjectById(getBaseUri()+id);
    }    
    
    
    /**
     * Regresa Objecto de cache por ID y si no lo tiene lo carga, de lo contrario regresa el valor por default
     * @param id
     * @return 
     */    
    public DataObject getObjectById(String id, DataObject def) {
        DataObject ret = getObjectById(id);
        if (ret != null) {
            return ret;
        }
        return def;
    }    
    
    /**
     * Regresa Objecto de cache por ID y si no lo tiene lo carga, de lo contrario regresa null
     * @param id
     * @return 
     */
    public DataObject getObjectById(String id)
    {
        DataObject obj=cache.get(id);
        if(obj==null)
        {
            synchronized(cache)
            {
                obj=cache.get(id);
                if(obj==null)
                {
                    try
                    {
                        obj=fetchObjById(id);
                        cache.put(id, obj);
                    }catch(IOException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        return obj;
    }
    
    /**
     *
     * @param id
     * @return
     * @throws IOException
     */
    public DataObject removeObjByNumId(String id) throws IOException
    {
        return removeObjById(getBaseUri()+id);
    }    
    
    /**
     *
     * @param id
     * @return
     * @throws IOException
     */
    public DataObject removeObjById(String id) throws IOException
    {
        DataObject ret=null;
        DataObject req=new DataObject();
        DataObject data=new DataObject();
        data.put("_id", id);
        req.put("data", data);

        DataObject r=(DataObject)remove(req);
        if(r!=null)
        {
            ret=(DataObject)r.get("response");       
        }
        
        cache.remove(id);
        
        return ret;
    }   
    
//    public DataObject update(String query) throws IOException
//    {
//        return update((DataObject)JSON.parse(query));
//    }

    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    
    public DataObject update(DataObject json) throws IOException
    {
        SWBMonitorMgr.startMonitor("/ds/"+name+"/"+SWBDataSource.ACTION_UPDATE);
        if(canDoAction(ACTION_UPDATE))
        {         
            DataObject trxParams=new DataObject();
            DataObject req=engine.invokeDataProcessors(name, SWBDataSource.ACTION_UPDATE, SWBDataProcessor.METHOD_REQUEST, json, trxParams);
            DataObject res=db.update(req,this);
            res=engine.invokeDataProcessors(name, SWBDataSource.ACTION_UPDATE, SWBDataProcessor.METHOD_RESPONSE, res, trxParams);
            engine.invokeDataServices(name, SWBDataSource.ACTION_UPDATE, req, res, trxParams);

            if(req!=null)
            {
                DataObject data=req.getDataObject("data");
                if(data!=null)
                {
                    String id=data.getString("_id");
                    cache.remove(id);
                }
            }            
            SWBMonitorMgr.endMonitor();
            return res;
        }else
        {
            SWBMonitorMgr.cancelMonitor();
            return getError(-5, "Forbidden Action");
        } 
    }   
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject update(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return update(DataUtils.toDataObject(json));
    }
    
//    public DataObject add(String query) throws IOException
//    {
//        return add((DataObject)JSON.parse(query));
//    }

    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    
    public DataObject add(DataObject json) throws IOException
    {
        SWBMonitorMgr.startMonitor("/ds/"+name+"/"+SWBDataSource.ACTION_ADD);        
        if(canDoAction(ACTION_ADD))
        {                  
            DataObject trxParams=new DataObject();
            DataObject req=engine.invokeDataProcessors(name, SWBDataSource.ACTION_ADD, SWBDataProcessor.METHOD_REQUEST, json, trxParams);
            DataObject res=db.add(req,this);
            res=engine.invokeDataProcessors(name, SWBDataSource.ACTION_ADD, SWBDataProcessor.METHOD_RESPONSE, res, trxParams);
            engine.invokeDataServices(name, SWBDataSource.ACTION_ADD, req, res, trxParams);
            SWBMonitorMgr.endMonitor();
            return res;
        }else
        {
            SWBMonitorMgr.cancelMonitor();
            return getError(-5, "Forbidden Action");
        }             
    }  
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject add(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return add(DataUtils.toDataObject(json));
    }
    
//    public DataObject remove(String query) throws IOException
//    {
//        return remove((DataObject)JSON.parse(query));
//    }

    /**
     *
     * @return
     */
    
    public HashMap getRemoveDependenceFields()
    {
        if(removeDependenceFields==null)
        {
            synchronized(this)
            {
                if(removeDependenceFields==null)
                {
                    System.out.println("Loading removeDependence "+getName());
                    removeDependenceFields=new HashMap();
                    ScriptObject fields=script.get("fields");
                    if(fields!=null)
                    {
                        Iterator<ScriptObject> it = fields.values().iterator();
                        while (it.hasNext()) {
                            ScriptObject obj = it.next();
                            String val = obj.getString("removeDependence");
                            if(val==null && "grid".equals(obj.getString("stype")))val="true"; //TODO: crear deficionion configurable de stipos contra propiedades
                            if(val!=null && val.equals("true"))
                            {
                                String name=obj.getString("name");
                                String dss=obj.getString("dataSource");
                                removeDependenceFields.put(name,dss);
                            }
                        }
                    }
                    ScriptObject links=script.get("links");
                    if(links!=null)
                    {
                        Iterator<ScriptObject>it = links.values().iterator();
                        while (it.hasNext()) {
                            ScriptObject obj = it.next();
                            String val = obj.getString("removeDependence");
                            if(val==null || val.equals("true"))
                            {
                                String name=obj.getString("name");
                                String dss=obj.getString("dataSource");
                                removeDependenceFields.put(name,dss);
                            }
                        }     
                    }
                }
            }            
        }
        return removeDependenceFields;
    }
    
    private void removeDependence(String id) throws IOException
    {
        //System.out.println("removeDependence:"+id);
        if(id==null)return;
        DataObject obj=fetchObjById(id);
        //System.out.println("obj:"+obj);
        HashMap map=getRemoveDependenceFields();
        Iterator<String> it=map.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            String dss=(String)map.get(name);
            Object o=obj.get(name);
            //System.out.println("prop:"+name+":"+o);
            if(o instanceof DataList)
            {
                DataList list=(DataList)o;
                Iterator<String> it2=list.iterator();
                while (it2.hasNext()) {
                    String str = it2.next();
                    //System.out.println("remove m:"+str+":"+dss);
                    SWBDataSource ds=engine.getDataSource(dss);
                    ds.removeObjById(str);
                }
            }else if(o instanceof String)
            {
                //System.out.println("remove s:"+o+":"+dss);
                SWBDataSource ds=engine.getDataSource(dss);
                ds.removeObjById(o.toString());
            }
            
        }
    }
    
    private void checkRemoveDependence(DataObject json) throws IOException
    {        
        HashMap map=getRemoveDependenceFields();
        if(!map.isEmpty())
        {
            DataObject data = json.getDataObject("data");
            boolean removeByID=json.getBoolean("removeByID",true);
            if(removeByID)
            {
                String id=data.getString("_id");
                removeDependence(id);
            }else
            {
                DataObject r=fetch(json);
                if(r!=null)
                {
                    DataObject res=(DataObject)r.get("response");       
                    if(res!=null)
                    {
                        DataList rdata=(DataList)res.get("data");
                        Iterator<DataObject> it=rdata.iterator();
                        while (it.hasNext()) {
                            DataObject obj = it.next();
                            removeDependence(obj.getId());
                        }
                    }   
                }
            }
        }
    }
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject remove(DataObject json) throws IOException
    {
        SWBMonitorMgr.startMonitor("/ds/"+name+"/"+SWBDataSource.ACTION_REMOVE);
        long time=System.currentTimeMillis();
        if(canDoAction(ACTION_REMOVE))
        {          
            DataObject trxParams=new DataObject();
            DataObject req=engine.invokeDataProcessors(name, SWBDataSource.ACTION_REMOVE, SWBDataProcessor.METHOD_REQUEST, json, trxParams);
            checkRemoveDependence(json);
            DataObject res=db.remove(req,this);
            res=engine.invokeDataProcessors(name, SWBDataSource.ACTION_REMOVE, SWBDataProcessor.METHOD_RESPONSE, res, trxParams);
            engine.invokeDataServices(name, SWBDataSource.ACTION_REMOVE, req, res, trxParams);
            cache.clear();    
            SWBMonitorMgr.endMonitor();
            return res;
        }else
        {
            SWBMonitorMgr.cancelMonitor();
            return getError(-5, "Forbidden Action");
        }         
    }   
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject remove(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return remove(DataUtils.toDataObject(json));
    }
    
//    public DataObject validate(String query) throws IOException
//    {
//        return validate((DataObject)JSON.parse(query));
//    }

    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    
    public DataObject validate(DataObject json) throws IOException
    {
        SWBMonitorMgr.startMonitor("/ds/"+name+"/"+SWBDataSource.ACTION_VALIDATE);
//        String modelid=dataSource.getModelId();
//        String scls=dataSource.getClassName();
        DataObject ret=new DataObject();
        DataObject resp=new DataObject();
        DataObject errors=new DataObject();
        ret.put("response", resp);

        boolean hasErrors=false;
        

        DataObject data=(DataObject)json.get("data");
        if(data!=null)
        {
            Iterator<Map.Entry<String,Object>> it=data.entrySet().iterator();
            while(it.hasNext())
            {
                Map.Entry<String,Object> entry=it.next(); 

                String key=entry.getKey();
                Object value=entry.getValue();
                ScriptObject field=getDataSourceScriptField(key);
                if(field!=null)
                {
                    ScriptObject validators=field.get("validators");
                    if(validators!=null)
                    {
                        Iterator<ScriptObject> it2=validators.values().iterator();
                        while (it2.hasNext()) 
                        {
                            ScriptObject validator = it2.next();
                            String type=validator.getString("type");

                            if("serverCustom".equals(type))
                            {
                                ScriptObject func=validator.get("serverCondition");
                                if(func!=null)
                                {
                                    //System.out.println(key+"-->"+value+"-->"+func);
                                    ScriptObject r=func.invoke(engine,key,value,json);
                                    //System.out.println("r:"+r.getValue());
                                    if(r!=null && r.getValue().equals(false))
                                    {
                                        //System.out.println("Error...");
                                        hasErrors=true;
                                        String errmsg=validator.getString("errorMessage");
                                        if(errmsg==null)errmsg="Error..";
                                        errors.put(key, errmsg);
                                    }
                                }
                            }else if("isUnique".equals(type))
                            {
                                String id=(String)data.get("_id");
                                DataObject req=new DataObject();
                                DataObject query=new DataObject();
                                req.put("data", query);
                                query.put(key, value);
                                DataList rdata=(DataList)((DataObject)fetch(req).get("response")).get("data");                                  
                                if(rdata!=null && rdata.size()>0)
                                {
                                    if(rdata.size()>1 || id==null || !((DataObject)rdata.get(0)).get("_id").toString().equals(id))
                                    {
                                        hasErrors=true;
                                        String errmsg=validator.getString("errorMessage");
                                        //TODO:Internacionalizar...
                                        if(errmsg==null)errmsg="El valor debe de ser único..";
                                        errors.put(key, errmsg);
                                    }
                                }                                
                                //System.out.println("isUnique:"+key+"->"+value+" "+id+" "+r);
                            }
                        }
                    }
                }
            }        
        }
        
        if(hasErrors)
        {
            resp.put("status", -4);
            resp.put("errors", errors);
        }else
        {
            resp.put("status", 0);
        }
        SWBMonitorMgr.endMonitor();
        return ret;                
    } 
    
    /**
     *
     * @param json
     * @return
     * @throws IOException
     */
    public DataObject validate(jdk.nashorn.api.scripting.ScriptObjectMirror json) throws IOException
    {        
        return validate(DataUtils.toDataObject(json));
    }
    
    /**
     * Return field with name 
     * @param name
     * @return
     * @deprecated replaced by getScriptField
     */
    @Deprecated
    public ScriptObject getDataSourceScriptField(String name)
    {
        return getScriptField(name);
    }
    
    /**
     * Return field with name 
     * @param name
     * @return 
     */
    public ScriptObject getScriptField(String name)
    {
        ScriptObject fields=script.get("fields");
        ScriptObject ret=DataUtils.getArrayNode(fields, "name", name);
        if(ret==null)
        {
            fields=script.get("links");
            ret=DataUtils.getArrayNode(fields, "name", name);
        }
        return ret;
    }
    
    /**
     * return fields that have property and value
     * @param prop
     * @param value
     * @return ArrayList with fields
     */
    public ArrayList<ScriptObject> findScriptFields(String prop, String value)
    {
        ArrayList<ScriptObject> ret=scriptFields.get(prop+"-"+value);
        if(ret==null)
        {
            synchronized(this)
            {
                ret=scriptFields.get(prop+"-"+value);
                if(ret==null)
                {
                    ret=DataUtils.getArrayNodes(script.get("fields"), prop, value);
                    ret.addAll(DataUtils.getArrayNodes(script.get("links"), prop, value));   
                    scriptFields.put(prop+"-"+value, ret);
                }
            }
        }
        return ret;
    }
    
    /**
     *
     * @return
     */
    public String getModelId()
    {
        if(modelid!=null)return modelid;
        String modelid=getDataSourceScript().getString("modelid");
        //System.out.println("getModelId 1:"+modelid);
        
        Iterator it=DataUtils.TEXT.findInterStr(modelid, "{contextData.", "}");
        while(it.hasNext())
        {
            String s=(String)it.next();
            String o=(String)engine.getContextData(s);
            if(o!=null)
            {
                modelid=modelid.replace("{contextData."+s+"}", o);
            }
        }
        //System.out.println("getModelId 2:"+modelid);        
        return modelid;
    }
    
    /**
     *
     * @return
     */
    public String getClassName()
    {
        return getDataSourceScript().getString("scls");
    }    
    
    /**
     *
     * @return
     */
    public String getBaseUri()
    {
        String modelid=getModelId();
        String scls=getClassName();
        //TODO:get NS
        return "_suri:"+modelid+":"+scls+":";
        //return "_suri:http://swb.org/"+dataStoreName+"/"+modelid+"/"+scls+":";
    }
    
    /**
     *
     * @param action
     * @return
     */
    public boolean canDoAction(String action)
    {
        ScriptObject obj=script.get("security");
        if(obj!=null)
        {
            ScriptObject act=obj.get(action);
            //System.out.println("act:"+act);
            if(act!=null)
            {
                ScriptObject roles=act.get("roles");    
                //System.out.println("roles:"+roles);
                if(roles!=null)
                {
                    boolean ret=false;
                    Iterator<ScriptObject> it=roles.values().iterator();
                    while (it.hasNext()) {
                        String role = (String)it.next().getValue();
                        //System.out.println("role:"+role);
                        if("*".equals(role) || engine.hasUserRole(role))
                        {
                            ret=true;
                            break;
                        }
                    }
                    if(!ret)return false;
                }
                
                ScriptObject groups=act.get("groups"); 
                //System.out.println("groups:"+groups);
                if(groups!=null)
                {
                    boolean ret=false;
                    Iterator<ScriptObject> it=groups.values().iterator();
                    while (it.hasNext()) {
                        String group = (String)it.next().getValue();
                        //System.out.println("group:"+group);
                        if("*".equals(group) || engine.hasUserGroup(group))
                        {
                            ret=true;
                            break;
                        }
                    }
                    if(!ret)return false;
                }
                
                ScriptObject users=act.get("users"); 
                //System.out.println("groups:"+groups);
                if(users!=null)
                {
                    boolean ret=false;
                    Iterator<ScriptObject> it=users.values().iterator();
                    while (it.hasNext()) {
                        ScriptObject user = it.next();
                        boolean ret2=true;
                        Iterator<String> it2=user.keySet().iterator();
                        while (it2.hasNext()) {
                            String prop = it2.next();
                            String value= user.getString(prop);
                            //System.out.println("prop:"+prop+":"+value);
                            if(!value.equals(engine.getUser().getString(prop)))
                            {
                                ret2=false;
                                break;
                            }
                        }     
                        ret=ret2;
                        if(ret)break;
                    }
                    if(!ret)return false;
                }
                
                return true;
            }else
            {
                return true;
            }
        }else
        {
            return true;
        }
/*      
    security:{
        add:{
            roles:["director","gerente"],
            groups:["DAC","DADS"],
            users:[{sex:"male"}]    //OR
        },
        remove:{
            roles:["director"],
        },
        update:{
            roles:["director","gerente"],
        }        
    }      
*/        
    }
    
//******************************************* static *******************************/            

    /**
     *
     * @param status
     * @return
     */
    public static DataObject getError(int status)
    {
        return getError(status, null);
    }
    
    /**
     *
     * @param status
     * @param errorMessage
     * @return
     */
    public static DataObject getError(int status, String errorMessage)
    {
        DataObject ret=new DataObject();
        DataObject resp=new DataObject();
        ret.put("response", resp);        
        resp.put("status", status);
        if(errorMessage!=null)
        {
            resp.put("data", errorMessage);
        }
        return ret;        
    }
    
//    private static ScriptObject getServerValidator(ScriptObject field, String type)
//    {
//        ScriptObject validators=field.get("validators");
//        return SWBFormsUtils.getArrayNode(validators, "type", type);
//    }    

    /**
     *
     * @return
     */
    
    public String getDisplayField()
    {
        return getDataSourceScript().getString("displayField");
    }
    
}
