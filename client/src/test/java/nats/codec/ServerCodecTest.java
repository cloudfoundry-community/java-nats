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

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import static org.testng.Assert.*;

import io.netty.handler.codec.DecoderException;
import org.testng.annotations.Test;

/**
 * @author Mike Heath
 */
public class ServerCodecTest {

	@Test
	public void clientConnect() throws Exception {
		final String user = "user";
		final String connectBody = "{\"user\":\"" + user + "\"}";
		final ClientConnectFrame connectFrame = (ClientConnectFrame) clientDecode("CONNECT " + connectBody + "\r\n");
		assertNotNull(connectFrame.getBody());
		assertEquals(connectFrame.getBody().getUser(), user);
	}

	@Test
	public void clientPong() throws Exception {
		final ClientPongFrame pongFrame = (ClientPongFrame) clientDecode("PONG\r\n");
		assertNotNull(pongFrame);
		assertSame(pongFrame, ClientPongFrame.PONG);
	}

	@Test
	public void clientPing() throws Exception {
		final ClientPingFrame pingFrame = (ClientPingFrame) clientDecode("PING\r\n");
		assertNotNull(pingFrame);
		assertSame(pingFrame, ClientPingFrame.PING);
	}

	@Test
	public void clientPublish() throws Exception {
		final String body = "Have a nice day!";
		final String subject = "foo";
		final ClientPublishFrame publishFrame = (ClientPublishFrame) clientDecode("PUB" + subject + " " + body.length() + "\r\n" + body + "\r\n");
		assertEquals(publishFrame.getSubject(), subject);
		assertEquals(publishFrame.getBody(), body);
		assertNull(publishFrame.getReplyTo());
	}

	@Test
	public void clientPublishWithReply() throws Exception {
		final String body = "Have a nice day!";
		final String subject = "foo";
		final String replyTo = "replyToMePlease";
		final ClientPublishFrame publishFrame = (ClientPublishFrame) clientDecode("PUB" + subject + " " + replyTo + "  " + body.length() + "\r\n" + body + "\r\n");
		assertNotNull(publishFrame);
		assertEquals(publishFrame.getSubject(), subject);
		assertEquals(publishFrame.getBody(), body);
		assertEquals(publishFrame.getReplyTo(), replyTo);
	}

	@Test
	public void clientSubscribe() throws Exception {
		final String subject = "foo.bar";
		final String id = "id1";
		final ClientSubscribeFrame subscribeFrame = (ClientSubscribeFrame) clientDecode("SUB " + subject + " " + id + "\r\n");
		assertEquals(subscribeFrame.getSubject(), subject);
		assertEquals(subscribeFrame.getId(), id);
	}

	@Test
	public void clientSubscribeWithQueueGroup() throws Exception {
		final String subject = "a.b.c.d.e";
		final String id = "thisIsTheId";
		final String queueGroup = "queueGroupQueueGroup";
		final ClientSubscribeFrame subscribeFrame = (ClientSubscribeFrame) clientDecode("SUB " + subject + " " + queueGroup + " " + id + "\r\n");
		assertEquals(subscribeFrame.getSubject(), subject);
		assertEquals(subscribeFrame.getId(), id);
		assertEquals(subscribeFrame.getQueueGroup(), queueGroup);
	}

	@Test
	public void clientUnsubscribe() throws Exception {
		final String id = "thisSubjectSucksLetsUnsubscribe";
		final ClientUnsubscribeFrame message = (ClientUnsubscribeFrame) clientDecode("UNSUB " + id + "\r\n");
		assertEquals(message.getId(), id);
		assertEquals(message.getMaxMessages(), null);
	}

	@Test
	public void clientUnsubscribeWithMaxMessages() throws Exception {
		final String id = "thisSubjectSucksLetsUnsubscribe";
		final Integer maxMessages = 2;
		final ClientUnsubscribeFrame message = (ClientUnsubscribeFrame) clientDecode("UNSUB " + id + " " + maxMessages + "\r\n");
		assertEquals(message.getId(), id);
		assertEquals(message.getMaxMessages(), maxMessages);
	}

	@Test(expectedExceptions = DecoderException.class)
	public void invalidMessage() throws Exception {
		clientDecode("bad message\r\n");
	}

	private ClientFrame clientDecode(String frame) {
		final EmbeddedChannel channel = new EmbeddedChannel(new ClientFrameDecoder());
		channel.writeInbound(Unpooled.wrappedBuffer(frame.getBytes()));
		return (ClientFrame) channel.readInbound();
	}

}
