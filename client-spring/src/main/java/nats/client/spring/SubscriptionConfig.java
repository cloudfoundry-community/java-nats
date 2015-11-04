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

/**
 * @author Mike Heath
 */
public class SubscriptionConfig {

	private final Object bean;
	private final String methodName;
	private final String queueGroup;
	private final String subscription;

	public SubscriptionConfig(String subscription, Object bean, String methodName, String queueGroup) {
		this.subscription = subscription;
		this.bean = bean;
		this.methodName = methodName;
		this.queueGroup = queueGroup;
	}

	public Object getBean() {
		return bean;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getQueueGroup() {
		return queueGroup;
	}

	public String getSubscription() {
		return subscription;
	}

}
