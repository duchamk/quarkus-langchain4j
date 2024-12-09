package io.quarkiverse.langchain4j.watsonx;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.client.api.LoggingScope;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequest;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponse;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import io.quarkiverse.langchain4j.watsonx.client.WatsonxRestApi;
import io.quarkiverse.langchain4j.watsonx.client.filter.BearerTokenHeaderFactory;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;

public abstract class Watsonx {

    private static final Logger logger = Logger.getLogger(Watsonx.class);

    protected final String modelId, projectId, spaceId, version;
    protected final WatsonxRestApi client;
    protected final List<ChatModelListener> listeners;

    public Watsonx(Builder<?> builder) {
        QuarkusRestClientBuilder restClientBuilder = QuarkusRestClientBuilder.newBuilder()
                .baseUrl(builder.url)
                .clientHeadersFactory(new BearerTokenHeaderFactory(builder.tokenGenerator))
                .connectTimeout(builder.timeout.toSeconds(), TimeUnit.SECONDS)
                .readTimeout(builder.timeout.toSeconds(), TimeUnit.SECONDS);

        if (builder.logRequests || builder.logResponses) {
            restClientBuilder.loggingScope(LoggingScope.REQUEST_RESPONSE);
            restClientBuilder.clientLogger(new WatsonxRestApi.WatsonClientLogger(
                    builder.logRequests,
                    builder.logResponses));
        }

        this.client = restClientBuilder.build(WatsonxRestApi.class);
        this.modelId = builder.modelId;
        this.spaceId = builder.spaceId;
        this.projectId = builder.projectId;
        this.version = builder.version;
        this.listeners = builder.listeners;
    }

    protected void beforeSentRequest(ChatModelRequest request, Map<Object, Object> attributes) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onRequest(new ChatModelRequestContext(request, attributes));
            } catch (Exception e) {
                logger.warn("Exception while calling model listener", e);
            }
        }
    }

    protected void afterReceivedResponse(ChatModelResponse response, ChatModelRequest request, Map<Object, Object> attributes) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onResponse(new ChatModelResponseContext(response, request, attributes));
            } catch (Exception e) {
                logger.warn("Exception while calling model listener", e);
            }
        }
    }

    protected void onRequestError(Throwable error, ChatModelRequest request, ChatModelResponse partialResponse,
            Map<Object, Object> attributes) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onError(new ChatModelErrorContext(error, request, partialResponse, attributes));
            } catch (Exception e) {
                logger.warn("Exception while calling model listener", e);
            }
        }
    }

    public WatsonxRestApi getClient() {
        return client;
    }

    public String getModelId() {
        return modelId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getVersion() {
        return version;
    }

    @SuppressWarnings("unchecked")
    public static abstract class Builder<T extends Builder<T>> {

        protected String modelId;
        protected String version;
        protected String spaceId;
        protected String projectId;
        protected Duration timeout;
        protected URL url;
        protected boolean logResponses;
        protected boolean logRequests;
        private List<ChatModelListener> listeners = Collections.emptyList();
        protected WatsonxTokenGenerator tokenGenerator;

        public T modelId(String modelId) {
            this.modelId = modelId;
            return (T) this;
        }

        public T version(String version) {
            this.version = version;
            return (T) this;
        }

        public T spaceId(String spaceId) {
            this.spaceId = spaceId;
            return (T) this;
        }

        public T projectId(String projectId) {
            this.projectId = projectId;
            return (T) this;
        }

        public T url(URL url) {
            this.url = url;
            return (T) this;
        }

        public T timeout(Duration timeout) {
            this.timeout = timeout;
            return (T) this;
        }

        public T listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return (T) this;
        }

        public T tokenGenerator(WatsonxTokenGenerator tokenGenerator) {
            this.tokenGenerator = tokenGenerator;
            return (T) this;
        }

        public T logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return (T) this;
        }

        public T logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return (T) this;
        }
    }
}
