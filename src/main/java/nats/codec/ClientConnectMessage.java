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
package nats.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientConnectMessage implements ClientMessage, ClientRequest {

	private static final String CMD_CONNECT = "CONNECT";

	private final String user;
	private final String password;
	private final boolean pedantic;
	private final boolean verbose;

	public ClientConnectMessage(String user, String password, boolean pedantic, boolean verbose) {
		this.user = user;
		this.password = password;
		this.pedantic = pedantic;
		this.verbose = verbose;
	}

	public String getPassword() {
		return password;
	}

	public boolean isPedantic() {
		return pedantic;
	}

	public String getUser() {
		return user;
	}

	public boolean isVerbose() {
		return verbose;
	}

	@Override
	public String encode() {
		StringBuilder builder = new StringBuilder();
		builder.append(CMD_CONNECT);
		builder.append(" {");
		if (user != null) {
			appendJsonField(builder, "user", user);
			appendJsonField(builder, "pass", password);
		}
		appendJsonField(builder, "verbose", Boolean.toString(verbose));
		appendJsonField(builder, "pedantic", Boolean.toString(pedantic));
		builder.append("}\r\n");
		return builder.toString();
	}

	private void appendJsonField(StringBuilder builder, String field, String value) {
		if (builder.length() > CMD_CONNECT.length() + 2) {
			builder.append(',');
		}
		// TODO We need some real JSON encoding to escape the values properly
		builder.append('"').append(field).append('"').append(':').append('"').append(value).append('"');
	}

}
