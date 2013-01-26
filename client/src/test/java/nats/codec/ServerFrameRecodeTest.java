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
import io.netty.channel.embedded.EmbeddedByteChannel;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerFrameRecodeTest {

	@Test
	public void serverErrorFrame() {
		final String errorMessage = "'This is an error message.'";
		final ServerErrorFrame recodedFrame = recode(new ServerErrorFrame(errorMessage));
		assertEquals(recodedFrame.getErrorMessage(), errorMessage);
		final ServerErrorFrame recodedFrameNullMessage = recode(new ServerErrorFrame(null));
		assertNull(recodedFrameNullMessage.getErrorMessage());
	}

	@Test
	public void serverInfoFrame() {
		final String info = "This is some server info or something.";
		final ServerInfoFrame recodedFrame = recode(new ServerInfoFrame(info));
		assertEquals(recodedFrame.getInfo(), info);
	}

	@Test
	public void serverOkFrame() {
		final ServerOkFrame recodedFrame = recode(ServerOkFrame.OK_MESSAGE);
		assertSame(recodedFrame, ServerOkFrame.OK_MESSAGE);
	}

	@Test
	public void serverPingFrame() {
		final ServerPingFrame recodedFrame = recode(ServerPingFrame.PING);
		assertSame(recodedFrame, ServerPingFrame.PING);
	}

	@Test
	public void serverPongFrame() {
		final ServerPongFrame recodedFrame = recode(ServerPongFrame.PONG);
		assertSame(recodedFrame, ServerPongFrame.PONG);
	}

	@Test
	public void serverPublishFrame() {
		final String id = "thisIsASubscriptionId";
		final String subject = "this.is.a.subject";
		final String replyTo = "_INBOX.9488293498239492303240";
		final String body = "Message body.";
		final ServerPublishFrame recodedFrame = recode(new ServerPublishFrame(id, subject, replyTo, body));
		assertEquals(recodedFrame.getId(), id);
		assertEquals(recodedFrame.getSubject(), subject);
		assertEquals(recodedFrame.getReplyTo(), replyTo);
		assertEquals(recodedFrame.getBody(), body);
	}

	@Test
	public void serverPublishFrameNoReply() {
		final String id = "thisIsASubscriptionId";
		final String subject = "this.is.a.subject";
		final String body = "Message body.";
		final ServerPublishFrame recodedFrame = recode(new ServerPublishFrame(id, subject, null, body));
		assertEquals(recodedFrame.getId(), id);
		assertEquals(recodedFrame.getSubject(), subject);
		assertEquals(recodedFrame.getBody(), body);
		assertNull(recodedFrame.getReplyTo());
	}

	protected <T extends NatsFrame> T recode(T frame) {
		final EmbeddedByteChannel channel = new EmbeddedByteChannel(new ServerFrameEncoder(), new ServerFrameDecoder());

		// Encode
		channel.write(frame);
		channel.checkException();
		final ByteBuf data = channel.readOutbound();

		// Decode
		channel.writeInbound(data);
		channel.checkException();
		final T recodedFrame = (T) channel.readInbound();

		// Ensure we got a frame
		assertNotNull(recodedFrame);

		return recodedFrame;
	}

}
