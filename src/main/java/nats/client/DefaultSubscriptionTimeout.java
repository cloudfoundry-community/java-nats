package nats.client;

import nats.HandlerRegistration;
import nats.NatsInterruptedException;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class DefaultSubscriptionTimeout implements SubscriptionTimeout {

	private final Object monitor = new Object();
	private final HandlerRegistrar<TimeoutHandler> registrar = new HandlerRegistrar<TimeoutHandler>();
	private final Subscription subscription;
	private final Timeout timeout;

	DefaultSubscriptionTimeout(Timer timer, final Subscription subscription, final ExceptionHandler exceptionHandler, long time, TimeUnit unit) {
		this.subscription = subscription;
		timeout = timer.newTimeout(new TimerTask() {
			@Override
			public void run(Timeout timeout) throws Exception {
				subscription.close();
				for (TimeoutHandler handler : registrar) {
					try {
						handler.onTimeout(DefaultSubscriptionTimeout.this);
					} catch (Throwable t) {
						exceptionHandler.onException(t);
					}
				}
				synchronized (monitor) {
					monitor.notifyAll();
				}
			}
		}, time, unit);
	}

	@Override
	public void await() {
		synchronized (monitor) {
			try {
				monitor.wait();
			} catch (InterruptedException e) {
				throw new NatsInterruptedException(e);
			}
		}
	}

	@Override
	public boolean cancel() {
		timeout.cancel();
		if (timeout.isCancelled()) {
			synchronized (monitor) {
				monitor.notifyAll();
			}
			return true;
		}
		return false;
	}

	@Override
	public Subscription getSubscription() {
		return subscription;
	}

	@Override
	public HandlerRegistration addTimeoutHandler(TimeoutHandler timeoutHandler) {
		return registrar.addHandler(timeoutHandler);
	}
}
