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
container.fraction(new LoggingFraction()
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
## Build

You will need to compile this example first:

    mvn install

## Run

To run the example type

    mvn wildfly-swarm:run

The rest service can be accessed from the following url

    curl http://localhost:8080/service/say/{name}
<http://localhost:8080/service/say/{name}>

For example to say Hello for the name `charles`

    curl http://localhost:8080/service/say/charles
<http://localhost:8080/service/say/charles>

## API Descriptions

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

## Management

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

## Running the example on OpenShift

It is assumed that an OpenShift platform is already running. If not, you can find details how to setup the infrastructure hereafter and more information here 
[get started](https://github.com/jimmidyson/minishift).

* Launch minishift

```
minishift delete
minishift start --deploy-registry=true --deploy-router=true --memory=4048 --vm-driver="xhyve"
minishift docker-env
eval $(minishift docker-env)
```

Remark : Don't forget to be authenticated with the OpenShift Server using the command `oc login -u admin -n default`

The example can be built and deployed using a single goal:

    mvn -Popenshift-local-deploy

When the example runs in fabric8, you can use the OpenShift client tool to inspect the status

To list all the running pods:

    oc get pods

Then find the name of the pod that runs this quickstart, and output the logs from the running pods with:

    oc logs <name of pod>

You can also use the OpenShift web console to manage the running pods, and view logs and much more.

As we have mounted a Kubernetes Volume between the host and the linux container, you can also consult the content of the WildFly Swarm log file `/var/log/swarm.log` if you ssh to the
virtual machine. If you use minishift, simply run this command `minishift ssh` to access it.

## Access services using a web browser

You can use any browser to perform a HTTP GET. This allows you to very easily test a few of the RESTful services we defined:

Notice: As it depends on your OpenShift setup, the hostname (route) might vary. Verify with oc get routes which hostname is valid for you.

Use this URL to display the response message from the REST service:

    export serviceURL=$(minishift service swarm-camel --url=true -n swarm-demo)
    curl $serviceURL/service/say/charles

## Testing

To test the service and also the pod deployed, we have created an integration test using the [Arquillian Testing framework](http://arquillian.org/) and the [Kubernetes Client
Api](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian) responsible to talk with the OpenShift platform in order to retrieve the pods, services, replication controllers, ... and to perform some assertions.

To check that a replication controller exists, you will design such JUnit Test

```java

@ArquillianResource
KubernetesClient client;
    
@Test
public void testAppProvisionsRunningPods() throws Exception {
    // assert that a Replication Controller exists
    assertThat(client).replicationController("swarm-camel");
}
```

or this one for a pod

```java
    @Test
    public void testPod() throws Exception {

        assertThat(client).pods()
                .runningStatus()
                .filterNamespace(session.getNamespace())
                .haveAtLeast(1, new Condition<Pod>() {
                    @Override
                    public boolean matches(Pod podSchema) {
                        return true;
                    }
                });
    }
```

And to test a HTTP endpoint/service deployed

```java
@Test
public void testHttpEndpoint() throws Exception {
    // assert that a pod is ready from the RC... It allows to capture also the logs if they barf before trying to invoke services (which may not be ready yet)
    assertThat(client).replicas("swarm-camel").pods().isPodReadyForPeriod();

    // Fetch the External Address of the Service
    String serviceURL = KubernetesHelper.getServiceURL(client,"swarm-camel",KubernetesHelper.DEFAULT_NAMESPACE,"http",true);
```

To run the Junit test, simply execute this maven command

    mvn test -Dtest=OpenshiftIntegrationKT
    mvn test -Dtest=OpenshiftServiceKT
    
Remark : Please verify prior to execute the test that you are logged to OpenShift using the admin user `oc login -u admin -n default`    

### All the Steps to execute to test Quickstart
 
```
bin/delete_start_minishift.sh
minishift start --deploy-registry=true --deploy-router=true --memory=4048 --vm-driver="xhyve"
eval $(minishift docker-env)
oc login -u admin -p admin -n default
mvn -Popenshift-local-deploy
mvn test -Dtest=OpenshiftIntegrationKT
mvn test -Dtest=OpenshiftServiceKT
```