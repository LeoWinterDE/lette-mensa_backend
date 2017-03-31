package de.lette;

import java.io.IOException;
import java.util.regex.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.*;

import com.mysql.jdbc.exceptions.MySQLNonTransientConnectionException;

@WebServlet("/Day")
public class Day extends HttpServlet {
	private static final long serialVersionUID = 1L;
	final String sql_terminNormal = "SELECT * FROM termine WHERE datum=?";
	final String sql_speiseNormal = "SELECT * FROM speisen WHERE id=?";
	final String sql_terminDiaet = "SELECT * FROM diaetermine WHERE datum=?";
	final String sql_speiseDiaet = "SELECT * FROM diaetspeisen WHERE id=?";
	ConnectDB connection;
	
	@Override	
	public void init()
	{
		connection = new ConnectDB();
		ServletContext context = getServletContext();
		String fullPath = context.getRealPath("/WEB-INF/db.cfg");
		
		connection.init(fullPath);
		try {
			connection.connectDB();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private JSONObject getWeekfromDate(String date, String termin, String speise, HttpServletResponse response)
			throws SQLException, ClassNotFoundException, IOException, MySQLNonTransientConnectionException {

		JSONObject dateObject = new JSONObject();
		JSONObject typeObject = null;
		ArrayList<Integer> dateArray = new ArrayList<Integer>();
		String formated = "";
		
		String[] dateElements = date.split("-");
		String year = dateElements[0];
		String month = dateElements[1];
		int day = Integer.parseInt(dateElements[2]);
		
		if (day < 10) {
			formated = String.format("%02d", day);
		} else {
			formated = Integer.toString(day);
		}

		if (connection.getDbConnection() != null) {
			PreparedStatement psFindDates = null;
			psFindDates = connection.getDbConnection().prepareStatement(termin);
			psFindDates.setString(1, year + "-" + month + "-" + formated);

			ResultSet rsDates = psFindDates.executeQuery();
			dateArray.clear();
			while (rsDates.next()) {
				dateArray.add(Integer.parseInt(rsDates.getString(3)));
			}
			typeObject = new JSONObject();
			for (int iterateDates = 0; iterateDates < dateArray.size(); iterateDates++) {
				PreparedStatement psFindMealByDate = connection.dbConnection.prepareStatement(speise);
				psFindMealByDate.setInt(1, dateArray.get(iterateDates));
				ResultSet rsMealsByDay = psFindMealByDate.executeQuery();

				while (rsMealsByDay.next()) {
					JSONObject foodObject = new JSONObject();

					if (rsMealsByDay.getString(2) == "") {
						foodObject.put("name", "Kein Name vorhanden");
					} else {
						foodObject.put("name", rsMealsByDay.getString(2));
					}
					if (rsMealsByDay.getString(4) == "") {
						foodObject.put("beachte", "Nichts zu beachten");
					} else {
						foodObject.put("beachte", rsMealsByDay.getString(4));
					}
					foodObject.put("kcal", rsMealsByDay.getString(5));
					foodObject.put("eiweisse", rsMealsByDay.getString(6));
					foodObject.put("fette", rsMealsByDay.getString(7));
					foodObject.put("kolenhydrate", rsMealsByDay.getString(8));

					if (rsMealsByDay.getString(9) == "") {
						foodObject.put("beschreibung", "Keine Beschreibung vorhanden");
					} else {
						foodObject.put("beschreibung", rsMealsByDay.getString(9));
					}
					foodObject.put("preis", rsMealsByDay.getString(10));
					foodObject.put("zusatzstoffe", rsMealsByDay.getString(11));

					String art = rsMealsByDay.getString(3);
					typeObject.put(art, foodObject);
				}
				psFindDates.close();
			}
			dateObject.put(year + "-" + month + "-" + formated, typeObject);
			day++;
		}
		return dateObject;

	}

	private boolean checkValidDate(String date) {
		return Pattern.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}", date);
	}

	private void getData(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, ClassNotFoundException, SQLException {

		if (connection.getDbConnection() != null) {
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json");

			if (request.getParameter("day") == null || request.getParameter("day") == "") {
				response.getWriter().write("Invalid input");
			} else {
				JSONObject masterObj = new JSONObject();
				try {
					if (checkValidDate(request.getParameter("day"))) {
						JSONObject usualCanteen = getWeekfromDate(request.getParameter("day"), sql_terminNormal,
								sql_speiseNormal, response);
						JSONObject diatCanteen = getWeekfromDate(request.getParameter("day"), sql_terminDiaet,
								sql_speiseDiaet, response);
						masterObj.put("Mensa0", usualCanteen);
						masterObj.put("Mensa1", diatCanteen);
					} else {
						response.sendError(400);
					}

				} catch (ClassNotFoundException | SQLException e) {
					e.printStackTrace();
				}
				response.getWriter().write(masterObj.toString());
			}
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			getData(request, response);
		} catch (ClassNotFoundException | SQLException e) {
			e.getStackTrace();
		}
	}

}