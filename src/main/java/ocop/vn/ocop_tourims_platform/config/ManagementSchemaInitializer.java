package ocop.vn.ocop_tourims_platform.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/** Runs the idempotent compatibility script after Hibernate schema creation/update. */
@Component
@Order(-100)
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "management.migrate-schema",
    havingValue = "true",
    matchIfMissing = true)
public class ManagementSchemaInitializer implements ApplicationRunner {
  private final DataSource dataSource;

  @Override
  public void run(ApplicationArguments args) {
    var script =
        new ResourceDatabasePopulator(new ClassPathResource("db/migrations/management.sql"));
    script.setSqlScriptEncoding("UTF-8");
    script.execute(dataSource);
  }
}
