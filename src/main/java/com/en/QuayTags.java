package com.en;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.Value;
import org.openrewrite.ipc.http.HttpSender;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class QuayTags {
    private static final ObjectMapper mapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    List<Tag> tags;

    @Data
    public static class Tag {
        String name;
    }

    public static List<String> getAvailableTags(String imageName, HttpSender httpSender, String token) {
        HttpSender.Request request = HttpSender.Request.build(
                        "https://registry.cigna.com/api/v1/repository/" + imageName + "/tag", httpSender)
                .withAuthentication("Bearer", token)
                .build();
        try (HttpSender.Response response = httpSender.send(request)) {
            if (response.isSuccessful()) {
                try {
                    return mapper.readValue(response.getBody(), QuayTags.class).tags.stream()
                            .map(Tag::getName)
                            .collect(Collectors.toList());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        throw new IllegalStateException("Unable to retrieve Quay tags for image " + imageName);
    }
}
