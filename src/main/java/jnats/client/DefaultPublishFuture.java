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
package jnats.client;

import jnats.CompletionHandler;
import jnats.HandlerRegistration;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class DefaultPublishFuture implements PublishFuture {

	private final String subject;
	private final String message;
	private final String replyTo;

	private boolean done;
	private Throwable cause;
	private List<PublishHandlerRegistration> listeners;
	private final Object lock = new Object();

	private final ExceptionHandler exceptionHandler;

	public DefaultPublishFuture(String subject, String message, String replyTo, ExceptionHandler exceptionHandler) {
		this.subject = subject;
		this.message = message;
		this.replyTo = replyTo;

		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public String getReplyTo() {
		return replyTo;
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public HandlerRegistration addCompletionHandler(final CompletionHandler listener) {
		synchronized (lock) {
			if (done) {
				invokeListener(listener);
				return EMPTY_HANDLER_REGISTRATION;
			} else {
				PublishHandlerRegistration registration = new PublishHandlerRegistration(listener) {
					@Override
					public void remove() {
						synchronized (lock) {
							listeners.remove(this);
						}
					}
				};
				if (listeners == null) {
					listeners = new LinkedList<PublishHandlerRegistration>();
				}
				listeners.add(registration);
				return registration;
			}
		}
	}

	@Override
	public boolean isDone() {
		synchronized (lock) {
			return done;
		}
	}

	@Override
	public boolean isSuccess() {
		synchronized (lock) {
			return cause == null;
		}
	}

	@Override
	public Throwable getCause() {
		synchronized (lock) {
			return cause;
		}
	}

	@Override
	public void await() throws InterruptedException {
		synchronized (lock) {
			lock.wait();
		}
	}

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		synchronized (lock) {
			lock.wait(unit.toMillis(timeout));
			return isDone();
		}
	}

	void setDone(Throwable cause) {
		synchronized (lock) {
			if (done) {
				return;
			}
			done = true;
			this.cause = cause;
			if (listeners != null) {
				for (PublishHandlerRegistration handler : listeners) {
					invokeListener(handler.getCompletionHandler());
				}
			}
			lock.notifyAll();
		}
	}

	private void invokeListener(CompletionHandler listener) {
		try {
			listener.onComplete(this);
		} catch (Throwable t) {
			exceptionHandler.onException(t);
		}
	}

	private static abstract class PublishHandlerRegistration implements HandlerRegistration {

		private final CompletionHandler completionHandler;

		private PublishHandlerRegistration(CompletionHandler completionHandler) {
			this.completionHandler = completionHandler;
		}

		public CompletionHandler getCompletionHandler() {
			return completionHandler;
		}
	}

	private static final HandlerRegistration EMPTY_HANDLER_REGISTRATION = new HandlerRegistration() {
		@Override
		public void remove() {
			// DO nothing.
		}
	};

}
