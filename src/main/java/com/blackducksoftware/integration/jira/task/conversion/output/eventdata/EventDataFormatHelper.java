/**
 * Black Duck JIRA Plugin
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.jira.task.conversion.output.eventdata;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.blackducksoftware.integration.jira.common.BlackDuckJiraConstants;
import com.blackducksoftware.integration.jira.common.BlackDuckJiraLogger;
import com.synopsys.integration.blackduck.api.core.HubResponse;
import com.synopsys.integration.blackduck.api.core.LinkSingleResponse;
import com.synopsys.integration.blackduck.api.generated.component.RemediatingVersionView;
import com.synopsys.integration.blackduck.api.generated.component.RemediationOptionsView;
import com.synopsys.integration.blackduck.api.generated.component.VersionBomLicenseView;
import com.synopsys.integration.blackduck.api.generated.enumeration.ComplexLicenseType;
import com.synopsys.integration.blackduck.api.generated.response.VersionRiskProfileView;
import com.synopsys.integration.blackduck.api.generated.view.ComplexLicenseView;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.LicenseView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.notification.content.VulnerabilityNotificationContent;
import com.synopsys.integration.blackduck.notification.content.VulnerabilitySourceQualifiedId;
import com.synopsys.integration.blackduck.service.ComponentService;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.bucket.HubBucket;
import com.synopsys.integration.exception.IntegrationException;

public class EventDataFormatHelper {
    private final BlackDuckJiraLogger logger = new BlackDuckJiraLogger(Logger.getLogger(this.getClass().getName()));
    private final HubService blackDuckService;

    public EventDataFormatHelper(final HubService blackDuckService) {
        this.blackDuckService = blackDuckService;
    }

    public String getIssueDescription(final EventCategory eventCategory, final String projectVersionUrl, final String componentVersionUrl, final HubBucket blackDuckBucket) {
        final StringBuilder issueDescription = new StringBuilder();

        issueDescription.append("Black Duck has detected ");
        if (EventCategory.POLICY.equals(eventCategory)) {
            issueDescription.append("a policy violation.  \n\n");
        } else if (EventCategory.VULNERABILITY.equals(eventCategory)) {
            issueDescription.append("vulnerabilities. For details, see the comments below, or the project's ");
            String vulnerableComponentsLink = null;
            final ProjectVersionView projectVersion = getView(projectVersionUrl, ProjectVersionView.class, blackDuckBucket);
            if (projectVersion != null) {
                vulnerableComponentsLink = blackDuckService.getFirstLinkSafely(projectVersion, ProjectVersionView.VULNERABLE_COMPONENTS_LINK);
            }
            if (vulnerableComponentsLink != null) {
                issueDescription.append("[vulnerabilities|");
                issueDescription.append(vulnerableComponentsLink);
                issueDescription.append("]");
            } else {
                issueDescription.append("vulnerabilities");
            }
            issueDescription.append(" in Black Duck.  \n\n");
        }

        if (componentVersionUrl != null) {
            final ComponentVersionView componentVersion = getView(componentVersionUrl, ComponentVersionView.class, blackDuckBucket);
            final String licenseText = getComponentLicensesStringWithLinksAtlassianFormat(componentVersion);
            if (StringUtils.isNotBlank(licenseText)) {
                issueDescription.append("KB Component license(s): ");
                issueDescription.append(licenseText);
            }
            if (EventCategory.VULNERABILITY.equals(eventCategory)) {
                appendRemediationOptionsText(issueDescription, componentVersion);
            }
        }
        return issueDescription.toString();
    }

    public String createIssueSummary(final EventCategory eventCategory, final String projectName, final String projectVersionName, final String componentName, final String componentVersionName, final String ruleName) {
        if (EventCategory.POLICY.equals(eventCategory)) {
            final String issueSummaryTemplate = "%s: Project '%s' / '%s', Component '%s' [Rule: '%s']";
            return String.format(issueSummaryTemplate, BlackDuckJiraConstants.BLACKDUCK_POLICY_VIOLATION_ISSUE, projectName, projectVersionName, getComponentString(componentName, componentVersionName), ruleName);
        } else if (EventCategory.VULNERABILITY.equals(eventCategory)) {
            final StringBuilder issueSummary = new StringBuilder();
            issueSummary.append(BlackDuckJiraConstants.BLACKDUCK_VULNERABILITY_ISSUE);
            issueSummary.append(": Project '");
            issueSummary.append(projectName);
            issueSummary.append("' / '");
            issueSummary.append(projectVersionName);
            issueSummary.append("', Component '");
            issueSummary.append(componentName);
            issueSummary.append("' / '");
            issueSummary.append(componentVersionName != null ? componentVersionName : "?");
            issueSummary.append("'");
            return issueSummary.toString();
        } else {
            return null;
        }
    }

    public String generateVulnerabilitiesComment(final VulnerabilityNotificationContent vulnerabilityContent) {
        return generateVulnerabilitiesComment(vulnerabilityContent.newVulnerabilityIds, vulnerabilityContent.updatedVulnerabilityIds, vulnerabilityContent.deletedVulnerabilityIds);
    }

    public String generateVulnerabilitiesComment(final List<VulnerabilitySourceQualifiedId> addedIds, final List<VulnerabilitySourceQualifiedId> updatedIds, final List<VulnerabilitySourceQualifiedId> deletedIds) {
        final StringBuilder commentText = new StringBuilder();
        commentText.append("(Black Duck plugin auto-generated comment)\n");
        appendVulnerabilitiesCommentText(commentText, addedIds, "added");
        appendVulnerabilitiesCommentText(commentText, updatedIds, "updated");
        appendVulnerabilitiesCommentText(commentText, deletedIds, "deleted");
        return commentText.toString();
    }

    private void appendRemediationOptionsText(final StringBuilder stringBuilder, final ComponentVersionView componentVersionView) {
        // TODO use the HubService once the Black Duck APIs have the link.
        final ComponentService componentService = new ComponentService(blackDuckService, logger);
        RemediationOptionsView remediationOptions;
        try {
            remediationOptions = componentService.getRemediationInformation(componentVersionView);
        } catch (final IntegrationException e) {
            logger.debug("Could not get remediation information: ");
            logger.debug(e.getMessage());
            return;
        }
        if (remediationOptions != null) {
            stringBuilder.append("\nRemediation Information:\n");
            if (remediationOptions.fixesPreviousVulnerabilities != null) {
                appendRemediationVersionText(stringBuilder, remediationOptions.fixesPreviousVulnerabilities, "fixes previous vulnerabilities");
            }
            if (remediationOptions.latestAfterCurrent != null) {
                appendRemediationVersionText(stringBuilder, remediationOptions.latestAfterCurrent, "is the most recent");
            }
            if (remediationOptions.noVulnerabilities != null) {
                appendRemediationVersionText(stringBuilder, remediationOptions.noVulnerabilities, "has no known vulnerabilities");
            }
        }
    }

    private void appendRemediationVersionText(final StringBuilder stringBuilder, final RemediatingVersionView remediatingVersionView, final String versionComment) {
        stringBuilder.append(" * Version [");
        stringBuilder.append(remediatingVersionView.name);
        stringBuilder.append("|");
        stringBuilder.append(remediatingVersionView.componentVersion);
        stringBuilder.append("] ");
        stringBuilder.append(versionComment);
        if (remediatingVersionView.vulnerabilityCount != null && remediatingVersionView.vulnerabilityCount > 0) {
            stringBuilder.append(". Vulnerability count: ");
            stringBuilder.append(remediatingVersionView.vulnerabilityCount);
        }
        stringBuilder.append(".\n");
    }

    private void appendVulnerabilitiesCommentText(final StringBuilder commentText, final List<VulnerabilitySourceQualifiedId> vulns, final String verb) {
        final boolean hasContent = vulns != null && !vulns.isEmpty();
        final String formattedVerb = hasContent ? "*" + verb + "*" : "_" + verb + "_";
        commentText.append("Vulnerabilities " + formattedVerb + ": ");
        int index = 0;
        if (hasContent) {
            for (final VulnerabilitySourceQualifiedId vuln : vulns) {
                commentText.append(vuln.vulnerabilityId + " (" + vuln.source + ")");
                if ((index + 1) < vulns.size()) {
                    commentText.append(", ");
                }
                index++;
            }
        } else {
            commentText.append("None");
        }
        commentText.append("\n");
    }

    public String getBomLastUpdated(final ProjectVersionView projectVersion) {
        try {
            final VersionRiskProfileView riskProfile = blackDuckService.getResponse(projectVersion, ProjectVersionView.RISKPROFILE_LINK_RESPONSE);
            final SimpleDateFormat dateFormat = new SimpleDateFormat();
            return dateFormat.format(riskProfile.bomLastUpdatedAt);
        } catch (final IntegrationException intException) {
            logger.debug(String.format("Could not find the risk profile: %s", intException.getMessage()));
        } catch (final NullPointerException npe) {
            logger.debug("The risk profile for the requested project was null.");
            logger.trace("Caught NPE in getBomLastUpdated()", npe);
        }
        return "";
    }

    public String getComponentLicensesStringPlainText(final List<VersionBomLicenseView> licenses) {
        if (CollectionUtils.isNotEmpty(licenses)) {
            EventDataLicense license;
            if (licenses.size() == 1) {
                license = new EventDataLicense(licenses.get(0));
            } else {
                license = new EventDataLicense(licenses);
            }
            return getComponentLicensesString(license, false);
        }
        return "";
    }

    public String getComponentLicensesStringWithLinksAtlassianFormat(final List<VersionBomLicenseView> licenses) {
        if (CollectionUtils.isNotEmpty(licenses)) {
            EventDataLicense license;
            if (licenses.size() == 1) {
                license = new EventDataLicense(licenses.get(0));
            } else {
                license = new EventDataLicense(licenses);
            }
            return getComponentLicensesString(license, true);
        }
        return "";
    }

    public String getComponentLicensesStringPlainText(final ComponentVersionView componentVersion) {
        if (componentVersion != null) {
            final EventDataLicense license = new EventDataLicense(componentVersion.license);
            return getComponentLicensesString(license, false);
        }
        return "";
    }

    public String getComponentLicensesStringWithLinksAtlassianFormat(final ComponentVersionView componentVersion) {
        if (componentVersion != null) {
            final EventDataLicense license = new EventDataLicense(componentVersion.license);
            return getComponentLicensesString(license, true);
        }
        return "";
    }

    public String getLicenseTextLink(final List<VersionBomLicenseView> licenses, final String licenseName) {
        if (CollectionUtils.isNotEmpty(licenses)) {
            VersionBomLicenseView versionBomLicense = licenses.get(0);
            for (final VersionBomLicenseView license : licenses) {
                if (licenseName.equals(license.licenseDisplay)) {
                    versionBomLicense = license;
                }
            }
            try {
                final LicenseView genericLicense = blackDuckService.getResponse(versionBomLicense.license, LicenseView.class);
                final LicenseView kbLicense = blackDuckService.getResponse(genericLicense, new LinkSingleResponse<>("license", LicenseView.class));
                return blackDuckService.getFirstLink(kbLicense, LicenseView.TEXT_LINK);
            } catch (final Exception e) {
                logger.debug("Unable to get the BOM component license text.");
            }
        }
        return "";
    }

    private String getComponentString(final String componentName, final String componentVersionName) {
        String componentString = "?";
        if (componentName != null) {
            componentString = componentName;
            if (componentVersionName != null) {
                componentString += "' / '" + componentVersionName;
            }
        }
        return componentString;
    }

    private String getComponentLicensesString(final EventDataLicense eventDataLicense, final boolean includeLinks) {
        String licensesString = "";
        if (eventDataLicense.isPopulated()) {
            final ComplexLicenseType type = eventDataLicense.licenseType;
            final StringBuilder sb = new StringBuilder();

            if (type != null) {
                final String licenseJoinString = (ComplexLicenseType.CONJUNCTIVE.equals(type)) ? BlackDuckJiraConstants.LICENSE_NAME_JOINER_AND : BlackDuckJiraConstants.LICENSE_NAME_JOINER_OR;
                int licenseIndex = 0;
                for (final EventDataLicense license : eventDataLicense.licenses) {
                    if (licenseIndex++ > 0) {
                        sb.append(licenseJoinString);
                    }
                    createLicenseString(sb, license, includeLinks);
                }

            } else {
                createLicenseString(sb, eventDataLicense, includeLinks);
            }
            licensesString = sb.toString();
        }
        return licensesString;
    }

    private void createLicenseString(final StringBuilder sb, final EventDataLicense license, final boolean includeLinks) {
        final String licenseTextUrl = getLicenseTextUrl(license);
        logger.debug("Link to license text: " + licenseTextUrl);

        if (includeLinks) {
            sb.append("[");
        }
        sb.append(license.licenseDisplay);
        if (includeLinks) {
            sb.append("|");
            sb.append(licenseTextUrl);
            sb.append("]");
        }
    }

    private String getLicenseTextUrl(final EventDataLicense license) {
        final String licenseUrl = license.licenseUrl;
        try {
            final ComplexLicenseView fullLicense = blackDuckService.getResponse(licenseUrl, ComplexLicenseView.class);
            return blackDuckService.getFirstLink(fullLicense, "text");
        } catch (final Exception e) {
            logger.debug("Error getting license text url.");
        }
        return blackDuckService.getHubBaseUrl().toString();
    }

    private <T extends HubResponse> T getView(final String uri, final Class<T> clazz, final HubBucket blackDuckBucket) {
        T view = blackDuckBucket.get(uri, clazz);
        if (view == null) {
            try {
                view = blackDuckService.getResponse(uri, clazz);
            } catch (final IntegrationException e) {
                logger.debug("Could not get view from Black Duck.", e);
            }
        }
        return view;
    }

    class EventDataLicense {
        public final String licenseUrl;
        public final String licenseDisplay;
        public final ComplexLicenseType licenseType;
        public final List<EventDataLicense> licenses;

        public EventDataLicense(final List<VersionBomLicenseView> licenses) {
            this.licenseUrl = null;
            this.licenseDisplay = "Multiple licenses";
            this.licenseType = ComplexLicenseType.CONJUNCTIVE;
            this.licenses = createLicenseListFromBom(licenses);
        }

        public EventDataLicense(final ComplexLicenseView licenseView) {
            this.licenseUrl = licenseView.license;
            this.licenseDisplay = licenseView.licenseDisplay;
            this.licenseType = licenseView.type;
            this.licenses = createLicenseListFromComplex(licenseView.licenses);
        }

        public EventDataLicense(final VersionBomLicenseView licenseView) {
            this.licenseUrl = licenseView.license;
            this.licenseDisplay = licenseView.licenseDisplay;
            this.licenseType = licenseView.licenseType;
            this.licenses = createLicenseListFromBom(licenseView.licenses);
        }

        public List<EventDataLicense> createLicenseListFromComplex(final List<ComplexLicenseView> licenses) {
            final List<EventDataLicense> eventDataLicenses = new ArrayList<>();
            if (licenses != null) {
                for (final ComplexLicenseView license : licenses) {
                    eventDataLicenses.add(new EventDataLicense(license));
                }
            }
            return eventDataLicenses;
        }

        public List<EventDataLicense> createLicenseListFromBom(final List<VersionBomLicenseView> licenses) {
            final List<EventDataLicense> eventDataLicenses = new ArrayList<>();
            if (licenses != null) {
                for (final VersionBomLicenseView license : licenses) {
                    eventDataLicenses.add(new EventDataLicense(license));
                }
            }
            return eventDataLicenses;
        }

        public boolean isPopulated() {
            return licenseDisplay != null && (licenseUrl != null || licenseType != null);
        }

    }

}
