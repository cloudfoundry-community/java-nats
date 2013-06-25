/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
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

import io.netty.buffer.Unpooled;
import static org.testng.Assert.*;

import io.netty.channel.embedded.EmbeddedChannel;
import org.testng.annotations.Test;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientCodecTest {

	@Test
	public void serverError() throws Exception {
		final String errorMessage = "'You are doing it wrong'";
		final ServerErrorFrame errorFrame = (ServerErrorFrame) clientDecode("-ERR " + errorMessage + "\r\n");

		assertNotNull(errorFrame);
		assertEquals(errorFrame.getErrorMessage(), errorMessage);
	}

	@Test
	public void serverInfo() throws Exception {
		final String jsonPayload = "{\"server_id\":\"8d8222a15560538b92751995be\",\"host\":\"0.0.0.0\",\"port\":4222,\"version\":\"0.4.22\",\"auth_required\":false,\"ssl_required\":false,\"max_payload\":1048576}";
		final ServerInfoFrame infoFrame = (ServerInfoFrame) clientDecode("INFO " + jsonPayload + "\r\n");

		assertNotNull(infoFrame);
		assertEquals(infoFrame.getInfo(), jsonPayload);
	}

	@Test
	public void serverOK() throws Exception {
		final ServerOkFrame okFrame = (ServerOkFrame) clientDecode("+OK\r\n");

		assertNotNull(okFrame);
		assertSame(okFrame, ServerOkFrame.OK_MESSAGE);
	}

	@Test
	public void serverPing() throws Exception {
		final ServerPingFrame pingFrame = (ServerPingFrame) clientDecode("PING\r\n");
		assertNotNull(pingFrame);
		assertSame(pingFrame, ServerPingFrame.PING);
	}

	@Test
	public void serverPong() throws Exception {
		final ServerPongFrame pongFrame = (ServerPongFrame) clientDecode("PONG\r\n");
		assertNotNull(pongFrame);
		assertSame(pongFrame, ServerPongFrame.PONG);
	}

	@Test
	public void publish() throws Exception {
		final String subject = "foo.bar";
		final String id = "1";
		final String body = "Hi there";
		final ServerPublishFrame publishFrame = (ServerPublishFrame) clientDecode("MSG " + subject + " " + id + " " + body.length() + "\r\n" + body + "\r\n");
		assertNotNull(publishFrame);
		assertEquals(publishFrame.getSubject(), subject);
		assertEquals(publishFrame.getId(), id);
		assertEquals(publishFrame.getBody(), body);
		assertNull(publishFrame.getReplyTo());
	}

	@Test
	public void publishEmptyBody() throws Exception {
		final String subject = "foo";
		final String id = "bob";
		final ServerPublishFrame publishFrame = (ServerPublishFrame) clientDecode("MSG " + subject + " " + id + " 0\r\n\r\n");
		assertNotNull(publishFrame);
		assertEquals(publishFrame.getSubject(), subject);
		assertEquals(publishFrame.getId(), id);
		assertEquals(publishFrame.getBody(), "");
		assertNull(publishFrame.getReplyTo());
	}

	@Test
	public void publishWithReply() throws Exception {
		final String subject = "request.foo";
		final String id = "3";
		final String replyTo = "reply.subject";
		final String body = "bar";
		final ServerPublishFrame publishFrame = (ServerPublishFrame) clientDecode("MSG " + subject + " " + id + " " + replyTo + " 3\r\n" + body + "\r\n");
		assertNotNull(publishFrame);
		assertEquals(publishFrame.getSubject(), subject);
		assertEquals(publishFrame.getId(), id);
		assertEquals(publishFrame.getBody(), body);
		assertEquals(publishFrame.getReplyTo(), replyTo);
	}

	private ServerFrame clientDecode(String frame) {
		final EmbeddedChannel channel = new EmbeddedChannel(new ServerFrameDecoder());
		channel.writeInbound(Unpooled.wrappedBuffer(frame.getBytes()));
		return (ServerFrame) channel.readInbound();
	}

}
