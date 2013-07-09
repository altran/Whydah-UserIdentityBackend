package no.freecode.iam.service.user;

public class Application {

	private final String id;
	private final String name;
	private final String defaultRoleName;
	private final String defaultOrganizationId;

	public Application(String applicationId, String applicationName, String defaultRoleName, String defaultOrganizationId) {
		this.id = applicationId;
		this.name = applicationName;
		this.defaultRoleName = defaultRoleName;
		this.defaultOrganizationId = defaultOrganizationId;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDefaultRoleName() {
		return defaultRoleName;
	}

	public String getDefaultOrganizationId() {
		return defaultOrganizationId;
	}

	@Override
	public String toString() {
		return "Application [id=" + id + ", name=" + name
				+ ", defaultRoleName=" + defaultRoleName
				+ ", defaultOrganizationId=" + defaultOrganizationId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Application other = (Application) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
