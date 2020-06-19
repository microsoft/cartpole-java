# CartPole Java
This example demonstrates using the classic CartPole example in Java. It assumes the default inkling when the user creates a CartPole example in Bonsai.

The focus of the example is the connection between a model in Java and the Bonsai platform. 

# Structure

The Swagger-generated Bonsai client is in `src/main/java/microsoft/bonsai/simulatorapi` and its child directories. 

The CartPole model, state, action and config files are also in `src/main/java/microsoft/bonsai/simulatorapi` and packaged with the client for this example.

The main entry point is App.java.

# References

The Swagger generated client requires a few dependencies be added to the pom.xml file:

```
<dependency>
    <groupId>com.microsoft.rest</groupId>
    <artifactId>client-runtime</artifactId>
    <version>1.7.5</version>
</dependency>
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>29.0-jre</version>
</dependency>
```

# App.java 
The main interaction between the model and Bonsai happens in App.java.

## Imports

The following imports need to be included:

```java
import java.util.LinkedHashMap;
import com.microsoft.rest.RestClient;
import microsoft.bonsai.simulatorapi.implementation.*;
import microsoft.bonsai.simulatorapi.models.*;
import com.microsoft.rest.serializer.JacksonAdapter;
import com.microsoft.rest.*;
```

## Client configuration

All initial sequence IDs are set to 1. Subsequent sequence IDs are generated by the service.

It should be noted that this example is using access keys to connect to the platform.

```java
int sequence_id = 1;
String workspaceName = "<WORKSPACE>";
String accessKey = "<ACCESS_KEY>";
String baseUrl = "https://api.bons.ai/";

// object that indicates if we have registered successfully
Object registered = null;
String sessionId = "";

//we need to add an authorization header for the access key, so we need to build a rest client
RestClient rc = new RestClient.Builder()
                                .withBaseUrl(baseUrl)
                                .withSerializerAdapter(new JacksonAdapter())        
                                .withResponseBuilderFactory(new ServiceResponseBuilder.Factory())                                                    
                                .build();

// add the Authorization header 
rc.headers().addHeader("Authorization", accessKey);

// using the Swagger-generated client
SimulatorAPIImpl client = new SimulatorAPIImpl(rc);

```

## Registering

The following steps are required for registering with Bonsai:

```java
Sessions sessions = client.sessions();
                
SimulatorInterface sim_interface = new SimulatorInterface();
sim_interface.withName("Cartpole-Java");
sim_interface.withTimeout(60.0);
sim_interface.withCapabilities(null);

// minimum required
sim_interface.withSimulatorContext("{}");

//create only returns an object, so we need to check what type of object
Object registrationResponse = sessions.create(workspaceName, sim_interface);

// if we get an error during registration
if(registrationResponse.getClass() == ProblemDetails.class)
{
    ProblemDetails details = (ProblemDetails)registrationResponse;
    System.out.println(java.time.LocalDateTime.now() + " - ProblemDetails - " +  details.title());
}
// successful registration
else if(registrationResponse.getClass() == SimulatorSessionResponse.class)
{
    registered = registrationResponse;
    SimulatorSessionResponse sessionResponse = (SimulatorSessionResponse)registrationResponse;

    //this is required
    sessionId = sessionResponse.sessionId();
}

System.out.println(java.time.LocalDateTime.now() + " - registered session " + sessionId);
```

## Control loop
After registering an receiving a session ID, the following can be used to check the events that are received from the service in the control loop:

```java
// build the SimulatorState object
SimulatorState sim_state = new SimulatorState();
sim_state.withSequenceId(sequence_id); //required
sim_state.withSessionId(sessionId); //required
sim_state.withState(model.getState()); //required
sim_state.withHalted(model.halted()); //required

//advance only returns an object, so we need to check what type of object
Object response = client.sessions().advance(workspaceName, sessionId, sim_state);

// if we get an error during advance
if(response.getClass() == ProblemDetails.class)
{
    ProblemDetails details = (ProblemDetails)response;
    
    System.out.println(java.time.LocalDateTime.now() + " - ProblemDetails - " +  details.title());
}
// succesful advance
else if(response.getClass() == Event.class)
{

    Event event = (Event) response;
    System.out.println(java.time.LocalDateTime.now() + " - received event: " + event.type());
    sequence_id = event.sequenceId(); // get the sequence from the result

    //now check the type of event and handle accordingly

    if(event.type() == EventTypesEventType.EPISODE_START)
    {
        // ignored event in Cartpole
        CartPoleConfig config = new CartPoleConfig();
        //model.start(event.episodeStart().config());
        
    }
    else if(event.type() == EventTypesEventType.EPISODE_STEP)
    {
        CartPoleAction action = new CartPoleAction();
        
        // action() returns an Object with a class value of LinkedHashMap
        LinkedHashMap map = (LinkedHashMap) event.episodeStep().action(); 
        
        // get the value of the action 
        Object theCommand = map.get("command");

        //sometimes that value is an integer -- somtimes its a double. in either case, CartPoleAction.command is a double
        if(theCommand.getClass() == Integer.class)
            action.command  = ((Integer)theCommand).doubleValue();
        else if (theCommand.getClass() == Double.class)
            action.command  = ((Double)theCommand).doubleValue();

        // move the model forward
        model.step(action);
    }
    else if(event.type() == EventTypesEventType.EPISODE_FINISH)
    {
        System.out.println("Episode Finish");
    }
    else if(event.type() == EventTypesEventType.IDLE)
    {
        Thread.sleep(event.idle().callbackTime().longValue() *  1000);
    }
    else if (event.type() == EventTypesEventType.UNREGISTER)
    {
        client.sessions().delete(workspaceName, sessionId);
    }
}

```