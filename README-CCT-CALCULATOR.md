# CCT Calculator Engine

This explains how the calculator works out a trainee's Completion of Training
(CCT) date. It starts in plain English, then goes through each step with pseudo
code and worked examples, so you can follow the business logic without reading
the Java.

The basic logic comes from an examination of NHS Calculators (including North West (NW) Excel calculator, and RCEM Excel Calculator). See https://hee-tis.atlassian.net/wiki/spaces/NTCS/pages/5738266632/TSS+-+Certificate+of+Completion+of+Training+CCT+Calculation#NHS-CCT-calculator-logic for the 2 main calculation used.

The CCT Calculation engine code in [`CctEngine.java`](src/main/java/uk/nhs/hee/trainee/details/service/CctEngine.java) is itself a reimplemented the [`JS calculation code in the POC`](https://github.com/john12321/cct-calculator-app/blob/main/src/core/calculations.ts) which mapped the NW Excel calculation logic.

Note: This file doesn't cover grade progression, which isn't built yet.

## The short version

A training programme needs a set number of full-time months. The calculator
adds up the months a trainee has been credited with, works out what's left, and
turns that into a date.

1. **Months needed** are the programme length, plus any additional months, less
   any accelerated months.
2. **Months completed** are the credit from each change the trainee has entered,
   plus full credit for the time around those changes.
3. **Months remaining** are months needed less months completed, rounded to 2
   decimal places.
4. **If months remain,** the CCT date is the projection start plus months
   remaining ÷ WTE × 30.4 days.
5. **If nothing remains,** the CCT date is the day the running total reached the
   months needed.

**A quick and dirty way to check a result:** a change moves the CCT date by the training
time it loses. Six months at 60% loses 40% of 6 months, which is 2.4 months, or
about 73 days. So the date moves about 73 days past the programme end date.

```text
CCT date ≈ adjusted programme end date + months lost × 30.4 days

months lost
  each dated change:        calendar months × (100% - credit rate)
  an open-ended LTFT:       months remaining × (1 ÷ WTE - 1)
```

This shortcut landed within 2 days of the real date in 98% of the scenarios
tested, and never more than 5 days away. Nice chack but not a replacement for the calculator.

## Terms

| Term                     | Meaning                                                                                                      |
| ------------------------ | ------------------------------------------------------------------------------------------------------------ |
| CCT date                 | Completion of Training date: the day the trainee is projected to finish                                      |
| WTE                      | Whole time equivalent: the share of full time worked, so 60% is 3 days in 5                                  |
| LTFT                     | Less than full time training                                                                                 |
| OOP                      | Out of programme                                                                                             |
| Change                   | A period that isn't ordinary full-time training, such as LTFT, out of programme time or parental leave       |
| Credit rate              | The share of a period that counts as training                                                                |
| Calendar months          | The length of a period, in months                                                                            |
| Credited months          | Calendar months × credit rate: what counts towards completion                                                |
| Assumed full-time period | Time around the changes. The trainee doesn't enter it, and it's credited at 100%                             |
| Recorded time            | All the changes with an end date, and the assumed full-time periods                                          |
| Projection               | The time after the recorded time, up to the CCT date                                                         |
| Open-ended LTFT          | An LTFT change with no end date ("It lasts for the rest of my training")                                     |
| NW workbook              | The North West Training End Date Calculator v2.17, the Excel workbook this logic comes from                  |
| Saveable                 | A calculation that can be saved: no change starts before the programme, or starts or ends after the CCT date |

## The two formulas

Everything rests on two conversions, both taken from the NW workbook.

```text
Recorded time: days to months (NW workbook cells F24:G46)
  calendar months = (end date - start date + 1) ÷ (365 ÷ 12)
  credited months = calendar months × credit rate

Projection: months to days (NW workbook cell I47)
  days to go = months remaining ÷ projection WTE × 30.4
```

- **The + 1** counts both the first and the last day of a period.
- **The WTE divides in the second formula** because a month of training takes
  longer at less than full time. One month at 50% takes two calendar months.
- **"Recorded" doesn't mean "past".** The calculator never looks at today's
  date. A change planned for next year is recorded time, so the first formula
  applies to it.

**Why there are two month lengths**

- **What they are.** 365 ÷ 12 is 30.4167 days. The other is 30.4 days.
- **Where they come from.** Both are in the NW workbook, which doesn't say why.
  30.4 is 365 ÷ 12 rounded to one decimal place, so they look like two versions
  of one number rather than a policy.
- **What they do.** A recorded month uses slightly more days than a projected
  month. That adds up to about 1 day for every 5 years of recorded time.
- **Why both stay.** Merging them moves many dates away from the workbook. See
  "Could the business logic be simpler?".

## Step by step

The calculator receives the programme details and the list of changes.

```text
STEP 1  Work out the months needed
  months needed = programme length + additional months - accelerated months

STEP 2  Work out where the projection starts:
  a. if there is an open-ended LTFT change,
    projection start = that change's start date
    projection WTE   = that change's WTE
  b. otherwise, if there are other changes
    projection start = the day after the latest change ends
    projection WTE   = 100%
  c. otherwise,
    projection start = the programme start date
    projection WTE   = 100%

STEP 3  Fill in the gaps
  every day from the programme start up to the projection start
  that isn't inside a change belongs to an assumed full-time period

STEP 4  Add up the months completed
  for each change with an end date, and each assumed full-time period:
    calendar months = (end date - start date + 1) ÷ (365 ÷ 12)
    credited months = calendar months × credit rate
  months completed = the total of the credited months

STEP 5  Work out the months remaining
  months remaining = months needed - months completed
  round to 2 decimal places

STEP 6  Work out the CCT date
  if months remaining is more than 0
    days to go = months remaining ÷ projection WTE × 30.4
    round days down to a whole day
    CCT date = projection start + days to go
  otherwise,
    go through the recorded time in date order, one day at a time
    add each day's credit to a running total
    CCT date = the first day that months needed less the running total
               rounds to 0 (or slightly less)

STEP 7  Work out the programme end dates:
  original end = programme start + (programme length × 30.4), rounded down
  adjusted end = programme start + (months needed × 30.4), rounded down
```

A few details behind those steps:

- **Rounding down matches Excel.** Excel keeps a fraction of a day in a date and
  shows the day it falls in, so the whole-day part is what people see.
- **Rounding to 2 decimal places matches Excel,** in cell I15. It means anyone
  can reproduce the date from the months remaining shown on screen.
- **There's a tiny allowance when rounding down.** A computer can store 84 ÷
  0.56 × 30.4 as 4,559.999999999999 instead of 4,560, which would lose a day. So
  the engine adds 0.000000001 of a day first.
- **The engine trusts its input.** Validation runs first and rejects, for
  example, overlapping changes or a second open-ended LTFT.
- **Two rules are checked after calculating.** A change can't start before the
  programme, or start or end after the CCT date. A preview reports these as
  errors and still shows figures, so the trainee can see what to fix. Saving
  refuses them.

## How each type of change is credited

| Period x                                                                                                                                | Counts as training?                            | Credit rate                                      |
| --------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------- | ------------------------------------------------ |
| Assumed full-time period                                                                                                                | Always                                         | 100%                                             |
| LTFT                                                                                                                                    | Always                                         | Its WTE, 1% to 99%                               |
| OOPT (training)                                                                                                                         | The trainee chooses, at most 12 months counted | 100% if counted, otherwise 0%                    |
| OOPR (research)                                                                                                                         | The trainee chooses                            | The approved 1% to 100% if counted, otherwise 0% |
| OOPC (career break), OOPE (experience), OOPP (pause), parental leave, phased return to work, shielding, sickness absence, accrued leave | Never                                          | 0%                                               |

A period at 0% still uses up calendar time, so it pushes the CCT date back by
its whole length.

## Worked examples

All five use Paediatrics: 84 months from 02/08/2023, with no additional or
accelerated months. Figures are shown to 2 decimal places, but the calculator
keeps the full figures until step 5. Every result here came from running the
engine.

### 1. No changes

```text
Months needed      84
Months completed   0
Months remaining   84.00
Days to go         84.00 ÷ 100% × 30.4 = 2,553.6, rounded down to 2,553
CCT date           02/08/2023 + 2,553 days = 29/07/2030
```

### 2. LTFT at 60% for six months

The trainee enters LTFT at 60% from 01/02/2024 to 01/08/2024.

| Period            | Dates                    | Days | Calendar months | Credit rate | Credited months |
| ----------------- | ------------------------ | ---- | --------------- | ----------- | --------------- |
| Assumed full time | 02/08/2023 to 31/01/2024 | 183  | 6.02            | 100%        | 6.02            |
| LTFT              | 01/02/2024 to 01/08/2024 | 183  | 6.02            | 60%         | 3.61            |
| **Total**         |                          |      |                 |             | **9.63**        |

```text
Months remaining   84 - 9.63 = 74.37
Projection         from 02/08/2024 at 100%, the day after the change ends
Days to go         74.37 ÷ 100% × 30.4 = 2,260.8, rounded down to 2,260
CCT date           02/08/2024 + 2,260 days = 10/10/2030
```

The quick check agrees: 2.41 months lost is 73 days, and 29/07/2030 plus 73 days
is 10/10/2030.

### 3. LTFT at 60% for the rest of training

The trainee enters open-ended LTFT at 60% from 01/02/2025. The time before it is
one assumed full-time period, 02/08/2023 to 31/01/2025: 549 days, or 18.05
months.

```text
Months remaining   84 - 18.05 = 65.95
Projection         from 01/02/2025 at 60%, the start of the open-ended change
Days to go         65.95 ÷ 60% × 30.4 = 3,341.5, rounded down to 3,341
CCT date           01/02/2025 + 3,341 days = 27/03/2034
```

### 4. Several changes

The trainee enters parental leave, then research counted at 50%, then open-ended
LTFT at 80% from 01/02/2027.

| Period            | Dates                    | Days | Calendar months | Credit rate | Credited months |
| ----------------- | ------------------------ | ---- | --------------- | ----------- | --------------- |
| Assumed full time | 02/08/2023 to 31/05/2024 | 304  | 9.99            | 100%        | 9.99            |
| Parental leave    | 01/06/2024 to 30/11/2024 | 183  | 6.02            | 0%          | 0.00            |
| Assumed full time | 01/12/2024 to 31/08/2025 | 274  | 9.01            | 100%        | 9.01            |
| OOPR              | 01/09/2025 to 31/08/2026 | 365  | 12.00           | 50%         | 6.00            |
| Assumed full time | 01/09/2026 to 31/01/2027 | 153  | 5.03            | 100%        | 5.03            |
| **Total**         |                          |      |                 |             | **30.03**       |

```text
Months remaining   84 - 30.03 = 53.97
Projection         from 01/02/2027 at 80%, the start of the open-ended change
Days to go         53.97 ÷ 80% × 30.4 = 2,050.9, rounded down to 2,050
CCT date           01/02/2027 + 2,050 days = 12/09/2032
```

### 5. A change that runs past completion

The trainee enters LTFT at 50% from 01/01/2030 to 31/12/2032, which is longer
than the training they have left.

| Period            | Dates                    | Days  | Calendar months | Credit rate | Credited months |
| ----------------- | ------------------------ | ----- | --------------- | ----------- | --------------- |
| Assumed full time | 02/08/2023 to 31/12/2029 | 2,344 | 77.06           | 100%        | 77.06           |
| LTFT              | 01/01/2030 to 31/12/2032 | 1,096 | 36.03           | 50%         | 18.02           |
| **Total**         |                          |       |                 |             | **95.08**       |

```text
Months remaining   84 - 95.08 = -11.08, so nothing remains
Left at 01/01/2030 84 - 77.06 = 6.94 months
Credit per day     50% × 12 ÷ 365 = 0.0164 months
Day found          day 422 of the change, when the remaining rounds to 0
CCT date           26/02/2031
```

- **The change is flagged.** It ends after the CCT date, so the trainee sees "End
  date cannot be after the current projected Completion of Training Date
  (26/02/2031)."
- **One edit fixes it.** Ending the change on 26/02/2031 clears the error, and
  the CCT date stays the same.

## Things that can surprise people

- **84 months isn't exactly 7 calendar years.** Example 1 ends on 29/07/2030, 3
  days before 01/08/2030. Each month is 30.4 days, so 84 months is 2,553 days,
  while those 7 years have 2,557. The NW workbook gives the same date.
- **Recording time can move the date by a day.** Entering the first two years as
  a full-time row in the NW workbook moves example 1 from 29/07/2030 to
  30/07/2030, though nothing about the training has changed. That's the two month
  lengths at work, and the calculator behaves the same way.
- **When a change happens can move the date slightly.** Twelve months at 60%
  gives a date from 22/12/2030 to 24/12/2030, depending on where it falls in the
  programme. Six months of parental leave gives 28/01/2031 to 30/01/2031.
- **A dated LTFT change stops at its end date.** After that the projection is at
  100%. A trainee who's staying LTFT needs the open-ended option.
- **Anything not entered counts as full time.** A missing LTFT period gives a
  date that's too early.
- **Months remaining can be negative.** It shows how far the recorded credit
  goes past the months needed, as in example 5.

## How it differs from the NW workbook

The arithmetic is the same. Three things are different by design.

| Area                             | NW workbook                                     | Calculator                                                  |
| -------------------------------- | ----------------------------------------------- | ----------------------------------------------------------- |
| Entering time                    | Every period is typed in, including full time   | Only changes are entered, and the rest is assumed full time |
| WTE of the projection            | The last WTE in the grid                        | 100%, unless there's an open-ended LTFT change              |
| A change running past completion | Accepted, and its end date becomes the CCT date | Flagged, with the day training completes as the CCT date    |

## Could the business logic be simpler?

**Not without moving dates.** Each idea below was run over 228,301 saveable
scenarios with every WTE at 50% or above.

| Idea                                       | CCT dates that move | By how much                                                      |
| ------------------------------------------ | ------------------- | ---------------------------------------------------------------- |
| Use 30.4 days everywhere                   | 38%, all earlier    | 1 or 2 days in 99% of those that move                            |
| Use 365 ÷ 12 days everywhere               | 68%, all later      | 1 to 5 days, and 95% of programme end dates move by up to 2 days |
| Don't round the months remaining           | 7.5%                | 1 day, in either direction                                       |
| Drop the tiny allowance when rounding down | 0.02%               | 1 day earlier                                                    |

The day-by-day search in step 6 is the most complicated part of the engine, and
it's worth knowing what it's for.

- **It never changes a saveable result.** On all 291,964 saveable scenarios it
  gave the same date as the workbook's simple rule, the latest end date.
- **It exists for changes that need fixing.** It gives the trainee the date to
  correct to, as in example 5. The workbook's rule would accept the change.

Two other ideas gave identical dates on every saveable scenario, but aren't worth
doing. A direct formula could replace the day-by-day loop, and whole-number
arithmetic could replace the tiny allowance. Both swap something that reads like
the rule for something harder to follow.

## How this was checked (mainly by Claude)

- **Against the NW workbook itself.** 399 scenarios were entered into copies of
  the workbook and recalculated in LibreOffice (my DB of choice!).
  - All 352 saveable scenarios matched the engine on both the CCT date and the
    months remaining. In 60 of them training completes inside a change.
  - All 40 scenarios that run past completion matched on months remaining, and
    differed on the date by design.
  - All 7 scenarios with full-time rows typed in matched the engine.
- **Against the workbook's formulas.** 400,000 random scenarios went through the
  real engine, and the real validator accepted all of them. The 291,964 saveable
  ones all matched a restatement of cells G47, I15, I47 and B13.
- **Local Office testing, separately.** Local Office staff compared results with
  the calculators they use day to day. Even complex cases matched, give or take
  the odd day. A different month
  length or rounding rule would be enough to cause one.
- **LibreOffice isn't Excel.** It runs the same formulas, but maybe where a date falls
  within a tiny fraction of a whole day, the two could differ by a day.
- **The scenarios** had programme lengths from 1 to 120 months, up to 6 dated
  changes of every type, and sometimes an open-ended LTFT.

## Where each step lives

| Step | What it does              | Java                                          | NW workbook                        |
| ---- | ------------------------- | --------------------------------------------- | ---------------------------------- |
| 1    | Months needed             | `calculate`                                   | I13                                |
| 2    | Projection start and WTE  | `deriveProjection`                            | I47                                |
| 3    | Assumed full-time periods | `deriveAssumedPeriods`                        | None: typed in by hand             |
| 4    | Credited months           | `completedMonths`, `wteMonths`, `wteFraction` | F24:G46, G47                       |
| 5    | Months remaining          | `roundToTwoDecimals`                          | I15                                |
| 6    | Date from the projection  | `dateAfterMonths`                             | I47, B13                           |
| 6    | Day training completes    | `findCompletionDate`                          | None: B13 uses the latest end date |
| 7    | Programme end dates       | `dateAfterMonths`                             | None                               |

In step 4, the Java gets the assumed full-time total by subtraction today: the
time before the projection, less the time in changes. On anything saveable it
gives the same figure as adding up the rows, as the pseudo code does.

In step 6, the Java always runs the day-by-day search first, and uses the
projection if the search finds no day. On anything saveable it gives the same
date as the order in the pseudo code.
