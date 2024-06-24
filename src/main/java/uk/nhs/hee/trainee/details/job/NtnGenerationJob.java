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

package uk.nhs.hee.trainee.details.job;

import static org.springframework.scheduling.annotation.Scheduled.CRON_DISABLED;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.nhs.hee.trainee.details.service.NtnGenerator;
import uk.nhs.hee.trainee.details.service.TraineeProfileService;

@Component
public class NtnGenerationJob {

  private final TraineeProfileService profileService;
  private final NtnGenerator ntnGenerator;

  public NtnGenerationJob(TraineeProfileService profileService, NtnGenerator ntnGenerator) {
    this.profileService = profileService;
    this.ntnGenerator = ntnGenerator;
  }

  @Scheduled(cron = CRON_DISABLED)
  @SchedulerLock(name = "NtnGenerationJob", lockAtLeastFor = "PT5m", lockAtMostFor = "PT15m")
  void run() {

  }
}
