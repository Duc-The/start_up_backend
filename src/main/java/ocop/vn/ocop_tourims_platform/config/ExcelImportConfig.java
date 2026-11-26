package ocop.vn.ocop_tourims_platform.config;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExcelImportConfig {
  public ExcelImportConfig() {
    ZipSecureFile.setMaxEntrySize(16L * 1024 * 1024);
    ZipSecureFile.setMaxFileCount(1000);
    ZipSecureFile.setMaxTextSize(10L * 1024 * 1024);
  }
}
