FROM maven:3-openjdk-14

# Add User/Group
#RUN addgroup -S pki
#RUN adduser -S pki -G pki
#RUN chown -R pki:pki /app
#USER pki

# Add source
WORKDIR /app

# Add pom.xml local dependencies
ADD local-maven-repo/ /app/local-maven-repo/
ADD pom.xml /app/
RUN mvn --no-transfer-progress dependency:go-offline

# Add entrypoint
ADD entrypoint.sh /app/
RUN chmod u+x /app/entrypoint.sh

# Compile
ADD src/ /app/src
RUN mvn --no-transfer-progress package
RUN mvn exec:java -q -Dexec.mainClass=de.farberg.file2dfn.Main -Dexec.args="-help"

# Start
VOLUME /data
ENTRYPOINT ["/app/entrypoint.sh"]
CMD [""]
