package whiterabbit;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.geotools.map.DirectLayer;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.sql.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.map.MapViewport;

public class ISSMapVisualizer extends JFrame {
	private static final String DB_URL = "jdbc:sqlite:test.db";
	private List<PositionData> positions;
	private MapContent map;
	private static final int WIDTH = 1200;
	private static final int HEIGHT = 600;
	
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
		setMinimumSize(new Dimension(800, 400));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		loadPositions();
		pack();
		setLocationRelativeTo(null);
		SwingUtilities.invokeLater(this::initializeMap);
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
			JOptionPane.showMessageDialog(this, "Error loading positions: " + e.getMessage(),
				"Database Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	private void initializeMap() {
		try {
			map = new MapContent();
			map.setTitle("ISS Position Map");
			
			File shapeFile = new File("/home/whiterabbit/CodingStuff/Javain/ISSTLE/naturalearthdata/ne_50m_admin_0_countries.shp");
			if (!shapeFile.exists()) {
				throw new IllegalStateException("ShapeFile not found: " + shapeFile.getAbsolutePath());
			}

			FileDataStore dataStore = FileDataStoreFinder.getDataStore(shapeFile);
			SimpleFeatureSource featureSource = dataStore.getFeatureSource();
			
			StyleBuilder styleBuilder = new StyleBuilder();
			Style style = styleBuilder.createStyle(styleBuilder.createPolygonSymbolizer());

			Layer countryLayer = new FeatureLayer(featureSource, style);
			map.addLayer(countryLayer);

			map.addLayer(new ISSPositionLayer());

			ReferencedEnvelope bounds = new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);

			JMapFrame mapFrame = new JMapFrame(map);
			mapFrame.enableStatusBar(true);
			mapFrame.enableToolBar(true);
			mapFrame.setSize(WIDTH, HEIGHT);

			mapFrame.getMapPane().setDisplayArea(bounds);

			mapFrame.setVisible(true);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
				"Error initializing map: " + e.getMessage(),
				"Map Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

	private class ISSPositionLayer extends DirectLayer {
		@Override
		public void draw(Graphics2D g2d, MapContent mapContent, MapViewport mapViewport) {
			if (positions.isEmpty()) return;

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

			Rectangle screenArea = mapViewport.getScreenArea();
			if (screenArea.width <= 0 || screenArea.height <= 0) {
				return;
			}

			ReferencedEnvelope mapBounds = mapViewport.getBounds();

			g2d.setFont(new Font("Arial", Font.PLAIN, 10));

			for (PositionData pos : positions) {
				Point point = geometryFactory.createPoint(new Coordinate(pos.longitude, pos.latitude));
				java.awt.Point screenPos = worldToScreen(point.getCoordinate(), screenArea, mapBounds);

				g2d.setColor(Color.RED);
				g2d.fill(new Ellipse2D.Double(screenPos.x - 3, screenPos.y - 3, 6, 6));

				g2d.setColor(Color.BLACK);
				ZonedDateTime dateTime = ZonedDateTime.parse(pos.date);
				String formattedDate = dateTime.format(DateTimeFormatter.ofPattern("MM/dd HH:mm"));
				g2d.drawString(formattedDate, screenPos.x + 5, screenPos.y + 4);
			}
		}
	
	private java.awt.Point worldToScreen(Coordinate coord, Rectangle screen, ReferencedEnvelope mapBounds) {
			double x = (coord.x - mapBounds.getMinX()) / mapBounds.getWidth() * screen.width;
			double y = screen.height - ((coord.y - mapBounds.getMinY()) / mapBounds.getHeight() * screen.height);
			return new java.awt.Point((int) x, (int) y);
		}

		@Override
		public ReferencedEnvelope getBounds() {
			return new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				ISSMapVisualizer map = new ISSMapVisualizer();
				map.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Error starting application: " + e.getMessage(), "Application Error",
				JOptionPane.ERROR_MESSAGE);
			}
		});
	}
}


