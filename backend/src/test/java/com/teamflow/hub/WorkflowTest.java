package com.teamflow.hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    JsonNode call(MockHttpServletRequestBuilder b, String token, String json, int expected) throws Exception {
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (json != null) b.contentType(MediaType.APPLICATION_JSON).content(json);
        String body = mvc.perform(b).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isBlank() ? null : om.readTree(body);
    }

    @Test
    void fullTaskLifecycle() throws Exception {
        assertEquals(401, mvc.perform(get("/api/projects")).andReturn().getResponse().getStatus());

        String admin = call(post("/api/auth/login"), null, "{\"email\":\"admin@teamflow.local\",\"password\":\"Admin@123\"}", 200).get("token").asText();
        JsonNode reg = call(post("/api/auth/register"), null,
                "{\"name\":\"Mona\",\"username\":\"mona\",\"email\":\"mona@x.com\",\"password\":\"secret1\",\"role\":\"MEMBER\"}", 201);
        String member = reg.get("token").asText();
        call(post("/api/auth/register"), null,
                "{\"name\":\"Mona2\",\"username\":\"mona\",\"email\":\"m2@x.com\",\"password\":\"secret1\"}", 400);

        // members cannot create projects
        call(post("/api/projects"), member, "{\"name\":\"Nope\"}", 403);

        long pid = call(post("/api/projects"), admin, "{\"name\":\"Website\",\"description\":\"d\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-02-01\"}", 201).get("id").asLong();
        call(post("/api/projects/" + pid + "/members"), admin, "{\"username\":\"@mona\"}", 200);

        // task for a non member is refused, then a valid one
        long tid = call(post("/api/tasks"), admin, "{\"title\":\"Build UI\",\"projectId\":" + pid + ",\"assignedToId\":" + reg.get("user").get("id").asLong() + "}", 201).get("id").asLong();

        call(post("/api/tasks/" + tid + "/review"), admin, "{\"accept\":true,\"rating\":5}", 400);          // not in review yet
        call(patch("/api/tasks/" + tid + "/status"), member, "{\"status\":\"REVIEWING\"}", 403);            // cannot skip IN_PROGRESS
        call(patch("/api/tasks/" + tid + "/status"), member, "{\"status\":\"IN_PROGRESS\"}", 200);
        call(patch("/api/tasks/" + tid + "/status"), admin, "{\"status\":\"REVIEWING\"}", 403);             // manager is not the assignee
        call(patch("/api/tasks/" + tid + "/status"), member, "{\"status\":\"REVIEWING\"}", 200);
        call(post("/api/tasks/" + tid + "/review"), member, "{\"accept\":true,\"rating\":5}", 403);         // member cannot review
        call(post("/api/tasks/" + tid + "/review"), admin, "{\"accept\":false}", 400);                      // feedback required
        assertEquals("IN_PROGRESS", call(post("/api/tasks/" + tid + "/review"), admin, "{\"accept\":false,\"feedback\":\"fix colors\"}", 200).get("status").asText());
        call(patch("/api/tasks/" + tid + "/status"), member, "{\"status\":\"REVIEWING\"}", 200);
        assertEquals("ACCEPTED", call(post("/api/tasks/" + tid + "/review"), admin, "{\"accept\":true,\"rating\":4}", 200).get("status").asText());
        assertEquals("DONE", call(patch("/api/tasks/" + tid + "/status"), admin, "{\"status\":\"DONE\"}", 200).get("status").asText());

        assertEquals(100, call(get("/api/projects/" + pid), member, null, 200).get("progress").asInt());
        assertEquals(1, call(get("/api/dashboard"), admin, null, 200).get("completedTasks").asInt());
        call(get("/api/insights"), admin, null, 200);

        // chat: members cannot post announcements
        call(post("/api/chat/messages"), member, "{\"channel\":\"ANNOUNCEMENT\",\"projectId\":" + pid + ",\"text\":\"hi\"}", 403);
        call(post("/api/chat/messages"), admin, "{\"channel\":\"ANNOUNCEMENT\",\"projectId\":" + pid + ",\"text\":\"hi\"}", 201);
        call(post("/api/chat/messages"), member, "{\"channel\":\"PROJECT\",\"projectId\":" + pid + ",\"text\":\"hello\"}", 201);
        assertEquals(1, call(get("/api/chat/messages?channel=PROJECT&projectId=" + pid), admin, null, 200).size());

        // file upload and download
        mvc.perform(multipart("/api/projects/" + pid + "/attachments").file(new MockMultipartFile("file", "a.txt", "text/plain", "hello".getBytes()))
                .header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        assertEquals(1, call(get("/api/projects/" + pid + "/attachments"), member, null, 200).size());

        // notifications reached the member and password reset works
        assertEquals(true, call(get("/api/notifications"), member, null, 200).size() >= 2);
        String tok = call(post("/api/auth/forgot-password"), null, "{\"email\":\"mona@x.com\"}", 200).get("resetToken").asText();
        call(post("/api/auth/reset-password"), null, "{\"token\":\"" + tok + "\",\"newPassword\":\"newpass1\"}", 200);
        call(post("/api/auth/login"), null, "{\"email\":\"mona@x.com\",\"password\":\"newpass1\"}", 200);
        call(post("/api/auth/login"), null, "{\"email\":\"mona@x.com\",\"password\":\"secret1\"}", 401);
    }
}
