# Profiling the Neo4j Server (local)
I want to use VisualVM to profile the Neo4j server running locally (OpenJDK 7).

Firstly: I were not able to achieve this. VisualVM was not able to detect the local server's Java process. I guess this is because the server uses the JRE rather than the JDK that VisualVM depends on, but again this is just a guess!
Since VisualVM could not find a Java installation and I had to use the JDK-Switch the problem might be somewhere else.

## Sample the Neo4j server using a JMX connection
However, as a first try I connected to the process using JXM.

### Prepare Neo4j server
To do so, one has to uncomment the following lines in `$NEO_HOME/conf/neo4j-wrapper.conf`:

    wrapper.java.additional=-Dcom.sun.management.jmxremote.port=3637
    wrapper.java.additional=-Dcom.sun.management.jmxremote.authenticate=true
    wrapper.java.additional=-Dcom.sun.management.jmxremote.ssl=false
    wrapper.java.additional=-Dcom.sun.management.jmxremote.password.file=conf/jmx.password
    wrapper.java.additional=-Dcom.sun.management.jmxremote.access.file=conf/jmx.access

Note: `$NEO_HOME` is `/var/lib/neo4j` if installed at Ubuntu using the package manager.

`conf/jmx.access` contains usernames and roles. You need to login with a user that has `readwrite` access in order to use the `Sampler`. There is a default user `monitor` that maches our needs, simply uncomment its declaration line.

    control readwrite

Now we have to define a password for this user in `conf/jmx.password`.

    control myNeatPassword
    
If you start the server now, you will get an error message like the following in `console.log`:
`Error: Password file read access must be restricted: conf/jmx.password`

Note: Log files are placed in `var/lib/neo4j/data/log` (links to `var/log/neo4j`) if installed at Ubuntu using the package manager.

To overcome this issue we have to reduce the read access to user-only using `chmod`:

    sudo chmod 400 conf/jmx.password

The server will now be able to start again. Check that and leave it running in order to connect to its process using VisualVM in the next step.

### Connect to the process with VisualVM using JMX
Start VisualVM (and add the JDK-Home-Switch if necessary)

    jvisualvm --jdkhome /usr/lib/jvm/java-7-.../

right-click at `Local` and add the JMX connection at the correct port, using the credentials of `control`.
You should be able to use the `Sampler` now.
