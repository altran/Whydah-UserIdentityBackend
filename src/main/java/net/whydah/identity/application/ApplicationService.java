package net.whydah.identity.application;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.whydah.identity.audit.ActionPerformed;
import net.whydah.identity.audit.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by baardl on 29.03.14.
 */
@Singleton
public class ApplicationService {
    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    private final ApplicationRepository applicationRepository;
    private final AuditLogRepository auditLogRepository;

    @Inject
    public ApplicationService(ApplicationRepository applicationRepository, AuditLogRepository auditLogRepository) {
        this.applicationRepository = applicationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Application createApplication(String applicationJson) {
        Application application = Application.fromJson(applicationJson);
        applicationRepository.create(application);
        //TODO update repository
        //TODO update application.id
        audit(ActionPerformed.ADDED, "application", application.toString());
        return application;
    }

    private void audit(String action, String what, String value) {
        String now = sdf.format(new Date());
        ActionPerformed actionPerformed = new ActionPerformed(value, now, action, what, value);
        auditLogRepository.store(actionPerformed);
    }


}
