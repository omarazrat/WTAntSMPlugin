/**
 * Resizes the iframe every time the user selects another issue in
 * the issue navigator.
 */
window.JIRA.bind(window.JIRA.Events.ISSUE_REFRESHED, function() {
    window.iFrameResize();
});