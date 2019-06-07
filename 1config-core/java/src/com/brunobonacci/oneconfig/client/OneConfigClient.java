package com.brunobonacci.oneconfig.client;


import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;


// <T extends Map<? extends Object, T>>

public class OneConfigClient {

    static {
        Clojure.var("clojure.core", "require")
            .invoke( Clojure.read("com.brunobonacci.oneconfig"));
        Clojure.var("clojure.core", "require")
            .invoke( Clojure.read("clojure.walk"));
    }

    private static final Keyword _key         = Keyword.intern("key");
    private static final Keyword _env         = Keyword.intern("env");
    private static final Keyword _version     = Keyword.intern("version");
    private static final Keyword _contentType = Keyword.intern("content-type");
    private static final Keyword _value       = Keyword.intern("value");

    private static final IFn arrayMap  = Clojure.var("clojure.core", "array-map");
    private static final IFn getIn     = Clojure.var("clojure.core", "get-in");
    private static final IFn configure = Clojure.var("com.brunobonacci.oneconfig",
                                                     "configure");
    private static final IFn deepMerge = Clojure.var("com.brunobonacci.oneconfig",
                                                     "deep-merge");
    private static final IFn stringify = Clojure.var("clojure.walk", "stringify-keys");


    private OneConfigClient(){}


    @SuppressWarnings("unchecked")
    public static ConfigEntry configure( String key, String env, String version ){

        final Object entry = arrayMap.invoke(_key, key, _env, env, _version, version );

        final Map<? extends Object, ? extends Object> config = (Map<? extends Object, ? extends Object>) configure.invoke(entry);

        if( config == null ){
            return null;
        }

        return new ConfigEntry(config);
    }


    @SuppressWarnings("unchecked")
    public static Map<? extends Object, ? extends Object> deepMerge(
        Map<? extends Object, ? extends Object> map1,
        Map<? extends Object, ? extends Object> map2 ){
        return (Map<? extends Object, ? extends Object>) deepMerge.invoke( map1, map2 );
    }

    /*
    @SuppressWarnings("unchecked")
    public static Map<? extends Object,? extends Object> deepMergeEdnMaps(
        Map<? extends Object,? extends Object> map1,
        Map<? extends Object,? extends Object> map2 ){
        return (Map<? extends Object,? extends Object>) deepMerge.invoke( map1, map2 );
    }
    */

    @SuppressWarnings("unchecked")
    public static Map<? extends Object, ? extends Object> deepMergeJsonMaps(
        Map<? extends Object, ? extends Object> map1,
        Map<? extends Object, ? extends Object> map2 ){
        return (Map<? extends Object, ? extends Object>) deepMerge.invoke( map1, map2 );
    }

    public static class ConfigEntry {

        private Map<? extends Object, ? extends Object> _entry = null;

        ConfigEntry(Map<? extends Object, ? extends Object> entry){
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
        public Map<? extends Object, ? extends Object> getValueAsMap() {
            return (Map<? extends Object, ? extends Object>) _entry.get(OneConfigClient._value);
        }

        @SuppressWarnings("unchecked")
        public Map<String, ? extends Object> getValueAsJsonMap() {
            return (Map<String, ? extends Object>) OneConfigClient.stringify.invoke(_entry.get(OneConfigClient._value));
        }

        @SuppressWarnings("unchecked")
        public Map<? extends Object, ? extends Object> getValueAsEdnMap() {
            return (Map<? extends Object, ? extends Object>) _entry.get(OneConfigClient._value);
        }

        public String getValueAsString() {
            return (String) _entry.get(OneConfigClient._value);
        }

        @SuppressWarnings("unchecked")
        public Object getIn( Object ... keys ){
            Map<? extends Object, ? extends Object> map = (Map<? extends Object, ? extends Object>) _entry.get(OneConfigClient._value);
            if( map != null ){
                return OneConfigClient.getIn.invoke(map, keys);
            } else {
                return null;
            }
        }

        public String toString() {
            return _entry.toString();
        }
    }
}
