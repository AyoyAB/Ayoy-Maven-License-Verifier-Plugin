# Publish To Maven Central

When a version is ready to be deployed, follow the
instructions below:

1. Check the plugin versions and update accordingly:
```bash
mvn versions:display-plugin-updates
```

2. Check the date of your private keys.
```bash
gpg2 --list-secret-keys
```

If they are expired, change expiration by following this
instruction:
https://www.g-loaded.eu/2010/11/01/change-expiration-date-gpg-key/

and publish them again 

```bash
gpg --keyserver hkp://pgp.mit.edu --send-keys C6EED57A
```

3. Check your gpg stuff in .m2/settings.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
		
<profiles>  
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg2</gpg.executable>
        <gpg.passphrase>the_pass_phrase</gpg.passphrase>
      </properties>
    </profile>
</profiles>
</settings>
```


4. Check your JIRA username / password in .m2/settings.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
		  
    <servers>
	    <server>
		    <id>ossrh</id>
            <username>username</username>
            <password>password</password>
		</server>
    </servers>
</settings>
```

5. Prepare a release by running the following command:
```bash
mvn versions:set -DnewVersion=1.0.0
```

6. Commit and push changes and check travis build status.

7. Run the following command
```bash
mvn clean deploy -DperformRelease=true 
```

8. Restore version to SNAPSHOT:
```bash
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
```
