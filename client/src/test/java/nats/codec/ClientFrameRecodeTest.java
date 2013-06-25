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

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientFrameRecodeTest {

	@Test
	public void clientConnectFrame() {
		final String user = "user";
		final String password = "password";
		final boolean pedantic = true;
		final boolean verbose = true;
		final ConnectBody connectBody = new ConnectBody(user, password, pedantic, verbose);
		final ClientConnectFrame clientConnectFrame = new ClientConnectFrame(connectBody);
		final ClientConnectFrame recodedFrame = recode(clientConnectFrame);
		assertEquals(recodedFrame.getBody().getUser(), user);
		assertEquals(recodedFrame.getBody().getPassword(), password);
		assertEquals(recodedFrame.getBody().isPedantic(), pedantic);
		assertEquals(recodedFrame.getBody().isVerbose(), verbose);
	}

	@Test
	public void clientPingFrame() {
		assertSame(recode(ClientPingFrame.PING), ClientPingFrame.PING);
	}

	@Test
	public void clientPongFrame() {
		assertSame(recode(ClientPongFrame.PONG), ClientPongFrame.PONG);
	}

	@Test
	public void clientPublishFrame() {
		final String subject = "some.subject";
		final String body = "This is the message body";
		final String replyTo = "_INBOX.1234567890";
		final ClientPublishFrame clientPublishFrame = new ClientPublishFrame(subject, body, null);
		final ClientPublishFrame recodedFrame = recode(clientPublishFrame);
		assertEquals(recodedFrame.getSubject(), subject);
		assertEquals(recodedFrame.getBody(), body);
		assertNull(recodedFrame.getReplyTo());

		final ClientPublishFrame clientPublishFrameReplyTo = new ClientPublishFrame(subject, body, replyTo);
		final ClientPublishFrame recodedFrameReplyTo = recode(clientPublishFrameReplyTo);
		assertEquals(recodedFrameReplyTo.getSubject(), subject);
		assertEquals(recodedFrameReplyTo.getBody(), body);
		assertEquals(recodedFrameReplyTo.getReplyTo(), replyTo);
	}

	@Test
	public void clientSubscribeFrame() {
		final String id = "someId";
		final String subject = "a.test.subject";
		final String queueGroup = "queueGroupsSuck";
		final ClientSubscribeFrame recodedFrame = recode(new ClientSubscribeFrame(id, subject, null));
		assertEquals(recodedFrame.getId(), id);
		assertEquals(recodedFrame.getSubject(), subject);
		assertNull(recodedFrame.getQueueGroup());

		final ClientSubscribeFrame recodedFrameWithQueueGroup = recode(new ClientSubscribeFrame(id, subject, queueGroup));
		assertEquals(recodedFrameWithQueueGroup.getId(), id);
		assertEquals(recodedFrameWithQueueGroup.getSubject(), subject);
		assertEquals(recodedFrameWithQueueGroup.getQueueGroup(), queueGroup);

	}

	@Test
	public void clientUnsubscribeFrame() {
		final String id = "somdId";
		final Integer maxMessages = 23;
		final ClientUnsubscribeFrame recodedFrame = recode(new ClientUnsubscribeFrame(id));
		assertEquals(recodedFrame.getId(), id);
		assertNull(recodedFrame.getMaxMessages());

		final ClientUnsubscribeFrame recodedFrameMaxMessages = recode(new ClientUnsubscribeFrame(id, maxMessages));
		assertEquals(recodedFrame.getId(), id);
		assertEquals(recodedFrameMaxMessages.getMaxMessages(), maxMessages);
	}

	private <T extends NatsFrame> T recode(T frame) {
		final EmbeddedChannel channel = new EmbeddedChannel(new ClientFrameEncoder(), new ClientFrameDecoder());

		// Encode
		channel.write(frame);
		channel.checkException();
		final ByteBuf data = (ByteBuf) channel.readOutbound();

		// Decode
		channel.writeInbound(data);
		channel.checkException();
		final T recodedFrame = (T) channel.readInbound();

		// Ensure we got a frame
		assertNotNull(recodedFrame);

		return recodedFrame;
	}

}
