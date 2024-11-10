package whiterabbit;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.data.DataContext;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class ISSPositionCalculator {
	private final String line1;
	private final String line2;
	private final String date;

	public ISSPositionCalculator(String line1, String line2, String date) {
		this.line1 = line1;
		this.line2 = line2;
		this.date = date;
	}

	public static class Position {
		public final double latitude;
		public final double longitude;

		public Position(double latitude, double longitude) {
			this.latitude = latitude;
			this.longitude = longitude;
		}

		@Override
		public String toString() {
			return String.format("Latitude: %.6f°, Longitude: %.6f°", latitude, longitude);
		}
	}

	public Position calculatePosition() throws Exception {
		File orekitData = new File("orekit-data");
		DataContext.getDefault().getDataProvidersManager().addProvider(new DirectoryCrawler(orekitData));

		TLE tle = new TLE(line1, line2);

		TLEPropagator propagator = TLEPropagator.selectExtrapolator(tle);

		ZonedDateTime dateTime = ZonedDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
		AbsoluteDate orbitDate = new AbsoluteDate(
			dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(),
			TimeScalesFactory.getUTC());

		PVCoordinates pvCoordinates = propagator.propagate(orbitDate).getPVCoordinates();

		Frame earthFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
		Transform transform = propagator.getFrame().getTransformTo(earthFrame, orbitDate);
		Vector3D earthFixedPosition = transform.transformPosition(pvCoordinates.getPosition());

		double latitude = FastMath.toDegrees(FastMath.asin(earthFixedPosition.getZ() / earthFixedPosition.getNorm()));
		double longitude = FastMath.toDegrees(FastMath.atan2(earthFixedPosition.getY(), earthFixedPosition.getX()));

		if (longitude > 180) {
			longitude -= 360;
		} else if (longitude < -180) {
			longitude += 360;
		}

		return new Position(latitude, longitude);

	}

    // Example usage
    public static void main(String[] args) {
        try {
            String line1 = "1 25544U 98067A   24314.17159890 -.00085559  00000+0 -14417-2 0  9996";
            String line2 = "2 25544  51.6396 317.1493 0010599 158.5386 326.8756 15.51182438481031";
            String date = "2024-11-09T04:07:06+00:00";

            ISSPositionCalculator calculator = new ISSPositionCalculator(line1, line2, date);
            Position position = calculator.calculatePosition();
            System.out.println(position);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
