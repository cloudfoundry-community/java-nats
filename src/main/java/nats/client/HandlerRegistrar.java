package nats.client;

import nats.HandlerRegistration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class HandlerRegistrar<T> implements Iterable<T> {

	public static final HandlerRegistration EMPTY_HANDLER_REGISTRATION = new HandlerRegistration() {
		@Override
		public void remove() {
			// DO nothing.
		}
	};

	private final List<HandlerRegistrarEntry> handlers = new ArrayList<HandlerRegistrarEntry>();

	HandlerRegistration addHandler(T handler) {
		HandlerRegistrarEntry entry = new HandlerRegistrarEntry(handler);
		synchronized (handlers) {
			handlers.add(entry);
		}
		return entry;
	}

	@Override
	public Iterator<T> iterator() {
		final List<HandlerRegistrarEntry> handlersCopy;
		synchronized (handlers) {
			handlersCopy = new ArrayList<HandlerRegistrarEntry>(handlers);
		}
		final Iterator<HandlerRegistrarEntry> iterator = handlers.iterator();
		return new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public T next() {
				return iterator.next().getHandler();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	private class HandlerRegistrarEntry implements HandlerRegistration {

		private final T handler;

		private HandlerRegistrarEntry(T handler) {
			this.handler = handler;
		}

		@Override
		public void remove() {
			synchronized (handlers) {
				handlers.remove(this);
			}
		}

		public T getHandler() {
			return handler;
		}
	}

}
