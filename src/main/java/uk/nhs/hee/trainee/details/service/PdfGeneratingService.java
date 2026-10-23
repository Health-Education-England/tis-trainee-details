/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.trainee.details.service;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FSFontUseCase;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import com.openhtmltopdf.util.XRLog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.Context;

/**
 * A service handling PDF generation.
 */
@Slf4j
@Service
public class PdfGeneratingService {

  private static final String FALLBACK_FONT_FAMILY = "Noto Sans";
  private static final String FALLBACK_FONT_REGULAR = "/fonts/NotoSans-Regular.ttf";
  private static final String FALLBACK_FONT_BOLD = "/fonts/NotoSans-Bold.ttf";

  private final TemplateEngine templateEngine;
  private final ZoneId timezone;

  /**
   * A service handling PDF generation.
   *
   * @param templateEngine The template engine to use for creating an HTML version of the letters.
   * @param timezone       The timezone.
   */
  public PdfGeneratingService(TemplateEngine templateEngine,
                              @Value("${application.timezone}") ZoneId timezone) {
    this.templateEngine = templateEngine;
    this.timezone = timezone;

    XRLog.setLoggerImpl(new Slf4jLogger());
  }

  /**
   * Generated a PDF with the template.
   *
   * @param templateSpec      The template spec to use.
   * @param templateVariables The variables to insert in to the template.
   * @return The generated PDF as an array of bytes.
   * @throws IOException If the renderer could not build a valid PDF.
   */
  public byte[] generatePdf(TemplateSpec templateSpec, Map<String, Object> templateVariables)
      throws IOException {
    log.info("Generating a PDF using template '{}'.", templateSpec.getTemplate());

    Map<String, Object> enhancedVariables = new HashMap<>(templateVariables);
    enhancedVariables.put("timezone", timezone.getId());

    String body = templateEngine.process(templateSpec,
        new Context(Locale.ENGLISH, enhancedVariables));
    Document parsedBody = Jsoup.parse(body);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    PdfRendererBuilder builder = new PdfRendererBuilder()
        .toStream(os)
        .useSVGDrawer(new BatikSVGDrawer())
        .withW3cDocument(W3CDom.convert(parsedBody), "classpath:/");
    registerFallbackFonts(builder);
    builder.run();

    return os.toByteArray();
  }

  /**
   * Register fallback fonts, used for any glyph not supported by the document's fonts (e.g. Latin
   * Extended characters such as Ł, ś, ő, ț which are missing from the NHS Frutiger fonts).
   * Without a fallback the renderer substitutes unsupported glyphs with '#'. The fonts are used as
   * a final fallback, after the built-in fonts, so that existing rendering is unaffected.
   *
   * @param builder The builder to register the fonts with.
   */
  private void registerFallbackFonts(PdfRendererBuilder builder) {
    Set<FSFontUseCase> useCases = EnumSet.of(FSFontUseCase.FALLBACK_FINAL);
    builder.useFont(() -> loadFont(FALLBACK_FONT_REGULAR), FALLBACK_FONT_FAMILY, 400,
        FontStyle.NORMAL, true, useCases);
    builder.useFont(() -> loadFont(FALLBACK_FONT_BOLD), FALLBACK_FONT_FAMILY, 700,
        FontStyle.NORMAL, true, useCases);
  }

  /**
   * Load a font from the classpath.
   *
   * @param path The classpath location of the font.
   * @return The font's input stream.
   */
  private InputStream loadFont(String path) {
    InputStream is = PdfGeneratingService.class.getResourceAsStream(path);

    if (is == null) {
      throw new IllegalStateException("Font not found on classpath: " + path);
    }

    return is;
  }
}
