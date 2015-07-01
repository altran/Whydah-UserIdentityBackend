package net.whydah.identity.application.authentication;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import net.whydah.identity.Main;
import net.whydah.identity.config.ApplicationMode;
import net.whydah.identity.dataimport.DatabaseMigrationHelper;
import net.whydah.identity.util.FileUtils;
import net.whydah.sso.application.Application;
import net.whydah.sso.application.ApplicationCredential;
import net.whydah.sso.application.ApplicationCredentialSerializer;
import net.whydah.sso.application.ApplicationSerializer;
import org.apache.commons.dbcp.BasicDataSource;
import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-01
 */
public class ApplicationAuthenticationResourceTest {
    private final String appToken1 = "appToken1";
    private final String userToken1 = "userToken1";
    private Main main;
    private Application app;
    private Application applicationResponse;

    @BeforeClass
    public void startServer() {
        ApplicationMode.setTags(ApplicationMode.CI_MODE, ApplicationMode.NO_SECURITY_FILTER);
        final ConstrettoConfiguration configuration = new ConstrettoBuilder()
                .createPropertiesStore()
                .addResource(Resource.create("classpath:useridentitybackend.properties"))
                .addResource(Resource.create("file:./useridentitybackend_override.properties"))
                .done()
                .getConfiguration();

        String roleDBDirectory = configuration.evaluateToString("roledb.directory");
        FileUtils.deleteDirectory(roleDBDirectory);
        BasicDataSource dataSource = initBasicDataSource(configuration);
        DatabaseMigrationHelper dbHelper = new DatabaseMigrationHelper(dataSource);
        dbHelper.cleanDatabase();
        dbHelper.upgradeDatabase();

        main = new Main(6644);
        main.start();
        RestAssured.port = main.getPort();
        RestAssured.basePath = Main.CONTEXT_PATH;
    }

    private static BasicDataSource initBasicDataSource(ConstrettoConfiguration configuration) {
        String jdbcdriver = configuration.evaluateToString("roledb.jdbc.driver");
        String jdbcurl = configuration.evaluateToString("roledb.jdbc.url");
        String roledbuser = configuration.evaluateToString("roledb.jdbc.user");
        String roledbpasswd = configuration.evaluateToString("roledb.jdbc.password");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(jdbcdriver);
        dataSource.setUrl(jdbcurl);
        dataSource.setUsername(roledbuser);
        dataSource.setPassword(roledbpasswd);
        return dataSource;
    }

    @AfterClass
    public void stop() {
        if (main != null) {
            main.stop();
        }
    }

    @Test
    public void testAuthenticateApplicationInvalidXml() throws Exception {
        String body = "this is invalid xml";
        String path = "/application/authenticate";
        given()
                .body(body)
                .contentType(ContentType.XML)
                .log().everything()
                .expect()
                .statusCode(400)
                .log().ifError()
                .when()
                .post(path);
    }

    @Test
    public void testAuthenticateApplicationNoSuchApplication() throws Exception {
        String body = ApplicationCredentialSerializer.toXML(new ApplicationCredential("nonExistingAppId", "someSecret"));
        String path = "/application/authenticate";
        given()
                .body(body)
                .contentType(ContentType.XML)
                .log().everything()
                .expect()
                .statusCode(401)
                .log().ifError()
                .when()
                .post(path);


        //existing application, but wrong secret
        app = new Application("ignoredId", "appName1");
        app.getSecurity().setSecret("secret1");
        app.setDefaultRoleName("originalDefaultRoleName");
        String json = ApplicationSerializer.toJson(app);

        String createApplicationPath = "/{applicationtokenid}/{userTokenId}/application";
        Response response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(createApplicationPath, appToken1, userToken1);

        String jsonResponse = response.body().asString();
        Application applicationResponse = ApplicationSerializer.fromJson(jsonResponse);

        body = ApplicationCredentialSerializer.toXML(new ApplicationCredential(applicationResponse.getId(), "wrongSecret"));
        given()
                .body(body)
                .contentType(ContentType.XML)
                .log().everything()
                .expect()
                .statusCode(401)
                .log().ifError()
                .when()
                .post(path);
    }

    @Test
    public void testAuthenticateApplicationWrongSecret() throws Exception {
        //add application
        app = new Application("ignoredId", "appName1");
        app.getSecurity().setSecret("secret1");
        app.setDefaultRoleName("originalDefaultRoleName");
        String json = ApplicationSerializer.toJson(app);

        String createApplicationPath = "/{applicationtokenid}/{userTokenId}/application";
        Response response = given()
                .body(json)
                .contentType(ContentType.JSON)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(createApplicationPath, appToken1, userToken1);

        //existing application, but wrong secret
        String jsonResponse = response.body().asString();
        applicationResponse = ApplicationSerializer.fromJson(jsonResponse);
        String path = "/application/authenticate";
        String body = ApplicationCredentialSerializer.toXML(new ApplicationCredential(applicationResponse.getId(), "wrongSecret"));
        given()
                .body(body)
                .contentType(ContentType.XML)
                .log().everything()
                .expect()
                .statusCode(401)
                .log().ifError()
                .when()
                .post(path);
    }

    @Test(dependsOnMethods = "testAuthenticateApplicationWrongSecret")
    public void testAuthenticateApplicationOK() throws Exception {
        String path = "/application/authenticate";
        ApplicationCredential validCredential = new ApplicationCredential(applicationResponse.getId(), applicationResponse.getSecurity().getSecret());
        String body = ApplicationCredentialSerializer.toXML(validCredential);
        Response response = given()
                .body(body)
                .contentType(ContentType.XML)
                .log().everything()
                .expect()
                .statusCode(200)
                .log().ifError()
                .when()
                .post(path);

        String jsonResponse = response.body().asString();
        Application authResponse = ApplicationSerializer.fromJson(jsonResponse);

        assertNotNull(authResponse.getId());
        assertEquals(applicationResponse.getId(), authResponse.getId());
        assertEquals(authResponse.getName(), app.getName());
        assertEquals(authResponse.getSecurity().getSecret(), app.getSecurity().getSecret());
        assertEquals(authResponse.getDefaultRoleName(), app.getDefaultRoleName());
    }
}
