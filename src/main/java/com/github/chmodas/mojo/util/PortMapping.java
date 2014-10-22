package com.github.chmodas.mojo.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.*;

/**
 * Utility for mapping ports.
 */
public class PortMapping {
    private final Map<String, ExposedPort> exposedPortMap = new LinkedHashMap<>();
    private final Ports portsBinding = new Ports();

    public PortMapping(List<String> ports) throws MojoExecutionException {
        if (ports != null) {
            for (String port : ports) {

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
                        addStaticMapping(hostPort, exposedPort);
                    } catch (NumberFormatException e) {
                        addDynamicMapping(ps[0], exposedPort);
                    }

                } catch (NumberFormatException e) {
                    throw new MojoExecutionException(
                            "Invalid port mapping '" + port + "'. " +
                            "Port mapping must be given in the format <hostPort>:<exposedPort> (e.g. 80:80).");
                }
            }
        }
    }

    private void addStaticMapping(Integer hostPort, Integer containerPort) throws MojoExecutionException {
        if (Used.hasPort(hostPort)) {
            throw new MojoExecutionException(
                    "The port '" + hostPort + "' is specified for use in multiple containers. " +
                    "One cannot run multiple containers that use the same port on the same host.");
        } else {
            Used.addPort(hostPort);
        }

        ExposedPort exposedPort = ExposedPort.tcp(containerPort);
        exposedPortMap.put(hostPort.toString(), exposedPort);
        portsBinding.bind(exposedPort, Ports.Binding(hostPort));
    }

    private void addDynamicMapping(String variableName, Integer containerPort) throws MojoExecutionException {
        if (Used.hasVariableName(variableName)) {
            throw new MojoExecutionException(
                    "The variable '" + variableName + "' is specified for us in multiple containers. " +
                    "One cannot use the same variable for multiple ports.");
        } else {
            Used.addVariableName(variableName);
        }

        ExposedPort exposedPort = ExposedPort.tcp(containerPort);
        exposedPortMap.put(variableName, exposedPort);
        /**
         * Setting the Ports.Binding to 0 forces a dynamic port allocation on the host.
         */
        portsBinding.bind(exposedPort, Ports.Binding(0));
    }

    public ExposedPort[] getExposedPorts() {
        if (exposedPortMap.size() > 0) {
            ExposedPort[] exposedPorts = new ExposedPort[exposedPortMap.size()];
            Integer i = 0;
            for (Map.Entry<String, ExposedPort> entry : exposedPortMap.entrySet()) {
                exposedPorts[i] = entry.getValue();
                i++;
            }
            return exposedPorts;

        }

        return new ExposedPort[0];
    }

    public Ports getPortsBinding() {
        return portsBinding;
    }

    public Map<String, String> getDynamicPortsBinding(DockerClient dockerClient, String containerId) {
        Map<String, String> dynamicPortsBinding = new HashMap<>();

        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
        for (Map.Entry<String, ExposedPort> entry : exposedPortMap.entrySet()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                Integer.parseInt(entry.getKey());
            } catch (NumberFormatException e) {
                Ports.Binding portsBinding = response.getNetworkSettings().getPorts().getBindings().get(entry.getValue());
                Integer hostPort = portsBinding.getHostPort();
                dynamicPortsBinding.put(entry.getKey(), hostPort.toString());
            }
        }
        return dynamicPortsBinding;
    }

    public static class Used {
        private static List<Integer> usedPorts;
        private static List<String> usedVariableNames;

        public static void reset() {
            usedPorts = new ArrayList<>();
            usedVariableNames = new ArrayList<>();
        }

        public static void addPort(Integer port) {
            usedPorts.add(port);
        }

        public static Boolean hasPort(Integer port) {
            return usedPorts.contains(port);
        }

        public static void addVariableName(String variableName) {
            usedVariableNames.add(variableName);
        }

        public static Boolean hasVariableName(String variableName) {
            return usedVariableNames.contains(variableName);
        }
    }
}
