package FuncTest.TestFunc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpTrigger-Java". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpTrigger-Java&code={your function key}
     * 2. curl "{your host}/api/HttpTrigger-Java?name=HTTP%20Query&code={your function key}"
     * Function Key is not needed when running locally, to invoke HttpTrigger deployed to Azure, see here(https://docs.microsoft.com/en-us/azure/azure-functions/functions-bindings-http-webhook#authorization-keys) on how to get function key for your app.
     */
    @FunctionName("CoolFunc")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        
     // Connect to database
        String hostName = "technobotserver.database.windows.net";
        String dbName = "technobot";
        String user = "techazure2@technobotserver";
        String password = "TechnionBot1234";
        String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", hostName, dbName, user, password);
        Connection connection = null;
        StringBuilder sb = new StringBuilder();
        try {
            connection = DriverManager.getConnection(url);
            String schema = connection.getSchema();
            System.out.println("Successful connection - Schema: " + schema);
             // Create and execute a SELECT SQL statement.
            String selectSql = "SELECT * FROM BUSINESSES FOR JSON PATH";
             try (Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(selectSql)) {
                while (resultSet.next())
                {
                    sb.append(resultSet.getString(1));
                }
                connection.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return request.createResponseBuilder(HttpStatus.OK).body(sb.toString()).build();
    }
}
