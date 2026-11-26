package ocop.vn.ocop_tourims_platform.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class SearchText {
  private SearchText() {}
  public static final String ACCENTS = "àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ";
  public static final String PLAIN = "aaaaaaaaaaaaaaaaaeeeeeeeeeeeiiiiiooooooooooooooooouuuuuuuuuuuyyyyyd";
  private static final Pattern MARKS = Pattern.compile("\\p{M}+");
  public static String normalize(String value) {
    return MARKS.matcher(Normalizer.normalize(value.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)).replaceAll("").replace('đ', 'd');
  }
  public static String pattern(String value) {
    return "%" + normalize(value).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }
}
