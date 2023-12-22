# Joplin

A Java client for the
[Philips Hue Entertainment API](https://developers.meethue.com/develop/hue-entertainment/hue-entertainment-api/),
named after [Scott Joplin](https://en.wikipedia.org/wiki/Scott_Joplin),
composer of [The Entertainer](https://en.wikipedia.org/wiki/The_Entertainer_(rag)).

## Using

### Supported Java versions

Java 11+ is required to use the client.

### Installation

The client is available on Maven Central and can be pulled in as a dependency
by adding the usual block to your Maven POM:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.c0urante</groupId>
        <artifactId>joplin</artifactId>
        <version>${joplin.version}</version>
    </dependency>
</dependencies>
```

Or to your Gradle build file:

```groovy
dependencies {
    "io.github.c0urante:joplin:$joplinVersion"
}
```

### Bridge Setup

Follow the steps in the Hue Entertainment API
[getting started guide](https://developers.meethue.com/develop/hue-entertainment/hue-entertainment-api/#getting-started-with-streaming-api)
to find the IP address for your bridge, set up credentials, and create an
entertainment area.

Once you've done all that, proceed to the next section to see how to put
it all to use.

### Creating and initializing a client

```java
import io.github.c0urante.joplin.HueEntertainmentClient;

public class Demo {

  public HueEntertainmentClient createClient() throws IOException, InterruptedException {
    // Instantiate the client
    HueEntertainmentClient client = HueEntertainmentClient.builder()
        .host("192.168.1.1")
        .username("9HihcZzDENE6OAtYHllimwehffPVSA6sucfawIki ")
        .clientKey("D88D89ABBE97E97465D8735A8986BD32")
        .entertainmentArea("acvvdn41-e5tz-s737-oq7s-8snimm9ttk8n")
        .build();

    // Use the bridge REST API to turn on streaming
    // This method must be called before light colors can be set
    client.initializeStream();

    return client;
  }
}
```

### Setting light colors

```java
import io.github.c0urante.joplin.HueEntertainmentClient;
import io.github.c0urante.joplin.Light;

import java.awt.Color;
import java.io.IOException;

public class Demo {

  public void setLights() throws IOException, InterruptedException {
    HueEntertainmentClient client = createClient();
    HueColor color1 = new Rgb(Color.GREEN);
    HueColor color2 = new Rgb(Color.RED);

    // Merry Christmas!
    client.sendColors(color1, color2);

    // Alternatively, if you just want to set N lights to a single fixed color
    HueColor color3 = new Rgb(Color.BLUE);
    client.sendColors(8, color3);

    // Or, if you want to set specific lights to specific colors
    Light light1 = new Light(0, color1);
    Light light3 = new Light(2, color2);
    Light light5 = new Light(4, color3);
    client.sendLights(light1, light3, light5);

    // Don't forget to clean up once you're finished
    client.close();
  }
}
```

## Building

```shell
# Build the library, skip tests, and install to your local Maven repository
mvn clean install -DskipTests
```

## Testing

All tests are currently integration tests. In order to run tests, you need
a working bridge with at least two lights, and all of the information outlined in
[Bridge Setup](#bridge-setup).

Once you have all that, populate the [lights.properties](src/test/resources/lights.properties)
file with the IP address, credentials, etc. for your setup:

```properties
bridge.host = 192.168.1.1
# Should always be 2100 but just in case Philips allows the port to change in the future
# bridge.port =
username = 9HihcZzDENE6OAtYHllimwehffPVSA6sucfawIki
client.key = D88D89ABBE97E97465D8735A8986BD32
entertainment.area.id = acvvdn41-e5tz-s737-oq7s-8snimm9ttk8n
```

If you'd like to ensure that no changes to this file are ever committed, you can
instruct git to ignore it:

```shell
git update-index --skip-worktree src/test/resources/lights.properties
```

And finally, you can run tests with:

```shell
mvn test
```

## Contributing

PRs and GitHub issues welcome!
