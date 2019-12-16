# TIS-TRAINEE-DETAILS
This is one of the BE microservices of Trainee UI app. In this service SpringBoot, MongoDB, Gradle and RabbitMQ have been used.

####Running TIS-TRAINEE-DETAILS
Like all of the existing projects, we use a build wrapper.

To build the project use:
`./gradlew clean build`

This will create a .jar artifact in the ./build/libs directory

You can run this project without going through the build with

`./gradlew bootRun`

####Testing

To run the tests use:
`./gradlew clean test`