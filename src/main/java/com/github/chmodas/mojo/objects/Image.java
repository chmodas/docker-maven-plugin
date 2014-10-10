package com.github.chmodas.mojo.objects;

import org.apache.maven.plugin.MojoFailureException;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

public class Image {
    private String name;
    private String registry;
    private String tag;

    public Image() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegistry() {
        if (registry == null) {
            throw new IllegalArgumentException("image.registry must not be null when invoking docker:start");
        }

        return registry;
    }

    public void setRegistry(String registry) {
        this.registry = registry;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    // TODO: not sure how much I like this
    public void validate() throws MojoFailureException {
        try {
            for (PropertyDescriptor x : Introspector.getBeanInfo(Image.class).getPropertyDescriptors()) {
                if (x.getReadMethod() != null && !"class".equals(x.getName())) {
                    x.getReadMethod().invoke(this);
                }
            }
        } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
            throw new MojoFailureException(e.getCause().getMessage(), e);
        }
    }
}
