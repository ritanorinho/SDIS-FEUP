keytool -genkey -keypass password -storepass password -keystore keystore.jks
keytool -export -storepass password -file server.cer -keystore keystore.jks
keytool -import -v -trustcacerts -file server.cer -keypass password -storepass password -keystore truststore.jks
