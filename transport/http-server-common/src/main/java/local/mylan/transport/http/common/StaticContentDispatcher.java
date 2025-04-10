/*
 * Copyright 2025 Ruslan Kashapov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package local.mylan.transport.http.common;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.ETAG;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_NONE_MATCH;
import static io.netty.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static local.mylan.transport.http.common.ResponseUtils.allowResponse;
import static local.mylan.transport.http.common.ResponseUtils.notFoundResponse;
import static local.mylan.transport.http.common.ResponseUtils.simpleResponse;
import static local.mylan.transport.http.common.ResponseUtils.unsupportedMethodResponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.stream.ChunkedStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import local.mylan.transport.http.api.ContextDispatcher;
import local.mylan.transport.http.api.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticContentDispatcher implements ContextDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(StaticContentDispatcher.class);
    private static final String ALLOWED_METHODS = "HEAD, GET, OPTIONS";

    private static final int CACHE_MAX_SIZE = 100;
    private static final int CACHE_ITEM_MAX_LENGTH = 2048;
    private static final Duration CACHE_EXPIRES = Duration.ofMinutes(10);
    private static final ContentSource NO_CONTENT = new ContentSource(0, 0, "", "", null, null);
    private static final int CHUNK_SIZE = 8192;

    protected final String contextPath;
    protected final String resourceBase;
    protected final SourceType type;
    private final Cache<String, ContentSource> cache;

    public StaticContentDispatcher(final String contextPath, final String resourceBase) {
        this(contextPath, resourceBase, SourceType.CLASSPATH);
    }

    public StaticContentDispatcher(final String contextPath, final String resourceBase, final SourceType type) {
        this.contextPath = contextPath;
        this.resourceBase = resourceBase;
        this.type = type;
        // TODO configurable cache and item length threshold
        cache = CacheBuilder.newBuilder()
            .maximumSize(CACHE_MAX_SIZE)
            .expireAfterAccess(CACHE_EXPIRES)
            .build();
        LOG.info("Initialized for context {} -> content root: {} ({})", contextPath, resourceBase, type);
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public boolean dispatch(final RequestContext ctx) {
        switch (ctx.method().name()) {
            case "OPTIONS" -> ctx.sendResponse(allowResponse(ctx.protocolVersion(), ALLOWED_METHODS));
            case "HEAD" -> handleContent(ctx, true);
            case "GET" -> handleContent(ctx, false);
            default -> ctx.sendResponse(unsupportedMethodResponse(ctx.protocolVersion()));
        }
        return true;
    }

    private void handleContent(final RequestContext ctx, final boolean headOnly) {
        final var path = ctx.contextPath();
        final var source = getContentSource(path);
        if (source.length() == 0) {
            ctx.sendResponse(notFoundResponse(ctx.protocolVersion()));
            return;
        }
        final var etag = source.etag();
        if (etag.equals(ctx.headers().get(IF_NONE_MATCH))) {
            ctx.sendResponse(simpleResponse(ctx.protocolVersion(), NOT_MODIFIED));
            return;
        }
        final var response = simpleResponse(ctx.protocolVersion(), OK);
        response.headers()
            .set(ETAG, etag)
            .set(CONTENT_TYPE, source.mediaType())
            .set(CONTENT_LENGTH, source.length());
        if (headOnly) {
            ctx.sendResponse(response);
        } else if (source.content() != null) {
            ctx.sendResponse(response.replace(Unpooled.wrappedBuffer(source.content)));
        } else if (source.streamProvider() != null) {
            final var channelCtx = ctx.channelHandlerContext();
            if (source.length() <= CHUNK_SIZE) {
                // send as single piece
                final var length = (int) source.length();
                try (var in = source.streamProvider().getInputStream()) {
                    final var buf = channelCtx.alloc().buffer((int) source.length());
                    buf.writeBytes(in, length);
                    ctx.sendResponse(response.replace(buf));
                } catch (IOException e) {
                    throw new IllegalStateException("Error reading resouece {}", e);
                }
            } else {
                // send as separate chunks
                try {
                    final var responseHeaders = new DefaultHttpResponse(ctx.protocolVersion(), OK, response.headers());
                    responseHeaders.headers().set(TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
                    final var chunkedInput = new HttpChunkedInput(
                        new ChunkedStream(source.streamProvider.getInputStream(), CHUNK_SIZE));
                    channelCtx.write(responseHeaders);
                    sendNextChunk(channelCtx, chunkedInput, path, 0);
                } catch (Exception e) {
                    throw new IllegalStateException("Error building stream for resource" + path, e);
                }
            }
        } else {
            throw new IllegalStateException("No content found for resource " + path);
        }
    }

    private static void sendNextChunk(final ChannelHandlerContext channelCtx, final HttpChunkedInput chunkedInput,
        final String resourcePath, int chunkCount) {
        try {
            final var nextChunk = chunkedInput.readChunk(channelCtx.alloc());
            if (nextChunk == null) {
                LOG.debug("Chunked file transfer completed for resource {}.", resourcePath);
                chunkedInput.close();
            }   else {
                LOG.trace("Sending chunk {} for resource {}", chunkCount, resourcePath);
                channelCtx.writeAndFlush(nextChunk).addListener(future -> {
                        if (future.isSuccess()) {
                            sendNextChunk(channelCtx, chunkedInput, resourcePath, chunkCount +1);
                        } else {
                            LOG.error("Sending chunk failed for resource {}", resourcePath, future.cause());
                            chunkedInput.close();
                        }
                    });
            }
        } catch (Exception e) {
            LOG.error("Exception processing chunk for resource {}", resourcePath, e);

        }
    }

    protected ContentSource getContentSource(final String path) {
        try {
            return cache.get(path, () -> loadContentSource(path));
        } catch (ExecutionException e) {
            LOG.warn("Exception on loading content source {} ({})", path, type, e);
            return NO_CONTENT;
        }
    }

    private ContentSource loadContentSource(final String path) throws IOException {
        final var fullPath = resourceBase + path;
        if (type == SourceType.FILE_SYSTEM) {
            final var file = new File(fullPath);
            if (file.exists() && file.isFile()) {
                return buildContentSource(fullPath, file.length(), file.lastModified(),
                    () -> new FileInputStream(file));
            }
        } else if (type == SourceType.CLASSPATH) {
            final var url = getClass().getResource(fullPath);
            if (url != null) {
                final var conn = url.openConnection();
                return buildContentSource(fullPath, conn.getContentLength(), conn.getLastModified(),
                    url::openStream);
            }
        }
        LOG.debug("requested resource {} not found", fullPath);
        return NO_CONTENT;
    }

    private ContentSource buildContentSource(final String path, final long length, final long modified,
        final StreamProvider streamProvider) {
        final var etag = "%s-%s".formatted(Long.toHexString(modified), Long.toHexString(length));
        final var guessMediaType = URLConnection.guessContentTypeFromName(path);
        final var mediaType = guessMediaType == null ? APPLICATION_OCTET_STREAM : guessMediaType;
        if (length > CACHE_ITEM_MAX_LENGTH) {
            return new ContentSource(length, modified, mediaType, etag, null, streamProvider);
        }
        try (var in = streamProvider.getInputStream()) {
            return new ContentSource(length, modified, mediaType, etag, in.readAllBytes(), null);
        } catch (IOException e) {
            LOG.warn("Error reading resource {} ({})", path, type, e);
            return NO_CONTENT;
        }
    }

    public enum SourceType {
        CLASSPATH, FILE_SYSTEM
    }

    public record ContentSource(long length, long lastModified, CharSequence mediaType, String etag,
        byte[] content, StreamProvider streamProvider) {
    }

    @FunctionalInterface
    protected interface StreamProvider {
        InputStream getInputStream() throws IOException;
    }
}
