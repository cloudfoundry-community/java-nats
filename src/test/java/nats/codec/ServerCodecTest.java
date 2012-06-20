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

import org.jboss.netty.handler.codec.embedder.CodecEmbedderException;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerCodecTest extends AbstractDecoderTest<ClientMessage> {

	@Test
	public void clientConnect() throws Exception {
		final String user = "user";
		final String connectBody = "{\"user\":\"" + user + "\"}";
		decoderTest("CONNECT " + connectBody + "\r\n", new DecoderAssertions<ClientMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				ClientConnectMessage message = (ClientConnectMessage) decoderEmbedder.poll();
				Assert.assertNotNull(message.getBody());
				Assert.assertEquals(message.getBody().getUser(), user);
			}
		});
	}

	@Test
	public void clientPong() throws Exception {
		decoderTest("PONG\r\n", new DecoderAssertions<ClientMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				ClientPongMessage message = (ClientPongMessage) decoderEmbedder.poll();
				Assert.assertNotNull(message);
			}
		});
	}

	@Test
	public void clientPing() throws Exception {
		decoderTest("PING\r\n", new DecoderAssertions<ClientMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				ClientPingMessage message = (ClientPingMessage) decoderEmbedder.poll();
				Assert.assertNotNull(message);
			}
		});
	}

	@Test
	public void clientPublish() throws Exception {
		final String body = "Have a nice day!";
		final String subject = "foo";
		decoderTest("PUB" + subject + " " + body.length() + "\r\n" + body + "\r\n", new DecoderAssertions<ClientMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				ClientPublishMessage message = (ClientPublishMessage) decoderEmbedder.poll();
				Assert.assertEquals(message.getSubject(), subject);
				Assert.assertEquals(message.getBody(), body);
				Assert.assertNull(message.getReplyTo());
			}
		});
	}

	public void clientPublishWithReply() throws Exception {
		final String body = "Have a nice day!";
		final String subject = "foo";
		final String replyTo = "replyToMePlease";
		decoderTest("PUB" + subject + " " + replyTo + "  " + body.length() + "\r\n" + body + "\r\n", new DecoderAssertions<ClientMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				ClientPublishMessage message = (ClientPublishMessage) decoderEmbedder.poll();
				Assert.assertNotNull(message);
				Assert.assertEquals(message.getSubject(), subject);
				Assert.assertEquals(message.getBody(), body);
				Assert.assertEquals(message.getReplyTo(), replyTo);
			}
		});
	}

	@Test
	public void clientSubscribe() throws Exception {
		final String subject = "foo.bar";
		final String id = "id1";
		decoderTest("SUB " + subject + " " + id + "\r\n", new DecoderAssertions<ClientSubscribeMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientSubscribeMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				ClientSubscribeMessage message = decoderEmbedder.poll();
				Assert.assertEquals(message.getSubject(), subject);
				Assert.assertEquals(message.getId(), id);
			}
		});
	}

	@Test
	public void clientSubscribeWithQueueGroup() throws Exception {
		final String subject = "a.b.c.d.e";
		final String id = "thisIsTheId";
		final String queueGroup = "queueGroupQueueGroup";
		decoderTest("SUB " + subject + " " + queueGroup + " " + id + "\r\n", new DecoderAssertions<ClientSubscribeMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientSubscribeMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				ClientSubscribeMessage message = decoderEmbedder.poll();
				Assert.assertEquals(message.getSubject(), subject);
				Assert.assertEquals(message.getId(), id);
				Assert.assertEquals(message.getQueueGroup(), queueGroup);
			}
		});
	}

	@Test
	public void clientUnsubscribe() throws Exception {
		final String id = "thisSubjectSucksLetsUnsubscribe";
		decoderTest("UNSUB " + id + "\r\n", new DecoderAssertions<ClientUnsubscribeMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientUnsubscribeMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				final ClientUnsubscribeMessage message = decoderEmbedder.poll();
				Assert.assertEquals(message.getId(), id);
				Assert.assertEquals(message.getMaxMessages(), null);
			}
		});
	}

	@Test
	public void clientUnsubscribeWithMaxMessages() throws Exception {
		final String id = "thisSubjectSucksLetsUnsubscribe";
		final Integer maxMessages = 2;
		decoderTest("UNSUB " + id + " " + maxMessages + "\r\n", new DecoderAssertions<ClientUnsubscribeMessage>() {
			@Override
			public void runAssertions(DecoderEmbedder<ClientUnsubscribeMessage> decoderEmbedder) throws Exception {
				Assert.assertEquals(decoderEmbedder.size(), 1);
				final ClientUnsubscribeMessage message = decoderEmbedder.poll();
				Assert.assertEquals(message.getId(), id);
				Assert.assertEquals(message.getMaxMessages(), maxMessages);
			}
		});
	}

	@Test
	public void invalidMessage() throws Exception {
		try {
			decoderTest("bad message\r\n", new DecoderAssertions() {
				@Override
				public void runAssertions(DecoderEmbedder decoderEmbedder) throws Exception {
					Assert.fail("An exception should have been thrown.");
				}
			});
		} catch (CodecEmbedderException e) {
			Assert.assertTrue(e.getCause() instanceof NatsDecodingException);
		}
	}

	@Override
	protected DecoderEmbedder<ClientMessage> createDecoderEmbedder() {
		return new DecoderEmbedder<ClientMessage>(new ServerCodec());
	}
}
