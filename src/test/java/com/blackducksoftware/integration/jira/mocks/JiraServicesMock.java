package com.blackducksoftware.integration.jira.mocks;

import java.util.Collection;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.avatar.AvatarManager;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.properties.IssuePropertyService;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.entity.property.JsonEntityPropertyManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.WorkflowSchemeManager;
import com.blackducksoftware.integration.jira.task.issue.JiraServices;

public class JiraServicesMock extends JiraServices {

	private ConstantsManager constantsManager;
	private ProjectManager projectManager;
	private AvatarManager avatarManager;
	private IssueService issueService;
	private JiraAuthenticationContext jiraAuthenticationContext;
	private IssuePropertyService issuePropertyService;
	private WorkflowManager workflowManager;
	private WorkflowSchemeManager workflowSchemeManager;
	private JsonEntityPropertyManager jsonEntityPropertyManager;
	private CommentManager commentManager;
	private GroupManager groupManager;
	private UserManager userManager;
	private Collection<IssueType> issueTypes;
	private UserUtil userUtil;

	@Override
	public UserUtil getUserUtil() {
		return userUtil;
	}

	public void setUserUtil(final UserUtil userUtil) {
		this.userUtil = userUtil;
	}

	@Override
	public Collection<IssueType> getIssueTypes() {
		return issueTypes;
	}

	public void setIssueTypes(final Collection<IssueType> issueTypes) {
		this.issueTypes = issueTypes;
	}

	public void setProjectManager(final ProjectManager projectManager) {
		this.projectManager = projectManager;
	}

	public void setIssueService(final IssueService issueService) {
		this.issueService = issueService;
	}

	public void setJiraAuthenticationContext(final JiraAuthenticationContext jiraAuthenticationContext) {
		this.jiraAuthenticationContext = jiraAuthenticationContext;
	}

	public void setIssuePropertyService(final IssuePropertyService issuePropertyService) {
		this.issuePropertyService = issuePropertyService;
	}

	public void setWorkflowManager(final WorkflowManager workflowManager) {
		this.workflowManager = workflowManager;
	}

	public void setWorkflowSchemeManager(final WorkflowSchemeManager workflowSchemeManager) {
		this.workflowSchemeManager = workflowSchemeManager;
	}

	public void setJsonEntityPropertyManager(final JsonEntityPropertyManager jsonEntityPropertyManager) {
		this.jsonEntityPropertyManager = jsonEntityPropertyManager;
	}

	public void setCommentManager(final CommentManager commentManager) {
		this.commentManager = commentManager;
	}

	public void setGroupManager(final GroupManager groupManager) {
		this.groupManager = groupManager;
	}

	public void setUserManager(final UserManager userManager) {
		this.userManager = userManager;
	}

	public void setAvatarManager(final AvatarManager avatarManager) {
		this.avatarManager = avatarManager;
	}

	@Override
	public ConstantsManager getConstantsManager() {
		return constantsManager;
	}

	public void setConstantsManager(final ConstantsManager constantsManager) {
		this.constantsManager = constantsManager;
	}

	@Override
	public ProjectManager getJiraProjectManager() {
		return projectManager;
	}

	@Override
	public AvatarManager getAvatarManager() {
		return avatarManager;
	}

	@Override
	public IssueService getIssueService() {
		return issueService;
	}

	@Override
	public JiraAuthenticationContext getAuthContext() {
		return jiraAuthenticationContext;
	}

	@Override
	public IssuePropertyService getPropertyService() {
		return issuePropertyService;
	}

	@Override
	public WorkflowManager getWorkflowManager() {
		return workflowManager;
	}

	@Override
	public WorkflowSchemeManager getWorkflowSchemeManager() {
		return workflowSchemeManager;
	}

	@Override
	public JsonEntityPropertyManager getJsonEntityPropertyManager() {
		return jsonEntityPropertyManager;
	}

	@Override
	public CommentManager getCommentManager() {
		return commentManager;
	}

	@Override
	public GroupManager getGroupManager() {
		return groupManager;
	}

	@Override
	public UserManager getUserManager() {
		return userManager;
	}

	@Override
	public ApplicationUser userToApplicationUser(final User user) {
		final ApplicationUserMock userMock = new ApplicationUserMock();
		userMock.setName(user.getName());
		return userMock;
	}

}
