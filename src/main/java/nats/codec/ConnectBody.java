package nats.codec;

import nats.NatsException;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ConnectBody {

	private final String user;
	private final String password;
	private final boolean pedantic;
	private final boolean verbose;

	@JsonCreator
	public ConnectBody(
			@JsonProperty("user")
			String user,
			@JsonProperty("password")
			String password,
			@JsonProperty("pedantic")
			boolean pedantic,
			@JsonProperty("verbose")
			boolean verbose) {
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

	public static ConnectBody parse(String body) {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			return mapper.readValue(body, ConnectBody.class);
		} catch (IOException e) {
			throw new NatsException(e);
		}
	}

}
