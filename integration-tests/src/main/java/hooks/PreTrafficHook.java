package hooks;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codedeploy.CodeDeployClient;
import software.amazon.awssdk.services.codedeploy.model.LifecycleEventStatus;
import software.amazon.awssdk.services.codedeploy.model.PutLifecycleEventHookExecutionStatusRequest;
import software.amazon.awssdk.services.codedeploy.model.PutLifecycleEventHookExecutionStatusResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

@Slf4j
public class PreTrafficHook implements RequestHandler<Map<String, String>, Void> {

  ObjectMapper objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
  private final SummaryGeneratingListener listener = new SummaryGeneratingListener();

  @SneakyThrows
  @Override
  public Void handleRequest(Map<String, String> request, Context context) {
    log.info("request: {}", objectMapper.writeValueAsString(request));

    TestExecutionSummary summary = launchTests();
    printExecutionSummary(summary);
    notifyCodeDeployWithTestsStatus(request, summary);

    return null;
  }

  private void notifyCodeDeployWithTestsStatus(Map<String, String> request, TestExecutionSummary summary) {
    LifecycleEventStatus lifecycleEventStatus = computeDeployStatus(summary);
    String lifecycleEventHookExecutionId = request.get("LifecycleEventHookExecutionId");
    String deploymentId = request.get("DeploymentId");
    doNotifyCodeDeployWithTestsStatus(lifecycleEventStatus, lifecycleEventHookExecutionId, deploymentId);
  }

  private void doNotifyCodeDeployWithTestsStatus(LifecycleEventStatus lifecycleEventStatus, String lifecycleEventHookExecutionId, String deploymentId) {
    try (CodeDeployClient codeDeployClient = CodeDeployClient.builder().region(Region.EU_WEST_3).build()) {
      PutLifecycleEventHookExecutionStatusRequest statusRequest = PutLifecycleEventHookExecutionStatusRequest.builder()
          .deploymentId(deploymentId)
          .lifecycleEventHookExecutionId(lifecycleEventHookExecutionId)
          .status(lifecycleEventStatus)
          .build();
      PutLifecycleEventHookExecutionStatusResponse putLifecycleEventHookExecutionStatusResponse = codeDeployClient.putLifecycleEventHookExecutionStatus(statusRequest);
      log.info("putLifecycleEventHookExecutionStatusResponse {}", putLifecycleEventHookExecutionStatusResponse);
    }
  }

  private LifecycleEventStatus computeDeployStatus(TestExecutionSummary summary) {
    return summary.getFailures().isEmpty() ? LifecycleEventStatus.SUCCEEDED : LifecycleEventStatus.FAILED;
  }

  private void printExecutionSummary(TestExecutionSummary summary) {
    StringWriter stringWriter = new StringWriter();
    summary.printTo(new PrintWriter(stringWriter));
    log.info(stringWriter.toString());
  }

  private TestExecutionSummary launchTests() {
    LauncherDiscoveryRequest launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
        .selectors(selectPackage("example"))
        .build();
    Launcher launcher = LauncherFactory.create();
    launcher.discover(launcherDiscoveryRequest);
    launcher.registerTestExecutionListeners(listener);
    launcher.execute(launcherDiscoveryRequest);
    return listener.getSummary();
  }
}
