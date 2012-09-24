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
