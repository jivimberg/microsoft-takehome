package com.microsoft.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.microsoft.model.ExecutionDag;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DagParser implements IDagParser {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    @NotNull
    public ExecutionDag parseDag(String dagXml) {
        try {
            DagXml dagXmlObject = xmlMapper.readValue(dagXml, DagXml.class);
            Set<DagXml.Node> nodes = new HashSet<>(dagXmlObject.nodes());
            if(nodes.size() != dagXmlObject.nodes().size()) {
                throw new IllegalArgumentException("The XML contains duplicate nodes");
            }

            return ExecutionDag.create(nodes);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse DAG XML", e);
        }
    }
}
