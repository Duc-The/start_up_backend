package ocop.vn.ocop_tourims_platform;

import static org.junit.jupiter.api.Assertions.*;

import ocop.vn.ocop_tourims_platform.common.CatalogCodes;
import ocop.vn.ocop_tourims_platform.dto.request.ManagementRequest.CatalogForm;
import org.junit.jupiter.api.Test;

class CatalogCodesTests {
  @Test
  void pastedCodesNormalizeBeforeValidation() {
    for (String code :
        new String[] {
          "HAI_CHAU", " hai_chau ", "\u00a0HAI_CHAU\u00a0", "HAI_\u200bCHAU\ufeff", "ＨＡＩ＿ＣＨＡＵ"
        }) {
      CatalogForm request = new CatalogForm("Hải Châu", code, null, null, null, null, 0, true);
      assertEquals("HAI_CHAU", request.code());
      assertTrue(request.code().matches("^[A-Z][A-Z0-9_]*$"));
    }
  }

  @Test
  void visibleInvalidCharactersRemainInvalid() {
    assertFalse(CatalogCodes.normalize("HAI CHAU").matches("^[A-Z][A-Z0-9_]*$"));
    assertFalse(CatalogCodes.normalize("1HAI_CHAU").matches("^[A-Z][A-Z0-9_]*$"));
    assertNull(CatalogCodes.normalize(null));
  }
}
