package com.github.chmodas;

import com.github.chmodas.mojo.objects.Image;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PortMapping {
    private final DockerClient dockerClient;
    private final Map<String, List<Entry<Integer, Integer>>> staticMap = new HashMap<>();
    private final Map<String, List<Entry<String, Integer>>> dynamicMap = new HashMap<>();
    private final List<Integer> usedHostPorts = new ArrayList<>();
    private final List<String> usedVariableNames = new ArrayList<>();

    public PortMapping(DockerClient dockerClient, List<Image> images) throws MojoExecutionException {
        this.dockerClient = dockerClient;

        for (Image image : images) {
            if (image.getPorts() != null && image.getPorts().size() > 0) {
                for (String port : image.getPorts()) {
                    String[] ps = port.split(":", 2);
                    if (ps.length != 2) {
                        throw new MojoExecutionException(
                                "Invalid port mapping '" + port + "'. " +
                                "Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).");
                    }

                    try {
                        Integer exposedPort = Integer.parseInt(ps[1]);
                        Integer hostPort;

                        try {
                            hostPort = Integer.parseInt(ps[0]);
                            addStaticMapEntry(image.getName(), hostPort, exposedPort);
                        } catch (NumberFormatException e) {
                            addDynamicMapEntry(image.getName(), ps[0], exposedPort);
                        }

                    } catch (NumberFormatException e) {
                        throw new MojoExecutionException(
                                "Invalid port mapping '" + port + "'. " +
                                "Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).");
                    }
                }
            }

        }
    }

    private void addStaticMapEntry(String containerName, Integer hostPort, Integer exposedPort) throws MojoExecutionException {
        if (usedHostPorts.contains(hostPort)) {
            throw new MojoExecutionException(
                    "The port '" + hostPort + "' is specified for use in multiple containers. " +
                    "One cannot run multiple containers that use the same port on the same host.");
        } else {
            usedHostPorts.add(hostPort);
        }

        Entry<Integer, Integer> entry = new SimpleEntry<>(hostPort, exposedPort);
        if (staticMap.containsKey(containerName)) {
            staticMap.get(containerName).add(entry);
        } else {
            List<Entry<Integer, Integer>> entries = new ArrayList<>();
            entries.add(entry);
            staticMap.put(containerName, entries);
        }
    }

    private void addDynamicMapEntry(String containerName, String variableName, Integer exposedPort) throws MojoExecutionException {
        if (usedVariableNames.contains(variableName)) {
            throw new MojoExecutionException(
                    "The variable '" + variableName + "' is specified for us in multiple containers. " +
                    "One cannot use the same variable for multiple ports.");
        } else {
            usedVariableNames.add(variableName);
        }

        Map.Entry<String, Integer> entry = new SimpleEntry<>(variableName, exposedPort);
        if (dynamicMap.containsKey(containerName)) {
            dynamicMap.get(containerName).add(entry);
        } else {
            List<Entry<String, Integer>> entries = new ArrayList<>();
            entries.add(entry);
            dynamicMap.put(containerName, entries);
        }
    }

    public List<Entry<Integer, Integer>> getStaticPortsMap(String containerName) {
        return staticMap.get(containerName);
    }

    public List<Entry<String, Integer>> getDynamicPortsMap(String containerName) {
        return dynamicMap.get(containerName);
    }

    public List<Entry<String, Integer>> getDynamicPortsForVariables(String containerName, String containerId) {
        List<Entry<String, Integer>> portsMap = new ArrayList<>();

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();

        for (Entry<String, Integer> entry : dynamicMap.get(containerName)) {
            Ports.Binding portsBinding = response.getNetworkSettings().getPorts().getBindings().get(ExposedPort.tcp(entry.getValue()));
            portsMap.add(new SimpleEntry<>(entry.getKey(), portsBinding.getHostPort()));
        }

        return portsMap;
    }
}
