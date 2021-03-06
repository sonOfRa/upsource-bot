package im.tox.upsourcebot.resources;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import im.tox.upsourcebot.client.GitHubConnector;
import im.tox.upsourcebot.core.payloads.IssueWebhook;
import im.tox.upsourcebot.core.payloads.PullRequestWebhook;
import im.tox.upsourcebot.filters.GitHubHMAC;

@Path("/github-webhook/{upsource-name}")
@Consumes(MediaType.APPLICATION_JSON)
@GitHubHMAC
public class GitHubWebhookResource {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitHubWebhookResource.class);

  private GitHubConnector gitHubConnector;

  public GitHubWebhookResource(GitHubConnector gitHubConnector) {
    this.gitHubConnector = gitHubConnector;
  }

  @POST
  @Path("/issue")
  public Response receiveHook(IssueWebhook payload,
      @PathParam("upsource-name") String upsourceName) {
    switch (payload.getAction()) {
      case "opened":
      case "assigned":
      case "unassigned":
      case "labeled":
      case "unlabeled":
      case "closed":
      case "reopened":
        break;
      default:
        LOGGER.error("GitHub Issue payload format changed");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
    return Response.accepted().build();
  }

  @POST
  @Path("/pull-request")
  public Response receiveHook(PullRequestWebhook payload,
      @PathParam("upsource-name") String upsourceName) {
    switch (payload.getAction()) {
      case "opened":
        // Handle creation
        // Fall through to synchronize
      case "synchronize":
        new Thread(() -> {
          String repoName = payload.getRepository().getFullName();
          String commitSHA = payload.getPullRequest().getHead().getSha();
          String url = "http://example.com/" + upsourceName;
          String description = "Code review is pending";
          String context = "review";
          gitHubConnector.setPendingCommitStatus(repoName, commitSHA, url, description, context);
        }).start();
        break;
      case "assigned":
      case "unassigned":
      case "labeled":
      case "unlabeled":
      case "closed":
      case "reopened":
        break;
      default:
        LOGGER.error("GitHub Pull request payload format changed");
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
    return Response.accepted().build();
  }

}
