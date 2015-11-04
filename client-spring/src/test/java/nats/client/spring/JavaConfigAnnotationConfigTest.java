/*
 *   Copyright (c) 2013 Intellectual Reserve, Inc.  All rights reserved.
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
package nats.client.spring;

import nats.client.Message;
import nats.client.MockNats;
import nats.client.Nats;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Mike Heath
 */
public class JavaConfigAnnotationConfigTest {

	@Test
	public void subscribeAnnotation() {
		final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AnnotationConfig.class);
		try {
			final Nats nats = context.getBean(Nats.class);
			testNatsConfig(context, nats);
		} finally {
			context.close();
		}
	}

	private static final String NATS_BEAN = "natsBean";
	private static final String WRONG_NATS_BEAN = "wrongNatsBean";

	@Test
	public void subscribeAnnotationNamedNatsBean() {
		final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(NamedNatsAnnotationConfig.class);
		try {
			final Nats nats = context.getBean(NATS_BEAN, Nats.class);
			final Nats wrongNats = context.getBean(WRONG_NATS_BEAN, Nats.class);
			wrongNats.publish("test", "Should not get processed");
			testNatsConfig(context, nats);
		} finally {
			context.close();
		}
	}

	private void testNatsConfig(AnnotationConfigApplicationContext context, Nats nats) {
		assertNotNull(nats);

		final String body = "Java config is cool.";
		nats.publish("test", body);

		final SubscribeBean subscribeBean = context.getBean(SubscribeBean.class);

		assertEquals(subscribeBean.messages.size(), 1);
		assertEquals(body, subscribeBean.messages.get(0).getBody());
	}

	@Configuration
	@EnableNatsAnnotations
	public static class AnnotationConfig {

		public @Bean Nats nats() {
			return new MockNats();
		}

		public @Bean SubscribeBean subscribeBean() {
			return new SubscribeBean();
		}

	}

	@Configuration
	@EnableNatsAnnotations(NATS_BEAN)
	public static class NamedNatsAnnotationConfig {

		public @Bean(name = NATS_BEAN) Nats nats() {
			return new MockNats();
		}

		public @Bean(name = WRONG_NATS_BEAN) Nats wrongNats() {
			return new MockNats();
		}

		public @Bean SubscribeBean subscribeBean() {
			return new SubscribeBean();
		}

	}

	public static class SubscribeBean {
		List<Message> messages = new ArrayList<>();

		@Subscribe("test")
		public void onMessage(Message message) {
			messages.add(message);
		}
	}
}
