package example;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

class SanityCheckIT {

  private final Region region = Region.EU_WEST_3;

  @Test
  void statusCodeIs200_andBodyHasCorrectVersion() {
    // given
    HelloResponse expectedResponse = new HelloResponse(HelloHandler.RESPONSE);

    // when
    HelloResponse actualResponse = invokeFunction();

    // then
    Assertions.assertThat(actualResponse)
        .usingRecursiveComparison()
        .isEqualTo(expectedResponse);
  }

  @SneakyThrows
  public HelloResponse invokeFunction() {
    String functionName = "my-app-autonome";
    try (LambdaClient awsLambda = LambdaClient.builder()
        .region(region)
        .build()) {

      //Setup an InvokeRequest
      InvokeRequest request = InvokeRequest.builder()
          .functionName(functionName)
          .payload(SdkBytes.fromUtf8String("{}"))
          .build();

      //Invoke the Lambda function
      String responseString = awsLambda.invoke(request).payload().asUtf8String();
      return new ObjectMapper().readValue(responseString, HelloResponse.class);
    }
  }
}
