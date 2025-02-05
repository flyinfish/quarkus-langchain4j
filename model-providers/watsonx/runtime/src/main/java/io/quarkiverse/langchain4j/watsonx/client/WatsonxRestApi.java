package io.quarkiverse.langchain4j.watsonx.client;

import static java.util.stream.Collectors.joining;
import static java.util.stream.StreamSupport.stream;

import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.client.api.ClientLogger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkiverse.langchain4j.QuarkusJsonCodecFactory;
import io.quarkiverse.langchain4j.QuarkusJsonCodecFactory.ObjectMapperHolder;
import io.quarkiverse.langchain4j.watsonx.bean.EmbeddingRequest;
import io.quarkiverse.langchain4j.watsonx.bean.EmbeddingResponse;
import io.quarkiverse.langchain4j.watsonx.bean.ScoringRequest;
import io.quarkiverse.langchain4j.watsonx.bean.ScoringResponse;
import io.quarkiverse.langchain4j.watsonx.bean.TextChatRequest;
import io.quarkiverse.langchain4j.watsonx.bean.TextChatResponse;
import io.quarkiverse.langchain4j.watsonx.bean.TextExtractionRequest;
import io.quarkiverse.langchain4j.watsonx.bean.TextExtractionResponse;
import io.quarkiverse.langchain4j.watsonx.bean.TextGenerationRequest;
import io.quarkiverse.langchain4j.watsonx.bean.TextGenerationResponse;
import io.quarkiverse.langchain4j.watsonx.bean.TextStreamingChatResponse;
import io.quarkiverse.langchain4j.watsonx.bean.TokenizationRequest;
import io.quarkiverse.langchain4j.watsonx.bean.TokenizationResponse;
import io.quarkiverse.langchain4j.watsonx.bean.WatsonxError;
import io.quarkiverse.langchain4j.watsonx.exception.WatsonxException;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.quarkus.rest.client.reactive.jackson.ClientObjectMapper;
import io.smallrye.mutiny.Multi;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;

/**
 * This Microprofile REST client is used as the building block of all the API calls to watsonx. The implementation is provided
 * by
 * the Reactive REST Client in Quarkus.
 */
