package com.vmware.reviewboard;

import com.vmware.AbstractRestService;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.NotFoundException;
import com.vmware.http.request.RequestParam;
import com.vmware.http.request.UrlParam;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.reviewboard.domain.GeneralCommentsResponse;
import com.vmware.reviewboard.domain.ReviewComment;
import com.vmware.reviewboard.domain.DiffCommentsResponse;
import com.vmware.reviewboard.domain.DiffToUpload;
import com.vmware.reviewboard.domain.Link;
import com.vmware.reviewboard.domain.Repository;
import com.vmware.reviewboard.domain.RepositoryResponse;
import com.vmware.reviewboard.domain.ResultsCount;
import com.vmware.reviewboard.domain.ReviewRequest;
import com.vmware.reviewboard.domain.ReviewRequestDiff;
import com.vmware.reviewboard.domain.ReviewRequestDiffsResponse;
import com.vmware.reviewboard.domain.ReviewRequestDraft;
import com.vmware.reviewboard.domain.ReviewRequestDraftResponse;
import com.vmware.reviewboard.domain.ReviewRequestResponse;
import com.vmware.reviewboard.domain.ReviewRequestStatus;
import com.vmware.reviewboard.domain.ReviewRequests;
import com.vmware.reviewboard.domain.ReviewUser;
import com.vmware.reviewboard.domain.ReviewUsersResponse;
import com.vmware.reviewboard.domain.RootList;
import com.vmware.reviewboard.domain.ServerInfo;
import com.vmware.reviewboard.domain.ServerInfoResponse;
import com.vmware.reviewboard.domain.UserReview;
import com.vmware.reviewboard.domain.UserReviewsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.vmware.http.cookie.ApiAuthentication.reviewBoard;
import static com.vmware.http.request.RequestHeader.anAcceptHeader;
import static com.vmware.reviewboard.domain.ReviewRequestDraft.aDraftForPublishingAReview;
import static com.vmware.reviewboard.domain.ReviewRequestStatus.all;
import static com.vmware.reviewboard.domain.ReviewRequestStatus.pending;

public class ReviewBoard extends AbstractRestService {
    private ServerInfo cachedServerInfo = null;
    private RootList cachedRootList = null;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public ReviewBoard(String reviewboardUrl, String username) {
        super(reviewboardUrl, "api/", ApiAuthentication.reviewBoard, username);
        connection = new HttpConnection(RequestBodyHandling.AsUrlEncodedFormEntity);
    }

    public RootList getRootLinkList() {
        if (cachedRootList == null) {
            cachedRootList = connection.get(apiUrl, RootList.class);
        }
        return cachedRootList;
    }

    public ReviewRequests getReviewRequests(ReviewRequestStatus status) {
        Link reviewRequestLink = getRootLinkList().getReviewRequestsLink();
        return get(reviewRequestLink.getHref(), ReviewRequests.class,
                new UrlParam("from-user", getUsername()), new UrlParam("status", status.name()));
    }

    public ReviewRequest[] getOpenReviewRequestsWithSubmittedComment() {
        List<ReviewRequest> reviewRequestsWithSubmittedComments = new ArrayList<ReviewRequest>();
        for (ReviewRequest reviewRequest : getOpenReviewRequestsWithShipIts().review_requests) {
            UserReview softSubmitReview = getSoftSubmitReview(reviewRequest);
            if (softSubmitReview != null) {
                reviewRequestsWithSubmittedComments.add(reviewRequest);
            }
        }
        return reviewRequestsWithSubmittedComments.toArray(new ReviewRequest[reviewRequestsWithSubmittedComments.size()]);
    }

    public ReviewRequests getOpenReviewRequestsWithShipIts() {
        Link reviewRequestLink = getRootLinkList().getReviewRequestsLink();
        return get(reviewRequestLink.getHref(), ReviewRequests.class, new UrlParam("from-user", getUsername()),
                new UrlParam("status", pending.name()), new UrlParam("ship-it", "1"));
    }

    public List<ReviewUser> searchUsersMatchingText(Link usersLink, String textToMatch, boolean searchByUsernameOnly) {
        List<RequestParam> params = new ArrayList<>();
        params.add(new UrlParam("limit", "15"));
        params.add(new UrlParam("q", textToMatch));
        if (!searchByUsernameOnly) {
            params.add(new UrlParam("fullname", "1"));
        }
        return get(usersLink.getHref(), ReviewUsersResponse.class, params.toArray(new RequestParam[0])).users;
    }

    public int getFilesCountForReviewRequestDiff(Link filesLink) {
        return get(filesLink.getHref(), ResultsCount.class, new UrlParam("counts-only", "1")).count;
    }

    public ReviewRequests getReviewRequestsWithShipItsForGroups(String groupNames, Date fromDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(fromDate);
        Link reviewRequestLink = getRootLinkList().getReviewRequestsLink();
        return get(reviewRequestLink.getHref(), ReviewRequests.class,
                new UrlParam("to-groups", groupNames), new UrlParam("max-results", "200"),
                new UrlParam("time-added-from", formattedDate),
                new UrlParam("ship-it", "1"), new UrlParam("status", all.name()));
    }

    public ReviewRequest getReviewRequestById(Integer id) {
        if (id == null) {
            return null;
        }
        Link reviewRequestLink = getRootLinkList().getReviewRequestsLink();
        reviewRequestLink.addPathParam(String.valueOf(id));
        return get(reviewRequestLink.getHref(), ReviewRequestResponse.class).review_request;
    }

