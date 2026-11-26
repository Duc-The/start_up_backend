package ocop.vn.ocop_tourims_platform.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.stream.Collectors;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.DirectionsRequest;
import ocop.vn.ocop_tourims_platform.dto.response.DirectionsResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class DirectionsService {
  private final RestClient client;
  private final String key;
  public DirectionsService(@Value("${map4d.route.base-url:https://api.map4d.vn}") String baseUrl,
      @Value("${map4d.route.api-key:}") String key) {
    this.key = key;
    var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    factory.setReadTimeout(Duration.ofSeconds(15));
    this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
  }
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ProviderResult(String code, DirectionsResponse result) {}

  public DirectionsResponse route(DirectionsRequest request) {
    if (key.isBlank()) throw new BusinessException(ErrorCode.ROUTE_UNAVAILABLE);
    var points = request.points();
    try {
      var response = client.get().uri(uri -> uri.path("/sdk/route")
          .queryParam("key", key).queryParam("origin", points.getFirst().encoded())
          .queryParam("destination", points.getLast().encoded())
          .queryParam("points", points.subList(1, points.size()-1).stream().map(DirectionsRequest.Coordinate::encoded).collect(Collectors.joining(";")))
          .queryParam("mode", request.mode()).queryParam("language", "vi")
          .queryParam("weighting", "foot".equals(request.mode()) ? 0 : 1)
          .queryParam("optimize", false).build()).retrieve().body(ProviderResult.class);
      if (response == null || !"ok".equalsIgnoreCase(response.code()) && !"success".equalsIgnoreCase(response.code()) || response.result() == null)
        throw new BusinessException(ErrorCode.ROUTE_UNAVAILABLE);
      if (response.result().routes() == null || response.result().routes().isEmpty())
        throw new BusinessException(ErrorCode.ROUTE_NOT_FOUND);
      return new DirectionsResponse(response.result().routes().stream()
          .sorted(java.util.Comparator.comparingDouble(DirectionsService::durationSeconds)).toList());
    } catch (RestClientException exception) {
      // Do not log provider URLs: they contain the private routing API key.
      throw new BusinessException(ErrorCode.ROUTE_UNAVAILABLE);
    }
  }
  private static double durationSeconds(DirectionsResponse.Route route) {
    if (route.duration() != null && route.duration().value() > 0) return route.duration().value();
    if (route.legs() == null || route.legs().isEmpty()) return Double.POSITIVE_INFINITY;
    double seconds = route.legs().stream().mapToDouble(leg -> leg.duration() == null ? 0 : leg.duration().value()).sum();
    return seconds > 0 ? seconds : Double.POSITIVE_INFINITY;
  }
}
