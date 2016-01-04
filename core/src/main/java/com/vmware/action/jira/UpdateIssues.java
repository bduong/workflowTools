package com.vmware.action.jira;

import com.vmware.action.base.AbstractBatchJiraAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.jira.domain.Issue;
import com.vmware.http.exception.NotFoundException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.List;

@ActionDescription("Bulk updates Jira issues.")
public class UpdateIssues extends AbstractBatchJiraAction {

    public UpdateIssues(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public boolean canRunAction() throws IOException, URISyntaxException, IllegalAccessException {
        if (projectIssues.getIssuesFromJira().isEmpty()) {
            log.info("Skipping {} as there are no issues loaded from Jira.", this.getClass().getSimpleName());
            return false;
        }
        return true;
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        List<Issue> issuesFromJira = projectIssues.getIssuesFromJira();
        log.info("Updating {} issues", issuesFromJira.size());

        for (Issue issueToUpdate : issuesFromJira) {
            try {
                jira.updateIssue(issueToUpdate);
                log.debug("Updated issue {}", issueToUpdate.getKey());
            } catch (NotFoundException e) {
                // ignore if the issue does not exist anymore in JIRA
                log.info("Ignoring missing issue '{}'", issueToUpdate.getKey());
            }
        }
    }

}
