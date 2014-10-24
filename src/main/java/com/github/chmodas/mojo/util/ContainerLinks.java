package com.github.chmodas.mojo.util;


import com.github.dockerjava.api.model.Link;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.*;

/**
 * Utility for linking containers.
 */
public class ContainerLinks {
    private final String prefix;
    private final Map<String, String> links = new LinkedHashMap<>();

    public ContainerLinks(String containerName, String prefix, List<String> links) throws MojoExecutionException {
        Used.addContainer(containerName);

        this.prefix = prefix;

        if (links != null) {
            for (String link : links) {
                String[] sl = link.split(":", 2);

                String name;
                String alias;

                if (sl.length == 1) {
                    name = alias = sl[0];
                } else {
                    name = sl[0];
                    alias = sl[1];
                }

                addLink(name, alias);
            }
        }
    }

    private void addLink(String name, String alias) throws MojoExecutionException {
        if (Used.hasLink(name)) {
            throw new MojoExecutionException("A link for container '" + name + "' with alias '" + alias + "' exists already.");
        } else {
            Used.addLink(name);
        }

        links.put(name, alias);
    }

    public Link[] getLinks() {
        if (links.size() > 0) {
            Link[] linksArray = new Link[links.size()];
            Integer i = 0;
            for (Map.Entry<String, String> entry : links.entrySet()) {
                linksArray[i] = new Link(prefix + "-" + entry.getKey(), entry.getValue());
                i++;
            }
            return linksArray;
        }
        return new Link[0];
    }

    public static class Used {
        private static String containerName;
        private static Map<String, List<String>> usedLinks = new HashMap<>();

        public static void reset() {
            usedLinks = new HashMap<>();
        }

        public static void verify() {

        }

        private static void addContainer(String name) {
            containerName = name;

            usedLinks.put(name, new ArrayList<String>());
        }

        private static Boolean hasLink(String containerName) {
            return usedLinks.get(Used.containerName).contains(containerName);
        }

        private static void addLink(String name) {
            usedLinks.get(containerName).add(name);
        }
    }
}
