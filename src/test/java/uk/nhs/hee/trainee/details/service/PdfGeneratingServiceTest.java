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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.templatemode.TemplateMode;

class PdfGeneratingServiceTest {
  private static final ZoneId TIMEZONE = ZoneId.of("Europe/London");

  private PdfGeneratingService service;
  private TemplateEngine templateEngine;

  @BeforeEach
  void setUp() {
    templateEngine = mock(TemplateEngine.class);
    service = new PdfGeneratingService(templateEngine, TIMEZONE);
  }

  @Test
  void shouldGeneratePdf() throws IOException {
    String templatePath = "path" + File.separatorChar + "version.html";
    TemplateSpec templateSpec = new TemplateSpec(templatePath, Set.of(), TemplateMode.HTML, null);

    when(templateEngine.process(any(TemplateSpec.class), any())).thenReturn(
        "<html>test content</html>");

    byte[] resultByte =  service.generatePdf(templateSpec, Map.of());
    ByteArrayInputStream resultStream = new ByteArrayInputStream(resultByte);
    PDDocument pdf = Loader.loadPDF(new RandomAccessReadBuffer(resultStream));
    String pdfText = new PDFTextStripper().getText(pdf);

    assertThat("Unexpected content.", pdfText, is("test content" + System.lineSeparator()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Łukasz", "Józef Wiśniewski", "Erdős", "Ağaoğlu Şahin", "Ćosić Čapek",
      "Dvořák Kněžek", "Țepeș Ștefan", "Zoë Muñoz Óscar"})
  void shouldRenderLatinExtendedCharactersUsingFallbackFont(String name) throws IOException {
    TemplateSpec templateSpec = new TemplateSpec("template", Set.of(), TemplateMode.HTML, null);

    when(templateEngine.process(any(TemplateSpec.class), any())).thenReturn(
        "<html><body style=\"font-family: 'Frutiger W01', Arial, sans-serif;\">" + name
            + "</body></html>");

    byte[] resultByte = service.generatePdf(templateSpec, Map.of());
    PDDocument pdf = Loader.loadPDF(new RandomAccessReadBuffer(resultByte));
    String pdfText = new PDFTextStripper().getText(pdf);

    assertThat("Unexpected content.", pdfText, is(name + System.lineSeparator()));
  }
}
