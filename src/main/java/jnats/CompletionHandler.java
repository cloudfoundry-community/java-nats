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
package jnats;

import jnats.NatsFuture;

/**
 * Provides a callback mechanism to be invoked when the operation represented by a {@link jnats.NatsFuture} completes.
 * 
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface CompletionHandler {

	/**
	 * This method gets invoked when the operation represented by a {@link jnats.NatsFuture} completes regardless of whether
	 * the operation was successful or not.
	 *
	 * @param future the future object that completed
	 */
	void onComplete(NatsFuture future);
}
