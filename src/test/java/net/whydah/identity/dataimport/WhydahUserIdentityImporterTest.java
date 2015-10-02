package net.whydah.identity.dataimport;

import net.whydah.identity.user.identity.UserIdentity;
import net.whydah.identity.util.FileUtils;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WhydahUserIdentityImporterTest {

	@Test
	public void parseUsers() {
		String userImportSource = "testusers.csv";

        InputStream userImportStream = FileUtils.openFileOnClasspath(userImportSource);
		List<UserIdentity> users = WhydahUserIdentityImporter.parseUsers(userImportStream);
		
		assertEquals("All users must be found.", 2, users.size());
		
		UserIdentity user1 = users.get(0);

		UserIdentity user2 = users.get(1);
		assertEquals("UserId must be set.", "erik.drolshammer", user2.getUid());
		assertEquals("UserName must be set.", "erikd", user2.getUsername());
		assertEquals("cellPhone must be set.", "+47123456", user2.getCellPhone());
		assertEquals("personRef must be set.", "2", user2.getPersonRef());
	}
}
