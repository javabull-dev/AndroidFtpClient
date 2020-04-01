package com.ljpc.createfile.component;

import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;
import lombok.NonNull;

/**
 * 注册所有的MainActivity中的组件
 */
@Getter
public class MainActivityComponentManager {
    static final ConcurrentHashMap<String, Object> map = new ConcurrentHashMap<>();

    private MainActivityComponentManager(){
    }

    public static Object get(@NonNull String name){
        return map.get(name);
    }

    public static void put(@NonNull String name,@NonNull Object view){
        map.put(name,view);
    }


}
