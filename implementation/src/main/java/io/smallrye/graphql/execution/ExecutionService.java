/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.smallrye.graphql.execution;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.jboss.logging.Logger;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutionId;

/**
 * Executing the GraphQL request
 * 
 * @author Phillip Kruger (phillip.kruger@redhat.com)
 */
@RequestScoped // TODO: Dependent ?
public class ExecutionService {
    private static final Logger LOG = Logger.getLogger(ExecutionService.class.getName());

    @Inject
    private GraphQL graphQL;

    public JsonObject execute(JsonObject jsonInput) {
        String query = jsonInput.getString(QUERY);

        Map<String, Object> variables = toMap(jsonInput.getJsonObject(VARIABLES));

        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables)
                .executionId(ExecutionId.generate())
                .build();

        ExecutionResult executionResult = this.graphQL.execute(executionInput);

        Object pojoData = executionResult.getData();

        JsonObjectBuilder returnObjectBuilder = Json.createObjectBuilder();

        LOG.error(pojoData);

        if (pojoData != null) {
            JsonObject data = getJsonObject(pojoData);
            returnObjectBuilder = returnObjectBuilder.add(DATA, data);
        } else {
            returnObjectBuilder = returnObjectBuilder.addNull(DATA);
        }
        return returnObjectBuilder.build();
    }

    private JsonObject getJsonObject(Object pojo) {
        JsonbConfig config = new JsonbConfig()
                .withNullValues(Boolean.TRUE)
                .withFormatting(Boolean.TRUE);

        Jsonb jsonb = JsonbBuilder.create(config);

        String json = jsonb.toJson(pojo);

        final JsonReader reader = Json.createReader(new StringReader(json));
        return reader.readObject();
    }

    private Map<String, Object> toMap(JsonObject jo) {
        Map<String, Object> ro = new HashMap<>();
        Set<Map.Entry<String, JsonValue>> entrySet = jo.entrySet();
        for (Map.Entry<String, JsonValue> es : entrySet) {
            ro.put(es.getKey(), toObject(es.getValue()));
        }
        return ro;
    }

    private Object toObject(JsonValue jsonValue) {
        Object ret = null;
        JsonValue.ValueType typ = jsonValue.getValueType();
        if (null != typ)
            switch (typ) {
                case NUMBER:
                    ret = ((JsonNumber) jsonValue).bigDecimalValue();
                    break;
                case STRING:
                    ret = ((JsonString) jsonValue).getString();
                    break;
                case FALSE:
                    ret = Boolean.FALSE;
                    break;
                case TRUE:
                    ret = Boolean.TRUE;
                    break;
                case ARRAY:
                    JsonArray arr = (JsonArray) jsonValue;
                    List<Object> vals = new ArrayList<>();
                    int sz = arr.size();
                    for (int i = 0; i < sz; i++) {
                        JsonValue v = arr.get(i);
                        vals.add(toObject(v));
                    }
                    ret = vals;
                    break;
                case OBJECT:
                    ret = jsonValue.toString();
                    break;
                default:
                    break;
            }
        return ret;
    }

    private static final String QUERY = "query";
    private static final String VARIABLES = "variables";
    private static final String DATA = "data";
}