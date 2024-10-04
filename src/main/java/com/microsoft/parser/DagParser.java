package com.microsoft.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.microsoft.model.Dag;
import com.microsoft.model.INodeWithDependencies;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class DagParser implements IDagParser {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    @NotNull
    public Dag parseDag(String dagXml) {
        try {
            DagXml dagXmlObject = xmlMapper.readValue(dagXml, DagXml.class);
            return Dag.create(dagXmlObject.getNodes());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse DAG XML", e);
        }
    }
}
