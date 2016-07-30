package com.vmware.action.commitInfo;

import com.vmware.action.base.BaseSetReviewerList;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;

@ActionDescription("Sets the reviewed by section. Reads reviewers from reviewboard, review groups can also be configured by setting the property reviewerGroups.")
public class SetReviewedBy extends BaseSetReviewerList {

    public SetReviewedBy(WorkflowConfig config) {
        super(config, true, false);
    }
}
