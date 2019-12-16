/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.semanticwb.datamanager;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import org.semanticwb.datamanager.datastore.DataStoreMongo;

/**
 *
 * @author javiersolis
 */
public class DataObject extends LinkedHashMap<String, Object> 
{
    private static SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");    
    private static SimpleDateFormat iso_base = new SimpleDateFormat("yyyy-MM-dd");
    private static DecimalFormat decimal_format = new DecimalFormat( "#,##0.00;(#,##0.00)" );    
    public static final DataObject EMPTY=new DataObject();
    
    static{
        iso.setTimeZone(TimeZone.getTimeZone("UTC"));
        //iso_base.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public Object put(String key, Object value) {
        return super.put(key, DataUtils.toData(value)); //To change body of generated methods, choose Tools | Templates.
    }    
    
    /**
     *
     * @param key
     * @param def
     * @return
     */
    public DataObject getDataObject(String key, DataObject def) {
        DataObject ret = getDataObject(key);
        if (ret != null) {
            return ret;
        }
        return def;
    }      

    /**
     *
     * @param key
     * @return
     */
    public DataObject getDataObject(String key) {
        Object obj = get(key);
        if (obj instanceof DataObject) {
            return (DataObject) obj;
        }
        return null;
    }
    
    /**
     *
     * @param key
     * @return
     */
    public DataObject getOrCreateDataObject(String key) {
        Object obj = get(key);
        if(obj==null)
        {
            synchronized(this)
            {
                obj = get(key);
                if(obj==null)
                {
                    obj=new DataObject();
                    put(key, obj);
                }
            }
            return (DataObject)obj;
        }else if (obj instanceof DataObject) {
            return (DataObject) obj;
        }
        return null;
    }    
    
    /**
     *
     * @param key
     * @param def
     * @return
     */
    public DataList getDataList(String key, DataList def) {
        DataList ret = getDataList(key);
        if (ret != null) {
            return ret;
        }
        return def;
    }    
    

    /**
     *
     * @param key
     * @return
     */
    public DataList getDataList(String key) {
        Object obj = get(key);
        if (obj instanceof DataList) {
            return (DataList) obj;
        }
        return null;
    }
    
    /**
     *
     * @param key
     * @return
     */
    public DataList getOrCreateDataList(String key) {
        Object obj = get(key);
        if(obj==null)
        {
            synchronized(this)
            {
                obj = get(key);
                if(obj==null)
                {
                    obj=new DataList();
                    put(key, obj);
                }
            }
            return (DataList)obj;            
        }
        if (obj instanceof DataList) {
            return (DataList) obj;
        }
        return null;
    }    

    /**
     *
     * @param key
     * @param def
     * @return
     */
    public String getString(String key, String def) {
        String ret = getString(key);
        if (ret != null) {
            return ret;
        }
        return def;
    }
    
    /**
     * Return String fomated uning String.format method
     * @param key
     * @param format
     * @return 
     */
    public String getStringFormated(String key, String format)
    {
        return getStringFormated(key, null, format);
    }
    
    /**
     * 
     * @param key
     * @param def
     * @param format
     * @return 
     */
    public String getStringFormated(String key, Object def, String format)
    {
        Object obj = get(key);
        if (obj == null) {
            obj=def;
        }
        if(obj==null)return null;
        return String.format(format, obj);     
    }
    

    /**
     *
     * @param key
     * @return
     */
    public String getString(String key) {
        Object obj = get(key);
        if (obj == null) {
            return null;
        }
        return obj.toString();
    }

    /**
     *
     * @param key
     * @param def
     * @return
     */
    public int getInt(String key, int def) {
        if (get(key) == null) {
            return def;
        }
        return getInt(key);
    }    

