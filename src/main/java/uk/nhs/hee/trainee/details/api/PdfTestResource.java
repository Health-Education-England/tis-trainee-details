/*
 * The MIT License (MIT)
 *
 * Copyright 2025 Crown Copyright (Health Education England)
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

package uk.nhs.hee.trainee.details.api;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.templatemode.TemplateMode;
import uk.nhs.hee.trainee.details.service.PdfGeneratingService;

@RestController
@RequestMapping("/api/pdf")
public class PdfTestResource {

  private PdfGeneratingService service;

  public PdfTestResource(PdfGeneratingService service) {
    this.service = service;
  }

  @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> downloadPdf() {


    try {
      String templatePath =
          "layout\\letter" + File.separatorChar + "v1.0.0" + ".html";
      TemplateSpec templateSpec = new TemplateSpec(
          templatePath, Set.of(), TemplateMode.HTML, null);

      byte[] generatedPdf = service.generatePdf(templateSpec, Map.of());
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_PDF)
          .body(generatedPdf);
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getLocalizedMessage());
    }
  }
}
