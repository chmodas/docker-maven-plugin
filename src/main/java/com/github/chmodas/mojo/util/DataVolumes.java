package com.github.chmodas.mojo.util;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for volumes management.
 */
public class DataVolumes {
    /**
     * List of volumes to create inside a container.
     */
    private final List<Volume> volumes = new ArrayList<>();

    /**
     * List of host directories to be mounted as volumes inside a container.
     */
    private final Map<String, Volume> binds = new LinkedHashMap<>();

    public DataVolumes(List<String> volumes) {
        if (volumes != null) {
            for (String x : volumes) {
                String[] vs = x.split(":", 2);

                Volume volume;
                if (vs.length == 1) {
                    volume = new Volume(vs[0]);
                } else {
                    volume = new Volume(vs[1]);
                    binds.put(vs[0], volume);
                }
                this.volumes.add(volume);
            }
        }
    }

    public Boolean hasVolumes() {
        return volumes.size() > 0;
    }

    public Volume[] getVolumes() {
        if (hasVolumes()) {
            return volumes.toArray(new Volume[volumes.size()]);
        }
        return new Volume[0];
    }

    public Boolean hasBinds() {
        return binds.size() > 0;
    }

    public Bind[] getBinds() {
        if (hasBinds()) {
            Bind[] bindArray = new Bind[binds.size()];
            Integer i = 0;
            for (Map.Entry<String, Volume> bind : binds.entrySet()) {
                bindArray[i] = new Bind(bind.getKey(), bind.getValue());
                i++;
            }
            return bindArray;
        }
        return new Bind[0];
    }
}
