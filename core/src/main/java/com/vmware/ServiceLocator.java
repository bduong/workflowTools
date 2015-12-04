package com.vmware;

import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.WorkflowConfig;
import com.vmware.jenkins.Jenkins;
import com.vmware.jira.Jira;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.trello.Trello;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Centralized locator for jenkins, jira, reviewboard and trello instances.
 * Ensures that the above classes are singletons.
 */
public class ServiceLocator {

    private static Jira jira;

    private static Bugzilla bugzilla;

    private static ReviewBoard reviewBoard;

    private static Jenkins jenkins;

    private static Trello trello;

    public static Jira getJira(String jiraUrl, String testIssueKey, boolean setupAuthenticatedConnection) throws IllegalAccessException, IOException, URISyntaxException {
        if (jira == null) {
            jira = new Jira(jiraUrl, testIssueKey);
        }
        if (setupAuthenticatedConnection) {
            jira.setupAuthenticatedConnection();
        }
        return jira;
    }

    public static Bugzilla getBugzilla(String bugzillaUrl, String username, int testBugNumber, boolean setupAuthenticatedConnection) throws IllegalAccessException, IOException, URISyntaxException {
        if (bugzilla == null) {
            bugzilla = new Bugzilla(bugzillaUrl, username, testBugNumber);
        }
        if (setupAuthenticatedConnection) {
            bugzilla.setupAuthenticatedConnection();
        }
        return bugzilla;
    }

    public static ReviewBoard getReviewBoard(String reviewboardUrl, String username, String reviewBoardDateFormat) throws IOException, URISyntaxException, IllegalAccessException {
        if (reviewBoard == null) {
            reviewBoard = new ReviewBoard(reviewboardUrl, username);
            reviewBoard.setupAuthenticatedConnection();
            reviewBoard.updateServerTimeZone(reviewBoardDateFormat);
        }
        return reviewBoard;
    }

    public static Jenkins getJenkins(String jenkinsUrl, String username, boolean jenkinsUsesCsrf, boolean disableLogin) throws IOException, URISyntaxException, IllegalAccessException {
        if (jenkins == null) {
            jenkins = new Jenkins(jenkinsUrl, username, jenkinsUsesCsrf, disableLogin);
            jenkins.setupAuthenticatedConnection();
        }
        return jenkins;
    }

    public static Trello getTrello(String trelloUrl) throws IOException, URISyntaxException, IllegalAccessException {
        if (trello == null) {
            trello = new Trello(trelloUrl);
            trello.setupAuthenticatedConnection();
        }
        return trello;
    }

}
