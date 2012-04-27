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

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientCodecTest {

	@Test
	public void serverError() throws Exception {
		final String errorMessage = "'You are doing it wrong'";
		decoderTest("-ERR " + errorMessage + "\r\n", new DecoderAssertions() {
			@Override
			public void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(1, decoderEmbedder.size());
				ServerErrorMessage errorMesage = (ServerErrorMessage) decoderEmbedder.poll();
				Assert.assertEquals(errorMesage.getErrorMessage(), errorMessage);
			}
		});
	}

	@Test
	public void serverInfo() throws Exception {
		final String jsonPayload = "{\"server_id\":\"8d8222a15560538b92751995be\",\"host\":\"0.0.0.0\",\"port\":4222,\"version\":\"0.4.22\",\"auth_required\":false,\"ssl_required\":false,\"max_payload\":1048576}";
		decoderTest("INFO " + jsonPayload + "\r\n", new DecoderAssertions() {
			@Override
			public void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(1, decoderEmbedder.size());
				ServerInfoMessage infoMessage = (ServerInfoMessage) decoderEmbedder.poll();
				Assert.assertEquals(infoMessage.getInfo(), jsonPayload);
			}
		});
	}

	@Test
	public void serverOk() throws Exception {
		decoderTest("+OK\r\n", new DecoderAssertions() {
			@Override
			public void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(1, decoderEmbedder.size());
				ServerOkMessage okMessage = (ServerOkMessage) decoderEmbedder.poll();
				Assert.assertNotNull(okMessage);
			}
		});
	}

	@Test
	public void serverPing() throws Exception {
		decoderTest("PING\r\n", new DecoderAssertions() {
			@Override
			public void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(1, decoderEmbedder.size());
				ServerPingMessage okMessage = (ServerPingMessage) decoderEmbedder.poll();
				Assert.assertNotNull(okMessage);
			}
		});
	}

	@Test
	public void serverPong() throws Exception {
		decoderTest("PONG\r\n", new DecoderAssertions() {
			@Override
			public void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(1, decoderEmbedder.size());
				ServerPongMessage okMessage = (ServerPongMessage) decoderEmbedder.poll();
				Assert.assertNotNull(okMessage);
			}
		});
	}

	@Test
	public void publish() throws Exception {
		final String subject = "foo.bar";
		final int id = 1;
		final String body = "Hi there";
		decoderTest("MSG " + subject + " " + id + " 8\r\n" + body + "\r\n", new DecoderAssertions() {
			@Override
			public void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(1, decoderEmbedder.size());
				ServerPublishMessage publishMessage = (ServerPublishMessage) decoderEmbedder.poll();
				Assert.assertEquals(publishMessage.getSubject(), subject);
				Assert.assertEquals(publishMessage.getId(), id);
				Assert.assertEquals(publishMessage.getBody(), body);
				Assert.assertNull(publishMessage.getReplyTo());
			}
		});
	}

	@Test
	public void publishEmptyBody() throws Exception {
		final String subject = "foo";
		final int id = 2;
		decoderTest("MSG " + subject + " " + id + " 0\r\n\r\n", new DecoderAssertions() {
			@Override
			public void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(1, decoderEmbedder.size());
				ServerPublishMessage publishMessage = (ServerPublishMessage) decoderEmbedder.poll();
				Assert.assertEquals(publishMessage.getSubject(), subject);
				Assert.assertEquals(publishMessage.getId(), id);
				Assert.assertEquals(publishMessage.getBody(), "");
				Assert.assertNull(publishMessage.getReplyTo());
			}
		});
	}

	@Test
	public void publishWithReply() throws Exception {
		final String subject = "request.foo";
		final int id = 3;
		final String replyTo = "reply.subject";
		final String body = "bar";
		decoderTest("MSG " + subject + " " + id + " " + replyTo + " 3\r\n" + body + "\r\n", new DecoderAssertions() {
			@Override
			public void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(1, decoderEmbedder.size());
				ServerPublishMessage publishMessage = (ServerPublishMessage) decoderEmbedder.poll();
				Assert.assertEquals(publishMessage.getSubject(), subject);
				Assert.assertEquals(publishMessage.getId(), id);
				Assert.assertEquals(publishMessage.getBody(), body);
				Assert.assertEquals(publishMessage.getReplyTo(), replyTo);
			}
		});
	}

	protected void decoderTest(String packet, DecoderAssertions resultAssertions) throws Exception {
		for (int packetSize = 1; packetSize < packet.length(); packetSize++) {
			DecoderEmbedder<ServerMessage> decoderEmbedder = new DecoderEmbedder<ServerMessage>(new ClientCodec());
			for (int i = 0; i < packet.length(); i+= packetSize) {
				decoderEmbedder.offer(ChannelBuffers.wrappedBuffer(packet.substring(i, Math.min(i + packetSize, packet.length())).getBytes()));
			}
			Assert.assertTrue(decoderEmbedder.finish(), "The decode should have at least one message available.");
			resultAssertions.runAssertions(decoderEmbedder);
		}
	}

	private interface DecoderAssertions {
		void runAssertions(DecoderEmbedder<ServerMessage> decoderEmbedder) throws Exception;
	}

}
