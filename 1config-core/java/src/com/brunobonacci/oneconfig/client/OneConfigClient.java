package com.brunobonacci.oneconfig.client;


import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;


public class OneConfigClient {

    static {
        Clojure.var("clojure.core", "require")
            .invoke( Clojure.read("com.brunobonacci.oneconfig"));
    }

    private static final Keyword _key         = Keyword.intern("key");
    private static final Keyword _env         = Keyword.intern("env");
    private static final Keyword _version     = Keyword.intern("version");
    private static final Keyword _contentType = Keyword.intern("content-type");
    private static final Keyword _value       = Keyword.intern("value");

    private static final IFn arraymap = Clojure.var("clojure.core", "array-map");
    private static final IFn configure = Clojure.var("com.brunobonacci.oneconfig",
                                                     "configure");
    private static final IFn deepMerge = Clojure.var("com.brunobonacci.oneconfig",
                                                     "deep-merge");


    private OneConfigClient(){}


    @SuppressWarnings("unchecked")
    public static ConfigEntry configure( String key, String env, String version ){

        final Object entry = arraymap.invoke(_key, key, _env, env, _version, version );

        final Map<Keyword,Object> config = (Map<Keyword,Object>) configure.invoke(entry);

        if( config == null ){
            return null;
        }

        return new ConfigEntry(config);
    }


    @SuppressWarnings("unchecked")
    public static Map<Object,Object> deepMerge( Map<Object,Object> map1,
                                                Map<Object,Object> map2 ){
        return (Map<Object,Object>) deepMerge.invoke( map1, map2 );
    }

    @SuppressWarnings("unchecked")
    public static Map<Keyword,Object> deepMergeEdnMaps( Map<Keyword,Object> map1,
                                                        Map<Keyword,Object> map2 ){
        return (Map<Keyword,Object>) deepMerge.invoke( map1, map2 );
    }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> deepMergeJsonMaps( Map<String,Object> map1,
                                                        Map<String,Object> map2 ){
        return (Map<String,Object>) deepMerge.invoke( map1, map2 );
    }

    public static class ConfigEntry {

        private Map<Keyword,Object> _entry = null;

        ConfigEntry(Map<Keyword,Object> entry){
            _entry = entry;
        }

        public String getKey() {
            return (String) _entry.get(OneConfigClient._key);
        }

        public String getEnv() {
            return (String) _entry.get(OneConfigClient._env);
        }

        public String getVersion() {
            return (String) _entry.get(OneConfigClient._version);
        }

        public String getContentType() {
            return (String) _entry.get(OneConfigClient._contentType);
        }

        public Object getValue() {
            return _entry.get(OneConfigClient._value);
        }

        public Properties getValueAsProperties() {
            return (Properties) _entry.get(OneConfigClient._value);
        }

        @SuppressWarnings("unchecked")
        public Map<Object,Object> getValueAsMap() {
            return (Map<Object,Object>) _entry.get(OneConfigClient._value);
        }

        @SuppressWarnings("unchecked")
        public Map<String,Object> getValueAsJsonMap() {
            return (Map<String,Object>) _entry.get(OneConfigClient._value);
        }

        @SuppressWarnings("unchecked")
        public Map<Keyword,Object> getValueAsEdnMap() {
            return (Map<Keyword,Object>) _entry.get(OneConfigClient._value);
        }

        public String getValueAsString() {
            return (String) _entry.get(OneConfigClient._value);
        }

        public String toString() {
            return _entry.toString();
        }
    }
}
