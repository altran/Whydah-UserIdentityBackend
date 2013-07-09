package no.freecode.iam.service.dataimport;

import com.google.inject.Inject;
import no.freecode.iam.service.domain.UserPropertyAndRole;
import no.freecode.iam.service.domain.WhydahUserIdentity;
import no.freecode.iam.service.helper.StringCleaner;
import no.freecode.iam.service.ldap.LDAPHelper;
import no.freecode.iam.service.repository.UserPropertyAndRoleRepository;
import no.freecode.iam.service.search.Indexer;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class PstyrImporter {
    private static final Logger logger = LoggerFactory.getLogger(PstyrImporter.class);

    public static final List<String> invoiceRoles = Arrays.asList("FM", "VF", "SM", "VA", "AND");

    private static final int BORETTSLAGSNR = 0;
    //    private static final int MEDLEMSNR = 1;
    private static final int ROLLEKODE = 2;
    private static final int ETTERNAVN = 3;
    private static final int FORNAVN = 4;
    //    private static final int ADRESSE = 5;
//    private static final int POSTSTED = 6;
//    private static final int POSTNR = 7;
//    private static final int TELEFON_KVELD = 8;
//    private static final int TELEFON_HJEMME = 9;
//    private static final int STYRET_REKKEFOLGE = 10;
    private static final int FODSELSDATO = 12;
    private static final int FODSELSNR = 13;
    private static final int TELEFON_MOBIL = 15;
    private static final int EPOST = 16;

    private static final int STYRE_AAR_FRA = 17;//?
    private static final int STYRE_AAR_TIL = 18;//?
//    private static final int STYRE_DATO_FRA = 19;//?
//    private static final int STYRE_DATO_TIL = 20;//?

    private static final StringCleaner stringCleaner = new StringCleaner();
    private final Set<String> userswithoutemail = new HashSet<String>();

    private LDAPHelper ldapHelper;
    private QueryRunner queryRunner;
    private UserPropertyAndRoleRepository roleRepository;
    private DatabaseHelper databaseHelper;
    private Directory index;

    public void importUsers(String userImportSource) {
        databaseHelper.initDB();
        BufferedReader reader = null;
        try {
            WhydahUserIdentity userIdentity;
            logger.info("Importing data from " + userImportSource);
            InputStream classpathStream = PstyrImporter.class.getClassLoader().getResourceAsStream(userImportSource);
            reader = new BufferedReader(new InputStreamReader(classpathStream, "ISO-8859-1"));
            final Set<String> idsAddedToLdap = new HashSet<String>();
            Indexer indexer = new Indexer(index);
            final IndexWriter indexWriter = indexer.getWriter();
            String line;
            while ((line = reader.readLine()) != null) {
                final String[] columns = line.split(",");
                String personnummer = getString(columns, FODSELSDATO) + getString(columns, FODSELSNR);
                if(personnummer.length() == 11) {
                    if(!idsAddedToLdap.contains(personnummer)) {
                        idsAddedToLdap.add(personnummer);
                        userIdentity = createUserIdentity(columns, personnummer);
                        if(userIdentity != null) {
                            String password = getString(columns, FODSELSDATO);
                            userIdentity.setPassword(password);
                            ldapHelper.addWhydahUserIdentity(userIdentity);
                            indexer.addToIndex(indexWriter, userIdentity);
                        }
                    } else {
                        userIdentity = ldapHelper.getUserinfo(createUsername(columns));
                    }
                    if(userIdentity != null) {
                        importToRole(columns, userIdentity);
                    }
                }
            }
//            legginnSuperbruker();
            userIdentity = legginnbrukeradmin(); //for test!
            indexer.addToIndex(indexWriter, userIdentity);
            indexWriter.optimize();
            indexWriter.close();
            logger.debug("{} regular users imported", idsAddedToLdap.size());
            logger.debug("{} regular users without email, not imported.", userswithoutemail.size());

        } catch (Exception e) {
            logger.error("Error importing from " + userImportSource, e);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.warn("Error closing stream", e);
                }
            }
        }
    }

    private WhydahUserIdentity legginnbrukeradmin() {
        WhydahUserIdentity whydahUserIdentity = new WhydahUserIdentity();
        whydahUserIdentity.setFirstName("Bruker");
        whydahUserIdentity.setLastName("Admin");
        whydahUserIdentity.setEmail("brukeradmin@nomail.qw");
        whydahUserIdentity.setCellPhone("11211211");
        whydahUserIdentity.setUid("badm");
        whydahUserIdentity.setUsername("badm");
        whydahUserIdentity.setPassword("mdab");
        try {
            ldapHelper.addWhydahUserIdentity(whydahUserIdentity);
            UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();
            userPropertyAndRole.setUid(whydahUserIdentity.getUid());
            userPropertyAndRole.setAppId("1");
            userPropertyAndRole.setOrgId("9999");
            userPropertyAndRole.setRoleName("Brukeradmin");
            roleRepository.addUserPropertyAndRole(userPropertyAndRole);
            logger.info("Added admin user:"+whydahUserIdentity);
        } catch (NamingException e) {
            logger.error(e.getLocalizedMessage(), e);
        }
        return whydahUserIdentity;
    }

    private WhydahUserIdentity createUserIdentity(String[] columns, String personnummer) throws NamingException {
        String username = createUsername(columns);
        if(username != null && username.length() > 5) {
            String personref = PersonrefHelper.createPersonref(personnummer);
            String uid = UUID.randomUUID().toString();

            WhydahUserIdentity whydahUserIdentity = new WhydahUserIdentity();
            whydahUserIdentity.setFirstName(getString(columns, FORNAVN));
            whydahUserIdentity.setLastName(getString(columns, ETTERNAVN));
            whydahUserIdentity.setEmail(getString(columns, EPOST));
            String cellphone = getString(columns, TELEFON_MOBIL);
            whydahUserIdentity.setCellPhone(cellphone.length() > 2 ? cellphone : "");
            whydahUserIdentity.setPersonRef(personref);
            whydahUserIdentity.setUid(uid);
            whydahUserIdentity.setUsername(username);
            return whydahUserIdentity;
        } else {
            userswithoutemail.add(getString(columns, FORNAVN) + " " + getString(columns, ETTERNAVN) + "," + personnummer);
        }
        return null;
    }

    private void importToRole(String[] columns, WhydahUserIdentity user) {
        String rolle = getString(columns, ROLLEKODE);
        UserPropertyAndRole userPropertyAndRole = new UserPropertyAndRole();
        userPropertyAndRole.setUid(user.getUid());
        if(invoiceRoles.contains(rolle)) {
            userPropertyAndRole.setAppId("bleh");
//            if(rolle.equals("SM") || rolle.equals("VM")) {
//                rolle += getString(columns, STYRET_REKKEFOLGE);
//            }
        } else {
            userPropertyAndRole.setAppId("blah");
        }
        String orgid = getString(columns, BORETTSLAGSNR);
        orgid = "0000".substring(orgid.length()) + orgid;
        userPropertyAndRole.setOrgId(orgid);
        userPropertyAndRole.setRoleName(rolle);
        String fra = getString(columns, STYRE_AAR_FRA);
        String til = getString(columns, STYRE_AAR_TIL);
        if(fra != null && !"0".equals(fra)) {
            userPropertyAndRole.setRoleValue(fra + " - " + til);
        }
        roleRepository.addUserPropertyAndRole(userPropertyAndRole);
    }

    private String createUsername(String[] columns) {
        return getString(columns, EPOST);
    }

    private String getString(String[] columns, int field) {
        return stringCleaner.cleanString(columns[field]);
    }

    @Inject
    public void setRoleRepository(UserPropertyAndRoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    @Inject
    public void setLdapHelper(LDAPHelper ldapHelper) {
        this.ldapHelper = ldapHelper;
    }
    @Inject
    public void setQueryRunner(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }
    @Inject
    public void setDatabaseHelper(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }
    @Inject
    public void setIndex(Directory index) {
        this.index = index;
    }
}