    public ReviewRequest createReviewRequest(String repository) {
        Link createLink = getReviewRequests(pending).getCreateLink();

        ReviewRequest initialReviewRequest = new ReviewRequest();
        initialReviewRequest.repository = repository;

        return connection.post(createLink.getHref(), ReviewRequestResponse.class, initialReviewRequest).review_request;
    }

    public ReviewRequest createReviewRequestFromDraft(ReviewRequestDraft reviewRequestDraft, String repository) {
        ReviewRequest addedRequest = createReviewRequest(repository);
        updateReviewRequestDraft(addedRequest.getDraftLink(), reviewRequestDraft);
        return getReviewRequestById(addedRequest.id);
    }

    public ReviewRequestDraft getReviewRequestDraft(Link draftLink) {
        return get(draftLink.getHref(), ReviewRequestDraftResponse.class).draft;
    }

    public ReviewRequestDraft getReviewRequestDraftWithExceptionHandling(Link draftLink) {
        try {
            return connection.get(draftLink.getHref(), ReviewRequestDraftResponse.class).draft;
        } catch (NotFoundException nfe) {
            log.debug("No draft found for link {}: {}", draftLink, nfe.getMessage());
            return null;
        }
    }

    public ReviewRequestDraft updateReviewRequestDraft(Link draftLink, ReviewRequestDraft draft) {
        String existingTestingDone = draft.testingDone;
        draft.testingDone = draft.fullTestingDoneSectionWithoutJobResults();

        ReviewRequestDraft updatedDraft = connection.put(draftLink.getHref(), ReviewRequestDraftResponse.class, draft).draft;
        draft.testingDone = existingTestingDone;
        return updatedDraft;
    }

    public void publishReview(Link draftLink, String changeDescription, boolean trivial) {
        connection.put(draftLink.getHref(), aDraftForPublishingAReview(changeDescription, trivial));
    }

    public void updateReviewRequest(ReviewRequest reviewRequest) {
        connection.put(reviewRequest.getUpdateLink().getHref(), ReviewRequestResponse.class, reviewRequest);
    }

    public void createReviewRequestDiff(Link diffLink, DiffToUpload diffToCreate) {
        post(diffLink.getHref(), diffToCreate);
    }

    public void createUserReview(ReviewRequest reviewRequest, UserReview review) {
        post(reviewRequest.getReviewsLink().getHref(), review);
    }

    public UserReview[] getReviewsForReviewRequest(Link reviewsLink) {
        return get(reviewsLink.getHref(), UserReviewsResponse.class).reviews;
    }

    public ReviewRequestDiff[] getDiffsForReviewRequest(Link diffsLink) {
        return get(diffsLink.getHref(), ReviewRequestDiffsResponse.class).diffs;
    }

    public ReviewComment[] getDiffCommentsForReview(Link diffCommentsLink) {
        ReviewComment[] comments = get(diffCommentsLink.getHref(), DiffCommentsResponse.class).diffComments;
        return comments != null ? comments : new ReviewComment[0];
    }

    public ReviewComment[] getGeneralCommentsForReview(Link commentsLink) {
        ReviewComment[] comments = get(commentsLink.getHref(), GeneralCommentsResponse.class).generalComments;
        return comments != null ? comments : new ReviewComment[0];
    }

    public Repository getRepository(Link repositoryLink) {
        return get(repositoryLink.getHref(), RepositoryResponse.class).repository;
    }

    public String getDiffData(Link diffLink) {
        String diffData = get(diffLink.getHref(), String.class, anAcceptHeader("text/x-patch"));
        // need to add in a trailing newline for git apply to work correctly
        diffData += "\n";
        return diffData;
    }

    public UserReview getSoftSubmitReview(ReviewRequest reviewRequest) {
        UserReview[] reviews = this.getReviewsForReviewRequest(reviewRequest.getReviewsLink());
        for (UserReview review : reviews) {
            if (review.getReviewUsername().equals(reviewRequest.getSubmitter())) {
                if (review.body_top.startsWith("Submitted as ")) {
                    return review;
                }
            }
        }
        return null;
    }

    public ServerInfo getServerInfo() {
        if (cachedServerInfo == null) {
            cachedServerInfo = connection.get(getRootLinkList().getInfoLink().getHref(), ServerInfoResponse.class).info;
        }
        return cachedServerInfo;
    }

    public boolean supportsDiffWithRenames() {
        return getVersion().compareTo("1.7") >= 0;
    }

    public boolean supportsDiffBaseCommitId() {
        return getVersion().compareTo("1.7.13") >= 0;
    }

    public void updateServerTimeZone(String serverDateFormat) {
        String serverTimeZone = getServerInfo().site.serverTimeZone;
        connection.updateTimezoneAndFormat(TimeZone.getTimeZone(serverTimeZone), serverDateFormat);
    }

    public void setupAuthenticatedConnectionWithLocalTimezone(String reviewBoardDateFormat) {
        if (isConnectionAuthenticated()) {
            return;
        }
        super.setupAuthenticatedConnection();
        updateServerTimeZone(reviewBoardDateFormat);
    }

    @Override
    protected void loginManually() {
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(reviewBoard, getUsername());
        connection.setupBasicAuthHeader(credentials);
    }

    @Override
    public void checkAuthenticationAgainstServer() {
        getServerInfo();
        if (!connection.hasCookie(reviewBoard)) {
            log.warn("Cookie {} should have been retrieved from reviewboard login!", reviewBoard.getCookieName());
        }
    }

    private String getVersion() {
        return getServerInfo().product.version;
    }

}
