package example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class HelloHandler implements RequestHandler<Object,HelloResponse> {
  @Override
  public HelloResponse handleRequest(Object helloRequest, Context context) {
    return new HelloResponse("hello from my-app-autonome - v3");
  }
}
