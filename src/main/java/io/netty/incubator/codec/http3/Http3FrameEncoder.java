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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

public final class Http3FrameEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (msg instanceof Http3DataFrame) {
            writeDataFrame(ctx, (Http3DataFrame) msg, promise);
        } else if (msg instanceof Http3HeadersFrame) {
            writeHeadersFrame(ctx, (Http3HeadersFrame) msg, promise);
        } else if (msg instanceof Http3CancelPushFrame) {
            writeCancelPushFrame(ctx, (Http3CancelPushFrame) msg, promise);
        } else if (msg instanceof Http3SettingsFrame) {
            writeSettingsFrame(ctx, (Http3SettingsFrame) msg, promise);
        } else if (msg instanceof Http3PushPromiseFrame) {
            writePushPromiseFrame(ctx, (Http3PushPromiseFrame) msg, promise);
        } else if (msg instanceof Http3GoAwayFrame) {
            writeGoAwayFrame(ctx, (Http3GoAwayFrame) msg, promise);
        } else if (msg instanceof Http3MaxPushIdFrame) {
            writeMaxPushIdFrame(ctx, (Http3MaxPushIdFrame) msg, promise);
        } else {
            unsupported(msg, promise);
        }
    }

    private static void writeDataFrame(
            ChannelHandlerContext ctx, Http3DataFrame frame, ChannelPromise promise) {
        ByteBuf out = ctx.alloc().directBuffer();
        writeVariableLengthInteger(out, 0x0);
        writeVariableLengthInteger(out, frame.content().readableBytes());
        writeBufferToContext(ctx, Unpooled.wrappedUnmodifiableBuffer(out, frame.content().retain()), promise);
        frame.release();
    }

    private static void writeHeadersFrame(
            ChannelHandlerContext ctx, Http3HeadersFrame frame, ChannelPromise promise) {
        ByteBuf out = ctx.alloc().directBuffer();
        writeVariableLengthInteger(out, 0x1);
        writeHeaders(out, frame.headers());
        writeBufferToContext(ctx, out, promise);
    }

    private static void writeCancelPushFrame(
            ChannelHandlerContext ctx, Http3CancelPushFrame frame, ChannelPromise promise) {
        ByteBuf out = ctx.alloc().directBuffer();
        writeVariableLengthInteger(out, 0x3);
        writeVariableLengthInteger(out, frame.id());
        writeBufferToContext(ctx, out, promise);
    }

    private static void writeSettingsFrame(
            ChannelHandlerContext ctx, Http3SettingsFrame frame, ChannelPromise promise) {
        ByteBuf out = ctx.alloc().directBuffer();
        writeVariableLengthInteger(out, 0x4);
        frame.forEach(e -> {
            writeVariableLengthInteger(out, e.getKey());
            writeVariableLengthInteger(out, e.getValue());
        });
        writeBufferToContext(ctx, out, promise);
    }

    private static void writePushPromiseFrame(
            ChannelHandlerContext ctx, Http3PushPromiseFrame frame, ChannelPromise promise) {
        ByteBuf out = ctx.alloc().directBuffer();
        writeVariableLengthInteger(out, 0x5);
        writeHeaders(out, frame.headers());
        writeBufferToContext(ctx, out, promise);
    }

    private static void writeGoAwayFrame(
            ChannelHandlerContext ctx, Http3GoAwayFrame frame, ChannelPromise promise) {
        ByteBuf out = ctx.alloc().directBuffer();
        writeVariableLengthInteger(out, 0x7);
        writeVariableLengthInteger(out, frame.id());
        writeBufferToContext(ctx, out, promise);
    }

    private static void writeMaxPushIdFrame(
            ChannelHandlerContext ctx, Http3MaxPushIdFrame frame, ChannelPromise promise) {
        ByteBuf out = ctx.alloc().directBuffer();
        writeVariableLengthInteger(out, 0xd);
        writeVariableLengthInteger(out, frame.id());
        writeBufferToContext(ctx, out, promise);
    }

    private static void unsupported(Object msg, ChannelPromise promise) {
        ReferenceCountUtil.release(msg);
        promise.setFailure(new UnsupportedOperationException());
    }

    private static void writeVariableLengthInteger(ByteBuf out, long value) {
        int numBytes = numBytesForVariableLengthInteger(value);
        int writerIndex = out.writerIndex();
        switch (numBytes) {
            case 1:
                out.writeByte((byte) value);
                break;
            case 2:
                out.writeShort((short) value);
                out.setByte(writerIndex, out.getByte(writerIndex) | 0x40);
                break;
            case 4:
                out.writeInt((int) value);
                out.setByte(writerIndex, out.getByte(writerIndex) | 0x80);
                break;
            case 8:
                out.writeLong(value);
                out.setByte(writerIndex, out.getByte(writerIndex) | 0xc0);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void writeHeaders(ByteBuf out, Http3Headers headers) {
        // TODO: Implement me
    }

    private static void writeBufferToContext(ChannelHandlerContext ctx, ByteBuf buffer, ChannelPromise promise) {
        // TODO: Maybe this should be wrapped in a QuicStreamFrame.
        ctx.write(buffer, promise);
    }

    /**
     * See <a href="https://tools.ietf.org/html/draft-ietf-quic-transport-32#section-16">
     *     Variable-Length Integer Encoding</a>.
     */
    private static int numBytesForVariableLengthInteger(long value) {
        if (value <= 64) {
            return 1;
        }
        if (value <= 16383) {
            return 2;
        }
        if (value <= 1073741823) {
            return 4;
        }
        if (value <= 4611686018427387903L) {
            return 8;
        }
        throw new IllegalArgumentException();
    }
}