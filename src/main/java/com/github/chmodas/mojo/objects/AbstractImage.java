package com.github.chmodas.mojo.objects;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

abstract class AbstractImage {
    private String name;
    private String repository;
    private String tag;
    private String command;
    private String hostname;
    private List<String> volumes;
    private List<String> ports;
    private List<String> links;
    private Integer wait;

    public void setName(String name) {
        this.name = name;
    }

    // TODO: better name validation
    public String getName() throws MojoExecutionException {
        if (name == null) {
            throw new MojoExecutionException("image.name must not be null");
        }
        return name.trim().toLowerCase();
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setVolumes(List<String> volumes) {
        this.volumes = volumes;
    }

    public void setPorts(List<String> ports) {
        this.ports = ports;
    }

    public void setLinks(List<String> links) {
        this.links = links;
    }

    public void setWait(Integer wait) {
        this.wait = wait;
    }
}