    /**
     *
     * @param key
     * @return
     */
    public int getInt(String key) {
        Object obj = get(key);
        if (obj instanceof Integer) {
            return (Integer) obj;
        }else if (obj instanceof Long) {
            return ((Long)obj).intValue();
        }else if (obj instanceof Double) {
            return ((Double)obj).intValue();
        }else if (obj instanceof Float) {
            return ((Float)obj).intValue();
        }
        try {
            String v=getString(key);
            if(v!=null)return Integer.parseInt(v);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Increment the value by inc
     * @param key
     * @param inc
     * @return 
     */
    public int incInt(String key, int inc)
    {
        int r=inc;
        synchronized(this)
        {
            r=getInt(key)+inc;
            addParam(key, r);
        }
        return r;
    }

    /**
     *
     * @param key
     * @param def
     * @return
     */
    public long getLong(String key, long def) {
        if (get(key) == null) {
            return def;
        }
        return getLong(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public long getLong(String key) {
        Object obj = get(key);
        if (obj instanceof Long) {
            return (Long) obj;
        }else if (obj instanceof Integer) {
            return ((Integer)obj).longValue();
        }else if (obj instanceof Double) {
            return ((Double)obj).longValue();
        }else if (obj instanceof Float) {
            return ((Float)obj).longValue();
        }
        try {
            String v=getString(key);
            if(v!=null)return Long.parseLong(v);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Increment the value by inc
     * @param key
     * @param inc
     * @return 
     */
    public long incLong(String key, long inc)
    {
        long r=inc;
        synchronized(this)
        {
            r=getLong(key)+inc;
            addParam(key, r);
        }
        return r;
    }    

    /**
     *
     * @param key
     * @param def
     * @return
     */
    public boolean getBoolean(String key, boolean def) {
        if (get(key) == null) {
            return def;
        }
        return getBoolean(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public boolean getBoolean(String key) {
        Object obj = get(key);
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        try {
            String v=getString(key);
            if(v!=null)return Boolean.parseBoolean(v);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *
     * @param key
     * @param def
     * @return
     */
    public double getDouble(String key, double def) {
        if (get(key) == null) {
            return def;
        }
        return getDouble(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public double getDouble(String key) {
        Object obj = get(key);
        if (obj instanceof Double) {
            return (Double) obj;
        }
        try {
            String v=getString(key);
            if(v!=null)return Double.parseDouble(v);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }
    
    public String getNumberFormated(String key)
    {
        return getNumberFormated(key, null);
    }
    
    public String getNumberFormated(String key, Object def)
    {
        return getNumberFormated(key, def, null);
    }    
    
    public String getNumberFormated(String key, Object def, String format)
    {
        Object obj = get(key);
        if (obj == null)obj=def;
        if(obj==null)return null;
        if(obj instanceof String)obj=Double.parseDouble((String)obj);
        if(format==null)        
            return decimal_format.format(obj);
        else{
            return new DecimalFormat(format).format(obj);
        }
    }
    
    /**
     *
     * @param key
     * @param def
     * @return
     */
    public Date getDate(String key, Date def) {
        Date ret=getDate(key);
        if (ret == null) {
            return def;
        }
        return ret;
    }

    /**
     *
     * @param key
     * @return
     */
    public Date getDate(String key) {
        Object obj = get(key);
        if (obj instanceof Date) {
            return (Date) obj;
        }if(obj instanceof String)
        {
            String txt=(String)obj;
            if(txt.length()>10)
            {
                try{return iso.parse(txt);}catch(ParseException e){}
            }else
            {
                try{return iso_base.parse(txt);}catch(ParseException e){}
            }
            
        }
        return null;
    }    
    
    /**
     *
     * @param key
     * @param format
     * @return
     */
    public String getDateFormated(String key, String format)
    {
        return getDateFormated(key, null, format);        
    }
    
    /**
     *
     * @param key
     * @param def
     * @param format
     * @return
     */
    public String getDateFormated(String key, Date def, String format)
    {
        return getDateFormated(key, def, format, new Locale("es","MX"));
    }
    
    /**
     *
     * @param key
     * @param def
     * @param format
     * @param locale
     * @return
     */
    public String getDateFormated(String key, Date def, String format, Locale locale)
    {
        if(format==null)format="yyyy-MM-dd HH:mm:ss.S";
        SimpleDateFormat df=new SimpleDateFormat(format, locale);
        Date d=getDate(key,def);
        if(d==null)return null;
        return df.format(d);
    }     
    
    /**
     *
     * @param key
     * @param format
     * @return
     */
    public String getISODate(String key)
    {
        Date date=getDate(key);
        if(date!=null)return DataUtils.TEXT.iso8601DateFormat(date);
        else return null;
    }    

    /**
     *
     * @param json
     * @return
     */
    public static Object parseJSON(String json) {
        return DataStoreMongo.parseJSON(json);
    }
    
    /**
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static Object parseHTTP(String url) throws IOException
    {
        return parseJSON(DataUtils.HTTP.httpGet(url));
    }   
    
    /**
     * 
     * @param url
     * @param timeout in milliseconds
     * @return
     * @throws IOException 
     */
    public static Object parseHTTP(String url, int timeout) throws IOException
    {
        return parseJSON(DataUtils.HTTP.httpGet(url, timeout));
    }     
    
    /**
     *
     * @param ident
     * @return
     */
    public String toStringHtmlScape(boolean ident)
    {
        String str=toString(ident);
        StringBuilder buf=new StringBuilder();
        int c=0;
        int i=str.indexOf("\\u",c);
        while(i>-1)
        {
            buf.append(str.substring(c,i));
            int v=Integer.parseInt(str.substring(i+2,i+6),16);
            buf.append("&#"+v+";");
            c=i+6;
            i=str.indexOf("\\u",c);
        }
        buf.append(str.substring(c));
        return buf.toString();
    }
    
    public String toString() {
        return toString(false);
    }
    
    /**
     *
     * @param ident
     * @return
     */
    public String toString(boolean ident) {
        if(ident)return toString("");
        return toString(null);
    }    

    private String toString(String sep) {
        Iterator<Entry<String, Object>> i = entrySet().iterator();
        if (!i.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            Entry<String, Object> e = i.next();
            String key = e.getKey();
            Object value = e.getValue();
            sb.append(encodeString(key,true));
            sb.append(':');
            if (value instanceof String) {
                sb.append(value == this ? "(this Map)" :  encodeString((String)value,true) );
            } else if (value instanceof Date) {
                sb.append("\""+DataUtils.TEXT.iso8601DateFormat((Date)value)+"\"");
            }else{
                sb.append(value == this ? "(this Map)" : value);
            }
            if (!i.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    /**
     *
     * @return
     */
    public String getId() {
        return getString("_id");
    }

    /**
     *
     * @return
     */
    public String getNumId() {
        String id = getId();
        return id.substring(id.lastIndexOf(":") + 1);
    }

    /**
     *
     * @return
     */
    public String getModelId() {
        String id = getId();
        int i1 = id.indexOf(":");
        if (i1 > -1) {
            int i2 = id.indexOf(":", i1 + 1);
            if (i2 > -1) {
                return id.substring(i1 + 1, i2);
            }
        }
        return null;
    }

    /**
     *
     * @return
     */
    public String getClassName() {
        String id = getId();
        int i1 = id.indexOf(":");
        if (i1 > -1) {
            int i2 = id.indexOf(":", i1 + 1);
            if (i2 > -1) {
                int i3 = id.indexOf(":", i2 + 1);
                if (i3 > -1) {
                    return id.substring(i2 + 1, i3);
                }
            }
        }
        return null;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public DataObject addParam(String key, Object value) {
        put(key, value);
        return this;
    }

    /**
     *
     * @param key
     * @return
     */
    public DataObject addSubObject(String key) {
        DataObject data = new DataObject();
        put(key, data);
        return data;
    }

    /**
     *
     * @param key
     * @return
     */
    public DataList addSubList(String key) {
        DataList data = new DataList();
        put(key, data);
        return data;
    }   
    
    /**
     *
     * @param comp
     */
    public void sort(Comparator<Map.Entry<String,Object>> comp)
    {
        entrySet().stream().sorted(comp).forEach((Map.Entry<String, Object> t) -> {
            remove(t.getKey());
            put(t.getKey(), t.getValue());
        });
    }
    
    public void orderBy(String keys[])
    {        
        for(String key:keys)
        {
            Object obj=remove(key);
            if(obj!=null)put(key, obj);
        }
    }
    
    /**
     *
     * @param value
     * @param addDoubleQuotes
     * @return
     */
    protected static String encodeString(String value, boolean addDoubleQuotes)
    {
        if(value==null)return "null";

        int len = value.length();
        boolean needEncode = false;
        char c;
        for (int i = 0; i < len; i++)
        {
            c = value.charAt(i);

            if (c >= 0 && c <= 31 || c == 34 || c == 39 || c == 60 || c == 62 || c == 92)
            {
                needEncode = true;
                break;
            }
        }

        if (!needEncode)return addDoubleQuotes ? "\"" + value + "\"" : value;

        StringBuilder sb = new StringBuilder();
        if (addDoubleQuotes)
            sb.append('"');

        for (int i = 0; i < len; i++)
        {
            c = value.charAt(i);
            if (c >= 0 && c <= 7 || c == 11 || c >= 14 && c <= 31 || c == 39 || c == 60 || c == 62)
                sb.append("\\u"+String.format("%04x", (int)c));
            else switch ((int)c)
                {
                    case 8:
                        sb.append("\\b");
                        break;

                    case 9:
                        sb.append("\\t");
                        break;

                    case 10:
                        sb.append("\\n");
                        break;

                    case 12:
                        sb.append("\\f");
                        break;

                    case 13:
                        sb.append("\\r");
                        break;

                    case 34:
                        sb.append("\\\"");
                        break;

                    case 92:
                        sb.append("\\\\");
                        break;

                    default:
                        sb.append(c);
                        break;
                }
        }

        if (addDoubleQuotes)
            sb.append('"');

        return sb.toString();
    }    
    
    /**
     * Clone recursively the dataObject with its childs
     * @return 
     */
    public DataObject cloneDataObject()
    {
        DataObject ret=new DataObject();
        
        for(Entry<String,Object> entry: entrySet())
        {
            String key=entry.getKey();
            Object obj=entry.getValue();
            if(obj instanceof DataObject)
            {
                ret.addParam(key, ((DataObject)obj).cloneDataObject());
            }else if(obj instanceof DataList)
            {
                ret.addParam(key, ((DataList)obj).cloneDataList());
            }else
            {
                ret.addParam(key, obj);
            }
        }        
        return ret;
    }
    
    /**
     * Return a DataObject with the difference of data
     * @param data
     * @return new DataObject with the difference
     */
    public DataObject difference(DataObject data)
    {
        DataObject dif=new DataObject();
        for(Map.Entry<String,Object> entry:data.entrySet())
        {
            String key=entry.getKey();
            Object obj=entry.getValue();
            if(obj instanceof DataObject)
            {
                DataObject nold=this.getDataObject(key);
                if(nold==null)nold=this.addSubObject(key);
                DataObject ndif=nold.difference((DataObject)obj);
                if(!ndif.isEmpty())
                {                        
                    dif.addParam(key, ndif);
                }
            }else if(obj instanceof DataList)
            {
                //ret.addParam(key, ((DataList)obj).cloneDataList());
            }else
            {
                Object ov=this.get(key);
                Object nv=data.get(key);

                if(nv!=null && !nv.equals(ov))
                {
                    this.addParam(key, nv);
                    dif.addParam(key, nv);                    
                }                
            }            
        }
        return dif;
    } 
    
    /**
     * Check if data if cointained in the dataobject
     * @param data
     * @return true is contained
     */
    public boolean contains(DataObject data)
    {
        DataObject dif=new DataObject();
        for(Map.Entry<String,Object> entry:data.entrySet())
        {
            String key=entry.getKey();
            Object nobj=entry.getValue();
            Object oobj=this.get(key);
                    
            if(oobj instanceof DataObject)
            {
                if(!(nobj instanceof DataObject))return false;
                if(!((DataObject) oobj).equals((DataObject)nobj))return false;
            }else if(oobj instanceof DataList)
            {
                //TODO:
            }else
            {
                if((nobj!=null && !nobj.equals(oobj)) || (nobj==null && oobj!=null))
                {
                    return false;                
                }                
            }            
        }
        return true;
    }  
    
    /**
     * Compare two DataObjects
     * @param data
     * @return true is are equals
     */
    public boolean equals(DataObject data)
    {
        return this.contains(data) && data.contains(this);
    }

}
