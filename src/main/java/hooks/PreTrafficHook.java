package hooks;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.codedeploy.CodeDeployClient;
import software.amazon.awssdk.services.codedeploy.model.LifecycleEventStatus;
import software.amazon.awssdk.services.codedeploy.model.PutLifecycleEventHookExecutionStatusRequest;
import software.amazon.awssdk.services.codedeploy.model.PutLifecycleEventHookExecutionStatusResponse;

import java.util.Map;

@Slf4j
public class PreTrafficHook implements RequestHandler<Map<String, String>, Map<String, String>> {

  public final static String RESPONSE = "hello from my-app-autonome - v11";
  ObjectMapper objectMapper = new ObjectMapper().setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

  @SneakyThrows
  @Override
  public Map<String, String> handleRequest(Map<String, String> request, Context context) {
    log.info("request: {}", objectMapper.writeValueAsString(request));

    CodeDeployClient codeDeployClient = CodeDeployClient.builder().region(Region.EU_WEST_3).build();
    PutLifecycleEventHookExecutionStatusRequest statusRequest = PutLifecycleEventHookExecutionStatusRequest.builder()
        .deploymentId(request.get("DeploymentId"))
        .lifecycleEventHookExecutionId(request.get("LifecycleEventHookExecutionId"))
        .status(LifecycleEventStatus.SUCCEEDED)
        .build();
    PutLifecycleEventHookExecutionStatusResponse putLifecycleEventHookExecutionStatusResponse = codeDeployClient.putLifecycleEventHookExecutionStatus(statusRequest);
    log.info("putLifecycleEventHookExecutionStatusResponse {}", putLifecycleEventHookExecutionStatusResponse);
    return Map.of("response", "hook OK");
  }
}
