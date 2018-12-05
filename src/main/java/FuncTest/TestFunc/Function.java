package FuncTest.TestFunc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import org.json.*;
import FuncTest.TestFunc.globals;
import FuncTest.TestFunc.utils;

/**
 * Azure Functions with HTTP Trigger.
 */
@SuppressWarnings("static-method")
public class Function {
	@FunctionName("CoolFunc")
	public HttpResponseMessage run(
			@HttpTrigger(name = "req", methods = { HttpMethod.GET,
					HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION) HttpRequestMessage<Optional<String>> s,
			final ExecutionContext c) {
		c.getLogger().info("=========== WEBHOOK INVOKED ===========");
		JSONObject queryResult = new JSONObject(s.getBody().get().toString()).getJSONObject("queryResult");
		// ===================================
		// INVOKE RELEVANT INTENT_HANDLER
		switch (queryResult.getJSONObject("intent").getString("displayName")) {
		case globals.BUSINESS_HOUR_BY_DAY_INTENT_NAME:
			return getHourByDay(queryResult, s, c);
		case globals.BUSINESS_HOUR_WEEK_INTENT_NAME:
			return getHourByWeek(queryResult, s, c);
		}
		return utils.createWebhookResponseContent("what is this intent?", s);

	}

	private HttpResponseMessage getHourByWeek(JSONObject queryResult, HttpRequestMessage<Optional<String>> s,
			final ExecutionContext c) {
		c.getLogger().info("=========== GET HOUR BY WEEK ===========");
		// ===================================
		// get query parameters
		JSONObject parameters = queryResult.getJSONObject("parameters");
		String bname = "";
		if (parameters.has("Business"))
			bname = parameters.getString("Business");
		else
			return utils.createWebhookResponseContent(globals.MISSING_BUSINESS_PARAM, s);
		// ===================================
		Connection connection = null;
		StringBuilder jsonResult = new StringBuilder();
		try {
			connection = DriverManager.getConnection(globals.CONNECTION_STRING);
			// Create and execute a SELECT SQL statement.
			String selectSql = "SELECT Sunday,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday FROM Businesses"
					+ " WHERE CHARINDEX('" + bname + "',BusinessName) != 0";
			try (Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery(selectSql)) {
				if (!resultSet.isBeforeFirst())
					jsonResult.append(globals.NO_BUSINESS_FOUND_ERROR);
				else {
					jsonResult.append(bname + " is open on:\n");
					ResultSetMetaData rsmd = resultSet.getMetaData();
					int columnsNumber = rsmd.getColumnCount();
					while (resultSet.next())
						for (int i = 1; i <= columnsNumber; ++i) {
							String columnValue = resultSet.getString(i);
							if (!"N\\A".equals(columnValue)) {
								if (i > 1)
									jsonResult.append("\n");
								jsonResult.append("Between " + columnValue + " on " + rsmd.getColumnName(i) + "s");
							}
						}
				}
				connection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return utils.createWebhookResponseContent(jsonResult.toString(), s);
	}

	private HttpResponseMessage getHourByDay(JSONObject queryResult, HttpRequestMessage<Optional<String>> s,
			final ExecutionContext c) {
		c.getLogger().info("=========== GET BUSINESS HOURS BY DAY ===========");
		// ===================================
		// get query parameters
		JSONObject parameters = queryResult.getJSONObject("parameters");
		String bname = "", day = "";
		if (parameters.has("Business"))
			bname = parameters.getString("Business");
		else
			return utils.createWebhookResponseContent(globals.MISSING_BUSINESS_PARAM, s);
		if (parameters.has("DayOfWeek"))
			day = parameters.getString("DayOfWeek");
		else
			return utils.createWebhookResponseContent(globals.MISSING_DAY_PARAM, s);
		// ===================================
		// check day is supported
		day = utils.dayValidation(day);
		if ("".equals(day))
			return utils.createWebhookResponseContent("I'm sorry I don't know what day you mean\n", s);
		// ===================================
		Connection connection = null;
		StringBuilder jsonResult = new StringBuilder();
		try {
			connection = DriverManager.getConnection(globals.CONNECTION_STRING);
			// Create and execute a SELECT SQL statement.
			String selectSql = "SELECT BusinessName, " + day + " FROM BUSINESSES WHERE CHARINDEX('" + bname
					+ "', BusinessName) != 0";
			try (Statement statement = connection.createStatement();
					ResultSet resultSet = statement.executeQuery(selectSql)) {
				if (resultSet.isBeforeFirst()) // we have values to read
					while (resultSet.next())
						if (!"N\\A".equals(resultSet.getString(2)))
							jsonResult.append("The " + resultSet.getString(1) + " is open between "
									+ resultSet.getString(2) + " on " + day + "s");
						else
							jsonResult.append(resultSet.getString(1) + " is not open on " + day + "s");
				else
					jsonResult.append(globals.NO_BUSINESS_FOUND_ERROR);
				connection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return utils.createWebhookResponseContent(jsonResult.toString(), s);

	}

}