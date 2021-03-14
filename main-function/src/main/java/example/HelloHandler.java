package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class HelloHandler implements RequestHandler<Object,HelloResponse> {

  public final static String RESPONSE = "hello from my-app-autonome - v15";

  @Override
  public HelloResponse handleRequest(Object helloRequest, Context context) {
    return new HelloResponse(RESPONSE);
  }
}
