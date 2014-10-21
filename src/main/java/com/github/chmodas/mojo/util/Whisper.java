package com.github.chmodas.mojo.util;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

/**
 * An Image becomes a whisper.
 */
public class Whisper {
    private String name;
    private String repository;
    private DataVolumes dataVolumes;
    private PortMapping portMapping;

    public Whisper() {
    }

    public String getName() {
        return name;
    }

    public void setName(String prefix, String name) throws MojoExecutionException {
        if (name == null) {
            throw new MojoExecutionException("image.name must not be null");
        }

        this.name = prefix + "-" + name.trim().toLowerCase();
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) throws MojoExecutionException {
        if (repository == null) {
            throw new MojoExecutionException("image.repository must not be null");
        }
        // TODO: have a better check here.

        this.repository = repository;
    }

    public DataVolumes getDataVolumes() {
        return this.dataVolumes;
    }

    public void setDataVolumes(List<String> volumes) {
        if (volumes != null) {
            this.dataVolumes = new DataVolumes(null, volumes);
        }
    }

    public void setPortMapping(List<String> ports) throws MojoExecutionException {
        if (ports != null) {
            this.portMapping = new PortMapping(ports);
        }
    }
}
