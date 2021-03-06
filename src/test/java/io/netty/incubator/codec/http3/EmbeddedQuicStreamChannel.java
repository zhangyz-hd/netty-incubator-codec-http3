/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.incubator.codec.http3;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelId;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.incubator.codec.quic.QuicChannel;
import io.netty.incubator.codec.quic.QuicStreamAddress;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import io.netty.incubator.codec.quic.QuicStreamChannelConfig;
import io.netty.incubator.codec.quic.QuicStreamType;

import java.util.Map;

final class EmbeddedQuicStreamChannel extends EmbeddedChannel implements QuicStreamChannel {
    private final boolean localCreated;
    private final QuicStreamType type;
    private final long id;
    private QuicStreamChannelConfig config;
    private boolean inputShutdown;
    private boolean outputShutdown;

    EmbeddedQuicStreamChannel(ChannelHandler... handlers) {
        this(null, false, QuicStreamType.BIDIRECTIONAL, 0, handlers);
    }

    EmbeddedQuicStreamChannel(boolean localCreated, QuicStreamType type, long id, ChannelHandler... handlers) {
        this(null, localCreated, type, id, handlers);
    }

    EmbeddedQuicStreamChannel(QuicChannel parent, boolean localCreated, QuicStreamType type,
                              long id, ChannelHandler... handlers) {
        super(parent, DefaultChannelId.newInstance(), true, false, handlers);
        this.localCreated = localCreated;
        this.type = type;
        this.id = id;
    }

    @Override
    public QuicStreamAddress localAddress() {
        return null;
    }

    @Override
    public QuicStreamAddress remoteAddress() {
        return null;
    }

    @Override
    public QuicChannel parent() {
        return (QuicChannel) super.parent();
    }

    @Override
    public QuicStreamChannelConfig config() {
        if (config == null) {
            config = new EmbeddedQuicStreamChannelConfig(super.config());
        }
        return config;
    }

    @Override
    public boolean isLocalCreated() {
        return localCreated;
    }

    @Override
    public QuicStreamType type() {
        return type;
    }

    @Override
    public long streamId() {
        return id;
    }

    @Override
    public boolean isInputShutdown() {
        return inputShutdown;
    }

    @Override
    public ChannelFuture shutdownInput() {
        inputShutdown = true;
        return newSucceededFuture();
    }

    @Override
    public ChannelFuture shutdownInput(ChannelPromise promise) {
        inputShutdown = true;
        return promise.setSuccess();
    }

    @Override
    public boolean isOutputShutdown() {
        return outputShutdown;
    }

    @Override
    public ChannelFuture shutdownOutput() {
        outputShutdown = true;
        return newSucceededFuture();
    }

    @Override
    public ChannelFuture shutdownOutput(ChannelPromise promise) {
        outputShutdown = true;
        return promise.setSuccess();
    }

    @Override
    public boolean isShutdown() {
        return outputShutdown;
    }

    @Override
    public ChannelFuture shutdown() {
        inputShutdown = true;
        outputShutdown = true;
        return newSucceededFuture();
    }

    @Override
    public ChannelFuture shutdown(ChannelPromise promise) {
        inputShutdown = true;
        outputShutdown = true;
        return promise.setSuccess();
    }

    private static final class EmbeddedQuicStreamChannelConfig implements QuicStreamChannelConfig {
        private final ChannelConfig config;
        private boolean allowHalfClosure;

        EmbeddedQuicStreamChannelConfig(ChannelConfig config) {
            this.config = config;
        }

        @Override
        public QuicStreamChannelConfig setReadFrames(boolean readFrames) {
            return this;
        }

        @Override
        public boolean isReadFrames() {
            return false;
        }

        @Override
        public QuicStreamChannelConfig setAllowHalfClosure(boolean allowHalfClosure) {
            this.allowHalfClosure = allowHalfClosure;
            return this;
        }

        @Override
        public QuicStreamChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
            config.setMaxMessagesPerRead(maxMessagesPerRead);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setWriteSpinCount(int writeSpinCount) {
            config.setWriteSpinCount(writeSpinCount);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setAllocator(ByteBufAllocator allocator) {
            config.setAllocator(allocator);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
            config.setRecvByteBufAllocator(allocator);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setAutoRead(boolean autoRead) {
            config.setAutoRead(autoRead);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setAutoClose(boolean autoClose) {
            config.setAutoClose(autoClose);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
            config.setMessageSizeEstimator(estimator);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
            config.setWriteBufferWaterMark(writeBufferWaterMark);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
            config.setConnectTimeoutMillis(connectTimeoutMillis);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
            config.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
            return this;
        }

        @Override
        public QuicStreamChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
            config.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
            return this;
        }

        @Override
        public boolean isAllowHalfClosure() {
            return allowHalfClosure;
        }

        @Override
        public Map<ChannelOption<?>, Object> getOptions() {
            return config.getOptions();
        }

        @Override
        public boolean setOptions(Map<ChannelOption<?>, ?> options) {
            return config.setOptions(options);
        }

        @Override
        public <T> T getOption(ChannelOption<T> option) {
            return config.getOption(option);
        }

        @Override
        public <T> boolean setOption(ChannelOption<T> option, T value) {
            return config.setOption(option, value);
        }

        @Override
        public int getConnectTimeoutMillis() {
            return config.getConnectTimeoutMillis();
        }

        @Override
        public int getMaxMessagesPerRead() {
            return config.getMaxMessagesPerRead();
        }

        @Override
        public int getWriteSpinCount() {
            return config.getWriteSpinCount();
        }

        @Override
        public ByteBufAllocator getAllocator() {
            return config.getAllocator();
        }

        @Override
        public <T extends RecvByteBufAllocator> T getRecvByteBufAllocator() {
            return config.getRecvByteBufAllocator();
        }

        @Override
        public boolean isAutoRead() {
            return config.isAutoRead();
        }

        @Override
        public boolean isAutoClose() {
            return config.isAutoClose();
        }

        @Override
        public int getWriteBufferHighWaterMark() {
            return config.getWriteBufferHighWaterMark();
        }

        @Override
        public int getWriteBufferLowWaterMark() {
            return config.getWriteBufferLowWaterMark();
        }

        @Override
        public MessageSizeEstimator getMessageSizeEstimator() {
            return config.getMessageSizeEstimator();
        }

        @Override
        public WriteBufferWaterMark getWriteBufferWaterMark() {
            return config.getWriteBufferWaterMark();
        }
    }
}
