package nats.codec;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.embedder.DecoderEmbedder;
import org.testng.Assert;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public abstract class AbstractDecoderTest<T> {

	protected void decoderTest(String packet, DecoderAssertions resultAssertions) throws Exception {
		for (int packetSize = 1; packetSize < packet.length(); packetSize++) {
			DecoderEmbedder<T> decoderEmbedder = createDecoderEmbedder();
			for (int i = 0; i < packet.length(); i+= packetSize) {
				decoderEmbedder.offer(ChannelBuffers.wrappedBuffer(packet.substring(i, Math.min(i + packetSize, packet.length())).getBytes()));
			}
			Assert.assertTrue(decoderEmbedder.finish(), "The decode should have at least one message available.");
			resultAssertions.runAssertions(decoderEmbedder);
		}
	}

	protected abstract DecoderEmbedder<T> createDecoderEmbedder();

	protected interface DecoderAssertions<T> {
		void runAssertions(DecoderEmbedder<T> decoderEmbedder) throws Exception;
	}

}