@Path("/ml/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface WatsonxRestApi {

    @POST
    @Path("text/generation")
    TextGenerationResponse generation(TextGenerationRequest request,
            @QueryParam("version") String version);

    @POST
    @Path("text/generation_stream")
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    Multi<TextGenerationResponse> generationStreaming(TextGenerationRequest request,
            @QueryParam("version") String version);

    @POST
    @Path("text/chat")
    TextChatResponse chat(TextChatRequest request,
            @QueryParam("version") String version);

    @POST
    @Path("text/chat_stream")
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    Multi<TextStreamingChatResponse> streamingChat(TextChatRequest request,
            @QueryParam("version") String version);

    @POST
    @Path("text/rerank")
    ScoringResponse rerank(ScoringRequest request,
            @QueryParam("version") String version);

    @POST
    @Path("text/tokenization")
    TokenizationResponse tokenization(TokenizationRequest request,
            @QueryParam("version") String version);

    @POST
    @Path("/text/embeddings")
    EmbeddingResponse embeddings(EmbeddingRequest request,
            @QueryParam("version") String version);

    @POST
    @Path("/text/extractions")
    TextExtractionResponse startTextExtractionJob(TextExtractionRequest request,
            @QueryParam("version") String version);

    @GET
    @Path("text/extractions/{id}")
    TextExtractionResponse getTextExtractionDetails(@PathParam("id") String id,
            @QueryParam("space_id") String spaceId,
            @QueryParam("project_id") String projectId,
            @QueryParam("version") String version);

    @ClientExceptionMapper
    static WatsonxException toException(jakarta.ws.rs.core.Response response) {
        MediaType mediaType = response.getMediaType();
        if ((mediaType != null) && mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
            try {

                WatsonxError ex = response.readEntity(WatsonxError.class);
                StringJoiner joiner = new StringJoiner("\n");

                if (ex.errors() != null && ex.errors().size() > 0) {
                    for (WatsonxError.Error error : ex.errors())
                        joiner.add("%s: %s".formatted(error.code(), error.message()));
                }

                return new WatsonxException(joiner.toString(), response.getStatus(), ex);
            } catch (Exception e) {
                return new WatsonxException(response.readEntity(String.class), response.getStatus());
            }
        }

        return new WatsonxException(response.readEntity(String.class), response.getStatus());
    }

    @ClientObjectMapper
    static ObjectMapper objectMapper(ObjectMapper defaultObjectMapper) {
        return QuarkusJsonCodecFactory.SnakeCaseObjectMapperHolder.MAPPER;
    }

    /**
     * Introduce a custom logger as the stock one logs at the DEBUG level by default...
     */
    class WatsonClientLogger implements ClientLogger {

        private static final Logger log = Logger.getLogger(WatsonClientLogger.class);
        private static final Pattern BEARER_PATTERN = Pattern.compile("(Bearer\\s*)(\\w{4})(\\w+)(\\w{4})");
        private static final Pattern BASE64_IMAGE_PATTERN = Pattern.compile("(data:.+;base64,)(.{15})(.+)(.{15})([\\s\\S]*)");

        private final boolean logRequests;
        private final boolean logResponses;

        public WatsonClientLogger(boolean logRequests, boolean logResponses) {
            this.logRequests = logRequests;
            this.logResponses = logResponses;
        }

        @Override
        public void setBodySize(int bodySize) {
            // ignore
        }

        @Override
        public void logRequest(HttpClientRequest request, Buffer body, boolean omitBody) {
            if (!logRequests || !log.isInfoEnabled()) {
                return;
            }
            try {
                log.infof(
                        "Request:\n- method: %s\n- url: %s\n- headers: %s\n- body: %s",
                        request.getMethod(),
                        request.absoluteURI(),
                        inOneLine(request.headers()),
                        bodyToString(body));
            } catch (Exception e) {
                log.warn("Failed to log request", e);
            }
        }

        @Override
        public void logResponse(HttpClientResponse response, boolean redirect) {
            if (!logResponses || !log.isInfoEnabled()) {
                return;
            }
            response.bodyHandler(new Handler<>() {
                @Override
                public void handle(Buffer body) {
                    String prettyBody;
                    try {
                        prettyBody = ObjectMapperHolder.WRITER
                                .writeValueAsString(ObjectMapperHolder.MAPPER.readTree(bodyToString(body)));
                    } catch (Exception e) {
                        prettyBody = bodyToString(body);
                    }
                    try {

                        log.infof(
                                "Response:\n- status code: %s\n- headers: %s\n- body: %s",
                                response.statusCode(),
                                inOneLine(response.headers()),
                                prettyBody);
                    } catch (Exception e) {
                        log.warn("Failed to log response", e);
                    }
                }
            });
        }

        private String bodyToString(Buffer body) {
            if (body == null) {
                return "";
            }
            return formatBase64ImageForLogging(body.toString());
        }

        private String inOneLine(MultiMap headers) {

            return stream(headers.spliterator(), false)
                    .map(header -> {
                        String headerKey = header.getKey();
                        String headerValue = header.getValue();
                        if ("Authorization".equals(headerKey)) {
                            headerValue = maskAuthorizationHeaderValue(headerValue);
                        } else if ("api-key".equals(headerKey)) {
                            headerValue = maskApiKeyHeaderValue(headerValue);
                        }
                        return String.format("[%s: %s]", headerKey, headerValue);
                    })
                    .collect(joining(", "));
        }

        private static String maskAuthorizationHeaderValue(String authorizationHeaderValue) {
            try {

                Matcher matcher = BEARER_PATTERN.matcher(authorizationHeaderValue);

                StringBuilder sb = new StringBuilder();
                while (matcher.find()) {
                    matcher.appendReplacement(sb, matcher.group(1) + matcher.group(2) + "..." + matcher.group(4));
                }

                return sb.toString();
            } catch (Exception e) {
                return "Failed to mask the API key.";
            }
        }

        private static String formatBase64ImageForLogging(String body) {
            try {

                if (body == null || body.isBlank())
                    return body;

                Matcher matcher = BASE64_IMAGE_PATTERN.matcher(body);

                StringBuilder sb = new StringBuilder();
                while (matcher.find()) {
                    matcher.appendReplacement(sb,
                            matcher.group(1) + matcher.group(2) + "..." + matcher.group(4) + matcher.group(5));
                }

                return sb.isEmpty() ? body : sb.toString();
            } catch (Exception e) {
                return "Failed to format the base64 image value.";
            }
        }

        private static String maskApiKeyHeaderValue(String apiKeyHeaderValue) {
            try {
                if (apiKeyHeaderValue.length() <= 4) {
                    return apiKeyHeaderValue;
                }
                return apiKeyHeaderValue.substring(0, 2)
                        + "..."
                        + apiKeyHeaderValue.substring(apiKeyHeaderValue.length() - 2);
            } catch (Exception e) {
                return "Failed to mask the API key.";
            }
        }
    }
}
