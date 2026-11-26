package ocop.vn.ocop_tourims_platform.dto.response;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DirectionsResponse(List<Route> routes) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Route(String overviewPolyline, String summary, Measure distance, Measure duration, List<Leg> legs) {}
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Measure(String text, double value) {}
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Leg(Measure distance, Measure duration, String startAddress, String endAddress, List<Step> steps) {}
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Step(String htmlInstructions, String maneuver, String streetName, Measure distance, Measure duration) {}
}
