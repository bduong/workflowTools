package com.vmware.action.batch;

import com.vmware.AbstractService;
import com.vmware.action.BaseAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.Gitlab;
import com.vmware.jenkins.Jenkins;
import com.vmware.jira.Jira;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.trello.Trello;
import com.vmware.util.StringUtils;
import com.vmware.vcd.Vcd;

@ActionDescription("Ensures that all apis have a valid token / cookie. Primarily for testing.")
public class AuthenticateAllApis extends BaseAction {

    public AuthenticateAllApis(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        checkAuthentication(new Gitlab(gitlabConfig.gitlabUrl, config.username));
        String ssoEmail = StringUtils.isNotBlank(ssoConfig.ssoEmail) ? ssoConfig.ssoEmail : git.configValue("user.email");
        checkAuthentication(new Vcd(vcdConfig.vcdUrl, vcdConfig.vcdApiVersion, vcdConfig.vcdApiVersion, vcdConfig.defaultVcdOrg, vcdConfig.vcdSso,
                ssoEmail, vcdConfig.refreshTokenName, vcdConfig.disableVcdRefreshToken, ssoConfig.ssoHeadless, ssoConfig));

        checkAuthentication(new ReviewBoard(reviewBoardConfig.reviewboardUrl, config.username));
        checkAuthentication(new Bugzilla(bugzillaConfig.bugzillaUrl, config.username, bugzillaConfig.bugzillaTestBug));
        checkAuthentication(new Jira(jiraConfig.jiraUrl, config.username, jiraConfig.jiraCustomFieldNames));
        checkAuthentication(new Jenkins(jenkinsConfig.jenkinsUrl, config.username, jenkinsConfig.jenkinsUsesCsrf, jenkinsConfig.disableJenkinsLogin, jenkinsConfig.testReportsUrlOverrides));
        checkAuthentication(new Trello(trelloConfig.trelloUrl, config.username, trelloConfig.trelloSso, ssoEmail, ssoConfig));
    }

    private void checkAuthentication(AbstractService restService) {
        if (StringUtils.isEmpty(restService.baseUrl)) {
            log.info("Skipping check of service {} as the base url is blank", restService.getClass().getSimpleName());
        }
        String serviceName = restService.getClass().getSimpleName();
        log.info("Checking authentication for service {}", serviceName);
        restService.setupAuthenticatedConnection();
        log.info("Finished checking authentication for service {}", serviceName);
    }
}
