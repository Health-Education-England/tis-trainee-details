# CCT Calculator Engine

This document describes how the application calculates a trainee's
Completion of Training (CCT) date. The implementation is in
[`CctEngine.java`](src/main/java/uk/nhs/hee/trainee/details/service/CctEngine.java).

The calculator works with a training programme, its planned duration, and any
periods that change the amount of training credited. It returns the calculated
CCT date, the months completed and remaining, the periods assumed to be full
time, and the details of the projection used.

## Summary

1. **Months required** are the programme length plus additional months, less
   accelerated months.
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

The approximation is for checking results only; the engine's full calculation
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

For projected time, the engine uses 30.4 days per month:

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
engine examines them in date order and calculates the first day on which the
required credit is reached. This also produces a useful CCT date when a
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

## Implementation outline

The main calculation follows these steps:

```text
months required = programme length + additional months - accelerated months

1. Trim changes to the programme period for calculation.
2. Identify the projection start date and WTE.
3. Build assumed full-time periods for gaps before the projection.
4. Add calendar and credited months for recorded periods.
5. Round months remaining to two decimal places.
6. Calculate the CCT date from the projection or recorded periods.
7. Calculate original and adjusted programme end dates.
```

The engine is deliberately independent of persistence and user identity. The
surrounding service handles request validation, saving, and presentation of
calculation errors.
