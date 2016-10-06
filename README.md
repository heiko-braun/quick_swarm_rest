# WildFly Swarm REST Quickstart

## Introduction

This quickstart uses WildFly Swarm to expose a RESTful endpoint and exposes the service contract using Swagger.
It provides a simple API like this:

```
GET api/say/{id}       - Say Hello to the user name
```

The REst endpoint use default JAX-RS API, and the service contract are provided through the Swagger fraction. To allow for remote monitoring,
the management fraction has been added to provide access to the server status.

The static resources (index.html file containing the link to the swagger.json doc file).

The MainApp class is bootstrapped by the WildFly Swarm container when we launch it.

```
public static void main(String[] args) throws Exception {
    Swarm container = new Swarm();
		[...]
		container.start();

    JAXRSArchive deployment = ShrinkWrap.create(JAXRSArchive.class);
		deployment.addResource(RestEndpoint.class);
		deployment.staticContent();

		[...]

		container.deploy(deployment);

```

To configure the logging appender responsible to collect the logs, we will add the Logging Fraction and define it as such

```
container.fraction(
  new LoggingFraction()
     .fileHandler("swarm-camel", f -> {

           Map<String, String> fileProps = new HashMap<>();
           fileProps.put("path", LOG_FILE);
           f.file(fileProps);
           f.level(Level.INFO);
           f.formatter("%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n");
         })
     .rootLogger(Level.INFO,"swarm-camel")
);
```

A FileHandler is defined with the name of the Logging file, the logging level and the format to be used to save the informations. To configure the Logging Api
to use this appender for the root logger, we have also configured the rootLogger field with the id of the fileHandler created.

The Swagger API information is exposed through a custom shrinkwrap descriptor:

```
SwaggerArchive archive = deployment.as(SwaggerArchive.class);
archive.setResourcePackages("org.wildfly.swarm.examples");
archive.setTitle("WildFly Swarm REST Example");
archive.setDescription("An example using REST and Swagger");
archive.setVersion("1.0");
archive.setResourcePackages(RestEndpoint.class.getPackage().getName());

```
# Build & Execute Standalone

## Build
You will need to compile this example first:

```
mvn install
```

## Run

To run the example type
```
mvn wildfly-swarm:run
```

The rest service can be accessed from the following url

    curl http://localhost:8080/service/say/{name}
<http://localhost:8080/service/say/{name}>

For example to say Hello for the name `charles`

    curl http://localhost:8080/service/say/charles
<http://localhost:8080/service/say/charles>

### API Descriptions

The rest services provides Swagger API which can be accessed from the following url

    curl http://localhost:8080/swagger.json
<http://localhost:8080/swagger.json>

This would result in:
```
{
    "basePath": "/",
    "info": {
        "description": "An example using REST and Swagger",
        "title": "WildFly Swarm REST Example",
        "version": "1.0"
    },
    "paths": {
        "/service/say/{name}": {
            "get": {
                "description": "Returns the response as a string",
                "operationId": "say",
                "parameters": [
                    {
                        "description": "name",
                        "in": "path",
                        "name": "name",
                        "required": true,
                        "type": "string"
                    }
                ],
                "responses": {
                    "200": {
                        "description": "successful operation",
                        "schema": {
                            "type": "string"
                        }
                    }
                },
                "summary": "Respond to request",
                "tags": [
                    "demo"
                ]
            }
        }
    }
}

```

To stop the example hit <kbd>ctrl</kbd>+<kbd>c</kbd>

### Management

We have registered the management fraction in order to access runtime status of the server

Here are some curl request that we can use to grab JVM data

```
curl --digest -D - http://localhost:9990/management --header "Content-Type: application/json" -d '{"operation":"read-resource", "include-runtime":"true" , "address":["core-service","platform-mbean","type","memory"], "json.pretty":1}'
```

Results in:

```
{
    "outcome" : "success",
    "result" : {
        "heap-memory-usage" : {
            "init" : 268435456,
            "used" : 179638608,
            "committed" : 857210880,
            "max" : 3817865216
        },
        "non-heap-memory-usage" : {
            "init" : 2555904,
            "used" : 63613736,
            "committed" : 69730304,
            "max" : -1
        },
        "object-name" : "java.lang:type=Memory",
        "object-pending-finalization-count" : 0,
        "verbose" : false
    }
}
```

# Running the example on OpenShift

It is assumed that an OpenShift platform is already running. If not, you can find details how to setup the infrastructure hereafter and more information here
[get started](https://github.com/jimmidyson/minishift).

## Launch minishift

```
./scripts/create-minishift.sh
eval $(minishift docker-env)
oc login -uadmin -padmin
```

When the example runs in minishift, you can use the OpenShift client tool to inspect the status

To list all the running pods:

```
oc get pods
```

Then find the name of the pod that runs this quickstart, and output the logs from the running pods with:

```
oc logs <name of pod>
```

You can also use the OpenShift web console to manage the running pods, and view logs and much more.

```
 minishift console
```

## Build and deploy the services to openshift

```
mvn fabric8:deploy
```

## Access services from outside openshift

You can use any browser to perform a HTTP GET. This allows you to very easily test a few of the RESTful services we defined:

```
curl $(minishift service swarm-rest-example --url=true)/service/say/pong
```

## Remove all openshift assets
This removes all pods, services, etc

```
mvn fabric8:undeploy
```
