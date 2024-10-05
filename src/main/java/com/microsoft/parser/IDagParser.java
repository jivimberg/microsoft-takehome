package com.microsoft.parser;

import com.microsoft.model.ExecutionDag;

public interface IDagParser {

    /**
     * Parses a DAG XML string into a DAG object.
     *
     * @param dagXml The DAG XML string to be parsed
     * @return The response.
     */
    ExecutionDag parseDag(String dagXml);
}
