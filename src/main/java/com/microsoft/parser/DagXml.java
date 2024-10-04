package com.microsoft.parser;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.microsoft.model.INodeWithDependencies;

import java.util.Set;

public class DagXml {
    @JacksonXmlElementWrapper(localName = "Nodes")
    @JacksonXmlProperty(localName = "Node")
    private Set<Node> nodes;

    public Set<Node> getNodes() {
        return nodes;
    }

    public static class Node implements INodeWithDependencies {
        @JacksonXmlProperty(isAttribute = true, localName = "Id")
        private Integer id;

        @JacksonXmlElementWrapper(localName = "dependencies")
        @JacksonXmlProperty(localName = "Node")
        private Set<Node> dependencies;

        public Integer getId() {
            return id;
        }

        public Set<Node> getDependencies() {
            return dependencies;
        }

    }
}