# CCT Calculator

This document describes how the application calculates a trainee's
Completion of Training (CCT) date. The implementation is in
[`CctCalculator.java`](src/main/java/uk/nhs/hee/trainee/details/service/CctCalculator.java).

The calculator works with a training programme, its planned duration, and any
periods that change the amount of training credited. It returns the calculated
CCT date, the months completed and remaining, the periods assumed to be full
time, the details of the projection used, and the grade for each year of the
programme.

## Summary

1. **Months required** are the programme length plus additional months, less
   accelerated months. Both are recorded per grade, and only their totals are
   used here.
2. **Months completed** are the credit from each recorded change plus full-time
   credit for gaps between changes.
3. **Months remaining** are months required less months completed, rounded to
   two decimal places.
4. If months remain, the CCT date is projected from the projection start date
   using the projection WTE.
5. If the recorded periods already provide enough credit, the CCT date is the
   day on which the required credit is reached.

A useful approximate check is:

```text
CCT date ≈ adjusted programme end date + months lost × 30.4 days

months lost
  each dated change: calendar months × (1 - credit rate)
  an open-ended LTFT: months remaining × (1 ÷ WTE - 1)
```

The approximation is for checking results only; the calculator's full calculation
also accounts for the order and exact dates of periods.

## Terms

| Term                     | Meaning                                                                  |
|--------------------------|--------------------------------------------------------------------------|
| CCT date                 | The projected date on which training is completed                        |
| WTE                      | Whole time equivalent; the proportion of full-time training, such as 60% |
| LTFT                     | Less than full-time training                                             |
| Change                   | A period that differs from ordinary full-time training                   |
| Credit rate              | The proportion of a period that counts towards completion                |
| Calendar months          | The duration of a period expressed in months                             |
| Credited months          | Calendar months multiplied by the credit rate                            |
| Assumed full-time period | A gap between recorded changes, credited at 100%                         |
| Recorded time            | Changes with an end date and assumed full-time periods                   |
| Projection               | The time after recorded periods up to the CCT date                       |
| Open-ended LTFT          | An LTFT change with no end date                                          |

## Calculation rules

### Converting dates and months

For a recorded period, both its start and end dates are included:

```text
calendar months = (end date - start date + 1) ÷ (365 ÷ 12)
credited months = calendar months × credit rate
```

For projected time, the calculator uses 30.4 days per month:

```text
projected days = months remaining ÷ projection WTE × 30.4
```

Projected days are rounded down to a whole day. A small tolerance prevents
floating-point representation from moving an exact whole-day result backwards
by one day.

The calculation does not depend on the current date. A dated change is
included even if it is planned for the future.

### Projection start

The projection is selected in this order:

1. An open-ended LTFT uses its start date and WTE.
2. Otherwise, the day after the latest dated change ends is used at 100% WTE.
3. If there are no changes, the programme start date is used at 100% WTE.

Before the projection starts, any time not covered by a change is treated as
an assumed full-time period.

### Completion within recorded time

When the recorded periods provide enough credit to complete training, the
calculator examines them in date order and calculates the first day on which
the required credit is reached. This also produces a useful CCT date when a
recorded change extends beyond the point at which training completes.

### Changes before the programme

Only time within the programme contributes to the calculation:

- A change that starts before the programme is counted from the programme start.
- A change that ends before the programme contributes no time.
- The response still shows the original change dates.

Input validation is performed before saving. It checks, among other things,
that changes do not overlap, dates are valid, and only one open-ended LTFT is
used. A calculation preview can return the calculated result together with
date errors so that they can be corrected before saving.

## How changes are credited

| Change or period | Counts towards completion | Credit rate |
| --------------- | ------------------------- | ----------- |
| Assumed full-time period | Always | 100% |
| LTFT | Always | Its WTE |
| OOPT (training) | Optional | 100% when counted, otherwise 0% |
| OOPR (research) | Optional | Approved WTE when counted, otherwise 0% |
| OOPC, OOPE, OOPP, parental leave and other non-training periods | No | 0% |

A period with 0% credit still occupies calendar time and therefore delays the
projection.

## Worked examples

The examples below use an 84-month programme starting on 02/08/2023, with no
additional or accelerated months.

### No changes

```text
Months required    84
Months completed   0
Months remaining   84.00
Days to go         84.00 × 30.4 = 2,553.6, rounded down to 2,553
CCT date           02/08/2023 + 2,553 days = 29/07/2030
```

