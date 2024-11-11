package whiterabbit;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.sql.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ISSMapVisualizer extends JFrame {
	private static final String DB_URL = "jdbc:sqlite:test.db";
	private List<PositionData> positions;
	private static final int WIDTH = 1200;
	private static final int HEIGHT = 600;

	
	private static final int[][] NORTH_AMERICA = {
        {54, -130}, {54, -100}, {48, -88}, {44, -82}, {40, -73}, 
        {25, -80}, {20, -105}, {35, -120}, {48, -124}, {54, -130}
	};
    
	private static final int[][] SOUTH_AMERICA = {
        {12, -72}, {8, -52}, {-10, -42}, {-20, -40}, {-35, -54},
        {-52, -70}, {-40, -74}, {-15, -76}, {0, -78}, {12, -72}
	};
    
	private static final int[][] EUROPE = {
        {60, -5}, {65, 10}, {60, 20}, {55, 25}, {45, 28},
        {40, 20}, {43, 5}, {48, -5}, {55, -8}, {60, -5}
	};
    
	private static final int[][] AFRICA = {
        {35, -10}, {30, 32}, {12, 45}, {-20, 40}, {-35, 20},
        {-35, 15}, {-22, 14}, {0, 10}, {15, -15}, {35, -10}
	};
    
	private static final int[][] ASIA = {
        {65, 30}, {68, 68}, {65, 100}, {50, 132}, {35, 140},
        {22, 110}, {12, 95}, {25, 65}, {45, 45}, {65, 30}
	};
    
	private static final int[][] AUSTRALIA = {
        {-12, 130}, {-12, 142}, {-20, 148}, {-32, 152}, {-38, 145},
        {-35, 135}, {-25, 115}, {-20, 115}, {-12, 125}, {-12, 130}
	};
	
	static class PositionData {
		double latitude;
		double longitude;
		String date;

		PositionData(double latitude, double longitude, String date) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.date = date;
		}
	}

	public ISSMapVisualizer() {
		setTitle("ISS Position Map");
		setSize(WIDTH, HEIGHT);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		loadPositions();

		JPanel mapPanel = new JPanel() {
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				drawMap(g);
			}
		};
		add(mapPanel);
	}

	private void loadPositions() {
		positions = new ArrayList<>();
		try (Connection conn = DriverManager.getConnection(DB_URL);
			PreparedStatement stmt = conn.prepareStatement("SELECT latitude, longitude, date FROM ISSData WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY date")) {

			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				positions.add(new PositionData(
					rs.getDouble("latitude"),
					rs.getDouble("longitude"),
					rs.getString("date")
					));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private Point.Double convertToPixel(double lat, double lon) {
		double x = (lon + 180) * (WIDTH - 40) / 360 + 20;
		double y = (90 - lat) * (HEIGHT - 40) / 180 + 20;
		return new Point.Double(x,y);
	}

	private void drawMap(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		//OCEAN
		g2d.setColor(new Color(200, 220, 255));
		g2d.fillRect(0, 0, WIDTH, HEIGHT);
		
		//GRID LINES
		g2d.setColor(new Color(180, 180, 180, 80));
		drawGrid(g2d);

		//CONTINENTS
		g2d.setColor(new Color(150, 200, 150));
		drawContinent(g2d, NORTH_AMERICA);
		drawContinent(g2d, SOUTH_AMERICA);
		drawContinent(g2d, EUROPE);
		drawContinent(g2d, AFRICA);
		drawContinent(g2d, ASIA);
		drawContinent(g2d, AUSTRALIA);

		drawISSPositions(g2d);
	}

	private void drawGrid(Graphics2D g2d) {
		for (int lon = -180; lon <= 180; lon += 30) {
			Point.Double start = convertToPixel(90, lon);
			Point.Double end = convertToPixel(-90, lon);
			g2d.draw(new Line2D.Double(start.x, start.y, end.x, end.y));
		}

		for (int lat = -90; lat <= 90; lat += 30) {
			Point.Double start = convertToPixel(lat, -180);
			Point.Double end = convertToPixel(lat, 180);
			g2d.draw(new Line2D.Double(start.x, start.y, end.x, end.y));
		}
	}

	private void drawContinent(Graphics2D g2d, int[][] coordinates) {
		Path2D.Double path = new Path2D.Double();
		boolean first = true;

		for (int[] coord : coordinates) {
			Point.Double point = convertToPixel(coord[0], coord[1]);
			if (first) {
				path.moveTo(point.x, point.y);
				first = false;
			} else {
				path.lineTo(point.x, point.y);
			}
		}

		path.closePath();
		g2d.fill(path);
		g2d.setColor(new Color(100, 150, 100));
		g2d.draw(path);
	}

	private void drawISSPositions(Graphics2D g2d) {
		if (positions.isEmpty()) return;

		//lines connecting positions
		/*
		g2d.setColor(new Color(200, 0, 0, 100));
		g2d.setStroke(new BasicStroke(1.5f));

		for (int i = 1; i < positions.size(); i++) {
			Point.Double prev = convertToPixel(
				positions.get(i-1).latitude,
				positions.get(i-1).longitude
			);
			Point.Double curr = convertToPixel(
				positions.get(i).latitude,
				positions.get(i).longitude
			);
			g2d.draw(new Line2D.Double(prev.x, prev.y, curr.x, curr.y));
		}
		*/

		//pts and labels
		g2d.setFont(new Font("Arial", Font.PLAIN, 10));
		for (PositionData pos : positions) {
			Point.Double point = convertToPixel(pos.latitude, pos.longitude);

			g2d.setColor(Color.RED);
			g2d.fill(new Ellipse2D.Double(point.x - 3, point.y - 3, 6, 6));

			g2d.setColor(Color.BLACK);
			ZonedDateTime dateTime = ZonedDateTime.parse(pos.date);
			String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
			g2d.drawString(formattedDate, (float)point.x + 5, (float)point.y + 4);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			ISSMapVisualizer map = new ISSMapVisualizer();
			map.setVisible(true);
		});
	}
}




