package edu.college.employee.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebRequestParsingTest {
    @Test
    void parsesJsonObjects() throws Exception {
        var result = Web.body(new ByteArrayInputStream("{\"value\":\"ok\"}".getBytes(StandardCharsets.UTF_8)));
        assertEquals("ok", result.get("value").asText());
    }

    @Test
    void rejectsMalformedJsonAsBadInput() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> Web.body(new ByteArrayInputStream("{broken".getBytes(StandardCharsets.UTF_8))));
        assertEquals("Malformed JSON", error.getMessage());
    }

    @Test
    void rejectsNonObjectJsonBodies() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> Web.body(new ByteArrayInputStream("[]".getBytes(StandardCharsets.UTF_8))));
        assertEquals("JSON object required", error.getMessage());
    }

    @Test
    void rejectsOversizedBodiesBeforeParsing() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> Web.body(new ByteArrayInputStream(new byte[1_048_577])));
        assertEquals("Request is too large", error.getMessage());
    }

    @Test
    void parsesEmployeeSortOptionsAndDefaultsMissingParametersAtTheEndpoint() {
        assertEquals(java.util.Map.of(), ApiController.employeeSortQuery(null));
        assertEquals(java.util.Map.of("sortBy", "joiningDate", "direction", "desc", "search", "Aarav", "status", "ACTIVE",
                        "searchAlgorithm", "binary"),
                ApiController.employeeSortQuery("sortBy=joiningDate&direction=desc&search=Aarav&status=ACTIVE&searchAlgorithm=binary"));
        assertThrows(IllegalArgumentException.class, () -> ApiController.employeeSortQuery("unknown=value"));
        assertThrows(IllegalArgumentException.class, () -> ApiController.employeeSortQuery("sortBy=id&sortBy=name"));
        assertThrows(IllegalArgumentException.class, () -> ApiController.employeeSortQuery("searchAlgorithm=binary&searchAlgorithm=bst"));
    }

    @Test
    void parsesGraphSelectionAndBfsStartParameters() {
        var selected = ApiController.queryParameters("ids=EMP001%2CEMP002", java.util.Set.of("ids", "start"));
        assertEquals("EMP001,EMP002", selected.get("ids"));
        var start = ApiController.queryParameters("start=EMP001", java.util.Set.of("ids", "start"));
        assertEquals("EMP001", start.get("start"));
    }

    @Test
    void rejectsUnexpectedDuplicateAndMalformedGraphParameters() {
        var allowed = java.util.Set.of("ids", "start");
        assertThrows(IllegalArgumentException.class, () -> ApiController.queryParameters("ids=EMP001&unknown=x", allowed));
        assertThrows(IllegalArgumentException.class, () -> ApiController.queryParameters("ids=EMP001&ids=EMP002", allowed));
        assertThrows(IllegalArgumentException.class, () -> ApiController.queryParameters("ids=%Q0", allowed));
    }

}
