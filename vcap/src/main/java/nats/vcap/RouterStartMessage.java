package nats.vcap;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * See http://apidocs.cloudfoundry.com/router/publish-router-start.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
@NatsSubject("router.start")
public class RouterStartMessage implements VcapMessage {
	private final String id;
	private final String version;

	@JsonCreator
	public RouterStartMessage(@JsonProperty("id") String id, @JsonProperty("version") String version) {
		this.id = id;
		this.version = version;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RouterStartMessage that = (RouterStartMessage) o;

		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (version != null ? !version.equals(that.version) : that.version != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (version != null ? version.hashCode() : 0);
		return result;
	}
}
