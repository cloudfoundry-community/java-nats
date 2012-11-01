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
package nats.client.spring;

import nats.client.Nats;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class NatsFactoryBeanTest {

	@Test
	public void natsFactory() throws Exception {
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("natsFactoryContext.xml");
		try {
			final Nats nats = context.getBean(Nats.class);
			Assert.assertNotNull(nats);
		} finally {
			context.close();
		}
	}

	@Test(expectedExceptions = BeanCreationException.class)
	public void brokenAnnotationConfig() {
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("brokenNatsAnnotationConfigContext.xml");
		try {
			Assert.fail("Creation of BrokenSubscribeBean bean should have failed.");
		} finally {
			context.close();
		}
	}

	@Test
	public void annotationConfig() throws Exception {
		final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("annotationConfigContext.xml");
		try {
			final Nats nats = context.getBean(Nats.class);
			Assert.assertNotNull(nats);
		} finally {
			context.close();
		}
	}
}
