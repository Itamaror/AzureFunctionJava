package FuncTest.TestFunc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.google.api.services.dialogflow.v2.model.GoogleCloudDialogflowV2IntentMessage;
import com.google.api.services.dialogflow.v2.model.GoogleCloudDialogflowV2IntentMessageSimpleResponse;
import com.google.api.services.dialogflow.v2.model.GoogleCloudDialogflowV2IntentMessageSimpleResponses;
import com.google.api.services.dialogflow.v2.model.GoogleCloudDialogflowV2WebhookResponse;
import com.microsoft.azure.functions.*;
import org.json.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
	@SuppressWarnings("static-method")
	@FunctionName("CoolFunc")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> s,
            final ExecutionContext c) {
        c.getLogger().info("=========== GET BUSINESS HOURS BY DAY ===========");
        //===================================
        // HANDLE HTTP PARAMETERS
        JSONObject obj = new JSONObject(s.getBody().get().toString());
        String bname = "";
        String day = "";
        if (obj.has("Business"))
        	bname = obj.getString("Business");
        else
        	return createWebhookResponseContent("Please choose a business to look into\n", s);
        if(obj.has("DayOfWeek"))
        	day = obj.getString("DayOfWeek");
        else
        	return createWebhookResponseContent("Please specify a day\n", s);
        
        //===================================
        // check day valid
        String [] days_of_week = new String[]{"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Today","Tomorrow","Now"};
        day = day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase();
        if(!Arrays.asList(days_of_week).contains(day))
        	return createWebhookResponseContent("What is this day? I did not understand\n", s);
        
        //===================================
        // handle special days
        if("Now".equals(day) || "Today".equals(day)) // question regarding this day
        {
        	  Calendar cal = Calendar.getInstance();
        	  cal.setTime(new Date());
        	  cal.add(Calendar.HOUR_OF_DAY, 2);
        	  day = fromIntToDay(cal.get(Calendar.DAY_OF_WEEK));
        	  c.getLogger().info(day);
        }
        else
            if("Tomorrow".equals(day))// question regarding the next day
            {
          	  Calendar cal = Calendar.getInstance();
        	  cal.setTime(new Date());
        	  cal.add(Calendar.HOUR_OF_DAY, 2);
        	  cal.add(Calendar.DATE, 1);
        	  day = fromIntToDay(cal.get(Calendar.DAY_OF_WEEK));
        	  c.getLogger().info(day);
            }
        
        String url = String.format("jdbc:sqlserver://%s:1433;database=%s;user=%s;password=%s;encrypt=true;"
            + "hostNameInCertificate=*.database.windows.net;loginTimeout=30;", "technobotserver.database.windows.net", "technobot", "techazure2@technobotserver", "TechnionBot1234");
        Connection connection = null;
        StringBuilder jsonResult = new StringBuilder();
        try {
            connection = DriverManager.getConnection(url);
             // Create and execute a SELECT SQL statement.
            String selectSql = "SELECT BusinessName, " + day + " FROM BUSINESSES WHERE CHARINDEX('" + bname + "', BusinessName) != 0";
             try (Statement statement = connection.createStatement();
            		 ResultSet resultSet = statement.executeQuery(selectSql))
             {
            	if (resultSet.isBeforeFirst() ) // we have values to read
	                while (resultSet.next()) 
						if (!"N\\A".equals(resultSet.getString(2)))
							jsonResult.append("The "+resultSet.getString(1)+" is open between "+resultSet.getString(2)+" on "+day+"s");
						else
							jsonResult.append(resultSet.getString(1)+" is not open on "+day+"s");
                else
                	jsonResult.append("No such business found. If this is an error please contact technionbot1@gmail.com");
                connection.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return createWebhookResponseContent(jsonResult.toString(), s);
    }


    private static String fromIntToDay(int i) {
    	String [] days_of_week = new String[]{"zeroDay","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
    	return days_of_week[i];
    }
    
    public static HttpResponseMessage createWebhookResponseContent(String resultText, HttpRequestMessage<Optional<String>> s){
//    	// create simple response
//    	GoogleCloudDialogflowV2IntentMessageSimpleResponse sr = new GoogleCloudDialogflowV2IntentMessageSimpleResponse();
//    	sr.setDisplayText("display text");
//    	sr.setTextToSpeech("text to speech");
//    	// create list of simple response
//    	List<GoogleCloudDialogflowV2IntentMessageSimpleResponse> sr_list = new ArrayList<>();
//    	sr_list.add(sr);
//    	// set simple_responses
//    	GoogleCloudDialogflowV2IntentMessageSimpleResponses sr1 = new GoogleCloudDialogflowV2IntentMessageSimpleResponses();
//    	sr1.setSimpleResponses(sr_list);
//    	// set intent msg
//    	GoogleCloudDialogflowV2IntentMessage intentmsg = new GoogleCloudDialogflowV2IntentMessage();
//    	intentmsg.setSimpleResponses(sr1);
//    	// set list of intent msgs
//    	List<GoogleCloudDialogflowV2IntentMessage> intent_list = new ArrayList<>();
//    	intent_list.add(intentmsg);
//    	// set up the response
//    	GoogleCloudDialogflowV2WebhookResponse response = new GoogleCloudDialogflowV2WebhookResponse();
//    	response.setFulfillmentMessages(intent_list);
//    	response.setFulfillmentText(resultText);
//    	Map<String,Object> my_map = new HashMap<String, Object>();
//    	my_map.put("expectUserResponse", Boolean.TRUE);
//    	response.setPayload(my_map);
//    	
//    	return s.createResponseBuilder(HttpStatus.OK).body(response.toString()).header("Content-Type", "application/json").build();
    	return s.createResponseBuilder(HttpStatus.OK)
				.body(new JSONObject().put("fulfillmentText", resultText)
						.put("fulfillmentMessages",
								new JSONArray().put(new JSONObject().put("simpleResponses",
										new JSONObject().put("simpleResponses",
												new JSONArray().put(new JSONObject().put("displayText", "display text")
														.put("textToSpeech", "display text"))))))
						.put("payload",
								new JSONObject().put("google", new JSONObject().put("expectUserResponse", Boolean.TRUE)))
						.toString()).header("Content-Type", "application/json")
				.build();
    }
}