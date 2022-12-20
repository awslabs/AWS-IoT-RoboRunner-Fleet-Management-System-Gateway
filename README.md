# What's included

This package contains sample code for AWS IoT RoboRunner Fleet Management System Gateway. It is intended to deploy on-premise in a customer site as an AWS IoT GreenGrass v2 Component. Instructions to download, customize, build and deploy the package are below. It is recommended to read through the [AWS IoT RoboRunner documentation](https://docs.aws.amazon.com/iotroborunner/latest/dev/iotroborunner-welcome.html) before using this package.

## Setup

Install the required dependencies:

- [Java 17 runtime environment (SE JRE)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- [Gradle v7.6](https://gradle.org/releases/)

## Configure

Change the FMSG configuration files in the `configuration/` directory.

To be able to run integration tests, change the configuration files in the `integrationtstconfiguration/` directory.

## Build

To build the package

```bash
# If building first time or upgrading gradle version, run the following to create
# a gradle wrapper.
gradle wrapper

# After wrapper was created/updated, you can always use the following for MacOs/Linux
./gradlew build

# To do a clean build, you can run the following for MacOs/Linux
./graldew clean build
```
To build/test the application with Windows, use the `./gradlew.bat` instead of `./gradlew`.

## Test
1. Run unit tests

```bash
# Run the entire test suite (these will  be executed as part of build commands as well)
./gradlew test

# Run a single test
./gradlew test --tests com.amazon.iotroborunner.fmsg.<module-name>.<test-class-name>

# Run a single test with debug logs - useful in debugging test failures
./gradlew test -d --tests com.amazon.iotroborunner.fmsg.<module-name>.<test-class-name>
```

2. Run integration tests locally

```bash
# Run the entire integration test suite
./gradlew integrationTest
```

Integration tests are modeled to be grouped by application type they are testing. And therefore, the system allows

configuration-based conditional test execution where some or all test groups are executed.

To control which integration test groups are executed, the developer will need to modify the configuration in the

`integrationtstconfiguration/` directory, and mark the application of interest as enabled/disabled.


**Note, that the integration tests are not setup to run as part of the build commands
and will need to be explicitly called to be executed as we might not always want to
execute them when building.**


## Deploy

1. Upload the generated build artifacts to an S3 bucket for deployment to GreenGrass. Source your AWS account credentials to grant access to S3. Run the script to upload to S3:

```bash
<package-root>/scripts/upload_to_s3.sh <s3-bucket-name>
```

The script uploads 2 zip archives to the S3 bucket in an `artifacts` directory:

- `fmsg_application.zip`: the FMSG application artifacts
- `fmsg_configs.zip`: the FMSG configuration JSON files

2. Create a GreenGrass Component using the recipe provided in the `greengrass` directory by replacing placeholders with the correct name and artifact S3 bucket names.

3. Deploy the GreenGrass Component to the AWS IoT Core device. The FMSG application is started by the recipe. FMSG application logs should be available in the GreenGrass logs directory on the device.

## Development

### Integration Test Development
Integration tests are modeled to be grouped by application type they are testing:
- Shared Space Management Application
- Worker Property Updates Application.

The integration test framework allows configuration-based conditional test execution where some or all test groups are executed.
To control which integration test groups are executed, the developer will need to modify the configuration in the
`integrationtstconfiguration/` directory, and mark the application of interest as enabled/disabled. For example,
to enable Worker Property Updates integration tests to be executed and skip Shared Space Management integration tests,
the developer will need to supply the following properties in the
`integrationtestconfiguration/roboRunnerFmsgConfiruation.json` file:
```json
"enableWorkerPropertyUpdates": true,
"enableSpaceManagement": false
```
Each new added integrationTest file that is subject to conditional execution, should be annotated accordingly
on the class level to belong to the correct test group:
- `@SmIntegrationTest` or
- `@WpuIntegrationTest`

and additionally have the `@RunTestIfApplicationEnabled` annotation on class level.

## Helpful Tip(s)
1. To see all possible helpful tasks that are defined within build.gradle or come by default,
   run the following:
```bash
./gradlew tasks
```
or
```bash
./gradlew tasks --all
```

2. To get more verbose output from the commands, use the following property `--console=verbose` with your commands.
   Example:
```bash
./gradlew clean build --console=verbose
```

3. To see the task tree, use the following command:
```bash
 ./gradlew check taskTree
```

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.
