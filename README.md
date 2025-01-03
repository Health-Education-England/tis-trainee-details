# TIS Trainee Details

[![Build Status][build-badge]][build-href]
[![License][license-badge]][license-href]
[![Quality Gate Status][quality-gate-badge]][quality-gate-href]
[![Coverage Stats][coverage-badge]][coverage-href]

## About
This is a Spring Boot microservice which provides a REST API for retrieving and
managing trainee details data, such as Personal details, Placements and Programmes, 
to be used in the Profile page.

## Developing

### Running

```shell
gradlew bootRun
```

#### Pre-Requisites

- A MongoDB instance.
- A running instance of localstack with the appropriate SQS queue set up
  or permission to push messages onto an AWS SQS queue
  (reference to the queue url set to the environmental variable `EVENT_QUEUE_URL`)

#### Environmental Variables

| Name                                  | Description                                                | Default   |
|---------------------------------------|------------------------------------------------------------|-----------|
| **Database:**                         |                                                            |           |
| DB_HOST                               | The MongoDB host to connect to.                            | localhost |
| DB_PORT                               | The port to connect to MongoDB on.                         | 27017     |
| DB_NAME                               | The name of the MongoDB database.                          | trainee   |
| DB_USER                               | The username to access the MongoDB instance.               | admin     |
| DB_PASSWORD                           | The password to access the MongoDB instance.               | pwd       |
| **Queues:**                           |                                                            |           |
| EVENT_QUEUE_URL                       | The queue for sync event.                                  |           |
| BASIC_DETAILS_UPDATE_QUEUE_URL        | The queue for basic details update.                        |           |
| CONTACT_DETAILS_UPDATE_QUEUE_URL      | The queue for contact details update.                      |           |
| GDC_DETAILS_UPDATE_QUEUE_URL          | The queue for GDC details update.                          |           |
| GMC_DETAILS_UPDATE_QUEUE_URL          | The queue for GMC details update.                          |           |
| PERSONAL_INFO_UPDATE_QUEUE_URL        | The queue for personal info update.                        |           |
| PERSON_OWNER_UPDATE_QUEUE_URL         | The queue for person owner update.                         |           |
| **Redis:**                            |                                                            |           |
| REDIS_HOST                            | Redis server host.                                         | localhost |
| REDIS_PASSWORD                        | Login password of the redis server.                        | password  |
| REDIS_PORT                            | Redis server port.                                         | 6379      |
| REDIS_SSL                             | Whether to enable SSL support.                             | false     |
| REDIS_USERNAME                        | Login username of the redis server                         | default   |
| **Related services:**                 |                                                            |           |
| TRAINEE_REFERENCE_HOST                | The tis-trainee-reference service host.                    | localhost |
| TRAINEE_REFERENCE_PORT                | The tis-trainee-reference service port.                    | 8205      |
| **SNS:**                              |                                                            |           |
| TOPIC_ARN_COJ_SIGNED                  | The SNS topic for sending CoJ signing events.              |           |
| TOPIC_ARN_GMC_DETAILS_PROVIDED        | The SNS topic for GMC detail provided event messages.      |           |
| **Others:**                           |                                                            |           |
| AWS_XRAY_DAEMON_ADDRESS               | The AWS XRay daemon host.                                  |           |
| ENVIRONMENT                           | The environment to log events against.                     | local     |
| SENTRY_DSN                            | A Sentry error monitoring Data Source Name.                |           |
| SIGNATURE_SECRET_KEY                  | The signature secret key.                                  |           |
| SIGNATURE_PLACEMENT_EXPIRY            | The signature placement expiry time in minutes.            | 1440      |
| SIGNATURE_PROGRAMME_MEMBERSHIP_EXPIRY | The signature programme membership expiry time in minutes. | 1440      |

#### Usage Examples

##### Get a Trainee Profile

This endpoint requires request header `token` as a String.

```
GET /trainee/api/trainee-profile
```

##### Get Trainee IDs by Email

```
GET /trainee/api/trainee-profile/trainee-ids?email={email}
```

##### Update Trainee Details

These PATCH endpoints apply to the following trainee details types:
- Basic details*: `basic-details`
- Contact details: `contact-details`
- GDC details: `gdc-details`
- GMC details: `gmc-details`
- Personal information: `personal-info`
- Person owner: `person-owner`
- Placement: `placement`
- Programme membership: `programme-membership`
- Qualification: `qualification`

```
PATCH /trainee/api/{details_type}/{tisId}
```

*The PATCH endpoint for Basic details would publish an event to the SQS queue (`EVENT_QUEUE_URL`)
when a trainee profile is first created, to trigger a full data refresh of the trainee.

##### Delete Trainee Details

These DELETE endpoints apply to the following trainee details types:
- Trainee profile: `trainee-profile`
- Programme membership: `programme-membership`

```
DELETE /trainee/api/{details_type}/{tisId}/
```

##### Delete Particular Trainee Details

These DELETE endpoints apply to the following trainee details types:
- Placement: `placement`
- Qualification: `qualification`

```
DELETE /trainee/api/{details_type}/{tisId}/{details_type_id}
```

### Testing

The Gradle `test` task can be used to run automated tests and produce coverage
reports.
```shell
gradlew test
```

The Gradle `check` lifecycle task can be used to run automated tests and also
verify formatting conforms to the code style guidelines.
```shell
gradlew check
```

### Building

```shell
gradlew bootBuildImage
```

## Versioning

This project uses [Semantic Versioning](https://semver.org).

## License

This project is license under [The MIT License (MIT)](LICENSE).

[coverage-badge]: https://sonarcloud.io/api/project_badges/measure?project=Health-Education-England_TIS-TRAINEE-DETAILS&metric=coverage
[coverage-href]: https://sonarcloud.io/component_measures?metric=coverage&id=Health-Education-England_TIS-TRAINEE-DETAILS
[build-badge]: https://badgen.net/github/checks/health-education-england/tis-trainee-details?label=build&icon=github
[build-href]: https://github.com/Health-Education-England/tis-trainee-details/actions/workflows/ci-cd-workflow.yml
[license-badge]: https://badgen.net/github/license/health-education-england/tis-trainee-details
[license-href]: LICENSE
[quality-gate-badge]: https://sonarcloud.io/api/project_badges/measure?project=Health-Education-England_TIS-TRAINEE-DETAILS&metric=alert_status
[quality-gate-href]: https://sonarcloud.io/summary/new_code?id=Health-Education-England_TIS-TRAINEE-DETAILS
