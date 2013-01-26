/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package nats.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class ByteBufUtil {

	static final byte[] CRLF = "\r\n".getBytes();
//
//	static ByteBuf directBuffer(byte[] array) {
//		final ByteBuf buffer = Unpooled.directBuffer(array.length);
//		buffer.writeBytes(array);
//		return buffer;
//	}
//
//	static ByteBuf directBuffer(String string) {
//		return directBuffer(string.getBytes());
//	}
//
//	static ByteBuf wrappedBuffer(String string) {
//		return Unpooled.wrappedBuffer(string.getBytes());
//	}

	static void writeIntegerAsString(ByteBuf buffer, int i) {
		buffer.writeBytes(Integer.toString(i).getBytes());
	}

}
