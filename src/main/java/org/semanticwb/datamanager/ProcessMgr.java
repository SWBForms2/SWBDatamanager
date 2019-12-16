/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.semanticwb.datamanager;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author javiersolis
 */
public class ProcessMgr {
    private static final Logger logger = Logger.getLogger(ProcessMgr.class.getName());    
    private SWBScriptEngine eng;
    private HashMap<String,SWBProcess> processes=new HashMap();
    
    private HashMap<String,DataObject> transitionsTimers=new HashMap();             //key = transition_numid - res_numid

    private ProcessMgr(SWBScriptEngine eng) {
        logger.log(Level.INFO,"Initializing ProcessMgr");                
        this.eng=eng;
        loadTimers();
    }
    
    /**
     * Create an instance of ProcessMgr
     * @param eng SWBScriptEngine
     * @return
     */
    protected static ProcessMgr createInstance(SWBScriptEngine eng)
    {
        return new ProcessMgr(eng);
    }
    
    /**
     * Return Process Object by id
     * @param id
     * @return
     * @throws IOException 
     */
    public SWBProcess getProcess(String id) throws IOException
    {
        SWBProcess ret=processes.get(id);
        if(ret==null)
        {
            synchronized(this)
            {
                ret=processes.get(id);
                if(ret==null)
                {
                    DataObject proc=eng.getDataSource("SWBF_Process").fetchObjById(id);
                    if(proc!=null)
                    {
                        ret=new SWBProcess(eng, proc);
                        processes.put(id, ret);
                    }
                }
            }
        }
        return ret;
    }
    
    /**
     * Process the accion to change the states of the resource data
     * @param user_eng
     * @param data the resource
     * @param trxParams the transaction params
     * @return error message or null if not error
     * @throws IOException 
     */
    public String processAction(SWBScriptEngine user_eng, DataObject data, DataObject trxParams) throws IOException
    {   
        String errorMsg=null;
        DataObject user=user_eng.getUser();
        SWBScriptEngine admin_eng=DataMgr.getUserScriptEngine("/admin/ds/admin.js", null);
        
        DataObject _swbf_processAction=(DataObject)data.remove("_swbf_processAction");  //{"itrn":"<%=transition.getNumId()%>","action":"[save]"} 
        
        //System.out.println("_swbf_processAction:"+_swbf_processAction);
        
        DataObject transition=admin_eng.getDataSource("SWBF_Transition").fetchObjByNumId(_swbf_processAction.getString("itrn"));
        String action=_swbf_processAction.getString("action");
        if(transition!=null)
        {
            SWBProcess process=admin_eng.getProcessMgr().getProcess(transition.getString("process"));
            String processDS=process.getDataObject().getString("ds");
            String asigProp=transition.getString("asigProp");
            String dateProp=transition.getString("dateProp");
            
            //Asignar Usuarios automaticamente
            if(asigProp!=null && asigProp.startsWith(processDS))
            {
                asigProp=asigProp.substring(processDS.length()+1);
                if(data.getString(asigProp)==null)
                {
                    data.addParam(asigProp, user.getId());
                }
            }
            
            //Asignar Fecha automaticamente
            if(dateProp!=null && dateProp.startsWith(processDS))
            {
                dateProp=dateProp.substring(processDS.length()+1);
                data.addParam(dateProp, new Date());
            }
            
            
            //Cambiar de estado
            if(action!=null)
            {
                DataList<DataObject> transStates=process.getTransitionStates(transition.getId());
                for(DataObject trstate:transStates)
                {
                    //System.out.println("trstate:"+trstate);
                    if(action.equals(trstate.getString("action")))
                    {
                        DataList<String> states=trstate.getDataList("states");
                        for(String st:states)
                        {
                            DataObject state=process.getState(st);
                            String prop=state.getString("prop");
                            String value=state.getString("value");
                            if(prop!=null && prop.startsWith(processDS))
                            {
                                String sprop=prop.substring(processDS.length()+1);
                                
                                String lastValue=data.getString(sprop);
                                DataObject lastState=process.findState(prop, lastValue);   
                                
                                data.addParam(sprop, value);
                                
                                //Registrar Procesor y service del state
                                if(state.getString("processor","").length()>0)
                                {
                                    trxParams.addParam("_swbf_processProcessor", trstate);
                                    trxParams.addParam("_swbf_processProcessorScript", state.getString("processor",""));
                                }                                
                                
                                if(state.getString("service","").length()>0)
                                {
                                    trxParams.addParam("_swbf_processService", trstate);
                                    trxParams.addParam("_swbf_processServiceScript", state.getString("service",""));
                                }                                                                
                                
                                //Create Log Object
                                DataObject log=new DataObject();
                                log.addParam("date", new Date());
                                log.addParam("ds", processDS);
                                //log.addParam("resid", process.getDataObject().getId());
                                log.addParam("process", process.getDataObject().getId());
                                log.addParam("process_name", process.getDataObject().getString("name"));
                                log.addParam("user", user.getId());
                                log.addParam("user_fullname", user.getString("fullname"));
                                log.addParam("transition", transition.getId());
                                log.addParam("transition_name", transition.getString("name"));
                                log.addParam("action", action);
                                log.addParam("action_title", trstate.getString("title"));
                                if(lastState!=null)
                                {
                                    log.addParam("lastState", lastState.getId());
                                    log.addParam("lastState_name", lastState.getString("name"));
                                }
                                log.addParam("actualState", state.getId());
                                log.addParam("actualState_name", state.getString("name"));
                                
                                trxParams.addParam("_swbf_processLog", log);
                            }
                        }
                    }
                }
            }            
            //System.out.println("asigProp:"+asigProp);
            //System.out.println("action:"+action);
            //System.out.println("transition:"+transition);
            //System.out.println("process:"+process.getDataObject());
            //System.out.println("user:"+user);
        }
        return errorMsg;
    }   
    
    /**
     * Load transitions for check timers
     */
    public void loadTimers()
    {
        try
        {
            DataObject states=eng.getDataSource("SWBF_State").mapById();
            DataObject query=new DataObject();
            query.addSubObject("data").addParam("type", "timer");                        
            DataObjectIterator it=eng.getDataSource("SWBF_Transition").find(query);            
            while (it.hasNext()) {
                DataObject trans = it.next();
                DataList<String> sourceStates=trans.getDataList("sourceStates");       
                DataList<String> resources=null;
                for(String stateid:sourceStates)
                {
                    DataObject state=states.getDataObject(stateid);
                    query=new DataObject();
                    query.addSubObject("data").addParam(state.getString("prop").substring(state.getString("ds").length()+1), state.getString("value"));       
                    SWBDataSource ds=eng.getDataSource(state.getString("ds"));
                    if(ds!=null)
                    {
                        DataObject map=ds.mapByNumId(query);
                        if(resources==null)
                        {
                            resources=new DataList();
                            resources.addAll(map.keySet());
                        }else
                        {
                            resources.retainAll(map.keySet());
                        }
                    }
                }
                if(resources!=null)
                {
                    for(String numid:resources)
                    {
                        transitionsTimers.put(trans.getNumId()+"-"+numid, trans);
                        //System.out.println("transitionsTimers:"+trans.getNumId()+"-"+numid);
                    }                
                }
            }                        
        }catch(Exception e)
        {
            logger.log(Level.SEVERE,"Error loading process timers...",e);
        }
    }
    
    public void clearCache()
    {
        processes.clear();
        transitionsTimers.clear();
        loadTimers();
    }
    
}
