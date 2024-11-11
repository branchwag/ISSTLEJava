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
		{74, -170}, {68, -140}, {72, -125}, {60, -140}, {54, -130},
		{48, -124}, {40, -124}, {35, -120}, {32, -117}, {30, -115},
		{25, -110}, {20, -105}, {25, -97}, {30, -90}, {35, -85},
		{40, -73}, {45, -70}, {50, -60}, {60, -70}, {65, -80},
		{70, -90}, {75, -100}, {80, -120}, {75, -140}, {74, -170}
	    };
	    
	    private static final int[][] SOUTH_AMERICA = {
		{12, -72}, {5, -75}, {0, -78}, {-5, -80}, {-10, -75},
		{-15, -76}, {-20, -70}, {-25, -70}, {-30, -72}, {-35, -73},
		{-40, -74}, {-50, -74}, {-55, -70}, {-50, -65}, {-45, -60},
		{-35, -54}, {-30, -50}, {-25, -48}, {-20, -40}, {-15, -38},
		{-10, -42}, {-5, -45}, {0, -50}, {5, -55}, {8, -52},
		{12, -72}
	    };
	    
	    private static final int[][] EUROPE = {
		{70, -10}, {65, 0}, {60, 10}, {65, 20}, {60, 30},
		{55, 25}, {50, 30}, {45, 28}, {40, 25}, {35, 25},
		{40, 20}, {43, 15}, {45, 10}, {48, 5}, {50, 0},
		{55, -5}, {60, -8}, {65, -10}, {70, -10}
	    };
	    
	    private static final int[][] AFRICA = {
		{35, -10}, {30, -5}, {25, 0}, {20, 5}, {15, 10},
		{10, 15}, {5, 20}, {0, 25}, {-5, 30}, {-10, 35},
		{-15, 40}, {-20, 40}, {-25, 35}, {-30, 30}, {-35, 20},
		{-35, 15}, {-30, 10}, {-25, 15}, {-20, 14}, {-15, 12},
		{-10, 10}, {-5, 8}, {0, 10}, {5, 5}, {10, 0},
		{15, -5}, {20, -10}, {25, -15}, {30, -12}, {35, -10}
	    };
	    
	    private static final int[][] ASIA = {
		{70, 30}, {75, 40}, {75, 60}, {70, 80}, {75, 100},
		{70, 120}, {65, 140}, {60, 150}, {55, 155}, {50, 145},
		{45, 140}, {40, 140}, {35, 140}, {30, 130}, {25, 120},
		{20, 110}, {15, 100}, {10, 95}, {15, 80}, {20, 70},
		{25, 65}, {30, 55}, {35, 50}, {40, 45}, {45, 40},
		{50, 35}, {55, 30}, {60, 25}, {65, 30}, {70, 30}
	    };
	    
	    private static final int[][] AUSTRALIA = {
		{-10, 110}, {-15, 120}, {-12, 130}, {-12, 135}, {-15, 140},
		{-20, 148}, {-25, 150}, {-30, 152}, {-35, 150}, {-38, 145},
		{-40, 140}, {-35, 135}, {-32, 130}, {-30, 125}, {-25, 115},
		{-20, 115}, {-15, 115}, {-12, 120}, {-10, 110}
	    };

	    private static final int[][] ANTARCTICA = {
		{-65, -60}, {-70, -40}, {-75, -20}, {-80, 0}, {-85, 20},
		{-80, 40}, {-75, 60}, {-70, 80}, {-75, 100}, {-70, 120},
		{-75, 140}, {-70, 160}, {-75, 180}, {-70, -160}, {-75, -140},
		{-70, -120}, {-75, -100}, {-70, -80}, {-65, -60}
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

		g2d.setColor(new Color(220, 220, 220));
		drawContinent(g2d, ANTARCTICA);

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
		Color currentColor = g2d.getColor();
		g2d.setColor(currentColor.darker());
		g2d.draw(path);
		g2d.setColor(currentColor);
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




