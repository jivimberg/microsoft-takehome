package com.microsoft.parser;

import com.microsoft.model.Dag;

public interface IDagParser {

    /**
     * Parses a DAG XML string into a DAG object.
     *
     * @param dagXml The DAG XML string to be parsed
     * @return The response.
     */
    Dag parseDag(String dagXml);
}
