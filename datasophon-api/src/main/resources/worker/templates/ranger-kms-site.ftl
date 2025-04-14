<configuration>
	<property>
		<name>ranger.service.host</name>
		<value>${rangerServiceHost!"localhost"}</value>
	</property>

	<property>
		<name>ranger.service.http.port</name>
		<value>9292</value>
	</property>
	
	<property>
		<name>ranger.service.shutdown.port</name>
		<value>7085</value>
	</property>
	
	<property>
		<name>ranger.contextName</name>
		<value>/kms</value>
	</property>			
	
	<property>
		<name>xa.webapp.dir</name>
		<value>./webapp</value>
	</property>	
	<property>
		<name>ranger.service.https.port</name>
		<value>9393</value>
	</property>
	<property>
		<name>ranger.service.https.attrib.ssl.enabled</name>
		<value>false</value>
	</property>
	<property>
		<name>ajp.enabled</name>
		<value>false</value>
	</property>
	<property>
		<name>ranger.service.https.attrib.client.auth</name>
		<value>want</value>
	</property>
	<property>
		<name>ranger.credential.provider.path</name>
		<value>/etc/ranger/kms/rangerkms.jceks</value>
	</property>
	<property>
		<name>ranger.service.https.attrib.keystore.file</name>
		<value />
	</property>
	<property>
		<name>ranger.service.https.attrib.keystore.keyalias</name>
		<value>rangerkms</value>
	</property>
	<property>
		<name>ranger.service.https.attrib.keystore.pass</name>
		<value />
	</property>
	<property>
		<name>ranger.service.https.attrib.keystore.credential.alias</name>
		<value>keyStoreCredentialAlias</value>
	</property>

</configuration>