package ocop.vn.ocop_tourims_platform.common;

import java.text.Normalizer;
import java.util.Locale;

public final class CatalogCodes {
  private CatalogCodes() {}

  public static String normalize(String value) {
    if (value == null) return null;
    return Normalizer.normalize(value, Normalizer.Form.NFKC)
        .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
        .strip()
        .toUpperCase(Locale.ROOT);
  }
}
