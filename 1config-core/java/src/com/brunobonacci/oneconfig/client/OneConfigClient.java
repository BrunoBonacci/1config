package com.brunobonacci.oneconfig.client;


import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;


public class OneConfigClient {

    static {
        Clojure.var("clojure.core", "require").invoke( Clojure.read("com.brunobonacci.oneconfig"));
    }

    private static final Keyword _key     = Keyword.intern("key");
    private static final Keyword _env     = Keyword.intern("env");
    private static final Keyword _version = Keyword.intern("version");
    private static final Keyword _value   = Keyword.intern("value");

    private static final IFn arraymap = Clojure.var("clojure.core", "array-map");
    private static final IFn configure = Clojure.var("com.brunobonacci.oneconfig", "configure");


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

        public Object getValue() {
            return _entry.get(OneConfigClient._value);
        }

        public Properties getValueAsProperties() {
            return (Properties) _entry.get(OneConfigClient._value);
        }

        @SuppressWarnings("unchecked")
        public Map<String,Object> getValueAsJsonMap() {
            return (Map<String,Object>) _entry.get(OneConfigClient._value);
        }

        public String getValueAsString() {
            return (String) _entry.get(OneConfigClient._value);
        }

        public String toString() {
            return _entry.toString();
        }
    }
}