### LTFT at 60% for six months

The trainee is at 60% WTE from 01/02/2024 to 01/08/2024.

```text
Full-time period   02/08/2023 to 31/01/2024 = 6.02 credited months
LTFT period        01/02/2024 to 01/08/2024 = 6.02 × 60% = 3.61 months
Months remaining   84 - 9.63 = 74.37
Projection         from 02/08/2024 at 100% WTE
CCT date           02/08/2024 + (74.37 × 30.4) days = 10/10/2030
```

### Open-ended LTFT at 60%

The trainee enters an open-ended 60% LTFT change from 01/02/2025.

```text
Full-time period   02/08/2023 to 31/01/2025 = 18.05 credited months
Months remaining   84 - 18.05 = 65.95
Projection         from 01/02/2025 at 60% WTE
CCT date           01/02/2025 + (65.95 ÷ 60% × 30.4) days = 27/03/2034
```

### A change that runs past completion

The trainee enters 50% LTFT from 01/01/2030 to 31/12/2032.

```text
Full-time period   02/08/2023 to 31/12/2029 = 77.06 credited months
LTFT period        01/01/2030 onwards = 50% credit per day
CCT date           26/02/2031, when the required credit is reached
```

The recorded end date is after the calculated CCT date, so validation reports
it as an error. Changing the end date to the CCT date resolves the error
without changing the calculated completion date.

## Grade progression

The result also lists the grade for each year of the programme and the date on
which that year ends. The rules are implemented in
[`CctGradeProgression.java`](src/main/java/uk/nhs/hee/trainee/details/service/CctGradeProgression.java).

- **Years.** There is one row for each year of the programme length, before
  additional and accelerated time. The grade increases by one each year from
  the start grade; DFT remains DFT. A DFT programme's additional and
  accelerated time, and its 18-month final year, apply only to its last row.
- **End dates.** Year N ends when N × 12 months have been completed, plus the
  additional time and less the accelerated time recorded against that grade
  and every earlier grade. The date is found in the same way as the CCT date,
  so no row ends after it and the final row ends on it.
- **18-month final year.** This applies to a programme that ends in a half
  year, such as 90 months. Without it, the final row is a 6-month grade. The
  18-month grade is the last full year before it and includes those 6 months,
  so the 6-month row is removed. The CCT date is unchanged. The specialties
  that run 6 months past ST9 have `eighteenMonthFinalYear` set, so the 18-month
  ST9 is their default and needs no reason while their length and start grade
  are unchanged.
- **Skipped grade.** From the skipped grade onwards, each row shows the next
  grade up. The dates are unchanged; any reduction in training is recorded as
  accelerated time.
- **Past the highest grade.** A year after ST9, CT4, DCT3 or F2 has no grade
  and is flagged.

For example, the 84-month programme above with 3 months of additional time and
1 month of accelerated time on ST1: ST1 requires 12 + 3 − 1 = 14 months and
ends on 30/09/2024. Each later grade ends 2 months later, and ST7 ends on the
CCT date.

The following grade checks prevent a calculation being saved. `/calculate`
returns them in `errors` alongside the result.

- Each grade with additional or accelerated time, the skipped grade and the
  18-month grade must be in the progression. The skipped grade is checked
  without the skip applied, and the 18-month grade cannot be the skipped grade.
- The skipped grade cannot be the start grade, so a programme with an
  unnumbered grade (DFT) cannot skip it.
- The 18-month grade must end the progression and run for 18 months, so it
  must be the last full year of a programme that ends in a half year (ST7 of a
  90-month programme from ST1).
- The accelerated time on a grade must be less than that grade's months (12,
  18, or fewer for a short final year) plus its additional time.

## Implementation outline

The main calculation follows these steps:

```text
months required = programme length + total additional months
                  - total accelerated months

1. Trim changes to the programme period for calculation.
2. Identify the projection start date and WTE.
3. Build assumed full-time periods for gaps before the projection.
4. Add calendar and credited months for recorded periods.
5. Round months remaining to two decimal places.
6. Calculate the CCT date from the projection or recorded periods.
7. Calculate original and adjusted programme end dates.
8. Calculate the end date of each year in the grade progression.
```

The calculator is deliberately independent of persistence and user identity.
The surrounding service handles request validation, saving, and presentation
of calculation errors.
