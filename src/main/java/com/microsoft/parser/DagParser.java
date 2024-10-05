package com.microsoft.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.microsoft.model.ExecutionDag;
import org.jetbrains.annotations.NotNull;

public class DagParser implements IDagParser {

    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    @NotNull
    public ExecutionDag parseDag(String dagXml) {
        try {
            DagXml dagXmlObject = xmlMapper.readValue(dagXml, DagXml.class);
            return ExecutionDag.create(dagXmlObject.getNodes());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse DAG XML", e);
        }
    }
}
