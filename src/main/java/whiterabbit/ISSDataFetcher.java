package whiterabbit;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.json.JSONObject;

public class ISSDataFetcher {
	private static final String DB_URL = "jdbc:sqlite:test.db";
	private static final String API_URL = "https://tle.ivanstanojevic.me/api/tle/25544";

	public static void main(String[] args) {
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(API_URL))
				.header("Accept", "application/json")
				.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			JSONObject json = new JSONObject(response.body());

			int satelliteId = json.getInt("satelliteId");
			String name = json.getString("name");
			String date = json.getString("date");
			String line1 = json.getString("line1");
			String line2 = json.getString("line2");

			try (Connection conn = DriverManager.getConnection(DB_URL)) {
				String sql = "INSERT INTO ISSData (satelliteId, name, date, line1, line2) VALUES (?, ?, ?, ?, ?)";
				
				try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
					pstmt.setInt(1, satelliteId);
					pstmt.setString(2, name);
					pstmt.setString(3, date);
					pstmt.setString(4, line1);
					pstmt.setString(5, line2);

					pstmt.executeUpdate();
					System.out.println("Data successfully inserted into database.");
			}
		}
	} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
