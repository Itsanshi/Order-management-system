package com.restaurantback.handlers.employee;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.restaurantback.services.EmployeeService;
import org.json.JSONObject;

public class PostEmployeeHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final EmployeeService employeeService;

    public PostEmployeeHandler(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {

        try {
            context.getLogger().log(request.getHttpMethod() + " " + request.getResource());
            employeeService.addEmployee(request);
            context.getLogger().log("employee added successfully");

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(new JSONObject().put("message", "Employee has been added successfully").toString());
        } catch (RuntimeException e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withBody(new JSONObject().put("message", e.getMessage()).toString());
        } catch (Exception e) {
            context.getLogger().log(e.toString());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody(new JSONObject().put("message", "Employee Not created. There is some internal error.").toString());
        }
    }
}