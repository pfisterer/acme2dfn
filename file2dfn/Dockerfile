FROM openjdk:14-alpine AS builder

# Add libs and entrypoint
ADD lib/ /app/lib/

# Add source
WORKDIR /app/src
ADD src/ /app/src/

# Compile
RUN find . -name '*.java' -print0 | xargs -0 javac -cp $(find /app/lib -type f -iname *.jar | tr "\n" ":")
RUN jar cfv /app/lib/file2dfn.jar de

#------------------------------------------
#------------------------------------------

FROM openjdk:14-alpine

# Copy app + lib jars
WORKDIR /app
COPY --from=builder /app/lib/ /app/

# Add entrypoint
ADD entrypoint.sh /app/
RUN chmod a+x /app/entrypoint.sh

# Create runtime classpath
RUN echo $(ls /app/*.jar | tr "\n" ':') > /app/cp.txt
RUN echo Runtime classpath is `cat /app/cp.txt`

# Add User/Group
RUN addgroup -S pki
RUN adduser -S pki -G pki
RUN chown -R pki:pki /app

USER pki

VOLUME /data
ENTRYPOINT ["/app/entrypoint.sh"]
CMD [""]
