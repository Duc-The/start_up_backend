package ocop.vn.ocop_tourims_platform;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import ocop.vn.ocop_tourims_platform.common.*;
import ocop.vn.ocop_tourims_platform.dto.request.DirectionsRequest;
import ocop.vn.ocop_tourims_platform.service.DirectionsService;
import org.junit.jupiter.api.Test;

class DirectionsServiceTests {
  @Test void forwardsStopsInOrderAndReadsVietnameseRoute() throws Exception {
    var query = new AtomicReference<String>();
    var server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/sdk/route", exchange -> {
      query.set(exchange.getRequestURI().getQuery());
      byte[] data = """
          {"code":"ok","result":{"routes":[
            {"overviewPolyline":"slow","summary":"Đường vòng","duration":{"text":"10 phút","value":600},"legs":[]},
            {"overviewPolyline":"abc","summary":"Đường ven biển","legs":[{"duration":{"text":"5 phút","value":300},"steps":[]}]}
          ]}}
          """.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, data.length);
      exchange.getResponseBody().write(data); exchange.close();
    });
    server.start();
    try {
      var service = new DirectionsService("http://localhost:"+server.getAddress().getPort(), "test-only");
      var response=service.route(new DirectionsRequest(List.of(new DirectionsRequest.Coordinate(16.0,108.0),new DirectionsRequest.Coordinate(16.1,108.1),new DirectionsRequest.Coordinate(16.2,108.2)),"motorcycle"));
      assertEquals("Đường ven biển",response.routes().getFirst().summary());
      assertTrue(query.get().contains("origin=16.0,108.0"));
      assertTrue(query.get().contains("destination=16.2,108.2"));
      assertTrue(query.get().contains("points=16.1,108.1"));
      assertTrue(query.get().contains("mode=motorcycle"));
      assertTrue(query.get().contains("language=vi"));
      assertTrue(query.get().contains("weighting=1"));
      assertTrue(query.get().contains("optimize=false"));
      assertEquals("Đường vòng",response.routes().getLast().summary());
      service.route(new DirectionsRequest(List.of(new DirectionsRequest.Coordinate(16.0,108.0),new DirectionsRequest.Coordinate(16.2,108.2)),"foot"));
      assertTrue(query.get().contains("mode=foot"));
      assertTrue(query.get().contains("weighting=0"));
    } finally {server.stop(0);}
  }
  @Test void missingProviderKeyReturnsVietnameseBusinessError() {
    var exception=assertThrows(BusinessException.class,()->new DirectionsService("http://localhost", "").route(new DirectionsRequest(List.of(),"car")));
    assertEquals(ErrorCode.ROUTE_UNAVAILABLE,exception.getErrorCode());
  }
  @Test void invalidAndTooManyCoordinatesAreRejected() {
    try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
      var validator=factory.getValidator();
      assertFalse(validator.validate(new DirectionsRequest(List.of(new DirectionsRequest.Coordinate(Double.NaN,108.0),new DirectionsRequest.Coordinate(16.0,108.0)),"flying")).isEmpty());
      assertFalse(validator.validate(new DirectionsRequest(java.util.Collections.nCopies(8,new DirectionsRequest.Coordinate(16.0,108.0)),"car")).isEmpty());
      assertTrue(validator.validate(new DirectionsRequest(List.of(new DirectionsRequest.Coordinate(16.0,108.0),new DirectionsRequest.Coordinate(16.1,108.1)),"foot")).isEmpty());
    }
  }
}
